package com.schiwy.haushaltsbuch

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.pow

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
private val LocalAppDark = staticCompositionLocalOf { false }
private fun appPaper(dark:Boolean)=if(dark)Color(0xFF0F1013) else Color(0xFFF2F2F5)
private fun appInk(dark:Boolean)=if(dark)Color(0xFFF2F2F4) else Color(0xFF191A1C)
private fun appInkSoft(dark:Boolean)=if(dark)Color(0xFFA9ABB2) else Color(0xFF6E7074)
private fun appGreen(dark:Boolean)=if(dark)Color(0xFF2ED991) else Color(0xFF00A971)
private fun appRed(dark:Boolean)=if(dark)Color(0xFFFF5B42) else Color(0xFFF0421F)
private fun appBlue(dark:Boolean)=if(dark)Color(0xFF5B9CFF) else Color(0xFF1268FF)
private fun appSurface(dark:Boolean)=if(dark)Color(0xF71E1F24) else Color(0xF7FFFFFF)
private fun appPrimarySoft(dark:Boolean)=if(dark)Color(0xFF1B2A44) else Color(0xFFE3EDFF)
private fun appSavingsSoft(dark:Boolean)=if(dark)Color(0xFF1B3A2C) else Color(0xFFDCF5EA)
private fun appExpenseSoft(dark:Boolean)=if(dark)Color(0xFF3A2420) else Color(0xFFFDE8E3)
private fun appExhaust(dark:Boolean)=if(dark)Color(0xFF9CA3AF) else Color(0xFF6B7280)
private fun appExhaustSoft(dark:Boolean)=if(dark)Color(0xFF3A3A3E) else Color(0xFFE4E4E7)
private fun heroExhaustStart(dark:Boolean)=if(dark)Color(0xFF3A3A3E) else Color(0xFFE4E4E7)
private fun heroExhaustEnd(dark:Boolean)=if(dark)Color(0xFF2C2C30) else Color(0xFFD4D4D8)
private fun heroExhaustText(dark:Boolean)=if(dark)Color(0xFFC4C4C8) else Color(0xFF3F3F46)
private fun heroExhaustSub(dark:Boolean)=if(dark)Color(0xFFA8A8AE) else Color(0xFF52525B)
private fun appRule(dark:Boolean)=if(dark)Color(0xFF33343B) else Color(0xFFEBEBEE)
private fun appRuleStrong(dark:Boolean)=if(dark)Color(0xFF45464E) else Color(0xFFDADADE)
private fun appGlass(dark:Boolean)=if(dark)Color(0xB81C1D22) else Color(0xC7FFFFFF)
private fun genId()=UUID.randomUUID().toString()
internal fun betaIsIncome(entry:JSONObject)=entry.optString("type") in setOf("einnahme","income")
internal fun betaBalanceAt(entries:List<JSONObject>,cutoff:LocalDate):Double=entries.filter{runCatching{!LocalDate.parse(it.optString("date")).isAfter(cutoff)}.getOrDefault(false)}.sumOf{if(betaIsIncome(it))it.optDouble("amount") else -it.optDouble("amount")}
internal fun betaKnownTitles(state:JSONObject):List<String>{
 val hidden=state.optJSONArray("betaHiddenTitles")?.let{a->(0 until a.length()).map{a.optString(it)}}.orEmpty().toSet()
 return state.optJSONArray("betaEntries")?.objects().orEmpty().sortedByDescending{it.optString("date")}.map{it.optString("title")}.filter{it.isNotBlank()&&it !in hidden}.distinct()
}
private fun heroBlueStart(dark:Boolean)=if(dark)Color(0xFF1B2A4A) else Color(0xFFCFE0FF)
private fun heroBlueEnd(dark:Boolean)=if(dark)Color(0xFF14203A) else Color(0xFFB8D2FF)
private fun heroBlueText(dark:Boolean)=if(dark)Color(0xFF8FB4E8) else Color(0xFF0D3C8C)
private fun heroBlueSub(dark:Boolean)=if(dark)Color(0xFFB8C4D9) else Color(0xFF3A66AD)

class MainActivity : ComponentActivity() {
    private val widgetAction = mutableStateOf<String?>(null)
    var nativeStateForLifecycle: JSONObject? = null
    var nativeRepoForLifecycle: HouseholdRepository? = null
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); widgetAction.value=intent.getStringExtra(BetaWidgetProvider.EXTRA_BETA_ACTION) }
    override fun onStop() { nativeStateForLifecycle?.let { nativeRepoForLifecycle?.autoBackup(it) }; super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetAction.value=intent?.getStringExtra(BetaWidgetProvider.EXTRA_BETA_ACTION)
        setContent {
            val activity=this@MainActivity
            val themeRepo=remember{HouseholdRepository(activity)}
            var themeRevision by remember{mutableStateOf(0)}
            val choice=remember(themeRevision){themeRepo.getSetting("household-theme")?.takeIf{it in setOf("light","dark","system")} ?: "system"}
            val systemDark=androidx.compose.foundation.isSystemInDarkTheme()
            val dark=choice=="dark" || (choice=="system" && systemDark)
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(primary=Color(0xFF5B9CFF), background=Color(0xFF484F5A), surface=appSurface(true), onBackground=appInk(true), onSurface=appInk(true), onSurfaceVariant=appInkSoft(true), outline=appRuleStrong(true), outlineVariant=appRule(true))
                else lightColorScheme(primary=Blue, background=Color(0xFFC4CAD3), surface=appSurface(false), onBackground=Ink, onSurface=Ink, onSurfaceVariant=InkSoft, outline=appRuleStrong(false), outlineVariant=appRule(false))
            ) { CompositionLocalProvider(LocalAppDark provides dark,LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily=androidx.compose.ui.text.font.FontFamily.SansSerif,fontSize=16.sp,letterSpacing=0.sp,color=appInk(dark),platformStyle=androidx.compose.ui.text.PlatformTextStyle(includeFontPadding=false))) { NativeApp(onThemeChanged={themeRevision++},initialWidgetAction=widgetAction.value,onWidgetActionConsumed={widgetAction.value=null}) } }
        }
    }
}

class HouseholdRepository(private val activity: Activity) {
    private val store = NativeKeyValueStore.getInstance(activity)
    private val key = "household-data-v8"
    var pendingFreedLoan by mutableStateOf<JSONObject?>(null)
    fun offerFreedRate(loan:JSONObject){if(freedRateOffer(loan,this)!=null)pendingFreedLoan=loan}
    fun load(): JSONObject = runCatching { JSONObject(currentSnapshot ?: store.getItem(key) ?: "{}") }.getOrDefault(JSONObject()).also{state->array(state,"betaEntries").objects().forEach{entry->when(entry.optString("type")){"income"->entry.put("type","einnahme");"expense"->entry.put("type","ausgabe")}}}
    var revision by mutableLongStateOf(0L)
        private set
    @Volatile private var lastSavedJson:String?=null
    companion object {
        private val writes=java.util.concurrent.Executors.newSingleThreadExecutor()
        private val backups=java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        private val settingCache=java.util.concurrent.ConcurrentHashMap<String,String>()
        private val absentSettings=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        @Volatile private var currentSnapshot:String?=null
    }
    private var pendingBackup:java.util.concurrent.ScheduledFuture<*>?=null
    fun save(state: JSONObject){
        val snapshot=state.toString()
        lastSavedJson=snapshot;currentSnapshot=snapshot
        writes.execute{runCatching{store.setItem(key,snapshot)}.onFailure{android.util.Log.e("Haushaltsbuch","Daten konnten nicht gespeichert werden",it)}}
        revision++
        scheduleBackup(snapshot)
    }
    fun getSetting(name:String):String? {
        settingCache[name]?.let{return it};if(name in absentSettings)return null
        val value=store.getItem(name);if(value==null)absentSettings.add(name) else settingCache[name]=value
        return value
    }
    fun setSetting(name:String,value:String){settingCache[name]=value;absentSettings.remove(name);writes.execute{store.setItem(name,value)}}
    fun removeSetting(name:String){settingCache.remove(name);absentSettings.add(name);writes.execute{store.removeItem(name)}}
    fun autoBackup(state:JSONObject){scheduleBackup(lastSavedJson?:state.toString())}
    private fun scheduleBackup(snapshot:String){
        if(getSetting("haushaltsbuch-autobackup-enabled")!="1")return
        val raw=getSetting("haushaltsbuch-autobackup-uri")?:return
        val resolver=activity.applicationContext.contentResolver
        val context=activity.applicationContext
        synchronized(this){pendingBackup?.cancel(false);pendingBackup=backups.schedule(Runnable{
            if(getSetting("haushaltsbuch-autobackup-enabled")=="1"&&getSetting("haushaltsbuch-autobackup-uri")==raw)runCatching{
                val tree=DocumentFile.fromTreeUri(context,Uri.parse(raw))?:return@Runnable
                val file=tree.findFile("haushaltsbuch-auto-backup.csv")?:tree.createFile("text/csv","haushaltsbuch-auto-backup.csv")?:return@Runnable
                val csv=buildBackup(JSONObject(snapshot))
                resolver.openOutputStream(file.uri,"wt")?.bufferedWriter()?.use{it.write(csv)}
            }.onFailure{android.util.Log.e("Haushaltsbuch","Automatisches Backup fehlgeschlagen",it)}
        },750,java.util.concurrent.TimeUnit.MILLISECONDS)}
    }
    fun buildBackup(state:JSONObject):String{val tables=listOf("income" to listOf("id","name","amount"),"fixedExpenses" to listOf("id","name","amount","icon"),"openInvoices" to listOf("id","name","amount","dueDate","paid","paidDate"),"variableCosts" to listOf("id","name","monthlyAmount","actualAmount","interval","dueMonth","dueYear"),"variableContributions" to listOf("id","date","amount","note"),"variablePayments" to listOf("id","itemId","itemName","date","amount"),"loans" to listOf("id","name","originalAmount","remainingBalance","monthlyPayment","totalTermMonths","paidOffDate","carryoverPct","totalWithInterest","monthlyRate","lastReconciledBalance","lastReconciledDate"),"loanPayments" to listOf("id","loanId","date","amount","type"),"savings" to listOf("id","date","amount","type","comment"),"cars" to listOf("id","name"),"fuelLogs" to listOf("id","carId","liters","pricePerLiter","date"),"betaEntries" to listOf("id","type","title","date","amount","category","recurring","reconciled","isRecurringTemplate","recurrencePeriod","recurrenceEnd","recurringGroupId"));fun esc(v:Any?):String{val s=if(v==null||v==JSONObject.NULL)"" else v.toString();return if(s.any{it==','||it=='"'||it=='\n'}) "\""+s.replace("\"","\"\"")+"\"" else s};val out=mutableListOf<String>();tables.forEach{(name,cols)->val lines=mutableListOf("# TABLE:$name",cols.joinToString(","));array(state,name).objects().forEach{o->lines+=cols.joinToString(","){esc(o.opt(it))}};out+=lines.joinToString("\n")};out+=listOf("# TABLE:settings","amount",esc(state.optDouble("potCarryover",0.0))).joinToString("\n");val vis=getSetting("household-tab-visibility")?:"{}";val extra=getSetting("household-tilgung-extra")?:"0";val current=getSetting("household-tilgung-current-month")?:"";out+=listOf("# TABLE:appSettings","tabVisibilityJson,tilgungExtra,tilgungCurrentMonth",listOf(esc(vis),esc(extra),esc(current)).joinToString(",")).joinToString("\n");return out.joinToString("\n\n")}
    fun restoreBackup(text:String):JSONObject=restoreParsedBackup(parseHouseholdBackup(text))
    fun restoreParsedBackup(result:JSONObject):JSONObject{
        result.optJSONObject("tabVisibility")?.let{setSetting("household-tab-visibility",it.toString())}
        if(result.has("tilgungExtra"))setSetting("household-tilgung-extra",result.optDouble("tilgungExtra").toString())
        result.optString("tilgungCurrentMonth").takeIf{it.isNotBlank()}?.let{setSetting("household-tilgung-current-month",it)}
        save(result);return result
    }
    fun emptyState():JSONObject=JSONObject().also{state->listOf("income","fixedExpenses","openInvoices","variableCosts","variableContributions","variablePayments","loans","loanPayments","savings","cars","fuelLogs","betaEntries","betaHiddenTitles").forEach{state.put(it,JSONArray())};state.put("potCarryover",0.0)}
    fun array(state: JSONObject, name: String): JSONArray = state.optJSONArray(name) ?: JSONArray().also { state.put(name, it) }
}

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
private val currencyFormat=object:java.lang.ThreadLocal<NumberFormat>(){override fun initialValue():NumberFormat=NumberFormat.getCurrencyInstance(Locale.GERMANY)}
private fun money(v: Double): String = currencyFormat.get()!!.format(v)
internal fun JSONArray.strings():List<String> =(0 until length()).map{optString(it)}
private fun sum(a: JSONArray, field: String, predicate: (JSONObject)->Boolean = { true }): Double = a.objects().filter(predicate).sumOf { it.optDouble(field, 0.0) }

