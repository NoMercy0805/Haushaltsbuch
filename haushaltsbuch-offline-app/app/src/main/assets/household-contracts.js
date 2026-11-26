(function(){
  'use strict';

  function readTermValue(rawValue){
    if(rawValue === undefined || rawValue === null) return 0;
    var s = String(rawValue).trim();
    if(!s || s === '0') return 0;
    if(/^[1-4]w$/i.test(s)) return s.toLowerCase();
    var n = parseInt(s, 10);
    return isNaN(n) ? 0 : n;
  }

  // Zerlegt einen Laufzeit-/Fristwert in Monate ODER Tage (nie beides).
  function parseTermUnit(term){
    if(!term || term === '0') return { months:0, days:0 };
    var s = String(term);
    if(/w$/i.test(s)){
      var weeks = parseInt(s, 10) || 0;
      return { months:0, days: weeks*7 };
    }
    return { months: parseInt(s, 10) || 0, days:0 };
  }

  // Verschiebt ein ISO-Datum um eine Laufzeit/Frist (Monate oder Wochen), vorwärts (sign=1) oder rückwärts (sign=-1).
  function shiftDateByTerm(iso, term, sign){
    if(!iso || !term) return iso;
    var u = parseTermUnit(term);
    if(u.days){
      var d = new Date(iso+'T00:00:00');
      d.setDate(d.getDate() + sign*u.days);
      return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
    }
    if(u.months) return addMonths(iso, sign*u.months);
    return iso;
  }

  function fmtDate(iso){
    if(!iso) return '—';
    var p = iso.split('-');
    if(p.length !== 3) return iso;
    return p[2] + '.' + p[1] + '.' + p[0];
  }

  function todayISO(){
    var d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
  }

  function daysUntil(iso){
    if(!iso) return null;
    var today = new Date(todayISO()+'T00:00:00');
    var target = new Date(iso+'T00:00:00');
    return Math.round((target - today) / 86400000);
  }

  // Addiert (oder subtrahiert, bei negativem Wert) Kalendermonate zu einem ISO-Datum,
  // ohne bei kürzeren Monaten (z. B. 31.01. + 1 Monat) ins Übernächste zu rutschen.
  function addMonths(iso, months){
    if(!iso || !months) return iso;
    var d = new Date(iso+'T00:00:00');
    var day = d.getDate();
    d.setDate(1);
    d.setMonth(d.getMonth() + months);
    var lastDayOfTargetMonth = new Date(d.getFullYear(), d.getMonth()+1, 0).getDate();
    d.setDate(Math.min(day, lastDayOfTargetMonth));
    return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
  }

  // Enddatum aus Startdatum + Mindestlaufzeit: der Starttag zählt mit, daher endet
  // z. B. eine 12-Monats-Laufzeit ab 01.01. am 31.12. (ein Tag vor dem Jahrestag).
  function computeTermEnd(startIso, term){
    var raw = shiftDateByTerm(startIso, term, 1);
    var d = new Date(raw+'T00:00:00');
    d.setDate(d.getDate() - 1);
    return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
  }

  function elapsedTermPercent(start, end){
    if(!start || !end) return null;
    var from = Date.parse(start);
    var to = Date.parse(end);
    var now = Date.parse(todayISO());
    if(to <= from) return now >= to ? 100 : 0;
    return Math.min(100, Math.max(0, (now - from) * 100 / (to - from)));
  }

  var statuses = {aktiv:'Aktiv', gekuendigt:'Gekündigt', pausiert:'Pausiert', beendet:'Beendet'};
  var noticeKey = 'household-contract-notifications';
  var daysKey = 'household-contract-notification-days';

  function isoDate(value){
    return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) &&
      !isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0,10) === value ? value : '';
  }

  function isStandaloneBackup(text){
    try{
      var data = JSON.parse(text);
      return !!data && Array.isArray(data.contracts) && !['income','fixedExpenses','openInvoices','betaEntries'].some(function(k){
        return Object.prototype.hasOwnProperty.call(data,k);
      });
    }catch(err){
      var tables = Array.from(text.matchAll(/^\s*# TABLE:([^\r\n]+)$/gm), function(m){ return m[1].trim(); });
      return tables.length === 1 && tables[0] === 'contracts';
    }
  }

  window.createHouseholdContracts = function(app){
    var editId = null;
    var storage = app.storage;
    var esc = app.escape;
    function el(id){ return document.getElementById(id); }
    function contracts(){ return app.getState().contracts; }
    function normalize(row){
      if(!row || typeof row.name !== 'string' || !row.name.trim()) throw new Error('Vertragsname fehlt');
      if((row.start && !isoDate(row.start)) || (row.end && !isoDate(row.end))) throw new Error('Ungültiges Vertragsdatum');
      return {
        id:typeof row.id === 'string' && row.id ? row.id : app.takeId(),
        name:row.name.trim(), start:row.start || '', end:row.end || '',
        minTerm:readTermValue(row.minTerm), notice:readTermValue(row.notice),
        status:Object.prototype.hasOwnProperty.call(statuses,row.status) ? row.status : 'aktiv',
        autoRenewed:row.autoRenewed === true || row.autoRenewed === 'true'
      };
    }
    function renew(){
      var changed = false;
      contracts().forEach(function(row){
        if(row.status !== 'aktiv' || !isoDate(row.end)) return;
        for(var n=0; daysUntil(row.end)<0 && n<240; n++){
          row.end = addMonths(row.end,1);
          row.autoRenewed = true;
          changed = true;
        }
      });
      if(changed) app.saveState();
    }
    function deadlines(){
      return contracts().filter(function(row){ return row.status === 'aktiv' && isoDate(row.end) && readTermValue(row.notice); })
        .map(function(row){
          var deadline = shiftDateByTerm(row.end,row.notice,-1);
          return {row:row, deadline:deadline, days:daysUntil(deadline)};
        }).sort(function(a,b){ return a.deadline.localeCompare(b.deadline); });
    }
    function render(){
      var list = el('contractList');
      var expanded = new Set(Array.from(list.querySelectorAll('.expanded'),function(card){ return card.dataset.contractId; }));
      var due = deadlines();
      var first = due[0], second = due[1];
      el('contractSummary').innerHTML =
        '<div class="hero-card-label">Nächste Kündigungsfrist</div>'+
        '<div class="hero-card-value">'+(first ? fmtDate(first.deadline) : '–')+'</div>'+
        (first ? '<div class="contract-hero-name">'+esc(first.row.name)+'</div>' : '')+
        '<div class="hero-card-sub-row">'+
          '<div><div class="hero-card-sub-label">Danach</div><div class="hero-card-sub-value">'+
            (second ? esc(second.row.name)+' · '+fmtDate(second.deadline) : '–')+'</div></div>'+
          '<div><div class="hero-card-sub-label">Aktive Verträge</div><div class="hero-card-sub-value">'+
            contracts().filter(function(row){ return row.status === 'aktiv'; }).length+'</div></div>'+
        '</div>';
      var ordered = contracts().slice().sort(function(a,b){
        return Number(a.status === 'beendet') - Number(b.status === 'beendet') ||
          (a.end || '9999').localeCompare(b.end || '9999');
      });
      list.innerHTML = ordered.map(function(row){
        var start = isoDate(row.start);
        var end = isoDate(row.end);
        var elapsed = elapsedTermPercent(start,end);
        var endDays = daysUntil(end);
        var deadline = end && readTermValue(row.notice) ? shiftDateByTerm(end,row.notice,-1) : '';
        var deadlineDays = daysUntil(deadline);
        var active = row.status === 'aktiv';
        var tone = active && deadline && deadlineDays < 0 ? ' entry-status-due' :
          active && ((deadline && deadlineDays <= 30) || (end && endDays <= 30)) ? ' contract-status-soon' :
          row.status === 'beendet' ? ' entry-status-clear' : '';
        return '<article class="vc-card contract-entry'+tone+(expanded.has(row.id)?' expanded':'')+'" data-contract-id="'+esc(row.id)+'">'+
          '<div class="contract-card-summary">'+
          (elapsed === null ? '' : app.renderRing(elapsed,false,'der Laufzeit vergangen'))+
          '<div class="contract-top"><strong>'+esc(row.name)+'</strong><strong>'+fmtDate(end)+'</strong>'+
          '<span class="contract-sub">'+esc(statuses[row.status] || 'Aktiv')+'</span>'+
          '<span class="contract-sub">'+(deadline ? 'Kündigen bis '+fmtDate(deadline) : end ? 'Vertragsende' : 'Kein Enddatum')+'</span></div></div>'+
          '<div class="contract-details">'+
            '<span>Startdatum: '+fmtDate(start)+'</span>'+
            (end ? '<span>'+(endDays<0 ? 'Abgelaufen' : endDays===0 ? 'Läuft heute aus' : 'Restlaufzeit: '+endDays+(endDays===1?' Tag':' Tage'))+'</span>' : '')+
            (deadline && active && deadlineDays<0 ? '<span>Kündigungsfrist verstrichen</span>' : '')+
            (row.autoRenewed === true && active ? '<span>Automatisch um einen Monat verlängert</span>' : '')+
          '</div>'+
          '<div class="vc-actions"><button type="button" class="action-icon-btn neutral" data-contract-action="edit">Bearbeiten</button>'+
          '<button type="button" class="action-icon-btn danger" data-contract-action="delete">Löschen</button></div></article>';
      }).join('') || '<div class="empty-state">Noch keine Verträge eingetragen.</div>';
      app.decorate();
    }

    function openForm(row){
      editId = row ? row.id : null;
      el('contractForm').reset();
      el('contractFormTitle').textContent = row ? 'Vertrag bearbeiten' : 'Neuer Vertrag';
      el('contractFormSubmit').textContent = row ? 'Speichern' : 'Vertrag hinzufügen';
      el('cName').value = row ? row.name : '';
      el('cStart').value = row ? row.start : todayISO();
      el('cEnd').value = row ? row.end : '';
      el('cStatus').value = row ? row.status : 'aktiv';
      el('cMinTerm').value = row ? String(row.minTerm || 0) : '0';
      el('cNotice').value = row ? String(row.notice || 0) : '0';
      el('addContractOverlay').classList.add('open');
    }

    async function importBackup(text){
      try{
        var rows;
        try{
          var json = JSON.parse(text);
          rows = json && json.contracts;
        }catch(err){
          if(!isStandaloneBackup(text)) throw err;
          var data = app.parseCSV(text);
          var header = data.findIndex(function(row){ return row[0].trim() === '# TABLE:contracts'; }) + 1;
          while(data[header] && data[header].every(function(cell){ return !cell.trim(); })) header++;
          var columns = data[header] || [];
          if(!columns.includes('name')) throw new Error('Spaltenkopf fehlt');
          rows = data.slice(header+1).filter(function(row){
            return row.some(function(cell){ return cell.trim(); });
          }).map(function(row){
            var entry = Object.create(null);
            columns.forEach(function(key,i){ entry[key.trim()] = row[i] || ''; });
            return entry;
          });
        }
        if(!Array.isArray(rows)) throw new Error('Verträge fehlen');
        var imported = rows.map(normalize);
        var known = new Set(contracts().map(function(row){ return row.id; }));
        var fresh = imported.filter(function(row){
          if(known.has(row.id)) return false;
          known.add(row.id);
          return true;
        });
        if(!fresh.length){
          await app.message('Keine neuen Verträge in dieser Sicherung.');
          return false;
        }
        if(!await app.confirm(fresh.length+(fresh.length === 1 ? ' Vertrag' : ' Verträge')+' hinzufügen? Vorhandene Einträge bleiben erhalten.', {icon:'📥',confirmLabel:'Importieren'})) return false;
        var current = new Set(contracts().map(function(row){ return row.id; }));
        fresh = fresh.filter(function(row){ return !current.has(row.id); });
        contracts().push.apply(contracts(),fresh);
        renew();
        await app.saveState();
        render();
        checkReminders();
        return true;
      }catch(err){
        await app.message('Die Vertragsdatei enthält keine gültige Sicherung.', {icon:'⚠️'});
        return false;
      }
    }

    function getReminderSettings(){
      return {enabled:storage.getItem(noticeKey) === '1', days:Number(storage.getItem(daysKey)) || 10};
    }

    function restoreReminderSettings(settings){
      if(typeof settings.enabled !== 'boolean') return;
      var days = Number(settings.days);
      if(!Number.isInteger(days) || days < 1 || days > 10) days = 10;
      storage.setItem(noticeKey,settings.enabled ? '1' : '0');
      storage.setItem(daysKey,String(days));
      el('contractNotify').checked = settings.enabled;
      el('contractNotifyDays').value = String(days);
    }

    function checkReminders(){
      var bridge = window.AndroidBridge;
      var settings = getReminderSettings();
      if(!settings.enabled || !bridge || typeof bridge.showContractNotification !== 'function') return;
      deadlines().forEach(function(item){
        if(item.days < 0 || item.days > settings.days) return;
        var key = 'household-contract-notice:'+item.row.id;
        var today = todayISO();
        if(storage.getItem(key) === today) return;
        if(bridge.showContractNotification(item.row.id,item.row.name+' muss bis '+fmtDate(item.deadline)+' gekündigt werden.')){
          storage.setItem(key,today);
        }
      });
    }

    function init(){
      app.getState().contracts = contracts().map(function(row){
        row.minTerm = readTermValue(row.minTerm);
        row.notice = readTermValue(row.notice);
        row.autoRenewed = row.autoRenewed === true || row.autoRenewed === 'true';
        return row;
      });
      renew();
      el('contractList').addEventListener('click',async function(event){
        var btn = event.target.closest('[data-contract-action]');
        if(!btn) return;
        var id = btn.closest('[data-contract-id]').dataset.contractId;
        var row = contracts().find(function(entry){ return entry.id === id; });
        if(!row) return;
        if(btn.dataset.contractAction === 'edit') openForm(row);
        else if(await app.confirm('„'+row.name+'“ wirklich löschen?',{confirmLabel:'Löschen'})){
          app.getState().contracts = contracts().filter(function(entry){ return entry.id !== id; });
          await app.saveState();
          render();
        }
      });
      function recalcEnd(){
        var term = readTermValue(el('cMinTerm').value);
        if(term && el('cStart').value) el('cEnd').value = computeTermEnd(el('cStart').value,term);
      }
      el('cStart').addEventListener('change',recalcEnd);
      el('cMinTerm').addEventListener('change',recalcEnd);
      el('contractForm').addEventListener('submit',async function(event){
        event.preventDefault();
        if(!el('cName').value.trim()) return;
        var start = el('cStart').value, end = el('cEnd').value;
        if(end && end < start){ app.message('Das Enddatum liegt vor dem Startdatum.'); return; }
        var row = normalize({id:editId, name:el('cName').value, start:start, end:end,
          minTerm:el('cMinTerm').value, notice:el('cNotice').value, status:el('cStatus').value});
        var index = contracts().findIndex(function(entry){ return entry.id === editId; });
        if(index >= 0) contracts()[index] = row;
        else if(!editId) contracts().push(row);
        el('addContractOverlay').classList.remove('open');
        await app.saveState();
        render();
        checkReminders();
      });
      el('contractImport').addEventListener('click',function(){ el('contractImportFile').click(); });
      el('contractImportFile').addEventListener('change',async function(event){
        var file = event.target.files[0];
        event.target.value = '';
        if(!file) return;
        try{
          var contents = await file.text();
          el('settingsSubOverlay-vertraege').classList.remove('open');
          await importBackup(contents);
        }catch(err){ app.message('Die Datei konnte nicht gelesen werden.',{icon:'⚠️'}); }
      });
      var notify = el('contractNotify'), days = el('contractNotifyDays');
      for(var d=1;d<=10;d++) days.add(new Option(String(d),String(d)));
      days.value = storage.getItem(daysKey) || '10';
      notify.checked = storage.getItem(noticeKey) === '1';
      notify.disabled = !window.AndroidBridge || typeof window.AndroidBridge.requestContractNotificationPermission !== 'function';
      days.addEventListener('change',function(){ storage.setItem(daysKey,days.value); checkReminders(); });
      notify.addEventListener('change',function(){
        storage.setItem(noticeKey,notify.checked ? '1' : '0');
        if(notify.checked) window.AndroidBridge.requestContractNotificationPermission();
        checkReminders();
      });
      document.addEventListener('visibilitychange',function(){
        if(!document.hidden){ renew(); checkReminders(); }
      });
      window.checkHouseholdContractReminders = checkReminders;
      checkReminders();
    }
    return {init:init, render:render, openForm:openForm, importBackup:importBackup, isStandaloneBackup:isStandaloneBackup, normalize:normalize,
      getReminderSettings:getReminderSettings, restoreReminderSettings:restoreReminderSettings};
  };
})();
