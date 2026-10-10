from pathlib import Path
p=Path('haushaltsbuch-offline-app/app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')
old="""    document.getElementById('betaTitleSuggestionsSettings').addEventListener('click', function(e){
      var titleBtn = e.target.closest('[data-action=\"hide-beta-title\"], [data-action=\"restore-beta-title\"]');
      if(!titleBtn) return;
      var title = titleBtn.dataset.title;
      if(titleBtn.dataset.action === 'hide-beta-title'){
        if(state.betaHiddenTitles.indexOf(title) === -1) state.betaHiddenTitles.push(title);
      } else {
        state.betaHiddenTitles = state.betaHiddenTitles.filter(function(t){ return t !== title; });
      }
      saveState();
"""
new="""    document.getElementById('betaTitleSuggestionsSettings').addEventListener('click', function(e){
      var titleBtn = e.target.closest('[data-action=\"delete-beta-title\"]');
      if(!titleBtn) return;
      var title = titleBtn.dataset.title;
      if(state.betaHiddenTitles.indexOf(title) === -1) state.betaHiddenTitles.push(title);
      saveState();
"""
if old not in s: raise SystemExit('handler block not found')
s=s.replace(old,new,1)
old2="""    var activeTitles = allTitles.filter(function(t){ return state.betaHiddenTitles.indexOf(t) === -1; }).sort(function(a,b){ return a.localeCompare(b, 'de'); });
    var hiddenTitles = state.betaHiddenTitles.slice().sort(function(a,b){ return a.localeCompare(b, 'de'); });
    if(headerSub) headerSub.textContent = activeTitles.length + ' aktiv'+(hiddenTitles.length ? ', '+hiddenTitles.length+' ausgeblendet' : '');

    function row(title, hidden){
      return '<div class=\"compact-list-row\" style=\"cursor:default;\">'+
               '<span class=\"compact-name-wrap\"><span class=\"compact-name\" style=\"'+(hidden?'color:var(--ink-soft);':'')+'\">'+escapeHtml(title)+'</span></span>'+ 
               '<button type=\"button\" class=\"action-icon-btn '+(hidden?'gold':'danger')+'\" data-action=\"'+(hidden?'restore-beta-title':'hide-beta-title')+'\" data-title=\"'+escapeHtml(title)+'\" style=\"margin-left:8px;\" aria-label=\"'+(hidden?'Wiederherstellen':'Ausblenden')+'\">'+
                 (hidden
                   ? '<svg viewBox=\"0 0 24 24\" width=\"15\" height=\"15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 12a9 9 0 1 0 2.6-6.3L3 8\"/><path d=\"M3 3v5h5\"/></svg>'
                   : '<svg viewBox=\"0 0 24 24\" width=\"15\" height=\"15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M6 6l12 12M18 6L6 18\"/></svg>')+
               '</button>'+ 
             '</div>';
    }

    var html = '';
    if(activeTitles.length === 0 && hiddenTitles.length === 0){
      html = '<div class=\"empty-state\">Noch keine Titel vorhanden.</div>';
    } else {
      html += '<div class=\"compact-list\">'+(activeTitles.length ? activeTitles.map(function(t){ return row(t, false); }).join('') : '<div class=\"empty-state\">Alle Titel sind ausgeblendet.</div>')+'</div>';
      if(hiddenTitles.length > 0){
        html += '<p class=\"chooser-group-label\" style=\"margin-top:12px;font-size:12px;\">Ausgeblendet</p>'+ 
                '<div class=\"compact-list\">'+hiddenTitles.map(function(t){ return row(t, true); }).join('')+'</div>';
      }
    }
"""
new2="""    var activeTitles = allTitles.filter(function(t){ return state.betaHiddenTitles.indexOf(t) === -1; }).sort(function(a,b){ return a.localeCompare(b, 'de'); });
    if(headerSub) headerSub.textContent = activeTitles.length + ' Vorschläge';

    function row(title){
      return '<div class=\"compact-list-row\" style=\"cursor:default;\">'+
               '<span class=\"compact-name-wrap\"><span class=\"compact-name\">'+escapeHtml(title)+'</span></span>'+ 
               '<button type=\"button\" class=\"action-icon-btn danger\" data-action=\"delete-beta-title\" data-title=\"'+escapeHtml(title)+'\" style=\"margin-left:8px;\" aria-label=\"Entfernen\">'+
                 '<svg viewBox=\"0 0 24 24\" width=\"15\" height=\"15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M6 6l12 12M18 6L6 18\"/></svg>'+
               '</button>'+ 
             '</div>';
    }

    var html = activeTitles.length
      ? '<div class=\"compact-list\">'+activeTitles.map(function(t){ return row(t); }).join('')+'</div>'
      : '<div class=\"empty-state\">Noch keine Titel-Vorschläge vorhanden.</div>';
"""
if old2 not in s: raise SystemExit('render block not found')
s=s.replace(old2,new2,1)
old3="""      saveState();
      generateBetaRecurringOccurrences();
      document.getElementById('addBetaOverlay').classList.remove('open');
"""
new3="""      state.betaHiddenTitles = state.betaHiddenTitles.filter(function(t){ return t !== title; });
      saveState();
      generateBetaRecurringOccurrences();
      document.getElementById('addBetaOverlay').classList.remove('open');
"""
if old3 not in s: raise SystemExit('submit save block not found')
s=s.replace(old3,new3,1)
s=s.replace('Nicht mehr benötigte Vorschläge (z.B. alte Titel wie \"O2\") lassen sich hier entfernen, ohne die dazugehörigen Einträge zu löschen.','Nicht mehr benötigte Vorschläge lassen sich hier vollständig aus der Vorschlagsliste entfernen. Wenn derselbe Titel später erneut verwendet wird, erscheint er automatisch wieder als Vorschlag.',1)
p.write_text(s,encoding='utf-8')
