package com.schiwy.haushaltsbuch

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.floor

internal fun referenceRound(value:Double)=floor(value+0.5)
internal fun referenceCents(value:Double)=referenceRound(value*100)/100

internal fun removeReferenceLoanPayment(state:JSONObject,loan:JSONObject,payment:JSONObject){
 val payments=state.optJSONArray("loanPayments")?:JSONArray().also{state.put("loanPayments",it)}
 for(i in payments.length()-1 downTo 0)if(payments.optJSONObject(i)?.optString("id")==payment.optString("id"))payments.remove(i)
 val amount=payment.optDouble("amount")
 if(payment.optString("type")=="increase"){
  loan.put("remainingBalance",referenceCents(max(0.0,loan.optDouble("remainingBalance")-amount)))
  loan.put("originalAmount",referenceCents(max(0.0,loan.optDouble("originalAmount")-amount)))
 }else{
  val reversal=if(payment.has("principalReduction")&&!payment.isNull("principalReduction"))payment.optDouble("principalReduction") else amount
  loan.put("remainingBalance",referenceCents(loan.optDouble("remainingBalance")+reversal))
 }
}

internal fun payoffReferenceLoan(state:JSONObject,loan:JSONObject,paymentId:String,today:String){
 val amount=loan.optDouble("remainingBalance")
 if(amount>0){
  val payments=state.optJSONArray("loanPayments")?:JSONArray().also{state.put("loanPayments",it)}
  payments.put(JSONObject().put("id",paymentId).put("loanId",loan.optString("id")).put("date",today).put("amount",amount).put("type","payment"))
 }
 loan.put("remainingBalance",0)
 if(loan.isNull("paidOffDate")||loan.optString("paidOffDate").isBlank())loan.put("paidOffDate",today)
}