private data class NativeChange(val version:Int,val date:String,val items:List<String>)
private val NativeChangelog = listOf(
    NativeChange(1,"15.09.2026, 19:30 Uhr",listOf("Neuer Tilgungsplan-Reiter: Schneeball-Methode mit frei wählbarem Extra-Betrag","Individuelle Prozent-Auswahl (0/25/50/75/100%) pro Kredit, wie viel der freiwerdenden Rate nach Abbezahlung automatisch zum Extra-Betrag dazukommt","Beträge eintippen wie bei der Sparkasse – die letzten zwei Ziffern sind automatisch die Cent","Farbige Übersichtskarten in jedem Reiter, manuell per Pfeil oder Antippen der ganzen Karte ein-/ausklappbar","Diagramme überarbeitet: Ringe statt Kreisdiagramme (Ausgaben in Rot), Balken mit Farbverlauf","Bearbeiten-Formulare zeigen jetzt klare Beschriftungen bei jedem Feld","Tab-Leiste blendet sich automatisch aus, solange die Tastatur offen ist","Kopfbereich kompakter gestaltet, weniger verschwendeter Platz oben")),
    NativeChange(2,"15.09.2026, 21:15 Uhr",listOf("Farbige Übersichtskarten jetzt auch per Antippen der ganzen Karte ein-/ausklappbar (nicht nur per Pfeil)","Buttons und Karten geben jetzt beim Antippen ein spürbares, weicheres Feedback")),
    NativeChange(3,"15.09.2026, 22:40 Uhr",listOf("Neues, moderneres Erscheinungsbild: flachere Karten ohne Schatten, dezentere Ecken-Rundung, größere Schalter","Farbige, abgerundete Icon-Kacheln in den Einstellungen statt grauer Kreise","Aktiver Reiter in der Tab-Leiste jetzt farbig hervorgehoben statt mit Hintergrund-Kachel","Alle Symbole mit runden Linienenden statt eckiger dargestellt")),
    NativeChange(4,"15.09.2026, 23:30 Uhr",listOf("Fixkosten zeigt jetzt automatisch berechnete Einträge für Kreditraten, Tilgungsplan-Extra und Sparbedarf (Variable Kosten) – fließen auch korrekt in den Monatlichen Rest mit ein","Hero-Karte in der Übersicht zeigt jetzt \"Ausgaben\" statt nur \"Fixkosten\" – die komplette Summe aller monatlichen Ausgaben auf einen Blick","Fixkosten-Symbole jetzt in kräftigen, bunten Farben je Kategorie dargestellt")),
    NativeChange(5,"15.09.2026, 23:49 Uhr",listOf("Neue Update-Historie in den Einstellungen: alle bisherigen Änderungen mit Datum und Uhrzeit nachlesbar","\"Neu in dieser Version\" zeigt jetzt alle übersprungenen Updates zusammen an, falls du mal länger nicht aktualisiert hast")),
    NativeChange(6,"17.09.2026, 09:38 Uhr",listOf("Plus-Button öffnet jetzt kein separates Fenster mehr, sondern klappt die Optionen direkt mit farbigem Symbol nach oben auf – der Button wird dabei zum X")),
    NativeChange(7,"17.09.2026, 09:47 Uhr",listOf("Plus-Button wird jetzt bei Übersicht, Tilgungsplan und Diagramm ausgeblendet, da es dort nichts zum Hinzufügen gibt","Abstand zwischen Plus-Button und den aufklappenden Optionen vergrößert, damit nichts mehr überlappt","\"Letzte Neuerungen anzeigen\"-Button in der Update-Historie ergänzt, um \"Neu in dieser Version\" bei Bedarf manuell erneut aufzurufen")),
    NativeChange(8,"17.09.2026, 09:53 Uhr",listOf("\"Neu in dieser Version\"-Popup zeigte sich seit der Einführung der Update-Historie nicht mehr richtig – jetzt behoben")),
    NativeChange(9,"17.09.2026, 10:13 Uhr",listOf("Extra-Tilgungsbetrag im Tilgungsplan steht bei Neuinstallation jetzt standardmäßig auf 0€ statt 100€")),
    NativeChange(10,"17.09.2026, 13:52 Uhr",listOf("Kredit-Karten bei Kredite: noch nicht diesen Monat bezahlte (rote) und schon bezahlte (grüne) Karten stehen jetzt gruppiert, innerhalb jeder Gruppe weiterhin nach Restsumme sortiert")),
    NativeChange(11,"17.09.2026, 23:10 Uhr",listOf("Zwei neue Design-Stile zur Auswahl in den Einstellungen: Windows XP und Windows Vista")),
    NativeChange(12,"17.09.2026, 23:16 Uhr",listOf("Windows XP und Vista Design-Stile wieder entfernt")),
    NativeChange(13,"18.09.2026, 23:55 Uhr",listOf("Fixkosten werden jetzt nach Kategorie gruppiert dargestellt (z.B. Wohnen, Verträge & Abos), mit Zwischensumme je Gruppe")),
    NativeChange(14,"19.09.2026, 00:02 Uhr",listOf("Einnahmen und Bezahlte Rechnungen zeigen ihre Einträge jetzt kompakter: ein zusammenhängendes Kästchen mit dünnen Trennlinien statt vieler einzelner Karten")),
    NativeChange(15,"19.09.2026, 00:06 Uhr",listOf("Verlauf Spartopf bei Variable Kosten zeigt Buchungen jetzt auch im kompakten Kästchen-Stil (die Jahres-Auswahl dafür gab es bereits)")),
    NativeChange(16,"19.09.2026, 00:15 Uhr",listOf("Fixkosten: verwandte Kategorien jetzt zu größeren Gruppen zusammengefasst (z.B. Miete+Strom+Wasser+Gas unter \"Wohnen\", Streaming+Musik unter \"Verträge & Medien\"), alles in einem durchgehenden, kompakten Kästchen")),
    NativeChange(17,"19.09.2026, 00:19 Uhr",listOf("Zahlungsdatum bei Bezahlten Rechnungen und Verlauf Spartopf wird nicht mehr bei langen Namen abgeschnitten","Offene Rechnungen und der automatisch berechnete Teil bei Fixkosten jetzt ebenfalls im kompakten Kästchen-Stil")),
    NativeChange(18,"19.09.2026, 00:21 Uhr",listOf("Abbezahlte Kredite und der Verlauf beim Tilgungsplan jetzt auch im kompakten Kästchen-Stil")),
    NativeChange(19,"19.09.2026, 00:34 Uhr",listOf("Bearbeiten/Löschen-Buttons in den kompakten Listen jetzt versteckt, bis man den Eintrag antippt (wie bei Kredite) - klappen nach unten auf, Größe der Buttons bleibt unverändert")),
    NativeChange(20,"19.09.2026, 00:38 Uhr",listOf("Beträge in den kompakten Listen jetzt größer und fetter dargestellt, nutzt den Platz rechts besser aus")),
    NativeChange(21,"19.09.2026, 00:42 Uhr",listOf("Namen bei Offenen Rechnungen starten jetzt zuverlässig an derselben Stelle, unabhängig von der Länge des Betrags daneben")),
    NativeChange(22,"19.09.2026, 12:10 Uhr",listOf("Symbol-Auswahl beim Bearbeiten von Fixkosten zeigt jetzt Gruppen-Überschriften (Wohnen, Verträge & Medien usw.) und eine kleine Beschriftung unter jedem Symbol")),
    NativeChange(23,"19.09.2026, 12:14 Uhr",listOf("Neues Symbol \"Tanken\" ergänzt (Kategorie Alltag & Einkauf), wird auch automatisch bei Namen wie \"Tank\" oder \"Benzin\" erkannt")),
    NativeChange(24,"19.09.2026, 13:11 Uhr",listOf("Variable Kosten: Kostenarten, die noch nicht fällig sind, werden jetzt auch grün dargestellt (nicht nur die, die bereits diesen Monat bezahlt wurden) - rot nur noch bei fällig UND unbezahlt")),
    NativeChange(25,"19.09.2026, 13:40 Uhr",listOf("Tab-Leiste unten jetzt über die komplette Bildschirmbreite statt schwebend mit Rand links/rechts")),
    NativeChange(26,"19.09.2026, 13:46 Uhr",listOf("Variable Kosten: nur noch tatsächlich diese Jahr bereits bezahlte Kostenarten werden grün dargestellt - noch nicht fällige, aber auch noch nicht bezahlte bleiben weiß")),
    NativeChange(27,"19.09.2026, 13:49 Uhr",listOf("Tab-Leiste unten jetzt bündig am unteren Bildschirmrand statt mit Abstand schwebend")),
    NativeChange(28,"19.09.2026, 13:53 Uhr",listOf("Variable Kosten: bei mehreren Fälligkeiten pro Jahr (z.B. vierteljährlich) wird erst grün, sobald die LETZTE Zahlung des Jahres erfasst ist, nicht schon bei der ersten")),
    NativeChange(29,"19.09.2026, 14:11 Uhr",listOf("Variable Kosten: Kostenarten, die dieses Jahr gar nicht fällig sind (z.B. TÜV alle 2 Jahre), werden jetzt korrekt grün dargestellt","Variable Kosten: eine diesen Monat bezahlte Kostenart wird jetzt sofort grün, statt erst im Folgemonat")),
    NativeChange(30,"19.09.2026, 18:27 Uhr",listOf("Neuer Tank-Tracker: eigener Button oben links neben den Einstellungen. Eigene Autos anlegen, getankte Liter und Literpreis eintragen (Gesamtbetrag wird automatisch berechnet), Verlauf je Auto einsehbar und bearbeitbar")),
    NativeChange(31,"19.09.2026, 18:33 Uhr",listOf("\"Abbrechen\"/\"Hinzufügen\"-Buttons beim Auto hinzufügen im Tank-Tracker haben jetzt die richtige, abgerundete Optik")),
    NativeChange(32,"19.09.2026, 18:36 Uhr",listOf("Tank-Tracker: Überlauf bei Liter/Literpreis-Feldern behoben (kein seitliches Wischen mehr nötig), Farbe von Orange auf das app-typische Blau umgestellt")),
    NativeChange(33,"19.09.2026, 18:43 Uhr",listOf("Neue Übersicht-Karte \"Tanken gesamt\": großer Gesamtbetrag oben, Preise je Auto klein darunter - wie die anderen Übersicht-Karten ein-/ausblendbar","Übersicht-Karten in den Einstellungen jetzt per Ziehen am Griff neu anordenbar, statt fester Reihenfolge")),
    NativeChange(34,"19.09.2026, 19:04 Uhr",listOf("Tank-Tracker: Verlauf gruppiert jetzt nach Monat mit Zwischensumme, statt einer langen Liste")),
    NativeChange(35,"19.09.2026, 20:01 Uhr",listOf("\"Tanken\" ist jetzt ein vollwertiger Reiter unten (statt Button oben), \"Diagramm\" ist dafür als Button oben neben den Einstellungen gewandert")),
    NativeChange(36,"19.09.2026, 20:04 Uhr",listOf("Tanken hat jetzt eine Hero-Karte oben, wie bei den anderen Reitern - zeigt Gesamtbetrag, Liter und Ø Preis/Liter des aktuellen Monats fürs gewählte Auto, ein-/ausklappbar")),
    NativeChange(37,"19.09.2026, 20:08 Uhr",listOf("Plus-Button gibt es jetzt auch bei Tanken: \"Tankvorgang\" springt direkt zum Eintragen-Formular, \"Auto hinzufügen\" öffnet das Auto-Formular")),
    NativeChange(38,"19.09.2026, 20:11 Uhr",listOf("€/Liter-Feld beim Tanken hat jetzt dieselbe automatische Komma-Eingabe wie bei anderen Beträgen, aber mit drei statt zwei Nachkommastellen")),
    NativeChange(39,"19.09.2026, 20:17 Uhr",listOf("Neues Diagramm bei Tanken: Spritpreis-Entwicklung der letzten Tankvorgänge als Linie, mit Veränderung zum letzten Mal","Plus-Button bei Tanken öffnet jetzt richtige Popup-Formulare für \"Tankvorgang\" und \"Auto hinzufügen\", statt nur zum Feld hinzuscrollen - wie bei den anderen Reitern")),
    NativeChange(40,"19.09.2026, 20:20 Uhr",listOf("Spritpreis-Entwicklung zeigt jetzt die Tankvorgänge aller Autos zusammen, statt nur des gerade ausgewählten")),
    NativeChange(41,"19.09.2026, 20:25 Uhr",listOf("Spritpreis-Diagramm: Zeitraum wählbar (Woche, Monat, 3 Monate, 6 Monate, aktuelles Jahr)","\"+ Auto\"-Chip bei Tanken entfernt (Auto hinzufügen läuft jetzt nur noch über den Plus-Button unten)","Auto-Auswahl etwas überarbeitet: kleines Auto-Symbol je Chip, aktiver Chip jetzt mit Schatten hervorgehoben")),
    NativeChange(42,"19.09.2026, 20:30 Uhr",listOf("\"Neuer Tankvorgang\"-Formular im Tanken-Tab entfernt, da schon über den Plus-Button erreichbar","Spritpreis-Diagramm zeigt jetzt die Zeitraum-Auswahl auch bei 0 oder 1 Tankvorgängen an, statt komplett zu verschwinden")),
    NativeChange(43,"19.09.2026, 20:34 Uhr",listOf("Auto-Auswahl bei Tanken zeigt jetzt alle Autos gleichzeitig (umbricht in mehrere Zeilen), statt seitlich wegzuscrollen","Neue \"Gesamt\"-Option zeigt alle Autos zusammen (Hero-Karte und Verlauf mit Auto-Namen je Eintrag)","Autos lassen sich jetzt auch wieder löschen (X erscheint am ausgewählten Auto-Chip, löscht auch dessen Tankvorgänge mit)")),
    NativeChange(44,"19.09.2026, 20:36 Uhr",listOf("Tankvorgang-Formular hat jetzt ein Datumsfeld (steht standardmäßig auf heute), falls man mal einen Tankvorgang nachträgt")),
    NativeChange(45,"19.09.2026, 21:45 Uhr",listOf("\"Tanken gesamt\"-Karte in der Übersicht zeigt jetzt nur noch den aktuellen Monat, statt der Gesamtsumme aller Zeit - wie bei der Hero-Karte im Tanken-Tab")),
    NativeChange(46,"19.09.2026, 22:02 Uhr",listOf("Tanken jetzt in einem Abgas-Grau statt Blau: Hero-Karte, Auto-/Zeitraum-Auswahl, Diagramm und Buttons")),
    NativeChange(47,"19.09.2026, 22:07 Uhr",listOf("Kredite und Tilgungsplan teilen sich jetzt einen Reiter unten, mit Umschalter (inkl. Symbolen) oben zwischen beiden Ansichten - dadurch mehr Platz in der Tab-Leiste")),
    NativeChange(48,"19.09.2026, 22:14 Uhr",listOf("Buchungen: Rechnungen, Einnahmen und Fixkosten jetzt über Umschalter statt untereinander","Variable Kosten: Kostenarten und Spartopf jetzt über Umschalter statt untereinander","Plus-Button zeigt in jeder Unteransicht direkt die passende Option, statt einer Auswahl")),
    NativeChange(49,"19.09.2026, 22:18 Uhr",listOf("Seitliches Wischen wechselt nicht mehr zwischen den Haupt-Reitern unten (nur noch per Antippen), sondern zwischen den Unteransichten innerhalb eines Reiters (z.B. Rechnungen/Einnahmen/Fixkosten)")),
    NativeChange(50,"19.09.2026, 22:31 Uhr",listOf("Einklapp-Funktion bei Einnahmen, Fixkosten, Kostenarten und Verlauf Spartopf entfernt, da diese jetzt eigene Unteransichten sind - Bezahlte Rechnungen, Abbezahlte Kredite und Tilgungsplan-Verlauf bleiben weiterhin einklappbar")),
    NativeChange(51,"19.09.2026, 23:20 Uhr",listOf("Die Android-Statusleiste oben (Uhrzeit, Akku usw.) passt sich jetzt automatisch an helles/dunkles Design an, statt immer blau zu sein")),
    NativeChange(52,"19.09.2026, 23:25 Uhr",listOf("Das blaue Standard-Antipp-Rechteck der Android WebView entfernt - die App nutzt jetzt nur noch ihre eigenen, weicheren Tipp-Effekte")),
    NativeChange(53,"19.09.2026, 23:44 Uhr",listOf("Neues App-Icon: dieselbe Buch-Komposition wie bisher, aber flach statt mit Verlauf und Schlagschatten - passend zum restlichen, flachen App-Design")),
    NativeChange(54,"20.09.2026, 13:30 Uhr",listOf("App-Icon nochmal angepasst: Buch weniger hochgezogen und mittig ausgerichtet","Fixkosten-Symbol (Tausch-Pfeile) korrigiert - Pfeilspitzen trafen die Linien vorher nicht ganz exakt")),
    NativeChange(55,"20.09.2026, 14:34 Uhr",listOf("Übersicht-Karten (Rechnungen, Spartopf, Kredite, Tilgungsplan) zeigen jetzt alle dasselbe helle Blau wie \"Monatlicher Rest\", statt jeweils eigener Farben","App-Icon in demselben Blauton eingefärbt, Haus/Euro-Zeichen in kräftigerem Dunkelblau für bessere Lesbarkeit")),
    NativeChange(56,"20.09.2026, 16:46 Uhr",listOf("Hero-Karten innerhalb der Reiter (Rechnungen, Spartopf, Kredite, Tilgungsplan, Tanken) jetzt ebenfalls einheitlich in Blau, statt Gold/Grün/Rot/Lila/Grau")),
    NativeChange(57,"20.09.2026, 16:48 Uhr",listOf("Tilgungsplan: Prozent-Chips (0/25/50/75/100%) und der Extra-Betrag-Eingabe-Rahmen jetzt auch in Blau statt Lila")),
    NativeChange(58,"20.09.2026, 16:51 Uhr",listOf("\"Tanken diesen Monat\"-Karte in der Übersicht jetzt ebenfalls in Blau statt Grau")),
    NativeChange(59,"20.09.2026, 18:26 Uhr",listOf("Jahres-Auswahl beim Verlauf Spartopf jetzt als Chips statt Dropdown, wie beim Extra-Betrag im Tilgungsplan - bricht bei vielen Jahren automatisch in eine neue Zeile um")),
    NativeChange(60,"20.09.2026, 18:37 Uhr",listOf("Verlauf Spartopf: die automatische \"Übertrag aus Vorjahr(en)\"-Anzeige bleibt erhalten, das manuelle Eingabefeld dafür ganz unten in der Liste wurde entfernt")),
    NativeChange(61,"20.09.2026, 19:03 Uhr",listOf("Neuer Reiter \"Beta\": eigenständiger Buchungs-Tracker ohne Verknüpfung zu anderen Reitern - Monat/Jahr-Navigation, Einträge nach Tag gruppiert mit Tagessaldo, laufender Kontostand unten","Eintragen-Formular mit Titel-Vorschlägen (Autovervollständigung) - wählst du einen bekannten Titel, wird der zuletzt eingetragene Betrag automatisch übernommen","Ausgabe/Einnahme/Umbuchung, Kategorie, Dauerauftrag- und Abgeglichen-Kennzeichnung (Dauerauftrag erzeugt noch keine automatischen Folgeeinträge)")),
    NativeChange(62,"20.09.2026, 19:08 Uhr",listOf("Beta: Dauerauftrag erzeugt jetzt automatisch echte Folgebuchungen - mit Periode (wöchentlich bis jährlich) und optionalem Ablaufdatum, ca. 2 Monate im Voraus")),
    NativeChange(63,"20.09.2026, 19:12 Uhr",listOf("Beta: Kontostand-Leiste überschnitt sich mit der Tab-Leiste unten - Position wird jetzt dynamisch anhand der tatsächlichen Höhe berechnet, statt mit einem festen Wert")),
    NativeChange(64,"20.09.2026, 19:14 Uhr",listOf("Beta: Der Kreis bei jedem Eintrag lässt sich jetzt direkt antippen, um \"Abgeglichen\" umzuschalten, ohne den Eintrag extra öffnen zu müssen")),
    NativeChange(65,"20.09.2026, 19:51 Uhr",listOf("Beta: Zeitzonen-Fehler bei Dauerauftrag behoben - der nächste Termin landete durch die Zeitumstellung teils auf einem falschen Tag und die Folgemonate fehlten dadurch","Beta: Plus-Button überschnitt sich mit der Kontostand-Leiste - Position wird jetzt ebenfalls dynamisch berechnet")),
    NativeChange(66,"20.09.2026, 20:05 Uhr",listOf("Beta: Beim Bearbeiten eines Dauerauftrag-Eintrags fragt die App jetzt, ob nur dieser eine oder dieser und alle folgenden Einträge geändert werden sollen")),
    NativeChange(67,"20.09.2026, 20:46 Uhr",listOf("Beta: CSV-Import hinzugefügt (Datum/Titel/Hauptkategorie/Betrag/Abgeglichen) - importiert alle Zeilen als eigenständige Beta-Einträge, ohne andere Bereiche der App zu berühren","Beta: Dauerauftrag erzeugt jetzt auch Termine für weiter in der Zukunft liegende Monate, sobald man im Kalender dorthin vorblättert, statt nur die nächsten 2 Monate ab heute","Beta: Titel-Vorschläge zeigen jetzt erst nach Eingabe eine gefilterte Liste passender Titel an, statt einer Dropdown-Liste mit Pfeil, die alle Titel sofort anzeigt")),
    NativeChange(68,"20.09.2026, 21:02 Uhr",listOf("Beta: Pfeile, Monat und Jahr bleiben jetzt beim Runterscrollen oben fest sichtbar","Beta: Monat und Jahr antippen öffnet jetzt jeweils eine eigene Auswahl, statt nur über die Pfeile durchzublättern")),
    NativeChange(69,"20.09.2026, 21:04 Uhr",listOf("CSV-Import für Beta aus dem Beta-Tab entfernt und stattdessen als eigener Button in Einstellungen → Backup verschoben")),
    NativeChange(70,"20.09.2026, 21:08 Uhr",listOf("Wichtiger Backup-Fix: Tanken (Autos/Tankvorgänge) und Beta fehlten bisher komplett in der Sicherung/Wiederherstellung - das hätte beim Wiederherstellen zu Fehlern in diesen Bereichen führen können. Ab jetzt werden beide vollständig mit gesichert")),
    NativeChange(71,"20.09.2026, 21:14 Uhr",listOf("Neues 4x1-Homescreen-Widget für Beta: zeigt den aktuellen Kontostand, \"+\" öffnet direkt eine neue Einnahme, \"-\" eine neue Ausgabe, Antippen der Zahl öffnet den Beta-Tab")),
    NativeChange(72,"20.09.2026, 21:18 Uhr",listOf("Beta: Monat/Jahr-Anzeige überlappte sich teils bei wenig Platz - Layout jetzt robuster gegen dieses Zusammenquetschen")),
    NativeChange(73,"20.09.2026, 21:23 Uhr",listOf("Beta: eigentliche Ursache des Monat/Jahr-Problems behoben - eine alte CSS-Regel für die Pfeile hatte versehentlich auch die neuen Monat/Jahr-Buttons auf 40×40px zusammengequetscht","Beta-Widget: Kontostand-Zahl deutlich größer und je nach Vorzeichen rot oder grün eingefärbt")),
    NativeChange(74,"20.09.2026, 21:27 Uhr",listOf("Beta-Widget: Anordnung geändert - Plus/Minus jetzt zusammen links, Kontostand rechts (wie beim Referenz-Widget)","Beta-Widget passt sich jetzt automatisch an Hell-/Dunkelmodus des Geräts an")),
    NativeChange(75,"20.09.2026, 21:29 Uhr",listOf("Beta: \"Umbuchung\"-Option entfernt, da es nur ein Konto gibt und diese Auswahl keinen Sinn ergibt")),
    NativeChange(76,"20.09.2026, 21:35 Uhr",listOf("Übersicht ist jetzt ein Button oben im Header (neben Diagramm) statt ein Reiter unten - öffnet sich als eigenes Fenster","\"Beta\" ist umbenannt in \"Kontostand\" und steht jetzt an der ersten Stelle unten, dort wo vorher Übersicht war (inkl. Widget-Beschriftung)")),
    NativeChange(77,"20.09.2026, 21:38 Uhr",listOf("Kontostand-Icon in der Tab-Leiste jetzt drei Münzstapel statt Dokument-Symbol")),
    NativeChange(78,"20.09.2026, 21:44 Uhr",listOf("Kontostand-Widget: Plus/Minus größer und mit mehr Abstand zueinander, Betrag ebenfalls größer, alles sauber bündig ausgerichtet")),
    NativeChange(79,"20.09.2026, 21:46 Uhr",listOf("Kontostand-Widget: Innenabstand oben/unten verringert, damit Symbole und Zahl näher an den Rand reichen (Gesamthöhe bleibt gleich)")),
    NativeChange(80,"20.09.2026, 22:57 Uhr",listOf("Kontostand-Widget grundlegend überarbeitet: Pillenform statt Karte, Plus/Minus jetzt als Ringe statt Vollflächen (angelehnt an den Akku-/Zähler-Look)","Jetzt in drei wählbaren Größen verfügbar (2×1, 3×1, 4×1) - beim Hinzufügen zum Homescreen erscheinen alle drei einzeln mit echter Live-Vorschau statt nur dem App-Symbol")),
    NativeChange(81,"20.09.2026, 23:03 Uhr",listOf("Kontostand-Widget-Hintergrund jetzt leicht transparent statt komplett blickdicht, wie beim Referenz-Widget")),
    NativeChange(82,"20.09.2026, 23:06 Uhr",listOf("Kontostand-Widget: Höhe deutlich reduziert (alle drei Größen), Symbole/Schrift entsprechend kleiner, näher am Referenz-Widget")),
    NativeChange(83,"20.09.2026, 23:10 Uhr",listOf("Kontostand-Widget: Plus/Minus-Größe und Schrift wieder auf die vorherige, größere Version zurückgesetzt - nur der äußere Balken bleibt niedriger")),
    NativeChange(84,"20.09.2026, 23:14 Uhr",listOf("Kontostand-Widget: äußerer Balken nochmal etwas niedriger, Plus/Minus-Größe unverändert gelassen")),
    NativeChange(85,"20.09.2026, 23:20 Uhr",listOf("Kontostand-Widget: Höhe manuell per Ziehen anpassbar (vorher nur Breite) - Android-Launcher binden die Höhe oft an eine feste Raster-Zeile, die sich über minHeight allein nicht unterschreiten lässt")),
    NativeChange(86,"21.09.2026, 08:32 Uhr",listOf("Kontostand: nicht abgeglichene Einträge sind jetzt gelb hervorgehoben, neue Filter-Chips \"Alle\"/\"Nicht abgeglichen\" oben in der Liste, sowie ein roter Zähl-Punkt am Kontostand-Symbol in der Tab-Leiste (zählt über alle Monate hinweg)")),
    NativeChange(87,"21.09.2026, 08:35 Uhr",listOf("Kontostand: Filter-Chips \"Alle\"/\"Nicht abgeglichen\" haben jetzt passende Symbole, wie bei allen anderen Chips in der App")),
    NativeChange(88,"21.09.2026, 08:43 Uhr",listOf("Kontostand: roter Zähl-Punkt am Tab-Symbol und gelbe Zeilen-Hervorhebung bei nicht abgeglichenen Einträgen wieder entfernt - die Filter-Chips bleiben")),
    NativeChange(89,"21.09.2026, 08:46 Uhr",listOf("Kontostand: Filter-Chips sehen jetzt genauso aus wie die Chips bei anderen Tabs (z.B. Jahr-Auswahl im Spartopf) - helles Blau bei Auswahl statt eigenem Stil")),
    NativeChange(90,"21.09.2026, 09:24 Uhr",listOf("Kontostand: Filter-Auswahl jetzt exakt im Segmented-Control-Stil wie bei Buchungen (Rechnungen/Einnahmen/Fixkosten) - eigener Rahmen mit grauem Hintergrund statt einzelner blauer Pillen")),
    NativeChange(91,"21.09.2026, 09:38 Uhr",listOf("Kontostand: Seitliches Wischen wechselt jetzt auch zwischen \"Alle\" und \"Nicht abgeglichen\", wie bei den anderen Unteransichten","Kontostand: Beim Markieren eines Eintrags als abgeglichen fragt die App jetzt erst nach, um versehentliches Abgleichen zu vermeiden - beim Zurücksetzen bleibt es ohne Nachfrage")),
    NativeChange(92,"21.09.2026, 10:00 Uhr",listOf("Kontostand: Monat/Pfeile bleiben beim Scrollen zuverlässiger oben (höherer z-index)","Kontostand-Widget zeigt jetzt immer den Kontostand des aktuellen Monats, unabhängig davon, welcher Monat gerade in der App durchgeblättert wird","Kontostand springt beim Zurückkehren zur App (z.B. nach dem Wechsel zu einer anderen App) automatisch wieder auf den aktuellen Monat")),
    NativeChange(93,"21.09.2026, 10:04 Uhr",listOf("Kontostand: Einträge kompakter gestaltet, Tages-Kopfzeile jetzt mit Kästchen-Datum, \"Tagessaldo\" und vollem Datum inkl. Wochentag - angelehnt an das Referenz-Design")),
    NativeChange(94,"21.09.2026, 10:10 Uhr",listOf("Kontostand: Monat/Jahr/Pfeile verschwanden beim Scrollen tatsächlich hinter dem (ebenfalls fest stehenden) Header - jetzt fest positioniert direkt unterhalb des Headers, verschwindet nicht mehr")),
    NativeChange(95,"21.09.2026, 10:13 Uhr",listOf("Kontostand: Abstand zwischen den Tages-Gruppen verringert, \"Nicht zugeordnet\" wird nicht mehr angezeigt (nur echte Kategorien) - Einträge ohne Kategorie sind dadurch einzeilig und die Liste insgesamt kompakter")),
    NativeChange(96,"21.09.2026, 10:21 Uhr",listOf("Kontostand: Tagessaldo-Kopfzeile verschmilzt jetzt mit den Einträgen darunter (kein grauer Hintergrund mehr dazwischen) und ist deutlich kleiner, die einzelnen Einträge dafür minimal größer")),
    NativeChange(97,"21.09.2026, 10:25 Uhr",listOf("Kontostand: Hintergrund der Tagessaldo-Kopfzeile jetzt im App-Blau, damit man einen neuen Tag sofort erkennt (Beträge zeigen weiterhin Rot/Grün je nach Vorzeichen)")),
    NativeChange(98,"21.09.2026, 10:32 Uhr",listOf("Kontostand: Tagessaldo-Kopfzeile wieder auf die ursprüngliche, größere Größe zurückgesetzt")),
    NativeChange(99,"21.09.2026, 10:36 Uhr",listOf("Kontostand: Kleiner Spalt zwischen Header und Monats-Leiste behoben, durch den Inhalt dahinter durchschimmerte")),
    NativeChange(100,"21.09.2026, 11:52 Uhr",listOf("Kontostand: Neu hinzugefügte Einträge stehen jetzt oben am jeweiligen Tag, statt unten","Einstellungen: Neuer Bereich \"Kontostand: Dauerauftrag-Vorlagen\" zum Verwalten und Löschen nicht mehr benötigter Dauerauftrag-Vorlagen")),
    NativeChange(101,"21.09.2026, 11:56 Uhr",listOf("Einstellungen: Neuer Bereich \"Kontostand: Titel-Vorschläge\" zum Ausblenden nicht mehr benötigter Vorschläge (z.B. alte Titel wie \"O2\"), ohne die zugehörigen Einträge zu löschen - ausgeblendete lassen sich auch wiederherstellen","Kontostand: Tippt man auf den automatisch befüllten Betrag, springt er jetzt auf leer statt den alten Wert stehen zu lassen")),
    NativeChange(102,"21.09.2026, 12:02 Uhr",listOf("Einstellungen: CSV-Import, Dauerauftrag-Vorlagen und Titel-Vorschläge für Kontostand haben jetzt einen eigenen Menüpunkt \"Kontostand\", statt unter Backup zu stehen")),
    NativeChange(103,"21.09.2026, 12:05 Uhr",listOf("Kontostand: Eine Vorlage aus den Dauerauftrag-Vorlagen zu entfernen löscht nicht mehr den zugrundeliegenden Eintrag - er bleibt als normale Buchung erhalten, nur die Wiederholung wird gestoppt")),
    NativeChange(104,"21.09.2026, 13:21 Uhr",listOf("Einstellungen: Dauerauftrag-Vorlagen und Titel-Vorschläge sind jetzt einklappbar","Kontostand: Ausgeblendete Titel-Vorschläge verschwinden jetzt aus der Hauptliste und stehen stattdessen in einem eigenen \"Ausgeblendet\"-Bereich zum Wiederherstellen")),
    NativeChange(105,"21.09.2026, 15:36 Uhr",listOf("Großer Design-Umbau: \"Liquid Glass\"-Optik über die ganze App - durchscheinende, mattierte Karten mit Blur-Effekt und Lichtrand über einem Grau-Verlauf-Hintergrund mit weichen Farbflecken, sowohl im Hell- als auch im Dunkelmodus. Betrifft Header, Tab-Leiste, Hero-Karten, Listen, Buttons und Einstellungen-Fenster gleichermaßen")),
    NativeChange(106,"21.09.2026, 15:47 Uhr",listOf("Liquid Glass: Kontraste verbessert - Karten sind jetzt deutlich blickdichter (weniger durchsichtig), Blur-Stärke reduziert, graue Texte etwas dunkler (hell) bzw. heller (dunkel) für bessere Lesbarkeit")),
    NativeChange(107,"21.09.2026, 16:29 Uhr",listOf("Kontostand-Widget: Hintergrund-Pille komplett entfernt (nur noch Plus/Minus-Ringe und Zahl sichtbar direkt auf dem Homescreen), Beschriftung \"Kontostand\" entfernt, Zahl hat jetzt eine eigene dünne Pillen-Umrandung in Rot/Grün je nach Vorzeichen, passend zur App-Farbe für Hell/Dunkel")),
    NativeChange(108,"21.09.2026, 16:36 Uhr",listOf("Kontostand-Widget: Innenabstand am Rand verringert (wird nicht mehr für eine Karte gebraucht) und Plus/Minus-Ringe dadurch etwas größer")),
    NativeChange(109,"21.09.2026, 16:59 Uhr",listOf("Kontostand-Widget: 3×1 und 2×1 nutzen jetzt dieselben Proportionen wie 4×1 (gleich große Ringe und Schrift) - nur die Breite passt sich an, 2×1 braucht dafür etwas mehr Mindestbreite")),
    NativeChange(110,"21.09.2026, 17:58 Uhr",listOf("Kontostand: \"Eintrag löschen\"-Button im Bearbeiten-Formular sieht jetzt genauso aus wie \"Speichern\" (gleiche Pillenform/Größe), nur in Rot statt Blau")),
    NativeChange(111,"21.09.2026, 19:45 Uhr",listOf("Kredite: Neues optionales Feld \"Gesamtbetrag inkl. Zinsen\" beim Anlegen - daraus wird intern der Zinssatz berechnet, damit der Tilgungsplan zwischen Zins- und Tilgungsanteil jeder Rate unterscheidet und genauer rechnet. Ohne Angabe bleibt alles wie bisher (lineare Berechnung)","Kredite: Laufzeit und Monatsrate stehen jetzt nebeneinander im Formular, jeweils mit eigener Beschreibung")),
    NativeChange(112,"21.09.2026, 19:48 Uhr",listOf("Kredite: Beim tatsächlichen Abhaken einer Zahlung wird jetzt (bei hinterlegtem Zinssatz) ebenfalls zwischen Zins- und Tilgungsanteil unterschieden, statt den kompletten Betrag von der Restschuld abzuziehen - Sondertilgungen über die reguläre Rate hinaus reduzieren weiterhin die Restschuld voll")),
    NativeChange(113,"21.09.2026, 19:52 Uhr",listOf("Kredite: \"Gesamtbetrag inkl. Zinsen\" lässt sich jetzt auch beim Bearbeiten eines bereits bestehenden Kredits nachtragen, nicht nur beim Neuanlegen")),
    NativeChange(114,"21.09.2026, 19:56 Uhr",listOf("Kredite: Rate/Monat-Feld beim Neuanlegen war falsch ausgerichtet (schwebte versetzt neben Laufzeit) - jetzt korrekt ausgerichtet","Kredite: Abbezahlte Kredite lassen sich jetzt wiederherstellen (Restschuld wird auf die volle Kreditsumme zurückgesetzt, gilt wieder als offen)")),
    NativeChange(115,"21.09.2026, 20:00 Uhr",listOf("Kredite: Formular-Felder neu gruppiert - Kreditsumme und Gesamtbetrag inkl. Zinsen stehen jetzt nebeneinander, Laufzeit und Rate/Monat darunter ebenfalls nebeneinander (beim Neuanlegen und Bearbeiten)")),
    NativeChange(116,"21.09.2026, 20:21 Uhr",listOf("Kredite: Alle vier Aktions-Buttons (Bearbeiten, Erhöhen, Rate buchen, Löschen) haben jetzt eine kleine Beschriftung darunter","Kredite: Die angezeigte Restlaufzeit \"wackelt\" nicht mehr bei jeder kleinen Sondertilgung - Extra-Beträge werden erst berücksichtigt, sobald sie zusammen mindestens eine volle Rate ergeben. Die tatsächliche Restschuld bleibt davon unberührt und ist immer exakt")),
    NativeChange(117,"21.09.2026, 20:27 Uhr",listOf("Wichtiger Fix bei Krediten mit Zinssatz: Kleine, separat gebuchte Extra-Beträge (z.B. eine 3€-Testbuchung) wurden fälschlich nochmal mit Zinsen für denselben Zeitraum belastet und dadurch teils gar nicht von der Restschuld abgezogen - jetzt reduzieren Beträge unter der regulären Rate die Restschuld immer komplett","Fix: Eine Buchung zu löschen rechnete bisher den vollen Zahlungsbetrag zurück, auch wenn wegen der Zinsberechnung tatsächlich weniger abgezogen worden war - das führte zu einer zu hohen Restschuld nach dem Löschen. Wird jetzt korrekt zurückgerechnet")),
    NativeChange(118,"21.09.2026, 20:55 Uhr",listOf("Kredite: Neuer \"Abgleichen\"-Button bei Krediten mit Zinssatz - trägst du die Restschuld laut Bank ein, berechnet die App daraus automatisch einen präziseren Zinssatz für künftige Buchungen, statt die Abweichung einfach nur einmalig zu überschreiben")),
    NativeChange(119,"21.09.2026, 21:00 Uhr",listOf("Kredite: \"Löschen\" ist bei fünf Buttons (mit \"Abgleichen\") in eine zweite Zeile gerutscht - Buttons jetzt kompakter, damit alle fünf nebeneinander in eine Zeile passen")),
    NativeChange(120,"21.09.2026, 21:08 Uhr",listOf("Kredite: Die fünf Aktions-Buttons waren zwar in einer Zeile, aber winzig und rechtsbündig mit viel Leerraum links - verteilen sich jetzt gleichmäßig über die ganze Breite und sind wieder etwas größer")),
    NativeChange(121,"21.09.2026, 23:13 Uhr",listOf("Kontostand: Filter-Chips (\"Alle\"/\"Nicht abgeglichen\") hatten versehentlich einen extra Rahmen vom Liquid-Glass-Umbau, den die Chips bei Buchungen nicht hatten - jetzt wieder identisch")),
    NativeChange(122,"21.09.2026, 23:17 Uhr",listOf("Alle Bearbeiten/Löschen/Sonstiges-Icon-Buttons in der ganzen App zeigen jetzt automatisch eine kleine Beschriftung darunter (wie bisher schon bei Kredite) - betrifft Einnahmen, Fixkosten, Rechnungen, Variable Kosten, Buchungen, Spartopf, Tanken und mehr")),
    NativeChange(123,"21.09.2026, 23:22 Uhr",listOf("Fix: Die automatischen Button-Beschriftungen aus Version 122 haben in der App gar nicht funktioniert (CSS-Ansatz wurde von der WebView nicht unterstützt) - jetzt über eine zuverlässigere Methode direkt im Code umgesetzt, betrifft wieder alle Icon-Buttons in der ganzen App inkl. Kredite")),
    NativeChange(124,"21.09.2026, 23:32 Uhr",listOf("Kontostand-Widget: Größe ist jetzt fest, nicht mehr ziehbar","Kontostand-Widget: Plus/Minus zu einem einzigen, größeren \"−\"-Button zusammengelegt (öffnet immer direkt \"Neue Ausgabe\") - der frei gewordene Platz kommt dem Betrag zugute, dadurch ist bei 3×1 jetzt auch das €-Zeichen zuverlässig sichtbar")),
    NativeChange(125,"21.09.2026, 23:35 Uhr",listOf("Kontostand-Widget: Button zeigt wieder \"+/-\" als Beschriftung (Funktion bleibt: öffnet immer \"Neue Ausgabe\")","Kontostand-Widget: Button und Betrags-Anzeige nutzen jetzt die tatsächlich verfügbare Höhe des Launchers komplett aus, statt sich an einer festen Mindestgröße zu orientieren")),
    NativeChange(126,"21.09.2026, 23:41 Uhr",listOf("Kontostand-Widget: match_parent-Höhe wieder zurückgenommen (hat auf Samsung-Launchern zu lang gezogenen Ovalen statt Kreisen geführt) - Button und Betrag haben jetzt wieder eine feste, kompakte Höhe, ähnlich dem Referenz-Widget","Kontostand-Widget: Button ist jetzt ein echter Kreis (gleich breit wie hoch) statt oval, und im App-Blau statt Rot")),
    NativeChange(127,"21.09.2026, 23:45 Uhr",listOf("Kontostand-Widget: Größe zwischen \"zu klein\" (52dp) und \"zu groß/oval\" (match_parent) gefunden - Button und Betrag jetzt bei 70dp (4×1), entsprechend kleiner bei 3×1/2×1, bleibt dabei ein echter Kreis")),
    NativeChange(128,"21.09.2026, 23:49 Uhr",listOf("Kontostand-Widget: Die 76dp-Mindesthöhe hat den Launcher dazu gebracht, statt einer Zeile zwei Zeilen zu belegen (z.B. \"3×2\" statt \"3×1\") - Mindesthöhe auf 66dp reduziert, Größe von Kreis/Betrag bleibt unverändert")),
    NativeChange(129,"21.09.2026, 23:55 Uhr",listOf("Kontostand-Widget: Neue Größe 5×1 hinzugefügt (nutzt dasselbe, bereits an die Breite anpassungsfähige 4×1-Layout - der Betrag bekommt dadurch noch mehr Platz)")),
    NativeChange(130,"22.09.2026, 12:39 Uhr",listOf("Kredite: Entfernst du \"Gesamtbetrag inkl. Zinsen\" bei einem Kredit wieder, wird die Restschuld jetzt automatisch neu und rein linear aus der kompletten Zahlungshistorie berechnet - dadurch verschwinden Reste einer vorherigen zinsbewussten Berechnung automatisch, ohne dass du selbst nachrechnen musst")),
    NativeChange(131,"22.09.2026, 12:43 Uhr",listOf("Wichtiger Fix: Die automatische Button-Beschriftung aus Version 122/123 hat dazu geführt, dass eingeklappte, eigentlich unsichtbare Beschriftungen als graue Textreste zwischen den Karten durchschimmerten (betraf Kredite, Variable Kosten und weitere Bereiche) - komplett zurückgenommen. Kredite behält seine Beschriftungen über einen anderen, bewährten Weg, alle anderen Bereiche haben vorerst wieder unbeschriftete Icons")),
    NativeChange(132,"22.09.2026, 12:53 Uhr",listOf("Der eigentliche Übeltäter für das Gekritzel war eine übersehene, nie richtig entfernte CSS-Regel aus einem noch früheren Versuch (Version 122) - die ist jetzt wirklich komplett weg. Betraf Variable Kosten, Kredite (abbezahlte) und alle anderen Bereiche mit Icon-Buttons")),
    NativeChange(133,"22.09.2026, 12:57 Uhr",listOf("Alle Icon-Buttons in der ganzen App (Einnahmen, Fixkosten, Rechnungen, Variable Kosten, Buchungen, Spartopf, Tanken, abbezahlte Kredite) haben jetzt Beschriftungen wie bisher schon bei Kredite - diesmal direkt und fest im Code statt automatisch, damit kein Gekritzel-Problem mehr auftreten kann")),
    NativeChange(134,"22.09.2026, 15:36 Uhr",listOf("Hintergrund war ein diagonaler Verlauf (oben hell, unten dunkler) - jetzt überall einheitlich im dunkleren Ton, wie er vorher nur unten sichtbar war")),
    NativeChange(135,"22.09.2026, 18:35 Uhr",listOf("Kontostand-Widget: Sichtschutz für den Betrag - er ist standardmäßig mit Punkten abgedeckt, ein Tipp deckt ihn für 3 Sekunden auf, danach wird er automatisch wieder verdeckt. Tippt man auf den (aufgedeckten) Betrag selbst, öffnet sich wie gewohnt die App")),
    NativeChange(136,"22.09.2026, 21:23 Uhr",listOf("Kontostand-Widget: Punkte-Abdeckung ist jetzt im selben Blau wie der +/- Button, statt Weiß/Grau")),
    NativeChange(137,"22.09.2026, 21:26 Uhr",listOf("Kontostand-Widget: Punkte-Abdeckung hat jetzt auch den blauen Rand, passend zum +/- Button")),
    NativeChange(138,"22.09.2026, 21:32 Uhr",listOf("Kontostand: Titel-Vorschläge beim Eintippen (das Dropdown unter dem Titel-Feld) sind jetzt deutlich blickdichter, statt der durchsichtigen Glas-Oberfläche - dadurch in Hell- und Dunkelmodus besser lesbar")),
    NativeChange(139,"23.09.2026, 09:10 Uhr",listOf("Neu: Kontostand als dauerhafte Benachrichtigung (Einstellungen → Kontostand → Benachrichtigung) - lässt sich nicht wegwischen, eingeklappt ist nur \"Kontostand\" sichtbar, der Betrag erscheint erst beim Herunterziehen. Ab Android 13 wird dafür einmalig die Benachrichtigungsberechtigung angefragt")),
    NativeChange(140,"23.09.2026, 09:24 Uhr",listOf("Kontostand-Benachrichtigung: Eigenes Design mit farbigem Rand links (Rot/Grün je nach Vorzeichen) und dem Betrag als farbige Pille im ausgeklappten Zustand, statt der Standard-Optik")),
    NativeChange(141,"23.09.2026, 09:30 Uhr",listOf("Kontostand-Benachrichtigung: Eigenes, einfaches weißes Symbol für die Statusleiste (oben neben der Uhrzeit), statt dem vollen, bunten App-Icon, das Android sonst unschön in einen weißen Klecks umwandelt")),
    NativeChange(142,"23.09.2026, 09:36 Uhr",listOf("Wichtiger Fix: Beim ersten Aktivieren der Kontostand-Benachrichtigung wurde die Anzeige versucht, bevor die Berechtigung überhaupt erteilt war (noch während der Dialog offen war) - sie erschien dadurch erst beim nächsten Kontostand-Update, statt sofort. Jetzt erscheint sie sofort, sobald du die Berechtigung erlaubst")),
    NativeChange(143,"23.09.2026, 09:46 Uhr",listOf("Kontostand-Benachrichtigung: Falls der Benachrichtigungs-Kanal durch frühere Tests blockiert wurde, hilft erneutes Erlauben der Berechtigung allein nicht (Android hebt eine Kanal-Blockierung nicht automatisch auf) - der Kanal wird jetzt frisch mit neuem Namen angelegt, dadurch wieder standardmäßig aktiviert. Zusätzlich gibt es jetzt eine Meldung, falls der Kanal doch noch blockiert sein sollte, statt dass einfach nichts passiert")),
    NativeChange(144,"23.09.2026, 10:17 Uhr",listOf("Kontostand-Benachrichtigung: Temporäre Diagnose-Meldungen eingebaut, die genau anzeigen, an welcher Stelle es hakt (Berechtigung, Kanal blockiert, oder ein technischer Fehler) - werden entfernt, sobald die Ursache gefunden ist")),
    NativeChange(145,"23.09.2026, 10:21 Uhr",listOf("Kontostand-Benachrichtigung: Wichtigkeitsstufe von \"Niedrig\" auf \"Standard\" erhöht - \"Niedrig\" landet bei manchen Android-Oberflächen (z.B. Samsung) in einem eingeklappten, leicht übersehbaren Bereich, obwohl sie technisch erfolgreich gesendet wurde")),
    NativeChange(146,"23.09.2026, 10:27 Uhr",listOf("Kontostand-Benachrichtigung: Zurück auf den einfachen, zuverlässigen Standard-Stil statt der custom Rand/Pillen-Optik - Custom-Layouts werden in einem anderen Systemprozess gerendert, ein Fehler dort kommt in der App nicht an (daher \"kein Fehler\" trotz unsichtbarer Benachrichtigung). Design bleibt: eingeklappt nur \"Kontostand\", Betrag erst beim Ausklappen")),
    NativeChange(147,"23.09.2026, 10:31 Uhr",listOf("Kontostand-Benachrichtigung: Statusleisten-Symbol war nur ein Quadrat (füllte fast die ganze Fläche) - jetzt ein Münzstapel-Symbol, passend zum Kontostand-Icon in der App")),
    NativeChange(148,"23.09.2026, 10:35 Uhr",listOf("Kontostand-Benachrichtigung: Farbliche Einfärbung (Rot/Grün je nach Vorzeichen) über die standardmäßig unterstützte Android-Funktion statt eigenem Layout - dadurch zuverlässiger, könnte je nach Android-Version aber trotzdem etwas anders aussehen als geplant")),
    NativeChange(149,"23.09.2026, 10:41 Uhr",listOf("Kontostand-Benachrichtigung: Farbliche Einfärbung wieder entfernt (Android ignoriert das bei normalen andauernden Benachrichtigungen ohne echten Hintergrunddienst) - bleibt bei der schlichten, dafür zuverlässig funktionierenden Standard-Optik")),
    NativeChange(150,"23.09.2026, 10:46 Uhr",listOf("Kontostand-Benachrichtigung: \"Haushaltsbuch+\" stand doppelt da (einmal automatisch von Android, einmal von der App selbst) - entfernt. Der Betrag im ausgeklappten Zustand ist jetzt außerdem größer und fett hervorgehoben")),
    NativeChange(151,"23.09.2026, 10:55 Uhr",listOf("Kontostand-Benachrichtigung: Die Textformatierung für den größeren Betrag hat sich nicht durchgesetzt - jetzt wird stattdessen der Betrag selbst zum großen Titel im ausgeklappten Zustand (eine zuverlässige Android-Funktion statt Textformatierungs-Tricks), \"Im Plus\"/\"Im Minus\" steht klein darunter")),
    NativeChange(152,"23.09.2026, 14:51 Uhr",listOf("Kontostand-Benachrichtigung: Komplett umgebaut auf einen echten Hintergrunddienst (wie bei Arbeitszeit+) - dadurch funktioniert die farbige Einfärbung (Rot/Grün) jetzt zuverlässig und ein größeres, custom Layout ist möglich. Wird auf Android 14+ die Benachrichtigung doch mal weggewischt, erscheint sie automatisch sofort wieder")),
    NativeChange(153,"23.09.2026, 15:02 Uhr",listOf("Kontostand-Benachrichtigung: Das ursprünglich geplante Design (weißer Akzent-Rand links + Betrag als halbtransparente weiße Pille) ist jetzt umgesetzt, da der neue Hintergrunddienst custom Layouts zuverlässig rendert")),
    NativeChange(154,"23.09.2026, 15:09 Uhr",listOf("Wichtiger Absturz-Fix: Der neue Kontostand-Hintergrunddienst konnte die App zum Abstürzen bringen, wenn Android das Starten aus dem Hintergrund verweigert (z.B. wenn die App gerade nicht aktiv geöffnet ist) - jetzt an mehreren Stellen abgesichert, damit sowas nie mehr zu einem Absturz führt, sondern die Aktualisierung höchstens einmal ausfällt")),
    NativeChange(155,"23.09.2026, 15:21 Uhr",listOf("Kontostand-Benachrichtigung: Design zurückgenommen auf den Stand vor dem Akzent-Rand/Pillen-Layout (wieder schlichter Titel/Betrag) - Absturz beim Deaktivieren/Aktivieren tritt vermutlich unabhängig vom Design auf und liegt am Hintergrunddienst selbst, wird weiter untersucht")),
    NativeChange(156,"23.09.2026, 15:27 Uhr",listOf("Kontostand-Benachrichtigung: Hintergrund ist jetzt wieder normal (keine komplette rote/grüne Einfärbung mehr) - nur der Betrag selbst ist in Rot/Grün eingefärbt")),
    NativeChange(157,"23.09.2026, 15:36 Uhr",listOf("Kontostand-Benachrichtigung: Textfarben-Markierung hat sich wieder nicht durchgesetzt - zurück auf die komplette Einfärbung des Hintergrunds (wie bei Arbeitszeit+), da das nachweislich zuverlässig funktioniert, reine Textfarben bei diesem Benachrichtigungstyp aber nicht")),
    NativeChange(158,"23.09.2026, 15:45 Uhr",listOf("Kontostand-Benachrichtigung: Einfärbung komplett entfernt, schlichte Optik ohne Rot/Grün","Statusleisten-Symbol von den drei Münzen auf ein Geldbeutel-Symbol umgestellt")),
    NativeChange(159,"23.09.2026, 16:01 Uhr",listOf("Wichtiger Fix: Beim Bearbeiten eines Dauerauftrags mit \"Diesen und alle folgenden\" und anschließendem Löschen wurde bisher nur der eine Termin entfernt - die Vorlage blieb aktiv und erzeugte im Folgemonat weiterhin neue Buchungen. Jetzt werden korrekt auch alle folgenden Termine entfernt und keine neuen mehr erzeugt, während bereits vergangene Termine der Serie erhalten bleiben")),
    NativeChange(160,"23.09.2026, 16:11 Uhr",listOf("Kontostand-Widget: Entfernst du ein Widget vom Homescreen, wird jetzt auch der zugehörige, nicht mehr gebrauchte gespeicherte Zustand (aufgedeckt/verdeckt) aufgeräumt, statt dauerhaft ungenutzt liegen zu bleiben")),
    NativeChange(161,"23.09.2026, 18:45 Uhr",listOf("Kontostand-Benachrichtigung: \"setColor\" war trotz entfernter Vollfärbung noch aktiv und sorgte auf manchen Android-Oberflächen für eine kräftige Einfärbung - jetzt bewusst als sehr leichte, pastellige Hintergrundfarbe (statt kräftigem Rot/Grün)")),
    NativeChange(162,"23.09.2026, 18:48 Uhr",listOf("Kontostand-Benachrichtigung: Neuer \"+/-\"-Aktions-Button (öffnet direkt \"Neue Ausgabe\", wie beim Widget) - die genaue Position innerhalb der Benachrichtigung bestimmt allerdings Android selbst, eine feste Ausrichtung \"ganz rechts\" lässt sich darüber nicht erzwingen")),
    NativeChange(163,"24.09.2026, 17:49 Uhr",listOf("Tilgungsplan: Prozent-Auswahl-Chips (Rate übertragen) hatten im Hellmodus zu wenig Kontrast zwischen ausgewähltem und nicht ausgewähltem Zustand - ausgewählte Chips jetzt in kräftigem Blau mit weißer Schrift")),
    NativeChange(164,"24.09.2026, 17:50 Uhr",listOf("Tilgungsplan: Nicht ausgewählte Prozent-Chips waren selbst kaum als Buttons erkennbar (zu weiß/unauffällig) - haben jetzt einen sichtbaren Rahmen")),
    NativeChange(165,"24.09.2026, 17:56 Uhr",listOf("Tilgungsplan: Ausgewählter Chip wieder zurück auf das ursprüngliche sanfte Blau (war schon vorher in Ordnung), Rahmen bei den nicht ausgewählten Chips bleibt")),
    NativeChange(166,"25.09.2026, 00:13 Uhr",listOf("Fix: Kurzes weißes Aufblitzen beim Öffnen der App - der Fenster-Hintergrund war noch auf eine alte, helle Farbe eingestellt und passte nicht mehr zum aktuellen (dunkleren) App-Hintergrund. Fenster- und WebView-Hintergrund sind jetzt für Hell- und Dunkelmodus separat richtig eingestellt")),
    NativeChange(167,"25.09.2026, 00:40 Uhr",listOf("Tab-Wechsel (Kontostand/Buchungen/Variable Kosten/Kredite/Tanken) hat den Inhalt bisher hart umgeschaltet - jetzt mit einer sanften Einblend-Animation, ähnlich wie bei iOS-Übergängen")),
    NativeChange(168,"25.09.2026, 18:29 Uhr",listOf("Kontostand: Unter der Monatsanzeige steht jetzt klein \"Einnahmen +X € · Ausgaben -Y €\" - nur für den aktuell angezeigten Monat")),
    NativeChange(169,"25.09.2026, 19:05 Uhr",listOf("Performance: Der Weichzeichner-Effekt (Liquid Glass) ist bei sich wiederholenden Karten (Kontostand-Tage, Kredite, Variable Kosten) entfernt - das ist eine der teuersten CSS-Eigenschaften und musste bei vielen gleichzeitig sichtbaren Karten laufend neu berechnet werden. Optisch kaum ein Unterschied (flacher Hintergrund dahinter), sollte sich aber beim Scrollen flüssiger anfühlen. Kopfzeile, Tab-Leiste und Chips behalten den Glas-Effekt")),
    NativeChange(170,"25.09.2026, 22:35 Uhr",listOf("Übersicht, Diagramm, Einstellungen und alle Formulare zum Hinzufügen neuer Einträge sind jetzt komplett blickdicht statt durchscheinend - das leichte Durchscheinen dort wurde als irritierend empfunden")),
    NativeChange(171,"25.09.2026, 22:37 Uhr",listOf("Tab-Wechsel hat jetzt eine richtungsabhängige Wisch-Animation (aktueller Inhalt wischt raus, neuer wischt rein) statt nur einem einfachen Einblenden - je nachdem, ob man zu einem \"weiter rechts\" oder \"weiter links\" liegenden Tab wechselt, wie bei iOS-Übergängen")),
    NativeChange(172,"25.09.2026, 22:44 Uhr",listOf("Wichtiger Fix: Die Wisch-Animation aus Version 171 hat das Layout durcheinandergebracht (Inhalt verschoben, Hero-Karte bei Kredite/Tanken falsch platziert) - komplett zurückgenommen, zurück auf die einfache, bewährte Einblend-Animation")),
    NativeChange(173,"25.09.2026, 22:49 Uhr",listOf("Tab-Wechsel: Einblend-Dauer verkürzt (0,22s auf 0,14s), wirkt dadurch etwas knackiger/direkter")),
    NativeChange(174,"25.09.2026, 22:54 Uhr",listOf("Wichtiger Kontrast-Fix: Beim Blickdicht-Machen von neuen Eintrag-Formularen und Übersicht/Diagramm/Einstellungen hatten diese versehentlich dieselbe Farbe wie viele ihrer eigenen Buttons/Eingabefelder (beide nutzten --paper) - dadurch waren Eingabefelder, Chips und Icon-Buttons darin kaum noch als eigene Elemente erkennbar. Jetzt mit eigener, unterscheidbarer Hintergrundfarbe behoben")),
    NativeChange(175,"25.09.2026, 22:57 Uhr",listOf("Nachgerechnet: Der Farbabstand aus Version 174 war mit nur 5% viel zu gering, um zuverlässig als getrennte Fläche erkennbar zu sein - jetzt deutlich stärkerer Abstand (ca. 13%), zusätzlich haben Eingabefelder in Formularen jetzt einen sichtbaren Rahmen als zusätzliche, robuste Absicherung, unabhängig vom Farbton")),
    NativeChange(176,"25.09.2026, 23:13 Uhr",listOf("Performance: Tab-Wechsel zu Kontostand hat bisher bei JEDEM Wechsel die Position von Monatsnavigation und Saldo-Leiste neu berechnet (erzwingt teure Layout-Neuvermessung) - das passiert jetzt nur noch beim App-Start und wenn sich die sichtbaren Tabs in den Einstellungen ändern, nicht mehr bei jedem einzelnen Wechsel. Sollte den Tab-Wechsel spürbar flüssiger machen")),
    NativeChange(177,"26.09.2026, 08:27 Uhr",listOf("Fix: Tab-Wechsel hat im Dunkelmodus kurz geblitzt, vermutlich durch die Positions-Optimierung aus Version 176 (veraltete zwischengespeicherte Position statt Neuberechnung) - Optimierung zurückgenommen, da sie ohnehin keinen spürbaren Geschwindigkeitsvorteil brachte")),
    NativeChange(178,"26.09.2026, 10:03 Uhr",listOf("Einnahmen und Fixkosten: Die kleine Titelzeile oben ist durch eine Hero-Karte ersetzt (wie bei Rechnungen/Spartopf) - zeigt Gesamtbetrag und Anzahl Einträge größer und prominenter an")),
    NativeChange(179,"26.09.2026, 10:07 Uhr",listOf("Fixkosten-Hero-Karte: Zusätzliche Anzeige \"Inkl. automatisch\" neben Einträge - zeigt den Gesamtbetrag inklusive der automatisch berechneten Posten (Kreditraten, Extra-Tilgung, Sparbedarf Variable Kosten)")),
    NativeChange(180,"26.09.2026, 10:09 Uhr",listOf("Variable Kosten - Kostenarten: Die kleine Titelzeile oben ist durch eine Hero-Karte ersetzt, zeigt monatlichen Sparbedarf und Anzahl Kostenarten")),
    NativeChange(181,"26.09.2026, 10:14 Uhr",listOf("Kostenarten-Hero-Karte: Zusätzliche Anzeige \"Gesamt Puffer\" neben Kostenarten - zeigt direkt, wie viel im Spartopf übrig ist (wird automatisch mit jeder Ausgabe verrechnet)")),
    NativeChange(182,"26.09.2026, 10:27 Uhr",listOf("Kostenarten-Hero-Karte: \"Gesamt Puffer\"-Anzeige wieder entfernt","Spartopf-Karte: \"Sparbedarf/Monat\" durch \"Ausgegeben\" ersetzt (zeigt, was bisher aus dem Spartopf bezahlt wurde)")),
    NativeChange(183,"26.09.2026, 16:20 Uhr",listOf("Fix: Bearbeiten/Löschen-Buttons (neutrale Icon-Buttons) hatten im Hellmodus zu wenig Kontrast zur jeweiligen Karte und gingen darin unter - haben jetzt einen sichtbaren Rahmen, unabhängig davon, in welcher Karte sie gerade sitzen")),
    NativeChange(184,"26.09.2026, 16:25 Uhr",listOf("Nachgerechnet: Der Rahmen aus Version 183 nutzte dieselbe Trennlinienfarbe wie der Button-Hintergrund selbst und war damit fast unsichtbar (nur 3% Unterschied) - jetzt deutlich kräftigerer, klar sichtbarer Rahmen. Betrifft auch die Eingabefelder in Formularen, die denselben Fehler hatten")),
    NativeChange(185,"26.09.2026, 16:30 Uhr",listOf("Jeder Button in der ganzen App hat jetzt einen dünnen, halbtransparenten Rahmen - sorgt für eine klar erkennbare Kontur, unabhängig von der eigenen Hintergrundfarbe des Buttons")),
    NativeChange(186,"26.09.2026, 16:32 Uhr",listOf("Kredite: \"Laufzeit in Monaten\" und \"Rate pro Monat\" waren beim Neuanlegen und Bearbeiten unterschiedlich hoch ausgerichtet (die längere Laufzeit-Beschriftung brach um, die kürzere Rate-Beschriftung nicht) - jetzt beide auf gleicher Höhe")),
    NativeChange(187,"26.09.2026, 16:37 Uhr",listOf("Der Rahmen bei allen Buttons ist wieder komplett entfernt - stattdessen haben die Bearbeiten/Löschen-Buttons jetzt kräftigere, klar unterscheidbare Farben (statt eines Rahmens), sowohl im Hell- als auch im Dunkelmodus")),
    NativeChange(188,"26.09.2026, 16:42 Uhr",listOf("Kontrast-Nachprüfung: \"Abbrechen\"-Buttons in Bearbeiten-Formularen, der Info-Button beim Tilgungsplan und der Lösch-Button bei Kreditraten hatten denselben grenzwertigen Kontrast wie zuvor die Bearbeiten-Buttons - alle jetzt mit denselben kräftigeren Farben behoben")),
    NativeChange(189,"26.09.2026, 17:11 Uhr",listOf("Fix: Löschen-Button bei unbezahlten Krediten war schlecht sichtbar, weil die ganze Karte bei unbezahlten Krediten dieselbe rote Tönung bekommt wie der Button selbst - Löschen-Button jetzt in vollem Rot mit weißem Symbol statt zartem Rot-auf-Rot. Gleiches beim goldenen \"Erhöhen\"-Button behoben (gleiches Problem bei der Bearbeiten-Zeile)")),
    NativeChange(190,"26.09.2026, 17:16 Uhr",listOf("\"Als bezahlt buchen\" (Variable Kosten) und \"Rate buchen\" (Kredite) waren in beiden Modi grenzwertig sichtbar, im Dunkelmodus besonders schwach - jetzt volles Blau mit weißem Symbol statt zartem Blau-auf-Blau")),
    NativeChange(191,"26.09.2026, 21:50 Uhr",listOf("Spartopf: Beim \"+\"-Button kann jetzt zwischen Einzahlung und Ausgabe gewählt werden - bei Ausgabe mit frei wählbarer Bezeichnung, ohne dass sie an eine bestimmte Kostenart gebunden sein muss")),
    NativeChange(192,"26.09.2026, 22:26 Uhr",listOf("Fix: Das Kalender-Symbol bei Datumsfeldern war im Dunkelmodus unsichtbar (dunkles Symbol auf dunklem Hintergrund) - jetzt in allen Datumsfeldern der App farblich invertiert und damit gut sichtbar")),
    NativeChange(193,"27.09.2026, 20:03 Uhr",listOf("Offene Rechnungen: Zeilen sind jetzt eingefärbt wie bei Krediten - rot bei Fälligkeit in diesem Monat, grün bei Fälligkeit in einem anderen Monat, bezahlte Rechnungen bleiben ungefärbt")),
    NativeChange(194,"28.09.2026, 22:39 Uhr",listOf("Tanken: Beim Hinzufügen eines Tankvorgangs kann jetzt direkt im Formular das Auto ausgewählt werden - vorher wurde automatisch das gerade angezeigte Auto verwendet, was bei \"Gesamt\"-Ansicht sogar fehlerhaft sein konnte")),
    NativeChange(195,"28.09.2026, 22:43 Uhr",listOf("Fix: Die Buttons \"Tankvorgang\"/\"Auto hinzufügen\" im \"+\"-Aufklapp-Menü hatten einen seltsamen rechteckigen Rahmen - der Glas-Effekt wurde irrtümlich auf den reinen Layout-Container (Icon+Text) statt auf ein sichtbares Element angewendet, jetzt entfernt")),
    NativeChange(196,"28.09.2026, 22:49 Uhr",listOf("Das separate \"Diagramm\"-Menü ist aufgelöst: Einnahmen/Ausgaben- und Rechnungen-Diagramm gibt es jetzt als eigenen Unterreiter bei Buchungen, das Kredit-Tilgungsdiagramm als eigenen Unterreiter bei Kredite - direkt bei den zugehörigen Daten statt in einem separaten Menü")),
    NativeChange(197,"28.09.2026, 22:53 Uhr",listOf("Das separate \"Übersicht\"-Menü ist entfernt (samt zugehöriger Einstellungen-Sektion) - der \"Monatliche Rest\" (Einnahmen minus alle Ausgaben) steht stattdessen jetzt direkt in der Einnahmen-Hero-Karte")),
    NativeChange(198,"28.09.2026, 22:58 Uhr",listOf("Der Trend-Verlauf (kleines Diagramm unter dem Titel) ist entfernt, da er nie richtig funktioniert hat")),
    NativeChange(199,"28.09.2026, 23:04 Uhr",listOf("Kredite: Der \"Diagramm\"-Unterreiter ist wieder entfernt - stattdessen zeigt die Kredite-Hero-Karte jetzt einen Fortschrittsbalken mit dem gesamten Prozentsatz aller Kredite zusammen, genau wie bei einzelnen Kreditkarten")),
    NativeChange(200,"28.09.2026, 23:09 Uhr",listOf("Buchungen: Der \"Diagramm\"-Unterreiter ist auch hier wieder entfernt - stattdessen zeigt ein durchgehender rot/grüner Balken unter dem Titel \"Haushaltsbuch+\" das Verhältnis von Einnahmen zu Ausgaben auf einen Blick")),
    NativeChange(201,"28.09.2026, 23:18 Uhr",listOf("Der Einnahmen/Ausgaben-Balken ist etwas höher und zeigt jetzt direkt die Euro-Beträge im Balken selbst - bei sehr schmalen Segmenten (unter 20%) wird der Betrag ausgeblendet, damit nichts überläuft")),
    NativeChange(202,"29.09.2026, 12:45 Uhr",listOf("Der Einnahmen/Ausgaben-Balken klappt jetzt sanft ein, wenn man nach unten scrollt, und wieder aus, wenn man nach oben scrollt oder ganz oben ankommt - gibt mehr Platz für Inhalte beim Scrollen"))
)

