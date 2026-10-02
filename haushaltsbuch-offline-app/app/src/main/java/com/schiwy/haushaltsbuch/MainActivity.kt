package com.schiwy.haushaltsbuch

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Paper = Color(0xFFF2F2F5)
private val Ink = Color(0xFF191A1C)
private val InkSoft = Color(0xFF6E7074)
private val Green = Color(0xFF00A971)
private val Red = Color(0xFFF0421F)
private val Blue = Color(0xFF1268FF)
private val SurfaceSolid = Color(0xFFFDFDFE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(primary=Color(0xFF5B9CFF), background=Color(0xFF0F1013), surface=Color(0xFF1E1F24), onBackground=Color(0xFFF2F2F4), onSurface=Color(0xFFF2F2F4))
                else lightColorScheme(primary=Blue, background=Paper, surface=SurfaceSolid, onBackground=Ink, onSurface=Ink)
            ) { NativeApp() }
        }
    }
}

private class HouseholdRepository(private val activity: Activity) {
    private val store = NativeKeyValueStore.getInstance(activity)
    private val key = "household-data-v8"
    fun load(): JSONObject = runCatching { JSONObject(store.getItem(key) ?: "{}") }.getOrDefault(JSONObject())
    fun save(state: JSONObject) = store.setItem(key, state.toString())
    fun array(state: JSONObject, name: String): JSONArray = state.optJSONArray(name) ?: JSONArray().also { state.put(name, it) }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
private fun money(v: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(v)
private fun sum(a: JSONArray, field: String, predicate: (JSONObject)->Boolean = { true }): Double = a.objects().filter(predicate).sumOf { it.optDouble(field, 0.0) }
private enum class Tab(val label:String,val symbol:String){BALANCE("Kontostand","▰"),BOOKINGS("Buchungen","▤"),VARIABLE("Variable Kosten","◎"),LOANS("Kredite","▣"),FUEL("Tanken","⛽")}

@Composable private fun NativeApp(){
    val activity=androidx.compose.ui.platform.LocalContext.current as Activity
    val repo=remember{HouseholdRepository(activity)}
    var state by remember{mutableStateOf(repo.load())};var tab by remember{mutableStateOf(Tab.BALANCE)};var addBeta by remember{mutableStateOf(false)};var month by remember{mutableStateOf(YearMonth.now())}
    fun persist(){repo.save(state);state=JSONObject(state.toString())}
    Scaffold(
        topBar={Surface(color=MaterialTheme.colorScheme.background.copy(alpha=.92f)){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Haushaltsbuch",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.6).sp);Text("+",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surface){Text("⚙",Modifier.padding(10.dp),fontSize=18.sp)}}}}},
        bottomBar={Surface(tonalElevation=3.dp,shape=RoundedCornerShape(26.dp),modifier=Modifier.padding(horizontal=6.dp,vertical=4.dp)){Row(Modifier.fillMaxWidth().padding(vertical=6.dp)){Tab.entries.forEach{t->Column(Modifier.weight(1f).padding(vertical=6.dp).clickable{tab=t},horizontalAlignment=Alignment.CenterHorizontally){Text(t.symbol,fontSize=17.sp,color=if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=if(tab==t)FontWeight.ExtraBold else FontWeight.Normal);Text(t.label,fontSize=10.sp,maxLines=1,color=if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Bold)}}}}},
        floatingActionButton={if(tab==Tab.BALANCE)FloatingActionButton(onClick={addBeta=true},containerColor=MaterialTheme.colorScheme.primary.copy(alpha=.80f),shape=RoundedCornerShape(50)){Text("+",fontSize=30.sp,color=Color.White)}}
    ){pad->Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)){when(tab){Tab.BALANCE->BalanceScreen(state,repo,month,{month=month.minusMonths(1)},{month=month.plusMonths(1)});Tab.BOOKINGS->BookingsScreen(state,repo);Tab.VARIABLE->VariableScreen(state,repo);Tab.LOANS->LoansScreen(state,repo);Tab.FUEL->FuelScreen(state,repo)}}}
    if(addBeta)BetaEntryDialog({addBeta=false}){type,title,date,amount,category->repo.array(state,"betaEntries").put(JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("title",title).put("date",date).put("amount",amount).put("category",category).put("recurring",false).put("reconciled",false));persist();addBeta=false}
}

@Composable private fun BalanceScreen(state:JSONObject,repo:HouseholdRepository,month:YearMonth,prev:()->Unit,next:()->Unit){
    val all=repo.array(state,"betaEntries").objects();val entries=all.filter{runCatching{YearMonth.from(LocalDate.parse(it.optString("date")))==month}.getOrDefault(false)}
    val income=entries.filter{it.optString("type")=="income"}.sumOf{it.optDouble("amount")};val expense=entries.filter{it.optString("type")!="income"}.sumOf{it.optDouble("amount")};val balance=income-expense
    Column(Modifier.fillMaxSize()){IncomeExpenseBar(income,expense);Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick=prev){Text("‹")};Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.GERMAN)).replaceFirstChar{it.uppercase()},fontWeight=FontWeight.Bold);TextButton(onClick=next){Text("›")}}
        Card(Modifier.fillMaxWidth().padding(horizontal=16.dp),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text("Kontostand",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(money(balance),fontSize=32.sp,fontWeight=FontWeight.Bold,color=if(balance>=0)Green else Red);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth()){Text("Einnahmen "+money(income),color=Green,modifier=Modifier.weight(1f));Text("Ausgaben "+money(expense),color=Red)}}}
        Text("Buchungen",Modifier.padding(16.dp,18.dp,16.dp,8.dp),fontWeight=FontWeight.Bold,fontSize=18.sp)
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(entries.isEmpty())item{EmptyCard("Keine Buchungen in diesem Monat")};items(entries.sortedByDescending{it.optString("date")}){e->EntryCard(e.optString("title","Buchung"),e.optString("date"),e.optDouble("amount"),e.optString("type")=="income")}}
    }
}
@Composable private fun IncomeExpenseBar(income:Double,expense:Double){val total=(income+expense).takeIf{it>0}?:1.0;val left=(income/total).toFloat().coerceIn(0f,1f);Row(Modifier.fillMaxWidth().height(34.dp)){Box(Modifier.weight(left.coerceAtLeast(.001f)).fillMaxHeight().background(Green),contentAlignment=Alignment.Center){if(left>.22f)Text(money(income),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)};Box(Modifier.weight((1-left).coerceAtLeast(.001f)).fillMaxHeight().background(Red),contentAlignment=Alignment.Center){if(1-left>.22f)Text(money(expense),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)}}}

