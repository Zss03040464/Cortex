package com.cortex.ai

internal object AppProcessPolicy {
    fun shouldInitializeFullRuntime(processName: String, packageName: String): Boolean =
        processName == packageName
}