internal enum class Tab(val label:String){BALANCE("Kontostand"),BOOKINGS("Buchungen"),VARIABLE("Variable Kosten"),LOANS("Kredite"),FUEL("Tanken")}

@Composable private fun NativeApp(onThemeChanged:()->Unit,initialWidgetAction:String?,onWidgetActionConsumed:()->Unit){val Blue=appBlue(LocalAppDark.current);
    val activity=androidx.compose.ui.platform.LocalContext.current as Activity
    val repo=remember{HouseholdRepository(activity)}
    var state by remember{mutableStateOf(JSONObject())}
    var loaded by remember{mutableStateOf(false)}
    LaunchedEffect(repo){state=withContext(Dispatchers.IO){repo.load()};loaded=true}
    if(!loaded){Text("Haushaltsbuch+ wird geöffnet…",Modifier.fillMaxWidth().padding(40.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(LocalAppDark.current));return}
    val dataRevision=repo.revision
    SideEffect { if(activity is MainActivity){activity.nativeStateForLifecycle=state;activity.nativeRepoForLifecycle=repo} }
    ;var startupWhatsNew by remember{mutableStateOf<List<NativeChange>>(emptyList())};var freshRestorePrompt by remember{mutableStateOf(false)};var freshRestorePending by remember{mutableStateOf(false)};var freshRestoreMessage by remember{mutableStateOf<String?>(null)};var freshRestoreData by remember{mutableStateOf<JSONObject?>(null)};var tab by remember{mutableStateOf(Tab.BALANCE)};var visibilityRevision by remember{mutableStateOf(0)};val tabVisibility=remember(visibilityRevision){runCatching{JSONObject(repo.getSetting("household-tab-visibility")?:"{}")}.getOrDefault(JSONObject())};val visibleTabs=Tab.entries.filter{t->val k=when(t){Tab.BALANCE->"beta";Tab.BOOKINGS->"buchungen";Tab.VARIABLE->"variabel";Tab.LOANS->"kredite";Tab.FUEL->"tanken"};if(tabVisibility.has(k))tabVisibility.optBoolean(k,true) else true}.ifEmpty{listOf(Tab.BALANCE)};LaunchedEffect(visibleTabs){if(tab !in visibleTabs)tab=visibleTabs.first()};var addBeta by remember{mutableStateOf(false)};var betaInitialType by remember{mutableStateOf<String?>(null)};var editBetaId by remember{mutableStateOf<String?>(null)};var editBetaScope by remember{mutableStateOf("this")};var betaScopeChoiceId by remember{mutableStateOf<String?>(null)};var reconcileConfirmId by remember{mutableStateOf<String?>(null)};var addForm by remember{mutableStateOf<String?>(null)};var fabOpen by remember{mutableStateOf(false)};var settings by remember{mutableStateOf(false)};var month by remember{mutableStateOf(YearMonth.now())};var headerBarCollapsed by remember{mutableStateOf(false)};var fabAllowed by remember{mutableStateOf(true)};var bookingsView by remember{mutableStateOf("rechnungen")};var variableView by remember{mutableStateOf("kostenarten")};var loanView by remember{mutableStateOf("kredite")};var fuelCar by remember{mutableStateOf("__all__")}
    LaunchedEffect(tab){headerBarCollapsed=false;fabOpen=false;if(tab!=Tab.LOANS)fabAllowed=true}
    fun persist(){repo.save(state)}
    fun updateWidgetBalance(){val entries=repo.array(state,"betaEntries").objects();val balance=betaBalanceAt(entries,YearMonth.now().atEndOfMonth());activity.getSharedPreferences(BetaWidgetProvider.PREFS_NAME,Context.MODE_PRIVATE).edit().putString(BetaWidgetProvider.KEY_BALANCE,money(balance)).putBoolean(BetaWidgetProvider.KEY_POSITIVE,balance>=0).apply();BetaWidgetProvider.updateAll(activity);if(activity.getSharedPreferences(BetaWidgetProvider.PREFS_NAME,Context.MODE_PRIVATE).getBoolean("kontostand_notification_enabled",false)){runCatching{ContextCompat.startForegroundService(activity,Intent(activity,KontostandForegroundService::class.java).putExtra(KontostandForegroundService.EXTRA_BALANCE_TEXT,money(balance)).putExtra(KontostandForegroundService.EXTRA_IS_POSITIVE,balance>=0))}}}
    LaunchedEffect(month,dataRevision,state){if(generateBetaRecurringOccurrences(state,repo,month))persist() else updateWidgetBalance()}
    LaunchedEffect(Unit){while(true){delay(10*60*1000L);repo.autoBackup(state)}}
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)month=YearMonth.now()};lifecycleOwner.lifecycle.addObserver(observer);onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}}
    LaunchedEffect(initialWidgetAction){when(initialWidgetAction){BetaWidgetProvider.ACTION_ADD_EINNAHME->{tab=Tab.BALANCE;betaInitialType="income";addBeta=true};BetaWidgetProvider.ACTION_ADD_AUSGABE->{tab=Tab.BALANCE;betaInitialType="expense";addBeta=true};BetaWidgetProvider.ACTION_OPEN->{tab=Tab.BALANCE}};if(initialWidgetAction!=null)onWidgetActionConsumed()}
    val startupScope=rememberCoroutineScope()
    val freshRestorePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null)startupScope.launch{
        runCatching{activity.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}
        repo.setSetting("haushaltsbuch-autobackup-uri",uri.toString());repo.setSetting("haushaltsbuch-autobackup-enabled","1")
        val name=withContext(Dispatchers.IO){DocumentFile.fromTreeUri(activity,uri)?.name?:"Ausgewählter Ordner"};repo.setSetting("haushaltsbuch-autobackup-name",name)
        if(freshRestorePending){
            freshRestorePending=false
            val result=withContext(Dispatchers.IO){runCatching{
                val file=DocumentFile.fromTreeUri(activity,uri)?.findFile("haushaltsbuch-auto-backup.csv")
                val text=file?.let{activity.contentResolver.openInputStream(it.uri)?.bufferedReader()?.use{reader->reader.readText()}}
                if(text.isNullOrBlank())null else parseHouseholdBackup(text)
            }}
            result.onSuccess{restored->if(restored==null){freshRestoreMessage="In diesem Ordner wurde keine Sicherung gefunden.";repo.autoBackup(state)}else freshRestoreData=restored}.onFailure{freshRestoreMessage="Das ist keine gültige Sicherung."}
        }
    }else freshRestorePending=false}

    LaunchedEffect(Unit){val current=NativeChangelog.maxOfOrNull{it.version}?:0;val seenRaw=repo.getSetting("household-last-seen-version");if(seenRaw.isNullOrBlank()){repo.setSetting("household-last-seen-version",current.toString())}else{var seen=seenRaw.toIntOrNull()?:0;if(seen>1000)seen=0;val unseen=NativeChangelog.filter{it.version>seen};if(unseen.isNotEmpty()){startupWhatsNew=unseen;repo.setSetting("household-last-seen-version",current.toString())}};val hasAnyData=listOf("income","fixedExpenses","openInvoices","variableCosts","loans","savings").sumOf{repo.array(state,it).length()}>0;if(repo.getSetting("household-fresh-restore-prompted")!="1"&&!hasAnyData){repo.setSetting("household-fresh-restore-prompted","1");freshRestorePrompt=true}}
    Scaffold(
        topBar={Surface(color=Color.White.copy(alpha=if(LocalAppDark.current).08f else .16f)){Column(Modifier.fillMaxWidth().padding(start=20.dp,end=20.dp,top=22.dp,bottom=10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Haushaltsbuch",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.6).sp);Text("+",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.weight(1f));Surface(shape=CircleShape,color=appGlass(LocalAppDark.current),modifier=Modifier.size(38.dp).clickable{settings=true}){Box(contentAlignment=Alignment.Center){SettingsGearIcon(MaterialTheme.colorScheme.onSurfaceVariant)}}};Box(Modifier.animateContentSize()){if(!headerBarCollapsed){val incomeTotal=sum(repo.array(state,"income"),"amount");val expenseTotal=sum(repo.array(state,"fixedExpenses"),"amount")+sum(repo.array(state,"variableCosts"),"monthlyAmount")+sum(repo.array(state,"loans"),"monthlyPayment"){it.optDouble("remainingBalance")>0}+sum(repo.array(state,"openInvoices"),"amount"){!it.optBoolean("paid")};IncomeExpenseBar(incomeTotal,expenseTotal)}}}}},
        bottomBar={Surface(color=appGlass(LocalAppDark.current),shape=RoundedCornerShape(26.dp),shadowElevation=4.dp){Row(Modifier.fillMaxWidth().padding(6.dp),horizontalArrangement=Arrangement.spacedBy(2.dp)){visibleTabs.forEach{t->Column(Modifier.weight(1f).clickable{tab=t}.padding(vertical=12.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally){OriginalTabIcon(t,if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,tab==t);Spacer(Modifier.height(4.dp));Text(t.label,fontSize=10.5.sp,maxLines=1,color=if(tab==t)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,fontWeight=FontWeight.Bold)}}}}},
        floatingActionButton={if(fabAllowed&&!fabOpen)Box(Modifier.padding(end=4.dp,bottom=if(tab==Tab.BALANCE)52.dp else 6.dp)){ReferenceFab(false){val options=fabOptions(tab,bookingsView,variableView);if(options.size==1){val opt=options.single();if(opt=="Neuer Eintrag"){betaInitialType=null;addBeta=true}else addForm=opt}else fabOpen=true}}}

    ){pad->Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)){when(tab){Tab.BALANCE->BalanceScreen(state,repo,month,{month=month.minusMonths(1)},{month=month.plusMonths(1)},{month=it},{e->if(e.optBoolean("isRecurringTemplate")||e.optString("recurringGroupId").isNotBlank())betaScopeChoiceId=e.optString("id") else {editBetaId=e.optString("id");editBetaScope="this"}},{e->if(e.optBoolean("reconciled")){e.put("reconciled",false);persist()}else reconcileConfirmId=e.optString("id")},{headerBarCollapsed=it});Tab.BOOKINGS->BookingsScreen(state,repo,bookingsView){bookingsView=it};Tab.VARIABLE->VariableScreen(state,repo,variableView){variableView=it};Tab.LOANS->LoansScreen(state,repo,loanView,{loanView=it}){fabAllowed=it};Tab.FUEL->FuelScreen(state,repo,fuelCar){fuelCar=it}}}}
    if(fabOpen)Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.4f)).clickable{fabOpen=false}){
     val extra=if(tab==Tab.BALANCE)46.dp else 0.dp
     Column(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=166.dp+extra),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(14.dp)){fabOptions(tab,bookingsView,variableView).asReversed().forEach{opt->ReferenceFabOption(opt){if(opt=="Neuer Eintrag"){betaInitialType=null;addBeta=true}else addForm=opt;fabOpen=false}}}
     Box(Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=90.dp+extra)){ReferenceFab(true){fabOpen=false}}
    }
    repo.pendingFreedLoan?.let{loan->ReferenceFreedRateDialog(loan,repo)}
    if(settings)SettingsDialog(state,repo,{settings=false},onThemeChanged,{visibilityRevision++},{restored->state=restored;visibilityRevision++;updateWidgetBalance()},onWhatsNew={startupWhatsNew=listOf(NativeChangelog.last());settings=false})
    addForm?.let{QuickAddForm(it,state,repo,{addForm=null},selectedCar=fuelCar){persist();addForm=null}}
    if(addBeta)BetaEntryDialog(betaInitialType,state,{addBeta=false;betaInitialType=null}){type,title,date,amount,category,recurring,period,endDate,reconciled->val entry=JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("title",title).put("date",date).put("amount",amount).put("category",category).put("recurring",recurring).put("reconciled",reconciled);if(recurring){entry.put("isRecurringTemplate",true).put("recurrencePeriod",period);if(endDate!=null)entry.put("recurrenceEnd",endDate)else entry.put("recurrenceEnd",JSONObject.NULL)};repo.array(state,"betaEntries").put(entry);month=YearMonth.from(LocalDate.parse(date));generateBetaRecurringOccurrences(state,repo,month);persist();addBeta=false}
    reconcileConfirmId?.let{id->val e=repo.array(state,"betaEntries").objects().firstOrNull{it.optString("id")==id};if(e!=null)ReferenceConfirm("\""+e.optString("title")+"\" als abgeglichen markieren?",icon="✅",confirmLabel="Abgleichen",onCancel={reconcileConfirmId=null},onConfirm={e.put("reconciled",true);persist();reconcileConfirmId=null})else reconcileConfirmId=null}
    betaScopeChoiceId?.let{id->val e=repo.array(state,"betaEntries").objects().firstOrNull{it.optString("id")==id};if(e!=null)BetaRecurringChoiceDialog({betaScopeChoiceId=null}){scope->editBetaId=id;editBetaScope=scope;betaScopeChoiceId=null}else betaScopeChoiceId=null}
    editBetaId?.let{id->val e=repo.array(state,"betaEntries").objects().firstOrNull{it.optString("id")==id};if(e!=null)BetaEditDialog(e,editBetaScope,state,repo,{editBetaId=null},{type,title,date,amount,category,recurring,reconciled,period,endDate->applyBetaEdit(state,repo,e,editBetaScope,type,title,date,amount,category,recurring,reconciled,period,endDate);val editedMonth=runCatching{YearMonth.from(LocalDate.parse(date))}.getOrDefault(month);generateBetaRecurringOccurrences(state,repo,editedMonth);month=editedMonth;persist();editBetaId=null},{deleteBetaEntry(state,repo,e,editBetaScope);persist();editBetaId=null})else editBetaId=null}
    if(freshRestorePrompt)ReferenceConfirm("Falls du zuvor ein automatisches Backup eingerichtet hattest, kann ich jetzt danach suchen. Ordner auswählen?",icon="📥",confirmLabel="Ordner wählen",onCancel={freshRestorePrompt=false},onConfirm={freshRestorePrompt=false;freshRestorePending=true;freshRestorePicker.launch(null)})
    freshRestoreData?.let{parsed->ReferenceConfirm("Aktuelle Daten durch die Sicherung ersetzen? Das kann nicht rückgängig gemacht werden.",icon="📥",confirmLabel="Ersetzen",onCancel={freshRestoreData=null;repo.autoBackup(state)},onConfirm={freshRestoreData=null;startupScope.launch{state=withContext(Dispatchers.IO){repo.restoreParsedBackup(parsed)};visibilityRevision++;freshRestoreMessage="Datenbank erfolgreich wiederhergestellt."}})}
    freshRestoreMessage?.let{msg->ReferenceMessage(msg,icon=if(msg.contains("keine Sicherung"))"ℹ️" else if(msg.contains("gültige"))"⚠️" else "✅",onDismiss={freshRestoreMessage=null})}
    if(startupWhatsNew.isNotEmpty())ReferenceWhatsNew(startupWhatsNew){startupWhatsNew=emptyList()}

}

internal fun fabOptions(tab:Tab,bookingsView:String="rechnungen",variableView:String="kostenarten"):List<String> = when(tab){Tab.BALANCE->listOf("Neuer Eintrag");Tab.BOOKINGS->listOf(when(bookingsView){"einnahmen"->"Einnahme";"fixkosten"->"Fixkosten";else->"Offene Rechnung"});Tab.VARIABLE->listOf(if(variableView=="spartopf")"Einzahlung in den Spartopf" else "Variable Kostenart");Tab.LOANS->listOf("Kredit");Tab.FUEL->listOf("Tankvorgang","Auto hinzufügen")}
@Composable private fun fabOptionColor(label:String):Color{val dark=LocalAppDark.current;return when(label){"Einnahme","Einzahlung in den Spartopf"->appGreen(dark);"Offene Rechnung"->if(dark)Color(0xFFFFB03F) else Color(0xFFFF9F0A);"Kredit"->appRed(dark);"Tankvorgang","Auto hinzufügen"->if(dark)Color(0xFF9CA3AF) else Color(0xFF6B7280);else->appBlue(dark)}}

@Composable private fun ReferenceFab(open:Boolean,onClick:()->Unit){
 val dark=LocalAppDark.current
 Surface(Modifier.size(60.dp).clickable(onClick=onClick),shape=CircleShape,color=appBlue(dark).copy(alpha=.8f),shadowElevation=8.dp){Box(contentAlignment=Alignment.Center){HtmlReferenceIcon("action-plus",Color.White,26.dp,2.6f,modifier=Modifier.rotate(if(open)45f else 0f))}}
}
@Composable private fun ReferenceFabOption(label:String,onClick:()->Unit){
 val dark=LocalAppDark.current;val icon=when(label){"Offene Rechnung"->"Rechnungen";"Einnahme"->"Einnahmen";"Fixkosten"->"Fixkosten";"Variable Kostenart"->"Kostenarten";"Einzahlung in den Spartopf"->"Spartopf";"Kredit"->"kredite";"Tankvorgang"->"tanken";"Auto hinzufügen"->"fuel-add-car";else->"fab-entry"}
 val gradient=when(label){"Offene Rechnung"->if(dark)listOf(Color(0xFF3A2E1B),Color(0xFF2C2314)) else listOf(Color(0xFFFFE3B0),Color(0xFFFFD48C));"Einzahlung in den Spartopf"->if(dark)listOf(Color(0xFF1F3A2E),Color(0xFF162B22)) else listOf(Color(0xFFBFEBD2),Color(0xFFA3E0BE));else->null}
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Surface(Modifier.clickable(onClick=onClick),shape=RoundedCornerShape(12.dp),color=appGlass(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=2.dp){Text(label,Modifier.padding(horizontal=13.dp,vertical=8.dp),fontSize=13.sp,fontWeight=FontWeight.Bold)}
  val field=Modifier.size(46.dp).clip(CircleShape)
  Box((if(gradient!=null)field.htmlGradient(gradient) else field.background(if(label=="Auto hinzufügen")appInkSoft(dark) else fabOptionColor(label))).clickable(onClick=onClick),contentAlignment=Alignment.Center){HtmlReferenceIcon(icon,Color.White,19.dp)}
 }
}

