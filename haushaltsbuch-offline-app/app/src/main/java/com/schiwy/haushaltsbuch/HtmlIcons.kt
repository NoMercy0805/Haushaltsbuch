package com.schiwy.haushaltsbuch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

// SVG geometry copied directly from the current HTML reference.
internal val htmlIconPaths=mapOf(
    "beta" to listOf("M2.0 18.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M2 18v-3","M10 18v-3","M2.0 15.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M9.0 20.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M9 20v-4","M17 20v-4","M9.0 16.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M15.0 17.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M15 17v-5","M23 17v-5","M15.0 12.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z"),
    "buchungen" to listOf("M6 2h9l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z","M14 2v5h5","M8 13h8M8 17h5"),
    "variabel" to listOf("M5.0 6.0a7.0 3.0 0 1 0 14.0 0a7.0 3.0 0 1 0 -14.0 0z","M5 6v5c0 1.66 3.13 3 7 3s7-1.34 7-3V6","M5 11v5c0 1.66 3.13 3 7 3s7-1.34 7-3v-5"),
    "kredite" to listOf("M4.5 5.0h15.0a2.5 2.5 0 0 1 2.5 2.5v9.0a2.5 2.5 0 0 1 -2.5 2.5h-15.0a2.5 2.5 0 0 1 -2.5 -2.5v-9.0a2.5 2.5 0 0 1 2.5 -2.5z","M2 10h20"),
    "tanken" to listOf("M4.5 7.0h7.0a1.5 1.5 0 0 1 1.5 1.5v11.0a1.5 1.5 0 0 1 -1.5 1.5h-7.0a1.5 1.5 0 0 1 -1.5 -1.5v-11.0a1.5 1.5 0 0 1 1.5 -1.5z","M6 7V4a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v3","M6.5 12h5","M13 11h2.3l3 3.1v4.4a1.5 1.5 0 0 1-3 0v-2.2"),
    "gear" to listOf("M8.8 12.0a3.2 3.2 0 1 0 6.4 0a3.2 3.2 0 1 0 -6.4 0z","M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"),
    "Rechnungen" to listOf("M14 3v4a1 1 0 0 0 1 1h4","M17 21H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7l5 5v11a2 2 0 0 1-2 2z","M9 14l2 2 4-4"),
    "Einnahmen" to listOf("M12 20V4M6 10l6-6 6 6"),
    "Fixkosten" to listOf("M17 3.6l4 4-4 4","M3 12.6v-1a4 4 0 0 1 4-4h14","M7 20.4l-4-4 4-4","M21 11.4v1a4 4 0 0 1-4 4H3"),
    "Kostenarten" to listOf("M5.0 6.0a7.0 3.0 0 1 0 14.0 0a7.0 3.0 0 1 0 -14.0 0z","M5 6v5c0 1.66 3.13 3 7 3s7-1.34 7-3V6","M5 11v5c0 1.66 3.13 3 7 3s7-1.34 7-3v-5"),
    "Spartopf" to listOf("M3 12a9 9 0 1 0 2.6-6.3L3 8","M3 3v5h5","M12 7v5l4 2"),
    "settings-Darstellung" to listOf("M8.0 12.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0z","M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"),
    "settings-Sichtbare Bereiche" to listOf("M5.0 9.0h14.0a3.0 3.0 0 0 1 3.0 3.0v0.0a3.0 3.0 0 0 1 -3.0 3.0h-14.0a3.0 3.0 0 0 1 -3.0 -3.0v0.0a3.0 3.0 0 0 1 3.0 -3.0z","M9 9v6M15 9v6"),
    "settings-Backup" to listOf("M7 17.5a4.5 4.5 0 0 1-.9-8.9A5.5 5.5 0 0 1 16.7 7 4.5 4.5 0 0 1 17.2 15.9","M12 10.5v8M9 15.5l3 3 3-3"),
    "settings-Kontostand" to listOf("M2.0 18.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M2 18v-3","M10 18v-3","M2.0 15.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M9.0 20.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M9 20v-4","M17 20v-4","M9.0 16.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M15.0 17.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z","M15 17v-5","M23 17v-5","M15.0 12.0a4.0 1.6 0 1 0 8.0 0a4.0 1.6 0 1 0 -8.0 0z"),
    "settings-Zurücksetzen" to listOf("M3 6h18","M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2","M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6","M10 11v6M14 11v6"),
    "settings-Update-Historie" to listOf("M3.0 12.0a9.0 9.0 0 1 0 18.0 0a9.0 9.0 0 1 0 -18.0 0z","M12 7v5l3.5 2"),
    "recurring" to listOf("M17 2.1l4 4-4 4","M3 12.6v-1a4 4 0 0 1 4-4h14","M7 21.9l-4-4 4-4","M21 11.4v1a4 4 0 0 1-4 4H3"),
    "filter-all" to listOf("M8 6h13M8 12h13M8 18h13","M3 6h.01M3 12h.01M3 18h.01"),
    "filter-open" to listOf("M4.0 12.0a8.0 8.0 0 1 0 16.0 0a8.0 8.0 0 1 0 -16.0 0z")
)

@Composable internal fun HtmlReferenceIcon(name:String,color:Color,size:Dp,strokeWidth:Float=2f){
    val paths=remember(name){htmlIconPaths[name].orEmpty().map{PathParser().parsePathString(it).toPath()}}
    Canvas(Modifier.size(size)){
        val scaleX=this.size.width/24f;val scaleY=this.size.height/24f
        withTransform({scale(scaleX,scaleY,pivot=Offset.Zero)}){
            paths.forEach{drawPath(it,color,style=Stroke(strokeWidth,cap=StrokeCap.Round,join=StrokeJoin.Round))}
        }
    }
}
