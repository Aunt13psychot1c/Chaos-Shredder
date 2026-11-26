// Grey or Rainbow Bright, Light / Dark / System. Remembered on the phone.
(function(){
  var store={get:function(k){try{return localStorage.getItem(k)}catch(e){return null}},set:function(k,v){try{localStorage.setItem(k,v)}catch(e){}}};
  var dark=window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)');
  function apply(){
    var color=store.get('color')||'rainbow',mode=store.get('mode')||'system';
    var real=mode==='system'?(dark&&dark.matches?'dark':'light'):mode;
    var root=document.documentElement;root.dataset.color=color;root.dataset.mode=real;
    var bg=getComputedStyle(root).getPropertyValue('--bg').trim()||'#000000';
    if(window.Android&&Android.barColor)try{Android.barColor(bg.length===4?'#'+bg[1]+bg[1]+bg[2]+bg[2]+bg[3]+bg[3]:bg,real==='light')}catch(e){}
    document.querySelectorAll('[data-mode-pick]').forEach(function(b){b.classList.toggle('on',b.dataset.modePick===mode)});
    document.querySelectorAll('[data-color-pick]').forEach(function(b){b.classList.toggle('on',b.dataset.colorPick===color)});
  }
  if(dark&&dark.addEventListener)dark.addEventListener('change',apply);
  window.ChaosTheme={store:store,apply:apply,set:function(k,v){store.set(k,v);apply()}};
  apply();
})();