@Composable private fun QuickAddForm(kind:String,state:JSONObject,repo:HouseholdRepository,onClose:()->Unit,selectedCar:String?=null,onSaved:()->Unit){
 val dark=LocalAppDark.current
 var name by remember{mutableStateOf("")}
 var amount by remember{mutableStateOf(if(kind=="Einzahlung in den Spartopf")repo.getSetting("household-last-contrib-amount")?.toDoubleOrNull()?.let{String.format(Locale.GERMANY,"%.2f",it)}?:"" else "")}
 var second by remember{mutableStateOf("")};var total by remember{mutableStateOf("")};var term by remember{mutableStateOf("")};var year by remember{mutableStateOf("")}
 var date by remember{mutableStateOf(LocalDate.now().toString())};var mode by remember{mutableStateOf("in")}
 var dueMonth by remember{mutableStateOf(LocalDate.now().monthValue)};var interval by remember{mutableStateOf("jaehrlich")}
 val cars=repo.array(state,"cars").objects();var carId by remember{mutableStateOf(cars.firstOrNull{it.optString("id")==selectedCar}?.optString("id")?:cars.firstOrNull()?.optString("id").orEmpty())}
 val title=if(kind=="Einzahlung in den Spartopf")"Spartopf-Buchung" else kind
 ReferencePanel(title,onClose){
  Column(Modifier.fillMaxWidth().padding(18.dp).padding(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   when(kind){
    "Einnahme","Fixkosten"->ReferenceTwoFields({BetaInput(name,{name=it},"Bezeichnung")},{BetaInput(amount,{amount=centInput(it)},"Betrag (€)",decimal=true)})
    "Offene Rechnung"->{BetaInput(name,{name=it},"Bezeichnung");ReferenceTwoFields({BetaInput(amount,{amount=centInput(it)},"Betrag (€)",decimal=true)},{ReferenceDateInput(date){date=it}})}
    "Einzahlung in den Spartopf"->{
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OriginalSubNav("Einzahlung",mode=="in",Modifier.weight(1f)){mode="in"};OriginalSubNav("Ausgabe",mode=="out",Modifier.weight(1f)){mode="out"}}
     if(mode=="out")BetaInput(name,{name=it},"Bezeichnung (optional)")
     ReferenceTwoFields({ReferenceDateInput(date){date=it}},{BetaInput(amount,{amount=centInput(it)},"Betrag (€)",decimal=true)})
    }
    "Variable Kostenart"->{
     BetaInput(name,{name=it},"Bezeichnung")
     ReferenceTwoFields({BetaInput(amount,{amount=centInput(it)},"Monatlich (€)",decimal=true)},{BetaInput(second,{second=centInput(it)},"Rechnung (€, optional)",decimal=true)})
     ReferenceTwoFields({ReferenceSelect(interval,listOf("monatlich" to "Monatlich fällig","vierteljaehrlich" to "Vierteljährlich fällig","halbjaehrlich" to "Halbjährlich fällig","jaehrlich" to "Jährlich fällig","zweijaehrlich" to "Alle 2 Jahre fällig")){interval=it}},{Spacer(Modifier.height(1.dp))})
     ReferenceSelect(dueMonth.toString(),(1..12).map{it.toString() to java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN)}){dueMonth=it.toInt()}
     if(interval=="zweijaehrlich")BetaInput(year,{year=it.filter(Char::isDigit)},"Startjahr (z. B. 2024)",decimal=true)
    }
    "Kredit"->{
     BetaInput(name,{name=it},"Bezeichnung")
     ReferenceTwoFields({ReferenceLabeledInput("Kredit ohne Zinsen",amount,{amount=centInput(it)},"Kredit ohne Zinsen (€)")},{ReferenceLabeledInput("Gesamtbetrag inkl. Zinsen (optional)",total,{total=centInput(it)},"z. B. 12.480,00")})
     ReferenceTwoFields({ReferenceLabeledInput("Laufzeit in Monaten (optional)",term,{term=it.filter(Char::isDigit)},"z. B. 36")},{ReferenceLabeledInput("Rate pro Monat",second,{second=centInput(it)},"Rate/Monat (€)")})
    }
    "Tankvorgang"->{
     Text("Auto",fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark))
     ReferenceSelect(carId,cars.map{it.optString("id") to it.optString("name")}){carId=it}
     BetaInput(amount,{amount=centInput(it)},"Liter",decimal=true)
     BetaInput(second,{second=milliInput(it)},"€/Liter",decimal=true)
     Text("Datum",fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));ReferenceDateInput(date){date=it}
    }
    "Auto hinzufügen"->BetaInput(name,{name=it},"Name des Autos (z.B. Peugeot 2008)")
   }
   val submit=when(kind){"Offene Rechnung"->"Rechnung hinzufügen";"Variable Kostenart"->"Kostenart hinzufügen";"Kredit"->"Kredit hinzufügen";"Einzahlung in den Spartopf"->if(mode=="in")"Einzahlen" else "Ausgeben";"Tankvorgang"->"Speichern";else->"Hinzufügen"}
   ReferenceSubmit(submit,if(kind in listOf("Tankvorgang","Auto hinzufügen"))appExhaust(dark) else appBlue(dark)){
    val built=buildReferenceQuickEntry(kind,name,amount,second,total,term,date,interval,dueMonth,year,mode,carId)
    if(built!=null){repo.array(state,built.first).put(built.second);if(kind=="Einzahlung in den Spartopf"&&mode=="in")repo.setSetting("household-last-contrib-amount",built.second.optDouble("amount").toString());onSaved()}
   }
  }
 }
}
internal fun buildReferenceQuickEntry(kind:String,name:String,amount:String,second:String="",total:String="",term:String="",date:String=LocalDate.now().toString(),interval:String="jaehrlich",dueMonth:Int=LocalDate.now().monthValue,year:String="",mode:String="in",carId:String=""):Pair<String,JSONObject>?{
 val n=name.trim();val v=parseNativeAmount(amount);val v2=parseNativeAmount(second);val validDate=runCatching{LocalDate.parse(date)}.isSuccess
 val o=JSONObject().put("id",genId())
 return when(kind){
  "Einnahme","Fixkosten"->if(n.isEmpty()||v==null)null else (if(kind=="Einnahme")"income" else "fixedExpenses") to o.put("name",n).put("amount",v)
  "Offene Rechnung"->if(n.isEmpty()||v==null||v<=0||!validDate)null else "openInvoices" to o.put("name",n).put("amount",v).put("dueDate",date).put("paid",false)
  "Auto hinzufügen"->if(n.isEmpty())null else "cars" to o.put("name",n)
  "Tankvorgang"->if(v==null||v<=0||v2==null||v2<=0||carId.isBlank()||!validDate)null else "fuelLogs" to o.put("carId",carId).put("liters",v).put("pricePerLiter",v2).put("date",date)
  "Einzahlung in den Spartopf"->if(v==null||v<=0||!validDate)null else if(mode=="out")"variablePayments" to o.put("itemId",JSONObject.NULL).put("itemName",n.ifEmpty{"Ausgabe"}).put("amount",v).put("date",date) else "variableContributions" to o.put("note","").put("amount",v).put("date",date)
  "Variable Kostenart"->if(n.isEmpty()||v==null)null else "variableCosts" to o.put("name",n).put("monthlyAmount",v).put("actualAmount",v2?:0.0).put("interval",interval).put("dueMonth",dueMonth).put("dueYear",if(interval=="zweijaehrlich")year.toIntOrNull()?:JSONObject.NULL else JSONObject.NULL)
  "Kredit"->{if(n.isEmpty()||v2==null)null else {val original=v?:0.0;val withInterest=parseNativeAmount(total);"loans" to o.put("name",n).put("originalAmount",original).put("remainingBalance",original).put("monthlyPayment",v2).put("totalWithInterest",withInterest?:JSONObject.NULL).put("totalTermMonths",term.toIntOrNull()?.takeIf{it>0}?:JSONObject.NULL).put("monthlyRate",if(original>0&&withInterest!=null)solveMonthlyRate(original,v2,withInterest)?:JSONObject.NULL else JSONObject.NULL).put("carryoverPct",100)}}
  else->null
 }
}
@Composable private fun ReferencePanel(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onClose,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  val panelHeight=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.85f).dp
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).imePadding().clickable(onClick=onClose),contentAlignment=Alignment.Center){
   Surface(Modifier.padding(20.dp).widthIn(max=400.dp).fillMaxWidth().heightIn(max=panelHeight).clickable{},shape=RoundedCornerShape(26.dp),color=if(dark)Color(0xFF2E2F35) else Color(0xFFD0D1D6),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){
    Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp)){
     Row(Modifier.fillMaxWidth().padding(bottom=4.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=appInk(dark));Surface(Modifier.clickable(onClick=onClose),shape=CircleShape,color=appPaper(dark)){Text("×",Modifier.padding(horizontal=13.dp,vertical=8.dp),fontSize=18.sp,color=appInkSoft(dark))}}
     content()
    }
   }
  }
 }
}
@Composable private fun ReferenceTwoFields(first:@Composable ()->Unit,second:@Composable ()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.weight(1f)){first()};Box(Modifier.weight(1f)){second()}}}
@Composable private fun ReferenceLabeledInput(label:String,value:String,onValue:(String)->Unit,placeholder:String){Column{Text(label,Modifier.heightIn(min=28.dp).padding(start=2.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(LocalAppDark.current));BetaInput(value,onValue,placeholder,decimal=true)}}
@Composable private fun ReferenceSubmit(label:String,color:Color,onClick:()->Unit){Surface(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(999.dp),color=color){Text(label,Modifier.padding(16.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)}}
@Composable private fun ReferenceSelect(value:String,options:List<Pair<String,String>>,edit:Boolean=false,onValue:(String)->Unit){
 val dark=LocalAppDark.current;var expanded by remember{mutableStateOf(false)}
 val field=if(edit)Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal=14.dp,vertical=13.dp) else Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(appPaper(dark)).border(1.5.dp,if(dark)Color(0xFF4A4C52) else Color(0xFFB0B2B8),RoundedCornerShape(16.dp)).padding(horizontal=16.dp,vertical=14.dp)
 Box{Row(Modifier.clickable{expanded=true}.then(field),verticalAlignment=Alignment.CenterVertically){Text(options.firstOrNull{it.first==value}?.second.orEmpty(),Modifier.weight(1f),fontSize=16.sp,color=appInk(dark));HtmlReferenceIcon("chevron-down",appInkSoft(dark),16.dp)};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){options.forEach{(key,label)->DropdownMenuItem(text={Text(label)},onClick={onValue(key);expanded=false})}}}
}
@Composable private fun ReferenceDateInput(value:String,edit:Boolean=false,onValue:(String)->Unit){
 var picking by remember{mutableStateOf(false)}
 if(edit)Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable{picking=true}.padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text(runCatching{LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault(value),Modifier.weight(1f),fontSize=16.sp);CalendarOutlineIcon(appInk(LocalAppDark.current))} else BetaDateField(value){picking=true}
 if(picking){val context=androidx.compose.ui.platform.LocalContext.current;DisposableEffect(Unit){val initial=runCatching{LocalDate.parse(value)}.getOrDefault(LocalDate.now());val picker=android.app.DatePickerDialog(context,{_,y,m,d->onValue(LocalDate.of(y,m+1,d).toString());picking=false},initial.year,initial.monthValue-1,initial.dayOfMonth);picker.setOnDismissListener{picking=false};picker.show();onDispose{picker.dismiss()}}}
}
internal fun parseNativeAmount(raw:String):Double?{
 var value=raw.trim();if(value.isEmpty())return null
 if(',' in value&&'.' in value)value=if(value.lastIndexOf(',')>value.lastIndexOf('.'))value.replace(".","").replace(',','.') else value.replace(",","")
 else if(',' in value)value=value.replace(',','.')
 else if(Regex("^[0-9]{1,3}(\\.[0-9]{3})+$").matches(value))value=value.replace(".","")
 return value.toDoubleOrNull()?.takeIf{it.isFinite()}
}
internal fun centInput(raw:String):String{val digits=raw.filter{it in '0'..'9'};if(digits.isEmpty())return "";val n=digits.trimStart('0').ifEmpty{"0"}.takeLast(9).toLong();return String.format(Locale.GERMANY,"%,d,%02d",n/100,n%100)}
internal fun milliInput(raw:String):String{val digits=raw.filter{it in '0'..'9'};if(digits.isEmpty())return "";val n=digits.trimStart('0').ifEmpty{"0"}.takeLast(9).toLong();return String.format(Locale.GERMANY,"%,d,%03d",n/1000,n%1000)}
private fun solveMonthlyRate(principal:Double,monthly:Double,total:Double):Double?{if(principal==0.0||monthly==0.0||total==0.0||total<=principal)return null;val n=total/monthly;if(n<1)return null;var r=.01;repeat(60){val pow=(1+r).pow(-n);val fv=monthly*(1-pow)/r-principal;val h=.00001;val powH=(1+r+h).pow(-n);val fH=monthly*(1-powH)/(r+h)-principal;val deriv=(fH-fv)/h;if(kotlin.math.abs(deriv)<1e-9)return@repeat;var nr=r-fv/deriv;if(nr<=0)nr=r/2;if(kotlin.math.abs(nr-r)<1e-9){r=nr;return@repeat};r=nr};return if(r>0&&r.isFinite())r else null}

internal fun betaAddPeriod(date:LocalDate,period:String):LocalDate=when(period){"weekly"->date.plusWeeks(1);"biweekly"->date.plusWeeks(2);"4weekly"->date.plusWeeks(4);"2monthly"->date.plusMonths(2);"3monthly"->date.plusMonths(3);"6monthly"->date.plusMonths(6);"yearly"->date.plusYears(1);"monthly"->date.plusMonths(1);else->date.plusDays(30)}
private fun generateBetaRecurringOccurrences(state:JSONObject,repo:HouseholdRepository,viewMonth:YearMonth):Boolean=generateBetaOccurrences(repo.array(state,"betaEntries"),viewMonth)
internal fun betaGenerationHorizon(today:LocalDate,viewMonth:YearMonth):LocalDate{
 val twoMonths=YearMonth.from(today).plusMonths(2).atDay(1).plusDays((today.dayOfMonth-1).toLong())
 return maxOf(twoMonths,viewMonth.atEndOfMonth())
}
internal fun generateBetaOccurrences(entries:JSONArray,viewMonth:YearMonth,today:LocalDate=LocalDate.now()):Boolean{
 val all=entries.objects();val templates=all.filter{it.optBoolean("isRecurringTemplate")}
 if(templates.isEmpty())return false
 val horizon=betaGenerationHorizon(today,viewMonth)
 // Index series dates once. A large import must not scan all bookings for every template.
 val datesByGroup=HashMap<String,MutableSet<String>>();val templateIds=templates.map{it.optString("id")}.toHashSet()
 all.forEach{entry->val id=entry.optString("id");val group=entry.optString("recurringGroupId");val date=entry.optString("date");if(id in templateIds)datesByGroup.getOrPut(id){HashSet()}.add(date);if(group in templateIds)datesByGroup.getOrPut(group){HashSet()}.add(date)}
 var changed=false
 templates.forEach{tpl->
  val id=tpl.optString("id");val existing=datesByGroup.getOrPut(id){HashSet()}
  val start=runCatching{LocalDate.parse(tpl.optString("date"))}.getOrNull()?:return@forEach
  val end=tpl.optString("recurrenceEnd").takeIf{it.isNotBlank()&&it!="null"}?.let{runCatching{LocalDate.parse(it)}.getOrNull()}
  val period=tpl.optString("recurrencePeriod").ifBlank{"monthly"};var cursor=betaAddPeriod(start,period);var guard=0
  while(!cursor.isAfter(horizon)&&guard<240){guard++;if(end!=null&&cursor.isAfter(end))break;val key=cursor.toString();if(existing.add(key)){entries.put(JSONObject().put("id",UUID.randomUUID().toString()).put("type",tpl.optString("type")).put("title",tpl.optString("title")).put("date",key).put("amount",tpl.optDouble("amount")).put("category",tpl.optString("category")).put("recurring",true).put("recurringGroupId",id).put("reconciled",false));changed=true};cursor=betaAddPeriod(cursor,period)}
 }
 return changed
}
@Composable private fun BalanceScreen(state:JSONObject,repo:HouseholdRepository,month:YearMonth,prev:()->Unit,next:()->Unit,onMonthSelected:(YearMonth)->Unit,onEntryClick:(JSONObject)->Unit,onToggleReconciled:(JSONObject)->Unit,onBarCollapsed:(Boolean)->Unit){
    val data=remember(state,repo.revision,month){betaMonthData(repo.array(state,"betaEntries").objects(),month)}
    val income=data.income;val expense=data.expense;val balance=data.balance
    var filter by remember{mutableStateOf("all")};var monthPicker by remember{mutableStateOf(false)};var yearPicker by remember{mutableStateOf(false)}
    val unreconciled=data.unreconciled
    val groups=remember(state,repo.revision,month,filter){val shown=if(filter=="unreconciled")data.entries.filter{!it.optBoolean("reconciled")} else data.entries;shown.groupBy{it.optString("date")}.toSortedMap(compareByDescending{it}).mapValues{(_,entries)->entries.sortedByDescending{it.optString("id")}}}
    val listState=rememberLazyListState()
    var barCollapsed by remember(month){mutableStateOf(false)}
    var lastScrollOffset by remember(month){mutableStateOf(0)}
    LaunchedEffect(listState.firstVisibleItemIndex,listState.firstVisibleItemScrollOffset){val current=if(listState.firstVisibleItemIndex==0)listState.firstVisibleItemScrollOffset else Int.MAX_VALUE;if(current<=8){barCollapsed=false;lastScrollOffset=current}else if(current!=Int.MAX_VALUE&&lastScrollOffset!=Int.MAX_VALUE&&current-lastScrollOffset>24){barCollapsed=true;lastScrollOffset=current}else if(current==Int.MAX_VALUE){barCollapsed=true;lastScrollOffset=current}else if(kotlin.math.abs(current-lastScrollOffset)>24){lastScrollOffset=current}}
    Column(Modifier.fillMaxSize().subViewSwipe(listOf("all","unreconciled"),filter){filter=it}){
        LaunchedEffect(barCollapsed){onBarCollapsed(barCollapsed)}
        Surface(color=Color.White.copy(alpha=if(LocalAppDark.current).10f else .35f),shape=RoundedCornerShape(bottomStart=20.dp,bottomEnd=20.dp)){
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.size(40.dp).clickable(onClick=prev)){Box(contentAlignment=Alignment.Center){ChevronIcon(left=true,color=MaterialTheme.colorScheme.onSurface)}}
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    Row{Text(month.format(DateTimeFormatter.ofPattern("MMMM",Locale.GERMAN)).replaceFirstChar{it.uppercase()},fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.clickable{monthPicker=true});Spacer(Modifier.width(6.dp));Text(month.year.toString(),fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.clickable{yearPicker=true})}
                    Text("Einnahmen +"+money(income)+" · Ausgaben -"+money(expense),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape=CircleShape,color=MaterialTheme.colorScheme.surface,modifier=Modifier.size(40.dp).clickable(onClick=next)){Box(contentAlignment=Alignment.Center){ChevronIcon(left=false,color=MaterialTheme.colorScheme.onSurface)}}
            }
        }
        Row(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,bottom=18.dp).background(appPaper(LocalAppDark.current), RoundedCornerShape(14.dp)).padding(4.dp)){
            BetaFilter("Alle",filter=="all",Modifier.weight(1f)){filter="all"}
            BetaFilter("Nicht abgeglichen ($unreconciled)",filter=="unreconciled",Modifier.weight(1f)){filter="unreconciled"}
        }
        LazyColumn(Modifier.weight(1f),state=listState,contentPadding=PaddingValues(horizontal=18.dp,vertical=2.dp)){
            if(groups.isEmpty())item{Text(if(filter=="unreconciled")"Alles in diesem Monat ist abgeglichen." else "Noch keine Einträge in diesem Monat.",Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=34.dp),textAlign=TextAlign.Center,color=appInkSoft(LocalAppDark.current),fontSize=15.5.sp,fontWeight=FontWeight.SemiBold)}
            groups.forEach{(date,entries)->
                item(key="day-"+date,contentType="day-header"){BetaDayHeader(date,entries.sumOf{if(betaIsIncome(it))it.optDouble("amount") else -it.optDouble("amount")})}
                items(entries.size,key={index->"entry-"+entries[index].optString("id")},contentType={"entry"}){index->BetaEntryRow(entries[index],index==entries.lastIndex,onEntryClick,onToggleReconciled)}
            }
        }
        Row(Modifier.fillMaxWidth().background(if(balance>=0)appGreen(LocalAppDark.current) else appRed(LocalAppDark.current)).padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Kontostand",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White.copy(alpha=.9f));Spacer(Modifier.width(10.dp));Text(money(balance),fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,modifier=Modifier.weight(1f))
            Text("01."+month.monthValue.toString().padStart(2,'0')+"."+month.year+"\n- "+month.lengthOfMonth().toString().padStart(2,'0')+"."+month.monthValue.toString().padStart(2,'0')+"."+month.year,fontSize=10.5.sp,color=Color.White.copy(alpha=.85f),textAlign=TextAlign.End)
        }
    }
    if(monthPicker)ReferencePeriodPicker("Monat wählen",(1..12).map{it to java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN).take(3)},month.monthValue,false,{monthPicker=false}){m->onMonthSelected(YearMonth.of(month.year,m));monthPicker=false}
    if(yearPicker){val realYear=LocalDate.now().year;ReferencePeriodPicker("Jahr wählen",(realYear-10..realYear+10).map{it to it.toString()},month.year,true,{yearPicker=false}){y->onMonthSelected(YearMonth.of(y,month.monthValue));yearPicker=false}}


}
@Composable private fun ReferencePeriodPicker(title:String,options:List<Pair<Int,String>>,selected:Int,scroll:Boolean,onDismiss:()->Unit,onSelect:(Int)->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).padding(24.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){Surface(Modifier.widthIn(max=340.dp).fillMaxWidth().clickable{},shape=RoundedCornerShape(26.dp),color=appGlass(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){Column(Modifier.padding(start=26.dp,end=26.dp,top=28.dp,bottom=8.dp)){
   Text(title,Modifier.padding(bottom=8.dp),fontSize=16.5.sp,lineHeight=23.1.sp,fontWeight=FontWeight.Bold,color=appInk(dark))
   Column((if(scroll)Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState()) else Modifier).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
    options.chunked(3).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(value,label)->val field=Modifier.weight(1f).clip(RoundedCornerShape(10.dp));Box((if(value==selected)field.htmlGradient(listOf(heroBlueStart(dark),heroBlueEnd(dark))) else field.background(appPaper(dark))).clickable{onSelect(value)}.padding(horizontal=4.dp,vertical=11.dp),contentAlignment=Alignment.Center){Text(label,fontSize=13.5.sp,fontWeight=FontWeight.Bold,color=if(value==selected)heroBlueText(dark) else appInk(dark))}};repeat(3-row.size){Spacer(Modifier.weight(1f))}}}
   }
   Surface(Modifier.fillMaxWidth().padding(top=10.dp).clickable(onClick=onDismiss),shape=RoundedCornerShape(999.dp),color=if(dark)Color(0xFF52545C) else Color.White){Text("Abbrechen",Modifier.padding(horizontal=10.dp,vertical=14.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}
  }}}
 }
}
@Composable private fun BetaFilter(label:String,active:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){
 val dark=LocalAppDark.current;val color=if(active)appInk(dark) else appInkSoft(dark)
 Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(11.dp),color=if(active)appGlass(dark) else Color.Transparent,shadowElevation=if(active)1.dp else 0.dp){Row(Modifier.padding(vertical=11.dp,horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically){HtmlReferenceIcon(if(label=="Alle")"filter-all" else "filter-open",color,13.dp,2.2f);Text(label,fontSize=13.5.sp,fontWeight=FontWeight.Bold,color=color)}}
}
internal data class BetaMonthData(val entries:List<JSONObject>,val income:Double,val expense:Double,val balance:Double,val unreconciled:Int)
internal fun betaMonthData(all:List<JSONObject>,month:YearMonth):BetaMonthData{
 val entries=ArrayList<JSONObject>();var income=0.0;var expense=0.0;var balance=0.0;var unreconciled=0;val cutoff=month.atEndOfMonth()
 all.forEach{entry->val date=runCatching{LocalDate.parse(entry.optString("date"))}.getOrNull()?:return@forEach;val amount=entry.optDouble("amount");val positive=betaIsIncome(entry);if(!date.isAfter(cutoff))balance+=if(positive)amount else -amount;if(YearMonth.from(date)==month){entries.add(entry);if(positive)income+=amount else expense+=amount;if(!entry.optBoolean("reconciled"))unreconciled++}}
 return BetaMonthData(entries,income,expense,balance,unreconciled)
}
@Composable private fun BetaDayHeader(date:String,total:Double){
 val dark=LocalAppDark.current;val d=runCatching{LocalDate.parse(date)}.getOrNull()
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=14.dp,topEnd=14.dp)).htmlGradient(listOf(heroBlueStart(dark),heroBlueEnd(dark))).padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.size(34.dp).border(1.5.dp,heroBlueText(dark),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text((d?.dayOfMonth?:0).toString().padStart(2,'0'),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=heroBlueText(dark))}
  Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("TAGESSALDO",fontSize=11.5.sp,fontWeight=FontWeight.ExtraBold,color=heroBlueText(dark),letterSpacing=.23.sp);Text(d?.format(DateTimeFormatter.ofPattern("MMMM yyyy (EEEE)",Locale.GERMAN)).orEmpty(),fontSize=11.sp,color=heroBlueSub(dark))}
  Text((if(total>=0)"+" else "")+money(total),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=if(total>=0)appGreen(dark) else appRed(dark))
 }
}
@Composable private fun BetaEntryRow(entry:JSONObject,last:Boolean,onEntryClick:(JSONObject)->Unit,onToggleReconciled:(JSONObject)->Unit){
 val dark=LocalAppDark.current;val reconciled=entry.optBoolean("reconciled")
 Surface(Modifier.fillMaxWidth().padding(bottom=if(last)3.dp else 0.dp),shape=RoundedCornerShape(bottomStart=if(last)14.dp else 0.dp,bottomEnd=if(last)14.dp else 0.dp),color=appGlass(dark)){
  Column{HorizontalDivider(color=appRule(dark));Row(Modifier.fillMaxWidth().clickable{onEntryClick(entry)}.padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(22.dp).border(2.dp,if(reconciled)appInkSoft(dark) else appRuleStrong(dark),CircleShape).background(if(reconciled)appInkSoft(dark) else Color.Transparent,CircleShape).clickable{onToggleReconciled(entry)},contentAlignment=Alignment.Center){if(reconciled)Text("✓",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)}
   Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(entry.optString("title"),modifier=Modifier.weight(1f,fill=false),fontSize=15.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=appInk(dark));if(entry.optBoolean("recurring"))BetaRecurringIcon(appInkSoft(dark))};if(entry.optString("category").isNotBlank())Text(entry.optString("category"),fontSize=12.sp,color=appInkSoft(dark))}
   Text((if(betaIsIncome(entry))"+" else "-")+money(entry.optDouble("amount")),fontSize=15.sp,fontWeight=FontWeight.ExtraBold,color=if(betaIsIncome(entry))appGreen(dark) else appRed(dark))
  }}
 }
}
@Composable private fun ChevronIcon(left:Boolean,color:Color){Canvas(Modifier.size(20.dp)){val stroke=Stroke(width=2.4.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round);val x1=if(left)size.width*.63f else size.width*.37f;val x2=if(left)size.width*.37f else size.width*.63f;drawLine(color,Offset(x1,size.height*.25f),Offset(x2,size.height*.5f),strokeWidth=stroke.width,cap=StrokeCap.Round);drawLine(color,Offset(x2,size.height*.5f),Offset(x1,size.height*.75f),strokeWidth=stroke.width,cap=StrokeCap.Round)}}
@Composable private fun BetaRecurringIcon(color:Color){HtmlReferenceIcon("recurring",color,12.dp,2.4f)}
@Composable private fun IncomeExpenseBar(income:Double,expense:Double){
 val total=income+expense;val left=if(total>0)(income/total).toFloat().coerceIn(0f,1f) else .5f
 Row(Modifier.fillMaxWidth().padding(top=12.dp).height(20.dp).clip(RoundedCornerShape(10.dp))){
  if(left>0)Box(Modifier.weight(left).fillMaxHeight().background(appGreen(LocalAppDark.current)).padding(start=8.dp),contentAlignment=Alignment.CenterStart){if(left>=.20f)Text(money(income),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}
  if(left<1)Box(Modifier.weight(1-left).fillMaxHeight().background(appRed(LocalAppDark.current)).padding(end=8.dp),contentAlignment=Alignment.CenterEnd){if(1-left>=.20f)Text(money(expense),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)}
 }
}

private fun Modifier.subViewSwipe(views:List<String>,current:String,onChange:(String)->Unit)=pointerInput(views,current){awaitPointerEventScope{while(true){val down=awaitFirstDown(requireUnconsumed=false);val start=down.position;var end=start;var pressed=true;while(pressed){val event=awaitPointerEvent();val change=event.changes.firstOrNull{it.id==down.id}?:break;end=change.position;pressed=change.pressed};val dx=end.x-start.x;val dy=end.y-start.y;val idx=views.indexOf(current);if(kotlin.math.abs(dx)>=60f&&kotlin.math.abs(dx)>=kotlin.math.abs(dy)*1.3f&&idx>=0){if(dx<0&&idx<views.lastIndex)onChange(views[idx+1]) else if(dx>0&&idx>0)onChange(views[idx-1])}}}}
@Composable private fun BookingsScreen(state:JSONObject,repo:HouseholdRepository,view:String,onView:(String)->Unit){val InkSoft=appInkSoft(LocalAppDark.current);
 var revision by remember{mutableStateOf(0)};revision
 val income=repo.array(state,"income");val fixed=repo.array(state,"fixedExpenses");val invoices=repo.array(state,"openInvoices");var paidOpen by remember{mutableStateOf(repo.getSetting("household-show-paid-invoices")=="1")}
 fun changed(){repo.save(state);revision++}
 Column(Modifier.fillMaxSize().subViewSwipe(listOf("rechnungen","einnahmen","fixkosten"),view){onView(it)}.padding(horizontal=18.dp)){Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current),RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Rechnungen",view=="rechnungen",Modifier.weight(1f)){onView("rechnungen")};OriginalSubNav("Einnahmen",view=="einnahmen",Modifier.weight(1f)){onView("einnahmen")};OriginalSubNav("Fixkosten",view=="fixkosten",Modifier.weight(1f)){onView("fixkosten")}};Spacer(Modifier.height(18.dp))
  when(view){"rechnungen"->{val all=invoices.objects();val open=all.filter{!it.optBoolean("paid")};val paid=all.filter{it.optBoolean("paid")}.sortedByDescending{it.optString("paidDate")};val today=LocalDate.now();val overdue=open.count{runCatching{LocalDate.parse(it.optString("dueDate")).isBefore(today)}.getOrDefault(false)}
   LazyColumn{item{OriginalHero("Offene Rechnungen gesamt",money(open.sumOf{it.optDouble("amount")}),"Rechnungen",open.size.toString(),"Davon überfällig",overdue.toString(),true)};if(open.isEmpty())item{OriginalEmpty("Keine offenen Rechnungen eingetragen.")};items(open.sortedBy{it.optString("dueDate")},key={it.optString("id")}){InvoiceEditableRow(it,state,repo,::changed)};item{PaidInvoicesHeader(paid.size,paidOpen){paidOpen=!paidOpen;repo.setSetting("household-show-paid-invoices",if(paidOpen)"1" else "0")}};if(paidOpen){if(paid.isEmpty())item{Text("Noch keine bezahlten Rechnungen.",Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=14.dp),textAlign=TextAlign.Center,color=InkSoft)}else items(paid,key={it.optString("id")}){PaidInvoiceEditableRow(it,state,repo,::changed)}}}}
   "einnahmen"->{val incomeTotal=sum(income,"amount");val activeLoanMonthly=repo.array(state,"loans").objects().filter{it.optDouble("remainingBalance")>0}.sumOf{it.optDouble("monthlyPayment")};val rest=incomeTotal-sum(fixed,"amount")-repo.array(state,"variableCosts").objects().sumOf{it.optDouble("monthlyAmount")}-activeLoanMonthly-(repo.getSetting("household-tilgung-extra")?.toDoubleOrNull()?:0.0);OriginalHero("Einnahmen gesamt",money(incomeTotal),"Einträge",income.length().toString(),"Monatlicher Rest",money(rest));LazyColumn{if(income.length()==0)item{OriginalEmpty("Noch nichts eingetragen.")};items(income.objects(),key={it.optString("id")}){EditableMoneyRow(it,state,repo,income,true,::changed)}}}
   else->{val manual=sum(fixed,"amount");val activeLoans=repo.array(state,"loans").objects().filter{it.optDouble("remainingBalance")>0};val loanTotal=activeLoans.sumOf{it.optDouble("monthlyPayment")};val extra=repo.getSetting("household-tilgung-extra")?.toDoubleOrNull()?.takeIf{it>=0}?:0.0;val vcNeed=repo.array(state,"variableCosts").objects().sumOf{it.optDouble("monthlyAmount")};val auto=loanTotal+extra+vcNeed;OriginalHero("Fixkosten gesamt",money(manual),"Einträge",fixed.length().toString(),"Inkl. automatisch",money(manual+auto));LazyColumn{if(fixed.length()==0)item{OriginalEmpty("Noch nichts eingetragen.")};groupFixed(fixed.objects()).forEach{g->item(key="group-"+g.first){EditableFixedGroup(g.first,g.second,state,repo,fixed,::changed)}};if(auto>0)item{AutoFixedGroup(loanTotal,extra,vcNeed)}}}
  }
 }
}
@Composable private fun EditableMoneyRow(o:JSONObject,state:JSONObject,repo:HouseholdRepository,array:JSONArray,income:Boolean,onChanged:()->Unit){
 val dark=LocalAppDark.current
 var editing by remember(o.optString("id")){mutableStateOf(false)};var expanded by remember(o.optString("id")){mutableStateOf(false)};var deleting by remember{o.let{mutableStateOf(false)}}
 var name by remember(o.optString("id")){mutableStateOf(o.optString("name"))};var amount by remember(o.optString("id")){mutableStateOf(String.format(Locale.GERMANY,"%.2f",o.optDouble("amount")))};var icon by remember(o.optString("id")){mutableStateOf(o.optString("icon").ifBlank{"auto"})}
 if(editing){
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(appPrimarySoft(dark)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ReferenceEditInput(name,{name=it},Modifier.weight(1f));ReferenceEditInput(amount,{amount=centInput(it)},Modifier.weight(1f),decimal=true)}
   if(!income)ReferenceIconPicker(icon){icon=it}
   ReferenceEditActions({editing=false}){val a=parseNativeAmount(amount);if(name.trim().isNotEmpty()&&a!=null){o.put("name",name.trim()).put("amount",a);if(!income){if(icon=="auto")o.remove("icon") else o.put("icon",icon)};editing=false;onChanged()}}
  }
 }else{
  Column(Modifier.fillMaxWidth().clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(o.optString("name"),Modifier.weight(1f),fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text(money(o.optDouble("amount")),Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=if(income)appGreen(dark) else appInk(dark))}
   if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){
    ReferenceAction("Bearbeiten","action-edit",false){name=o.optString("name");amount=String.format(Locale.GERMANY,"%.2f",o.optDouble("amount"));icon=o.optString("icon").ifBlank{"auto"};editing=true}
    ReferenceAction("Löschen","action-delete",true){deleting=true}
   }
  }
 }
 if(deleting)ReferenceConfirm("Diesen Eintrag wirklich löschen?",onCancel={deleting=false},onConfirm={for(i in array.length()-1 downTo 0)if(array.optJSONObject(i)?.optString("id")==o.optString("id"))array.remove(i);deleting=false;onChanged()})
}
@Composable private fun ReferenceEditInput(value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,decimal:Boolean=false,placeholder:String=""){
 val dark=LocalAppDark.current
 BasicTextField(value,onValue,modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal=14.dp,vertical=13.dp),singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(fontSize=16.sp,color=appInk(dark)),cursorBrush=androidx.compose.ui.graphics.SolidColor(appBlue(dark)),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=if(decimal)androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text),decorationBox={inner->Box{if(value.isEmpty())Text(placeholder,fontSize=16.sp,color=appInkSoft(dark));inner()}})
}
@Composable private fun ReferenceEditActions(onCancel:()->Unit,saveLabel:String="Speichern",saveColor:Color?=null,onSave:()->Unit){
 val dark=LocalAppDark.current
 Row(Modifier.fillMaxWidth().padding(top=2.dp),horizontalArrangement=Arrangement.spacedBy(10.dp,Alignment.End)){
  Surface(Modifier.clickable(onClick=onCancel),shape=RoundedCornerShape(999.dp),color=if(dark)Color(0xFF52545C) else Color.White){Text("Abbrechen",Modifier.padding(horizontal=22.dp,vertical=12.dp),fontSize=14.5.sp,fontWeight=FontWeight.Bold,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}
  Surface(Modifier.clickable(onClick=onSave),shape=RoundedCornerShape(999.dp),color=saveColor?:appBlue(dark)){Text(saveLabel,Modifier.padding(horizontal=22.dp,vertical=12.dp),fontSize=14.5.sp,fontWeight=FontWeight.Bold,color=Color.White)}
 }
}
@Composable private fun ReferenceAction(label:String,icon:String,danger:Boolean,buttonColor:Color?=null,onClick:()->Unit){
 val dark=LocalAppDark.current
 Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
  Surface(Modifier.height(40.dp).widthIn(min=48.dp).clickable(onClick=onClick),shape=RoundedCornerShape(999.dp),color=buttonColor?:if(danger)appRed(dark) else if(dark)Color(0xFF52545C) else Color.White){Box(Modifier.padding(horizontal=12.dp),contentAlignment=Alignment.Center){HtmlReferenceIcon(icon,if(danger||buttonColor!=null)Color.White else if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40),15.dp)}}
  Text(label,fontSize=9.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark))
 }
}
@Composable private fun ReferenceIconPicker(selected:String,onSelect:(String)->Unit){
 val dark=LocalAppDark.current
 val groups=listOf("auto" to listOf("auto"))+referenceIconLabels.keys.filter{it!="auto"}.groupBy{fixedGroupKey(JSONObject().put("icon",it))}.entries.map{it.key to it.value}
 BoxWithConstraints(Modifier.fillMaxWidth()){
 val columns=((maxWidth.value+10f)/68f).toInt().coerceAtLeast(1)
 Column{groups.forEach{(group,keys)->
  Text(if(group=="auto")"AUTOMATISCH" else fixedGroupLabel(group).uppercase(Locale.GERMAN),Modifier.padding(top=8.dp,bottom=4.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.33.sp,color=appInkSoft(dark))
  keys.chunked(columns).forEach{row->Row(Modifier.fillMaxWidth().padding(top=2.dp,bottom=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{key->Column(Modifier.width(58.dp).clickable{onSelect(key)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
   Box(Modifier.size(38.dp).clip(CircleShape).background(referenceIconColors[key]?:Color.Gray).border(2.dp,if(key==selected)appInk(dark) else Color.Transparent,CircleShape),contentAlignment=Alignment.Center){HtmlReferenceIcon("fixed-"+key,Color.White,17.dp,2.6f)}
   Text(referenceIconLabels[key].orEmpty(),fontSize=10.sp,lineHeight=12.sp,textAlign=TextAlign.Center,color=if(key==selected)appInk(dark) else appInkSoft(dark),fontWeight=if(key==selected)FontWeight.Bold else FontWeight.Normal)
  }}}}
 }
 }}
}
@Composable private fun ReferenceMessage(message:String,icon:String="✅",onDismiss:()->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).padding(24.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){Surface(Modifier.widthIn(max=340.dp).fillMaxWidth().clickable{},shape=RoundedCornerShape(26.dp),color=appGlass(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){Column(Modifier.padding(start=26.dp,end=26.dp,top=28.dp,bottom=22.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Box(Modifier.size(56.dp).clip(CircleShape).background(appExpenseSoft(dark)),contentAlignment=Alignment.Center){Text(icon,fontSize=26.sp)};Spacer(Modifier.height(16.dp));Text(message,fontSize=16.5.sp,lineHeight=23.1.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=appInk(dark));Spacer(Modifier.height(22.dp))
   Surface(Modifier.fillMaxWidth().clickable(onClick=onDismiss),shape=RoundedCornerShape(999.dp),color=appBlue(dark)){Text("OK",Modifier.padding(horizontal=10.dp,vertical=14.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=Color.White)}
  }}}
 }
}
@Composable private fun ReferenceConfirm(message:String,icon:String="🗑️",confirmLabel:String="Löschen",onCancel:()->Unit,onConfirm:()->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onCancel,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).padding(24.dp).clickable(onClick=onCancel),contentAlignment=Alignment.Center){Surface(Modifier.widthIn(max=340.dp).fillMaxWidth().clickable{},shape=RoundedCornerShape(26.dp),color=appGlass(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){
   Column(Modifier.padding(start=26.dp,end=26.dp,top=28.dp,bottom=22.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(56.dp).clip(CircleShape).background(appExpenseSoft(dark)),contentAlignment=Alignment.Center){Text(icon,fontSize=26.sp)};Spacer(Modifier.height(16.dp));Text(message,fontSize=16.5.sp,lineHeight=23.1.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=appInk(dark));Spacer(Modifier.height(22.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Surface(Modifier.weight(1f).clickable(onClick=onCancel),shape=RoundedCornerShape(999.dp),color=if(dark)Color(0xFF52545C) else Color.White){Text("Abbrechen",Modifier.padding(horizontal=10.dp,vertical=14.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}
    Surface(Modifier.weight(1f).clickable(onClick=onConfirm),shape=RoundedCornerShape(999.dp),color=appRed(dark)){Text(confirmLabel,Modifier.padding(horizontal=10.dp,vertical=14.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=Color.White)}
   }}
  }}
 }
}
@Composable private fun InvoiceEditableRow(o:JSONObject,state:JSONObject,repo:HouseholdRepository,onChanged:()->Unit){
 val dark=LocalAppDark.current
 var edit by remember(o.optString("id")){mutableStateOf(false)};var expanded by remember(o.optString("id")){mutableStateOf(false)};var deleting by remember{o.let{mutableStateOf(false)}}
 var name by remember(o.optString("id")){mutableStateOf(o.optString("name"))};var amount by remember(o.optString("id")){mutableStateOf(String.format(Locale.GERMANY,"%.2f",o.optDouble("amount")))};var date by remember(o.optString("id")){mutableStateOf(o.optString("dueDate"))}
 if(edit){Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(appPrimarySoft(dark)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  ReferenceEditInput(name,{name=it})
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Column(Modifier.weight(1f)){Text("Betrag",Modifier.padding(start=4.dp,bottom=4.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));ReferenceEditInput(amount,{amount=centInput(it)},decimal=true)}
   Column(Modifier.weight(1f)){Text("Fälligkeitsdatum",Modifier.padding(start=4.dp,bottom=4.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));ReferenceDateInput(date,edit=true){date=it}}
  }
  ReferenceEditActions({edit=false}){val value=parseNativeAmount(amount);if(name.trim().isNotEmpty()&&value!=null&&runCatching{LocalDate.parse(date)}.isSuccess){o.put("name",name.trim()).put("amount",value).put("dueDate",date);edit=false;onChanged()}}
 }}else{
  val due=runCatching{LocalDate.parse(o.optString("dueDate"))}.getOrNull();val overdue=due?.isBefore(LocalDate.now())==true;val dueThisMonth=due?.let{YearMonth.from(it)==YearMonth.now()}==true
  Column(Modifier.fillMaxWidth().background(if(dueThisMonth)appExpenseSoft(dark) else appSavingsSoft(dark)).clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(2.dp,appRuleStrong(dark),CircleShape).clickable{o.put("paid",true).put("paidDate",LocalDate.now().toString());onChanged()})
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)){Text(o.optString("name"),fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text((if(overdue)"Überfällig seit " else "Fällig am ")+(due?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))?:""),Modifier.padding(top=2.dp),fontSize=11.5.sp,color=if(overdue)appRed(dark) else appInkSoft(dark))}
    Text(money(o.optDouble("amount")),Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=appInk(dark))
   }
   if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){ReferenceAction("Bearbeiten","action-edit",false){edit=true};ReferenceAction("Löschen","action-delete",true){deleting=true}}
  }
 }
 if(deleting)InvoiceDeleteConfirm(o,state,repo,{deleting=false},onChanged)
}
@Composable private fun PaidInvoiceEditableRow(o:JSONObject,state:JSONObject,repo:HouseholdRepository,onChanged:()->Unit){
 var expanded by remember(o.optString("id")){mutableStateOf(false)};var deleting by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.optString("name"),fontSize=14.sp);Text("Bezahlt am "+runCatching{LocalDate.parse(o.optString("paidDate")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault("–"),fontSize=11.5.sp,color=appInkSoft(LocalAppDark.current),modifier=Modifier.padding(top=2.dp))};Text(money(o.optDouble("amount")),Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=appGreen(LocalAppDark.current))}
  if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){ReferenceAction("Als offen markieren","action-reopen",false){o.put("paid",false).put("paidDate","");onChanged()};ReferenceAction("Löschen","action-delete",true){deleting=true}}
 }
 if(deleting)InvoiceDeleteConfirm(o,state,repo,{deleting=false},onChanged)
}
@Composable private fun InvoiceDeleteConfirm(o:JSONObject,state:JSONObject,repo:HouseholdRepository,onCancel:()->Unit,onChanged:()->Unit){ReferenceConfirm("Diese Rechnung wirklich löschen?",onCancel=onCancel,onConfirm={val a=repo.array(state,"openInvoices");for(i in a.length()-1 downTo 0)if(a.optJSONObject(i)?.optString("id")==o.optString("id"))a.remove(i);onCancel();onChanged()})}
@Composable private fun EditableFixedGroup(key:String,xs:List<JSONObject>,state:JSONObject,repo:HouseholdRepository,array:JSONArray,onChanged:()->Unit){val InkSoft=appInkSoft(LocalAppDark.current);Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current)).padding(horizontal=16.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(22.dp).clip(CircleShape).background(fixedGroupColor(key)),contentAlignment=Alignment.Center){HtmlReferenceIcon("fixed-"+fixedGroupIcon(key),Color.White,17.dp,2.6f)};Spacer(Modifier.width(8.dp));Text(fixedGroupLabel(key).uppercase(Locale.GERMAN),Modifier.weight(1f),fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.345.sp,color=InkSoft);Text(money(xs.sumOf{it.optDouble("amount")}),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=InkSoft)};xs.forEach{EditableMoneyRow(it,state,repo,array,false,onChanged)}};Spacer(Modifier.height(14.dp))}
@Composable private fun PaidInvoicesHeader(count:Int,open:Boolean,onClick:()->Unit){val InkSoft=appInkSoft(LocalAppDark.current);val Blue=appBlue(LocalAppDark.current);Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Blue),contentAlignment=Alignment.Center){Canvas(Modifier.size(17.dp)){val s=Color.White;val st=Stroke(2.dp.toPx());drawRoundRect(s,Offset(size.width*.12f,size.height*.08f),Size(size.width*.76f,size.height*.84f),CornerRadius(1.5.dp.toPx()),style=st);drawLine(s,Offset(size.width*.35f,size.height*.58f),Offset(size.width*.47f,size.height*.7f),2.dp.toPx());drawLine(s,Offset(size.width*.47f,size.height*.7f),Offset(size.width*.72f,size.height*.43f),2.dp.toPx())}};Column(Modifier.weight(1f)){Text("Bezahlte Rechnungen",fontSize=15.sp,fontWeight=FontWeight.Bold);Text("$count "+if(count==1)"Rechnung" else "Rechnungen",fontSize=13.sp,color=InkSoft,modifier=Modifier.padding(top=1.dp))};Canvas(Modifier.size(18.dp)){val y=if(open)size.height*.62f else size.height*.38f;val y2=if(open)size.height*.38f else size.height*.62f;drawLine(InkSoft,Offset(size.width*.2f,y),Offset(size.width*.5f,y2),2.4.dp.toPx());drawLine(InkSoft,Offset(size.width*.5f,y2),Offset(size.width*.8f,y),2.4.dp.toPx())}}}
@Composable private fun PaidInvoiceRow(o:JSONObject){val InkSoft=appInkSoft(LocalAppDark.current);val Green=appGreen(LocalAppDark.current);val d=if(o.optString("paidDate").isBlank())"–" else runCatching{LocalDate.parse(o.optString("paidDate")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault("–");Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.optString("name"),fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text("Bezahlt am $d",fontSize=11.5.sp,color=InkSoft,modifier=Modifier.padding(top=2.dp))};Text(money(o.optDouble("amount")),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=Green,modifier=Modifier.padding(start=10.dp))}}
private fun fixedIconKey(o:JSONObject):String{if(o.has("icon")&&!o.isNull("icon")&&o.optString("icon").isNotBlank())return o.optString("icon");val n=o.optString("name").lowercase(Locale.GERMAN);val rules=listOf(
 "house" to listOf("miete","wohnung","nebenkosten","hausgeld"),"bolt" to listOf("strom","energie","elektr"),"drop" to listOf("wasser"),"flame" to listOf("gas","heizung","fernwärme"),"wifi" to listOf("internet","wlan","dsl","glasfaser","fiber","router"),"phone" to listOf("handy","mobil","congstar","telekom","vodafone","o2","1&1","1und1","simyo","fonic","klarmobil","aldi talk","telefon"),"shield" to listOf("versicherung","haftpflicht"),"backpack" to listOf("kindergarten","kita","hort","schule"),"ballerina" to listOf("ballett","tanz"),"dumbbell" to listOf("fitness","sport","gym","yoga"),"music" to listOf("spotify","musik","deezer","apple music"),"film" to listOf("netflix","disney","amazon prime","streaming","magenta tv","sky","dazn","wow","paramount"),"car" to listOf("auto","kfz","tüv","tuv"),"fuel" to listOf("tank","tanken","benzin","diesel","sprit","tankstelle"),"card" to listOf("sparkasse","bank","gebühr","kredit","darlehen"),"piggy" to listOf("sparen","spar","rücklage","ruecklage"),"food" to listOf("essen","lebensmittel","restaurant","supermarkt","einkauf","edeka","rewe","lidl","aldi"),"shirt" to listOf("kleidung","klamotten","schuhe"),"ticket" to listOf("freizeit","hobby","kino","konzert","event","ticket"),"heart" to listOf("gesundheit","arzt","apotheke","medizin","zahnarzt"),"gift" to listOf("geschenk","geschenke"),"suitcase" to listOf("reise","urlaub","flug","hotel"),"paw" to listOf("tier","hund","katze","haustier"));return rules.firstOrNull{(_,ks)->ks.any{n.contains(it)}}?.first?:"tag"}
