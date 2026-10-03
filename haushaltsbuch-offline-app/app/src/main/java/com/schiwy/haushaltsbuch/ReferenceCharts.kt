package com.schiwy.haushaltsbuch

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

internal fun referenceFuelChartPoints(logs:List<JSONObject>,range:String,now:LocalDateTime):List<JSONObject>{
 val days=mapOf("week" to 7L,"month" to 30L,"3months" to 90L,"6months" to 180L)
 val cutoff=if(range=="year")now.toLocalDate().withDayOfYear(1).atStartOfDay() else now.minusDays(days[range]?:30)
 return logs.sortedByDescending{it.optString("date")}.filter{runCatching{!LocalDate.parse(it.optString("date")).atStartOfDay().isBefore(cutoff)}.getOrDefault(false)}.reversed()
}
internal fun referenceFuelChartCoordinates(prices:List<Double>):List<Pair<Float,Float>>{
 if(prices.size<2)return emptyList()
 var min=prices.minOrNull()!!;var max=prices.maxOrNull()!!
 if(min==max){min-=.05;max+=.05}
 val pad=(max-min)*.15;min-=pad;max+=pad
 return prices.mapIndexed{i,v->
  val x=300.0*i/(prices.size-1);val y=100-((v-min)/(max-min))*100
  (referenceRound(x*10)/10).toFloat() to (referenceRound(y*10)/10).toFloat()
 }
}
