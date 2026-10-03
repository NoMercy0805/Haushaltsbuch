package com.schiwy.haushaltsbuch

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ReferenceChartsTest {
 @Test fun rollingRangeKeepsTheHtmlTimeOfDayCutoff(){
  val logs=listOf("2026-09-26","2026-09-27","2026-10-03").map{JSONObject().put("date",it)}
  val points=referenceFuelChartPoints(logs,"week",LocalDateTime.of(2026,10,3,12,0))
  assertEquals(listOf("2026-09-27","2026-10-03"),points.map{it.getString("date")})
 }
 @Test fun yearRangeStartsAtMidnightOnJanuaryFirst(){
  val logs=listOf("2025-12-31","2026-01-01").map{JSONObject().put("date",it)}
  assertEquals("2026-01-01",referenceFuelChartPoints(logs,"year",LocalDateTime.of(2026,10,3,12,0)).single().getString("date"))
 }
 @Test fun chartCoordinatesUseTheHtmlViewBoxAndOneDecimalPrecision(){
  val points=referenceFuelChartCoordinates(listOf(1.0,2.0,1.5))
  assertEquals(listOf(0f to 88.5f,150f to 11.5f,300f to 50f),points)
  assertEquals(listOf(0f to 50f,300f to 50f),referenceFuelChartCoordinates(listOf(1.5,1.5)))
 }
 @Test fun sameDayPointOrderMatchesTheStableHtmlDescendingThenReverseOrder(){
  val logs=listOf(JSONObject().put("id","first").put("date","2026-10-03"),JSONObject().put("id","second").put("date","2026-10-03"))
  assertEquals(listOf("second","first"),referenceFuelChartPoints(logs,"week",LocalDateTime.of(2026,10,3,12,0)).map{it.getString("id")})
 }
}
