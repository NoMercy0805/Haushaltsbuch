from pathlib import Path
p=Path('haushaltsbuch-offline-app/app/src/main/assets/index.html')
s=p.read_text(encoding='utf-8')

start=s.index("    document.getElementById('betaTitleSuggestionsSettings').addEventListener('click', function(e){")
end=s.index("      saveState();", start)+len("      saveState();\n")
handler="""    document.getElementById('betaTitleSuggestionsSettings').addEventListener('click', function(e){
      var titleBtn = e.target.closest('[data-action=\"delete-beta-title\"]');
      if(!titleBtn) return;
      var title = titleBtn.dataset.title;
      if(state.betaHiddenTitles.indexOf(title) === -1) state.betaHiddenTitles.push(title);
      saveState();
"""
s=s[:start]+handler+s[end:]

start=s.index("    var activeTitles = allTitles.filter(function(t){ return state.betaHiddenTitles.indexOf(t) === -1; })")
end=s.index("    container.innerHTML = html;", start)
replacement="""    var activeTitles = allTitles.filter(function(t){ return state.betaHiddenTitles.indexOf(t) === -1; }).sort(function(a,b){ return a.localeCompare(b, 'de'); });
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
s=s[:start]+replacement+s[end:]

old="""      saveState();
      generateBetaRecurringOccurrences();
      document.getElementById('addBetaOverlay').classList.remove('open');
"""
new="""      state.betaHiddenTitles = state.betaHiddenTitles.filter(function(t){ return t !== title; });
      saveState();
      generateBetaRecurringOccurrences();
      document.getElementById('addBetaOverlay').classList.remove('open');
"""
if old not in s: raise SystemExit('submit save block not found')
s=s.replace(old,new,1)

s=s.replace('Nicht mehr benötigte Vorschläge (z.B. alte Titel wie \"O2\") lassen sich hier entfernen, ohne die dazugehörigen Einträge zu löschen.','Nicht mehr benötigte Vorschläge lassen sich hier vollständig aus der Vorschlagsliste entfernen. Wenn derselbe Titel später erneut verwendet wird, erscheint er automatisch wieder als Vorschlag.',1)
p.write_text(s,encoding='utf-8')
