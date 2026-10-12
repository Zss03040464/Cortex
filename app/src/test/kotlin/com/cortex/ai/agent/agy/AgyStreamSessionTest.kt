package com.cortex.ai.agent.agy

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AgyStreamSessionTest {
    private fun process(script: String) = ProcessBuilder("/bin/sh", "-c", script).start()

    @Test fun reusesProcessAndSendsUserMessages() {
        var starts = 0
        AgyStreamSession({
            starts++
            process("""
                echo '{"event":"init","conversation_id":"conversation-1","init":{"tools":[]}}'
                while IFS= read -r line; do
                  printf '{"event":"result","result":{"status":"SUCCESS","echo":%s}}\n' "${'$'}line"
                done
            """.trimIndent())
        }).use { session ->
            val events = mutableListOf<String>()
            val first = session.ask("hello\nworld") { events += it.getString("event") }
            assertEquals("user", first.getJSONObject("echo").getString("event"))
            assertEquals("hello\nworld", first.getJSONObject("echo").getJSONObject("message").getString("content"))
            assertEquals("conversation-1", session.conversationId)
            assertEquals("SUCCESS", session.ask("second").getString("status"))
            assertEquals(listOf("init", "result"), events)
            assertEquals(1, starts)
        }
    }

    @Test fun errorPoisonsSessionWithoutReplay() {
        var starts = 0
        AgyStreamSession({ starts++; process("read line; echo '{\"event\":\"result\",\"result\":{\"status\":\"ERROR\"}}'") }).use { session ->
            assertThrows(Exception::class.java) { session.ask("one") }
            assertThrows(Exception::class.java) { session.ask("two") }
            assertEquals(1, starts)
        }
    }

    @Test fun eofIsFailure() {
        AgyStreamSession({ process("read line; exit 0") }).use { session ->
            assertThrows(Exception::class.java) { session.ask("one") }
        }
    }

    @Test fun timeoutTerminatesProcess() {
        val process = process("read line; while :; do :; done")
        AgyStreamSession({ process }).use { session ->
            assertThrows(Exception::class.java) { session.ask("one", 100) }
            assertTrue(process.waitFor(3, TimeUnit.SECONDS))
        }
    }

    @Test fun cancellationUnblocksPendingAsk() {
        val process = process("read line; while :; do :; done")
        val pool = Executors.newSingleThreadExecutor()
        AgyStreamSession({ process }).use { session ->
            try {
                val pending = pool.submit<Boolean> {
                    try { session.ask("one"); false } catch (_: Exception) { true }
                }
                session.cancel()
                assertTrue(pending.get(3, TimeUnit.SECONDS))
            } finally { pool.shutdownNow(); process.destroy() }
        }
    }
}
