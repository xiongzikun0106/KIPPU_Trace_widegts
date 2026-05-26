package com.kippu.trace

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // 测试应用的 Context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.kippu.trace", appContext.packageName)
    }
}