private fun fixedGroupKey(o:JSONObject)=when(fixedIconKey(o)){"house","bolt","drop","flame"->"wohnen";"wifi","phone","music","film"->"medien";"shield"->"versicherung";"backpack"->"kinder";"ballerina","dumbbell","ticket"->"freizeit";"car"->"auto";"card","piggy"->"finanzen";"food","shirt","fuel"->"alltag";"heart"->"gesundheit";else->"sonstiges"}
private fun groupFixed(xs:List<JSONObject>)=xs.groupBy{fixedGroupKey(it)}.entries.sortedByDescending{e->e.value.sumOf{it.optDouble("amount")}}.map{it.key to it.value}
private fun fixedGroupLabel(k:String)=when(k){"wohnen"->"Wohnen";"medien"->"Verträge & Medien";"versicherung"->"Versicherungen";"kinder"->"Kinder & Bildung";"freizeit"->"Sport & Freizeit";"auto"->"Auto";"finanzen"->"Bank & Sparen";"alltag"->"Alltag & Einkauf";"gesundheit"->"Gesundheit";else->"Sonstiges"}
private fun fixedGroupIcon(k:String)=when(k){"wohnen"->"house";"medien"->"film";"versicherung"->"shield";"kinder"->"backpack";"freizeit"->"dumbbell";"auto"->"car";"finanzen"->"card";"alltag"->"food";"gesundheit"->"heart";else->"tag"}
private fun fixedGroupColor(k:String)=when(k){"wohnen"->Color(0xFFF5A623);"medien"->Color(0xFFE63946);"versicherung"->Color(0xFF5E5CE6);"kinder"->Color(0xFFFF375F);"freizeit"->Color(0xFFFF9500);"auto"->Color(0xFF64B5F6);"finanzen"->Color(0xFF30D158);"alltag"->Color(0xFFFF9F0A);"gesundheit"->Color(0xFFFF453A);else->Color(0xFF8E8E93)}
@Composable private fun FixedGroup(key:String,xs:List<JSONObject>){val InkSoft=appInkSoft(LocalAppDark.current);Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current)).padding(horizontal=16.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(22.dp).clip(CircleShape).background(fixedGroupColor(key)),contentAlignment=Alignment.Center){HtmlReferenceIcon("fixed-"+fixedGroupIcon(key),Color.White,17.dp,2.6f)};Spacer(Modifier.width(8.dp));Text(fixedGroupLabel(key).uppercase(Locale.GERMAN),Modifier.weight(1f),fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.345.sp,color=InkSoft);Text(money(xs.sumOf{it.optDouble("amount")}),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=InkSoft)};xs.forEach{OriginalCompactRow(it.optString("name"),it.optDouble("amount"),false)}};Spacer(Modifier.height(14.dp))}
@Composable private fun AutoFixedGroup(loans:Double,extra:Double,vc:Double){val InkSoft=appInkSoft(LocalAppDark.current);Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current)).padding(horizontal=16.dp,vertical=9.dp)){Text("AUTOMATISCH BERECHNET",fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.345.sp,color=InkSoft)};if(loans>0)OriginalCompactRow("Kreditraten gesamt",loans,false);if(extra>0)OriginalCompactRow("Extra-Tilgung (Tilgungsplan)",extra,false);if(vc>0)OriginalCompactRow("Sparbedarf (Variable Kosten)",vc,false)}}
@Composable private fun OriginalSubNav(label:String,active:Boolean,modifier:Modifier,onClick:()->Unit){
 val dark=LocalAppDark.current;val color=if(active)appInk(dark) else appInkSoft(dark)
 Surface(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(11.dp),color=if(active)appGlass(dark) else Color.Transparent,shadowElevation=if(active)1.dp else 0.dp){Row(Modifier.padding(vertical=11.dp,horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically){if(htmlIconPaths.containsKey(label))HtmlReferenceIcon(label,color,16.dp);Text(label,fontSize=13.5.sp,fontWeight=FontWeight.Bold,color=color)}}
}
private fun Modifier.htmlHeroScale(collapsed:Boolean)=graphicsLayer{scaleX=if(collapsed).56f else 1f;scaleY=if(collapsed).56f else 1f;transformOrigin=androidx.compose.ui.graphics.TransformOrigin(0f,.5f)}
@Composable private fun OriginalHero(label:String,value:String,sub1:String?=null,val1:String?=null,sub2:String?=null,val2:String?=null,collapsible:Boolean=false){
    var collapsed by remember(label){mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).then(if(collapsible)Modifier.clickable{collapsed=!collapsed} else Modifier).htmlGradient(listOf(heroBlueStart(LocalAppDark.current),heroBlueEnd(LocalAppDark.current))).padding(start=20.dp,end=20.dp,top=if(collapsed)10.dp else 22.dp,bottom=if(collapsible)0.dp else 22.dp)){
        if(!collapsed){Text(label,fontSize=13.sp,color=heroBlueText(LocalAppDark.current));Spacer(Modifier.height(6.dp))};Text(value,fontSize=34.sp,modifier=Modifier.htmlHeroScale(collapsed),fontWeight=FontWeight.ExtraBold,letterSpacing=(-.68).sp,color=MaterialTheme.colorScheme.onSurface)
        if(!collapsed&&sub1!=null&&val1!=null){Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Column{Text(sub1,fontSize=11.sp,color=heroBlueSub(LocalAppDark.current));Spacer(Modifier.height(2.dp));Text(val1,fontSize=14.sp,fontWeight=FontWeight.Bold)};if(sub2!=null&&val2!=null)Column{Text(sub2,fontSize=11.sp,color=heroBlueSub(LocalAppDark.current));Spacer(Modifier.height(2.dp));Text(val2,fontSize=14.sp,fontWeight=FontWeight.Bold)}}}
        if(collapsible)Box(Modifier.fillMaxWidth().padding(top=14.dp,bottom=10.dp).rotate(if(collapsed)180f else 0f),contentAlignment=Alignment.Center){HtmlReferenceIcon("chevron-up",heroBlueText(LocalAppDark.current).copy(alpha=.85f),16.dp,2.4f)}
    }
}
@Composable private fun OriginalEmpty(text:String)=Text(text,Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=34.dp),textAlign=TextAlign.Center,fontSize=15.5.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
private fun intervalMonthsExact(i:String)=when(i){"monatlich"->1;"vierteljaehrlich"->3;"halbjaehrlich"->6;"zweijaehrlich"->24;else->12}
private fun monthsUntilNextDueExact(o:JSONObject):Int{val cycle=intervalMonthsExact(o.optString("interval"));val now=LocalDate.now();val ay=if(o.optString("interval")=="zweijaehrlich"&&o.has("dueYear")&&!o.isNull("dueYear"))o.optInt("dueYear") else now.year;val cur=now.year*12+now.monthValue-1;val anchor=ay*12+o.optInt("dueMonth",1)-1;return anchor+kotlin.math.ceil((cur-anchor).toDouble()/cycle).toInt()*cycle-cur}
private fun paidThisCycleExact(o:JSONObject,p:List<JSONObject>):Boolean{if(monthsUntilNextDueExact(o)!=0)return false;val m=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));return p.any{it.optString("itemId")==o.optString("id")&&it.optString("date").startsWith(m)}}
private fun intervalLabelExact(o:JSONObject):String{val d=LocalDate.now().withDayOfMonth(1).plusMonths(monthsUntilNextDueExact(o).toLong());val mn=d.month.getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN);return when(o.optString("interval")){"monatlich"->"Monatlich";"vierteljaehrlich"->"Vierteljährlich · Fällig: "+mn;"halbjaehrlich"->"Halbjährlich · Fällig: "+mn;"zweijaehrlich"->"Alle 2 Jahre · Fällig: "+mn+" "+d.year;else->"Jährlich · Fällig: "+mn}}
private fun goalLabelExact(i:String)=when(i){"monatlich"->"Zielbetrag (1×)";"vierteljaehrlich"->"Zielbetrag (3×)";"halbjaehrlich"->"Zielbetrag (6×)";"zweijaehrlich"->"Zielbetrag (24×)";else->"Zielbetrag (12×)"}
@Composable private fun VariableScreen(state:JSONObject,repo:HouseholdRepository,view:String,onView:(String)->Unit){val costs=repo.array(state,"variableCosts");val contrib=repo.array(state,"variableContributions");val payments=repo.array(state,"variablePayments");val paid=sum(contrib,"amount")+state.optDouble("potCarryover",0.0);val spent=sum(payments,"amount");Column(Modifier.fillMaxSize().subViewSwipe(listOf("kostenarten","spartopf"),view){onView(it)}.padding(horizontal=18.dp)){Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current),RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Kostenarten",view=="kostenarten",Modifier.weight(1f)){onView("kostenarten")};OriginalSubNav("Spartopf",view=="spartopf",Modifier.weight(1f)){onView("spartopf")}};Spacer(Modifier.height(18.dp));if(view=="kostenarten"){OriginalHero("Monatlicher Sparbedarf",money(sum(costs,"monthlyAmount")),"Kostenarten",costs.length().toString());LazyColumn{if(costs.length()==0)item{OriginalEmpty("Noch keine Kostenarten angelegt.")};val payList=payments.objects();val sorted=costs.objects().sortedWith(compareBy<JSONObject>{if(paidThisCycleExact(it,payList))1 else 0}.thenBy{monthsUntilNextDueExact(it)});items(sorted){VariableCostCard(it,payList,state,repo)}}}else{OriginalHero("Rest im Spartopf",money(paid-spent),"Eingezahlt",money(paid),"Ausgegeben",money(spent),true);Spacer(Modifier.height(12.dp));PotHistory(contrib.objects(),payments.objects(),costs.objects(),state,repo)}}}
private data class PotTx(val id:String,val date:String,val amount:Double,val label:String)
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun PotHistory(contrib:List<JSONObject>,payments:List<JSONObject>,costs:List<JSONObject>,state:JSONObject,repo:HouseholdRepository){
 val dark=LocalAppDark.current;val names=costs.associate{it.optString("id") to it.optString("name")}
 val all=buildList{contrib.forEach{add(PotTx(it.optString("id"),it.optString("date"),it.optDouble("amount"),it.optString("note").ifBlank{"Einzahlung"}))};payments.forEach{p->add(PotTx(p.optString("id"),p.optString("date"),-p.optDouble("amount"),names[p.optString("itemId")]?:p.optString("itemName").ifBlank{"Ausgabe"}))}}
 val years=(all.mapNotNull{runCatching{LocalDate.parse(it.date).year}.getOrNull()}+LocalDate.now().year).distinct().sorted();var selectedYear by remember{mutableStateOf(years.last())};val year=if(selectedYear in years)selectedYear else years.last()
 val carry=all.filter{runCatching{LocalDate.parse(it.date).year<year}.getOrDefault(false)}.sumOf{it.amount};val txs=all.filter{runCatching{LocalDate.parse(it.date).year==year}.getOrDefault(false)}.sortedByDescending{it.date}
 LazyColumn{
  item{Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(appBlue(dark)),contentAlignment=Alignment.Center){HtmlReferenceIcon("Spartopf",Color.White,17.dp)};Spacer(Modifier.width(14.dp));Column{Text("Verlauf Spartopf",fontSize=15.sp,fontWeight=FontWeight.Bold);Text(all.size.toString()+if(all.size==1)" Buchung" else " Buchungen",fontSize=13.sp,color=appInkSoft(dark))}}}
  item{FlowRow(Modifier.padding(horizontal=4.dp).padding(bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){years.forEach{y->Surface(shape=RoundedCornerShape(50),color=if(y==year)appBlue(dark) else appPaper(dark),modifier=Modifier.clickable{selectedYear=y}){Text(y.toString(),Modifier.padding(horizontal=14.dp,vertical=7.dp),fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(y==year)Color.White else appInkSoft(dark))}}}}
  if(carry!=0.0)item{Row(Modifier.fillMaxWidth().padding(vertical=10.dp).clip(RoundedCornerShape(14.dp)).background(appPrimarySoft(dark)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(if(carry>=0)"Übertrag aus Vorjahren" else "Übertrag aus Vorjahr",Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.Bold,color=appBlue(dark));Text((if(carry>=0)"+" else "")+money(carry),fontSize=16.sp,fontWeight=FontWeight.Bold,color=appBlue(dark))}}
  if(txs.isEmpty())item{OriginalEmpty(if(carry==0.0)"Noch keine Buchungen im Spartopf." else "Keine Buchungen in $year.")}
  items(txs,key={it.id}){t->
   var action by remember(t.id){mutableStateOf("")};var expanded by remember(t.id){mutableStateOf(false)};var editDate by remember(t.id){mutableStateOf(t.date)};var editAmount by remember(t.id){mutableStateOf(String.format(Locale.GERMANY,"%,.2f",kotlin.math.abs(t.amount)))}
   if(action=="edit")ReferenceEditGrid{
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){ReferenceDateInput(editDate,edit=true){editDate=it}};Box(Modifier.weight(1f)){ReferenceEditInput(editAmount,{editAmount=centInput(it)},decimal=true)}}
    ReferenceEditActions({action=""}){val a=parseNativeAmount(editAmount);if(runCatching{LocalDate.parse(editDate)}.isSuccess&&a!=null&&a>0){val arr=repo.array(state,if(t.amount>=0)"variableContributions" else "variablePayments");arr.objects().firstOrNull{it.optString("id")==t.id}?.apply{put("date",editDate);put("amount",a)};repo.save(state);action=""}}
   }else Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.label,fontSize=14.sp);Text(runCatching{LocalDate.parse(t.date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault(t.date),fontSize=11.5.sp,color=appInkSoft(dark),modifier=Modifier.padding(top=2.dp))};Text((if(t.amount>=0)"+" else "")+money(t.amount),Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=if(t.amount>=0)appGreen(dark) else appRed(dark))}
    if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){ReferenceAction("Bearbeiten","action-edit",false){editDate=t.date;editAmount=String.format(Locale.GERMANY,"%,.2f",kotlin.math.abs(t.amount));action="edit"};ReferenceAction("Löschen","action-delete",true){action="delete"}}
   }
   if(action=="delete")ReferenceConfirm("Diese Buchung wirklich löschen?",onCancel={action=""},onConfirm={val arr=repo.array(state,if(t.amount>=0)"variableContributions" else "variablePayments");for(i in arr.length()-1 downTo 0)if(arr.optJSONObject(i)?.optString("id")==t.id)arr.remove(i);action="";repo.save(state)})
  }
 }
}

private fun paidThisYearExact(item:JSONObject,pays:List<JSONObject>):Boolean{
 val y=LocalDate.now().year.toString()
 return pays.any{it.optString("itemId")==item.optString("id")&&it.optString("date").startsWith(y)}
}
private fun nothingDueRestOfYearExact(item:JSONObject,pays:List<JSONObject>):Boolean{
 val cycle=intervalMonthsExact(item.optString("interval"));val anchor=item.optInt("dueMonth",1);val now=LocalDate.now();val year=now.year
 val dueYear=if(item.has("dueYear")&&!item.isNull("dueYear"))item.optInt("dueYear") else 0
 val anchorYear=if(item.optString("interval")=="zweijaehrlich"&&dueYear>0)dueYear else year
 val anchorAbs=anchorYear*12+(anchor-1);val currentAbs=year*12+(now.monthValue-1)
 val step=kotlin.math.ceil((currentAbs-anchorAbs).toDouble()/cycle).toInt();val nextAbs=anchorAbs+step*cycle
 val nextYear=Math.floorDiv(nextAbs,12)
 if(nextYear>year){val lastYear=Math.floorDiv(nextAbs-cycle,12);if(lastYear<year)return true;return paidThisYearExact(item,pays)}
 return false
}
@Composable private fun VariableCostCard(o:JSONObject,pays:List<JSONObject>,state:JSONObject,repo:HouseholdRepository){val Green=appGreen(LocalAppDark.current);val Red=appRed(LocalAppDark.current);
 val monthly=o.optDouble("monthlyAmount");val actual=o.optDouble("actualAmount");val months=intervalMonthsExact(o.optString("interval"));val goal=monthly*months;val buffer=goal-actual;val due=monthsUntilNextDueExact(o)==0;val paid=paidThisCycleExact(o,pays);val doneYear=nothingDueRestOfYearExact(o,pays);val bg=if(due&&!paid)appExpenseSoft(LocalAppDark.current) else if((due&&paid)||doneYear)appSavingsSoft(LocalAppDark.current) else MaterialTheme.colorScheme.surface
 var mode by remember{o.let{mutableStateOf("")}};var expanded by remember(o.optString("id")){mutableStateOf(false)};var date by remember{mutableStateOf(LocalDate.now().toString())};var amount by remember{o.let{mutableStateOf(if(actual>0)actual.toString() else "")}};var editName by remember{o.let{mutableStateOf(o.optString("name"))}};var editMonthly by remember{o.let{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("monthlyAmount")))}};var editActual by remember{o.let{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("actualAmount")))}};var editInterval by remember{o.let{mutableStateOf(o.optString("interval").ifBlank{"jaehrlich"})}};var editDueMonth by remember{o.let{mutableStateOf(o.optInt("dueMonth",1).toString())}};var editDueYear by remember{o.let{mutableStateOf(if(o.has("dueYear")&&!o.isNull("dueYear"))o.optInt("dueYear").toString() else "")}}
  if(mode=="edit"){VariableCostEditForm(editName,{editName=it},editMonthly,{editMonthly=it},editActual,{editActual=it},editInterval,{editInterval=it},editDueMonth,{editDueMonth=it},editDueYear,{editDueYear=it},{mode=""}){val m=parseNativeAmount(editMonthly);val a=parseNativeAmount(editActual);val dm=editDueMonth.toIntOrNull();if(editName.isNotBlank()&&m!=null&&dm!=null&&dm in 1..12){o.put("name",editName.trim());o.put("monthlyAmount",m);o.put("actualAmount",a?:0.0);o.put("interval",editInterval);o.put("dueMonth",dm);if(editInterval=="zweijaehrlich"&&editDueYear.toIntOrNull()!=null)o.put("dueYear",editDueYear.toInt()) else o.remove("dueYear");repo.save(state);mode=""}};return}
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).clickable{expanded=!expanded}.background(bg).padding(horizontal=20.dp,vertical=18.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text(o.optString("name"),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text(money(monthly)+" / Monat",fontSize=14.5.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  Spacer(Modifier.height(10.dp));VariableDetail("Fälligkeit",intervalLabelExact(o));VariableDetail(goalLabelExact(o.optString("interval")),money(goal));VariableDetail("Letzte Rechnung",money(actual));VariableDetail("Puffer",money(buffer),if(buffer>=0)Green else Red)
  if(expanded)Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.SpaceBetween){
   ReferenceAction("Bearbeiten","action-edit",false){editName=o.optString("name");editMonthly=String.format(Locale.GERMANY,"%,.2f",monthly);editActual=String.format(Locale.GERMANY,"%,.2f",actual);editInterval=o.optString("interval");editDueMonth=o.optInt("dueMonth").toString();editDueYear=if(o.has("dueYear")&&!o.isNull("dueYear"))o.optInt("dueYear").toString() else "";mode="edit"}
   ReferenceAction("Als bezahlt buchen","action-check",false,buttonColor=appBlue(LocalAppDark.current)){date=LocalDate.now().toString();amount=actual.toString();mode="pay"}
   ReferenceAction("Löschen","action-delete",true){mode="delete"}
  }

  if(mode=="pay")LoanActionForm(date,{date=it},amount,{amount=it},{mode=""}){val a=parseNativeAmount(amount);if(runCatching{LocalDate.parse(date)}.isSuccess&&a!=null&&a>0){o.put("actualAmount",a);repo.array(state,"variablePayments").put(JSONObject().put("id",genId()).put("itemId",o.optString("id")).put("date",date).put("amount",a));repo.save(state);mode=""}}


 }
 if(mode=="delete")ReferenceConfirm("Diese Kostenart wirklich löschen? Die bisherige Zahlungshistorie bleibt im Verlauf erhalten.",onCancel={mode=""},onConfirm={val id=o.optString("id");repo.array(state,"variablePayments").objects().filter{it.optString("itemId")==id}.forEach{it.put("itemName",o.optString("name"))};val arr=repo.array(state,"variableCosts");for(i in arr.length()-1 downTo 0)if(arr.optJSONObject(i)?.optString("id")==id)arr.remove(i);mode="";repo.save(state)})
}

