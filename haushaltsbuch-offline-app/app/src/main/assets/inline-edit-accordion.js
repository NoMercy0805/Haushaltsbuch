/* Bearbeiten in derselben Karte wie "Rate buchen", Kontostand bleibt unveraendert. */
(function(){
  'use strict';
  if(window.__householdInlineEditInstalled)return;
  window.__householdInlineEditInstalled=true;
  var editActions={
    'edit':true,'edit-invoice':true,'edit-vc':true,
    'edit-tx':true,'edit-spar':true,'edit-loan':true,'edit-fuel':true
  };
  function makeCardPreview(original){
    if(!original)return null;
    var clone=original.cloneNode(true);
    clone.classList.add('inline-edit-card');
    clone.removeAttribute('id');
    if(clone.classList.contains('loan-card')||clone.classList.contains('vc-card')){
      var isLoan=clone.classList.contains('loan-card');
      Array.prototype.slice.call(clone.children).forEach(function(child){
        if(!child.matches(isLoan?'.loan-card-summary,.loan-sub':'.vc-card-top,.vc-detail-row'))child.remove();
      });
    }else{
      clone.querySelectorAll('.compact-actions,button,.invoice-check').forEach(function(el){el.remove();});
    }
    // Die Originalansicht soll ein ruhiger Kartenkopf bleiben, ohne klickbare Dubletten.
    clone.querySelectorAll('[data-action]').forEach(function(el){el.removeAttribute('data-action');});
    return clone;
  }
  function init(){
    var css=document.createElement('style');
    css.textContent=[
      '.inline-edit-card{cursor:default!important;overflow:visible!important}',
      '.inline-edit-card>.edit-row{width:100%;box-sizing:border-box;margin:14px 0 0;',
      'background:var(--primary-soft);border-radius:15px;padding:14px;',
      'animation:inline-edit-open .22s ease-out}',
      '.inline-edit-card.compact-list-row>.edit-row{flex:0 0 100%;min-width:0}',
      '.inline-edit-card.compact-list-row{border-radius:16px;background:var(--surface);',
      'margin:6px 0 12px;box-shadow:inset 0 0 0 1px var(--rule)}',
      '.inline-edit-card .edit-row .edit-actions{display:flex;grid-column:1/-1}',
      '.inline-edit-card .edit-row input{box-sizing:border-box;min-width:0}',
      '@keyframes inline-edit-open{from{opacity:.35;transform:translateY(-5px)}',
      'to{opacity:1;transform:translateY(0)}}',
      '@media(prefers-reduced-motion:reduce){.inline-edit-card>.edit-row{animation:none}}'
    ].join('');
    document.head.appendChild(css);
    document.addEventListener('click',function(event){
      var btn=event.target.closest('button[data-action]');
      if(!btn||!editActions[btn.dataset.action])return;
      var id=btn.dataset.id;
      var original=btn.closest('.compact-list-row,.vc-card,.loan-card');
      if(!id||!original)return;
      var card=makeCardPreview(original);
      if(!card)return;
      // Bestehende Bearbeiten-Handler erstellen zuerst die passende edit-row.
      // Anschliessend wird die edit-row als ausklappbarer Bereich IN die Karte gesetzt.
      setTimeout(function(){
        var row=null;
        document.querySelectorAll('.edit-row[data-id]').forEach(function(candidate){
          if(candidate.dataset.id===id&&candidate.isConnected)row=candidate;
        });
        if(!row||row.closest('.inline-edit-card'))return;
        row.replaceWith(card);
        card.appendChild(row);
      },0);
    },true);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);
  else init();
})();