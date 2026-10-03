package com.schiwy.haushaltsbuch
import org.junit.Assert.*
import org.junit.Test

class ReferenceFormsTest {
 @Test fun requiredNamesAndAmountsDoNotBecomeBlankZeroEntries(){
  for(kind in listOf("Einnahme","Fixkosten","Offene Rechnung","Variable Kostenart")){
   assertNull(buildReferenceQuickEntry(kind," ","100,00"))
   assertNull(buildReferenceQuickEntry(kind,"Test",""))
  }
 }
 @Test fun incomeAndFixedAllowZeroAsInHtmlSubmitHandlers(){
  for(kind in listOf("Einnahme","Fixkosten")){val entry=buildReferenceQuickEntry(kind," Test ","0,00")!!.second;assertEquals("Test",entry.getString("name"));assertEquals(0.0,entry.getDouble("amount"),0.0)}
 }
 @Test fun invoiceRequiresPositiveAmountAndDate(){
  assertNull(buildReferenceQuickEntry("Offene Rechnung","Test","0,00",date="2026-10-03"))
  assertNull(buildReferenceQuickEntry("Offene Rechnung","Test","1,00",date=""))
  val entry=buildReferenceQuickEntry("Offene Rechnung"," Test ","19,50",date="2026-10-03")!!.second
  assertEquals("2026-10-03",entry.getString("dueDate"));assertFalse(entry.getBoolean("paid"))
 }
 @Test fun potDepositUsesNoteAndNoName(){
  val result=buildReferenceQuickEntry("Einzahlung in den Spartopf","ignored","25,00",date="2026-10-03")!!
  assertEquals("variableContributions",result.first);assertEquals("",result.second.getString("note"));assertFalse(result.second.has("name"))
 }
 @Test fun potExpenseUsesItemNameAndNullItemId(){
  val result=buildReferenceQuickEntry("Einzahlung in den Spartopf"," ","25,00",date="2026-10-03",mode="out")!!
  assertEquals("variablePayments",result.first);assertTrue(result.second.isNull("itemId"));assertEquals("Ausgabe",result.second.getString("itemName"))
  assertEquals("Lebensmittel",buildReferenceQuickEntry("Einzahlung in den Spartopf"," Lebensmittel ","25,00",mode="out")!!.second.getString("itemName"))
 }
 @Test fun loanOriginalIsOptionalButMonthlyIsRequired(){
  assertNull(buildReferenceQuickEntry("Kredit","Test","100,00"))
  val entry=buildReferenceQuickEntry("Kredit","Test","",second="100,00",term="0")!!.second
  assertEquals(0.0,entry.getDouble("remainingBalance"),0.0);assertTrue(entry.isNull("totalTermMonths"));assertTrue(entry.isNull("totalWithInterest"));assertTrue(entry.isNull("monthlyRate"));assertEquals(100,entry.getInt("carryoverPct"))
 }
 @Test fun variableCostOptionalInvoiceAndStartYearDefaults(){
  val entry=buildReferenceQuickEntry("Variable Kostenart","Test","10,00",dueMonth=8)!!.second
  assertEquals(0.0,entry.getDouble("actualAmount"),0.0);assertTrue(entry.isNull("dueYear"));assertEquals(8,entry.getInt("dueMonth"))
  assertEquals(2024,buildReferenceQuickEntry("Variable Kostenart","Test","10,00",interval="zweijaehrlich",year="2024")!!.second.getInt("dueYear"))
 }
 @Test fun fuelNeedsCarAndPositiveLitersAndMilliPrice(){
  assertNull(buildReferenceQuickEntry("Tankvorgang","","50,00",second="1,799"))
  assertNull(buildReferenceQuickEntry("Tankvorgang","","0,00",second="1,799",carId="car"))
  val entry=buildReferenceQuickEntry("Tankvorgang","","50,00",second="1,799",carId="car")!!.second
  assertEquals(1.799,entry.getDouble("pricePerLiter"),0.000001);assertEquals(50.0,entry.getDouble("liters"),0.0)
 }
 @Test fun plusActionMatchesActiveHtmlSubview(){
  assertEquals(listOf("Neuer Eintrag"),fabOptions(Tab.BALANCE))
  assertEquals(listOf("Offene Rechnung"),fabOptions(Tab.BOOKINGS,"rechnungen"))
  assertEquals(listOf("Einnahme"),fabOptions(Tab.BOOKINGS,"einnahmen"))
  assertEquals(listOf("Fixkosten"),fabOptions(Tab.BOOKINGS,"fixkosten"))
  assertEquals(listOf("Variable Kostenart"),fabOptions(Tab.VARIABLE,variableView="kostenarten"))
  assertEquals(listOf("Einzahlung in den Spartopf"),fabOptions(Tab.VARIABLE,variableView="spartopf"))
  assertEquals(listOf("Kredit"),fabOptions(Tab.LOANS))
  assertEquals(2,fabOptions(Tab.FUEL).size)
 }
}
