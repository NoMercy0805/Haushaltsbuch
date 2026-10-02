package com.schiwy.haushaltsbuch

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.style.TextAlign
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
private val HeroBlueStart = Color(0xFFCFE0FF)
private val HeroBlueEnd = Color(0xFFB8D2FF)
private val HeroBlueText = Color(0xFF0D3C8C)
private val HeroBlueSub = Color(0xFF3A66AD)

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
private enum class Tab(val label:String){BALANCE("Kontostand"),BOOKINGS("Buchungen"),VARIABLE("Variable Kosten"),LOANS("Kredite"),FUEL("Tanken")}

@Composable private fun NativeApp(){
    val activity=androidx.compose.ui.platform.LocalContext.current as Activity
    val repo=remember{HouseholdRepository(activity)}
    var state by remember{mutableStateOf(repo.load())};var tab by remember{mutableStateOf(Tab.BALANCE)};var addBeta by remember{mutableStateOf(false)};var addForm by remember{mutableStateOf<String?>(null)};var fabOpen by remember{mutableStateOf(false)};var settings by remember{mutableStateOf(false)};var month by remember{mutableStateOf(YearMonth.now())}
    fun persist(){repo.save(state);state=JSONObject(state.toString())}
    Scaffold(
        topBar={Surface(color=MaterialTheme.colorScheme.background.copy(alpha=.92f)){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Haushaltsbuch",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.6).sp);Text("+",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.weight(1f));Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.size(38.dp).clickable{settings=true}){Box(contentAlignment=Alignment.Center){SettingsGearIcon(MaterialTheme.colorScheme.onSurfaceVariant)}}}}}},
        bottomBar={Surface(tonalElevation=3.dp,shape=RoundedCornerShape(26.dp),modifier=Modifier.padding(horizontal=6.dp,vertical=4.dp)){Row(Modifier.fillMaxWidth().padding(vertical=6.dp)){Tab.entries.forEach{t->Column(Modifier.weight(1f).padding(vertical=6.dp).clickable{tab=t},horizontalAlignment=Alignment.CenterHorizontally){OriginalTabIcon(t,if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,tab==t);Text(t.label,fontSize=10.5.sp,maxLines=1,color=if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Bold)}}}}},
        floatingActionButton={Box(contentAlignment=Alignment.BottomEnd){if(fabOpen){Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.4f)).clickable{fabOpen=false});Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.padding(bottom=76.dp)){fabOptions(tab).forEach{opt->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surface){Text(opt,Modifier.padding(horizontal=13.dp,vertical=8.dp),fontSize=13.sp,fontWeight=FontWeight.Bold)};Surface(shape=CircleShape,color=fabOptionColor(opt),modifier=Modifier.size(46.dp).clickable{if(opt=="Neuer Eintrag")addBeta=true else addForm=opt;fabOpen=false}){Box(contentAlignment=Alignment.Center){Text("+",fontSize=22.sp,color=Color.White)}}}}}};Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.80f),modifier=Modifier.size(60.dp).clickable{fabOpen=!fabOpen},shadowElevation=8.dp){Box(contentAlignment=Alignment.Center){Text(if(fabOpen)"×" else "+",fontSize=30.sp,color=Color.White)}}}}
    ){pad->Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)){when(tab){Tab.BALANCE->BalanceScreen(state,repo,month,{month=month.minusMonths(1)},{month=month.plusMonths(1)});Tab.BOOKINGS->BookingsScreen(state,repo);Tab.VARIABLE->VariableScreen(state,repo);Tab.LOANS->LoansScreen(state,repo);Tab.FUEL->FuelScreen(state,repo)}}}
    if(settings)SettingsDialog(state,{settings=false})\n    addForm?.let{QuickAddForm(it,state,repo,{addForm=null}){persist();addForm=null}}\n    if(addBeta)BetaEntryDialog({addBeta=false}){type,title,date,amount,category->repo.array(state,"betaEntries").put(JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("title",title).put("date",date).put("amount",amount).put("category",category).put("recurring",false).put("reconciled",false));persist();addBeta=false}
}

