package com.loggym.ui.theme

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.loggym.R
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LogGymIdentityTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Test fun approvedColorsRemainExactForBothThemes() {
        for (dark in listOf(false, true)) {
            val c = logGymColorScheme(context.resources, dark)
            val actual = listOf(c.background,c.surface,c.onBackground,c.onSurface,c.onSurfaceVariant,c.primary,c.outlineVariant,c.onPrimary).map { it.toArgb().toUInt() }
            val expected = if (dark) listOf(0xFF191B25u,0xFF242735u,0xFFEEEDF6u,0xFFEEEDF6u,0xFFB5B5C7u,0xFFAAB7EBu,0xFF3C4056u,0xFF191B25u)
                else listOf(0xFFF8F7F5u,0xFFFFFFFFu,0xFF292B35u,0xFF292B35u,0xFF646577u,0xFF505F8Cu,0xFFDEDFE5u,0xFFFFFFFFu)
            assertEquals(expected, actual)
        }
    }
    @Test fun functionalTextAndBordersMeetContrastWithoutUsingDecorativeDividers() {
        for (dark in listOf(false,true)) {
            val c = logGymColorScheme(context.resources, dark)
            for (surface in listOf(c.background,c.surface,c.surfaceContainerLow,c.surfaceContainer,c.surfaceContainerHigh,c.surfaceContainerHighest)) {
                assertTrue(contrast(c.onSurface,surface) >= 4.5)
                assertTrue(contrast(c.onSurfaceVariant,surface) >= 4.5)
                assertTrue(contrast(c.outline,surface) >= 3)
            }
            for ((fg,bg) in listOf(c.onPrimary to c.primary,c.onSecondary to c.secondary,c.onPrimaryContainer to c.primaryContainer,
                c.onSecondaryContainer to c.secondaryContainer,c.onError to c.error,c.onErrorContainer to c.errorContainer,c.inverseOnSurface to c.inverseSurface))
                assertTrue("Unreadable pair $fg / $bg",contrast(fg,bg) >= 4.5)
            assertTrue(contrast(c.outlineVariant,c.surface) < 3)
        }
    }
    @Test fun foregroundAndMonochromeKeepApprovedGeometryAndSafeMargins() {
        val ns = "http://schemas.android.com/apk/res/android"
        for (id in listOf(R.drawable.ic_launcher_foreground,R.drawable.ic_launcher_monochrome)) {
            val xml = context.resources.getXml(id)
            var found = false
            while (xml.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == org.xmlpull.v1.XmlPullParser.START_TAG) when(xml.name) {
                    "vector" -> { assertEquals(108f,xml.getAttributeValue(ns,"viewportWidth").toFloat(),0f); assertEquals(108f,xml.getAttributeValue(ns,"viewportHeight").toFloat(),0f) }
                    "group" -> { assertEquals(19.5f,xml.getAttributeValue(ns,"translateX").toFloat(),0f);assertEquals(36f,xml.getAttributeValue(ns,"translateY").toFloat(),0f);assertEquals(0.3f,xml.getAttributeValue(ns,"scaleX").toFloat(),0f);assertEquals(0.3f,xml.getAttributeValue(ns,"scaleY").toFloat(),0f) }
                    "path" -> { found = true; val hash = MessageDigest.getInstance("SHA-256").digest(xml.getAttributeValue(ns,"pathData").toByteArray()).joinToString("") { "%02x".format(it) }
                        assertEquals("0a7f4a410ee117dd4991386f19a34c47862530e823a87df26cd6687debf7ec7f",hash) }
                }
                xml.next()
            }
            xml.close();assertTrue(found)
        }
    }
    @Test @Config(sdk = [26,33]) fun launcherAndRoundResourcesResolveWithSeparateMonochromeOnSupportedApi() {
        val info = context.packageManager.getApplicationInfo(context.packageName,0)
        assertEquals(R.mipmap.ic_launcher,info.icon)
        for (id in listOf(R.drawable.ic_loggym_logo_light,R.drawable.ic_loggym_logo_dark,R.drawable.ic_loggym_symbol)) assertNotNull(context.getDrawable(id))
        for (id in listOf(R.mipmap.ic_launcher,R.mipmap.ic_launcher_round)) {
            val icon = context.getDrawable(id) as AdaptiveIconDrawable
            assertNotNull(icon.background);assertNotNull(icon.foreground)
            if (Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
        }
    }
    private fun contrast(a:Color,b:Color):Double {
        fun luminance(c:Color):Double {
            fun linear(v:Float) = if(v<=0.04045f) v.toDouble()/12.92 else Math.pow((v+0.055)/1.055,2.4)
            return .2126*linear(c.red)+.7152*linear(c.green)+.0722*linear(c.blue)
        }
        val x=luminance(a);val y=luminance(b)
        return (maxOf(x,y)+.05)/(minOf(x,y)+.05)
    }
}
