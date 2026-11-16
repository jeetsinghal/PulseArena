package com.example

import com.example.data.FirebaseLiveTranslator
import com.example.data.TranslationLanguage
import com.example.data.security.UrlSecurityValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityValidatorTest {

    @Test
    fun `accepts valid https and http stream urls`() {
        val httpsResult = UrlSecurityValidator.validateStreamUrl("  https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8 ")
        val httpResult = UrlSecurityValidator.validateStreamUrl("http://stream.example.org/live.m3u8")

        assertTrue(httpsResult.isSuccess)
        assertEquals("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", httpsResult.getOrNull())
        assertTrue(httpResult.isSuccess)
    }

    @Test
    fun `blocks non-http schemes and private metadata hosts`() {
        assertTrue(UrlSecurityValidator.validateStreamUrl("file:///etc/passwd").isFailure)
        assertTrue(UrlSecurityValidator.validateStreamUrl("javascript:alert(1)").isFailure)
        assertTrue(UrlSecurityValidator.validateStreamUrl("http://127.0.0.1:8080/live.m3u8").isFailure)
        assertTrue(UrlSecurityValidator.validateStreamUrl("http://169.254.169.254/latest/meta-data").isFailure)
        assertTrue(UrlSecurityValidator.validateStreamUrl("http://localhost/test.m3u8").isFailure)
    }

    @Test
    fun `original translation language passes lines through without network call`() = runTest {
        val translator = FirebaseLiveTranslator()
        val input = listOf("Goal scored in 42nd minute", "IND 142/2 (15.4)")
        val result = translator.translateBatch(input, TranslationLanguage.ORIGINAL)

        assertTrue(result.isSuccess)
        assertEquals(input, result.getOrNull())
    }
}