@Composable private fun ReferenceEditGrid(gold:Boolean=false,content:@Composable ColumnScope.()->Unit){
 val dark=LocalAppDark.current
 Column(Modifier.fillMaxWidth().padding(bottom=6.dp).clip(RoundedCornerShape(18.dp)).background(if(gold)if(dark)Color(0xFF3B2E16) else Color(0xFFFFEFD6) else appPrimarySoft(dark)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable private fun ReferenceEditLabeled(label:String,value:String,onValue:(String)->Unit,placeholder:String,decimal:Boolean=true){Column{Text(label,Modifier.heightIn(min=28.dp).padding(start=4.dp,bottom=4.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(LocalAppDark.current));ReferenceEditInput(value,onValue,decimal=decimal,placeholder=placeholder)}}
@Composable private fun VariableCostEditForm(name:String,onName:(String)->Unit,monthly:String,onMonthly:(String)->Unit,actual:String,onActual:(String)->Unit,interval:String,onInterval:(String)->Unit,dueMonth:String,onDueMonth:(String)->Unit,dueYear:String,onDueYear:(String)->Unit,onCancel:()->Unit,onSave:()->Unit){
 ReferenceEditGrid{
  ReferenceEditInput(name,onName,placeholder="Bezeichnung")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){ReferenceEditLabeled("Monatlich zurücklegen",monthly,{onMonthly(centInput(it))},"Monatlich (€)")};Column(Modifier.weight(1f)){ReferenceEditLabeled("Letzte Rechnung",actual,{onActual(centInput(it))},"Rechnung (€)")}}
  ReferenceSelect(interval,listOf("monatlich" to "Monatlich fällig","vierteljaehrlich" to "Vierteljährlich fällig","halbjaehrlich" to "Halbjährlich fällig","jaehrlich" to "Jährlich fällig","zweijaehrlich" to "Alle 2 Jahre fällig"),edit=true,onValue=onInterval)
  ReferenceSelect(dueMonth,(1..12).map{it.toString() to java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN)},edit=true,onValue=onDueMonth)
  if(interval=="zweijaehrlich")ReferenceEditInput(dueYear,{onDueYear(it.filter(Char::isDigit))},placeholder="Startjahr (z. B. 2024)")
  ReferenceEditActions(onCancel,onSave=onSave)
 }
}
@Composable private fun VariableDetail(label:String,value:String,color:Color=MaterialTheme.colorScheme.onSurface){Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(label,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Text(value,fontSize=14.sp,fontWeight=FontWeight.Bold,color=color)}}
@Composable private fun LoansScreen(state:JSONObject,repo:HouseholdRepository,view:String,onView:(String)->Unit,onFabAllowed:(Boolean)->Unit){
 val loans=remember(state,repo.revision){repo.array(state,"loans").objects()};LaunchedEffect(view){onFabAllowed(view=="kredite")}
 val active=loans.filter{it.optDouble("remainingBalance")>0};val original=active.sumOf{it.optDouble("originalAmount")};val remaining=active.sumOf{it.optDouble("remainingBalance")};val monthly=active.sumOf{it.optDouble("monthlyPayment")}
 Column(Modifier.fillMaxSize().subViewSwipe(listOf("kredite","tilgung"),view){onView(it)}.padding(horizontal=18.dp)){
  Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current),RoundedCornerShape(14.dp)).padding(4.dp)){OriginalSubNav("Kredite",view=="kredite",Modifier.weight(1f)){onView("kredite")};OriginalSubNav("Tilgungsplan",view=="tilgung",Modifier.weight(1f)){onView("tilgung")}}
  Spacer(Modifier.height(18.dp))
  if(view=="kredite"){val payments=repo.array(state,"loanPayments").objects();val month=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));val paid: (JSONObject)->Boolean = { l -> payments.any{it.optString("loanId")==l.optString("id")&&it.optString("type")=="payment"&&it.optString("date").startsWith(month)} };val sorted=active.sortedWith(compareBy<JSONObject>{if(paid(it))1 else 0}.thenBy{it.optDouble("remainingBalance")});val paidLoans=loans.filter{it.optDouble("remainingBalance")<=0}.sortedByDescending{it.optString("paidOffDate")};var showPaid by remember{mutableStateOf(repo.getSetting("household-show-paid-loans")=="1")};LoanHero(remaining,original,monthly);LazyColumn{if(sorted.isEmpty())item{OriginalEmpty("Noch keine laufenden Kredite oder Raten angelegt.")};items(sorted){LoanCardOriginal(it,paid(it),payments,state,repo)};item{PaidLoansHeader(paidLoans.size,showPaid){showPaid=!showPaid;repo.setSetting("household-show-paid-loans",if(showPaid)"1" else "0")}};if(showPaid){if(paidLoans.isEmpty())item{OriginalEmpty("Noch keine abbezahlten Kredite.")}else items(paidLoans){PaidLoanRow(it,state,repo)}}}}
  else{TilgungScreen(active,monthly,repo.array(state,"loanPayments").objects(),repo,state)}
 }
}
@Composable private fun PaidLoansHeader(count:Int,open:Boolean,onClick:()->Unit){
 val dark=LocalAppDark.current
 Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(appBlue(dark)),contentAlignment=Alignment.Center){HtmlReferenceIcon("kredite",Color.White,17.dp)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Abbezahlte Kredite",fontSize=15.sp,fontWeight=FontWeight.Bold);Text(count.toString()+if(count==1)" Kredit" else " Kredite",fontSize=13.sp,color=appInkSoft(dark))};HtmlReferenceIcon(if(open)"chevron-up" else "chevron-down",appInkSoft(dark),18.dp,2.4f)}
}
@Composable private fun PaidLoanRow(o:JSONObject,state:JSONObject,repo:HouseholdRepository){
 val dark=LocalAppDark.current;var action by remember(o.optString("id")){mutableStateOf("")};var expanded by remember(o.optString("id")){mutableStateOf(false)}
 val date=runCatching{LocalDate.parse(o.optString("paidOffDate")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault("–")
 Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.optString("name"),fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text("Kreditsumme "+money(o.optDouble("originalAmount"))+" · Abbezahlt am "+date,fontSize=11.5.sp,color=appInkSoft(dark),modifier=Modifier.padding(top=2.dp))};Text("Abbezahlt",Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=appGreen(dark))}
  if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){ReferenceAction("Wiederherstellen","action-refresh",false,buttonColor=if(dark)Color(0xFFFFB03F) else Color(0xFFFF9F0A)){action="restore"};ReferenceAction("Löschen","action-delete",true){action="delete"}}
 }
 if(action=="restore")ReferenceConfirm("\""+o.optString("name")+"\" wiederherstellen? Die Restschuld wird wieder auf die volle Kreditsumme gesetzt, der Kredit gilt dann wieder als offen.",icon="↩️",confirmLabel="Wiederherstellen",onCancel={action=""},onConfirm={o.put("remainingBalance",o.optDouble("originalAmount"));o.put("paidOffDate",JSONObject.NULL);action="";repo.save(state)})
 if(action=="delete")ReferenceConfirm("Diesen Kredit wirklich löschen? Die zugehörige Ratenhistorie wird ebenfalls entfernt.",onCancel={action=""},onConfirm={val id=o.optString("id");val la=repo.array(state,"loans");for(i in la.length()-1 downTo 0)if(la.optJSONObject(i)?.optString("id")==id)la.remove(i);val pa=repo.array(state,"loanPayments");for(i in pa.length()-1 downTo 0)if(pa.optJSONObject(i)?.optString("loanId")==id)pa.remove(i);action="";repo.save(state)})
}

@Composable private fun TilgungScreen(loans:List<JSONObject>,monthly:Double,payments:List<JSONObject>,repo:HouseholdRepository,state:JSONObject){
 val dark=LocalAppDark.current;var extra by remember{mutableStateOf(repo.getSetting("household-tilgung-extra")?.toDoubleOrNull()?.takeIf{it>=0}?:0.0)};var extraText by remember{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",extra))};var showInfo by remember{mutableStateOf(false)};var confirmMonth by remember{mutableStateOf(false)};var monthRevision by remember{mutableStateOf(0)}
 val currentMonth=remember(monthRevision){tilgungCurrentMonth(repo)};val plan=remember(extra,currentMonth,repo.revision){makePayoffPlan(loans,extra,baseMonth=currentMonth)}
 fun saveExtra(){val v=parseNativeAmount(extraText)?.takeIf{it>=0}?:0.0;extra=v;extraText=String.format(Locale.GERMANY,"%,.2f",v);repo.setSetting("household-tilgung-extra",v.toString())}
 val payoffById=linkedMapOf<String,Pair<PayRow,PayMonth>>();plan.forEach{m->m.rows.filter{it.remaining<=.005}.forEach{r->payoffById[r.id]=r to m}};val payoff=payoffById.values.sortedBy{it.second.month}
 LazyColumn(Modifier.fillMaxWidth()){
  item{
   if(loans.isEmpty())Text("Keine laufenden Kredite - nichts zu tilgen. 🎉",Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=6.dp),textAlign=TextAlign.Center,color=appInkSoft(dark),fontSize=14.sp)
   else if(plan.isEmpty())Text("Betrag zu niedrig, um die Kredite abzuzahlen.",Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=6.dp),textAlign=TextAlign.Center,color=appInkSoft(dark),fontSize=14.sp)
   else{val last=plan.last();val years=last.month/12;val rest=last.month%12;val duration=(if(years>0)"$years "+if(years==1)"Jahr" else "Jahre" else "")+(if(years>0&&rest>0)", " else "")+(if(rest>0)"$rest "+if(rest==1)"Monat" else "Monate" else if(years==0)"0 Monate" else "");OriginalHero("Schuldenfrei in",duration,"Voraussichtlich fertig",last.label,"Betrag/Monat",money(monthly+extra),true)}
   ReferenceSummary{
    Row(Modifier.fillMaxWidth().padding(bottom=10.dp),verticalAlignment=Alignment.CenterVertically){Text("Zusätzlicher Betrag pro Monat",Modifier.weight(1f),fontSize=13.sp,color=appInkSoft(dark));Box(Modifier.size(34.dp).clip(CircleShape).background(if(dark)Color(0xFF52545C) else Color.White).clickable{showInfo=true},contentAlignment=Alignment.Center){Text("i",fontSize=17.sp,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}}
    var focused by remember{mutableStateOf(false)}
    BasicTextField(extraText,{extraText=centInput(it)},singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(textAlign=TextAlign.Center,fontSize=30.sp,fontWeight=FontWeight.ExtraBold,color=appInk(dark)),cursorBrush=androidx.compose.ui.graphics.SolidColor(appBlue(dark)),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),keyboardActions=androidx.compose.foundation.text.KeyboardActions(onDone={saveExtra()}),modifier=Modifier.fillMaxWidth().padding(bottom=6.dp).onFocusChanged{if(focused&&!it.isFocused)saveExtra();focused=it.isFocused})
    HorizontalDivider(thickness=2.dp,color=heroBlueText(dark));Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(50.0,100.0,150.0,200.0).forEach{v->val field=Modifier.weight(1f).clip(RoundedCornerShape(999.dp));Box((if(extra==v)field.htmlGradient(listOf(heroBlueStart(dark),heroBlueEnd(dark))) else field.background(appPaper(dark))).clickable{extra=v;extraText=String.format(Locale.GERMANY,"%,.2f",v);repo.setSetting("household-tilgung-extra",v.toString())}.padding(vertical=9.dp),contentAlignment=Alignment.Center){Text(v.toInt().toString()+" €",fontSize=13.sp,color=if(extra==v)heroBlueText(dark) else appInkSoft(dark),fontWeight=if(extra==v)FontWeight.Bold else FontWeight.Normal)}}}
   }
   if(payoff.isNotEmpty())Text("Wann welcher Kredit fertig ist",Modifier.padding(start=4.dp,end=4.dp,top=20.dp,bottom=12.dp),fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.2).sp)
  }
  if(payoff.isNotEmpty())item{
   ReferenceSummary{payoff.forEach{(r,m)->Column(Modifier.fillMaxWidth().padding(vertical=10.dp)){
    Text(r.name,Modifier.padding(bottom=3.dp),fontSize=15.sp,fontWeight=FontWeight.Bold);Text("in "+payoffDuration(m.month)+" · "+m.label,fontSize=13.5.sp,color=appInkSoft(dark));val loan=loans.firstOrNull{it.optString("id")==r.id};if(loan!=null&&loan.optDouble("monthlyPayment")>0){val pct=loan.optInt("carryoverPct",100).coerceIn(0,100);Text("+"+money(cents(loan.optDouble("monthlyPayment")*pct/100.0))+" zum Extra-Betrag",Modifier.padding(top=2.dp),fontSize=13.sp,color=appGreen(dark));Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(0,25,50,75,100).forEach{v->val shape=RoundedCornerShape(999.dp);val field=Modifier.weight(1f).clip(shape);Box((if(v==pct)field.htmlGradient(listOf(heroBlueStart(dark),heroBlueEnd(dark))) else field.background(appPaper(dark)).border(1.5.dp,appRule(dark),shape)).clickable{loan.put("carryoverPct",v);repo.save(state)}.padding(vertical=6.dp),contentAlignment=Alignment.Center){Text("$v%",fontSize=11.5.sp,color=if(v==pct)heroBlueText(dark) else appInk(dark),fontWeight=if(v==pct)FontWeight.Bold else FontWeight.Normal)}}}}
   };HorizontalDivider(color=appRule(dark))}}
  }
  items(plan,key={it.month}){m->val current=m.month==1;Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).then(if(current)Modifier.border(1.5.dp,appBlue(dark),RoundedCornerShape(14.dp)) else Modifier).padding(horizontal=20.dp,vertical=18.dp)){
   Row(Modifier.fillMaxWidth().padding(bottom=10.dp),verticalAlignment=Alignment.Bottom){Text(m.label+(if(current)" · aktuell" else ""),Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text(money(m.total),fontSize=14.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark))}
   m.rows.filter{it.extra>.005}.forEach{r->Row(Modifier.fillMaxWidth().padding(top=7.dp,bottom=2.dp),verticalAlignment=Alignment.CenterVertically){Text(r.name,Modifier.weight(1f),fontSize=16.sp,fontWeight=FontWeight.SemiBold);Text("+"+money(r.extra),fontSize=14.sp,fontWeight=FontWeight.Bold,color=appGreen(dark))};Text("Restschuld danach: "+money(r.remaining),Modifier.padding(start=2.dp,bottom=6.dp),fontSize=12.sp,color=appInkSoft(dark))}
   if(current){HorizontalDivider(Modifier.padding(top=8.dp),color=appRule(dark));Row(Modifier.fillMaxWidth().clickable{confirmMonth=true}.padding(horizontal=2.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text("Diesen Monat erledigt",Modifier.weight(1f),fontSize=15.sp);Box(Modifier.size(22.dp).border(1.dp,appRuleStrong(dark),RoundedCornerShape(3.dp)))}}
  }}
  item{TilgungHistory(repo.array(state,"loans").objects(),payments,repo)}
 }
 if(showInfo)TilgungInfo{showInfo=false}
 if(confirmMonth)TilgungMonthConfirm({confirmMonth=false}){advanceTilgungCurrentMonth(repo);monthRevision++;confirmMonth=false}
}

@Composable private fun TilgungInfo(onClose:()->Unit){ReferenceMessage("Methode: Schneeball (kleinste Restschuld zuerst). Werden Kredite freigewordene Mindestraten frei, fließen sie automatisch als zusätzliche Tilgung in den nächsten Kredit.",icon="ℹ️",onDismiss=onClose)}
@Composable private fun TilgungMonthConfirm(onCancel:()->Unit,onConfirm:()->Unit){ReferenceConfirm("Diesen Monat wirklich als erledigt markieren? Der Plan geht dann zum nächsten Monat weiter.",icon="✅",confirmLabel="Ja, erledigt",onCancel=onCancel,onConfirm=onConfirm)}

private data class TilgungHist(val key:String,val label:String,val entries:List<Pair<String,Double>>,val total:Double)
private fun tilgungHistory(loans:List<JSONObject>,payments:List<JSONObject>):List<TilgungHist>{val by=payments.filter{it.optString("type")=="payment"&&it.optString("date").length>=7}.groupBy{it.optString("date").take(7)};return by.keys.sortedDescending().mapNotNull{k->val es=by[k].orEmpty().groupBy{it.optString("loanId")}.mapNotNull{(id,ps)->val l=loans.firstOrNull{it.optString("id")==id}?:return@mapNotNull null;val e=cents(kotlin.math.max(0.0,ps.sumOf{it.optDouble("amount")}-l.optDouble("monthlyPayment")));if(e>.005)l.optString("name") to e else null};if(es.isEmpty())null else{val d=runCatching{LocalDate.parse(k+"-01")}.getOrNull();val label=if(d!=null)d.month.getDisplayName(java.time.format.TextStyle.SHORT,Locale.GERMAN)+" "+d.year else k;TilgungHist(k,label,es,cents(es.sumOf{it.second}))}}}
@Composable private fun TilgungHistory(loans:List<JSONObject>,payments:List<JSONObject>,repo:HouseholdRepository){val InkSoft=appInkSoft(LocalAppDark.current);val Green=appGreen(LocalAppDark.current);val Blue=appBlue(LocalAppDark.current);val h=tilgungHistory(loans,payments);var open by remember{mutableStateOf(repo.getSetting("household-show-tilgung-history")=="1")};val count=h.sumOf{it.entries.size};Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).clickable{open=!open;repo.setSetting("household-show-tilgung-history",if(open)"1" else "0")}.padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Blue));Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Verlauf",fontSize=15.sp,fontWeight=FontWeight.Bold);Text("$count "+if(count==1)"Extra-Zahlung" else "Extra-Zahlungen",fontSize=13.sp,color=InkSoft)};Text(if(open)"⌃" else "⌄",color=InkSoft)};if(open){if(h.isEmpty())OriginalEmpty("Noch keine Extra-Zahlungen gebucht.") else Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)){h.forEach{m->Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current)).padding(horizontal=16.dp,vertical=9.dp)){Text(m.label.uppercase(Locale.GERMAN),Modifier.weight(1f),fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.345.sp,color=InkSoft);Text(money(m.total),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=InkSoft)};m.entries.forEach{e->Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp)){Text(e.first,Modifier.weight(1f),fontSize=14.sp);Text("+"+money(e.second),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=Green)}}}}}}
private fun tilgungCurrentMonth(repo:HouseholdRepository):LocalDate{val real=YearMonth.now();val stored=repo.getSetting("household-tilgung-current-month")?.let{runCatching{YearMonth.parse(it)}.getOrNull()};val current=if(stored==null||stored<real)real else stored;if(stored!=current)repo.setSetting("household-tilgung-current-month",current.toString());return current.atDay(1)}
private fun advanceTilgungCurrentMonth(repo:HouseholdRepository){val next=YearMonth.from(tilgungCurrentMonth(repo)).plusMonths(1);repo.setSetting("household-tilgung-current-month",next.toString())}
internal data class PayRow(val id:String,val name:String,val payment:Double,val extra:Double,val remaining:Double)
internal data class PayMonth(val month:Int,val label:String,val total:Double,val remaining:Double,val rows:List<PayRow>)
private fun cents(v:Double)=referenceCents(v)
internal fun makePayoffPlan(loans:List<JSONObject>,extra:Double,maxMonths:Int=600,baseMonth:LocalDate=LocalDate.now().withDayOfMonth(1)):List<PayMonth>{
 data class D(val id:String,val name:String,var balance:Double,val payment:Double,val rate:Double?,val carry:Int,var counted:Boolean=false)
 val ds=loans.filter{it.optDouble("remainingBalance")>0}.map{D(it.optString("id"),it.optString("name"),it.optDouble("remainingBalance"),it.optDouble("monthlyPayment"),if(it.isNull("monthlyRate"))null else it.optDouble("monthlyRate"),it.optInt("carryoverPct",100).coerceIn(0,100))}.toMutableList()
 if(ds.isEmpty())return emptyList();var currentBudget=ds.sumOf{it.payment}+extra;if(currentBudget<=0)return emptyList();val out=mutableListOf<PayMonth>();var m=0
 fun mark(d:D){if(d.balance<=.005&&!d.counted){d.counted=true;currentBudget=cents(currentBudget-cents(d.payment*(100-d.carry)/100.0))}}
 while(ds.any{it.balance>.005}&&m<maxMonths){m++;val start=currentBudget;var budget=start;val rows=mutableListOf<PayRow>()
  ds.forEach{d->if(d.balance>.005){val interest=if(d.rate!=null)cents(d.balance*d.rate) else 0.0;val maxPay=kotlin.math.min(d.payment,budget);val principal=kotlin.math.max(0.0,kotlin.math.min(maxPay-interest,d.balance));var pay=cents(principal+interest);pay=kotlin.math.min(pay,budget);d.balance=cents(d.balance-principal);budget=cents(budget-pay);rows+=PayRow(d.id,d.name,pay,0.0,d.balance);mark(d)}}
  while(budget>.005&&ds.any{it.balance>.005}){val d=ds.filter{it.balance>.005}.minBy{it.balance};val pay=kotlin.math.min(budget,d.balance);d.balance=cents(d.balance-pay);budget=cents(budget-pay);val i=rows.indexOfFirst{it.id==d.id};if(i>=0){val x=rows[i];rows[i]=x.copy(extra=x.extra+pay,remaining=d.balance)}else rows+=PayRow(d.id,d.name,0.0,pay,d.balance);mark(d)}
  val date=baseMonth.withDayOfMonth(1).plusMonths((m-1).toLong());val label=date.month.getDisplayName(java.time.format.TextStyle.FULL,Locale.GERMAN).take(3)+" "+date.year
  out+=PayMonth(m,label,cents(start-budget),cents(ds.sumOf{it.balance}),rows)
 };return out
}
private fun payoffDuration(m:Int):String{val y=m/12;val r=m%12;return (if(y>0)"$y "+if(y==1)"Jahr" else "Jahre" else "")+(if(y>0&&r>0)", " else "")+(if(r>0)"$r "+if(r==1)"Monat" else "Monate" else if(y==0)"diesen Monat" else "")}
@Composable private fun LoanHero(remaining:Double,original:Double,monthly:Double){
 val dark=LocalAppDark.current;var collapsed by remember{mutableStateOf(false)};val pct=if(original>0)(original-remaining)/original else 0.0
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).clickable{collapsed=!collapsed}.htmlGradient(listOf(heroBlueStart(dark),heroBlueEnd(dark))).padding(start=20.dp,end=20.dp,top=if(collapsed)10.dp else 22.dp)){
  if(!collapsed){Text("Restschuld gesamt",fontSize=13.sp,color=heroBlueText(dark));Spacer(Modifier.height(6.dp))}
  Text(money(remaining),Modifier.htmlHeroScale(collapsed),fontSize=34.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.68).sp)
  if(!collapsed)Spacer(Modifier.height(14.dp))
  if(original>0){Box(Modifier.fillMaxWidth().padding(vertical=10.dp).height(10.dp).clip(RoundedCornerShape(20.dp)).background(appPaper(dark))){Box(Modifier.fillMaxWidth(pct.toFloat().coerceIn(0f,1f)).fillMaxHeight().background(appBlue(dark)))};Row(Modifier.fillMaxWidth()){Text(referenceRound(pct*100).toInt().toString()+"% abbezahlt",Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));Text("von "+money(original),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark))}}
  if(!collapsed)Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Column{Text("Kreditsumme",fontSize=11.sp,color=heroBlueSub(dark));Text(money(original),Modifier.padding(top=2.dp),fontSize=14.sp,fontWeight=FontWeight.Bold)};Column{Text("Raten/Monat",fontSize=11.sp,color=heroBlueSub(dark));Text(money(monthly),Modifier.padding(top=2.dp),fontSize=14.sp,fontWeight=FontWeight.Bold)}}
  Box(Modifier.fillMaxWidth().padding(top=14.dp,bottom=10.dp).rotate(if(collapsed)180f else 0f),contentAlignment=Alignment.Center){HtmlReferenceIcon("chevron-up",heroBlueText(dark).copy(alpha=.85f),16.dp,2.4f)}
 }
}