private fun fabOptions(tab:Tab):List<String> = when(tab){Tab.BALANCE->listOf("Neuer Eintrag");Tab.BOOKINGS->listOf("Offene Rechnung","Einnahme","Fixkosten");Tab.VARIABLE->listOf("Einzahlung in den Spartopf","Variable Kostenart");Tab.LOANS->listOf("Kredit");Tab.FUEL->listOf("Tankvorgang","Auto hinzufügen")}\nprivate fun fabOptionColor(label:String):Color=when(label){"Einnahme","Einzahlung in den Spartopf"->Green;"Offene Rechnung"->Color(0xFFFF9F0A);"Kredit"->Red;"Tankvorgang","Auto hinzufügen"->Color(0xFF6B7280);else->Blue}\n\n@Composable private fun QuickAddForm(kind:String,state:JSONObject,repo:HouseholdRepository,onClose:()->Unit,onSaved:()->Unit){
 var name by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var second by remember{mutableStateOf("")};var third by remember{mutableStateOf("")};var fourth by remember{mutableStateOf("")};var fifth by remember{mutableStateOf("")};var dueMonth by remember{mutableStateOf(LocalDate.now().monthValue)};var mode by remember{mutableStateOf("in")}
 val cars=repo.array(state,"cars").objects();var carId by remember{mutableStateOf(cars.firstOrNull()?.optString("id").orEmpty())};var interval by remember{mutableStateOf("jaehrlich")}
 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.45f)).clickable{onClose()},contentAlignment=Alignment.Center){Column(Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color(0xFFD0D1D6)).clickable(enabled=false){}.padding(22.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(kind=="Einzahlung in den Spartopf")"Spartopf-Buchung" else kind,fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text("×",fontSize=20.sp,modifier=Modifier.clickable{onClose()}.padding(8.dp))};Spacer(Modifier.height(8.dp))
 if(kind=="Einzahlung in den Spartopf"){Row(Modifier.fillMaxWidth().background(Paper,RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Einzahlung",mode=="in",Modifier.weight(1f)){mode="in"};OriginalSubNav("Ausgabe",mode=="out",Modifier.weight(1f)){mode="out"}}}
 if(kind=="Tankvorgang"&&cars.isNotEmpty()){Text("Auto",fontSize=12.sp,color=InkSoft);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){cars.forEach{o->FuelChip(o.optString("name"),carId==o.optString("id")){carId=o.optString("id")}}}}
 if(kind!="Tankvorgang"){OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),placeholder={Text(if(kind=="Auto hinzufügen")"Name des Autos (z.B. Peugeot 2008)" else if(kind=="Einzahlung in den Spartopf")"Bezeichnung (optional)" else "Bezeichnung")})}
 if(kind!="Auto hinzufügen"){Spacer(Modifier.height(10.dp));OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),placeholder={Text(if(kind=="Tankvorgang")"Liter" else if(kind=="Variable Kostenart")"Monatlich (€)" else if(kind=="Kredit")"Kredit ohne Zinsen (€)" else "Betrag (€)")})}
 if(kind=="Offene Rechnung"||kind=="Tankvorgang"||kind=="Variable Kostenart"||kind=="Kredit"){Spacer(Modifier.height(10.dp));OutlinedTextField(second,{second=it},Modifier.fillMaxWidth(),placeholder={Text(when(kind){"Tankvorgang"->"€/Liter";"Variable Kostenart"->"Rechnung (€, optional)";"Kredit"->"Rate/Monat (€)";else->"Fälligkeitsdatum YYYY-MM-DD"})})}
 if(kind=="Tankvorgang"||kind=="Einzahlung in den Spartopf"){Spacer(Modifier.height(10.dp));OutlinedTextField(third,{third=it},Modifier.fillMaxWidth(),placeholder={Text("Datum YYYY-MM-DD")})};if(kind=="Kredit"){Spacer(Modifier.height(10.dp));OutlinedTextField(third,{third=it},Modifier.fillMaxWidth(),placeholder={Text("Gesamtbetrag inkl. Zinsen (optional)")});Spacer(Modifier.height(10.dp));OutlinedTextField(fourth,{fourth=it},Modifier.fillMaxWidth(),placeholder={Text("Laufzeit in Monaten (optional)")})}
 if(kind=="Variable Kostenart"){Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("monatlich","vierteljaehrlich","halbjaehrlich","jaehrlich","zweijaehrlich").forEach{v->Surface(shape=RoundedCornerShape(50),color=if(interval==v)Blue else Paper,modifier=Modifier.clickable{interval=v}){Text(v,Modifier.padding(horizontal=8.dp,vertical=7.dp),fontSize=10.sp,color=if(interval==v)Color.White else InkSoft)}}};Spacer(Modifier.height(10.dp));Text("Fälligkeitsmonat",fontSize=12.sp,color=InkSoft);Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){(1..12).forEach{m->Surface(shape=CircleShape,color=if(dueMonth==m)Blue else Paper,modifier=Modifier.size(25.dp).clickable{dueMonth=m}){Box(contentAlignment=Alignment.Center){Text(m.toString(),fontSize=9.sp,color=if(dueMonth==m)Color.White else InkSoft)}}}};if(interval=="zweijaehrlich"){Spacer(Modifier.height(10.dp));OutlinedTextField(fourth,{fourth=it},Modifier.fillMaxWidth(),placeholder={Text("Startjahr (z. B. 2024)")})}}
 Spacer(Modifier.height(14.dp));Button({val v=amount.replace(",",".").toDoubleOrNull()?:0.0;val v2=second.replace(",",".").toDoubleOrNull()?:0.0;when(kind){
 "Einnahme"->repo.array(state,"income").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("amount",v))
 "Fixkosten"->repo.array(state,"fixedExpenses").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("amount",v))
 "Offene Rechnung"->repo.array(state,"openInvoices").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("amount",v).put("dueDate",second).put("paid",false))
 "Auto hinzufügen"->repo.array(state,"cars").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name))
 "Tankvorgang"->repo.array(state,"fuelLogs").put(JSONObject().put("id",UUID.randomUUID().toString()).put("carId",carId).put("liters",v).put("pricePerLiter",v2).put("date",if(third.isBlank())LocalDate.now().toString() else third))
 "Einzahlung in den Spartopf"->{val key=if(mode=="in")"variableContributions" else "variablePayments";repo.array(state,key).put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("amount",v).put("date",if(third.isBlank())LocalDate.now().toString() else third))}
 "Variable Kostenart"->repo.array(state,"variableCosts").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("monthlyAmount",v).put("actualAmount",v2).put("interval",interval).put("dueMonth",dueMonth).put("dueYear",if(interval=="zweijaehrlich")fourth.toIntOrNull() else null))
 "Kredit"->{val total=third.replace(",",".").toDoubleOrNull();val term=fourth.toIntOrNull();repo.array(state,"loans").put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",name).put("originalAmount",v).put("remainingBalance",v).put("monthlyPayment",v2).put("totalTermMonths",term).put("totalWithInterest",total).put("monthlyRate",if(v>0&&total!=null)solveMonthlyRate(v,v2,total) else null).put("carryoverPct",100))}
 };onSaved()},Modifier.fillMaxWidth(),shape=RoundedCornerShape(50)){Text(when(kind){"Offene Rechnung"->"Rechnung hinzufügen";"Variable Kostenart"->"Kostenart hinzufügen";"Kredit"->"Kredit hinzufügen";"Einzahlung in den Spartopf"->if(mode=="in")"Einzahlen" else "Ausgeben";else->if(kind=="Auto hinzufügen")"Hinzufügen" else "Speichern"})}}
 }
}
private fun solveMonthlyRate(principal:Double,monthly:Double,total:Double):Double?{if(principal==0.0||monthly==0.0||total==0.0||total<=principal)return null;val n=total/monthly;if(n<1)return null;var r=.01;repeat(60){val pow=kotlin.math.pow(1+r,-n);val fv=monthly*(1-pow)/r-principal;val h=.00001;val powH=kotlin.math.pow(1+r+h,-n);val fH=monthly*(1-powH)/(r+h)-principal;val deriv=(fH-fv)/h;if(kotlin.math.abs(deriv)<1e-9)return@repeat;var nr=r-fv/deriv;if(nr<=0)nr=r/2;if(kotlin.math.abs(nr-r)<1e-9){r=nr;return@repeat};r=nr};return if(r>0&&r.isFinite())r else null}

