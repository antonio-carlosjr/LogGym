package com.loggym.e2e

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loggym.R
import com.loggym.ui.components.AdaptiveActionHeader
import com.loggym.ui.components.LogGymLogo
import com.loggym.ui.theme.LogGymTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.*

@RunWith(AndroidJUnit4::class)
class Sprint8BrandTest {
    @get:Rule val compose = createComposeRule()
    @Test fun explicitThemesRenderOriginalLogoColorsAndLargeTextActionsWithoutOverlap() {
        val dark = mutableStateOf(false);val scale = mutableStateOf(1f)
        var scheme: ColorScheme? = null;var hits = 0
        val title = "Divisão com nome longo para preservar leitura e ações"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale.value)) {
                LogGymTheme(darkTheme = dark.value) {
                    val colors = MaterialTheme.colorScheme
                    SideEffect { scheme = colors }
                    Surface {
                        Column(Modifier.width(288.dp).padding(16.dp)) {
                            LogGymLogo()
                            AdaptiveActionHeader(title,192.dp) {
                                repeat(4) { i -> IconButton(onClick = { hits++ },modifier = Modifier.testTag("brand-action-$i").sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text(i.toString()) } }
                            }
                        }
                    }
                }
            }
        }
        for ((night,font) in listOf(false to 1f,true to 1f,false to 2f,true to 2f)) {
            compose.runOnIdle { dark.value = night;scale.value = font }
            val image = compose.onNodeWithTag("loggym-logo").captureToImage().toPixelMap()
            val colors = requireNotNull(scheme)
            assertEquals(if(night) 0xFFAAB7EB.toInt() else 0xFF505F8C.toInt(),colors.primary.toArgb())
            var primary = 0;var text = 0
            for (y in 0 until image.height) for(x in 0 until image.width) {
                if(image[x,y].toArgb()==colors.primary.toArgb()) primary++
                if(image[x,y].toArgb()==colors.onSurface.toArgb()) text++
            }
            assertTrue("Missing approved symbol",primary>10);assertTrue("Missing approved wordmark",text>10)
            val heading = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("brand-action-0").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(heading.bottom <= action.top + 1)
            val target = 48 * ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.density
            assertTrue("Tap width ${action.width}, expected $target",action.width + .01f >= target)
            assertTrue("Tap height ${action.height}, expected $target",action.height + .01f >= target)
        }
        compose.onNodeWithTag("brand-action-0").performClick()
        compose.runOnIdle { assertEquals(1,hits) }
    }

    @Test fun originalForegroundFitsMasksAtSmallSizesAndMonochromeKeepsItsSilhouette() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fun layer(id:Int):Bitmap {
            val bitmap=Bitmap.createBitmap(108,108,Bitmap.Config.ARGB_8888)
            requireNotNull(context.getDrawable(id)).apply { setBounds(0,0,108,108);draw(Canvas(bitmap)) }
            return bitmap
        }
        val fg=layer(R.drawable.ic_launcher_foreground);val mono=layer(R.drawable.ic_launcher_monochrome)
        if(InstrumentationRegistry.getArguments().getString("evidencePhase")!=null) {
            val dir=File(context.getExternalFilesDir(null),"s8-evidence/after/launcher").apply { mkdirs() }
            File(dir,"foreground.png").outputStream().use { fg.compress(Bitmap.CompressFormat.PNG,100,it) }
            File(dir,"monochrome.png").outputStream().use { mono.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
        var ink=0
        for(y in 0..107) for(x in 0..107) {
            val visible=android.graphics.Color.alpha(fg.getPixel(x,y))>0
            // Different RGB fills can quantize antialiased edge alpha by one 8-bit step.
            val delta = abs(android.graphics.Color.alpha(fg.getPixel(x,y))-android.graphics.Color.alpha(mono.getPixel(x,y)))
            assertTrue("Alpha differs by $delta at $x,$y",delta <= 1)
            if(visible) { ink++;assertTrue("Ink outside approved safe circle",hypot(x+.5-54,y+.5-54)<=33.5) }
        }
        assertTrue(ink>0)
        val preview=Bitmap.createBitmap(480,360,Bitmap.Config.ARGB_8888);val canvas=Canvas(preview)
        canvas.drawColor(0xFFF8F7F5.toInt())
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=0xFF292B35.toInt();textSize=12f }
        val sizes=listOf(24,32,48,64)
        for(row in 0..2) for((col,size) in sizes.withIndex()) {
            val x=col*120f+(120-size)/2f;val y=row*120f+20
            val rect=RectF(x,y,x+size,y+size);val mask=Path()
            when(row) {
                0->mask.addCircle(rect.centerX(),rect.centerY(),size/2f,Path.Direction.CW)
                1->{ for(i in 0..128) {
                    val a=i*2*PI/128;val px=rect.centerX()+size/2*sign(cos(a))*sqrt(abs(cos(a)))
                    val py=rect.centerY()+size/2*sign(sin(a))*sqrt(abs(sin(a)))
                    if(i==0)mask.moveTo(px.toFloat(),py.toFloat()) else mask.lineTo(px.toFloat(),py.toFloat())
                };mask.close() }
                else->mask.addRoundRect(rect,size*.22f,size*.22f,Path.Direction.CW)
            }
            canvas.save();canvas.clipPath(mask);canvas.drawColor(0xFF505F8C.toInt())
            canvas.drawBitmap(fg,null,rect,null);canvas.restore()
            canvas.drawText("${listOf("circle","squircle","rounded")[row]} ${size}px",col*120f+8,row*120f+106,paint)
        }
        if(InstrumentationRegistry.getArguments().getString("evidencePhase")!=null) {
            val dir=File(context.getExternalFilesDir(null),"s8-evidence/after/launcher").apply { mkdirs() }
            File(dir,"masks-and-sizes.png").outputStream().use { assertTrue(preview.compress(Bitmap.CompressFormat.PNG,100,it)) }
            File(dir,"monochrome.png").outputStream().use { assertTrue(mono.compress(Bitmap.CompressFormat.PNG,100,it)) }
        }
        fg.recycle();mono.recycle();preview.recycle()
    }
}