private fun formatRemainingTermExact(loan:JSONObject,payments:List<JSONObject>):String{if(loan.optDouble("remainingBalance")<=0)return "Abbezahlt";val ps=payments.filter{it.optString("loanId")==loan.optString("id")&&it.optString("type")=="payment"};if(loan.has("totalTermMonths")&&!loan.isNull("totalTermMonths")&&loan.optInt("totalTermMonths")>0){val rem=(loan.optInt("totalTermMonths")-ps.size).coerceAtLeast(0);if(rem==0)return "Letzte Rate fällig";return termLabel(rem)};val monthly=loan.optDouble("monthlyPayment");if(monthly<=0)return "–";val extra=ps.sumOf{kotlin.math.max(0.0,it.optDouble("amount")-monthly)};val unconfirmed=extra%monthly;val display=loan.optDouble("remainingBalance")+unconfirmed;val months=kotlin.math.ceil(display/monthly-0.0001).toInt().coerceAtLeast(0);return termLabel(months)}
private fun termLabel(months:Int):String{val y=months/12;val m=months%12;if(y==0)return m.toString()+if(m==1)" Monat" else " Monate";val ys=y.toString()+if(y==1)" Jahr" else " Jahre";return if(m==0)ys else ys+", "+m+if(m==1)" Monat" else " Monate"}
private fun freedRateOffer(loan:JSONObject,repo:HouseholdRepository):Triple<Double,Double,Double>?{val monthly=loan.optDouble("monthlyPayment");if(monthly<=0)return null;val pct=if(loan.has("carryoverPct")&&!loan.isNull("carryoverPct"))loan.optInt("carryoverPct").coerceIn(0,100) else 100;val freed=cents(monthly*pct/100.0);if(freed<=0)return null;val current=repo.getSetting("household-tilgung-extra")?.toDoubleOrNull()?.takeIf{it>=0}?:0.0;return Triple(current,freed,cents(current+freed))}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun LoanCardOriginal(o:JSONObject,paidThisMonth:Boolean,payments:List<JSONObject>,state:JSONObject,repo:HouseholdRepository){val InkSoft=appInkSoft(LocalAppDark.current);val Red=appRed(LocalAppDark.current);val Blue=appBlue(LocalAppDark.current);
 var action by remember{o.let{mutableStateOf("")}};var expanded by remember(o.optString("id")){mutableStateOf(false)};var editLoanName by remember{o.let{mutableStateOf(o.optString("name"))}};var editOriginal by remember{o.let{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("originalAmount")))}};var editMonthly by remember{o.let{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("monthlyPayment")))}};var editTotal by remember{o.let{mutableStateOf(if(o.has("totalWithInterest")&&!o.isNull("totalWithInterest"))String.format(Locale.GERMANY,"%,.2f",o.optDouble("totalWithInterest")) else "")}};var editTerm by remember{o.let{mutableStateOf(if(o.has("totalTermMonths")&&!o.isNull("totalTermMonths"))o.optInt("totalTermMonths").toString() else "")}};var reconcileBalance by remember{o.let{mutableStateOf("")}};var actionDate by remember{mutableStateOf(LocalDate.now().toString())};var actionAmount by remember{o.let{mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("monthlyPayment")))}};val original=o.optDouble("originalAmount");val remaining=o.optDouble("remainingBalance");val pct=if(original>0)((original-remaining)/original).coerceIn(0.0,1.0) else 0.0
 if(action=="edit"){LoanEditForm(editLoanName,{editLoanName=it},editOriginal,{editOriginal=it},editTotal,{editTotal=it},editMonthly,{editMonthly=it},editTerm,{editTerm=it},{action=""}){val newOriginal=parseNativeAmount(editOriginal)?:0.0;val mp=parseNativeAmount(editMonthly);if(editLoanName.isNotBlank()&&mp!=null){val had=o.optDouble("totalWithInterest")!=0.0;val total=parseNativeAmount(editTotal);val delta=newOriginal-o.optDouble("originalAmount");o.put("remainingBalance",kotlin.math.max(0.0,referenceRound((o.optDouble("remainingBalance")+delta)*100)/100));o.put("name",editLoanName.trim());o.put("originalAmount",newOriginal);o.put("monthlyPayment",mp);editTerm.toIntOrNull()?.takeIf{it>0}?.let{o.put("totalTermMonths",it)}?:o.remove("totalTermMonths");if(total!=null)o.put("totalWithInterest",total) else o.remove("totalWithInterest");val rate=if(total!=null)solveMonthlyRate(newOriginal,mp,total) else null;if(rate!=null)o.put("monthlyRate",rate) else o.remove("monthlyRate");if(had&&(total==null||total==0.0)){val hist=payments.filter{it.optString("loanId")==o.optString("id")};val ps=hist.filter{it.optString("type")=="payment"}.sumOf{it.optDouble("amount")};val inc=hist.filter{it.optString("type")=="increase"}.sumOf{it.optDouble("amount")};o.put("remainingBalance",kotlin.math.max(0.0,referenceRound((newOriginal-ps+inc)*100)/100))};repo.save(state);action=""}};return}
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(14.dp)).clickable{expanded=!expanded}.background(if(paidThisMonth)appSavingsSoft(LocalAppDark.current) else appExpenseSoft(LocalAppDark.current)).padding(horizontal=20.dp,vertical=18.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom){Text(o.optString("name","Kredit"),Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text(money(remaining),fontSize=18.sp,fontWeight=FontWeight.ExtraBold,color=Red)}
  if(original>0){Spacer(Modifier.height(10.dp));Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(20.dp)).background(appPaper(LocalAppDark.current))){Box(Modifier.fillMaxWidth(pct.toFloat()).fillMaxHeight().background(Blue))};Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth()){Text(referenceRound(pct*100).toInt().toString()+"% abbezahlt",Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft);Text("von "+money(original),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft)}}
  Spacer(Modifier.height(6.dp));LoanDetail("Rate",money(o.optDouble("monthlyPayment"))+" / Monat");Spacer(Modifier.height(2.dp));LoanDetail("Restlaufzeit",formatRemainingTermExact(o,payments));if(expanded)FlowRow(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(4.dp)){
 ReferenceAction("Bearbeiten","action-edit",false){editLoanName=o.optString("name");editOriginal=String.format(Locale.GERMANY,"%,.2f",o.optDouble("originalAmount"));editMonthly=String.format(Locale.GERMANY,"%,.2f",o.optDouble("monthlyPayment"));editTotal=if(o.optDouble("totalWithInterest")!=0.0)String.format(Locale.GERMANY,"%,.2f",o.optDouble("totalWithInterest")) else "";editTerm=if(o.optInt("totalTermMonths")>0)o.optInt("totalTermMonths").toString() else "";action="edit"}
 ReferenceAction("Erhöhen","action-plus",false,buttonColor=if(LocalAppDark.current)Color(0xFFFFB03F) else Color(0xFFFF9F0A)){actionDate=LocalDate.now().toString();actionAmount="";action="increase"}
 ReferenceAction("Rate buchen","action-check",false,buttonColor=appBlue(LocalAppDark.current)){actionDate=LocalDate.now().toString();actionAmount=o.optDouble("monthlyPayment").toString();action="pay"}
 if(o.optDouble("monthlyRate")>0)ReferenceAction("Abgleichen","action-refresh",false){reconcileBalance="";action="reconcile"}
 ReferenceAction("Löschen","action-delete",true){action="delete"}
};if(action=="pay"||action=="increase")LoanActionForm(actionDate,{actionDate=it},actionAmount,{actionAmount=it},{action=""},kind=action){val a=parseNativeAmount(actionAmount);if(actionDate.isNotBlank()&&a!=null&&a>0){val type=if(action=="increase")"increase" else "payment";val before=o.optDouble("remainingBalance");val payment=JSONObject().put("id",genId()).put("loanId",o.optString("id")).put("date",actionDate).put("amount",a).put("type",type);if(type=="increase"){o.put("remainingBalance",referenceRound((before+a)*100)/100);o.put("originalAmount",referenceRound((o.optDouble("originalAmount")+a)*100)/100)}else{var principalReduction=a;val monthlyRate=o.optDouble("monthlyRate");val monthlyPayment=o.optDouble("monthlyPayment");if(monthlyRate>0&&a>=monthlyPayment-0.005){val interestPortion=referenceRound(before*monthlyRate*100)/100;val regularAmount=kotlin.math.min(a,monthlyPayment);val regularPrincipal=kotlin.math.max(0.0,referenceRound((regularAmount-interestPortion)*100)/100);val extraAmount=kotlin.math.max(0.0,referenceRound((a-monthlyPayment)*100)/100);principalReduction=referenceRound((regularPrincipal+extraAmount)*100)/100};payment.put("principalReduction",principalReduction);o.put("remainingBalance",kotlin.math.max(0.0,referenceRound((before-principalReduction)*100)/100));if(o.optDouble("remainingBalance")<=0&&(o.isNull("paidOffDate")||o.optString("paidOffDate").isBlank()))o.put("paidOffDate",LocalDate.now().toString());if(before>0&&o.optDouble("remainingBalance")<=0)repo.offerFreedRate(o)};repo.array(state,"loanPayments").put(payment);repo.save(state);action=""}};if(action=="reconcile")ReferenceEditGrid{
 Text("Aktuelle Restschuld laut Bank/Kontoauszug eintragen - die App berechnet daraus automatisch einen genaueren Zinssatz für künftige Buchungen.",Modifier.padding(bottom=2.dp),fontSize=12.5.sp,color=InkSoft)
 ReferenceEditInput(reconcileBalance,{reconcileBalance=centInput(it)},decimal=true,placeholder="Restschuld laut Bank (€)")
 ReferenceEditActions({action=""},saveLabel="Abgleichen"){val entered=parseNativeAmount(reconcileBalance);if(entered!=null&&entered>=0){val checkpoint=if(o.has("lastReconciledBalance")&&!o.isNull("lastReconciledBalance"))o.optDouble("lastReconciledBalance") else o.optDouble("originalAmount");val checkpointDate=o.optString("lastReconciledDate","0000-00-00").ifBlank{"0000-00-00"};val ps=payments.filter{it.optString("loanId")==o.optString("id")&&it.optString("type")=="payment"&&it.optString("date")>checkpointDate};val sum=ps.sumOf{it.optDouble("amount")};val reduction=referenceRound((checkpoint-entered)*100)/100;if(ps.isNotEmpty()&&reduction>0&&reduction<sum){val interest=referenceRound((sum-reduction)*100)/100;val avg=(checkpoint+entered)/2;val rate=if(avg>0)(interest/ps.size)/avg else 0.0;if(rate>0&&rate<0.05)o.put("monthlyRate",rate)};o.put("remainingBalance",entered);o.put("lastReconciledBalance",entered);o.put("lastReconciledDate",LocalDate.now().toString());if(entered<=0&&(o.isNull("paidOffDate")||o.optString("paidOffDate").isBlank()))o.put("paidOffDate",LocalDate.now().toString());repo.save(state);action=""}}
};if(action=="delete")ReferenceConfirm("Diesen Kredit wirklich löschen? Die zugehörige Ratenhistorie wird ebenfalls entfernt.",onCancel={action=""},onConfirm={val id=o.optString("id");val la=repo.array(state,"loans");for(i in la.length()-1 downTo 0)if(la.optJSONObject(i)?.optString("id")==id)la.remove(i);val pa=repo.array(state,"loanPayments");for(i in pa.length()-1 downTo 0)if(pa.optJSONObject(i)?.optString("loanId")==id)pa.remove(i);repo.save(state);action=""});;if(expanded)LoanHistory(o,payments,state,repo)
 }
}
@Composable private fun LoanEditForm(name:String,onName:(String)->Unit,original:String,onOriginal:(String)->Unit,total:String,onTotal:(String)->Unit,monthly:String,onMonthly:(String)->Unit,term:String,onTerm:(String)->Unit,onCancel:()->Unit,onSave:()->Unit){
 ReferenceEditGrid{
  ReferenceEditInput(name,onName,placeholder="Bezeichnung")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){ReferenceEditLabeled("Kreditsumme",original,{onOriginal(centInput(it))},"Kreditsumme (€)")};Column(Modifier.weight(1f)){ReferenceEditLabeled("Gesamtbetrag inkl. Zinsen (optional)",total,{onTotal(centInput(it))},"z. B. 12.480,00")}}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){ReferenceEditLabeled("Rate pro Monat",monthly,{onMonthly(centInput(it))},"Rate/Monat (€)")};Column(Modifier.weight(1f)){ReferenceEditLabeled("Laufzeit in Monaten (optional)",term,{onTerm(it.filter(Char::isDigit))},"z. B. 36",decimal=false)}}
  ReferenceEditActions(onCancel,onSave=onSave)
 }
}
@Composable private fun LoanActionForm(date:String,onDate:(String)->Unit,amount:String,onAmount:(String)->Unit,onCancel:()->Unit,kind:String="pay",onSave:()->Unit){
 val dark=LocalAppDark.current;val increasing=kind=="increase"
 ReferenceEditGrid(gold=increasing){
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){ReferenceDateInput(date,edit=true,onValue=onDate)};Box(Modifier.weight(1f)){ReferenceEditInput(amount,onAmount,decimal=true,placeholder="Betrag (€)")}}
  ReferenceEditActions(onCancel,if(increasing)"Erhöhen" else "Buchen",if(increasing)if(dark)Color(0xFFFFB03F) else Color(0xFFFF9F0A) else null,onSave)
 }
}
@Composable private fun ReferenceFreedRateDialog(loan:JSONObject,repo:HouseholdRepository){
 val offer=freedRateOffer(loan,repo)?:return;val pct=if(loan.has("carryoverPct")&&!loan.isNull("carryoverPct"))loan.optInt("carryoverPct").coerceIn(0,100) else 100
 val note=if(pct<100)" ("+pct+"% von "+money(loan.optDouble("monthlyPayment"))+")" else ""
 ReferenceConfirm("„"+loan.optString("name")+"\" ist jetzt abbezahlt! Soll die freigewordene Monatsrate"+note+" zum zusätzlichen Tilgungsbetrag dazugerechnet werden? Neuer Betrag: "+money(offer.first)+" + "+money(offer.second)+" = "+money(offer.third),icon="🎉",confirmLabel="Hinzufügen",onCancel={repo.pendingFreedLoan=null},onConfirm={repo.setSetting("household-tilgung-extra",offer.third.toString());repo.pendingFreedLoan=null})
}
@Composable private fun LoanHistory(loan:JSONObject,payments:List<JSONObject>,state:JSONObject,repo:HouseholdRepository){
 val dark=LocalAppDark.current;val loanId=loan.optString("id");val ps=payments.filter{it.optString("loanId")==loanId}.sortedByDescending{it.optString("date")};var confirmPayoff by remember(loanId){mutableStateOf(false)};var deleting by remember(loanId){mutableStateOf<JSONObject?>(null)}
 Column(Modifier.fillMaxWidth().padding(top=20.dp)){
  HorizontalDivider(color=appRule(dark));Spacer(Modifier.height(6.dp))
  if(ps.isEmpty())Text("Noch keine Ratenzahlungen gebucht.",Modifier.fillMaxWidth().padding(vertical=14.dp,horizontal=4.dp),textAlign=TextAlign.Center,fontSize=14.sp,color=appInkSoft(dark)) else ps.forEach{p->
   val inc=p.optString("type")=="increase";val d=runCatching{LocalDate.parse(p.optString("date")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault(p.optString("date"))
   Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
    Text(if(inc)"Erhöhung" else "Rate",fontSize=16.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(d,fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));Text((if(inc)"−" else "+")+money(p.optDouble("amount")),fontSize=16.sp,fontWeight=FontWeight.Bold,color=if(inc)appRed(dark) else appGreen(dark))
    Box(Modifier.size(34.dp).clip(CircleShape).background(if(dark)Color(0xFF52545C) else Color.White).clickable{deleting=p},contentAlignment=Alignment.Center){Text("×",fontSize=17.sp,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}
   }
   if(p!==ps.last())HorizontalDivider(color=appRule(dark))
  }
  Surface(Modifier.fillMaxWidth().padding(top=12.dp).clickable{confirmPayoff=true},shape=RoundedCornerShape(999.dp),color=appGreen(dark)){Text("Vorzeitig abbezahlt",Modifier.padding(horizontal=18.dp,vertical=14.dp),textAlign=TextAlign.Center,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White)}
 }
 deleting?.let{p->ReferenceConfirm("Diesen Eintrag wirklich löschen? Kreditsumme und Restschuld werden entsprechend zurückgerechnet.",onCancel={deleting=null},onConfirm={removeReferenceLoanPayment(state,loan,p);deleting=null;repo.save(state)})}
 if(confirmPayoff)ReferenceConfirm("Diesen Kredit wirklich als vorzeitig abbezahlt markieren? Die Restschuld wird auf 0 gesetzt.",onCancel={confirmPayoff=false},onConfirm={payoffReferenceLoan(state,loan,genId(),LocalDate.now().toString());confirmPayoff=false;repo.offerFreedRate(loan);repo.save(state)})
}

@Composable private fun LoanDetail(label:String,value:String){val InkSoft=appInkSoft(LocalAppDark.current);Row(Modifier.fillMaxWidth()){Text(label,Modifier.weight(1f),fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft);Text(value,fontSize=12.5.sp,fontWeight=FontWeight.SemiBold,color=InkSoft)}}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun FuelScreen(state:JSONObject,repo:HouseholdRepository,selectedCar:String,onSelectedCar:(String)->Unit){
 val dark=LocalAppDark.current;val cars=remember(state,repo.revision){repo.array(state,"cars").objects()};val logs=remember(state,repo.revision){repo.array(state,"fuelLogs").objects()};val selected=if(selectedCar=="__all__"||cars.any{it.optString("id")==selectedCar})selectedCar else if(cars.isEmpty())"" else "__all__";var deleteCarId by remember{mutableStateOf<String?>(null)};var range by remember{mutableStateOf(repo.getSetting("household-fuel-chart-range")?.takeIf{it in setOf("week","month","3months","6months","year")}?:"month")}
 LaunchedEffect(selected){if(selectedCar!=selected)onSelectedCar(selected)}
 val shown=remember(selected,logs){if(selected=="__all__")logs else logs.filter{it.optString("carId")==selected}};val monthKey=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));val current=shown.filter{it.optString("date").startsWith(monthKey)};val total=current.sumOf{it.optDouble("liters")*it.optDouble("pricePerLiter")};val liters=current.sumOf{it.optDouble("liters")};val avg=if(liters>0)total/liters else 0.0;val groups=shown.sortedByDescending{it.optString("date")}.groupBy{it.optString("date").take(7)}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=18.dp,vertical=0.dp)){
  item{
   Text("Tanken",Modifier.padding(start=4.dp,end=4.dp,bottom=12.dp),fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.2).sp)
   FuelHero(if(cars.isEmpty())"Tanken diesen Monat" else "Tanken diesen Monat – "+if(selected=="__all__")"Alle Autos" else cars.firstOrNull{it.optString("id")==selected}?.optString("name").orEmpty(),if(cars.isEmpty())0.0 else total,if(cars.isEmpty())0.0 else liters,avg,hasCar=cars.isNotEmpty())
   FuelPriceChart(logs,range){range=it;repo.setSetting("household-fuel-chart-range",it)}
   FlowRow(Modifier.fillMaxWidth().padding(bottom=16.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    if(cars.isNotEmpty())FuelChip("Gesamt",selected=="__all__",true,{onSelectedCar("__all__")})
    cars.forEach{o->FuelChip(o.optString("name"),selected==o.optString("id"),false,{onSelectedCar(o.optString("id"))},if(selected==o.optString("id")){{deleteCarId=o.optString("id")}} else null)}
   }
   if(cars.isEmpty())OriginalEmpty("Noch kein Auto hinzugefügt. Tippe unten auf das Plus, um eins hinzuzufügen.") else ReferenceSettingsLabel("Verlauf")
   if(cars.isNotEmpty()&&shown.isEmpty())OriginalEmpty("Noch keine Tankvorgänge"+if(selected=="__all__")"." else " für dieses Auto.")
  }
  if(cars.isNotEmpty())groups.forEach{(key,entries)->item{FuelGroupHeader(key,entries.sumOf{it.optDouble("liters")*it.optDouble("pricePerLiter")})};items(entries,key={it.optString("id")}){o->FuelRow(o,cars,selected=="__all__",state,repo)}}
 }
 deleteCarId?.let{id->val car=cars.firstOrNull{it.optString("id")==id};ReferenceConfirm("\""+(car?.optString("name")?:"dieses Auto")+"\" wirklich löschen? Alle zugehörigen Tankvorgänge werden mitgelöscht.",onCancel={deleteCarId=null},onConfirm={val ca=repo.array(state,"cars");for(i in ca.length()-1 downTo 0)if(ca.optJSONObject(i)?.optString("id")==id)ca.remove(i);val la=repo.array(state,"fuelLogs");for(i in la.length()-1 downTo 0)if(la.optJSONObject(i)?.optString("carId")==id)la.remove(i);deleteCarId=null;onSelectedCar(if(ca.length()>0)"__all__" else "");repo.save(state)})}
}
@Composable private fun FuelHero(label:String,total:Double,liters:Double,avg:Double,hasCar:Boolean=true){
 val dark=LocalAppDark.current;var collapsed by remember{mutableStateOf(false)}
 Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).clickable{collapsed=!collapsed}.htmlGradient(listOf(heroExhaustStart(dark),heroExhaustEnd(dark))).padding(start=20.dp,end=20.dp,top=if(collapsed)10.dp else 22.dp,bottom=if(hasCar)0.dp else 22.dp)){
  if(!collapsed){Text(label,fontSize=13.sp,color=heroExhaustText(dark));Spacer(Modifier.height(6.dp))};Text(money(total),fontSize=34.sp,modifier=Modifier.htmlHeroScale(collapsed),fontWeight=FontWeight.ExtraBold,letterSpacing=(-.68).sp)
  if(!collapsed&&hasCar){Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Column{Text("Liter",fontSize=11.sp,color=heroExhaustSub(dark));Text(String.format(Locale.GERMANY,"%.2f L",liters),Modifier.padding(top=2.dp),fontSize=14.sp,fontWeight=FontWeight.Bold)};Column{Text("Ø Preis/Liter",fontSize=11.sp,color=heroExhaustSub(dark));Text(if(avg>0)String.format(Locale.GERMANY,"%.3f €",avg) else "– €",Modifier.padding(top=2.dp),fontSize=14.sp,fontWeight=FontWeight.Bold)}}}
  if(hasCar)Box(Modifier.fillMaxWidth().padding(top=14.dp,bottom=10.dp).rotate(if(collapsed)180f else 0f),contentAlignment=Alignment.Center){HtmlReferenceIcon("chevron-up",heroExhaustText(dark).copy(alpha=.85f),16.dp,2.4f)}
 }
}

@Composable private fun FuelPriceChart(logs:List<JSONObject>,range:String,onRange:(String)->Unit){
 val dark=LocalAppDark.current;val pts=referenceFuelChartPoints(logs,range,java.time.LocalDateTime.now());val prices=pts.map{it.optDouble("pricePerLiter")};val coords=referenceFuelChartCoordinates(prices);val exhaust=appExhaust(dark);val soft=appExhaustSoft(dark)
 Column(Modifier.fillMaxWidth().padding(bottom=16.dp).clip(RoundedCornerShape(14.dp)).background(appGlass(dark)).padding(horizontal=18.dp,vertical=16.dp)){
  Text("Spritpreis-Entwicklung",Modifier.padding(bottom=2.dp),fontSize=11.sp,color=appInkSoft(dark))
  Row(Modifier.fillMaxWidth().padding(bottom=10.dp),verticalAlignment=Alignment.Bottom){if(pts.size>=2){val last=prices.last();val delta=last-prices[prices.lastIndex-1];Text(String.format(Locale.GERMANY,"%.3f €/L",last),fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.width(8.dp));Text((if(delta>=0)"+" else "")+String.format(Locale.GERMANY,"%.3f €",delta),fontSize=12.5.sp,fontWeight=FontWeight.Bold,color=if(delta>0)appRed(dark) else if(delta<0)appGreen(dark) else appInkSoft(dark))}}
  Canvas(Modifier.fillMaxWidth().height(100.dp)){
   if(coords.isNotEmpty())withTransform({scale(size.width/300f,size.height/100f,pivot=Offset.Zero)}){
    val line=androidx.compose.ui.graphics.Path().apply{moveTo(coords[0].first,coords[0].second);coords.drop(1).forEach{lineTo(it.first,it.second)}}
    val area=androidx.compose.ui.graphics.Path().apply{addPath(line);lineTo(300f,100f);lineTo(0f,100f);close()}
    drawPath(area,soft);drawPath(line,exhaust,style=Stroke(width=2.5f,cap=StrokeCap.Round,join=StrokeJoin.Round));coords.forEach{drawCircle(exhaust,3.2f,Offset(it.first,it.second))}
   }
  }
  Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.SpaceBetween){if(pts.size<2)Text("Noch zu wenige Tankvorgänge in diesem Zeitraum.",fontSize=10.5.sp,color=appInkSoft(dark)) else{Text(LocalDate.parse(pts.first().optString("date")).format(DateTimeFormatter.ofPattern("dd.MM.")),fontSize=10.5.sp,color=appInkSoft(dark));Text(LocalDate.parse(pts.last().optString("date")).format(DateTimeFormatter.ofPattern("dd.MM.")),fontSize=10.5.sp,color=appInkSoft(dark))}}
  Row(Modifier.fillMaxWidth().padding(top=14.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("week" to "Woche","month" to "Monat","3months" to "3 Monate","6months" to "6 Monate","year" to "Jahr").forEach{(key,label)->Surface(Modifier.clickable{onRange(key)},shape=RoundedCornerShape(999.dp),color=if(range==key)exhaust else appPaper(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f))){Text(label,Modifier.padding(horizontal=12.dp,vertical=6.dp),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=if(range==key)Color.White else appInkSoft(dark))}}}
 }
}
@Composable private fun FuelChip(label:String,active:Boolean,all:Boolean,onClick:()->Unit,onDelete:(()->Unit)?=null){
 val dark=LocalAppDark.current;val ink=if(active)Color.White else appInkSoft(dark)
 Surface(Modifier.clickable(onClick=onClick),shape=RoundedCornerShape(999.dp),color=if(active)appExhaust(dark) else appPaper(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f))){Row(Modifier.padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
  HtmlReferenceIcon(if(all)"fuel-all-cars" else "fuel-car",ink.copy(alpha=if(active)1f else .7f),14.dp,2.2f);Text(label,fontSize=13.sp,fontWeight=FontWeight.Bold,color=ink)
  if(active&&!all&&onDelete!=null)Box(Modifier.padding(start=2.dp).size(18.dp).clip(CircleShape).background(Color.White.copy(alpha=.25f)).clickable(onClick=onDelete),contentAlignment=Alignment.Center){HtmlReferenceIcon("action-close",Color.White,13.dp,2.4f)}
 }}
}

@Composable private fun FuelGroupHeader(key:String,total:Double){val InkSoft=appInkSoft(LocalAppDark.current);val label=runCatching{LocalDate.parse(key+"-01").format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.GERMAN))}.getOrDefault(key);Row(Modifier.fillMaxWidth().background(appPaper(LocalAppDark.current)).padding(horizontal=16.dp,vertical=9.dp)){Text(label.uppercase(Locale.GERMAN),Modifier.weight(1f),fontSize=11.5.sp,fontWeight=FontWeight.Bold,letterSpacing=.345.sp,color=InkSoft);Text(money(total),fontSize=11.5.sp,fontWeight=FontWeight.Bold,color=InkSoft)}}
@Composable private fun FuelRow(o:JSONObject,cars:List<JSONObject>,showCar:Boolean,state:JSONObject,repo:HouseholdRepository){
 val dark=LocalAppDark.current;var mode by remember(o.optString("id")){mutableStateOf("")};var expanded by remember(o.optString("id")){mutableStateOf(false)};var date by remember(o.optString("id")){mutableStateOf(o.optString("date"))};var liters by remember(o.optString("id")){mutableStateOf(String.format(Locale.GERMANY,"%,.2f",o.optDouble("liters")))};var price by remember(o.optString("id")){mutableStateOf(String.format(Locale.GERMANY,"%,.3f",o.optDouble("pricePerLiter")))}
 if(mode=="edit")ReferenceEditGrid{
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){ReferenceDateInput(date,edit=true){date=it}};Column(Modifier.weight(1f)){Text("Liter",Modifier.padding(start=4.dp,bottom=4.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));ReferenceEditInput(liters,{liters=centInput(it)},decimal=true)}}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){Text("€/Liter",Modifier.padding(start=4.dp,bottom=4.dp),fontSize=11.5.sp,fontWeight=FontWeight.SemiBold,color=appInkSoft(dark));ReferenceEditInput(price,{price=milliInput(it)},decimal=true)};Spacer(Modifier.weight(1f))}
  ReferenceEditActions({mode=""},saveColor=appExhaust(dark)){val l=parseNativeAmount(liters);val p=parseNativeAmount(price);if(l!=null&&p!=null&&runCatching{LocalDate.parse(date)}.isSuccess){o.put("liters",l).put("pricePerLiter",p).put("date",date)};mode="";repo.save(state)}
 }else Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable{expanded=!expanded}.padding(horizontal=16.dp,vertical=13.dp)){
  val d=runCatching{LocalDate.parse(o.optString("date")).format(DateTimeFormatter.ofPattern("dd.MM."))}.getOrDefault(o.optString("date"));val car=if(showCar)(cars.firstOrNull{it.optString("id")==o.optString("carId")}?.optString("name")?:"?")+" · " else ""
  Row(verticalAlignment=Alignment.CenterVertically){Text(car+d+" · "+String.format(Locale.GERMANY,"%.2f L · %.3f €/L",o.optDouble("liters"),o.optDouble("pricePerLiter")),Modifier.weight(1f),fontSize=14.sp);Text(money(o.optDouble("liters")*o.optDouble("pricePerLiter")),Modifier.padding(start=10.dp),fontSize=17.sp,fontWeight=FontWeight.ExtraBold)}
  if(expanded)Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End)){ReferenceAction("Bearbeiten","action-edit",false){date=o.optString("date");liters=String.format(Locale.GERMANY,"%,.2f",o.optDouble("liters"));price=String.format(Locale.GERMANY,"%,.3f",o.optDouble("pricePerLiter"));mode="edit"};ReferenceAction("Löschen","action-delete",true){mode="delete"}}
 }
 if(mode=="delete")ReferenceConfirm("Diesen Tankvorgang wirklich löschen?",onCancel={mode=""},onConfirm={val a=repo.array(state,"fuelLogs");for(i in a.length()-1 downTo 0)if(a.optJSONObject(i)?.optString("id")==o.optString("id"))a.remove(i);mode="";repo.save(state)})
}

@Composable private fun SectionTitle(t:String)=Text(t,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))
@Composable private fun EmptyCard(t:String)=Card(Modifier.fillMaxWidth()){Text(t,Modifier.padding(18.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun SummaryCard(title:String,value:String,accent:Color)=Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(18.dp)){Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontSize=28.sp,fontWeight=FontWeight.Bold,color=accent)}}
@Composable private fun SimpleMoneyRow(name:String,amount:Double,sub:String="")=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(name.ifBlank{"Eintrag"},fontWeight=FontWeight.SemiBold);if(sub.isNotBlank())Text(sub,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(money(amount),fontWeight=FontWeight.Bold)}}
@Composable private fun EntryCard(title:String,date:String,amount:Double,positive:Boolean)=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title.ifBlank{"Buchung"},fontWeight=FontWeight.SemiBold);Text(date,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text((if(positive)"+ " else "− ")+money(amount),fontWeight=FontWeight.Bold,color=if(positive)appGreen(LocalAppDark.current) else appRed(LocalAppDark.current))}}


private fun replaceBetaEntries(state:JSONObject,items:List<JSONObject>){val a=JSONArray();items.forEach{a.put(it)};state.put("betaEntries",a)}
private fun applyBetaEdit(state:JSONObject,repo:HouseholdRepository,entry:JSONObject,scope:String,type:String,title:String,date:String,amount:Double,category:String,recurring:Boolean,reconciled:Boolean,period:String,endDate:String?){val all=repo.array(state,"betaEntries").objects();val originalDate=entry.optString("date");entry.put("type",type).put("title",title).put("date",date).put("amount",amount).put("category",category).put("reconciled",reconciled);if(entry.optBoolean("isRecurringTemplate"))entry.put("recurrencePeriod",period).put("recurrenceEnd",endDate?:JSONObject.NULL) else if(entry.optString("recurringGroupId").isBlank()&&recurring)entry.put("recurring",true).put("isRecurringTemplate",true).put("recurrencePeriod",period).put("recurrenceEnd",endDate?:JSONObject.NULL);if(scope=="following"&&entry.optString("recurringGroupId").isNotBlank()){val gid=entry.optString("recurringGroupId");all.filter{it.optString("recurringGroupId")==gid&&it.optString("id")!=entry.optString("id")&&it.optString("date")>=originalDate}.forEach{it.put("type",type).put("title",title).put("amount",amount).put("category",category)};all.firstOrNull{it.optString("id")==gid}?.let{tpl->tpl.put("type",type).put("title",title).put("category",category);if(tpl.optString("date")>=originalDate)tpl.put("amount",amount);tpl.put("recurrencePeriod",period).put("recurrenceEnd",endDate?:JSONObject.NULL)}}}
private fun deleteBetaEntry(state:JSONObject,repo:HouseholdRepository,entry:JSONObject,scope:String){val all=repo.array(state,"betaEntries").objects();val series=entry.optBoolean("isRecurringTemplate")||entry.optString("recurringGroupId").isNotBlank();if(scope=="following"&&series){val tplId=if(entry.optBoolean("isRecurringTemplate"))entry.optString("id") else entry.optString("recurringGroupId");val from=entry.optString("date");val tpl=all.firstOrNull{it.optString("id")==tplId};val kept=all.filterNot{it.optString("id")!=tplId&&it.optString("recurringGroupId")==tplId&&it.optString("date")>=from}.toMutableList();if(tpl!=null){if(tpl.optString("date")>=from)kept.removeAll{it.optString("id")==tplId}else runCatching{LocalDate.parse(from).minusDays(1).toString()}.getOrNull()?.let{tpl.put("recurrenceEnd",it)}};replaceBetaEntries(state,kept)}else replaceBetaEntries(state,all.filterNot{it.optString("id")==entry.optString("id")})}
@Composable private fun BetaRecurringChoiceDialog(onDismiss:()->Unit,onChoice:(String)->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).padding(24.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){Surface(Modifier.widthIn(max=340.dp).fillMaxWidth().clickable{},shape=RoundedCornerShape(26.dp),color=appGlass(dark),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){Column(Modifier.padding(start=26.dp,end=26.dp,top=28.dp,bottom=22.dp)){
   Text("Dies ist ein Dauerauftrag. Was möchtest du ändern?",Modifier.padding(bottom=14.dp),fontSize=16.5.sp,lineHeight=23.1.sp,fontWeight=FontWeight.Bold,color=appInk(dark))
   Text("Diesen Eintrag ändern",Modifier.fillMaxWidth().clickable{onChoice("this")}.padding(horizontal=4.dp,vertical=14.dp),fontSize=15.sp,color=appInk(dark))
   HorizontalDivider(color=appRule(dark))
   Text("Diesen und alle folgenden Einträge ändern",Modifier.fillMaxWidth().clickable{onChoice("following")}.padding(horizontal=4.dp,vertical=14.dp),fontSize=15.sp,color=appInk(dark))
   Surface(Modifier.fillMaxWidth().padding(top=10.dp).clickable(onClick=onDismiss),shape=RoundedCornerShape(999.dp),color=if(dark)Color(0xFF52545C) else Color.White){Text("Abbrechen",Modifier.padding(horizontal=10.dp,vertical=14.dp),fontSize=15.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=if(dark)Color(0xFFF0F0F2) else Color(0xFF3A3B40))}
  }}}
 }
}
@Composable private fun BetaEditDialog(entry:JSONObject,scope:String,state:JSONObject,repo:HouseholdRepository,onDismiss:()->Unit,onSave:(String,String,String,Double,String,Boolean,Boolean,String,String?)->Unit,onDelete:()->Unit){
 val template=if(entry.optBoolean("isRecurringTemplate"))entry else repo.array(state,"betaEntries").objects().firstOrNull{it.optString("id")==entry.optString("recurringGroupId")}
 var confirmDelete by remember(entry){mutableStateOf(false)}
 BetaForm(entry,template,scope,null,state,onDismiss,{type,title,date,amount,category,recurring,reconciled,period,endDate->onSave(type,title,date,amount,category,recurring,reconciled,period,endDate)},{confirmDelete=true})
 if(confirmDelete){
  val series=entry.optBoolean("isRecurringTemplate")||entry.optString("recurringGroupId").isNotBlank()
  val message=if(scope=="following"&&series)"Diesen und alle folgenden Termine dieses Dauerauftrags wirklich löschen? Es werden auch keine neuen mehr erzeugt." else if(entry.optBoolean("isRecurringTemplate"))"Diesen Dauerauftrag wirklich löschen? Bereits erzeugte Folgebuchungen bleiben erhalten, es werden aber keine neuen mehr angelegt." else "Diesen Eintrag wirklich löschen?"
  ReferenceConfirm(message,onCancel={confirmDelete=false},onConfirm=onDelete)
 }
}
@Composable private fun BetaEntryDialog(initialType:String?,state:JSONObject,onDismiss:()->Unit,onSave:(String,String,String,Double,String,Boolean,String,String?,Boolean)->Unit){
 BetaForm(null,null,"this",initialType,state,onDismiss,{type,title,date,amount,category,recurring,reconciled,period,endDate->onSave(type,title,date,amount,category,recurring,period,endDate,reconciled)},null)
}