@Composable private fun BookingsScreen(state:JSONObject,repo:HouseholdRepository){val income=repo.array(state,"income");val fixed=repo.array(state,"fixedExpenses");val invoices=repo.array(state,"openInvoices");LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{SummaryCard("Monatliche Einnahmen",money(sum(income,"amount")),Green)};item{SectionTitle("Einnahmen")};items(income.objects()){SimpleMoneyRow(it.optString("name"),it.optDouble("amount"))};item{SectionTitle("Fixkosten")};items(fixed.objects()){SimpleMoneyRow(it.optString("name"),it.optDouble("amount"))};item{SectionTitle("Offene Rechnungen")};items(invoices.objects().filter{!it.optBoolean("paid")}){SimpleMoneyRow(it.optString("name"),it.optDouble("amount"),it.optString("dueDate"))}}}
@Composable private fun VariableScreen(state:JSONObject,repo:HouseholdRepository){val costs=repo.array(state,"variableCosts");val savings=repo.array(state,"savings");val saved=savings.objects().sumOf{if(it.optString("type","deposit")=="expense")-it.optDouble("amount") else it.optDouble("amount")};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{SummaryCard("Spartopf",money(saved),Blue)};item{SectionTitle("Variable Kosten")};items(costs.objects()){o->SimpleMoneyRow(o.optString("name"),o.optDouble("monthlyAmount"),o.optString("interval"))};item{SectionTitle("Sparen")};items(savings.objects().sortedByDescending{it.optString("date")}){o->EntryCard(o.optString("comment","Sparen"),o.optString("date"),o.optDouble("amount"),o.optString("type","deposit")!="expense")}}}
@Composable private fun LoansScreen(state:JSONObject,repo:HouseholdRepository){val loans=repo.array(state,"loans").objects();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{SummaryCard("Restschuld",money(loans.sumOf{it.optDouble("remainingBalance")}),Red)};items(loans){o->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(o.optString("name","Kredit"),fontWeight=FontWeight.Bold);Text("Restschuld "+money(o.optDouble("remainingBalance")),fontSize=20.sp);Text("Rate "+money(o.optDouble("monthlyPayment"))+" / Monat",color=MaterialTheme.colorScheme.onSurfaceVariant)}}};if(loans.isEmpty())item{EmptyCard("Keine Kredite angelegt")}}}
@Composable private fun FuelScreen(state:JSONObject,repo:HouseholdRepository){val cars=repo.array(state,"cars").objects();val logs=repo.array(state,"fuelLogs").objects();LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{SummaryCard("Tankvorgänge",logs.size.toString(),Blue)};item{SectionTitle("Fahrzeuge")};items(cars){o->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(o.optString("name","Fahrzeug"),fontWeight=FontWeight.Bold);Text(logs.count{it.optString("carId")==o.optString("id")}.toString()+" Tankvorgänge",color=MaterialTheme.colorScheme.onSurfaceVariant)}}};item{SectionTitle("Letzte Tankvorgänge")};items(logs.sortedByDescending{it.optString("date")}.take(30)){o->SimpleMoneyRow(o.optString("date"),o.optDouble("liters")*o.optDouble("pricePerLiter"),o.optDouble("liters").toString()+" l · "+money(o.optDouble("pricePerLiter"))+"/l")}}}

