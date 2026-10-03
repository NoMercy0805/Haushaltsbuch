package com.schiwy.haushaltsbuch

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LoanParityTest {
 @Test fun deletingInterestBearingPaymentRestoresOnlyItsPrincipal(){
  val loan=JSONObject().put("originalAmount",1000).put("remainingBalance",910)
  val payment=JSONObject().put("id","p").put("type","payment").put("amount",100).put("principalReduction",90)
  val unrelated=JSONObject().put("id","other")
  val state=JSONObject().put("loanPayments",JSONArray().put(payment).put(unrelated))
  removeReferenceLoanPayment(state,loan,payment)
  assertEquals(1000.0,loan.getDouble("remainingBalance"),0.0001)
  assertEquals("other",state.getJSONArray("loanPayments").getJSONObject(0).getString("id"))
 }
 @Test fun legacyPaymentWithoutPrincipalUsesTheFullAmount(){
  val loan=JSONObject().put("remainingBalance",20.12)
  removeReferenceLoanPayment(JSONObject(),loan,JSONObject().put("id","p").put("type","payment").put("amount",30.34))
  assertEquals(50.46,loan.getDouble("remainingBalance"),0.0001)
 }
 @Test fun reversingAnIncreaseClampsBothBalancesAtZero(){
  val loan=JSONObject().put("originalAmount",50).put("remainingBalance",20)
  removeReferenceLoanPayment(JSONObject(),loan,JSONObject().put("id","p").put("type","increase").put("amount",100))
  assertEquals(0.0,loan.getDouble("remainingBalance"),0.0001)
  assertEquals(0.0,loan.getDouble("originalAmount"),0.0001)
 }
 @Test fun earlyPayoffRecordsTheActualRemainingBalanceAndRetainsExistingDate(){
  val loan=JSONObject().put("id","loan").put("remainingBalance",234.56).put("paidOffDate","2026-09-20")
  val state=JSONObject()
  payoffReferenceLoan(state,loan,"payoff","2026-10-03")
  val payment=state.getJSONArray("loanPayments").getJSONObject(0)
  assertEquals(234.56,payment.getDouble("amount"),0.0001)
  assertEquals("loan",payment.getString("loanId"))
  assertEquals("2026-10-03",payment.getString("date"))
  assertFalse(payment.has("principalReduction"))
  assertEquals("2026-09-20",loan.getString("paidOffDate"))
  assertEquals(0.0,loan.getDouble("remainingBalance"),0.0001)
 }
 @Test fun alreadyPaidLoanDoesNotCreateAZeroPayment(){
  val loan=JSONObject().put("id","loan").put("remainingBalance",0).put("paidOffDate",JSONObject.NULL)
  val state=JSONObject()
  payoffReferenceLoan(state,loan,"payoff","2026-10-03")
  assertFalse(state.has("loanPayments"))
  assertEquals("2026-10-03",loan.getString("paidOffDate"))
 }

 @Test fun currencyRoundingMatchesJavascriptAtHalfCent(){
  assertEquals(0.13,referenceCents(0.125),0.000001)
  assertEquals(-0.12,referenceCents(-0.125),0.000001)
 }
 @Test fun snowballKeepsDistinctLoanIdsWithIdenticalNamesAndUsesTheSelectedMonth(){
  val small=JSONObject().put("id","small").put("name","Gleich").put("remainingBalance",50).put("monthlyPayment",50).put("carryoverPct",100)
  val large=JSONObject().put("id","large").put("name","Gleich").put("remainingBalance",250).put("monthlyPayment",50).put("carryoverPct",100)
  val plan=makePayoffPlan(listOf(small,large),0.0,baseMonth=java.time.LocalDate.of(2027,2,1))
  assertEquals(3,plan.size)
  assertEquals("Feb 2027",plan.first().label)
  assertEquals("Apr 2027",plan.last().label)
  assertEquals(listOf("small","large"),plan.first().rows.map{it.id})
  assertEquals(50.0,plan[1].rows.first().extra,0.0001)
  small.put("carryoverPct",0)
  assertEquals(5,makePayoffPlan(listOf(small,large),0.0).size)
 }
 @Test fun snowballInterestRoundsLikeTheActualHtml(){
  val loan=JSONObject().put("id","interest").put("name","Zins").put("remainingBalance",100).put("monthlyPayment",10).put("monthlyRate",0.00125)
  assertEquals(90.13,makePayoffPlan(listOf(loan),0.0).first().rows.first().remaining,0.0001)
 }
}
