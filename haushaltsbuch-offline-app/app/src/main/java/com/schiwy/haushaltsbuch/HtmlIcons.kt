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
    "fixed-house" to listOf("M3 11.5 12 4l9 7.5","M5.5 10v9.5h13V10"),
    "fixed-bolt" to listOf("M13 2 4 14h6l-1 8 9-12h-6l1-8z"),
    "fixed-drop" to listOf("M12 3c4 5 6 8 6 11a6 6 0 1 1-12 0c0-3 2-6 6-11z"),
    "fixed-flame" to listOf("M12 3c2 3-2.5 5-1.5 8.5A3.5 3.5 0 0 0 12 18a3.5 3.5 0 0 0 3.4-4.4C15 12 16 12.5 16 14a4 4 0 0 1-8 0c0-4 2-3 4-11z"),
    "fixed-wifi" to listOf("M5 13a10 10 0 0 1 14 0","M8.5 16.5a5.5 5.5 0 0 1 7 0","M11.0 19.2a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0z"),
    "fixed-phone" to listOf("M9.2 2.0h5.6a2.2 2.2 0 0 1 2.2 2.2v15.6a2.2 2.2 0 0 1 -2.2 2.2h-5.6a2.2 2.2 0 0 1 -2.2 -2.2v-15.6a2.2 2.2 0 0 1 2.2 -2.2z","M11 18h2"),
    "fixed-shield" to listOf("M12 3 20 6.5v5C20 16.5 16.5 20 12 21 7.5 20 4 16.5 4 11.5v-5L12 3z"),
    "fixed-backpack" to listOf("M8.2 8.0h7.6a2.2 2.2 0 0 1 2.2 2.2v8.6a2.2 2.2 0 0 1 -2.2 2.2h-7.6a2.2 2.2 0 0 1 -2.2 -2.2v-8.6a2.2 2.2 0 0 1 2.2 -2.2z","M9 8V6a3 3 0 0 1 6 0v2","M9.5 12.5h5"),
    "fixed-ballerina" to listOf("M10.1 4.6a1.9 1.9 0 1 0 3.8 0a1.9 1.9 0 1 0 -3.8 0z","M8.2 14.5 12 8l3.8 6.5z","M12 14.5 9 21","M12 14.5 16.5 20","M9 8.5C7 6.8 5.3 6.6 4 7.3","M15 8.5c2-1.7 3.7-1.9 5-1.2"),
    "fixed-dumbbell" to listOf("M2.5 9.5h1.0a1.0 1.0 0 0 1 1.0 1.0v3.0a1.0 1.0 0 0 1 -1.0 1.0h-1.0a1.0 1.0 0 0 1 -1.0 -1.0v-3.0a1.0 1.0 0 0 1 1.0 -1.0z","M20.5 9.5h1.0a1.0 1.0 0 0 1 1.0 1.0v3.0a1.0 1.0 0 0 1 -1.0 1.0h-1.0a1.0 1.0 0 0 1 -1.0 -1.0v-3.0a1.0 1.0 0 0 1 1.0 -1.0z","M7.5 7.5h0.20000000000000018a1.0 1.0 0 0 1 1.0 1.0v7.0a1.0 1.0 0 0 1 -1.0 1.0h-0.20000000000000018a1.0 1.0 0 0 1 -1.0 -1.0v-7.0a1.0 1.0 0 0 1 1.0 -1.0z","M16.3 7.5h0.20000000000000018a1.0 1.0 0 0 1 1.0 1.0v7.0a1.0 1.0 0 0 1 -1.0 1.0h-0.20000000000000018a1.0 1.0 0 0 1 -1.0 -1.0v-7.0a1.0 1.0 0 0 1 1.0 -1.0z","M8.7 12h6.6"),
    "fixed-music" to listOf("M9 18V5l12-2v13","M3.0 18.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0z","M15.0 16.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0z"),
    "fixed-film" to listOf("M5.2 4.0h13.6a2.2 2.2 0 0 1 2.2 2.2v11.6a2.2 2.2 0 0 1 -2.2 2.2h-13.6a2.2 2.2 0 0 1 -2.2 -2.2v-11.6a2.2 2.2 0 0 1 2.2 -2.2z","M9.5 8.5v7l5.5-3.5-5.5-3.5z"),
    "fixed-car" to listOf("M3.5 13 5 8.3A2 2 0 0 1 6.9 7h10.2a2 2 0 0 1 1.9 1.3L20.5 13","M3.6 13.0h16.8a1.6 1.6 0 0 1 1.6 1.6v1.7999999999999998a1.6 1.6 0 0 1 -1.6 1.6h-16.8a1.6 1.6 0 0 1 -1.6 -1.6v-1.7999999999999998a1.6 1.6 0 0 1 1.6 -1.6z","M5.6 18.2a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z","M15.6 18.2a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z"),
    "fixed-fuel" to listOf("M4.5 7.0h7.0a1.5 1.5 0 0 1 1.5 1.5v11.0a1.5 1.5 0 0 1 -1.5 1.5h-7.0a1.5 1.5 0 0 1 -1.5 -1.5v-11.0a1.5 1.5 0 0 1 1.5 -1.5z","M6 7V4a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v3","M6.5 12h5","M13 11h2.3l3 3.1v4.4a1.5 1.5 0 0 1-3 0v-2.2"),
    "fixed-card" to listOf("M4.5 5.0h15.0a2.5 2.5 0 0 1 2.5 2.5v9.0a2.5 2.5 0 0 1 -2.5 2.5h-15.0a2.5 2.5 0 0 1 -2.5 -2.5v-9.0a2.5 2.5 0 0 1 2.5 -2.5z","M2 10h20"),
    "fixed-tag" to listOf("M6 3h9l3 3v15l-3-2-3 2-3-2-3 2V3z","M9 8.5h6M9 12.5h6"),
    "fixed-auto" to listOf("M20 11A8 8 0 1 0 12 20","M20 4v7h-7"),
    "fixed-piggy" to listOf("M4.5 13.5c0-3.4 3.3-5.8 7.5-5.8s7.5 2.4 7.5 5.8c0 2.6-1.9 4.7-4.5 5.5V21l-2-1.3-2 1.3v-1.5c-.7.1-1.4.1-2 0","M9 8.2 7.5 6l2 .4","M14.4 12.2a0.9 0.9 0 1 0 1.8 0a0.9 0.9 0 1 0 -1.8 0z","M8.5 10h3"),
    "fixed-food" to listOf("M6 2v6a1.5 1.5 0 0 0 3 0V2","M7.5 8v14","M17 2c1.8 1 2.4 3 2.1 6.1-.2 2-1.6 2.9-2.6 2.9","M16.5 2v20"),
    "fixed-shirt" to listOf("M8 4 4 7l2 3 2-1v11h8V9l2 1 2-3-4-3-2 2h-4z"),
    "fixed-ticket" to listOf("M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8z","M14 6v1.2M14 9v1.6M14 13v1.6M14 16.8V18"),
    "fixed-heart" to listOf("M12 21s-7-4.4-9.4-8.9A5 5 0 0 1 12 6a5 5 0 0 1 9.4 6.1C19 16.6 12 21 12 21z"),
    "fixed-gift" to listOf("M5.5 9.0h13.0a1.5 1.5 0 0 1 1.5 1.5v8.0a1.5 1.5 0 0 1 -1.5 1.5h-13.0a1.5 1.5 0 0 1 -1.5 -1.5v-8.0a1.5 1.5 0 0 1 1.5 -1.5z","M4 12.5h16","M12 9v11","M9 9c-1.5 0-2.5-1-2.5-2S8 5.5 9 6.5 12 9 12 9 10.5 9 9 9z","M15 9c1.5 0 2.5-1 2.5-2S16 5.5 15 6.5 12 9 12 9s1.5 0 3 0z"),
    "fixed-suitcase" to listOf("M6.0 7.0h12.0a2.0 2.0 0 0 1 2.0 2.0v8.0a2.0 2.0 0 0 1 -2.0 2.0h-12.0a2.0 2.0 0 0 1 -2.0 -2.0v-8.0a2.0 2.0 0 0 1 2.0 -2.0z","M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2","M4 12h16"),
    "fixed-paw" to listOf("M5.4 10.2a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z","M10.4 8.0a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z","M15.4 10.2a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z","M12 12c-3 0-5 2-5 4.5S8.5 21 12 21s5-2 5-4.5-2-4.5-5-4.5z"),
    "action-edit" to listOf("M12 20h9","M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"),
    "action-delete" to listOf("M3 6h18","M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2","M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6","M10 11v6M14 11v6"),
    "chevron-up" to listOf("M6 15l6-6 6 6"),
    "chevron-down" to listOf("M6 9l6 6 6-6"),
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

