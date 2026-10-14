(function(){
  if(window.__betaEntryActionsV2Installed) return;
  window.__betaEntryActionsV2Installed = true;

  function findEntry(id){
    if(!window.state || !Array.isArray(state.betaEntries)) return null;
    return state.betaEntries.filter(function(x){ return String(x.id) === String(id); })[0] || null;
  }

  function makeActions(row){
    var existing = row.querySelector('.beta-entry-actions-v2');
    if(existing) return existing;

    row.style.flexWrap = 'wrap';
    row.style.cursor = 'pointer';

    var actions = document.createElement('span');
    actions.className = 'beta-entry-actions-v2';
    actions.style.cssText = 'display:none;justify-content:flex-end;gap:8px;flex-basis:100%;width:100%;margin-top:10px;';
    actions.innerHTML =
      '<span class="action-icon-btn-wrap">'+
        '<button type="button" class="action-icon-btn neutral" data-beta-row-action-v2="edit" aria-label="Bearbeiten" title="Bearbeiten">'+
          '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>'+
        '</button><span class="action-icon-btn-label">Bearbeiten</span>'+
      '</span>'+
      '<span class="action-icon-btn-wrap">'+
        '<button type="button" class="action-icon-btn danger" data-beta-row-action-v2="delete" aria-label="Löschen" title="Löschen">'+
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
      var a = r.querySelector('.beta-entry-actions-v2');
      if(a) a.style.display = 'none';
      r.removeAttribute('data-beta-actions-open-v2');
    });
  }

  document.addEventListener('click', async function(e){
    var row = e.target.closest && e.target.closest('.beta-entry-row');
    if(!row) return;
    var list = document.getElementById('betaList');
    if(!list || !list.contains(row)) return;

    if(e.target.closest('[data-action="toggle-reconciled"]')) return;

    e.preventDefault();
    e.stopPropagation();
    if(e.stopImmediatePropagation) e.stopImmediatePropagation();

    var entry = findEntry(row.getAttribute('data-id'));
    if(!entry) return;

    var actionButton = e.target.closest('[data-beta-row-action-v2]');
    if(!actionButton){
      var actions = makeActions(row);
      var isOpen = row.getAttribute('data-beta-actions-open-v2') === '1';
      closeOtherRows(row);
      if(isOpen){
        actions.style.display = 'none';
        row.removeAttribute('data-beta-actions-open-v2');
      } else {
        actions.style.display = 'flex';
        row.setAttribute('data-beta-actions-open-v2','1');
      }
      return;
    }

    var action = actionButton.getAttribute('data-beta-row-action-v2');
    var scope = 'this';

    if(entry.isRecurringTemplate || entry.recurringGroupId){
      scope = await betaRecurringChoiceDialog();
      if(!scope) return;
    }

    if(action === 'edit'){
      openBetaEditForm(entry, scope);
      return;
    }

    if(action === 'delete'){
      editingBetaId = entry.id;
      editingBetaScope = scope;
      var deleteButton = document.getElementById('betaDeleteBtn');
      if(deleteButton) deleteButton.click();
    }
  }, true);
})();
