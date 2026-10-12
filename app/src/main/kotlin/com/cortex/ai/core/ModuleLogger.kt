package com.cortex.ai.core

import android.util.Log
import io.github.libxposed.api.XposedModule

internal class ModuleLogger private constructor(
    private val module: XposedModule,
    private val scope: String?,
    private val logThrottle: LogThrottle
) : AgentLogger {

    constructor(module: XposedModule) : this(module, null, LogThrottle())

    fun scoped(childScope: String): ModuleLogger {
        require(childScope.isNotBlank()) { "日志作用域不能为空" }
        val combinedScope = scope?.let { "$it/$childScope" } ?: childScope
        return ModuleLogger(module, combinedScope, logThrottle)
    }

    override fun debug(message: () -> String) {
        val formatted = format(message())
        Log.d(ModuleConfig.TAG, formatted)
        module.log(Log.DEBUG, ModuleConfig.TAG, formatted)
    }

    override fun info(message: String) {
        val formatted = format(message)
        Log.i(ModuleConfig.TAG, formatted)
        module.log(Log.INFO, ModuleConfig.TAG, formatted)
    }

    override fun warn(message: String) {
        val formatted = format(message)
        Log.w(ModuleConfig.TAG, formatted)
        module.log(Log.WARN, ModuleConfig.TAG, formatted)
    }

    fun warnThrottled(
        key: String,
        windowMs: Long = ModuleConfig.HOT_PATH_LOG_WINDOW_MS,
        message: () -> String
    ) {
        if (logThrottle.shouldLog(scopedThrottleKey("warn", key), windowMs)) {
            warn(message())
        }
    }

    override fun error(message: String, throwable: Throwable?) {
        val formatted = format(message)
        if (throwable == null) {
            Log.e(ModuleConfig.TAG, formatted)
            module.log(Log.ERROR, ModuleConfig.TAG, formatted)
        } else {
            Log.e(ModuleConfig.TAG, formatted, throwable)
            module.log(Log.ERROR, ModuleConfig.TAG, formatted, throwable)
        }
    }

    fun errorThrottled(
        key: String,
        throwable: Throwable? = null,
        windowMs: Long = ModuleConfig.HOT_PATH_LOG_WINDOW_MS,
        message: () -> String
    ) {
        if (!logThrottle.shouldLog(scopedThrottleKey("error", key), windowMs)) return
        error(message(), throwable)
    }

    private fun scopedThrottleKey(level: String, key: String): String =
        scope?.let { "$level:$it:$key" } ?: "$level:$key"

    private fun format(message: String): String =
        scope?.let { "[$it] $message" } ?: message
}
