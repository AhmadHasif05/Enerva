package com.example.a211198_hasif_drnelson_Project2.view.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CompressReelImageTest {

    // A large, noisy bitmap is the worst case for JPEG (noise does not compress).
    private fun noisyBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rnd = Random(7)
        for (x in 0 until size step 4) {
            for (y in 0 until size step 4) {
                canvas.drawColor(Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
                bmp.setPixel(x, y, Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)))
            }
        }
        return bmp
    }

    @Test
    fun stays_under_cap_for_large_noisy_bitmap() {
        val bytes = compressReelImage(noisyBitmap(2000), maxBytes = 900_000)
        assertNotNull("should produce bytes", bytes)
        assertTrue("under cap: ${bytes!!.size}", bytes.size <= 900_000)
    }

    @Test
    fun small_bitmap_round_trips() {
        val bytes = compressReelImage(noisyBitmap(300))
        assertNotNull(bytes)
        assertTrue(bytes!!.isNotEmpty())
    }
}
