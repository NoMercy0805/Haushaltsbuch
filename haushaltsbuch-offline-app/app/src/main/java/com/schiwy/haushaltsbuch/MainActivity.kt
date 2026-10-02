package com.schiwy.haushaltsbuch

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\nimport androidx.compose.foundation.clickable
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
    var state by remember{mutableStateOf(repo.load())};var tab by remember{mutableStateOf(Tab.BALANCE)};var addBeta by remember{mutableStateOf(false)};var settings by remember{mutableStateOf(false)};var month by remember{mutableStateOf(YearMonth.now())}
    fun persist(){repo.save(state);state=JSONObject(state.toString())}
    Scaffold(
        topBar={Surface(color=MaterialTheme.colorScheme.background.copy(alpha=.92f)){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Haushaltsbuch",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.6).sp);Text("+",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.surface,modifier=Modifier.clickable{settings=true}){Text("⚙",Modifier.padding(10.dp),fontSize=18.sp)}}}}},
        bottomBar={Surface(tonalElevation=3.dp,shape=RoundedCornerShape(26.dp),modifier=Modifier.padding(horizontal=6.dp,vertical=4.dp)){Row(Modifier.fillMaxWidth().padding(vertical=6.dp)){Tab.entries.forEach{t->Column(Modifier.weight(1f).padding(vertical=6.dp).clickable{tab=t},horizontalAlignment=Alignment.CenterHorizontally){OriginalTabIcon(t,if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,tab==t);Text(t.label,fontSize=10.5.sp,maxLines=1,color=if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Bold)}}}}},
        floatingActionButton={if(tab==Tab.BALANCE)FloatingActionButton(onClick={addBeta=true},containerColor=MaterialTheme.colorScheme.primary.copy(alpha=.80f),shape=RoundedCornerShape(50)){Text("+",fontSize=30.sp,color=Color.White)}}
    ){pad->Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)){when(tab){Tab.BALANCE->BalanceScreen(state,repo,month,{month=month.minusMonths(1)},{month=month.plusMonths(1)});Tab.BOOKINGS->BookingsScreen(state,repo);Tab.VARIABLE->VariableScreen(state,repo);Tab.LOANS->LoansScreen(state,repo);Tab.FUEL->FuelScreen(state,repo)}}}
    if(settings)SettingsDialog(state,{settings=false})\n    if(addBeta)BetaEntryDialog({addBeta=false}){type,title,date,amount,category->repo.array(state,"betaEntries").put(JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("title",title).put("date",date).put("amount",amount).put("category",category).put("recurring",false).put("reconciled",false));persist();addBeta=false}
}

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
