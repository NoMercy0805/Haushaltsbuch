/* Haushaltsbuch+: Monatsauswertung. Keine Buchungen werden verändert. */
(function(){
  'use strict';
  if(window.__householdAiMonthlyInstalled) return;
  window.__householdAiMonthlyInstalled=true;
  var REPORT_KEY='household-monthly-reports-v2';
  var DATA_KEY='household-data-v8';
  var monthNames=['Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'];
  var activeKey='';

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
    var beforePrev=prevKey(prevKey(k));
    var history=all.filter(function(e){return e.date.slice(0,7)<=k&&e.date.slice(0,7)>=beforePrev;});
    var sums=sumEntries(cur),older=sumEntries(prev);
    var balance=all.filter(function(e){return e.date.slice(0,7)<=k;}).reduce(function(acc,e){return acc+(e.type==='einnahme'?1:-1)*Number(e.amount);},0);
    var categories=groupsFor(cur),prevCategories=groupsFor(prev);
    return {key:k,entries:cur,previousEntries:prev,history:history,
      hash:hashEntries(history),income:sums.income,expense:sums.expense,
      remaining:sums.income-sums.expense,balance:balance,
      previousIncome:older.income,previousExpense:older.expense,
      expenseDelta:sums.expense-older.expense,incomeDelta:sums.income-older.income,
      categories:categories,previousCategories:prevCategories,previousCount:prev.length};
  }
  function byTitle(entries){
    var groups={};
    entries.forEach(function(e){
      if(e.type==='einnahme')return;
      var title=String(e.title||'Ohne Titel').replace(/\s+/g,' ').trim();
      var key=title.toLocaleLowerCase('de-DE');
      if(!groups[key])groups[key]={title:title,amount:0,count:0};
      groups[key].amount+=Number(e.amount);groups[key].count++;
    });
    return Object.keys(groups).map(function(k){return groups[k];}).sort(function(a,b){return b.amount-a.amount;});
  }
  function recurringTitles(m){
    var seen={};
    m.history.forEach(function(e){
      if(e.type==='einnahme')return;
      var title=String(e.title||'').replace(/\s+/g,' ').trim();
      if(!title)return;
      var key=title.toLocaleLowerCase('de-DE');
      if(!seen[key])seen[key]={title:title,months:{},thisAmount:0,thisCount:0};
      seen[key].months[e.date.slice(0,7)]=true;
      if(e.date.slice(0,7)===m.key){seen[key].thisAmount+=Number(e.amount);seen[key].thisCount++;}
    });
    return Object.keys(seen).map(function(k){var e=seen[k];return {title:e.title,monthCount:Object.keys(e.months).length,amount:e.thisAmount,count:e.thisCount};})
      .filter(function(e){return e.monthCount>=2&&e.thisCount>0;})
      .sort(function(a,b){return b.amount-a.amount;}).slice(0,6);
  }
  function shortDate(iso){var parts=String(iso||'').split('-');return parts.length===3?parts[2]+'.'+parts[1]+'.'+parts[0]:iso;}
  function pctChange(diff,base){
    if(base<=0)return '';
    var value=Math.abs(diff/base*100);
    return ' ('+value.toLocaleString('de-DE',{maximumFractionDigits:1})+' %)';
  }
  function baseAnalysis(m){
    var good=[],notice=[],tips=[],prev=m.previousCount>0,ongoing=m.key===currentMonthKey();
    var largest=m.entries.filter(function(e){return e.type!=='einnahme';})
      .slice().sort(function(a,b){return Number(b.amount)-Number(a.amount);}).slice(0,5)
      .map(function(e){return {title:String(e.title||'Ohne Titel'),amount:Number(e.amount),date:e.date};});
    var merchants=byTitle(m.entries).slice(0,6);
    var regular=recurringTitles(m);
    if(!m.entries.length) return {good:['Keine erfassten Buchungen in diesem Monat.'],notice:[],tips:['Trage zunächst Buchungen im Kontostand ein.'],largest:[],merchants:[],regular:[]};
    if(m.remaining>0){
      good.push('Der erfasste Monatsüberschuss beträgt '+euros(m.remaining)+'.'+(m.income>0?' Das sind '+(100*m.remaining/m.income).toLocaleString('de-DE',{maximumFractionDigits:1})+' % deiner erfassten Einnahmen.':''));
    } else if(m.remaining<0){
      notice.push('Die Ausgaben übersteigen die Einnahmen im betrachteten Monat um '+euros(-m.remaining)+'. Das kann auch an größeren notwendigen Einmalausgaben liegen.');
    } else good.push('Einnahmen und Ausgaben sind im betrachteten Monat ausgeglichen.');
    if(ongoing)notice.push('Der ausgewählte Monat läuft noch: Der Vergleich zum vollständigen Vormonat ist vorläufig.');
    if(prev){
      var d=m.expenseDelta;
      if(d < -0.005)good.push('Die erfassten Ausgaben liegen '+euros(-d)+pctChange(d,m.previousExpense)+' unter '+keyLabel(prevKey(m.key))+'.');
      else if(d > 0.005)notice.push('Die erfassten Ausgaben liegen '+euros(d)+pctChange(d,m.previousExpense)+' über '+keyLabel(prevKey(m.key))+'.');
      if(m.incomeDelta>0.005)good.push('Die Einnahmen sind gegenüber dem Vormonat um '+euros(m.incomeDelta)+' gestiegen.');
      else if(m.incomeDelta< -0.005)notice.push('Die Einnahmen liegen '+euros(-m.incomeDelta)+' unter dem Vormonat.');
    }else notice.push('Für '+keyLabel(prevKey(m.key))+' liegen keine Buchungen vor; Trends zum Vormonat sind nicht belastbar.');
    if(m.balance<0)notice.push('Der errechnete Kontostand zum Monatsende ist mit '+euros(m.balance)+' negativ. Wichtig ist auch, welche Zahlungen in den nächsten Wochen anstehen.');
    else if(m.balance>0)good.push('Der errechnete Kontostand zum Monatsende liegt bei '+euros(m.balance)+'.');
    if(m.categories.length){
      var top=m.categories[0];
      notice.push('Der größte Ausgabenbereich ist '+top.name+' mit '+euros(top.amount)+(m.expense>0?' ('+(top.amount/m.expense*100).toLocaleString('de-DE',{maximumFractionDigits:1})+' % aller Ausgaben).':'.'));
      var unknown=m.categories.filter(function(g){return g.name==='Nicht eindeutig';})[0];
      if(unknown && unknown.amount>0){notice.push('Ausgaben von '+euros(unknown.amount)+' lassen sich nicht sicher aus Titel oder Kategorie einordnen. Sie werden nicht automatisch als vermeidbar bewertet.');}
      if(prev){
        var previousAmounts={};m.previousCategories.forEach(function(g){previousAmounts[g.name]=g.amount;});
        var changes=m.categories.map(function(g){return {name:g.name,amount:g.amount,previous:previousAmounts[g.name]||0,delta:g.amount-(previousAmounts[g.name]||0)};})
          .filter(function(g){return g.previous>0&&g.delta>=20;}).sort(function(a,b){return b.delta-a.delta;});
        if(changes.length){
          var rise=changes[0];
          notice.push('Besonders gestiegen ist '+rise.name+': '+euros(rise.amount)+' statt '+euros(rise.previous)+' im Vormonat (+'+euros(rise.delta)+').');
          tips.push('Prüfe die Buchungen im Bereich '+rise.name+'. Der Anstieg muss nicht unnötig gewesen sein.');
        }
        var falling=m.categories.map(function(g){return {name:g.name,amount:g.amount,delta:g.amount-(previousAmounts[g.name]||0),previous:previousAmounts[g.name]||0};})
          .filter(function(g){return g.previous>0&&g.delta<=-20;}).sort(function(a,b){return a.delta-b.delta;});
        if(falling.length)good.push('Im Bereich '+falling[0].name+' wurden '+euros(-falling[0].delta)+' weniger erfasst als im Vormonat.');
      }
    }
    if(largest.length && largest[0].amount>=Math.max(100,m.expense*0.15))notice.push('Größte Einzelbuchung: '+largest[0].title+' am '+shortDate(largest[0].date)+' mit '+euros(largest[0].amount)+'. Aus dem Titel allein lässt sich nicht beurteilen, ob sie notwendig war.');
    if(regular.length){
      tips.push('Möglicherweise wiederkehrend: '+regular.slice(0,3).map(function(x){return x.title;}).join(', ')+'. Diese Titel erscheinen in mindestens zwei der letzten drei Monate – prüfe, ob alle Zahlungen gewollt sind.');
    }
    if(m.remaining<0)tips.push('Sieh dir zuerst größere Einzelbeträge und wiederkehrende Verpflichtungen an, statt pauschal bei Lebensmitteln oder notwendigen Ausgaben zu sparen.');
    if(!tips.length)tips.push('Vergleiche ähnliche Monate und behalte besonders größere Ausgaben im Blick. Auch unregelmäßige Zahlungen sollten realistisch eingeplant sein.');
    return {good:good,notice:notice,tips:tips,largest:largest,merchants:merchants,regular:regular};
  }
  function reports(){try{return JSON.parse(storageGet(REPORT_KEY)||'{}')||{};}catch(e){return {};}}
  function saveReport(report){var data=reports();data[report.key]=report;storageSet(REPORT_KEY,JSON.stringify(data));}
  function showFeedback(msg,isError){
    var el=document.getElementById('aiFeedback');
    el.textContent=msg||'';
    el.style.color=isError?'var(--expense)':'var(--ink-soft)';
  }
  function renderReport(report){
    var target=document.getElementById('aiReport');
    target.innerHTML='';
    if(!report){target.style.display='none';return;}
    target.style.display='block';
    var metrics=document.createElement('div');metrics.className='ai-metrics';
    [['Einnahmen',euros(report.income),'positive'],['Ausgaben',euros(report.expense),'negative'],
     ['Monatlicher Rest',euros(report.remaining),report.remaining>=0?'positive':'negative'],
     ['Kontostand Monatsende',euros(report.balance),report.balance>=0?'positive':'negative']]
      .forEach(function(m){
        var card=document.createElement('div');card.className='ai-metric';
        var name=document.createElement('span');name.textContent=m[0];
        var value=document.createElement('strong');value.className=m[2];value.textContent=m[1];
        card.appendChild(name);card.appendChild(value);metrics.appendChild(card);
      });
    target.appendChild(metrics);
    var meta=document.createElement('p');meta.className='ai-report-label';
    meta.textContent='Monatsbericht · '+keyLabel(report.key)+' · '+report.entryCount+' Buchungen';
    target.appendChild(meta);
    function section(title,items,kind){
      if(!items||!items.length)return;
      var block=document.createElement('section');block.className='ai-section '+(kind||'');
      var h=document.createElement('h3');h.textContent=title;block.appendChild(h);
      items.forEach(function(str){var p=document.createElement('p');p.textContent=str;block.appendChild(p);});
      target.appendChild(block);
    }
    section('Das lief gut',report.good,'good');
    section('Das ist aufgefallen',report.notice,'notice');
    section('Worauf du achten könntest',report.tips,'tip');
    if(report.categories&&report.categories.length){
      var cat=document.createElement('section');cat.className='ai-section';
      var h=document.createElement('h3');h.textContent='Wofür wurde Geld ausgegeben?';cat.appendChild(h);
      report.categories.forEach(function(c){
        var row=document.createElement('div');row.className='ai-cat ai-category-line';
        var name=document.createElement('span');name.textContent=c.name+' · '+c.count+'×';
        var right=document.createElement('strong');right.textContent=euros(c.amount);
        row.appendChild(name);row.appendChild(right);cat.appendChild(row);
        var progress=document.createElement('div');progress.className='ai-progress';
        var bar=document.createElement('span');bar.style.width=(report.expense>0?Math.min(100,c.amount/report.expense*100):0)+'%';
        progress.appendChild(bar);cat.appendChild(progress);
      });
      target.appendChild(cat);
    }
    function entrySection(title,rows,mapper){
      if(!rows||!rows.length)return;
      var block=document.createElement('section');block.className='ai-section';
      var h=document.createElement('h3');h.textContent=title;block.appendChild(h);
      rows.forEach(function(item){
        var pair=mapper(item),row=document.createElement('div');row.className='ai-cat';
        var name=document.createElement('span');name.textContent=pair[0];
        var amount=document.createElement('strong');amount.textContent=pair[1];
        row.appendChild(name);row.appendChild(amount);block.appendChild(row);
      });
      target.appendChild(block);
    }
    entrySection('Größte Einzelbuchungen',report.largest,function(e){return [e.title+' · '+shortDate(e.date),euros(e.amount)];});
    entrySection('Häufige Ausgabetitel',report.merchants,function(e){return [e.title+' · '+e.count+'×',euros(e.amount)];});
    entrySection('Über mehrere Monate wiederkehrend',report.regular,function(e){return [e.title+' · '+e.monthCount+' von 3 Monaten',euros(e.amount)+' aktuell'];});
    var note=document.createElement('p');note.className='ai-disclaimer';
    note.textContent='Die Auswertung erfolgt vollständig offline und ohne Sprachmodell. Bekannte Händler werden anhand von Titel-Regeln grob zugeordnet, eigene Kategorien übernommen. Unbekannte Titel bleiben ungeklärt; die App kann nicht erkennen, ob ein Kauf notwendig war.';
    target.appendChild(note);
  }
  function reportFrom(m){
    var base=baseAnalysis(m);
    return {key:m.key,hash:m.hash,generatedAt:new Date().toISOString(),
      entryCount:m.entries.length,income:m.income,expense:m.expense,
      remaining:m.remaining,balance:m.balance,categories:m.categories,
      good:base.good,notice:base.notice,tips:base.tips,
      largest:base.largest,merchants:base.merchants,regular:base.regular};
  }
  function refreshReport(){
    var m=compute(activeKey),cached=reports()[activeKey];
    renderReport(cached||null);
    if(cached&&cached.hash!==m.hash)showFeedback('Seit diesem Bericht wurden Buchungen geändert. Bitte eine neue Auswertung erstellen.',false);
    else showFeedback(cached?'Gespeicherter Bericht vom '+new Date(cached.generatedAt).toLocaleDateString('de-DE')+'.':'',false);
  }
  function open(){
    var options=monthOptions(),wanted=prevKey(currentMonthKey());
    activeKey=options.indexOf(wanted)>-1?wanted:(options[0]||currentMonthKey());
    var sel=document.getElementById('aiMonthSelect');sel.innerHTML='';
    options.forEach(function(k){var el=document.createElement('option');el.value=k;el.textContent=keyLabel(k);sel.appendChild(el);});
    sel.value=activeKey;
    document.getElementById('aiMonthlyOverlay').classList.add('open');
    refreshReport();
  }
  function close(){
    document.getElementById('aiMonthlyOverlay').classList.remove('open');
  }
  function generateBase(){
    var m=compute(activeKey);
    if(!m.entries.length){showFeedback('Für '+keyLabel(activeKey)+' sind keine Kontostand-Einträge vorhanden.',true);return;}
    var report=reportFrom(m);
    saveReport(report);renderReport(report);
    showFeedback('Auswertung auf diesem Gerät erstellt und gespeichert.',false);
  }
  function init(){
    var settings=document.getElementById('settingsIconBtn');if(!settings)return;
    var ai=document.createElement('button');ai.type='button';ai.id='aiHeaderBtn';ai.className='settings-icon-btn';ai.setAttribute('aria-label','Monatsauswertung');ai.setAttribute('title','Monatsauswertung');
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
      '.ai-disclaimer{font-size:11px;line-height:1.5;color:var(--ink-soft)}'+
      '.ai-progress{height:5px;background:var(--rule);border-radius:5px;margin:0 0 10px;overflow:hidden}.ai-progress span{display:block;height:100%;border-radius:5px;background:var(--primary)}';
    document.head.appendChild(css);
    var root=document.createElement('div');root.id='aiMonthlyOverlay';root.className='ai-overlay';
    root.innerHTML='<div class="ai-panel" role="dialog" aria-modal="true" aria-labelledby="aiTitle"><div class="ai-head"><h2 id="aiTitle">✦ Monatsauswertung</h2><button type="button" class="ai-close" id="aiClose" aria-label="Schließen">×</button></div>'+
      '<div class="ai-body"><label for="aiMonthSelect" style="font-weight:700;font-size:14px">Monat auswählen</label><select id="aiMonthSelect"></select>'+
      '<div class="ai-intro">Dein persönlicher Monatsrückblick – mit Einnahmen, Ausgaben, Kontostand, auffälligen Buchungen, häufigen Titeln und Vergleich zum Vormonat. Alles lokal, ohne KI-Modell.</div>'+
      '<button type="button" id="aiBaseGenerate" class="ai-action">Monat auswerten</button>'+
      '<div id="aiFeedback" role="status" aria-live="polite"></div><div id="aiReport" style="display:none"></div></div></div>';
    document.body.appendChild(root);
    ai.addEventListener('click',open);
    document.getElementById('aiClose').addEventListener('click',close);
    root.addEventListener('click',function(e){if(e.target===root)close();});
    document.getElementById('aiMonthSelect').addEventListener('change',function(e){activeKey=e.target.value;refreshReport();});
    document.getElementById('aiBaseGenerate').addEventListener('click',generateBase);
    document.addEventListener('keydown',function(e){if(e.key==='Escape'&&root.classList.contains('open'))close();});
    var oldClose=window.closeAnyOpenOverlay;
    window.closeAnyOpenOverlay=function(){if(root.classList.contains('open')){close();return true;}return typeof oldClose==='function'?oldClose():false;};
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();