package com.schiwy.haushaltsbuch

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Expected values come from the current index.html, including its month-end rules. */
class HtmlParityTest {
    private fun booking(id:String,date:String,amount:Double,type:String="ausgabe",title:String=id)=JSONObject()
        .put("id",id).put("date",date).put("amount",amount).put("type",type).put("title",title)

    @Test fun widgetExcludesFutureMonthsButIncludesTheEntireCurrentMonth(){
        val entries=listOf(booking("old","2026-09-01",100.0,"einnahme"),booking("expense","2026-10-03",30.0),booking("end","2026-10-31",20.0,"einnahme"),booking("future","2026-11-01",500.0,"einnahme"))
        assertEquals(90.0,betaBalanceAt(entries,LocalDate.parse("2026-10-31")),.0001)
    }
    @Test fun oldNativeAliasesHaveTheSameSigns(){
        assertEquals(7.0,betaBalanceAt(listOf(booking("a","2026-10-01",10.0,"income"),booking("b","2026-10-01",3.0,"expense")),LocalDate.parse("2026-10-31")),.0001)
    }
    @Test fun monthTotalsAndRunningBalanceRemainIndependent(){
        val entries=listOf(booking("old","2026-09-01",100.0,"einnahme"),booking("a","2026-10-01",50.0,"einnahme").put("reconciled",true),booking("b","2026-10-02",30.0),booking("future","2026-11-01",1000.0,"einnahme"))
        val data=betaMonthData(entries,YearMonth.of(2026,10))
        assertEquals(50.0,data.income,.0001);assertEquals(30.0,data.expense,.0001);assertEquals(120.0,data.balance,.0001)
        assertEquals(2,data.entries.size);assertEquals(1,data.unreconciled)
    }
    @Test fun hiddenTitlesAreStringsAndSuggestionsAreNewestFirst(){
        val state=JSONObject().put("betaEntries",JSONArray().put(booking("1","2026-01-01",1.0,title="Alt")).put(booking("2","2026-10-01",1.0,title="Neu")).put(booking("3","2026-09-01",1.0,title="Alt")).put(booking("4","2026-10-02",1.0,title="O2"))).put("betaHiddenTitles",JSONArray().put("O2"))
        assertEquals(listOf("Neu","Alt"),betaKnownTitles(state))
        assertEquals(listOf("O2"),state.getJSONArray("betaHiddenTitles").strings())
    }
    @Test fun centInputMatchesBankingInputIncludingZeroAndGrouping(){
        assertEquals("",centInput(""));assertEquals("0,00",centInput("0"));assertEquals("0,01",centInput("1"))
        assertEquals("1.234,56",centInput("123456"));assertEquals("2.345.678,90",centInput("1234567890"))
        assertEquals("1,679",milliInput("1679"));assertEquals("0,000",milliInput("000"))
    }
    @Test fun amountsAcceptBothGermanAndDecimalNotation(){
        assertEquals(1234.56,parseNativeAmount("1.234,56")!!,.0001)
        assertEquals(1234.56,parseNativeAmount("1,234.56")!!,.0001)
        assertEquals(15000.0,parseNativeAmount("15.000")!!,.0001)
        assertEquals(1.679,parseNativeAmount("1,679")!!,.0001)
        assertEquals(12.5,parseNativeAmount("12.50")!!,.0001)
        assertNull(parseNativeAmount(""));assertNull(parseNativeAmount("NaN"))
    }
    @Test fun recurringPeriodsMatchHtml(){
        val start=LocalDate.parse("2026-01-31")
        val expected=mapOf("weekly" to "2026-02-07","biweekly" to "2026-02-14","4weekly" to "2026-02-28","monthly" to "2026-02-28","2monthly" to "2026-03-31","3monthly" to "2026-04-30","6monthly" to "2026-07-31","yearly" to "2027-01-31","unknown" to "2026-03-02")
        expected.forEach{(period,date)->assertEquals(period,LocalDate.parse(date),betaAddPeriod(start,period))}
        assertEquals(LocalDate.parse("2025-02-28"),betaAddPeriod(LocalDate.parse("2024-02-29"),"yearly"))
    }
    @Test fun recurrenceHorizonUsesJavascriptMonthOverflow(){
        assertEquals(LocalDate.parse("2026-10-01"),betaGenerationHorizon(LocalDate.parse("2026-07-31"),YearMonth.of(2026,7)))
        assertEquals(LocalDate.parse("2027-03-03"),betaGenerationHorizon(LocalDate.parse("2026-12-31"),YearMonth.of(2026,12)))
        assertEquals(LocalDate.parse("2027-12-31"),betaGenerationHorizon(LocalDate.parse("2026-10-03"),YearMonth.of(2027,12)))
    }
    @Test fun generationIsIdempotentAndRespectsEndDatesAndExistingOccurrences(){
        val template=booking("template","2026-01-31",50.0,"einnahme").put("isRecurringTemplate",true).put("recurrencePeriod","monthly").put("recurrenceEnd","2026-03-28").put("recurring",true)
        val existing=booking("existing","2026-02-28",75.0,"einnahme").put("recurringGroupId","template").put("reconciled",true)
        val entries=JSONArray().put(template).put(existing)
        assertTrue(generateBetaOccurrences(entries,YearMonth.of(2026,10),LocalDate.parse("2026-10-03")))
        assertEquals(listOf("2026-01-31","2026-02-28","2026-03-28"),entries.objects().map{it.getString("date")})
        assertEquals(75.0,existing.getDouble("amount"),.0001);assertTrue(existing.getBoolean("reconciled"))
        assertFalse(generateBetaOccurrences(entries,YearMonth.of(2026,10),LocalDate.parse("2026-10-03")))
        assertEquals(3,entries.length())
    }
    @Test fun deletingAFollowingSeriesCannotRegenerateBeyondItsEnd(){
        val entries=JSONArray().put(booking("template","2026-01-01",10.0).put("isRecurringTemplate",true).put("recurrencePeriod","monthly").put("recurrenceEnd","2026-01-31"))
        assertFalse(generateBetaOccurrences(entries,YearMonth.of(2026,10),LocalDate.parse("2026-10-03")))
        assertEquals(1,entries.length())
    }
    @Test fun csvRestoresQuotedCommasNewlinesAndQuotes(){
        val csv="# TABLE:income\nid,name,amount\n1,\"Gehalt, Bonus\n\"\"Sonderzahlung\"\"\",1234.56\n\n# TABLE:betaEntries\nid,type,title,date,amount,recurring,reconciled\n2,einnahme,Gehalt,2026-10-01,1234.56,true,1"
        val state=parseHouseholdBackup(csv)
        assertEquals("Gehalt, Bonus\n\"Sonderzahlung\"",state.getJSONArray("income").getJSONObject(0).getString("name"))
        val beta=state.getJSONArray("betaEntries").getJSONObject(0)
        assertTrue(beta.getBoolean("recurring"));assertTrue(beta.getBoolean("reconciled"))
    }
    @Test fun legacyJsonBackupsKeepHiddenTitlesAndNormalizeNativeTypes(){
        val state=parseHouseholdBackup("""{"betaEntries":[{"id":"1","type":"income","title":"Gehalt","date":"2026-10-01","amount":100}],"betaHiddenTitles":["O2"],"variableCosts":[{"id":"v","name":"Strom"}],"potCarryover":42} """)
        assertEquals("einnahme",state.getJSONArray("betaEntries").getJSONObject(0).getString("type"))
        assertEquals("O2",state.getJSONArray("betaHiddenTitles").getString(0))
        assertEquals("jaehrlich",state.getJSONArray("variableCosts").getJSONObject(0).getString("interval"))
        assertEquals(1,state.getJSONArray("variableCosts").getJSONObject(0).getInt("dueMonth"))
        assertEquals(42.0,state.getDouble("potCarryover"),.0001)
    }
    @Test fun csvLoanDefaultsMatchHtmlInsteadOfInventingZeroInterestFields(){
        val state=parseHouseholdBackup("# TABLE:loans\nid,name,originalAmount,remainingBalance,monthlyPayment,totalTermMonths,carryoverPct,totalWithInterest,monthlyRate,lastReconciledBalance\nl,Kredit,1000,500,100,,,,,")
        val loan=state.getJSONArray("loans").getJSONObject(0)
        assertEquals(100,loan.getInt("carryoverPct"));assertTrue(loan.isNull("monthlyRate"));assertTrue(loan.isNull("totalWithInterest"));assertTrue(loan.isNull("lastReconciledBalance"))
    }
    @Test fun csvRetainsAppSettings(){
        val state=parseHouseholdBackup("# TABLE:appSettings\ntabVisibilityJson,tilgungExtra,tilgungCurrentMonth\n\"{\"\"beta\"\":true,\"\"tanken\"\":false}\",150,2026-11")
        assertFalse(state.getJSONObject("tabVisibility").getBoolean("tanken"));assertEquals(150.0,state.getDouble("tilgungExtra"),.0001);assertEquals("2026-11",state.getString("tilgungCurrentMonth"))
    }
    @Test(expected=IllegalArgumentException::class) fun unrelatedTextIsNotABackup(){parseHouseholdBackup("keine Sicherung")}
    @Test(timeout=15000) fun largeImportedDatabaseRetainsCorrectBalancesAndDoesNotDuplicateSeries(){
        val entries=JSONArray()
        repeat(50000){entries.put(booking("entry-$it","2026-10-01",1.0))}
        repeat(200){entries.put(booking("template-$it","2026-01-31",10.0,"einnahme").put("isRecurringTemplate",true).put("recurrencePeriod","monthly"))}
        val start=System.nanoTime()
        assertTrue(generateBetaOccurrences(entries,YearMonth.of(2026,10),LocalDate.parse("2026-10-03")))
        val count=entries.length()
        assertFalse(generateBetaOccurrences(entries,YearMonth.of(2026,10),LocalDate.parse("2026-10-03")))
        assertEquals(count,entries.length())
        val data=betaMonthData(entries.objects(),YearMonth.of(2026,10))
        assertEquals(50000.0,data.expense,.0001);assertEquals(2000.0,data.income,.0001);assertEquals(-30000.0,data.balance,.0001)
        println("50,000 bookings / 200 templates: "+((System.nanoTime()-start)/1000000)+" ms for generation twice and month totals")
    }
}