@Composable private fun BetaForm(entry:JSONObject?,template:JSONObject?,scope:String,initialType:String?,state:JSONObject,onDismiss:()->Unit,onSave:(String,String,String,Double,String,Boolean,Boolean,String,String?)->Unit,onDelete:(()->Unit)?){
 val dark=LocalAppDark.current
 var type by remember(entry,initialType){mutableStateOf(if((entry?.optString("type")?:initialType) in setOf("income","einnahme"))"einnahme" else "ausgabe")}
 var title by remember(entry){mutableStateOf(entry?.optString("title").orEmpty())}
 var date by remember(entry){mutableStateOf(entry?.optString("date")?:LocalDate.now().toString())}
 var amount by remember(entry){mutableStateOf(entry?.let{String.format(Locale.GERMANY,"%.2f",it.optDouble("amount"))}.orEmpty())}
 var category by remember(entry){mutableStateOf(entry?.optString("category").orEmpty())}
 var recurring by remember(entry){mutableStateOf(entry?.optBoolean("recurring")?:false)}
 var reconciled by remember(entry){mutableStateOf(entry?.optBoolean("reconciled")?:false)}
 var period by remember(entry){mutableStateOf(template?.optString("recurrencePeriod","monthly")?:"monthly")}
 val initialEnd=template?.optString("recurrenceEnd").orEmpty().takeIf{it!="null"}.orEmpty()
 var endEnabled by remember(entry){mutableStateOf(initialEnd.isNotBlank())}
 var endDate by remember(entry){mutableStateOf(initialEnd)}
 var amountAutofilled by remember(entry){mutableStateOf(false)}
 var showSuggestions by remember(entry){mutableStateOf(false)}
 var titleHadFocus by remember(entry){mutableStateOf(false)}
 var datePicker by remember(entry){mutableStateOf<String?>(null)}
 var error by remember(entry){mutableStateOf("")}
 val knownTitles=remember(state,entry){betaKnownTitles(state)}
 val latestByTitle=remember(state,entry){val latest=HashMap<String,JSONObject>();state.optJSONArray("betaEntries")?.objects()?.forEach{item->val key=item.optString("title");val previous=latest[key];if(previous==null||item.optString("date")>previous.optString("date"))latest[key]=item};latest}
 val suggestions=if(showSuggestions&&title.isNotBlank())knownTitles.filter{it.contains(title.trim(),true)}.take(6) else emptyList()
 val recurrenceFields=entry==null||entry.optBoolean("isRecurringTemplate")||scope=="following"||entry.optString("recurringGroupId").isBlank()
 fun autofill(selected:String){
  if(entry!=null)return
  latestByTitle[selected.trim()]?.let{
   amount=String.format(Locale.GERMANY,"%.2f",it.optDouble("amount"));amountAutofilled=true;type=if(betaIsIncome(it))"einnahme" else "ausgabe"
  }
 }
 val heading=if(entry==null)"Neuer Eintrag" else if(scope=="following")"Diesen und alle folgenden ändern" else "Eintrag bearbeiten"
 androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  val panelHeight=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.85f).dp
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).imePadding().clickable(onClick=onDismiss),contentAlignment=Alignment.Center){
   Surface(Modifier.padding(20.dp).widthIn(max=400.dp).fillMaxWidth().heightIn(max=panelHeight).clickable{},shape=RoundedCornerShape(26.dp),color=if(dark)Color(0xFF2E2F35) else Color(0xFFD0D1D6),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){
    Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp)){
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(heading,Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=appInk(dark));Surface(Modifier.size(36.dp).clickable(onClick=onDismiss),shape=CircleShape,color=appPaper(dark)){Box(contentAlignment=Alignment.Center){Text("×",fontSize=18.sp,color=appInkSoft(dark))}}}
     Column(Modifier.padding(top=4.dp).fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
      Row(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){
       BetaTypeOption("Ausgabe",type=="ausgabe",appRed(dark)){type="ausgabe"}
       BetaTypeOption("Einnahme",type=="einnahme",appGreen(dark)){type="einnahme"}
      }
      BetaInput(title,{title=it;showSuggestions=true},"Titel",Modifier.onFocusChanged{focus->if(!focus.isFocused&&titleHadFocus){autofill(title);titleHadFocus=false};if(focus.isFocused)titleHadFocus=true})
      if(suggestions.isNotEmpty())Surface(shape=RoundedCornerShape(12.dp),color=appSurface(dark),shadowElevation=4.dp){Column{suggestions.forEach{suggestion->Text(suggestion,Modifier.fillMaxWidth().clickable{title=suggestion;autofill(suggestion);showSuggestions=false}.padding(horizontal=14.dp,vertical=10.dp),fontSize=14.sp,color=appInk(dark))}}}
      Text(if(recurring)"Erste Ausführung" else "Datum",fontSize=12.sp,color=appInkSoft(dark))
      BetaDateField(date){datePicker="date"}
      BetaInput(amount,{amount=centInput(it);amountAutofilled=false},"Betrag (€)",Modifier.onFocusChanged{if(it.isFocused){showSuggestions=false;if(amountAutofilled){amount="";amountAutofilled=false}}},decimal=true)
      BetaInput(category,{category=it},"Kategorie (optional)")
      BetaCheck("Dauerauftrag",recurring,{recurring=it})
      if(recurring&&recurrenceFields){Surface(shape=RoundedCornerShape(14.dp),color=appPaper(dark)){Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
       BetaCheck("Ablaufdatum festlegen",endEnabled,{endEnabled=it},small=true)
       if(endEnabled)BetaDateField(endDate){datePicker="end"}
       Text("Periode",fontSize=12.sp,color=appInkSoft(dark))
       BetaPeriodField(period){period=it}
      }}}
      BetaCheck("Abgeglichen",reconciled,{reconciled=it})
      if(error.isNotBlank())Text(error,fontSize=12.sp,color=appRed(dark))
      Surface(Modifier.fillMaxWidth().clickable{
       val value=amount.replace(".","").replace(",",".").toDoubleOrNull()
       error=when{
        title.isBlank()->"Bitte einen Titel eingeben."
        runCatching{LocalDate.parse(date)}.isFailure->"Bitte ein gültiges Datum wählen."
        value==null||!value.isFinite()||value<=0->"Bitte einen Betrag größer als 0 eingeben."
        recurring&&endEnabled&&runCatching{LocalDate.parse(endDate)}.isFailure->"Bitte ein gültiges Ablaufdatum wählen."
        else->""
       }
       if(error.isEmpty()&&value!=null)onSave(type,title.trim(),date,value,category.trim(),recurring,reconciled,period,endDate.takeIf{endEnabled&&it.isNotBlank()})
      },shape=RoundedCornerShape(999.dp),color=appBlue(dark)){Text("Speichern",Modifier.padding(16.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)}
      if(onDelete!=null)Surface(Modifier.fillMaxWidth().clickable(onClick=onDelete),shape=RoundedCornerShape(999.dp),color=appExpenseSoft(dark)){Text("Eintrag löschen",Modifier.padding(16.dp),textAlign=TextAlign.Center,fontSize=16.sp,fontWeight=FontWeight.Bold,color=appRed(dark))}
     }
    }
   }
  }
 }
 datePicker?.let{target->
  val context=androidx.compose.ui.platform.LocalContext.current
  DisposableEffect(target){
   val initial=runCatching{LocalDate.parse(if(target=="date")date else endDate)}.getOrDefault(LocalDate.now())
   val dialog=android.app.DatePickerDialog(context,{_,year,month,day->val selected=LocalDate.of(year,month+1,day).toString();if(target=="date")date=selected else endDate=selected;datePicker=null},initial.year,initial.monthValue-1,initial.dayOfMonth)
   dialog.setOnDismissListener{datePicker=null};dialog.show();onDispose{dialog.dismiss()}
  }
 }
}
@Composable private fun BetaInput(value:String,onValue:(String)->Unit,placeholder:String,modifier:Modifier=Modifier,decimal:Boolean=false){
 val dark=LocalAppDark.current
 BasicTextField(value,onValue,modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(appPaper(dark)).border(1.5.dp,if(dark)Color(0xFF4A4C52) else Color(0xFFB0B2B8),RoundedCornerShape(16.dp)).padding(horizontal=16.dp,vertical=14.dp),singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(fontSize=16.sp,color=appInk(dark)),cursorBrush=androidx.compose.ui.graphics.SolidColor(appBlue(dark)),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=if(decimal)androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text),decorationBox={inner->Box{if(value.isEmpty())Text(placeholder,fontSize=16.sp,color=appInkSoft(dark));inner()}})
}
@Composable private fun BetaDateField(value:String,onClick:()->Unit){
 val dark=LocalAppDark.current
 val label=runCatching{LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault("tt.mm.jjjj")
 Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(appPaper(dark)).border(1.5.dp,if(dark)Color(0xFF4A4C52) else Color(0xFFB0B2B8),RoundedCornerShape(16.dp)).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),fontSize=16.sp,color=appInk(dark));CalendarOutlineIcon(appInkSoft(dark))}
}
@Composable private fun CalendarOutlineIcon(color:Color){Canvas(Modifier.size(18.dp)){val w=size.width/24;val h=size.height/24;val st=Stroke(2*w);drawRoundRect(color,Offset(3*w,5*h),Size(18*w,16*h),CornerRadius(2*w),style=st);drawLine(color,Offset(3*w,10*h),Offset(21*w,10*h),2*w);drawLine(color,Offset(8*w,2*h),Offset(8*w,7*h),2*w,StrokeCap.Round);drawLine(color,Offset(16*w,2*h),Offset(16*w,7*h),2*w,StrokeCap.Round)}}
@Composable private fun BetaTypeOption(label:String,selected:Boolean,color:Color,onClick:()->Unit){Row(Modifier.clickable(onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Canvas(Modifier.size(14.dp)){drawCircle(if(selected)color else appRuleStrongForCanvas,style=Stroke(1.5.dp.toPx()));if(selected)drawCircle(color,radius=size.minDimension*.25f)};Text(label,fontSize=14.sp,color=appInk(LocalAppDark.current))}}
private val appRuleStrongForCanvas=Color(0xFF9B9DA4)
@Composable private fun BetaCheck(label:String,checked:Boolean,onChecked:(Boolean)->Unit,small:Boolean=false){val dark=LocalAppDark.current;Row(Modifier.clickable{onChecked(!checked)},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.size(if(small)16.dp else 18.dp).clip(RoundedCornerShape(3.dp)).background(if(checked)appBlue(dark) else appPaper(dark)).border(1.dp,if(checked)appBlue(dark) else appRuleStrong(dark),RoundedCornerShape(3.dp)),contentAlignment=Alignment.Center){if(checked)Text("✓",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)};Text(label,fontSize=if(small)13.5.sp else 14.sp,color=appInk(dark))}}
@Composable private fun BetaPeriodField(value:String,onValue:(String)->Unit){
 val periods=listOf("weekly" to "Jede Woche","biweekly" to "Alle 2 Wochen","4weekly" to "Alle 4 Wochen","monthly" to "Jeden Monat","2monthly" to "Alle 2 Monate","3monthly" to "Alle 3 Monate","6monthly" to "Alle 6 Monate","yearly" to "Jedes Jahr")
 val dark=LocalAppDark.current;var expanded by remember{mutableStateOf(false)}
 Box{Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(appGlass(dark)).clickable{expanded=true}.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(periods.firstOrNull{it.first==value}?.second?:"Jeden Monat",Modifier.weight(1f),fontSize=14.sp,color=appInk(dark));Text("⌄",color=appInkSoft(dark))};DropdownMenu(expanded,{expanded=false}){periods.forEach{(key,label)->DropdownMenuItem(text={Text(label)},onClick={onValue(key);expanded=false})}}}
}

private fun parseBetaImportCsv(text:String):List<JSONObject>{fun split(line:String):List<String>{var s=line;if(s.startsWith("\""))s=s.drop(1);if(s.endsWith("\""))s=s.dropLast(1);return s.split("\";\"")};val lines=text.split(Regex("\\r\\n|\\n|\\r")).filter{it.isNotBlank()};if(lines.size<2)return emptyList();val header=split(lines.first()).map{it.trim().lowercase()};val datum=header.indexOf("datum");val titel=header.indexOf("titel");val kat=header.indexOf("hauptkategorie");val betrag=header.indexOf("betrag");val abgeglichen=header.indexOf("abgeglichen");if(datum<0||titel<0||betrag<0)return emptyList();return lines.drop(1).mapNotNull{line->val c=split(line);val raw=c.getOrElse(datum){""}.trim().split(".");if(raw.size!=3)return@mapNotNull null;val date=raw[2]+"-"+raw[1].padStart(2,'0')+"-"+raw[0].padStart(2,'0');val title=c.getOrElse(titel){""}.trim();if(title.isBlank())return@mapNotNull null;val amount=c.getOrElse(betrag){"0"}.trim().replace(".","").replace(",",".").toDoubleOrNull()?:return@mapNotNull null;if(amount==0.0)return@mapNotNull null;var category=if(kat>=0)c.getOrElse(kat){""}.trim() else "";if(category=="Nicht zugeordnet")category="";val rec=abgeglichen>=0&&c.getOrElse(abgeglichen){""}.trim().lowercase()=="ja";JSONObject().put("id",UUID.randomUUID().toString()).put("type",if(amount>=0)"einnahme" else "ausgabe").put("title",title).put("date",date).put("amount",kotlin.math.abs(amount)).put("category",category).put("recurring",false).put("reconciled",rec)}}

@Composable private fun ReferenceSummary(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().padding(bottom=20.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),content=content)}
@Composable private fun ReferenceSettingsLabel(label:String,top:Int=4){Text(label.uppercase(Locale.GERMAN),Modifier.padding(start=4.dp,end=4.dp,top=top.dp),fontSize=12.5.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=.5.sp,color=appInkSoft(LocalAppDark.current))}
@Composable private fun ReferenceSettingsButton(label:String,color:Color?=null,textColor:Color=Color.White,onClick:()->Unit){Surface(Modifier.fillMaxWidth().padding(bottom=20.dp).clickable(onClick=onClick),shape=RoundedCornerShape(999.dp),color=color?:appBlue(LocalAppDark.current)){Text(label,Modifier.padding(16.dp),fontSize=16.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=textColor)}}
@Composable private fun ReferenceToggle(checked:Boolean,onChecked:(Boolean)->Unit){
 val dark=LocalAppDark.current
 Box(Modifier.size(width=51.dp,height=31.dp).clip(RoundedCornerShape(999.dp)).background(if(checked)appBlue(dark) else appRule(dark)).clickable{onChecked(!checked)}){Surface(Modifier.padding(start=if(checked)22.dp else 2.dp,top=2.dp).size(27.dp),shape=CircleShape,color=Color.White,shadowElevation=2.dp){}}
}
@Composable private fun ReferenceSectionHeader(title:String,sub:String,icon:String,open:Boolean,top:Int=26,onClick:()->Unit){
 val dark=LocalAppDark.current
 Row(Modifier.fillMaxWidth().padding(top=top.dp,bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(appBlue(dark)),contentAlignment=Alignment.Center){HtmlReferenceIcon(icon,Color.White,17.dp)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,fontSize=15.sp,fontWeight=FontWeight.Bold);Text(sub,Modifier.padding(top=1.dp),fontSize=13.sp,color=appInkSoft(dark))};HtmlReferenceIcon(if(open)"chevron-up" else "chevron-down",appInkSoft(dark),18.dp,2.4f)}
}
@Composable private fun ReferenceKontostandSettings(state:JSONObject,repo:HouseholdRepository,notificationEnabled:Boolean,onNotification:(Boolean)->Unit,onImport:()->Unit,onRestored:(JSONObject)->Unit){
 val dark=LocalAppDark.current;val entries=remember(state,repo.revision){repo.array(state,"betaEntries").objects()};val templates=entries.filter{it.optBoolean("isRecurringTemplate")};val hidden=remember(state,repo.revision){repo.array(state,"betaHiddenTitles").strings()};val comparator=remember{java.text.Collator.getInstance(Locale.GERMAN)};val activeTitles=entries.map{it.optString("title")}.distinct().filter{it !in hidden}.sortedWith(comparator);val hiddenTitles=hidden.sortedWith(comparator)
 var showTemplates by remember{mutableStateOf(repo.getSetting("household-show-beta-templates")=="1")};var showTitles by remember{mutableStateOf(repo.getSetting("household-show-beta-title-suggestions")=="1")};var templateToRemove by remember{mutableStateOf<JSONObject?>(null)}
 Column{
  ReferenceSettingsLabel("CSV-Import",0)
  ReferenceSummary{Text("Importiert eine CSV-Datei (Datum/Titel/Hauptkategorie/Betrag/Abgeglichen) als eigenständige Einträge in den Kontostand-Tab, ohne andere Bereiche der App zu berühren.",Modifier.padding(bottom=4.dp),fontSize=15.sp)}
  ReferenceSettingsButton("CSV importieren (Kontostand)",onClick=onImport)
  ReferenceSettingsLabel("Benachrichtigung")
  ReferenceSummary{Row(Modifier.fillMaxWidth().padding(horizontal=2.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Text("Kontostand dauerhaft anzeigen",Modifier.weight(1f),fontSize=15.sp);ReferenceToggle(notificationEnabled,onNotification)};Text("Zeigt den Kontostand als dauerhafte Benachrichtigung, die sich nicht wegwischen lässt. Eingeklappt ist nur \"Kontostand\" sichtbar, der Betrag erscheint erst beim Herunterziehen der Benachrichtigung.",Modifier.padding(top=8.dp),fontSize=13.sp,color=appInkSoft(dark))}
  ReferenceSectionHeader("Dauerauftrag-Vorlagen",templates.size.toString()+" aktiv","recurring",showTemplates,16){showTemplates=!showTemplates;repo.setSetting("household-show-beta-templates",if(showTemplates)"1" else "0")}
  if(showTemplates){
   if(templates.isEmpty())OriginalEmpty("Noch keine Dauerauftrag-Vorlagen vorhanden.") else templates.forEach{t->
    var expanded by remember(t.optString("id")){mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().padding(bottom=14.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable{expanded=!expanded}.padding(horizontal=20.dp,vertical=18.dp)){
     Row(Modifier.fillMaxWidth().padding(bottom=10.dp),verticalAlignment=Alignment.Bottom){Text(t.optString("title"),Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text((if(t.optString("type")=="einnahme")"+" else "-")+money(t.optDouble("amount")),fontSize=14.5.sp,fontWeight=FontWeight.Bold,color=if(t.optString("type")=="einnahme")appGreen(dark) else appRed(dark))}
     val labels=mapOf("weekly" to "Jede Woche","biweekly" to "Alle 2 Wochen","4weekly" to "Alle 4 Wochen","monthly" to "Jeden Monat","2monthly" to "Alle 2 Monate","3monthly" to "Alle 3 Monate","6monthly" to "Alle 6 Monate","yearly" to "Jedes Jahr");val end=t.optString("recurrenceEnd").takeIf{it.isNotBlank()&&it!="null"}
     Text((labels[t.optString("recurrencePeriod")]?:"Jeden Monat")+(end?.let{" · bis "+it.split('-').reversed().joinToString(".")}?:""),Modifier.padding(bottom=8.dp),fontSize=12.5.sp,color=appInkSoft(dark))
     if(expanded)Row(Modifier.fillMaxWidth().padding(top=14.dp)){ReferenceAction("Aus Vorlagen entfernen","action-template-delete",true){templateToRemove=t}}
    }
   }
  }
  ReferenceSectionHeader("Titel-Vorschläge",activeTitles.size.toString()+" aktiv"+if(hidden.isNotEmpty())", "+hidden.size+" ausgeblendet" else "","filter-all",showTitles){showTitles=!showTitles;repo.setSetting("household-show-beta-title-suggestions",if(showTitles)"1" else "0")}
  if(showTitles){
   ReferenceSummary{Text("Nicht mehr benötigte Vorschläge (z.B. alte Titel wie \"O2\") lassen sich hier entfernen, ohne die dazugehörigen Einträge zu löschen.",Modifier.padding(bottom=4.dp),fontSize=15.sp)}
   if(activeTitles.isEmpty()&&hiddenTitles.isEmpty())OriginalEmpty("Noch keine Titel vorhanden.") else{
    if(activeTitles.isEmpty())OriginalEmpty("Alle Titel sind ausgeblendet.") else activeTitles.forEach{title->ReferenceTitleSuggestionRow(title,false){repo.array(state,"betaHiddenTitles").put(title);repo.save(state);onRestored(state)}}
    if(hiddenTitles.isNotEmpty()){ReferenceSettingsLabel("Ausgeblendet",12);hiddenTitles.forEach{title->ReferenceTitleSuggestionRow(title,true){val a=JSONArray();repo.array(state,"betaHiddenTitles").strings().filter{it!=title}.forEach{a.put(it)};state.put("betaHiddenTitles",a);repo.save(state);onRestored(state)}}}
   }
  }
 }
 templateToRemove?.let{tpl->ReferenceConfirm("\""+tpl.optString("title")+"\" wirklich aus den Vorlagen entfernen? Der Eintrag selbst und bereits erzeugte Folgebuchungen bleiben erhalten, es werden nur keine neuen mehr angelegt.",icon="🔁",confirmLabel="Entfernen",onCancel={templateToRemove=null},onConfirm={tpl.remove("isRecurringTemplate");tpl.remove("recurrencePeriod");tpl.remove("recurrenceEnd");tpl.put("recurring",false);templateToRemove=null;repo.save(state);onRestored(state)})}
}
@Composable private fun ReferenceTitleSuggestionRow(title:String,hidden:Boolean,onClick:()->Unit){
 val dark=LocalAppDark.current
 Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=14.sp,color=if(hidden)appInkSoft(dark) else appInk(dark));Surface(Modifier.padding(start=8.dp).height(40.dp).widthIn(min=48.dp).clickable(onClick=onClick),shape=RoundedCornerShape(999.dp),color=if(hidden)if(dark)Color(0xFFFFB03F) else Color(0xFFFF9F0A) else appRed(dark)){Box(Modifier.padding(horizontal=12.dp),contentAlignment=Alignment.Center){HtmlReferenceIcon(if(hidden)"action-refresh" else "action-close",Color.White,15.dp)}}}
}

@Composable private fun ReferenceWhatsNew(entries:List<NativeChange>,onDismiss:()->Unit){
 val dark=LocalAppDark.current
 androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)){
  Box(Modifier.fillMaxSize().background(Color(0x73141416)).padding(20.dp).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){Surface(Modifier.widthIn(max=400.dp).fillMaxWidth().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.85f).dp).clickable{},shape=RoundedCornerShape(26.dp),color=if(dark)Color(0xFF2E2F35) else Color(0xFFD0D1D6),border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(dark).16f else .5f)),shadowElevation=12.dp){Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp)){
   Row(Modifier.fillMaxWidth().padding(bottom=14.dp),verticalAlignment=Alignment.CenterVertically){Text("🎉 Neu in dieser Version",Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.ExtraBold);Surface(Modifier.clickable(onClick=onDismiss),shape=CircleShape,color=appPaper(dark)){Text("✕",Modifier.padding(horizontal=13.dp,vertical=8.dp),fontSize=18.sp,color=appInkSoft(dark))}}
   Column(Modifier.padding(bottom=20.dp)){entries.forEach{entry->if(entries.size>1)Text(entry.date.uppercase(Locale.GERMAN),Modifier.padding(top=14.dp,bottom=6.dp),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=.44.sp,color=appInkSoft(dark));entry.items.forEach{item->Row(Modifier.padding(bottom=8.dp)){Text("•",Modifier.width(16.dp),fontSize=15.sp,lineHeight=25.5.sp);Text(item,Modifier.weight(1f),fontSize=15.sp,lineHeight=25.5.sp)}}}}
   Surface(Modifier.fillMaxWidth().clickable(onClick=onDismiss),shape=RoundedCornerShape(999.dp),color=appBlue(dark)){Text("Verstanden",Modifier.padding(16.dp),fontSize=16.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=Color.White)}
  }}}
 }
}

@Composable private fun SettingsDialog(state:JSONObject,repo:HouseholdRepository,onDismiss:()->Unit,onThemeChanged:()->Unit,onVisibilityChanged:()->Unit,onRestored:(JSONObject)->Unit,onWhatsNew:()->Unit){val InkSoft=appInkSoft(LocalAppDark.current);val Red=appRed(LocalAppDark.current);val Blue=appBlue(LocalAppDark.current);
    var page by remember{mutableStateOf<String?>(null)};var visibilityWarning by remember{mutableStateOf("")};var resetConfirm by remember{mutableStateOf(false)};val activity=androidx.compose.ui.platform.LocalContext.current as Activity;var notificationEnabled by remember{mutableStateOf(activity.getSharedPreferences(BetaWidgetProvider.PREFS_NAME,Context.MODE_PRIVATE).getBoolean("kontostand_notification_enabled",false))};var backupText by remember{mutableStateOf("")};var restoreData by remember{mutableStateOf<JSONObject?>(null)};var restoreStatus by remember{mutableStateOf("")};var restoreConfirm by remember{mutableStateOf(false)};var betaImportRows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var betaImportStatus by remember{mutableStateOf("")};val ioScope=rememberCoroutineScope();var ioBusy by remember{mutableStateOf(false)}
    val createBackup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->if(uri!=null)ioScope.launch{
        ioBusy=true
        // The snapshot belongs to this export; later edits cannot change it during IO.
        val snapshot=state.toString()
        val result=withContext(Dispatchers.IO){runCatching{val csv=repo.buildBackup(JSONObject(snapshot));activity.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(csv)}?:error("Datei konnte nicht geöffnet werden")}}
        restoreStatus=if(result.isSuccess)"Sicherung gespeichert." else "Sicherung konnte nicht gespeichert werden.";ioBusy=false
    }}
    val openBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)ioScope.launch{
        ioBusy=true
        val result=withContext(Dispatchers.IO){runCatching{val text=activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:error("Datei konnte nicht gelesen werden.");parseHouseholdBackup(text)}}
        result.onSuccess{restoreData=it;restoreConfirm=true}.onFailure{restoreStatus=if(it.message=="Datei konnte nicht gelesen werden.")"Datei konnte nicht gelesen werden." else "Das ist keine gültige Sicherung."};ioBusy=false
    }}
    val openBetaImport=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)ioScope.launch{
        ioBusy=true
        val result=withContext(Dispatchers.IO){runCatching{val text=activity.contentResolver.openInputStream(uri)?.use{String(it.readBytes(),java.nio.charset.Charset.forName("ISO-8859-1"))}?:error("Datei konnte nicht geöffnet werden");parseBetaImportCsv(text)}}
        result.onSuccess{rows->betaImportRows=rows;betaImportStatus=if(rows.isEmpty())"Keine gültigen Zeilen in der CSV-Datei gefunden." else ""}.onFailure{betaImportRows=emptyList();betaImportStatus="Import fehlgeschlagen: Datei konnte nicht gelesen werden."};ioBusy=false
    }}
    if(betaImportRows.isNotEmpty())ReferenceConfirm(betaImportRows.size.toString()+" Einträge aus der CSV-Datei importieren? Sie werden zu den bestehenden Beta-Einträgen hinzugefügt.",icon="📥",confirmLabel="Importieren",onCancel={betaImportRows=emptyList()},onConfirm={val target=repo.array(state,"betaEntries");val count=betaImportRows.size;betaImportRows.forEach{target.put(it)};betaImportRows=emptyList();generateBetaRecurringOccurrences(state,repo,YearMonth.now());repo.save(state);betaImportStatus=count.toString()+" Einträge erfolgreich importiert.";onRestored(state)})
    if(betaImportStatus.isNotBlank())ReferenceMessage(betaImportStatus,icon=if(betaImportStatus.contains("erfolgreich"))"✅" else "⚠️",onDismiss={betaImportStatus=""})
    var autoFolderName by remember{mutableStateOf(repo.getSetting("haushaltsbuch-autobackup-name"))}
    val chooseAutoFolder=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null)ioScope.launch{
        runCatching{activity.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}
        repo.setSetting("haushaltsbuch-autobackup-uri",uri.toString());repo.setSetting("haushaltsbuch-autobackup-enabled","1")
        val name=withContext(Dispatchers.IO){DocumentFile.fromTreeUri(activity,uri)?.name?:"Ausgewählter Ordner"};repo.setSetting("haushaltsbuch-autobackup-name",name);autoFolderName=name;repo.autoBackup(state)
    }};var themeChoice by remember{mutableStateOf(repo.getSetting("household-theme")?.takeIf{it in setOf("light","dark","system")} ?: "system")};var tabVisibility by remember{mutableStateOf(runCatching{JSONObject(repo.getSetting("household-tab-visibility")?:"{}")}.getOrDefault(JSONObject()))}
    if(restoreConfirm)ReferenceConfirm("Aktuelle Daten durch die Sicherung ersetzen? Das kann nicht rückgängig gemacht werden.",icon="📥",confirmLabel="Ersetzen",onCancel={restoreConfirm=false;restoreData=null},onConfirm={restoreConfirm=false;val parsed=restoreData;restoreData=null;ioScope.launch{ioBusy=true;val result=withContext(Dispatchers.IO){runCatching{repo.restoreParsedBackup(requireNotNull(parsed))}};result.onSuccess{restored->onRestored(restored);restoreStatus="Datenbank erfolgreich wiederhergestellt."}.onFailure{restoreStatus="Sicherung konnte nicht wiederhergestellt werden."};ioBusy=false}})
    Box(Modifier.fillMaxSize().background(Color(0x73141416)).clickable(onClick=onDismiss),contentAlignment=Alignment.Center){
        Surface(
            modifier=Modifier.padding(20.dp).widthIn(max=400.dp).fillMaxWidth().clickable{},
            shape=RoundedCornerShape(26.dp),
            color=if(LocalAppDark.current) Color(0xFF2E2F35) else Color(0xFFD0D1D6),
            shadowElevation=12.dp,
            border=androidx.compose.foundation.BorderStroke(1.dp,Color.White.copy(alpha=if(LocalAppDark.current).16f else .5f))
        ){
            Column(Modifier.heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.85f).dp).verticalScroll(rememberScrollState()).padding(22.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    if(page!=null){
                        Surface(shape=CircleShape,color=appPaper(LocalAppDark.current),modifier=Modifier.size(36.dp).clickable{page=null}){
                            Box(contentAlignment=Alignment.Center){ChevronIcon(left=true,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(page?:"Einstellungen",fontSize=19.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f),textAlign=if(page==null)TextAlign.Start else TextAlign.Center)
                    if(page==null)Surface(shape=CircleShape,color=appPaper(LocalAppDark.current),modifier=Modifier.size(36.dp).clickable(onClick=onDismiss)){
                        Box(contentAlignment=Alignment.Center){Text("×",fontSize=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    } else Spacer(Modifier.width(34.dp))
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
                        "Darstellung"->ReferenceSummary{Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(appPaper(LocalAppDark.current)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("light" to "Hell","dark" to "Dunkel","system" to "System").forEach{(key,label)->Surface(Modifier.weight(1f).clickable{themeChoice=key;repo.setSetting("household-theme",key);onThemeChanged()},shape=RoundedCornerShape(999.dp),color=if(themeChoice==key)MaterialTheme.colorScheme.surface else Color.Transparent,shadowElevation=if(themeChoice==key)1.dp else 0.dp){Text(label,Modifier.padding(horizontal=8.dp,vertical=11.dp),textAlign=TextAlign.Center,fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(themeChoice==key)appInk(LocalAppDark.current) else InkSoft)}}}}

                        "Backup"->Column{
 ReferenceSummary{Text("Alle deine Daten werden sicher direkt auf deinem Gerät gespeichert.",Modifier.padding(bottom=4.dp),fontSize=15.sp)}
 ReferenceSettingsLabel("Manuelles Backup",16);ReferenceSettingsButton("Jetzt sichern (Downloads-Ordner)"){createBackup.launch("haushaltsbuch-sicherung-"+LocalDate.now()+".csv")}
 ReferenceSettingsLabel("Automatisches Backup",16)
 ReferenceSummary{Text("Wähle einen Ordner (z. B. auf einer Cloud- oder NAS-verknüpften Speicherkarte). Die App sichert dort automatisch, solange die App geöffnet ist bzw. in den Hintergrund wechselt.",Modifier.padding(bottom=4.dp),fontSize=15.sp);Text(if(autoFolderName!=null&&repo.getSetting("haushaltsbuch-autobackup-enabled")=="1")"Aktiv: „$autoFolderName“" else "Noch kein Ordner gewählt",Modifier.padding(top=8.dp),fontSize=14.sp,fontWeight=FontWeight.Bold)}
 ReferenceSettingsButton(if(autoFolderName!=null)"Anderen Ordner wählen" else "Ordner für automatisches Backup wählen"){chooseAutoFolder.launch(null)}
 if(autoFolderName!=null&&repo.getSetting("haushaltsbuch-autobackup-enabled")=="1")ReferenceSettingsButton("Automatisches Backup deaktivieren",appPrimarySoft(LocalAppDark.current),Blue){repo.setSetting("haushaltsbuch-autobackup-enabled","0");repo.removeSetting("haushaltsbuch-autobackup-uri");repo.removeSetting("haushaltsbuch-autobackup-name");autoFolderName=null}
 ReferenceSettingsLabel("Wiederherstellen",16);ReferenceSettingsButton("Sicherungsdatei auswählen…"){openBackup.launch(arrayOf("text/csv","text/comma-separated-values","text/plain","application/json"))}
 if(restoreStatus.isNotBlank())ReferenceMessage(restoreStatus,icon=if(restoreStatus.contains("erfolgreich")||restoreStatus=="Sicherung gespeichert.")"✅" else "⚠️",onDismiss={restoreStatus=""})
 }

                        "Kontostand"->ReferenceKontostandSettings(state,repo,notificationEnabled,{enabled->notificationEnabled=enabled;activity.getSharedPreferences(BetaWidgetProvider.PREFS_NAME,Context.MODE_PRIVATE).edit().putBoolean("kontostand_notification_enabled",enabled).apply();if(enabled){if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(activity,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),4401);val entries=repo.array(state,"betaEntries").objects();val balance=betaBalanceAt(entries,YearMonth.now().atEndOfMonth());val intent=Intent(activity,KontostandForegroundService::class.java).putExtra(KontostandForegroundService.EXTRA_BALANCE_TEXT,money(balance)).putExtra(KontostandForegroundService.EXTRA_IS_POSITIVE,balance>=0);ContextCompat.startForegroundService(activity,intent)}else activity.stopService(Intent(activity,KontostandForegroundService::class.java))},{openBetaImport.launch(arrayOf("text/csv","text/comma-separated-values","text/plain"))},onRestored)

                        "Zurücksetzen"->Column{
 ReferenceSettingsButton("Alle Daten löschen"){resetConfirm=true}
 if(resetConfirm)ReferenceConfirm("Wirklich alle Einträge löschen? Das kann nicht rückgängig gemacht werden.",icon="⚠️",confirmLabel="Alles löschen",onCancel={resetConfirm=false},onConfirm={val fresh=repo.emptyState();repo.save(fresh);resetConfirm=false;onRestored(fresh)})
 }

                        "Update-Historie"->Column{
 ReferenceSettingsButton("Letzte Neuerungen anzeigen",onClick=onWhatsNew)
 Column(Modifier.padding(top=8.dp)){NativeChangelog.asReversed().forEach{entry->Column(Modifier.padding(bottom=18.dp)){Text(entry.date,fontSize=12.5.sp,fontWeight=FontWeight.Bold,color=Blue,modifier=Modifier.padding(bottom=6.dp));entry.items.forEach{item->Row(Modifier.padding(bottom=6.dp)){Text("•",Modifier.width(16.dp),fontSize=14.sp,lineHeight=20.3.sp);Text(item,Modifier.weight(1f),fontSize=14.sp,lineHeight=20.3.sp)}}}}}
 }

                        "Sichtbare Bereiche"->ReferenceSummary{val tabs=listOf("buchungen" to "Buchungen","variabel" to "Variable Kosten","kredite" to "Kredite","tanken" to "Tanken","beta" to "Kontostand");fun setVisible(key:String,v:Boolean){val visibleCount=tabs.count{(k,_)->if(tabVisibility.has(k))tabVisibility.optBoolean(k,true) else true};if(!v&&visibleCount<=1){visibilityWarning="Mindestens ein Reiter muss sichtbar bleiben.";return};visibilityWarning="";tabVisibility.put(key,v);repo.setSetting("household-tab-visibility",tabVisibility.toString());onVisibilityChanged()};tabs.forEach{(key,label)->val checked=if(tabVisibility.has(key))tabVisibility.optBoolean(key,true) else true;Row(Modifier.fillMaxWidth().clickable{setVisible(key,!checked)}.padding(horizontal=2.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),fontSize=15.sp);ReferenceToggle(checked){v->setVisible(key,v)}}};if(visibilityWarning.isNotBlank())ReferenceMessage(visibilityWarning,icon="⚠️",onDismiss={visibilityWarning=""})}
                        else->Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}
@Composable private fun SettingsRow(title:String,sub:String,divider:Boolean=true,onClick:()->Unit){
    Column(Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=2.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.primary,modifier=Modifier.size(32.dp)){Box(contentAlignment=Alignment.Center){HtmlReferenceIcon("settings-"+title,Color.White,22.dp)}}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)){Text(title,fontSize=16.sp,fontWeight=FontWeight.Bold);Text(sub,Modifier.padding(top=2.dp),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Text("›",fontSize=22.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(divider)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable private fun OriginalTabIcon(tab:Tab,color:Color,active:Boolean){HtmlReferenceIcon(when(tab){Tab.BALANCE->"beta";Tab.BOOKINGS->"buchungen";Tab.VARIABLE->"variabel";Tab.LOANS->"kredite";Tab.FUEL->"tanken"},color,16.dp,if(active)2.6f else 2f)}
@Composable private fun SettingsGearIcon(color:Color){HtmlReferenceIcon("gear",color,20.dp)}

@Composable private fun OriginalCompactRow(name:String,amount:Double,income:Boolean,sub:String?=null,tint:Color=Color.Transparent,leading:(@Composable ()->Unit)?=null){val Green=appGreen(LocalAppDark.current);
    Row(Modifier.fillMaxWidth().background(tint).padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
        if(leading!=null){leading();Spacer(Modifier.width(10.dp))}
        Column(Modifier.weight(1f)){Text(name,fontSize=14.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);if(!sub.isNullOrBlank()){Spacer(Modifier.height(2.dp));Text(sub,fontSize=11.5.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
        Spacer(Modifier.width(10.dp));Text(money(amount),fontSize=17.sp,fontWeight=FontWeight.ExtraBold,color=if(income)Green else MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
@Composable private fun InvoiceCompactRow(o:JSONObject){
    val d=runCatching{LocalDate.parse(o.optString("dueDate"))}.getOrNull();val today=LocalDate.now();val overdue=d?.isBefore(today)==true
    val dueThisMonth=d?.let{it.month==today.month&&it.year==today.year}==true
    val sub=if(d==null)"" else if(overdue)"Überfällig seit "+d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) else "Fällig am "+d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val tint=if(dueThisMonth)appExpenseSoft(LocalAppDark.current) else appSavingsSoft(LocalAppDark.current)
    OriginalCompactRow(o.optString("name"),o.optDouble("amount"),false,sub,tint){
        Box(Modifier.size(24.dp).border(2.dp,Color(0xFFDADADE),CircleShape).background(MaterialTheme.colorScheme.surface,CircleShape))
    }
}


// CSS linear-gradient(145deg): project the card bounds onto the gradient axis.
private fun Modifier.htmlGradient(colors:List<Color>)=drawWithCache{
 val direction=Offset(.57357644f,.81915204f)
 val halfLength=(size.width*direction.x+size.height*direction.y)/2f
 val center=Offset(size.width/2f,size.height/2f)
 val brush=Brush.linearGradient(colors,start=center-direction*halfLength,end=center+direction*halfLength)
 onDrawBehind{drawRect(brush)}
}
