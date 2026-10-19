/* Haushaltsbuch+: Monatsauswertung. Keine Buchungen werden verändert. */
(function(){
  'use strict';
  if(window.__householdAiMonthlyInstalled) return;
  window.__householdAiMonthlyInstalled=true;
  var REPORT_KEY='household-ai-reports-v1';
  var DATA_KEY='household-data-v8';
  var monthNames=['Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'];
  var activeKey='',currentReport=null,working=false,requestNumber=0;

  function storageGet(key){
    try{if(window.AndroidBridge&&window.AndroidBridge.nativeGetItem)return window.AndroidBridge.nativeGetItem(key);}
    catch(e){}
    try{return localStorage.getItem(key);}catch(e){return null;}
  }
  function storageSet(key,val){
    try{if(window.AndroidBridge&&window.AndroidBridge.nativeSetItem){window.AndroidBridge.nativeSetItem(key,val);return;}}
    catch(e){}
    try{localStorage.setItem(key,val);}catch(e){}
  }
  function escapeHtml(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
  function euros(n){return Number(n||0).toLocaleString('de-DE',{style:'currency',currency:'EUR'});}
  function getEntries(){
    try{
      var raw=storageGet(DATA_KEY),s=raw?JSON.parse(raw):{};
      return Array.isArray(s.betaEntries)?s.betaEntries.filter(function(e){
        return e&&/^\d{4}-\d{2}-\d{2}$/.test(e.date||'')&&isFinite(Number(e.amount))&&Number(e.amount)>=0;
      }):[];
    }catch(e){return [];}
  }
  function prevKey(k){
    var d=new Date(Number(k.slice(0,4)),Number(k.slice(5,7))-2,1);
    return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
  }
  function keyLabel(k){return monthNames[Number(k.slice(5,7))-1]+' '+k.slice(0,4);}
  function currentMonthKey(){
    var now=new Date();return now.getFullYear()+'-'+String(now.getMonth()+1).padStart(2,'0');
  }
  function monthOptions(){
    var current=currentMonthKey(), keys={};
    var d=new Date();for(var i=0;i<18;i++){keys[d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')]=1;d.setDate(1);d.setMonth(d.getMonth()-1);}
    getEntries().forEach(function(e){if(e.date.slice(0,7)<=current)keys[e.date.slice(0,7)]=1;});
    return Object.keys(keys).filter(function(k){return k<=current;}).sort().reverse();
  }
  function categoryFor(e){
    var explicit=String(e.category||'').trim();
    if(explicit&&explicit.toLowerCase()!=='sonstiges'&&explicit.toLowerCase()!=='keine')return explicit;
    var t=String(e.title||'').toLowerCase();
    if(/\b(rewe|edeka|lidl|aldi|kaufland|netto|penny|tegut|norma|globus|lebensmittel|supermarkt|dm|rossmann)\b/.test(t))return 'Lebensmittel & Drogerie';
    if(/\b(aral|shell|esso|jet tank|tanken|tankstelle|diesel|benzin|kfz|auto|peugeot|werkstatt|reifen|tüv|hu au|versicherung kfz|parken|parkhaus|bahn|db ticket)\b/.test(t))return 'Mobilität & Auto';
    if(/\b(netflix|spotify|disney|prime video|youtube premium|dazn|apple music|abo|abonnement)\b/.test(t))return 'Abos & Medien';
    if(/\b(mcdonald|burger king|restaurant|pizza|döner|lieferando|subway|café|cafe|bäcker|bäckerei)\b/.test(t))return 'Essen außer Haus';
    if(/\b(miete|nebenkosten|strom|gasabschlag|heizung|wasser|internet|telefon|mobilfunk|rundfunk|gez)\b/.test(t))return 'Wohnen & Verträge';
    if(/\b(apotheke|arzt|zahnarzt|medizin|rezept|krankenhaus)\b/.test(t))return 'Gesundheit';
    if(/\b(kino|konzert|ausflug|freizeit|spiel|steam|playstation|xbox)\b/.test(t))return 'Freizeit';
    return 'Nicht eindeutig';
  }
  function sumEntries(entries){
    return entries.reduce(function(acc,e){var val=Number(e.amount)||0;if(e.type==='einnahme')acc.income+=val;else acc.expense+=val;return acc;},{income:0,expense:0});
  }
  function groupsFor(entries){
    var groups={};
    entries.forEach(function(e){
      if(e.type==='einnahme')return;
      var cat=categoryFor(e);
      if(!groups[cat])groups[cat]={name:cat,amount:0,count:0,titles:[]};
      groups[cat].amount+=Number(e.amount)||0;groups[cat].count++;
      if(groups[cat].titles.indexOf(e.title)===-1&&groups[cat].titles.length<5)groups[cat].titles.push(String(e.title||'Ohne Titel'));
    });
    return Object.keys(groups).map(function(k){return groups[k];}).sort(function(a,b){return b.amount-a.amount;});
  }
  function hashEntries(entries){
    var data=entries.map(function(e){return [e.id,e.date,e.title,e.type,e.amount,e.category].join('|');}).sort().join('\n');
    var h=2166136261;for(var i=0;i<data.length;i++){h^=data.charCodeAt(i);h=Math.imul(h,16777619);}
    return (h>>>0).toString(16)+':'+entries.length;
  }
  function compute(k){
    var all=getEntries(),cur=all.filter(function(e){return e.date.slice(0,7)===k;}),prev=all.filter(function(e){return e.date.slice(0,7)===prevKey(k);});
    var sums=sumEntries(cur),older=sumEntries(prev);
    var balance=all.filter(function(e){return e.date.slice(0,7)<=k;}).reduce(function(acc,e){return acc+(e.type==='einnahme'?1:-1)*Number(e.amount);},0);
    var categories=groupsFor(cur),prevCategories=groupsFor(prev);
    var expenseDelta=sums.expense-older.expense;
    return {key:k,entries:cur,previousEntries:prev,hash:hashEntries(cur.concat(prev)),
      income:sums.income,expense:sums.expense,remaining:sums.income-sums.expense,balance:balance,
      previousIncome:older.income,previousExpense:older.expense,expenseDelta:expenseDelta,
      categories:categories,previousCategories:prevCategories,previousCount:prev.length};
  }
  function baseAnalysis(m){
    var good=[],notice=[],tips=[],prev=m.previousCount>0;
    if(!m.entries.length){return {good:['Für diesen Monat sind noch keine Kontostand-Buchungen vorhanden.'],notice:['Ohne Einträge ist keine sinnvolle Bewertung möglich.'],tips:['Trage Einnahmen und Ausgaben für diesen Monat ein und starte die Auswertung erneut.']};}
    if(m.remaining>0)good.push('Du hast '+euros(m.remaining)+' mehr eingenommen als ausgegeben.');
    else if(m.remaining<0)notice.push('Deine Ausgaben lagen '+euros(Math.abs(m.remaining))+' über deinen Einnahmen. Prüfe größere Einmalzahlungen und wiederkehrende Kosten.');
    else good.push('Einnahmen und Ausgaben waren in diesem Monat ausgeglichen.');
    if(prev){
      if(m.expenseDelta < -0.01)good.push('Gegenüber '+keyLabel(prevKey(m.key))+' sind die erfassten Ausgaben um '+euros(-m.expenseDelta)+' gesunken.');
      else if(m.expenseDelta > 0.01)notice.push('Die erfassten Ausgaben sind gegenüber dem Vormonat um '+euros(m.expenseDelta)+' gestiegen. Das muss nicht schlecht sein – einmalige notwendige Käufe können die Ursache sein.');
    }else notice.push('Für den Vormonat fehlen Vergleichsbuchungen. Ein Monatsvergleich ist noch nicht zuverlässig möglich.');
    if(m.categories.length){
      var largest=m.categories[0];
      notice.push('Größter erfasster Ausgabenbereich: '+largest.name+' mit '+euros(largest.amount)+' ('+largest.count+' Buchungen).');
      var ambiguous=m.categories.filter(function(c){return c.name==='Nicht eindeutig';})[0];
      if(ambiguous){notice.push(euros(ambiguous.amount)+' lassen sich anhand der Titel nicht eindeutig zuordnen. Die Ausgaben sind deshalb nicht automatisch unnötig.');}
      var subs=m.categories.filter(function(c){return /abo|medien/i.test(c.name);})[0];
      if(subs){tips.push('Prüfe, ob du alle Abos und Medienangebote regelmäßig nutzt ('+euros(subs.amount)+' erfasst).');}
      if(prev){
        var prevMap={};m.previousCategories.forEach(function(g){prevMap[g.name]=g.amount;});
        var increases=m.categories.map(function(g){return {name:g.name,delta:g.amount-(prevMap[g.name]||0)};}).filter(function(g){return g.delta>15;}).sort(function(a,b){return b.delta-a.delta;});
        if(increases.length)tips.push('Vergleiche besonders '+increases[0].name+': hier liegen die erfassten Ausgaben '+euros(increases[0].delta)+' über dem Vormonat.');
      }
    }
    if(!tips.length)tips.push('Behalte größere Einzelbuchungen und wiederkehrende Kosten im Blick. Eine höhere Ausgabe ist nicht automatisch vermeidbar.');
    return {good:good,notice:notice,tips:tips};
  }
  function reports(){try{return JSON.parse(storageGet(REPORT_KEY)||'{}')||{};}catch(e){return {};}}
  function saveReport(report){var data=reports();data[report.key]=report;storageSet(REPORT_KEY,JSON.stringify(data));}
  function showFeedback(msg,isError){var el=document.getElementById('aiFeedback');el.textContent=msg||'';el.style.color=isError?'var(--expense)':'var(--ink-soft)';}
  function installAvailable(){return !!(window.LocalAiBridge&&typeof window.LocalAiBridge.importModel==='function');}
  function hasModel(){try{return installAvailable()&&!!window.LocalAiBridge.hasModel();}catch(e){return false;}}
  function setBusy(val){working=val;['aiGenerate','aiBaseGenerate','aiImportModel','aiMonthSelect'].forEach(function(id){var el=document.getElementById(id);if(el)el.disabled=val;});}
  function renderReport(report){
    var target=document.getElementById('aiReport');target.innerHTML='';
    if(!report){target.style.display='none';return;}
    target.style.display='block';
    var metrics=document.createElement('div');metrics.className='ai-metrics';
    [['Einnahmen',euros(report.income),'positive'],['Ausgaben',euros(report.expense),'negative'],['Monatlicher Rest',euros(report.remaining),report.remaining>=0?'positive':'negative'],['Kontostand Monatsende',euros(report.balance),report.balance>=0?'positive':'negative']].forEach(function(m){var card=document.createElement('div');card.className='ai-metric';card.innerHTML='<span>'+m[0]+'</span><strong class="'+m[2]+'">'+escapeHtml(m[1])+'</strong>';metrics.appendChild(card);});
    target.appendChild(metrics);
    var label=document.createElement('div');label.className='ai-report-label';
    label.textContent=report.mode==='gemma'?'Mit lokalem Sprachmodell erstellt · '+keyLabel(report.key):'Zahlenbasierte Auswertung · '+keyLabel(report.key);
    target.appendChild(label);
    var sections=report.mode==='gemma'?[{title:'Deine Monatsanalyse',items:[report.text||'Keine Antwort vom Modell.']}]:[
      {title:'Das lief gut',items:report.good,kind:'good'},
      {title:'Das ist aufgefallen',items:report.notice,kind:'notice'},
      {title:'Deine Tipps',items:report.tips,kind:'tip'}
    ];
    sections.forEach(function(section){
      var card=document.createElement('section');card.className='ai-section '+(section.kind||'');
      var h=document.createElement('h3');h.textContent=section.title;card.appendChild(h);
      section.items.forEach(function(str){var p=document.createElement('p');p.textContent=str;card.appendChild(p);});
      target.appendChild(card);
    });
    if(report.categories&&report.categories.length){
      var area=document.createElement('section');area.className='ai-section';
      area.innerHTML='<h3>Ausgaben nach Bereichen</h3>';
      report.categories.forEach(function(c){var p=document.createElement('div');p.className='ai-cat';var n=document.createElement('span');n.textContent=c.name;var val=document.createElement('strong');val.textContent=euros(c.amount);p.appendChild(n);p.appendChild(val);area.appendChild(p);});
      target.appendChild(area);
    }
    var note=document.createElement('p');note.className='ai-disclaimer';
    note.textContent='Grundlage sind ausschließlich die gespeicherten Kontostand-Buchungen. Kategorien aus Titeln sind Schätzungen. Beträge werden von der App berechnet, nicht vom Sprachmodell.';
    target.appendChild(note);
  }
  function reportFrom(m,mode,text){
    var base=baseAnalysis(m);
    return {key:m.key,hash:m.hash,mode:mode,generatedAt:new Date().toISOString(),
      income:m.income,expense:m.expense,remaining:m.remaining,balance:m.balance,categories:m.categories,
      good:base.good,notice:base.notice,tips:base.tips,text:text||''};
  }
  function refreshReport(){
    var m=compute(activeKey),cached=reports()[activeKey];
    currentReport=cached||null;renderReport(currentReport);
    if(cached&&cached.hash!==m.hash)showFeedback('Seit diesem Bericht wurden Buchungen geändert. Bitte eine neue Auswertung erstellen.',false);
    else showFeedback(cached?'Gespeicherter Bericht vom '+new Date(cached.generatedAt).toLocaleDateString('de-DE')+'.':'',false);
  }
  function refreshModel(){
    var el=document.getElementById('aiModelStatus'),button=document.getElementById('aiGenerate'),importBtn=document.getElementById('aiImportModel');
    if(hasModel()){
      el.textContent='Gemma-Modell auf diesem Gerät vorhanden · Analyse funktioniert offline.';
      button.style.display='block';importBtn.textContent='Modell ersetzen';
    }else{
      el.textContent=installAvailable()?'Noch kein lokales Gemma-Modell importiert. Für die echte KI-Analyse wird einmalig eine .litertlm-Datei benötigt.':'Sprachmodell ist in dieser App-Version nicht verfügbar. Die zahlenbasierte Auswertung funktioniert offline.';
      button.style.display='none';importBtn.style.display=installAvailable()?'block':'none';
    }
  }
  function open(){
    var options=monthOptions(),wanted=prevKey(currentMonthKey());
    activeKey=options.indexOf(wanted)>-1?wanted:(options[0]||currentMonthKey());
    var sel=document.getElementById('aiMonthSelect');sel.innerHTML='';
    options.forEach(function(k){var el=document.createElement('option');el.value=k;el.textContent=keyLabel(k);sel.appendChild(el);});
    sel.value=activeKey;
    document.getElementById('aiMonthlyOverlay').classList.add('open');
    refreshModel();refreshReport();
  }
  function close(){
    if(working)return;
    document.getElementById('aiMonthlyOverlay').classList.remove('open');
  }
  function makePrompt(m){
    var recent=m.entries.map(function(e){return {datum:e.date,titel:String(e.title||'').slice(0,75),betrag:Number(e.amount),art:e.type==='einnahme'?'Einnahme':'Ausgabe',kategorie:categoryFor(e)};});
    var previous=m.previousCategories.map(function(c){return {bereich:c.name,ausgaben:Number(c.amount.toFixed(2))};});
    var summary={monat:keyLabel(m.key),einnahmen:m.income,ausgaben:m.expense,rest:m.remaining,kontostand_monatsende:m.balance,
      ausgaben_vormonat:m.previousCount?m.previousExpense:null,kategorien:m.categories,vorherige_kategorien:previous,buchungen:recent.slice(0,110),
      ausgelassene_buchungen:Math.max(0,recent.length-110)};
    return 'Du bist ein sachlicher, respektvoller deutscher Finanzassistent. Schreibe einen persönlichen Monatsrückblick in verständlichem Deutsch, etwa 180 bis 260 Wörter. '+
      'Strukturiere ihn mit \"Das lief gut\", \"Das ist aufgefallen\" und \"Mein Tipp\". '+
      'Nutze AUSSCHLIESSLICH die folgenden berechneten Summen und Buchungen; rechne keine neuen Summen im Kopf. '+
      'Bewerte notwendige Ausgaben nicht pauschal negativ. Aus unklaren Titeln (z.B. Amazon) darfst du den konkreten Kauf NICHT erraten. '+
      'Falls Daten fehlen, benenne die Unsicherheit. Keine erfundenen Werte, keine Anlageberatung. '+
      'Alle Werte sind in Euro. Daten als JSON:\n'+JSON.stringify(summary);
  }
  function generateBase(){
    var m=compute(activeKey);
    if(!m.entries.length){showFeedback('Für '+keyLabel(activeKey)+' sind keine Kontostand-Einträge vorhanden.',true);return;}
    var r=reportFrom(m,'basis');saveReport(r);currentReport=r;renderReport(r);
    showFeedback('Auswertung auf diesem Gerät erstellt und gespeichert – ohne Sprachmodell.',false);
  }
  function generateGemma(){
    if(working)return;
    if(!hasModel()){showFeedback('Bitte zuerst eine .litertlm-Modell-Datei importieren.',true);return;}
    var m=compute(activeKey);
    if(!m.entries.length){showFeedback('Für diesen Monat sind keine Einträge vorhanden.',true);return;}
    var id='ai-'+(++requestNumber),key=activeKey,hash=m.hash;
    window.__aiPending={id:id,key:key,hash:hash};
    setBusy(true);showFeedback('Gemma wird nur für diese Auswertung geladen. Bitte App geöffnet lassen …',false);
    try{window.LocalAiBridge.generate(makePrompt(m),id);}
    catch(e){window.__aiPending=null;setBusy(false);showFeedback('Lokale KI konnte nicht gestartet werden: '+e.message,true);}
  }
  window.__aiLocalResult=function(id,response,error){
    var req=window.__aiPending;if(!req||req.id!==id)return;
    window.__aiPending=null;setBusy(false);
    if(error){showFeedback('Die lokale KI konnte nicht antworten: '+error,true);return;}
    var m=compute(req.key);
    if(m.hash!==req.hash){showFeedback('Buchungen haben sich während der Analyse geändert. Bitte erneut starten.',true);return;}
    var responseText=String(response||'').trim();
    if(!responseText){showFeedback('Das lokale Modell hat keinen Bericht zurückgegeben.',true);return;}
    var report=reportFrom(m,'gemma',responseText);saveReport(report);
    if(activeKey===req.key){currentReport=report;renderReport(report);}
    showFeedback('Lokale KI-Auswertung erstellt und gespeichert. Das Modell wurde aus dem RAM entladen.',false);
  };
  window.__aiModelEvent=function(status,message){
    if(status==='done'){refreshModel();showFeedback('Lokales KI-Modell erfolgreich eingerichtet.',false);}
    else if(status==='progress'){showFeedback('Modell wird kopiert: '+message,false);}
    else if(status==='error'){refreshModel();showFeedback('Modellimport fehlgeschlagen: '+message,true);}
    else if(status==='cancel'){refreshModel();showFeedback('Modellimport abgebrochen.',false);}
  };
  function init(){
    var settings=document.getElementById('settingsIconBtn');if(!settings)return;
    var ai=document.createElement('button');ai.type='button';ai.id='aiHeaderBtn';ai.className='settings-icon-btn';ai.setAttribute('aria-label','KI-Monatsauswertung');ai.setAttribute('title','KI-Monatsauswertung');
    ai.innerHTML='<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7z"/><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"/><path d="M3 3v4M1 5h4"/></svg>';
    settings.parentNode.insertBefore(ai,settings);
    var css=document.createElement('style');
    css.textContent='#aiHeaderBtn{color:var(--primary)} #aiHeaderBtn:active{transform:scale(.96)}'+
      '.ai-overlay{position:fixed;inset:0;z-index:10020;background:rgba(0,0,0,.52);display:none;align-items:center;justify-content:center;padding:16px;box-sizing:border-box;overflow:hidden;}'+
      '.ai-overlay.open{display:flex} .ai-panel{background:var(--surface-solid);color:var(--ink);border:1px solid var(--rule);border-radius:22px;width:100%;max-width:490px;max-height:calc(100dvh - 42px);display:flex;flex-direction:column;box-shadow:0 16px 50px rgba(0,0,0,.24);overflow:hidden;}'+
      '.ai-head{padding:19px 19px 12px;display:flex;align-items:center;justify-content:space-between;gap:10px;border-bottom:1px solid var(--rule)}'+
      '.ai-head h2{margin:0;font-size:20px;font-weight:800}.ai-close{background:var(--surface-2);border:0;border-radius:50%;width:36px;height:36px;color:var(--ink);font-size:24px;cursor:pointer}'+
      '.ai-body{overflow:auto;overscroll-behavior:contain;padding:18px;display:flex;flex-direction:column;gap:13px}'+
      '.ai-body select{width:100%;background:var(--surface-2);color:var(--ink);border:1px solid var(--rule-strong);border-radius:13px;padding:12px;font-size:15px;box-sizing:border-box}'+
      '.ai-intro{border-radius:15px;background:var(--primary-soft);padding:13px;font-size:13px;color:var(--ink);line-height:1.55}'+
      '.ai-box{border:1px solid var(--rule);background:var(--surface);border-radius:16px;padding:14px} .ai-box h3{font-size:15px;margin:0 0 9px}'+
      '.ai-action{width:100%;border:0;border-radius:13px;background:var(--primary);color:white;padding:13px;font-size:14px;font-weight:750;cursor:pointer}'+
      '.ai-action.secondary{background:var(--primary-soft);color:var(--primary);border:1px solid var(--primary)}.ai-action:disabled{opacity:.5;cursor:wait}'+
      '.ai-small{color:var(--ink-soft);font-size:12px;line-height:1.55;margin:6px 0 0} #aiFeedback{font-size:12px;min-height:18px;line-height:1.5}'+
      '.ai-metrics{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px;margin-bottom:10px}'+
      '.ai-metric{background:var(--surface-2);padding:11px;border-radius:12px;display:flex;flex-direction:column;gap:5px;min-width:0}'+
      '.ai-metric span{font-size:11px;color:var(--ink-soft)}.ai-metric strong{font-size:15px;overflow-wrap:anywhere}'+
      '.ai-metric .positive{color:var(--savings)}.ai-metric .negative{color:var(--expense)}'+
      '.ai-report-label{font-size:11px;font-weight:650;color:var(--ink-soft);margin:3px 0 7px}'+
      '.ai-section{border-radius:14px;background:var(--surface);padding:13px;margin:8px 0}'+
      '.ai-section.good{border-left:3px solid var(--savings)}.ai-section.notice{border-left:3px solid var(--expense)}.ai-section.tip{border-left:3px solid var(--primary)}'+
      '.ai-section h3{margin:0 0 8px;font-size:14px}.ai-section p{white-space:pre-wrap;font-size:13px;line-height:1.65;margin:8px 0}'+
      '.ai-cat{display:flex;align-items:center;justify-content:space-between;gap:9px;padding:8px 0;border-bottom:1px solid var(--rule);font-size:12px}.ai-cat:last-child{border:0}'+
      '.ai-disclaimer{font-size:11px;line-height:1.5;color:var(--ink-soft)}';
    document.head.appendChild(css);
    var root=document.createElement('div');root.id='aiMonthlyOverlay';root.className='ai-overlay';
    root.innerHTML='<div class="ai-panel" role="dialog" aria-modal="true" aria-labelledby="aiTitle"><div class="ai-head"><h2 id="aiTitle">✦ KI-Monatsauswertung</h2><button type="button" class="ai-close" id="aiClose" aria-label="Schließen">×</button></div>'+
      '<div class="ai-body"><label for="aiMonthSelect" style="font-weight:700;font-size:14px">Monat auswählen</label><select id="aiMonthSelect"></select>'+
      '<div class="ai-intro">Dein Finanzrückblick bewertet die tatsächlichen Kontostand-Einträge und vergleicht sie mit dem Vormonat. Die Beträge berechnet immer das Haushaltsbuch.</div>'+
      '<button type="button" id="aiBaseGenerate" class="ai-action secondary">Zahlenbasierte Auswertung erstellen</button>'+
      '<div class="ai-box"><h3>✦ Lokale KI mit Gemma</h3><p id="aiModelStatus" class="ai-small"></p>'+
      '<button type="button" id="aiGetModel" class="ai-action secondary" style="margin-bottom:8px">Offizielle Modell-Datei herunterladen</button>'+
      '<button type="button" id="aiImportModel" class="ai-action secondary">Gemma-Modell importieren</button>'+
      '<p class="ai-small">Importiere einmalig die offizielle Gemma-4-E2B-Datei im Format .litertlm (ca. 2,6 GB). Die KI läuft danach nur auf Knopfdruck, offline auf diesem Gerät.</p>'+
      '<button type="button" id="aiGenerate" class="ai-action" style="margin-top:10px">Mit Gemma 4 analysieren</button></div>'+
      '<div id="aiFeedback" role="status" aria-live="polite"></div><div id="aiReport" style="display:none"></div></div></div>';
    document.body.appendChild(root);
    ai.addEventListener('click',open);
    document.getElementById('aiClose').addEventListener('click',close);
    root.addEventListener('click',function(e){if(e.target===root)close();});
    document.getElementById('aiMonthSelect').addEventListener('change',function(e){activeKey=e.target.value;refreshReport();});
    document.getElementById('aiBaseGenerate').addEventListener('click',generateBase);
    document.getElementById('aiGenerate').addEventListener('click',generateGemma);
    document.getElementById('aiGetModel').addEventListener('click',function(){
      try{window.LocalAiBridge.openModelDownloadPage();}
      catch(e){showFeedback('Bitte Modell-Datei auf Hugging Face herunterladen.',true);}
    });
    document.getElementById('aiImportModel').addEventListener('click',function(){
      try{window.LocalAiBridge.importModel();showFeedback('Wähle die heruntergeladene .litertlm-Modell-Datei aus.',false);}
      catch(e){showFeedback('Modellimport ist nicht verfügbar.',true);}
    });
    document.addEventListener('keydown',function(e){if(e.key==='Escape'&&root.classList.contains('open'))close();});
    var oldClose=window.closeAnyOpenOverlay;
    window.closeAnyOpenOverlay=function(){if(root.classList.contains('open')){if(!working)close();return true;}return typeof oldClose==='function'?oldClose():false;};
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();