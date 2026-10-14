(function(){
  if(window.__betaEntryActionsV3Installed) return;
  window.__betaEntryActionsV3Installed = true;

  // Der alte Android-Hook darf nicht zusaetzlich installiert werden. Er kann
  // nicht auf den innerhalb der index.html-IIFE liegenden `state` zugreifen
  // und wuerde deshalb Klicks abfangen, ohne danach etwas auszufuehren.
  var betaList = document.getElementById('betaList');
  if(betaList) betaList.__androidExpandableActionsInstalled = true;

  // Gleiche Aufklapp-Animation wie bei den kompakten Listen/Rechnungen:
  // Hoehe, Transparenz und Abstand werden weich animiert, statt die Aktionen
  // hart ueber display:none / display:flex ein- und auszublenden.
  if(!document.getElementById('betaEntryActionsV3Style')){
    var style = document.createElement('style');
    style.id = 'betaEntryActionsV3Style';
    style.textContent =
      '.beta-entry-row{flex-wrap:wrap;cursor:pointer;}' +
      '.beta-entry-row .beta-entry-actions-v3{' +
        'display:flex;justify-content:flex-end;gap:8px;flex-basis:100%;width:100%;' +
        'max-height:0;opacity:0;overflow:hidden;margin-top:0;pointer-events:none;' +
        'transition:max-height .2s ease,opacity .2s ease,margin-top .2s ease;' +
      '}' +
      '.beta-entry-row[data-beta-actions-open-v3="1"] .beta-entry-actions-v3{' +
        'max-height:78px;opacity:1;margin-top:10px;pointer-events:auto;' +
      '}';
    document.head.appendChild(style);
  }

  function makeActions(row){
    var existing = row.querySelector('.beta-entry-actions-v3');
    if(existing) return existing;

    var actions = document.createElement('span');
    actions.className = 'beta-entry-actions-v3';
    actions.innerHTML =
      '<span class="action-icon-btn-wrap" data-beta-row-action-v3="edit">'+
        '<button type="button" class="action-icon-btn neutral" data-beta-row-action-v3="edit" aria-label="Bearbeiten" title="Bearbeiten">'+
          '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>'+
        '</button><span class="action-icon-btn-label">Bearbeiten</span>'+
      '</span>'+
      '<span class="action-icon-btn-wrap" data-beta-row-action-v3="delete">'+
        '<button type="button" class="action-icon-btn danger" data-beta-row-action-v3="delete" aria-label="Löschen" title="Löschen">'+
          '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>'+
        '</button><span class="action-icon-btn-label">Löschen</span>'+
      '</span>';
    row.appendChild(actions);
    return actions;
  }

  function closeOtherRows(exceptRow){
    var list = document.getElementById('betaList');
    if(!list) return;
    Array.prototype.forEach.call(list.querySelectorAll('.beta-entry-row'), function(r){
      if(r === exceptRow) return;
      r.removeAttribute('data-beta-actions-open-v3');
    });
  }

  function scheduleDeleteAfterOriginalEdit(){
    var token = String(Date.now()) + Math.random();
    window.__betaPendingDeleteV3 = token;
    var started = Date.now();

    function check(){
      if(window.__betaPendingDeleteV3 !== token) return;
      var overlay = document.getElementById('addBetaOverlay');
      var deleteButton = document.getElementById('betaDeleteBtn');
      var overlayOpen = overlay && overlay.classList.contains('visible');
      var deleteVisible = deleteButton && deleteButton.style.display !== 'none';

      if(overlayOpen && deleteVisible){
        window.__betaPendingDeleteV3 = null;
        setTimeout(function(){ deleteButton.click(); }, 0);
        return;
      }

      if(Date.now() - started < 30000){
        setTimeout(check, 100);
      } else if(window.__betaPendingDeleteV3 === token){
        window.__betaPendingDeleteV3 = null;
      }
    }

    setTimeout(check, 0);
  }

  document.addEventListener('click', function(e){
    var row = e.target.closest && e.target.closest('.beta-entry-row');
    if(!row) return;
    var list = document.getElementById('betaList');
    if(!list || !list.contains(row)) return;

    // Der Abgleich-Kreis bleibt komplett beim vorhandenen Original-Handler.
    if(e.target.closest('[data-action="toggle-reconciled"]')) return;

    var actionTarget = e.target.closest('[data-beta-row-action-v3]');
    if(actionTarget){
      var action = actionTarget.getAttribute('data-beta-row-action-v3');

      // Bearbeiten wird bewusst NICHT abgefangen. Der bereits vorhandene
      // betaList-Handler in index.html ermittelt den Eintrag aus seinem lokalen
      // State und zeigt bei Dauerauftraegen die Auswahl "diesen"/"folgende".
      if(action === 'edit') return;

      // Fuer Loeschen darf ebenfalls zuerst der Original-Bearbeiten-Handler
      // laufen. So bleibt auch hier die Dauerauftrag-Auswahl erhalten. Sobald
      // das Original-Formular geoeffnet ist, wird dessen bestehender
      // Loeschen-Button ausgeloest und damit die vorhandene Loeschlogik genutzt.
      if(action === 'delete'){
        scheduleDeleteAfterOriginalEdit();
        return;
      }
    }

    // Nur der nackte Zeilenklick wird abgefangen: statt direkt zu bearbeiten
    // werden die beiden Aktionen auf-/zugeklappt.
    e.preventDefault();
    e.stopPropagation();
    if(e.stopImmediatePropagation) e.stopImmediatePropagation();

    makeActions(row);
    var isOpen = row.getAttribute('data-beta-actions-open-v3') === '1';
    closeOtherRows(row);
    if(isOpen){
      row.removeAttribute('data-beta-actions-open-v3');
    } else {
      row.setAttribute('data-beta-actions-open-v3','1');
    }
  }, true);
})();
