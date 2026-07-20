package com.stealthcrypt.app

import android.graphics.Bitmap
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun `generateQrCode handles empty input`() {
        val result = generateQrCode("")
        assertNull(result)
    }

    @Test
    fun `generateQrCode creates correct bounds for valid input text`() {
        val result = generateQrCode("test_secret_key")
        assertNotNull(result)
        result?.let {
            assertTrue(it.width == 512)
            assertTrue(it.height == 512)
        }
    }
}
