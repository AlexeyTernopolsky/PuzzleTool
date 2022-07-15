package com.alextern.puzzletool

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.InputStream


@SmallTest
class AnalyzerTest {

    @Test
    fun bombs1() {
        val str = parseBitmap("test1.png")
        assertEquals(str, "Vn Rn Vb Vn Bn Bn Rn \n" +
                "Rn Gn Vn Bn Gn Vn Rn \n" +
                "Bn Rn Bn Gn Bn Vn Bn \n" +
                "Bn Rn Rb Bn Gn Bn Vn \n" +
                "Yn Bn Vn Yn Bn Gn Bn \n" +
                "Rn Gn Rn Vn Bn Rn Vn \n" +
                "Vn Bn Vn Bn Yn Vn Rn \n")
    }

    @Test
    fun bombs2() {
        val str = parseBitmap("test2.png")
        assertEquals(str, "Gn Yn Yn Vn Yn Gn Gn \n" +
                "Rn Vn Rn Gn Vn Gn Vn \n" +
                "Vn Vn Bn Vn Yb Yn Rn \n" +
                "Gb Rn Vn Vn Gn Bn Rn \n" +
                "Rn Rn Gn Gn Vn Bn Vn \n" +
                "Bn Bn Yn Vn Yn Yn Rn \n" +
                "Rb Yn Yn Vn Yn Gn Rn \n")
    }

    @Test
    fun bombs3() {
        val str = parseBitmap("test3.png")
        assertEquals(str, "Rn Bn Gn Yn Bn Yn Gn \n" +
                "Yn Bn Gn Vn Bn Vn Vn \n" +
                "Vn Vn Rn Gn Vg Gn Gn \n" +
                "Bn Yn Bn Yn Bn Vn Bn \n" +
                "Gn Rn Gn Gn Rn Vn Rn \n" +
                "Gn Yn Bn Yn Yn Gn Bn \n" +
                "Rn Yn Rn Rn Bn Vn Rn \n")
    }

    @Test
    fun bombs4() {
        val str = parseBitmap("test4.png")
        assertEquals(str, "Bn Bb Gn Bn Gn Rn Rn \n" +
                "Vn Yn Gn Rn Yn Bn Bn \n" +
                "Bn Gn Yn Vn Rn Bn Yn \n" +
                "Vn Gn Rn Yb Bn Gn Vn \n" +
                "Vn Bn Gn Rn Vn Vn Bn \n" +
                "Bn Yn Yn Bn Gn Rn Rn \n" +
                "Vn Bn Yn Bn Bn Vn Bn \n")
    }

    private fun parseBitmap(fileName: String): String {
        val bitmap = getBitmapFromTestAssets(fileName)
        if (bitmap != null) {
            val converter = BitmapToPuzzleConverter(bitmap, ConverterType.kPuzzleDuel)
            val puzzle = converter.analyze()
           return puzzle.toString()
        } else {
            fail("Fail to encode bitmap")
        }
        return ""
    }

    private fun getBitmapFromTestAssets(fileName: String): Bitmap? {
        val testContext: Context =getInstrumentation().context
        val assetManager: AssetManager = testContext.assets
        val testInput: InputStream = assetManager.open(fileName)
        return BitmapFactory.decodeStream(testInput)
    }
}