@Composable private fun BalanceScreen(state:JSONObject,repo:HouseholdRepository,month:YearMonth,prev:()->Unit,next:()->Unit){
    val all=repo.array(state,"betaEntries").objects()
    val monthEntries=all.filter{runCatching{YearMonth.from(LocalDate.parse(it.optString("date")))==month}.getOrDefault(false)}
    val income=monthEntries.filter{it.optString("type")=="einnahme" || it.optString("type")=="income"}.sumOf{it.optDouble("amount")}
    val expense=monthEntries.filterNot{it.optString("type")=="einnahme" || it.optString("type")=="income"}.sumOf{it.optDouble("amount")}
    val cutoff=month.atEndOfMonth()
    val balance=all.filter{runCatching{!LocalDate.parse(it.optString("date")).isAfter(cutoff)}.getOrDefault(false)}.sumOf{if(it.optString("type")=="einnahme"||it.optString("type")=="income")it.optDouble("amount") else -it.optDouble("amount")}
    var filter by remember{mutableStateOf("all")}
    val unreconciled=monthEntries.count{!it.optBoolean("reconciled")}
    val shown=if(filter=="unreconciled")monthEntries.filter{!it.optBoolean("reconciled")}else monthEntries
    val groups=shown.groupBy{it.optString("date")}.toSortedMap(compareByDescending{it})
    Column(Modifier.fillMaxSize()){
        IncomeExpenseBar(income,expense)
        Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.35f),shape=RoundedCornerShape(bottomStart=20.dp,bottomEnd=20.dp)){
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.size(40.dp).clickable(onClick=prev)){Box(contentAlignment=Alignment.Center){Text("‹",fontSize=27.sp)}}
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    Row{Text(month.format(DateTimeFormatter.ofPattern("MMMM",Locale.GERMAN)).replaceFirstChar{it.uppercase()},fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.width(6.dp));Text(month.year.toString(),fontSize=19.sp,fontWeight=FontWeight.ExtraBold)}
                    Text("Einnahmen +"+money(income)+" · Ausgaben -"+money(expense),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.size(40.dp).clickable(onClick=next)){Box(contentAlignment=Alignment.Center){Text("›",fontSize=27.sp)}}
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(14.dp)).padding(4.dp)){
            BetaFilter("Alle",filter=="all",Modifier.weight(1f)){filter="all"}
            BetaFilter("Nicht abgeglichen ($unreconciled)",filter=="unreconciled",Modifier.weight(1f)){filter="unreconciled"}
        }
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=18.dp,vertical=2.dp)){
            if(groups.isEmpty())item{Text(if(filter=="unreconciled")"Alles in diesem Monat ist abgeglichen." else "Noch keine Einträge in diesem Monat.",Modifier.fillMaxWidth().padding(30.dp),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            groups.forEach{(date,entries)->item(key=date){BetaDayGroup(date,entries)}}
        }
        Row(Modifier.fillMaxWidth().background(if(balance>=0)Green else Red).padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Kontostand",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White.copy(alpha=.9f));Spacer(Modifier.width(10.dp));Text(money(balance),fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,modifier=Modifier.weight(1f))
            Text("01."+month.monthValue.toString().padStart(2,'0')+"."+month.year+"\n- "+month.lengthOfMonth().toString().padStart(2,'0')+"."+month.monthValue.toString().padStart(2,'0')+"."+month.year,fontSize=10.5.sp,color=Color.White.copy(alpha=.85f),textAlign=TextAlign.End)
        }
    }
}
@Composable private fun BetaFilter(label:String,active:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){
    Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(11.dp),color=if(active)MaterialTheme.colorScheme.surface else Color.Transparent,shadowElevation=if(active)1.dp else 0.dp){Text(label,Modifier.padding(vertical=11.dp,horizontal=8.dp),textAlign=TextAlign.Center,fontSize=13.5.sp,fontWeight=FontWeight.Bold,color=if(active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)}
}
@Composable private fun BetaDayGroup(date:String,entries:List<JSONObject>){
    val d=runCatching{LocalDate.parse(date)}.getOrNull()
    val total=entries.sumOf{if(it.optString("type")=="einnahme"||it.optString("type")=="income")it.optDouble("amount") else -it.optDouble("amount")}
    Surface(Modifier.fillMaxWidth().padding(bottom=3.dp),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface){
        Column{
            Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(HeroBlueStart,HeroBlueEnd))).padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(34.dp).border(1.5.dp,HeroBlueText,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text((d?.dayOfMonth?:0).toString().padStart(2,'0'),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary)}
                Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("TAGESSALDO",fontSize=11.5.sp,fontWeight=FontWeight.ExtraBold,color=HeroBlueText,letterSpacing=.23.sp);Text(if(d==null)"" else d.format(DateTimeFormatter.ofPattern("MMMM yyyy (EEEE)",Locale.GERMAN)),fontSize=11.sp,color=HeroBlueSub)}
                Text((if(total>=0) "+" else "")+money(total),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=if(total>=0)Green else Red)
            }
            entries.sortedByDescending{it.optString("id")}.forEachIndexed{idx,e->
                if(idx>0)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
                    val reconciled=e.optBoolean("reconciled")
                    Box(Modifier.size(22.dp).border(2.dp,if(reconciled)InkSoft else MaterialTheme.colorScheme.outline,RoundedCornerShape(50)).background(if(reconciled)InkSoft else Color.Transparent,RoundedCornerShape(50)),contentAlignment=Alignment.Center){if(reconciled)Text("✓",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)}
                    Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(e.optString("title"),fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1);if(e.optString("category").isNotBlank())Text(e.optString("category"),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    val positive=e.optString("type")=="einnahme"||e.optString("type")=="income";Text((if(positive) "+" else "-")+money(e.optDouble("amount")),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=if(positive)Green else Red)
                }
            }
        }
    }
}
@Composable private fun IncomeExpenseBar(income:Double,expense:Double){val total=(income+expense).takeIf{it>0}?:1.0;val left=(income/total).toFloat().coerceIn(0f,1f);Row(Modifier.fillMaxWidth().padding(top=12.dp).height(20.dp).clip(RoundedCornerShape(10.dp))){Box(Modifier.weight(left.coerceAtLeast(.001f)).fillMaxHeight().background(Green).padding(start=8.dp),contentAlignment=Alignment.CenterStart){if(left>.22f)Text(money(income),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)};Box(Modifier.weight((1-left).coerceAtLeast(.001f)).fillMaxHeight().background(Red).padding(end=8.dp),contentAlignment=Alignment.CenterEnd){if(1-left>.22f)Text(money(expense),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}}}

@Composable private fun BookingsScreen(state:JSONObject,repo:HouseholdRepository){
    val income=repo.array(state,"income");val fixed=repo.array(state,"fixedExpenses");val invoices=repo.array(state,"openInvoices")
    var view by remember{mutableStateOf("rechnungen")}
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background,RoundedCornerShape(14.dp)).padding(4.dp)){
            OriginalSubNav("Rechnungen",view=="rechnungen",Modifier.weight(1f)){view="rechnungen"}
            OriginalSubNav("Einnahmen",view=="einnahmen",Modifier.weight(1f)){view="einnahmen"}
            OriginalSubNav("Fixkosten",view=="fixkosten",Modifier.weight(1f)){view="fixkosten"}
        }
        Spacer(Modifier.height(18.dp))
        when(view){
            "rechnungen"->{
                val open=invoices.objects().filter{!it.optBoolean("paid")};val total=open.sumOf{it.optDouble("amount")}
                val today=LocalDate.now();val overdue=open.count{runCatching{LocalDate.parse(it.optString("dueDate")).isBefore(today)}.getOrDefault(false)}
                OriginalHero("Offene Rechnungen gesamt",money(total),"Rechnungen",open.size.toString(),"Davon überfällig",overdue.toString())
                LazyColumn{if(open.isEmpty())item{OriginalEmpty("Keine offenen Rechnungen eingetragen.")};items(open.sortedBy{it.optString("dueDate")}){o->InvoiceCompactRow(o)}}
            }
            "einnahmen"->{OriginalHero("Einnahmen gesamt",money(sum(income,"amount")));LazyColumn{if(income.length()==0)item{OriginalEmpty("Noch nichts eingetragen.")};items(income.objects()){OriginalCompactRow(it.optString("name"),it.optDouble("amount"),true)}}}
            else->{OriginalHero("Fixkosten gesamt",money(sum(fixed,"amount")));LazyColumn{if(fixed.length()==0)item{OriginalEmpty("Noch nichts eingetragen.")};items(fixed.objects()){OriginalCompactRow(it.optString("name"),it.optDouble("amount"),false)}}}
        }
    }
}
@Composable private fun OriginalSubNav(label:String,active:Boolean,modifier:Modifier,onClick:()->Unit){
    Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(11.dp),color=if(active)MaterialTheme.colorScheme.surface else Color.Transparent,shadowElevation=if(active)1.dp else 0.dp){Text(label,Modifier.padding(vertical=11.dp,horizontal=8.dp),textAlign=TextAlign.Center,fontSize=13.5.sp,fontWeight=FontWeight.Bold,color=if(active)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)}
}
@Composable private fun OriginalHero(label:String,value:String,sub1:String?=null,val1:String?=null,sub2:String?=null,val2:String?=null){
    Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(HeroBlueStart,HeroBlueEnd))).padding(horizontal=20.dp,vertical=22.dp)){
        Text(label,fontSize=13.sp,color=HeroBlueText);Spacer(Modifier.height(6.dp));Text(value,fontSize=34.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.68).sp,color=MaterialTheme.colorScheme.onSurface)
        if(sub1!=null&&val1!=null){Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Column{Text(sub1,fontSize=11.sp,color=HeroBlueSub);Spacer(Modifier.height(2.dp));Text(val1,fontSize=14.sp,fontWeight=FontWeight.Bold)};if(sub2!=null&&val2!=null)Column{Text(sub2,fontSize=11.sp,color=HeroBlueSub);Spacer(Modifier.height(2.dp));Text(val2,fontSize=14.sp,fontWeight=FontWeight.Bold)}}}
    }
}
@Composable private fun OriginalEmpty(text:String)=Text(text,Modifier.fillMaxWidth().padding(vertical=28.dp),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
@Composable private fun VariableScreen(state:JSONObject,repo:HouseholdRepository){val costs=repo.array(state,"variableCosts");val contrib=repo.array(state,"variableContributions");val payments=repo.array(state,"variablePayments");var view by remember{mutableStateOf("kostenarten")};val paid=sum(contrib,"amount")+state.optDouble("potCarryover",0.0);val spent=sum(payments,"amount");Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background,RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Kostenarten",view=="kostenarten",Modifier.weight(1f)){view="kostenarten"};OriginalSubNav("Spartopf",view=="spartopf",Modifier.weight(1f)){view="spartopf"}};Spacer(Modifier.height(18.dp));if(view=="kostenarten"){OriginalHero("Monatlicher Sparbedarf",money(sum(costs,"monthlyAmount")),"Kostenarten",costs.length().toString());LazyColumn{if(costs.length()==0)item{OriginalEmpty("Noch keine Kostenarten angelegt.")};items(costs.objects()){VariableCostCard(it)}}}else{OriginalHero("Rest im Spartopf",money(paid-spent),"Eingezahlt",money(paid),"Ausgegeben",money(spent));Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary));Spacer(Modifier.width(14.dp));Column{Text("Verlauf Spartopf",fontSize=15.sp,fontWeight=FontWeight.Bold);Text((contrib.length()+payments.length()).toString()+" Buchungen",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
@Composable private fun VariableCostCard(o:JSONObject){val monthly=o.optDouble("monthlyAmount");val actual=o.optDouble("actualAmount");val months=when(o.optString("interval")){"monatlich"->1;"vierteljaehrlich"->3;"halbjaehrlich"->6;"zweijaehrlich"->24;else->12};val goal=monthly*months;val buffer=goal-actual;Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal=20.dp,vertical=18.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text(o.optString("name"),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text(money(monthly),fontSize=14.5.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)};Spacer(Modifier.height(10.dp));VariableDetail("Zielbetrag",money(goal));VariableDetail("Letzte Rechnung",money(actual));VariableDetail("Puffer",money(buffer),if(buffer>=0)Green else Red)}}\n@Composable private fun VariableDetail(label:String,value:String,color:Color=MaterialTheme.colorScheme.onSurface){Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(label,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Text(value,fontSize=14.sp,fontWeight=FontWeight.Bold,color=color)}}\n@Composable private fun LoansScreen(state:JSONObject,repo:HouseholdRepository){
 val loans=repo.array(state,"loans").objects();var view by remember{mutableStateOf("kredite")}
 val active=loans.filter{it.optDouble("remainingBalance")>0};val original=active.sumOf{it.optDouble("originalAmount")};val remaining=active.sumOf{it.optDouble("remainingBalance")};val monthly=active.sumOf{it.optDouble("monthlyPayment")}
 Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
  Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background,RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Kredite",view=="kredite",Modifier.weight(1f)){view="kredite"};OriginalSubNav("Tilgungsplan",view=="tilgung",Modifier.weight(1f)){view="tilgung"}}
  Spacer(Modifier.height(18.dp))
  if(view=="kredite"){LoanHero(remaining,original,monthly);LazyColumn{if(active.isEmpty())item{OriginalEmpty("Noch keine laufenden Kredite oder Raten angelegt.")};items(active.sortedBy{it.optDouble("remainingBalance")}){LoanCardOriginal(it)}}}
  else{TilgungScreen(active,monthly)}
 }
}
@Composable private fun TilgungScreen(loans:List<JSONObject>,monthly:Double){
 var extra by remember{mutableStateOf(0.0)}
 if(loans.isEmpty()){OriginalEmpty("Keine laufenden Kredite - nichts zu tilgen. 🎉");return}
 val plan=makePayoffPlan(loans,extra);val estimate=plan.lastOrNull()?.month?:0
 val years=estimate/12;val rest=estimate%12
 val duration=(if(years>0)"$years "+if(years==1)"Jahr" else "Jahre" else "")+(if(years>0&&rest>0)", " else "")+(if(rest>0)"$rest "+if(rest==1)"Monat" else "Monate" else if(years==0)"0 Monate" else "")
 OriginalHero("Schuldenfrei in",duration,"Voraussichtlich fertig",LocalDate.now().plusMonths(estimate.toLong()).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.GERMAN)),"Betrag/Monat",money(monthly+extra))
 Column(Modifier.fillMaxWidth().padding(bottom=20.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp)){
  Text("Zusätzlicher Betrag pro Monat",fontSize=13.sp,color=InkSoft);Spacer(Modifier.height(10.dp));Text(money(extra),Modifier.fillMaxWidth().padding(bottom=6.dp),textAlign=TextAlign.Center,fontSize=30.sp,fontWeight=FontWeight.ExtraBold);HorizontalDivider(thickness=2.dp,color=HeroBlueText);Spacer(Modifier.height(14.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(50.0,100.0,150.0,200.0).forEach{v->Surface(Modifier.weight(1f).clickable{extra=v},shape=RoundedCornerShape(50),color=if(extra==v)HeroBlueStart else Paper){Text(v.toInt().toString()+" €",Modifier.padding(vertical=9.dp),textAlign=TextAlign.Center,fontSize=13.sp,color=if(extra==v)HeroBlueText else InkSoft)}}}
 }
 val finished=plan.flatMap{m->m.rows.filter{it.remaining<=.005}.map{Triple(it.name,m.month,it.payment)}}
 if(finished.isNotEmpty()){Text("Wann welcher Kredit fertig ist",Modifier.padding(top=20.dp,bottom=8.dp),fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal=20.dp)){finished.distinctBy{it.first}.forEachIndexed{i,p->Column(Modifier.padding(vertical=10.dp)){Text(p.first,fontSize=15.sp,fontWeight=FontWeight.Bold);Text("in "+payoffDuration(p.second)+" · "+LocalDate.now().plusMonths(p.second.toLong()).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.GERMAN)),fontSize=13.5.sp,color=InkSoft);Text("+"+money(p.third)+" zum Extra-Betrag",fontSize=13.sp,color=Green)};if(i<finished.distinctBy{it.first}.lastIndex)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}}}
 Spacer(Modifier.height(14.dp))
 plan.take(12).forEachIndexed{idx,m->Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).then(if(idx==0)Modifier.border(1.5.dp,Blue,RoundedCornerShape(14.dp)) else Modifier).padding(horizontal=20.dp,vertical=18.dp)){Row(Modifier.fillMaxWidth()){Text(m.label+(if(idx==0)" · aktuell" else ""),Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text(money(m.total),fontSize=14.5.sp,fontWeight=FontWeight.SemiBold)};m.rows.filter{it.extra>.005}.forEach{r->Row(Modifier.fillMaxWidth().padding(top=7.dp)){Text(r.name,Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.SemiBold);Text("+"+money(r.extra),fontSize=14.sp,fontWeight=FontWeight.Bold,color=Green)};Text("Restschuld danach: "+money(r.remaining),fontSize=12.sp,color=InkSoft)}}}
}
private data class PayRow(val id:String,val name:String,val payment:Double,val extra:Double,val remaining:Double)
private data class PayMonth(val month:Int,val label:String,val total:Double,val remaining:Double,val rows:List<PayRow>)
private fun cents(v:Double)=kotlin.math.round(v*100.0)/100.0
private fun makePayoffPlan(loans:List<JSONObject>,extra:Double,maxMonths:Int=600):List<PayMonth>{
 data class D(val id:String,val name:String,var balance:Double,val payment:Double,val rate:Double?,val carry:Int,var counted:Boolean=false)
 val ds=loans.filter{it.optDouble("remainingBalance")>0}.map{D(it.optString("id"),it.optString("name"),it.optDouble("remainingBalance"),it.optDouble("monthlyPayment"),if(it.isNull("monthlyRate"))null else it.optDouble("monthlyRate"),it.optInt("carryoverPct",100).coerceIn(0,100))}.toMutableList()
 if(ds.isEmpty())return emptyList();var currentBudget=ds.sumOf{it.payment}+extra;if(currentBudget<=0)return emptyList();val out=mutableListOf<PayMonth>();var m=0
 fun mark(d:D){if(d.balance<=.005&&!d.counted){d.counted=true;currentBudget=cents(currentBudget-cents(d.payment*(100-d.carry)/100.0))}}
 while(ds.any{it.balance>.005}&&m<maxMonths){m++;val start=currentBudget;var budget=start;val rows=mutableListOf<PayRow>()
  ds.forEach{d->if(d.balance>.005){val interest=if(d.rate!=null)cents(d.balance*d.rate) else 0.0;val maxPay=kotlin.math.min(d.payment,budget);val principal=kotlin.math.max(0.0,kotlin.math.min(maxPay-interest,d.balance));var pay=cents(principal+interest);pay=kotlin.math.min(pay,budget);d.balance=cents(d.balance-principal);budget=cents(budget-pay);rows+=PayRow(d.id,d.name,pay,0.0,d.balance);mark(d)}}
  while(budget>.005&&ds.any{it.balance>.005}){val d=ds.filter{it.balance>.005}.minBy{it.balance};val pay=kotlin.math.min(budget,d.balance);d.balance=cents(d.balance-pay);budget=cents(budget-pay);val i=rows.indexOfFirst{it.id==d.id};if(i>=0){val x=rows[i];rows[i]=x.copy(extra=x.extra+pay,remaining=d.balance)}else rows+=PayRow(d.id,d.name,0.0,pay,d.balance);mark(d)}
  val date=LocalDate.now().withDayOfMonth(1).plusMonths((m-1).toLong());val label=date.month.getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN).take(3)+" "+date.year
  out+=PayMonth(m,label,cents(start-budget),cents(ds.sumOf{it.balance}),rows)
 };return out
}
private fun payoffDuration(m:Int):String{val y=m/12;val r=m%12;return (if(y>0)"$y "+if(y==1)"Jahr" else "Jahre" else "")+(if(y>0&&r>0)", " else "")+(if(r>0)"$r "+if(r==1)"Monat" else "Monate" else if(y==0)"diesen Monat" else "")}
@Composable private fun LoanHero(remaining:Double,original:Double,monthly:Double){
 val pct=if(original>0)((original-remaining)/original).coerceIn(0.0,1.0) else 0.0
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(HeroBlueStart,HeroBlueEnd))).padding(horizontal=20.dp,vertical=22.dp)){
  Text("Restschuld gesamt",fontSize=13.sp,color=HeroBlueText);Spacer(Modifier.height(6.dp));Text(money(remaining),fontSize=34.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.68).sp)
  if(original>0){Spacer(Modifier.height(14.dp));Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(20.dp)).background(Paper)){Box(Modifier.fillMaxWidth(pct.toFloat()).fillMaxHeight().background(Blue))};Spacer(Modifier.height(4.dp));Row(Modifier.fillMaxWidth()){Text((pct*100).toInt().toString()+"% abbezahlt",Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft);Text("von "+money(original),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft)}}
  Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Column{Text("Kreditsumme",fontSize=11.sp,color=HeroBlueSub);Text(money(original),fontSize=14.sp,fontWeight=FontWeight.Bold)};Column{Text("Raten/Monat",fontSize=11.sp,color=HeroBlueSub);Text(money(monthly),fontSize=14.sp,fontWeight=FontWeight.Bold)}}
 }
}
@Composable private fun LoanCardOriginal(o:JSONObject){
 val original=o.optDouble("originalAmount");val remaining=o.optDouble("remainingBalance");val pct=if(original>0)((original-remaining)/original).coerceIn(0.0,1.0) else 0.0
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFDE8E3)).padding(horizontal=20.dp,vertical=18.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text(o.optString("name","Kredit"),Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text(money(remaining),fontSize=18.sp,fontWeight=FontWeight.ExtraBold,color=Red)}
  if(original>0){Spacer(Modifier.height(10.dp));Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(20.dp)).background(Paper)){Box(Modifier.fillMaxWidth(pct.toFloat()).fillMaxHeight().background(Blue))};Spacer(Modifier.height(4.dp));Row(Modifier.fillMaxWidth()){Text((pct*100).toInt().toString()+"% abbezahlt",Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft);Text("von "+money(original),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft)}}
  Spacer(Modifier.height(6.dp));LoanDetail("Rate",money(o.optDouble("monthlyPayment"))+" / Monat")
 }
}
@Composable private fun LoanDetail(label:String,value:String){Row(Modifier.fillMaxWidth().padding(top=2.dp)){Text(label,Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft);Text(value,fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft)}}
@Composable private fun FuelScreen(state:JSONObject,repo:HouseholdRepository){
 val cars=repo.array(state,"cars").objects();val logs=repo.array(state,"fuelLogs").objects();var selected by remember{mutableStateOf("__all__")}
 val shown=if(selected=="__all__")logs else logs.filter{it.optString("carId")==selected};val monthKey=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));val current=shown.filter{it.optString("date").startsWith(monthKey)}
 val total=current.sumOf{it.optDouble("liters")*it.optDouble("pricePerLiter")};val liters=current.sumOf{it.optDouble("liters")};val avg=if(liters>0)total/liters else 0.0
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=18.dp,vertical=0.dp)){
  item{Text("Tanken",fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.padding(bottom=12.dp));OriginalHero("Tanken diesen Monat – "+if(selected=="__all__")"Alle Autos" else cars.firstOrNull{it.optString("id")==selected}?.optString("name").orEmpty(),money(total),"Liter",String.format(Locale.GERMAN,"%.2f L",liters),"Ø Preis/Liter",if(avg>0)String.format(Locale.GERMAN,"%.3f €",avg) else "– €");Spacer(Modifier.height(2.dp));Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){FuelChip("Alle Autos",selected=="__all__"){selected="__all__"};cars.take(2).forEach{o->FuelChip(o.optString("name"),selected==o.optString("id")){selected=o.optString("id")}}};Spacer(Modifier.height(16.dp))}
  if(cars.isEmpty())item{OriginalEmpty("Noch kein Auto hinzugefügt. Tippe unten auf das Plus, um eins hinzuzufügen.")}
  else{item{Text("VERLAUF",fontSize=12.5.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=.5.sp,color=InkSoft,modifier=Modifier.padding(horizontal=4.dp,vertical=8.dp))};val groups=shown.sortedByDescending{it.optString("date")}.groupBy{it.optString("date").take(7)};groups.forEach{(key,items)->item{FuelGroupHeader(key,items.sumOf{it.optDouble("liters")*it.optDouble("pricePerLiter")})};items(items){o->FuelRow(o,cars,selected=="__all__")}}}
 }
}
@Composable private fun FuelChip(label:String,active:Boolean,onClick:()->Unit){Surface(Modifier.clickable(onClick=onClick),shape=RoundedCornerShape(50),color=if(active)Color(0xFF6B7280) else Paper){Text(label,Modifier.padding(horizontal=16.dp,vertical=10.dp),fontSize=13.sp,fontWeight=FontWeight.Bold,color=if(active)Color.White else InkSoft)}}
@Composable private fun FuelGroupHeader(key:String,total:Double){val label=runCatching{LocalDate.parse(key+"-01").format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.GERMAN))}.getOrDefault(key);Row(Modifier.fillMaxWidth().background(Paper).padding(horizontal=16.dp,vertical=9.dp)){Text(label.uppercase(Locale.GERMAN),Modifier.weight(1f),fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.35.sp,color=InkSoft);Text(money(total),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=InkSoft)}}
@Composable private fun FuelRow(o:JSONObject,cars:List<JSONObject>,showCar:Boolean){val d=runCatching{LocalDate.parse(o.optString("date")).format(DateTimeFormatter.ofPattern("dd.MM."))}.getOrDefault(o.optString("date"));val car=if(showCar)cars.firstOrNull{it.optString("id")==o.optString("carId")}?.optString("name")?.plus(" · ") ?: "" else "";OriginalCompactRow(car+d+" · "+String.format(Locale.GERMAN,"%.2f L · %.3f €/L",o.optDouble("liters"),o.optDouble("pricePerLiter")),o.optDouble("liters")*o.optDouble("pricePerLiter"),false)}
@Composable private fun SectionTitle(t:String)=Text(t,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))
@Composable private fun EmptyCard(t:String)=Card(Modifier.fillMaxWidth()){Text(t,Modifier.padding(18.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun SummaryCard(title:String,value:String,accent:Color)=Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=28.sp,fontWeight=FontWeight.Bold,color=accent)}}
@Composable private fun SimpleMoneyRow(name:String,amount:Double,sub:String="")=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(name.ifBlank{"Eintrag"},fontWeight=FontWeight.SemiBold);if(sub.isNotBlank())Text(sub,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(money(amount),fontWeight=FontWeight.Bold)}}
@Composable private fun EntryCard(title:String,date:String,amount:Double,positive:Boolean)=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title.ifBlank{"Buchung"},fontWeight=FontWeight.SemiBold);Text(date,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text((if(positive)"+ " else "− ")+money(amount),fontWeight=FontWeight.Bold,color=if(positive)Green else Red)}}

