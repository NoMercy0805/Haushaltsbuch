/* Haushaltsbuch+: Monatsauswertung. Keine Buchungen werden verändert. */
(function(){
  'use strict';
  if(window.__householdAiMonthlyInstalled) return;
  window.__householdAiMonthlyInstalled=true;
  var REPORT_KEY='household-monthly-reports-v2';
  var DATA_KEY='household-data-v8';
  var monthNames=['Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'];
  var activeKey='';
  var trendPeriod=6;

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
  var expenseCategories=[
    'Wohnen & Nebenkosten','Lebensmittel & Drogerie','Auto & Mobilität',
    'Versicherungen','Internet & Mobilfunk','Abos & Streaming',
    'Freizeit & Hobbys','Restaurant & Lieferdienst','Shopping & Kleidung',
    'Gesundheit & Pflege','Familie & Kinder','Haustiere',
    'Kredite & Finanzierung','Sparen & Rücklagen','Sonstiges'
  ];
  function categoryFor(e){
    var explicit=String(e.category||'').trim();
    // Fest gewaehlte Kategorien gehen immer vor – auch "Sonstiges".
    if(expenseCategories.indexOf(explicit)!==-1)return explicit;
    var legacy={
      'Wohnen & Verträge':'Wohnen & Nebenkosten',
      'Mobilität & Auto':'Auto & Mobilität',
      'Abos & Medien':'Abos & Streaming',
      'Essen außer Haus':'Restaurant & Lieferdienst',
      'Freizeit':'Freizeit & Hobbys',
      'Gesundheit':'Gesundheit & Pflege'
    };
    if(legacy[explicit])return legacy[explicit];
    // Kategorien aus aelteren CSV-Imports werden nicht verworfen.
    if(explicit && explicit!=='Keine' && explicit!=='Nicht zugeordnet' && explicit!=='Nicht eindeutig')return explicit;
    var t=String(e.title||'').toLocaleLowerCase('de-DE');
    if(/rewe|edeka|lidl|aldi|kaufland|netto|penny|tegut|norma|globus|supermarkt|lebensmittel|rossmann|drogerie|dm markt/.test(t))return 'Lebensmittel & Drogerie';
    if(/tankstelle|aral|shell|esso|diesel|benzin|tanken|werkstatt|reifen|parkhaus|parken|tüv|bahn|fahrkarte/.test(t))return 'Auto & Mobilität';
    if(/miete|nebenkosten|strom|gasabschlag|heizung|wasser|grundsteuer|hausgeld/.test(t))return 'Wohnen & Nebenkosten';
    if(/haftpflicht|hausrat|rechtsschutz|lebensversicherung|versicherung/.test(t))return 'Versicherungen';
    if(/mobilfunk|handyvertrag|telefon|internet|dsl|vodafone|telekom/.test(t))return 'Internet & Mobilfunk';
    if(/netflix|spotify|disney|prime video|youtube premium|dazn|abo|streaming/.test(t))return 'Abos & Streaming';
    if(/restaurant|lieferando|pizza|döner|imbiss|mcdonald|burger king|café|bäckerei/.test(t))return 'Restaurant & Lieferdienst';
    if(/arzt|apotheke|zahnarzt|medikament|klinik|physio/.test(t))return 'Gesundheit & Pflege';
    if(/tierarzt|hundefutter|katzenfutter|zooplus|fressnapf|haustier/.test(t))return 'Haustiere';
    if(/kindergarten|kita|schulbedarf|spielzeug|klassenfahrt/.test(t))return 'Familie & Kinder';
    if(/kreditrate|darlehen|finanzierung|tilgung/.test(t))return 'Kredite & Finanzierung';
    if(/sparkonto|rücklage|ruecklage|sparplan|sparen/.test(t))return 'Sparen & Rücklagen';
    if(/kleidung|schuhe|amazon|zalando|elektronik|shopping/.test(t))return 'Shopping & Kleidung';
    if(/kino|konzert|freizeit|steam|playstation|xbox|hobby|ausflug/.test(t))return 'Freizeit & Hobbys';
    return 'Sonstiges';
  }
  function isSavingsTransfer(e){return e.type!=='einnahme'&&categoryFor(e)==='Sparen & Rücklagen';}
  function sumEntries(entries){
    return entries.reduce(function(acc,e){
      var val=Number(e.amount)||0;
      if(e.type==='einnahme')acc.income+=val;
      else if(isSavingsTransfer(e))acc.transfers+=val;
      else acc.expense+=val;
      return acc;
    },{income:0,expense:0,transfers:0});
  }
  function groupsFor(entries){
    var groups={};
    entries.forEach(function(e){
      if(e.type==='einnahme'||isSavingsTransfer(e))return;
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
      hash:hashEntries(history),income:sums.income,expense:sums.expense,transfers:sums.transfers,
      remaining:sums.income-sums.expense,balance:balance,
      clearedBalance:all.filter(function(e){return e.reconciled&&e.date.slice(0,7)<=k;}).reduce(function(acc,e){return acc+(e.type==='einnahme'?1:-1)*Number(e.amount);},0),
      pendingIncome:cur.filter(function(e){return !e.reconciled&&e.type==='einnahme';}).reduce(function(a,e){return a+Number(e.amount);},0),
      pendingExpense:cur.filter(function(e){return !e.reconciled&&e.type!=='einnahme';}).reduce(function(a,e){return a+Number(e.amount);},0),
      previousIncome:older.income,previousExpense:older.expense,
      expenseDelta:sums.expense-older.expense,incomeDelta:sums.income-older.income,
      categories:categories,previousCategories:prevCategories,previousCount:prev.length};
  }
  function byTitle(entries){
    var groups={};
    entries.forEach(function(e){
      if(e.type==='einnahme'||isSavingsTransfer(e))return;
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
      .filter(function(e){return e.monthCount>=2&&e.count>0;})
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
    var largest=m.entries.filter(function(e){return e.type!=='einnahme'&&!isSavingsTransfer(e);})
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
  function formatSigned(n){return (n>0?'+':'')+euros(n);}
  function monthShift(k,steps){
    var d=new Date(Number(k.slice(0,4)),Number(k.slice(5,7))-1+steps,1);
    return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');
  }
  function metric(label,value,tone,icon){
    return '<div class="ai-metric"><span class="ai-metric-name">'+icon+' '+escapeHtml(label)+'</span>'+
      '<strong class="'+(tone||'')+'">'+escapeHtml(value)+'</strong></div>';
  }
  function dashboardSection(title,body,extra){
    return '<section class="ai-dashboard-section"><h3>'+escapeHtml(title)+'</h3>'+(extra||'')+body+'</section>';
  }
  function pair(label,value,tone){
    return '<div class="ai-pair"><span>'+escapeHtml(label)+'</span><strong class="'+(tone||'')+'">'+escapeHtml(value)+'</strong></div>';
  }
  function progressRow(name,amount,total,index){
    var pct=total>0?Math.min(100,amount/total*100):0;
    return '<div class="ai-progress-row"><div class="ai-pair"><span>'+escapeHtml(name)+'</span><strong>'+escapeHtml(euros(amount))+'</strong></div>'+
      '<div class="ai-progress"><span class="ai-color-'+(index%5)+'" style="width:'+pct.toFixed(2)+'%"></span></div></div>';
  }
  function trendData(k,count){
    var all=getEntries(),data=[];
    for(var i=count-1;i>=0;i--){
      var key=monthShift(k,-i),vals=sumEntries(all.filter(function(e){return e.date.slice(0,7)===key;}));
      data.push({key:key,label:monthNames[Number(key.slice(5,7))-1].slice(0,3),income:vals.income,expense:vals.expense});
    }
    return data;
  }
  function trendBars(k,count){
    var data=trendData(k,count),max=1;
    data.forEach(function(d){max=Math.max(max,d.income,d.expense);});
    var html='<div class="ai-chart" role="img" aria-label="Monatliche Einnahmen und Ausgaben">';
    data.forEach(function(d){
      html+='<div class="ai-chart-column" title="'+escapeHtml(keyLabel(d.key))+' – Einnahmen '+escapeHtml(euros(d.income))+'; Ausgaben '+escapeHtml(euros(d.expense))+'">'+
        '<div class="ai-chart-bars"><span class="ai-income-bar" style="height:'+(d.income/max*100).toFixed(1)+'%"></span>'+
        '<span class="ai-expense-bar" style="height:'+(d.expense/max*100).toFixed(1)+'%"></span></div>'+
        '<span class="ai-chart-label">'+d.label+'</span></div>';
    });
    return html+'</div><div class="ai-chart-legend"><span>● Einnahmen</span><span>● Ausgaben</span></div>';
  }
  function details(title,body,open){
    return '<details class="ai-details"'+(open?' open':'')+'><summary>'+escapeHtml(title)+
      '<span aria-hidden="true">⌄</span></summary><div class="ai-details-body">'+body+'</div></details>';
  }
  function entryRows(entries,empty){
    if(!entries||!entries.length)return '<p class="ai-small">'+escapeHtml(empty||'Keine Buchungen vorhanden.')+'</p>';
    return entries.map(function(e){
      return '<div class="ai-entry-line"><span>'+escapeHtml(e.title||'Ohne Titel')+
      (e.date?' <small>· '+escapeHtml(shortDate(e.date))+'</small>':'')+
      '</span><strong>'+escapeHtml(euros(Number(e.amount)||0))+'</strong></div>';
    }).join('');
  }
  function forecastFor(key){
    var next=monthShift(key,1),all=getEntries(),future=all.filter(function(e){return e.date.slice(0,7)===next;});
    var expected=sumEntries(future), recurring=future.filter(function(e){return e.recurring||e.recurringGroupId||e.isRecurringTemplate;});
    var rows=future.slice().sort(function(a,b){return a.date.localeCompare(b.date);}).slice(0,8);
    return {key:next,expected:expected,recurring:recurring,rows:rows,count:future.length};
  }
  function renderReport(report){
    var target=document.getElementById('aiReport');
    target.innerHTML='';
    if(!report){target.style.display='none';return;}
    target.style.display='block';
    var m=compute(report.key),all=getEntries(),cur=m.entries,now=currentMonthKey();
    var ongoing=report.key===now,complete=report.key<now;
    var historical=report.key>now;
    var rate=m.income>0?m.remaining/m.income*100:null;
    var pct=rate===null?'–':rate.toLocaleString('de-DE',{maximumFractionDigits:1})+' %';
    var meta='<p class="ai-report-label">'+escapeHtml(keyLabel(report.key))+' · '+cur.length+' Buchungen · '+
      (complete?'Abgeschlossener Monat':ongoing?'Laufender Monat':'Zukünftiger Monat')+'</p>';
    var html=meta+'<div class="ai-metrics">'+
      metric('Einnahmen',euros(m.income),'positive','↙')+
      metric('Ausgaben',euros(m.expense),'negative','↗')+
      metric('Monatsüberschuss',formatSigned(m.remaining),m.remaining>=0?'positive':'negative','◉')+
      metric('Überschussquote',pct,m.remaining>=0?'positive':'negative','◷')+
      '</div>';
    if(ongoing)html+='<p class="ai-small">Dieser Monat läuft noch. Vergleiche mit abgeschlossenen Monaten sind vorläufig.</p>';
    html+=dashboardSection('Monatsbilanz',
      progressRow('Einnahmen',m.income,Math.max(m.income,m.expense),0)+
      progressRow('Ausgaben',m.expense,Math.max(m.income,m.expense),1)+
      pair('Überschuss vor Sparüberträgen',formatSigned(m.remaining),m.remaining>=0?'positive':'negative')+
      (m.transfers?pair('Sparen & Rücklagen (separat)',euros(m.transfers),''):'')+
      '<p class="ai-small">Der Überschuss ergibt sich aus erfassten Einnahmen minus normalen Ausgaben. Sparüberträge werden separat ausgewiesen.</p>');
    html+=dashboardSection('Kontostand & offene Buchungen',
      pair('Abgeglichener Kontostand',euros(m.clearedBalance),m.clearedBalance>=0?'positive':'negative')+
      pair('Offene Einnahmen',formatSigned(m.pendingIncome),'positive')+
      pair('Offene Ausgaben',m.pendingExpense?'-'+euros(m.pendingExpense):euros(0),'negative')+
      pair('Endsaldo inkl. offener Buchungen',euros(m.balance),m.balance>=0?'positive':'negative')+
      '<p class="ai-small">Kontostände bis einschließlich '+escapeHtml(keyLabel(m.key))+'. Offen bedeutet: noch nicht abgeglichen. Die Anzeige basiert ausschließlich auf den erfassten Kontostand-Buchungen.</p>');
    var prev=trendData(m.key,2)[0];
    var prevAvailable=all.some(function(e){return e.date.slice(0,7)===prev.key;});
    var diff=m.expense-prev.expense;
    var comparison=prevAvailable?
      pair('Ausgaben zum Vormonat',formatSigned(diff),diff<=0?'positive':'negative')+
      pair('Einnahmen zum Vormonat',formatSigned(m.income-prev.income),m.income>=prev.income?'positive':'negative'):
      '<p class="ai-small">Keine erfassten Buchungen im Vormonat für einen belastbaren Vergleich.</p>';
    var switches='<div class="ai-periods" role="group" aria-label="Zeitraum">'+[3,6,12].map(function(n){
      return '<button type="button" class="ai-period'+(trendPeriod===n?' active':'')+'" data-ai-period="'+n+'">'+n+' Monate</button>';
    }).join('')+'</div>';
    html+=dashboardSection('Entwicklung',comparison+switches+trendBars(m.key,trendPeriod));
    var catRows=m.categories.map(function(c,i){
      var book=cur.filter(function(e){return e.type!=='einnahme'&&!isSavingsTransfer(e)&&categoryFor(e)===c.name;});
      var lines=entryRows(book,'Keine Buchungen.');
      return details(c.name+' · '+euros(c.amount)+' · '+c.count+' Buchungen',
        progressRow('Anteil an Ausgaben',c.amount,m.expense,i)+lines,false);
    }).join('');
    html+=dashboardSection('Ausgaben nach Kategorien',
      m.categories.length?catRows:'<p class="ai-small">Keine Ausgaben in diesem Monat.</p>'+
      '<p class="ai-small">Die im Kontostand ausgewählten Kategorien werden verwendet.</p>');
    var recurring=recurringTitles(m);
    var recurringKeys={};
    recurring.forEach(function(r){recurringKeys[r.title.trim().toLocaleLowerCase('de-DE')]=true;});
    var expenses=cur.filter(function(e){return e.type!=='einnahme'&&!isSavingsTransfer(e);});
    var fixed=expenses.filter(function(e){return e.recurring||e.isRecurringTemplate||e.recurringGroupId||recurringKeys[String(e.title||'').trim().toLocaleLowerCase('de-DE')];});
    var variable=expenses.filter(function(e){return fixed.indexOf(e)===-1;});
    var fixedTotal=fixed.reduce(function(a,e){return a+Number(e.amount);},0);
    var variableTotal=variable.reduce(function(a,e){return a+Number(e.amount);},0);
    html+=dashboardSection('Regelmäßige & variable Kosten',
      pair('Regelmäßige Buchungen',euros(fixedTotal),'')+
      pair('Übrige Buchungen',euros(variableTotal),'')+
      '<p class="ai-small">Regelmäßig = Dauerauftrag oder gleicher Titel in mindestens zwei der letzten drei Monate. Das ist eine Näherung, keine zusätzliche Abbuchung.</p>'+
      details('Regelmäßige Buchungen ('+fixed.length+')',entryRows(fixed),false)+
      details('Übrige Buchungen ('+variable.length+')',entryRows(variable),false));
    var largest=expenses.slice().sort(function(a,b){return Number(b.amount)-Number(a.amount);}).slice(0,5);
    var grouped=byTitle(cur).slice(0,6);
    var merchantRows=grouped.map(function(e){
      return '<div class="ai-entry-line"><span>'+escapeHtml(e.title)+' <small>· '+e.count+'×</small></span><strong>'+escapeHtml(euros(e.amount))+'</strong></div>';
    }).join('');
    html+=dashboardSection('Auffällige Buchungen',
      details('Fünf größte Ausgaben',entryRows(largest),true)+
      details('Häufige Ausgabetitel',merchantRows||'<p class="ai-small">Keine Daten.</p>',false));
    var fc=forecastFor(m.key);
    var forecastBody=fc.count?
      pair('Bereits erfasste Einnahmen',euros(fc.expected.income),'positive')+
      pair('Bereits erfasste Ausgaben',euros(fc.expected.expense),'negative')+
      pair('Erfasste Sparüberträge',euros(fc.expected.transfers),'')+
      details('Bekannte Buchungen ('+fc.count+')',entryRows(fc.rows),false):
      '<p class="ai-small">Für diesen Monat sind noch keine Buchungen eingetragen. Eine verlässliche Prognose ist nicht möglich.</p>';
    html+=dashboardSection('Ausblick: '+keyLabel(fc.key),forecastBody+
      '<p class="ai-small">Nur bereits gespeicherte Buchungen einschließlich Daueraufträgen. Fehlende Einnahmen oder Zahlungen werden nicht geschätzt; keine Garantie für den künftigen Kontostand.</p>');
    var analysis=baseAnalysis(m);
    var insights=analysis.good.map(function(x){return '<p class="ai-insight good">✓ '+escapeHtml(x)+'</p>';}).join('')+
      analysis.notice.map(function(x){return '<p class="ai-insight notice">• '+escapeHtml(x)+'</p>';}).join('')+
      analysis.tips.map(function(x){return '<p class="ai-insight tip">✦ '+escapeHtml(x)+'</p>';}).join('');
    html+=dashboardSection('Deine Erkenntnisse',details('Hinweise und Empfehlungen ('+
      (analysis.good.length+analysis.notice.length+analysis.tips.length)+')',
      insights||'<p class="ai-small">Keine Auffälligkeiten.</p>',true));
    html+='<p class="ai-disclaimer">Alle Berechnungen erfolgen offline und nur auf Basis der im Kontostand erfassten Buchungen. Kategorien sind manuell festgelegt oder anhand des Titels geschätzt. Buchungen aus Kredite, Buchungen und Variable Kosten werden nicht zusätzlich in die Summen eingerechnet. Sparüberträge sind getrennt ausgewiesen.</p>';
    target.innerHTML=html;
    target.querySelectorAll('[data-ai-period]').forEach(function(btn){
      btn.addEventListener('click',function(){
        trendPeriod=Number(btn.getAttribute('data-ai-period'));
        renderReport(report);
      });
    });
  }
  function reportFrom(m){
    var base=baseAnalysis(m);
    return {key:m.key,hash:m.hash,generatedAt:new Date().toISOString(),
      entryCount:m.entries.length,income:m.income,expense:m.expense,transfers:m.transfers,
      remaining:m.remaining,balance:m.balance,clearedBalance:m.clearedBalance,
      pendingIncome:m.pendingIncome,pendingExpense:m.pendingExpense,categories:m.categories,
      good:base.good,notice:base.notice,tips:base.tips,
      largest:base.largest,merchants:base.merchants,regular:base.regular};
  }
  function refreshReport(){
    // Die aktuelle Auswertung wird direkt aus dem Kontostand erstellt – kein veralteter Cache.
    var m=compute(activeKey);
    renderReport(reportFrom(m));
    showFeedback(m.entries.length?'Aktuelle Daten aus dem Kontostand.':'Für diesen Monat sind noch keine Buchungen erfasst.',false);
  }
  function open(){
    var options=monthOptions(),wanted=currentMonthKey();
    activeKey=options.indexOf(wanted)>-1?wanted:(options[0]||currentMonthKey());
    var items=document.getElementById('aiMonthOptions');
    items.innerHTML='';
    options.forEach(function(k){
      var btn=document.createElement('button');
      btn.type='button';btn.className='ai-month-option'+(k===activeKey?' selected':'');
      btn.dataset.monthKey=k;
      btn.setAttribute('role','option');btn.setAttribute('aria-selected',k===activeKey?'true':'false');
      btn.innerHTML='<span>'+escapeHtml(keyLabel(k))+'</span><span class="ai-month-check" aria-hidden="true">'+(k===activeKey?'✓':'')+'</span>';
      items.appendChild(btn);
    });
    document.getElementById('aiMonthTriggerLabel').textContent=keyLabel(activeKey);
    document.getElementById('aiMonthTrigger').setAttribute('aria-expanded','false');
    document.getElementById('aiMonthSheet').classList.remove('open');
    document.getElementById('aiMonthlyOverlay').classList.add('open');
    refreshReport();
  }
  function closeMonthSheet(){
    var sheet=document.getElementById('aiMonthSheet');
    if(sheet)sheet.classList.remove('open');
    var trigger=document.getElementById('aiMonthTrigger');
    if(trigger)trigger.setAttribute('aria-expanded','false');
  }
  function close(){
    closeMonthSheet();
    document.getElementById('aiMonthlyOverlay').classList.remove('open');
  }
  function generateBase(){
    var m=compute(activeKey);
    var report=reportFrom(m);
    saveReport(report);renderReport(report);
    showFeedback('Aktuelle Auswertung auf diesem Gerät gespeichert.',false);
  }
  function init(){
    var settings=document.getElementById('settingsIconBtn');if(!settings)return;
    var ai=document.createElement('button');ai.type='button';ai.id='aiHeaderBtn';ai.className='settings-icon-btn';ai.setAttribute('aria-label','Monatsauswertung');ai.setAttribute('title','Monatsauswertung');
    ai.innerHTML='<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7z"/><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"/><path d="M3 3v4M1 5h4"/></svg>';
    settings.parentNode.insertBefore(ai,settings);
    var css=document.createElement('style');
    css.textContent='#aiHeaderBtn{color:var(--primary)} #aiHeaderBtn:active{transform:scale(.96)}'+
      '.ai-overlay{position:fixed;inset:0;z-index:10020;background:rgba(0,0,0,.52);display:none;align-items:center;justify-content:center;padding:16px;box-sizing:border-box;overflow:hidden;}'+
      '.ai-overlay.open{display:flex} .ai-panel{background:var(--surface-solid);color:var(--ink);border:1px solid var(--rule);border-radius:22px;width:100%;max-width:none;max-height:calc(100dvh - 42px);display:flex;flex-direction:column;box-shadow:0 16px 50px rgba(0,0,0,.24);overflow:hidden;}'+
      '.ai-head{padding:19px 19px 12px;display:flex;align-items:center;justify-content:space-between;gap:10px;border-bottom:1px solid var(--rule)}'+
      '.ai-head h2{margin:0;font-size:20px;font-weight:800}.ai-close{background:var(--surface-2);border:0;border-radius:50%;width:36px;height:36px;color:var(--ink);font-size:24px;cursor:pointer}'+
      '.ai-body{overflow:auto;overscroll-behavior:contain;padding:18px;display:flex;flex-direction:column;gap:13px}'+
      '.ai-panel{position:relative}'+
      '.ai-month-trigger{display:flex;align-items:center;justify-content:space-between;gap:10px;width:100%;min-height:52px;padding:12px 16px;border-radius:15px;border:1px solid rgba(255,255,255,.55);background:rgba(246,248,252,.83);color:var(--ink);box-shadow:inset 0 1px 0 rgba(255,255,255,.65),0 4px 13px rgba(0,0,0,.07);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);font-size:16px;font-weight:650;cursor:pointer;text-align:left}'+
      '.ai-month-trigger svg{width:18px;height:18px;flex:none;color:var(--ink-soft)}'+
      'html[data-theme="dark"] .ai-month-trigger{background:rgba(52,57,68,.91);border-color:rgba(255,255,255,.2);box-shadow:inset 0 1px 0 rgba(255,255,255,.11),0 4px 13px rgba(0,0,0,.18)}'+
      '.ai-month-sheet{position:absolute;inset:0;z-index:40;display:none;align-items:flex-end;background:rgba(10,13,20,.38);border-radius:22px;overflow:hidden}'+
      '.ai-month-sheet.open{display:flex}'+
      '.ai-month-picker{width:100%;max-height:min(68dvh,590px);display:flex;flex-direction:column;overflow:hidden;padding:12px 12px 16px;background:rgba(246,247,250,.97);border-top:1px solid rgba(255,255,255,.7);border-radius:22px 22px 0 0;backdrop-filter:blur(22px);-webkit-backdrop-filter:blur(22px);box-shadow:0 -10px 40px rgba(0,0,0,.2)}'+
      'html[data-theme="dark"] .ai-month-picker{background:rgba(43,47,55,.98);border-color:rgba(255,255,255,.2)}'+
      '.ai-month-picker-head{display:flex;align-items:center;justify-content:space-between;padding:4px 8px 12px;font-weight:800;font-size:16px;color:var(--ink)}'+
      '.ai-month-close{border:0;background:var(--surface-2);color:var(--ink);width:32px;height:32px;border-radius:50%;font-size:22px;cursor:pointer}'+
      '.ai-month-options{overflow:auto;overscroll-behavior:contain;display:flex;flex-direction:column;gap:5px;padding:3px 2px 6px}'+
      '.ai-month-option{width:100%;display:flex;justify-content:space-between;align-items:center;min-height:47px;gap:8px;border-radius:12px;padding:11px 14px;text-align:left;font-size:14px;font-weight:650;color:var(--ink);border:1px solid rgba(255,255,255,.45);background:var(--surface);cursor:pointer}'+
      '.ai-month-option.selected{background:var(--selection-bg);color:var(--selection-text);border-color:var(--selection-ring)}'+
      '.ai-month-check{font-size:18px;min-width:22px;text-align:center}'+
      '.ai-month-sheet{background:transparent!important;pointer-events:none;overflow:visible!important;border-radius:0!important}'+
      '.ai-month-sheet.open{display:block!important}'+
      '.ai-month-picker{position:absolute;box-sizing:border-box;pointer-events:auto;min-width:0;overflow:hidden;width:auto;max-height:none;padding:7px!important;border-radius:15px!important;border:1px solid rgba(255,255,255,.68)!important;box-shadow:0 10px 28px rgba(0,0,0,.19)!important}'+
      'html[data-theme="dark"] .ai-month-picker{border-color:rgba(255,255,255,.19)!important;box-shadow:0 10px 28px rgba(0,0,0,.37)!important}'+
      '.ai-month-picker-head{display:none!important}'+
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
      '.ai-progress{height:7px;background:var(--rule);border-radius:999px;margin:7px 0 12px;overflow:hidden}.ai-progress span{display:block;height:100%;border-radius:999px;background:var(--primary)}'+
      '.ai-dashboard-section{border-radius:17px;background:var(--surface);border:1px solid var(--rule-strong);padding:14px;margin:10px 0;box-shadow:inset 0 1px 0 rgba(255,255,255,.13)}'+
      '.ai-dashboard-section h3{font-size:15px;margin:0 0 13px;font-weight:800;color:var(--ink)}'+
      '.ai-metrics{margin-bottom:4px}.ai-metric{background:var(--surface-2);border:1px solid var(--rule-strong);border-radius:14px;padding:13px 11px;min-height:68px;justify-content:center;box-shadow:inset 0 1px 0 rgba(255,255,255,.1)}'+
      '.ai-metric .ai-metric-name{font-size:11px;font-weight:600}.ai-metric strong{font-size:clamp(15px,4vw,19px);letter-spacing:-.03em}'+
      '.ai-pair{display:flex;gap:10px;align-items:baseline;justify-content:space-between;padding:9px 0;border-bottom:1px solid var(--rule);font-size:12.5px;line-height:1.4}'+
      '.ai-pair:last-child{border-bottom:0}.ai-pair span{color:var(--ink-soft);flex:1;min-width:0}.ai-pair strong{color:var(--ink);font-size:13px;text-align:right;flex-shrink:0}'+
      '.ai-pair strong.positive{color:var(--savings)}.ai-pair strong.negative{color:var(--expense)}'+
      '.ai-progress-row{margin-bottom:8px}.ai-progress-row .ai-pair{border:0;padding:2px 0}.ai-progress .ai-color-0{background:var(--savings)}'+
      '.ai-progress .ai-color-1{background:var(--expense)}.ai-progress .ai-color-2{background:var(--primary)}'+
      '.ai-progress .ai-color-3{background:var(--gold)}.ai-progress .ai-color-4{background:#a38bd8}'+
      '.ai-periods{display:flex;gap:6px;margin:12px 0 8px}.ai-period{flex:1;border:1px solid var(--rule-strong);border-radius:11px;padding:8px 3px;background:var(--surface-2);color:var(--ink-soft);font-size:12px;font-weight:700;cursor:pointer}'+
      '.ai-period.active{background:var(--selection-bg);color:var(--selection-text);box-shadow:inset 0 0 0 1px var(--selection-ring)}'+
      '.ai-chart{height:155px;display:flex;gap:6px;align-items:stretch;justify-content:space-between;padding:12px 2px 2px;border-bottom:1px solid var(--rule-strong)}'+
      '.ai-chart-column{flex:1;min-width:0;display:flex;align-items:center;flex-direction:column;justify-content:flex-end;gap:6px}'+
      '.ai-chart-bars{width:100%;height:124px;display:flex;gap:3px;justify-content:center;align-items:flex-end}'+
      '.ai-chart-bars span{display:block;width:34%;max-width:22px;min-height:1px;border-radius:4px 4px 0 0}'+
      '.ai-income-bar{background:var(--savings)}.ai-expense-bar{background:var(--expense)}.ai-chart-label{font-size:10px;color:var(--ink-soft);white-space:nowrap}'+
      '.ai-chart-legend{display:flex;justify-content:center;gap:18px;font-size:11px;margin:9px 0;color:var(--ink-soft)}'+
      '.ai-chart-legend span:first-child{color:var(--savings)}.ai-chart-legend span:last-child{color:var(--expense)}'+
      '.ai-details{background:var(--surface-2);border:1px solid var(--rule);border-radius:13px;margin:8px 0;overflow:hidden}'+
      '.ai-details summary{cursor:pointer;padding:12px;font-weight:700;font-size:12.5px;display:flex;justify-content:space-between;align-items:center;gap:8px;list-style:none}'+
      '.ai-details summary::-webkit-details-marker{display:none}.ai-details[open] summary span{transform:rotate(180deg)}'+
      '.ai-details-body{padding:0 12px 12px}.ai-entry-line{display:flex;justify-content:space-between;gap:10px;padding:9px 0;border-bottom:1px solid var(--rule);font-size:12px}'+
      '.ai-entry-line:last-child{border-bottom:0}.ai-entry-line span{flex:1;min-width:0;overflow-wrap:anywhere}.ai-entry-line small{color:var(--ink-soft)}'+
      '.ai-entry-line strong{white-space:nowrap;font-size:12px}.ai-insight{padding:9px 11px;border-radius:9px;background:var(--surface);font-size:12.5px;line-height:1.5}'+
      '.ai-insight.good{border-left:3px solid var(--savings)}.ai-insight.notice{border-left:3px solid var(--expense)}.ai-insight.tip{border-left:3px solid var(--primary)}';
    document.head.appendChild(css);
    var root=document.createElement('div');root.id='aiMonthlyOverlay';root.className='ai-overlay';
    root.innerHTML='<div class="ai-panel" role="dialog" aria-modal="true" aria-labelledby="aiTitle"><div class="ai-head"><h2 id="aiTitle">✦ Monatsauswertung</h2><button type="button" class="ai-close" id="aiClose" aria-label="Schließen">×</button></div>'+
      '<div class="ai-body"><label for="aiMonthTrigger" style="font-weight:700;font-size:14px">Monat auswählen</label>'+
      '<button type="button" class="ai-month-trigger" id="aiMonthTrigger" aria-haspopup="listbox" aria-expanded="false" aria-controls="aiMonthOptions"><span id="aiMonthTriggerLabel"></span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg></button>'+
      '<div class="ai-intro">Dein persönliches Finanz-Dashboard: Monatsbilanz, Kontostand, Entwicklung, Kategorien, regelmäßige Ausgaben, Ausblick und konkrete Erkenntnisse. Vollständig offline.</div>'+
      '<div id="aiFeedback" role="status" aria-live="polite"></div><div id="aiReport" style="display:none"></div></div>'+
      '<div class="ai-month-sheet" id="aiMonthSheet"><div class="ai-month-picker"><div class="ai-month-picker-head"><span>Monat auswählen</span><button type="button" id="aiMonthSheetClose" class="ai-month-close" aria-label="Monatsauswahl schließen">×</button></div><div class="ai-month-options" id="aiMonthOptions" role="listbox" aria-label="Monate"></div></div></div></div>';
    document.body.appendChild(root);
    ai.addEventListener('click',open);
    document.getElementById('aiClose').addEventListener('click',close);
    root.addEventListener('click',function(e){if(e.target===root)close();});
    var monthSheet=document.getElementById('aiMonthSheet');
    var monthTrigger=document.getElementById('aiMonthTrigger');
    monthTrigger.addEventListener('click',function(){
      if(monthSheet.classList.contains('open')){closeMonthSheet();return;}
      var picker=monthSheet.querySelector('.ai-month-picker');
      var panel=monthSheet.parentElement;
      var r=monthTrigger.getBoundingClientRect(),p=panel.getBoundingClientRect(),gap=6;
      var below=Math.max(0,p.bottom-r.bottom-10),above=Math.max(0,r.top-p.top-10);
      var down=below>=Math.min(310,160)||below>=above;
      picker.style.width=r.width+'px';
      picker.style.left=(r.left-p.left)+'px';
      picker.style.maxHeight=Math.min(340,Math.max(65,down?below:above))+'px';
      picker.style.top=down?(r.bottom-p.top+gap)+'px':'auto';
      picker.style.bottom=down?'auto':(p.bottom-r.top+gap)+'px';
      monthSheet.classList.add('open');
      monthTrigger.setAttribute('aria-expanded','true');
      var selected=monthSheet.querySelector('.ai-month-option.selected');
      if(selected)selected.scrollIntoView({block:'nearest'});
    });
    document.getElementById('aiMonthSheetClose').addEventListener('click',closeMonthSheet);
    monthSheet.addEventListener('click',function(e){if(e.target===monthSheet)closeMonthSheet();});
    document.addEventListener('pointerdown',function(e){
      if(monthSheet.classList.contains('open')&&!e.target.closest('.ai-month-picker')&&
         !monthTrigger.contains(e.target))closeMonthSheet();
    },true);
    document.getElementById('aiMonthOptions').addEventListener('click',function(e){
      var button=e.target.closest('[data-month-key]');
      if(!button)return;
      activeKey=button.dataset.monthKey;
      monthTrigger.querySelector('#aiMonthTriggerLabel').textContent=keyLabel(activeKey);
      document.querySelectorAll('.ai-month-option').forEach(function(option){
        var selected=option.dataset.monthKey===activeKey;
        option.classList.toggle('selected',selected);
        option.setAttribute('aria-selected',selected?'true':'false');
        option.querySelector('.ai-month-check').textContent=selected?'✓':'';
      });
      closeMonthSheet();
      refreshReport();
    });
    document.addEventListener('keydown',function(e){
      if(e.key!=='Escape'||!root.classList.contains('open'))return;
      if(monthSheet.classList.contains('open'))closeMonthSheet();else close();
    });
    var oldClose=window.closeAnyOpenOverlay;
    window.closeAnyOpenOverlay=function(){if(root.classList.contains('open')){close();return true;}return typeof oldClose==='function'?oldClose():false;};
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();