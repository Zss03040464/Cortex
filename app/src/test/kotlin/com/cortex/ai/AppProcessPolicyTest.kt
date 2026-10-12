package com.cortex.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProcessPolicyTest {
    @Test
    fun `仅主进程初始化完整 Runtime 依赖`() {
        assertTrue(AppProcessPolicy.shouldInitializeFullRuntime("com.cortex.ai", "com.cortex.ai"))
        assertFalse(AppProcessPolicy.shouldInitializeFullRuntime("com.cortex.ai:voice", "com.cortex.ai"))
        assertFalse(AppProcessPolicy.shouldInitializeFullRuntime("com.cortex.ai:voice_session", "com.cortex.ai"))
    }
}
