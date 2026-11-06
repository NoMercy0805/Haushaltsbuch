/* Haushaltsbuch+: einheitliche Glas-Auswahl fuer native Select-Felder.
   Die echten Selects bleiben als Formularwerte erhalten; Android-Popups werden
   durch eine eigene Auswahl ersetzt. Die Kategorie und Monatsauswertung haben
   bereits eigene Picker und werden hier bewusst nicht veraendert. */
(function(){
  'use strict';
  if(window.__glassSelectsInstalled)return;
  window.__glassSelectsInstalled=true;
  var skipIds={betaCategoryInput:true};
  var activeSelect=null,activeTrigger=null;
  var sheet,optionsList;
  function init(){
    var css=document.createElement('style');
    css.textContent=[
      'select.app-glass-native{display:none!important}',
      '.app-glass-trigger{display:flex;align-items:center;justify-content:space-between;gap:8px;',
      'width:100%;min-width:0;min-height:48px;padding:12px 14px;border-radius:15px;',
      'border:1px solid rgba(255,255,255,.6);background:rgba(246,248,252,.83);',
      'color:var(--ink);font:600 15px var(--sans);text-align:left;cursor:pointer;',
      'backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);',
      'box-shadow:inset 0 1px 0 rgba(255,255,255,.55),0 4px 13px rgba(0,0,0,.07)}',
      '.app-glass-trigger.full{grid-column:1/-1}',
      '.app-glass-trigger .app-glass-value{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}',
      '.app-glass-trigger .app-glass-arrow{width:18px;height:18px;flex:none;color:var(--ink-soft)}',
      '.app-glass-trigger:disabled{opacity:.55;cursor:default}',
      'html[data-theme="dark"] .app-glass-trigger{background:rgba(52,57,68,.91);',
      'border-color:rgba(255,255,255,.20);',
      'box-shadow:inset 0 1px 0 rgba(255,255,255,.11),0 4px 13px rgba(0,0,0,.18)}',
      '.app-glass-overlay{position:fixed;inset:0;z-index:22000;display:none;',
      'align-items:flex-end;justify-content:center;padding:0 12px 12px;',
      'box-sizing:border-box;background:rgba(10,13,20,.48)}',
      '.app-glass-overlay.open{display:flex}',
      '.app-glass-picker{display:flex;flex-direction:column;min-width:0;',
      'width:min(100%,400px);max-height:min(75dvh,680px);overflow:hidden;',
      'padding:13px 14px calc(14px + env(safe-area-inset-bottom,0px));',
      'border-radius:22px;background:rgba(246,247,250,.97);',
      'border:1px solid rgba(255,255,255,.65);',
      'backdrop-filter:blur(22px);-webkit-backdrop-filter:blur(22px);',
      'box-shadow:0 -12px 42px rgba(0,0,0,.22);color:var(--ink)}',
      'html[data-theme="dark"] .app-glass-picker{background:rgba(43,47,55,.98);',
      'border-color:rgba(255,255,255,.18);box-shadow:0 -12px 42px rgba(0,0,0,.38)}',
      '.app-glass-head{display:flex;align-items:center;justify-content:space-between;',
      'gap:10px;padding:4px 4px 13px;font-size:16px;font-weight:800}',
      '.app-glass-close{flex:none;width:34px;height:34px;border:0;border-radius:50%;',
      'background:var(--surface-2);color:var(--ink);font-size:24px;cursor:pointer}',
      '.app-glass-options{display:flex;flex-direction:column;gap:6px;overflow-y:auto;',
      'overscroll-behavior:contain;-webkit-overflow-scrolling:touch;padding:2px 0 6px}',
      '.app-glass-option{display:flex;align-items:center;justify-content:space-between;',
      'width:100%;min-height:48px;gap:12px;padding:11px 14px;text-align:left;',
      'border-radius:13px;border:1px solid rgba(255,255,255,.46);background:var(--surface);',
      'color:var(--ink);font:650 14px var(--sans);cursor:pointer}',
      'html[data-theme="dark"] .app-glass-option{border-color:rgba(255,255,255,.12)}',
      '.app-glass-option.selected{background:var(--selection-bg);',
      'border-color:var(--selection-ring);color:var(--selection-text)}',
      '.app-glass-option span:first-child{flex:1;min-width:0}',
      '.app-glass-check{flex:none;width:22px;text-align:right;font-weight:800;font-size:18px}'
    ].join('');
    document.head.appendChild(css);
    sheet=document.createElement('div');
    sheet.id='appGlassSelectOverlay';
    sheet.className='app-glass-overlay';
    sheet.setAttribute('aria-hidden','true');
    sheet.innerHTML='<div class="app-glass-picker" role="dialog" aria-modal="true" aria-label="Auswahl">'+
      '<div class="app-glass-head"><strong id="appGlassSelectTitle">Auswählen</strong>'+
      '<button type="button" class="app-glass-close" aria-label="Schließen">×</button></div>'+
      '<div class="app-glass-options" id="appGlassSelectOptions" role="listbox"></div></div>';
    document.body.appendChild(sheet);
    optionsList=sheet.querySelector('.app-glass-options');
    sheet.querySelector('.app-glass-close').addEventListener('click',close);
    sheet.addEventListener('click',function(e){if(e.target===sheet)close();});
    optionsList.addEventListener('click',function(e){
      var choice=e.target.closest('[data-glass-index]');
      if(!choice||!activeSelect)return;
      var option=activeSelect.options[Number(choice.dataset.glassIndex)];
      if(option&&!option.disabled){
        activeSelect.value=option.value;
        activeSelect.dispatchEvent(new Event('input',{bubbles:true}));
        activeSelect.dispatchEvent(new Event('change',{bubbles:true}));
        sync(activeSelect);
      }
      close();
    });
    document.addEventListener('keydown',function(e){
      if(e.key==='Escape'&&sheet.classList.contains('open')){
        e.preventDefault();e.stopImmediatePropagation();close();
      }
    },true);
    // Dynamisch erzeugte Editier-Formulare werden ebenfalls automatisch angepasst.
    document.querySelectorAll('select').forEach(enhance);
    var observer=new MutationObserver(function(records){
      records.forEach(function(record){
        record.addedNodes.forEach(function(node){
          if(node.nodeType!==1)return;
          if(node.matches&&node.matches('select'))enhance(node);
          if(node.querySelectorAll)node.querySelectorAll('select').forEach(enhance);
        });
      });
    });
    observer.observe(document.body,{childList:true,subtree:true});
    document.addEventListener('change',function(e){
      if(e.target.tagName==='SELECT'&&e.target.classList.contains('app-glass-native'))sync(e.target);
    },true);
    // Programmgesteuerte Zuweisungen an select.value sind ohne Event moeglich.
    // Deshalb wird der sichtbare Wert vor jedem Oeffnen frisch gelesen.
  }
  function enhance(select){
    if(!select||select.classList.contains('app-glass-native')||skipIds[select.id]||
       select.closest('.app-glass-overlay')||select.multiple||select.size>1)return;
    var trigger=document.createElement('button');
    trigger.type='button';
    trigger.className='app-glass-trigger'+(select.classList.contains('full')?' full':'');
    trigger.setAttribute('aria-haspopup','listbox');
    trigger.setAttribute('aria-expanded','false');
    trigger.setAttribute('aria-label',select.getAttribute('aria-label')||labelFor(select)||'Auswahl öffnen');
    trigger.innerHTML='<span class="app-glass-value"></span>'+
      '<svg class="app-glass-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>';
    select.classList.add('app-glass-native');
    select.setAttribute('tabindex','-1');
    select.insertAdjacentElement('afterend',trigger);
    select._glassTrigger=trigger;
    trigger.addEventListener('click',function(){open(select,trigger);});
    sync(select);
  }
  function labelFor(select){
    if(select.id){
      var label=document.querySelector('label[for="'+select.id.replace(/"/g,'')+'"]');
      if(label)return label.textContent.trim();
    }
    var previous=select.previousElementSibling;
    if(previous&&previous.tagName==='LABEL')return previous.textContent.trim();
    return '';
  }
  function sync(select){
    var trigger=select._glassTrigger;
    if(!trigger)return;
    var option=select.options[select.selectedIndex];
    trigger.querySelector('.app-glass-value').textContent=option?option.textContent.trim():'Bitte auswählen';
    trigger.disabled=select.disabled;
  }
  function open(select,trigger){
    if(select.disabled)return;
    activeSelect=select;activeTrigger=trigger;sync(select);
    var text=labelFor(select)||select.getAttribute('aria-label')||'Auswählen';
    document.getElementById('appGlassSelectTitle').textContent=text;
    optionsList.replaceChildren();
    Array.prototype.forEach.call(select.options,function(option,index){
      if(option.hidden)return;
      var button=document.createElement('button');
      button.type='button';
      button.className='app-glass-option'+(index===select.selectedIndex?' selected':'');
      button.dataset.glassIndex=String(index);
      button.setAttribute('role','option');
      button.setAttribute('aria-selected',index===select.selectedIndex?'true':'false');
      button.disabled=option.disabled;
      var title=document.createElement('span');title.textContent=option.textContent.trim();
      var check=document.createElement('span');check.className='app-glass-check';
      check.textContent=index===select.selectedIndex?'✓':'';
      button.appendChild(title);button.appendChild(check);optionsList.appendChild(button);
    });
    sheet.querySelector('.app-glass-picker').style.width=
      Math.max(200,Math.min(trigger.getBoundingClientRect().width,window.innerWidth-24))+'px';
    sheet.classList.add('open');sheet.setAttribute('aria-hidden','false');
    trigger.setAttribute('aria-expanded','true');
    var selected=optionsList.querySelector('.selected');
    if(selected)selected.scrollIntoView({block:'nearest'});
  }
  function close(){
    if(!sheet)return;
    sheet.classList.remove('open');sheet.setAttribute('aria-hidden','true');
    if(activeTrigger&&activeTrigger.isConnected){
      activeTrigger.setAttribute('aria-expanded','false');
      activeTrigger.focus({preventScroll:true});
    }
    activeSelect=null;activeTrigger=null;
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();