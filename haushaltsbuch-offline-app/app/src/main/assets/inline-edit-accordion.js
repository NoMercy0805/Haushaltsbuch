/* Editieren klappt direkt unter dem Originaleintrag auf (Kontostand ausgenommen). */
(function(){
  'use strict';
  if(window.__inlineEditAccordionInstalled)return;
  window.__inlineEditAccordionInstalled=true;
  var actions={
    'edit':true,'edit-invoice':true,'edit-vc':true,
    'edit-tx':true,'edit-spar':true,'edit-loan':true,'edit-fuel':true
  };
  function createPreview(source){
    if(!source)return null;
    var clone=source.cloneNode(true);
    clone.removeAttribute('data-id');
    clone.removeAttribute('id');
    clone.classList.add('edit-accordion-preview');
    var wanted=source.classList.contains('loan-card')?
      '.loan-top,.loan-bar,.loan-sub':
      source.classList.contains('vc-card')?
      '.vc-card-top,.vc-detail-row':
      '.compact-name,.compact-name-wrap,.compact-amount,.compact-sub';
    if(source.classList.contains('vc-card')||source.classList.contains('loan-card')){
      Array.prototype.slice.call(clone.children).forEach(function(child){
        if(!child.matches(wanted))child.remove();
      });
    }else{
      clone.querySelectorAll('button,.compact-actions,.action-icon-btn-wrap').forEach(function(el){el.remove();});
    }
    clone.querySelectorAll('[id],[data-action],[data-id],[tabindex]').forEach(function(el){
      el.removeAttribute('id');el.removeAttribute('data-action');el.removeAttribute('data-id');el.removeAttribute('tabindex');
    });
    return clone;
  }
  function init(){
    var style=document.createElement('style');
    style.textContent=[
      '.edit-row.edit-accordion-open{overflow:visible;animation:edit-accordion-in .22s ease-out}',
      '.edit-row>.edit-accordion-preview{grid-column:1/-1;min-width:0;width:auto;',
      'margin:-16px -16px 8px;padding:14px 16px;border-radius:18px 18px 11px 11px;',
      'pointer-events:none;box-shadow:none}',
      '.edit-row>.edit-accordion-preview.compact-list-row{display:flex;border-bottom:1px solid var(--rule)}',
      '.edit-row>.edit-accordion-preview .vc-actions{display:none}',
      '@keyframes edit-accordion-in{from{opacity:.5;transform:translateY(-5px)}to{opacity:1;transform:translateY(0)}}',
      '@media(prefers-reduced-motion:reduce){.edit-row.edit-accordion-open{animation:none}}'
    ].join('');
    document.head.appendChild(style);
    document.addEventListener('click',function(event){
      var btn=event.target.closest('button[data-action]');
      if(!btn||!actions[btn.dataset.action])return;
      var id=btn.dataset.id;
      if(!id)return;
      var source=btn.closest('.vc-card,.loan-card,.compact-list-row');
      var preview=createPreview(source);
      if(!preview)return;
      // Die bestehende Editierlogik rendert synchron. Danach bleibt die
      // bisherige Karte als Kopf des aufklappenden Bearbeitungsfeldes sichtbar.
      setTimeout(function(){
        var rows=document.querySelectorAll('.edit-row[data-id]');
        var row=null;
        rows.forEach(function(candidate){if(candidate.dataset.id===id)row=candidate;});
        if(!row||row.querySelector('.edit-accordion-preview'))return;
        row.insertBefore(preview,row.firstChild);
        row.classList.add('edit-accordion-open');
      },0);
    },true);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);
  else init();
})();