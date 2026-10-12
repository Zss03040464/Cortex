package com.cortex.ai.agent.agy

import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

/** Owns one CLI session. A failed or cancelled session is never restarted implicitly. */
internal class AgyStreamSession(
    private val startProcess: () -> Process,
    private val stopProcess: (Process) -> Unit = { it.destroy() },
) : AutoCloseable {
    private val askLock = Any()
    private val lifecycleLock = Any()
    private val events = ArrayBlockingQueue<JSONObject>(4096)
    @Volatile private var failure: Exception? = null
    @Volatile private var process: Process? = null
    @Volatile var conversationId: String? = null
        private set

    fun ask(
        prompt: String,
        timeoutMillis: Long = 3_600_000L, // 默认 1 小时单任务最大保护时限
        idleTimeoutMillis: Long = 180_000L, // 3 分钟无输出空闲超时（真假死防护）
        onEvent: (JSONObject) -> Unit = {},
    ): JSONObject = synchronized(askLock) {
        require(timeoutMillis > 0) { "Timeout must be positive" }
        require(prompt.length <= MAX_LINE_CHARS / 2) { "Prompt exceeds stream limit" }
        try {
            val child = synchronized(lifecycleLock) {
                failure?.let { throw it }
                process ?: startProcess().also { process = it; readOutput(it) }
            }
            val hardDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.coerceAtMost(86_400_000L))
            val idleTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(idleTimeoutMillis.coerceAtMost(timeoutMillis))
            var lastActiveTime = System.nanoTime()

            val input = JSONObject().put("event", "user")
                .put("message", JSONObject().put("content", prompt)).toString() + "\n"
            // Writing also runs off-thread so a CLI that stops reading cannot defeat the timeout.
            thread(name = "agy-input", isDaemon = true) {
                try {
                    child.outputStream.write(input.toByteArray(Charsets.UTF_8))
                    child.outputStream.flush()
                } catch (_: Exception) { fail(IOException("Agy input stream failed")) }
            }
            while (true) {
                val now = System.nanoTime()
                if (now >= hardDeadline) throw TimeoutException("Agy task exceeded maximum duration of 1 hour")
                if (now - lastActiveTime >= idleTimeoutNanos) throw TimeoutException("Agy idle timed out (no output for 3 minutes)")
                failure?.takeUnless { it is EndOfStreamException }?.let { throw it }
                val event = events.poll(50, TimeUnit.MILLISECONDS)
                if (event != null) {
                    lastActiveTime = System.nanoTime() // 只要收到任何事件，立即刷新保活时间！
                    consume(event, onEvent)?.let { return@synchronized it }
                } else {
                    failure?.let { throw it }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            throw IOException("Agy result missing")
        } catch (error: Exception) {
            fail(error)
            throw error
        }
    }

    private fun consume(event: JSONObject, onEvent: (JSONObject) -> Unit): JSONObject? {
        val payload = event.optJSONObject(event.optString("event")) ?: event
        if (event.optString("event") == "init") {
            conversationId = event.optString("conversation_id", payload.optString("conversation_id")).takeIf { it.isNotBlank() }
        }
        onEvent(event)
        if (event.optString("event") != "result") return null
        if (payload.optString("status") != "SUCCESS") throw IOException("Agy returned a non-success result")
        return payload
    }

    private fun readOutput(child: Process) {
        thread(name = "agy-stderr", isDaemon = true) {
            try { child.errorStream.use { stream ->
                val buffer = ByteArray(4096)
                while (stream.read(buffer) != -1) { /* Drain without retaining potentially private diagnostics. */ }
            } } catch (_: Exception) { /* Process termination closes this stream. */ }
        }
        thread(name = "agy-stdout", isDaemon = true) {
            try {
                child.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            val event = try { JSONObject(line) }
                                catch (_: Exception) { throw IOException("Invalid Agy stream event") }
                            if (!events.offer(event, 5, TimeUnit.SECONDS)) {
                                throw IOException("Agy event queue exceeded limit")
                            }
                        }
                    }
                    throw EndOfStreamException()
                }
            } catch (error: Exception) { fail(error) }
        }
    }

    private fun fail(error: Exception) {
        val child = synchronized(lifecycleLock) {
            if (failure == null || failure is EndOfStreamException) failure = error
            process
        }
        child?.let { runCatching { stopProcess(it) } }
    }

    fun cancel() = fail(IOException("Agy session cancelled"))
    override fun close() = cancel()

    private class EndOfStreamException : IOException("Agy stream closed before a result")
    private companion object { const val MAX_LINE_CHARS = 1_048_576 }
}