private val htmlFilledIconPaths=mapOf("fixed-bolt" to setOf(0),"fixed-wifi" to setOf(2),"fixed-ballerina" to setOf(0),"fixed-film" to setOf(1),"fixed-car" to setOf(2,3),"fixed-piggy" to setOf(2),"fixed-paw" to setOf(0,1,2))

@Composable internal fun HtmlReferenceIcon(name:String,color:Color,size:Dp,strokeWidth:Float=2f){
    val paths=remember(name){htmlIconPaths[name].orEmpty().map{PathParser().parsePathString(it).toPath()}}
    Canvas(Modifier.size(size)){
        val scaleX=this.size.width/24f;val scaleY=this.size.height/24f
        withTransform({scale(scaleX,scaleY,pivot=Offset.Zero)}){
            paths.forEachIndexed{index,path->if(index in htmlFilledIconPaths[name].orEmpty())drawPath(path,color) else drawPath(path,color,style=Stroke(strokeWidth,cap=StrokeCap.Round,join=StrokeJoin.Round))}
        }
    }
}

internal val referenceIconColors=mapOf("auto" to Color(0xFF8E8E93),"house" to Color(0xFFF5A623),"bolt" to Color(0xFFFFCC00),"drop" to Color(0xFF0A84FF),"flame" to Color(0xFFFF6B35),"wifi" to Color(0xFFAF52DE),"phone" to Color(0xFF30B0C7),"shield" to Color(0xFF5E5CE6),"backpack" to Color(0xFFFF375F),"ballerina" to Color(0xFFFF2D78),"dumbbell" to Color(0xFFFF9500),"music" to Color(0xFFBF5AF2),"film" to Color(0xFFE63946),"car" to Color(0xFF64B5F6),"fuel" to Color(0xFFD97706),"card" to Color(0xFF30D158),"piggy" to Color(0xFFFF6482),"food" to Color(0xFFFF9F0A),"shirt" to Color(0xFF64D2FF),"ticket" to Color(0xFFAC8FFF),"heart" to Color(0xFFFF453A),"gift" to Color(0xFFFF375F),"suitcase" to Color(0xFFAC8E68),"paw" to Color(0xFFA2845E),"tag" to Color(0xFF8E8E93))
internal val referenceIconLabels=mapOf("auto" to "Automatisch","house" to "Haus","bolt" to "Strom","drop" to "Wasser","flame" to "Gas","wifi" to "Internet","phone" to "Telefon","shield" to "Versicherung","backpack" to "Kita/Schule","ballerina" to "Tanz","dumbbell" to "Fitness","music" to "Musik","film" to "Streaming","car" to "Auto","fuel" to "Tanken","card" to "Bank/Kredit","piggy" to "Sparen","food" to "Essen","shirt" to "Kleidung","ticket" to "Freizeit","heart" to "Gesundheit","gift" to "Geschenk","suitcase" to "Reisen","paw" to "Haustier","tag" to "Sonstiges")