@Composable private fun BetaEntryDialog(onDismiss:()->Unit,onSave:(String,String,String,Double,String)->Unit){var type by remember{mutableStateOf("expense")};var title by remember{mutableStateOf("")};var date by remember{mutableStateOf(LocalDate.now().toString())};var amount by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Buchung hinzufügen")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(type=="income",{type="income"},{Text("Einnahme")});FilterChip(type=="expense",{type="expense"},{Text("Ausgabe")})};OutlinedTextField(title,{title=it},label={Text("Titel")},singleLine=true);OutlinedTextField(amount,{amount=it},label={Text("Betrag")},singleLine=true);OutlinedTextField(date,{date=it},label={Text("Datum (JJJJ-MM-TT)")},singleLine=true);OutlinedTextField(category,{category=it},label={Text("Kategorie")},singleLine=true)}},confirmButton={Button(onClick={val v=amount.replace(".","").replace(",",".").toDoubleOrNull();if(title.isNotBlank()&&v!=null&&v>0)onSave(type,title,date,v,category)}){Text("Speichern")}},dismissButton={TextButton(onClick=onDismiss){Text("Abbrechen")}})}


@Composable private fun SettingsDialog(state:JSONObject,onDismiss:()->Unit){
    var page by remember{mutableStateOf<String?>(null)}
    Box(Modifier.fillMaxSize().background(Color(0x73141416)).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){
        Surface(
            modifier=Modifier.padding(20.dp).widthIn(max=400.dp).fillMaxWidth().clickable(enabled=false){},
            shape=RoundedCornerShape(26.dp),
            color=if(androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF2E2F35) else Color(0xFFD0D1D6),
            shadowElevation=12.dp
        ){
            Column(Modifier.padding(22.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    if(page!=null){
                        Surface(shape=CircleShape,color=MaterialTheme.colorScheme.background,modifier=Modifier.size(36.dp).clickable{page=null}){
                            Box(contentAlignment=Alignment.Center){Text("‹",fontSize=26.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(page?:"Einstellungen",fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                    if(page==null)Surface(shape=CircleShape,color=MaterialTheme.colorScheme.background,modifier=Modifier.size(36.dp).clickable(onClick=onDismiss)){
                        Box(contentAlignment=Alignment.Center){Text("×",fontSize=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    } else Spacer(Modifier.width(36.dp))
                }
                Spacer(Modifier.height(6.dp))
                if(page==null){
                    SettingsRow("Darstellung","Hell, Dunkel, System"){page="Darstellung"}
                    SettingsRow("Sichtbare Bereiche","Welche Tabs angezeigt werden"){page="Sichtbare Bereiche"}
                    SettingsRow("Backup","Sichern, wiederherstellen, automatisch"){page="Backup"}
                    SettingsRow("Kontostand","Import, Dauerauftrag-Vorlagen, Titel-Vorschläge"){page="Kontostand"}
                    SettingsRow("Zurücksetzen","Alle Daten löschen"){page="Zurücksetzen"}
                    SettingsRow("Update-Historie","Alle bisherigen Änderungen",false){page="Update-Historie"}
                }else{
                    when(page){
                        "Darstellung"->Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            listOf("Hell","Dunkel","System").forEach{label->Surface(Modifier.weight(1f),shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.background){Text(label,Modifier.padding(vertical=11.dp),textAlign=TextAlign.Center,fontSize=13.5.sp,fontWeight=FontWeight.Bold)}}
                        }
                        "Sichtbare Bereiche"->Column(Modifier.padding(top=10.dp)){listOf("Buchungen","Variable Kosten","Kredite","Tanken").forEach{Text(it,Modifier.fillMaxWidth().padding(vertical=13.dp),fontSize=16.sp,fontWeight=FontWeight.SemiBold)}}
                        else->Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}
@Composable private fun SettingsRow(title:String,sub:String,divider:Boolean=true,onClick:()->Unit){
    Column(Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.primary,modifier=Modifier.size(32.dp)){Box(contentAlignment=Alignment.Center){Text("•",color=Color.White,fontSize=20.sp)}}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)){Text(title,fontSize=16.sp,fontWeight=FontWeight.Bold);Text(sub,fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Text("›",fontSize=22.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(divider)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable private fun OriginalTabIcon(tab:Tab,color:Color,active:Boolean){
    Canvas(Modifier.size(16.dp)){
        val sx=size.width/24f; val sy=size.height/24f
        fun p(x:Float,y:Float)=Offset(x*sx,y*sy)
        val stroke=Stroke(width=(if(active)2.6f else 2f)*sx)
        when(tab){
            Tab.BOOKINGS->{drawLine(color,p(6f,2f),p(15f,2f),stroke.width);drawLine(color,p(6f,2f),p(4f,4f),stroke.width);drawLine(color,p(4f,4f),p(4f,20f),stroke.width);drawLine(color,p(4f,20f),p(6f,22f),stroke.width);drawLine(color,p(6f,22f),p(18f,22f),stroke.width);drawLine(color,p(18f,22f),p(20f,20f),stroke.width);drawLine(color,p(20f,20f),p(20f,7f),stroke.width);drawLine(color,p(15f,2f),p(20f,7f),stroke.width);drawLine(color,p(14f,2f),p(14f,7f),stroke.width);drawLine(color,p(14f,7f),p(19f,7f),stroke.width);drawLine(color,p(8f,13f),p(16f,13f),stroke.width);drawLine(color,p(8f,17f),p(13f,17f),stroke.width)}
            Tab.VARIABLE->{drawOval(color,p(5f,3f),Size(14*sx,6*sy),style=stroke);drawLine(color,p(5f,6f),p(5f,16f),stroke.width);drawLine(color,p(19f,6f),p(19f,16f),stroke.width);drawArc(color,0f,180f,false,p(5f,8f),Size(14*sx,6*sy),style=stroke);drawArc(color,0f,180f,false,p(5f,13f),Size(14*sx,6*sy),style=stroke)}
            Tab.LOANS->{drawRoundRect(color,p(2f,5f),Size(20*sx,14*sy),cornerRadius=androidx.compose.ui.geometry.CornerRadius(2.5f*sx),style=stroke);drawLine(color,p(2f,10f),p(22f,10f),stroke.width)}
            Tab.FUEL->{drawRoundRect(color,p(3f,7f),Size(10*sx,14*sy),cornerRadius=androidx.compose.ui.geometry.CornerRadius(1.5f*sx),style=stroke);drawLine(color,p(6f,7f),p(6f,4f),stroke.width);drawLine(color,p(6f,4f),p(7f,3f),stroke.width);drawLine(color,p(7f,3f),p(10f,3f),stroke.width);drawLine(color,p(10f,3f),p(11f,4f),stroke.width);drawLine(color,p(11f,4f),p(11f,7f),stroke.width);drawLine(color,p(6.5f,12f),p(11.5f,12f),stroke.width);drawLine(color,p(13f,11f),p(15.3f,11f),stroke.width);drawLine(color,p(15.3f,11f),p(18.3f,14.1f),stroke.width)}
            Tab.BALANCE->{listOf(Triple(2f,13.4f,6f),Triple(9f,14.4f,13f),Triple(15f,10.4f,19f)).forEach{(x,y,cx)->drawOval(color,p(x,y),Size(8*sx,3.2f*sy),style=stroke)};drawLine(color,p(2f,15f),p(2f,18f),stroke.width);drawLine(color,p(10f,15f),p(10f,18f),stroke.width);drawLine(color,p(9f,16f),p(9f,20f),stroke.width);drawLine(color,p(17f,16f),p(17f,20f),stroke.width);drawLine(color,p(15f,12f),p(15f,17f),stroke.width);drawLine(color,p(23f,12f),p(23f,17f),stroke.width)}
        }
    }
}

@Composable private fun SettingsGearIcon(color:Color){
    Canvas(Modifier.size(20.dp)){
        val s=size.width/24f; val w=2f*s
        drawCircle(color,3.2f*s,Offset(12f*s,12f*s),style=Stroke(w))
        val pts=listOf(Offset(12f,3f),Offset(12f,21f),Offset(3f,12f),Offset(21f,12f),Offset(5.7f,5.7f),Offset(18.3f,18.3f),Offset(18.3f,5.7f),Offset(5.7f,18.3f))
        pts.forEach{p->val dx=(p.x-12f);val dy=(p.y-12f);val len=kotlin.math.sqrt(dx*dx+dy*dy);val ux=dx/len;val uy=dy/len;drawLine(color,Offset((12f+ux*6f)*s,(12f+uy*6f)*s),Offset((12f+ux*9f)*s,(12f+uy*9f)*s),w)}
    }
}

@Composable private fun OriginalCompactRow(name:String,amount:Double,income:Boolean,sub:String?=null,tint:Color=Color.Transparent,leading:(@Composable ()->Unit)?=null){
    Row(Modifier.fillMaxWidth().background(tint).padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
        if(leading!=null){leading();Spacer(Modifier.width(10.dp))}
        Column(Modifier.weight(1f)){Text(name,fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);if(!sub.isNullOrBlank()){Spacer(Modifier.height(2.dp));Text(sub,fontSize=11.5.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        Spacer(Modifier.width(10.dp));Text(money(amount),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=if(income)Green else MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
@Composable private fun InvoiceCompactRow(o:JSONObject){
    val d=runCatching{LocalDate.parse(o.optString("dueDate"))}.getOrNull();val today=LocalDate.now();val overdue=d?.isBefore(today)==true
    val dueThisMonth=d?.month==today.month&&d.year==today.year
    val sub=if(d==null)"" else if(overdue)"Überfällig seit "+d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) else "Fällig am "+d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val tint=if(dueThisMonth)Color(0xFFFDE8E3) else Color(0xFFDCF5EA)
    OriginalCompactRow(o.optString("name"),o.optDouble("amount"),false,sub,tint){
        Box(Modifier.size(24.dp).border(2.dp,Color(0xFFDADADE),CircleShape).background(MaterialTheme.colorScheme.surface,CircleShape))
    }
}