@Composable private fun SectionTitle(t:String)=Text(t,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))
@Composable private fun EmptyCard(t:String)=Card(Modifier.fillMaxWidth()){Text(t,Modifier.padding(18.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun SummaryCard(title:String,value:String,accent:Color)=Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=28.sp,fontWeight=FontWeight.Bold,color=accent)}}
@Composable private fun SimpleMoneyRow(name:String,amount:Double,sub:String="")=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(name.ifBlank{"Eintrag"},fontWeight=FontWeight.SemiBold);if(sub.isNotBlank())Text(sub,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(money(amount),fontWeight=FontWeight.Bold)}}
@Composable private fun EntryCard(title:String,date:String,amount:Double,positive:Boolean)=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title.ifBlank{"Buchung"},fontWeight=FontWeight.SemiBold);Text(date,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text((if(positive)"+ " else "− ")+money(amount),fontWeight=FontWeight.Bold,color=if(positive)Green else Red)}}

@Composable private fun BetaEntryDialog(onDismiss:()->Unit,onSave:(String,String,String,Double,String)->Unit){var type by remember{mutableStateOf("expense")};var title by remember{mutableStateOf("")};var date by remember{mutableStateOf(LocalDate.now().toString())};var amount by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Buchung hinzufügen")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(type=="income",{type="income"},{Text("Einnahme")});FilterChip(type=="expense",{type="expense"},{Text("Ausgabe")})};OutlinedTextField(title,{title=it},label={Text("Titel")},singleLine=true);OutlinedTextField(amount,{amount=it},label={Text("Betrag")},singleLine=true);OutlinedTextField(date,{date=it},label={Text("Datum (JJJJ-MM-TT)")},singleLine=true);OutlinedTextField(category,{category=it},label={Text("Kategorie")},singleLine=true)}},confirmButton={Button(onClick={val v=amount.replace(".","").replace(",",".").toDoubleOrNull();if(title.isNotBlank()&&v!=null&&v>0)onSave(type,title,date,v,category)}){Text("Speichern")}},dismissButton={TextButton(onClick=onDismiss){Text("Abbrechen")}})}
