
const icons={
 search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/>',
 music:'<path d="M9 18V5l11-2v13M9 9l11-2"/><ellipse cx="6" cy="18" rx="3" ry="2.5"/><ellipse cx="17" cy="16" rx="3" ry="2.5"/>',
 heart:'<path d="M20.8 4.8a5.3 5.3 0 0 0-7.5 0L12 6.1l-1.3-1.3a5.3 5.3 0 0 0-7.5 7.5L12 21l8.8-8.7a5.3 5.3 0 0 0 0-7.5Z"/>',
 download:'<path d="M12 3v12m-5-5 5 5 5-5M4 16v4h16v-4"/>',
 settings:'<path d="M9.28 5.44L10.16 5.14L10.13 3.20L13.87 3.20L13.84 5.14L14.72 5.44L14.72 5.44L15.55 5.85L16.90 4.45L19.55 7.10L18.15 8.45L18.56 9.28L18.56 9.28L18.86 10.16L20.80 10.13L20.80 13.87L18.86 13.84L18.56 14.72L18.56 14.72L18.15 15.55L19.55 16.90L16.90 19.55L15.55 18.15L14.72 18.56L14.72 18.56L13.84 18.86L13.87 20.80L10.13 20.80L10.16 18.86L9.28 18.56L9.28 18.56L8.45 18.15L7.10 19.55L4.45 16.90L5.85 15.55L5.44 14.72L5.44 14.72L5.14 13.84L3.20 13.87L3.20 10.13L5.14 10.16L5.44 9.28L5.44 9.28L5.85 8.45L4.45 7.10L7.10 4.45L8.45 5.85L9.28 5.44Z"/><circle cx="12" cy="12" r="3.1"/>',
 play:'<path d="m8 4 13 8-13 8Z" fill="currentColor" stroke="none"/>',
 pause:'<path d="M7 5h3v14H7zM14 5h3v14h-3z" fill="currentColor" stroke="none"/>',
 next:'<path d="m5 5 11 7-11 7Z" fill="currentColor" stroke="none"/><path d="M19 5v14" stroke-width="2.5"/>',
 prev:'<path d="m19 5-11 7 11 7Z" fill="currentColor" stroke="none"/><path d="M5 5v14" stroke-width="2.5"/>',
 shuffle:'<path d="M3 6h3c5 0 7 12 12 12h3m-3-3 3 3-3 3M3 18h3c1.5 0 3-2 4-4m3-4c1.5-2.5 3-4 5-4h3m-3-3 3 3-3 3"/>',
 repeat:'<path d="m17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4m14-1v2a3 3 0 0 1-3 3H3"/>',
 more:'<circle cx="12" cy="4.5" r="1" fill="currentColor"/><circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="19.5" r="1" fill="currentColor"/>',
 down:'<path d="m6 9 6 6 6-6"/>',
 arrow:'<path d="M19 12H5m6-6-6 6 6 6"/>',
 queue:'<path d="M3 5h17M3 10h17M3 15h10m4-1 5 4-5 4Z"/>',
 sort:'<path d="M4 5h16M4 10h11M4 15h7M4 20h3m11-8v9m-3-3 3 3 3-3"/>',
 plus:'<path d="M12 4v16M4 12h16"/>',
 check:'<path d="m5 12 4 4L20 5"/>',
 sun:'<circle cx="12" cy="12" r="4"/><path d="M12 1v2m0 18v2M1 12h2m18 0h2M4 4l1.5 1.5m13 13L20 20M4 20l1.5-1.5m13-13L20 4"/>',
 moon:'<path d="M20.5 14A9 9 0 0 1 10 3.5 9 9 0 1 0 20.5 14Z"/>',
 system:'<rect x="3" y="3" width="18" height="14" rx="2"/><path d="M8 21h8m-4-4v4M12 3v14"/>',
 volume:'<path d="m11 4-6 5H2v6h3l6 5Zm4 4a6 6 0 0 1 0 8m3-11a10 10 0 0 1 0 14"/>',
 wifi:'<path d="M3 9a14 14 0 0 1 18 0M6 12a9 9 0 0 1 12 0m-9 3a4 4 0 0 1 6 0M12 19h.01"/>',
 bars:'<path d="M3 20v-3m5 3v-7m5 7V9m5 11V5" stroke-width="2.5"/>'
};
const icon=(name,cls='')=>`<svg class="icon ${cls}" viewBox="0 0 24 24" aria-hidden="true">${icons[name]||icons.music}</svg>`;
const ib=(name,action,label,cls='')=>`<button class="iconbutton ${cls}" data-action="${action}" aria-label="${label}">${icon(name)}</button>`;
const colors=[{name:'Красный',value:'#ff354c'},{name:'Жёлтый',value:'#f4ef00'},{name:'Зелёный',value:'#4bdb87'},{name:'Синий',value:'#4e9eff'}];
const modes=[{name:'Тёмная',value:'dark',icon:'moon'},{name:'Светлая',value:'light',icon:'sun'},{name:'Как в системе',value:'system',icon:'system'}];
const tracks=[];
let saved={};try{saved=JSON.parse(localStorage.getItem('maxmusic-look')||'{}')}catch{}
const state={mode:modes.some(m=>m.value===saved.mode)?saved.mode:'dark',accent:colors.some(c=>c.value===saved.accent)?saved.accent:'#f4ef00',view:'library',tab:'tracks',index:0,playing:true,seek:36,query:'',favorite:true,repeat:false,shuffle:false};
const logo=()=>{
 const max='<path d="M-86 110-25 76-14 75-10 67 20 51 26 51 47 33 62 28 65 22 73 50 70 57 82 61 123 21 151 4 164-4 147 32 139 36 109 118 85 127 111 56 78 98 63 90 55 54 40 74 28 81-40 105-33 98Z"/><path fill-rule="evenodd" d="M110 137 180 42 211 15 229 107 207 117 201 91 157 98 135 127 122 128ZM172 77 199 73 194 44Z"/><path d="M224 39 250 34 269 57 330-3 352-9 388-44 335 26 325 30 284 78 338 119 392 152 339 138 308 122 265 96 231 143 212 150 190 180 220 120 248 82Z"/>';
 const glyph={M:'M0 64 10 0H24L32 29 52 0H67L56 64H42L49 25 32 51H24L17 25 10 64Z',U:'M7 0H22L15 46H34L41 0H56L47 56L38 64H9L0 54Z',S:'M8 0H54L51 13H20L18 26H42L49 34L44 55L34 64H-4L-1 50H29L31 37H8L0 29L5 6Z',I:'M12 0H27L16 64H1Z',C:'M17 0H56L53 14H24L17 50H47L44 64H7L0 54L9 10Z'};
 let x=0;const music=[['M',68],['U',58],['S',53],['I',29],['C',57]].map(([letter,width])=>{const p=`<path transform="translate(${x} 0)" d="${glyph[letter]}"/>`;x+=width+4;return p}).join('');
 return `<svg viewBox="-96 -54 736 248" role="img" aria-label="MaxMusic"><g class="logo-depth" fill="#1396b0" opacity=".65" transform="translate(1 4)">${max}</g><g class="logo-depth" fill="#55e5ee" opacity=".85" transform="translate(.5 2)">${max}</g><g fill="currentColor">${max}<g transform="translate(385 47) scale(.82)">${music}</g><path d="m117 143 86-24-9 8-48 14-33 13-16 2Zm272-22 105-6-7 4-80 5-16 7-17 1Z"/></g><path d="m-21 82 35-14-4 4-34 14Zm23 4 26-9-4 4-25 9Zm153 1 41-6-5 4-39 6Zm103 2 30 5-3 3-29-5Zm157-12 124-2-1 2-124 2Z" fill="var(--bg,#0b0d11)" opacity=".85"/><path d="m47 34 14-5m123 11 24-21m123-20 20-7" fill="none" stroke="#fffbd0" stroke-width="1.3" opacity=".65"/></svg>`;
};
let artworkSerial=0;
function art(palette){const gradientId='chrome-'+(++artworkSerial);const designs=[
 `<rect width="300" height="300" fill="#d7d5c8"/><path d="M0 130 150 40 300 105v195H0Z" fill="#454846"/><path d="m148 40 9 156 143 104V105Z" fill="#292d2d"/><path d="m0 130 148-90 9 156L0 270Z" fill="#8e9189"/><g stroke="#414944" stroke-width="7">${Array.from({length:10},(_,i)=>`<path d="M${i*17} ${132-i*10}v${132+i*5}"/>`).join('')}</g><g stroke="#929a8f" stroke-width="4">${Array.from({length:7},(_,i)=>`<path d="m163 ${77+i*22} 137 ${66-i*2}"/>`).join('')}</g><text x="18" y="26" fill="#323833" font-size="11" font-family="Arial" letter-spacing="5">МОЛЧАТ ДОМА</text><rect y="0" width="300" height="300" fill="#ffffff09"/>`,
 `<rect width="300" height="300" fill="#111b2a"/><circle cx="155" cy="118" r="72" fill="#e97950"/><path d="M0 219 74 146 149 242 218 173 300 235v65H0Z" fill="#122e3c"/><path d="m0 262 100-53 93 47 107-26v70H0Z" fill="#0b151e"/><g stroke="#f1bb85" opacity=".6"><path d="m40 55 2 2m191 14 1 1m29 58 2 1m-155-93 1 1m-84 122 1 1"/></g><text x="18" y="278" fill="#d7d7c6" font-size="15" font-family="Arial" letter-spacing="8">КИНО</text>`,
 `<rect width="300" height="300" fill="#160b31"/><defs><linearGradient id="${gradientId}" x2="1" y2="1"><stop stop-color="#bbd6e2"/><stop offset=".3" stop-color="#fff8ee"/><stop offset=".45" stop-color="#9861de"/><stop offset=".6" stop-color="#091626"/><stop offset=".8" stop-color="#70a4bd"/><stop offset="1" stop-color="#e7dfc1"/></linearGradient></defs><ellipse cx="150" cy="150" rx="111" ry="90" fill="none" stroke="url(#${gradientId})" stroke-width="36" transform="rotate(-27 150 150)"/><path d="m74 175 152-67" stroke="#f9908a" stroke-width="3"/><text x="17" y="278" fill="#cfb8f2" font-size="10" font-family="Arial" letter-spacing="7">DISCOVERY</text>`,
 `<rect width="300" height="300" fill="#c6d3d8"/><path d="m0 200 65-90 38 49 44-132 62 102 91-21v192H0Z" fill="#33414f"/><path d="m0 220 65-110 38 49 44-132 3 139 59-37 91-21-95 126Z" fill="#dde0db"/><path d="m0 240 71-56 75 32 65-68 89 59v93H0Z" fill="#172432"/><path d="M0 263h300" stroke="#f35545" stroke-width="6"/><text x="16" y="24" fill="#283441" font-size="10" font-family="Arial" letter-spacing="5">RADIOHEAD</text>`,
 `<rect width="300" height="300" fill="#270e13"/><circle cx="177" cy="116" r="90" fill="#c52c3e"/><path d="M0 300 91 126h33L300 300Z" fill="#151a24"/><path d="m64 300 41-153 21 153m8-161 69 161" stroke="#f18c68" stroke-width="3"/><path d="M12 55h98m88 197h91" stroke="#e52e48" stroke-width="2"/><text x="18" y="282" fill="#ffb597" font-size="10" font-family="Arial" letter-spacing="6">AFTER HOURS</text>`
 ];return `<svg viewBox="0 0 300 300" aria-hidden="true">${designs[palette%designs.length]}</svg>`}
// Sample the actual cover artwork so its colours, rather than the accent setting, tint the player.
const artworkColorCache=new Map();
function artworkColors(palette){
 if(artworkColorCache.has(palette))return artworkColorCache.get(palette);
 const result=new Promise(resolve=>{
  const image=new Image();
  image.onload=()=>{try{
   const canvas=document.createElement('canvas');canvas.width=48;canvas.height=48;
   const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.drawImage(image,0,0,48,48);
   const pixels=ctx.getImageData(0,0,48,48).data,buckets=new Map();
   for(let i=0;i<pixels.length;i+=4){
    const r=pixels[i],g=pixels[i+1],b=pixels[i+2],hi=Math.max(r,g,b),lo=Math.min(r,g,b);
    if(pixels[i+3]<200||hi<28||lo>235)continue;
    const key=[r>>5,g>>5,b>>5].join(','),weight=1+(hi-lo)/255*.65;
    const group=buckets.get(key)||{weight:0,r:0,g:0,b:0};
    group.weight+=weight;group.r+=r*weight;group.g+=g*weight;group.b+=b*weight;buckets.set(key,group);
   }
   const ranked=[...buckets.values()].sort((a,b)=>b.weight-a.weight).map(c=>({weight:c.weight,r:c.r/c.weight,g:c.g/c.weight,b:c.b/c.weight}));
   if(!ranked.length){resolve(null);return}
   const primary=ranked[0],distance=c=>Math.hypot(c.r-primary.r,c.g-primary.g,c.b-primary.b);
   const secondary=ranked.filter(c=>c.weight>=primary.weight*.08&&distance(c)>45).sort((a,b)=>b.weight*distance(b)-a.weight*distance(a))[0]||primary;
   const css=c=>`rgb(${Math.round(c.r)} ${Math.round(c.g)} ${Math.round(c.b)})`;
   resolve([css(primary),css(secondary)]);
  }catch{resolve(null)}};
  image.onerror=()=>resolve(null);
  image.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(art(palette).replace('<svg ','<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300" '));
 });
 artworkColorCache.set(palette,result);return result;
}
async function applyArtworkTheme(){
 const index=state.index,palette=tracks[index]?.palette,phones=[document.getElementById('libraryPhone'),document.getElementById('playerPhone')];
 phones.forEach(p=>p.classList.toggle('artwork-player',palette!=null&&(p.id==='playerPhone'||state.view==='player')));
 const colors=palette==null?null:await artworkColors(palette);
 if(state.index!==index)return;
 phones.forEach(p=>{p.classList.toggle('artwork-tinted',Boolean(colors));if(colors){p.style.setProperty('--art-primary',colors[0]);p.style.setProperty('--art-secondary',colors[1])}else{p.style.removeProperty('--art-primary');p.style.removeProperty('--art-secondary')}});
}
function status(){return `<div class="statusbar"><span>18:41</span><div class="statusicons">${icon('bars')}${icon('wifi')}<svg class="battery" viewBox="0 0 24 13" fill="none"><rect x="1" y="2" width="19" height="9" rx="2" stroke="currentColor"/><path d="M22 5v3" stroke="currentColor" stroke-width="2"/><rect x="3" y="4" width="14" height="5" rx=".5" fill="currentColor"/></svg></div></div>`}
function splash(){const mark=logo();return `<div class="splash"><div class="signal-bits" aria-hidden="true"></div><div class="splashlogo"><div class="glitch-base">${mark}</div><div class="glitch-layer glitch-cyan" aria-hidden="true">${mark}</div><div class="glitch-layer glitch-red" aria-hidden="true">${mark}</div><div class="glitch-layer glitch-yellow" aria-hidden="true">${mark}</div></div><div class="by">by <b>SATURN</b></div><div class="homehandle"></div></div>`}
function header(){return `<header class="appheader"><div class="wordmark">${logo()}</div><button class="account" data-action="settings" aria-label="Настройки">${icon('settings')}</button></header>`}
function row(t,i){return `<div class="track ${state.index===i?'current':''}" data-track="${i}" role="button" tabindex="0" aria-label="${t.artist}, ${t.title}"><div class="cover">${art(t.palette)}</div><div class="tracktext"><strong>${t.title}</strong><small>${t.artist}</small></div>${state.index===i&&state.playing?'<div class="eqbars"><i></i><i></i><i></i><i></i></div>':`<span class="trackduration">${t.duration}</span>`}<button class="more" data-action="trackmenu" data-index="${i}" aria-label="Действия с треком">${icon('more')}</button></div>`}
function list(items=tracks.map((t,i)=>({t,i}))){return items.map(({t,i})=>row(t,i)).join('')}
function mini(){const t=tracks[state.index];return `<div class="miniplayer"><div class="minibody" data-action="openplayer" role="button" tabindex="0" aria-label="Открыть плеер"><div class="cover">${art(t.palette)}</div><div class="tracktext"><strong>${t.title}</strong><small>${t.artist}</small></div>${ib(state.playing?'pause':'play','play',state.playing?'Пауза':'Играть')}${ib('next','next','Следующий трек')}</div><div class="mini-progress"><span style="width:${state.seek}%"></span></div></div>`}
function nav(){return `<nav class="nav">${[['library','music','Моя музыка'],['search','search','Поиск'],['downloads','download','Скачано'],['settings','settings','Настройки']].map(([v,i,n])=>`<button data-action="${v}" class="${state.view===v?'active':''}" aria-label="${n}">${icon(i)}<span>${n}</span></button>`).join('')}</nav>`}
function library(){return `<div class="titleline"><h1>Моя музыка</h1>${ib('search','search','Поиск музыки')}</div><div class="tabs">${[['tracks','Треки'],['playlists','Плейлисты'],['albums','Альбомы']].map(([v,n])=>`<button class="chip ${state.tab===v?'selected':''}" data-tab="${v}">${n}</button>`).join('')}</div>${state.tab==='tracks'?`<div class="collections"><button class="collection" data-action="favorites"><div class="cicon">${icon('heart')}</div><strong>Любимые треки</strong><small>Твоя личная подборка</small>${icon('heart','ornament')}</button><button class="collection" data-action="downloads"><div class="cicon">${icon('download')}</div><strong>На устройстве</strong><small>Музыка с собой</small>${icon('download','ornament')}</button></div><div class="listheading"><div><strong>Треки <small>Все</small></strong></div><div class="tools">${ib('sort','sort','Сортировка')}${ib('shuffle','shuffle','Перемешать')}</div></div>${list()}`:playlistGrid()}`}
function playlistGrid(){return `<div class="playlistgrid">${[['На повторе','Избранное за последнее время',0],['Вечерний маршрут','Музыка для дороги',1],['В своём ритме','Электроника и не только',2],['Тихий час','Для себя',3]].map(([title,desc,p])=>`<button class="playlistcard" data-action="playlist"><div class="cover">${art(p)}</div><strong>${title}</strong><small>${desc}</small></button>`).join('')}</div>`}
function search(){return `<div class="titleline"><h1>Поиск</h1></div><div class="searchfield">${icon('search')}<input type="search" placeholder="Трек, исполнитель или плейлист" aria-label="Поиск музыки" value="${state.query.replaceAll('&','&amp;').replaceAll('"','&quot;')}"></div><div class="searchsection">${state.query?'Результаты':'Недавно слушали'}</div><div class="searchresults">${searchRows()}</div>`}
function searchRows(){const q=state.query.toLowerCase();return list(tracks.map((t,i)=>({t,i})).filter(({t})=>(t.title+' '+t.artist).toLowerCase().includes(q)))||'<div class="downloadnote">Ничего не найдено</div>'}
function downloads(){return `<div class="titleline"><h1>Скачано</h1>${ib('sort','sort','Сортировка')}</div><p class="downloadnote">Треки, которые всегда под рукой.</p>${list(tracks.slice(0,3).map((t,i)=>({t,i})))}`}
function settings(){return `<div class="titleline"><h1>Настройки</h1></div><section class="settingssection"><h2>Оформление</h2><div class="settingmodes">${modes.map(m=>`<button class="settingmode ${state.mode===m.value?'selected':''}" data-mode="${m.value}">${icon(m.icon)}${m.name}</button>`).join('')}</div></section><section class="settingssection"><h2>Цвет акцента</h2><div class="settingaccents">${colors.map(c=>`<button class="settingaccent ${state.accent===c.value?'selected':''}" style="--swatch:${c.value}" data-accent="${c.value}" aria-label="${c.name}">${state.accent===c.value?icon('check'):''}</button>`).join('')}</div></section><section class="settingssection"><h2>Воспроизведение</h2><button class="settingrow" style="width:100%" data-action="quality"><span>Качество звука</span><small>Автоматически</small></button><button class="settingrow" style="width:100%" data-action="cache"><span>Хранилище и кэш</span>${icon('download')}</button></section><div class="aboutmark">MaxMusic<br><span style="display:block;margin-top:5px;color:#ff354c">by SATURN</span></div>`}
function player(){const t=tracks[state.index];return `<div class="playerheader">${ib('down','library','Закрыть плеер')}<div class="center">СЕЙЧАС ИГРАЕТ<b>Моя музыка</b></div>${ib('more','trackmenu','Действия с треком')}</div><div class="playerbody"><div class="bigcover">${art(t.palette)}</div><div class="songinfo"><div><h2>${t.title}</h2><p>${t.artist}</p></div>${ib('heart','favorite',state.favorite?'Убрать из любимого':'В любимое')}</div><div class="seek"><input type="range" min="0" max="100" value="${state.seek}" aria-label="Позиция воспроизведения" style="background:linear-gradient(90deg,var(--accent) 0%,var(--accent) ${state.seek}%,var(--stroke) ${state.seek}%)"><div class="timecodes"><span class="elapsed">${elapsed(t)}</span><span>${t.duration}</span></div></div><div class="playcontrols">${ib('shuffle','shuffle','Перемешать','dim')}${ib('prev','prev','Предыдущий трек','skip')}${ib(state.playing?'pause':'play','play',state.playing?'Пауза':'Играть','play')}${ib('next','next','Следующий трек','skip')}${ib('repeat','repeat','Повтор','dim')}</div><div class="playerfooter"><button data-action="download">${icon('download')}Скачать</button><button class="plain" data-action="volume" aria-label="Громкость">${icon('volume')}</button><button data-action="queue">${icon('queue')}Очередь</button></div></div>`}
function elapsed(t=tracks[state.index]){const [m,s]=t.duration.split(':').map(Number);const v=Math.floor((m*60+s)*state.seek/100);return Math.floor(v/60)+':'+String(v%60).padStart(2,'0')}
function render(){document.getElementById('globalModes').innerHTML=modes.map(m=>`<button data-mode="${m.value}" class="${state.mode===m.value?'active':''}">${m.name}</button>`).join('');document.getElementById('globalAccents').innerHTML=colors.map(c=>`<button class="accent ${state.accent===c.value?'active':''}" data-accent="${c.value}" style="--swatch:${c.value}" aria-label="${c.name}" title="${c.name}"><span></span></button>`).join('');const body=state.view==='player'?player():header()+`<main class="scroll">${({library,search,downloads,settings}[state.view]||library)()}</main>`+mini()+nav();document.getElementById('libraryPhone').innerHTML=status()+`<div class="app">${body}</div><div class="homehandle"></div><div class="splashflash">${splash()}</div><div class="toast"></div>`;document.getElementById('playerPhone').innerHTML=status()+`<div class="app">${player()}</div><div class="homehandle"></div><div class="toast"></div>`;document.getElementById('splashPhone').innerHTML=status()+splash();applyTheme();}
function applyTheme(){const light=state.mode==='light'||(state.mode==='system'&&matchMedia('(prefers-color-scheme: light)').matches);document.querySelectorAll('.phone').forEach(p=>{p.classList.toggle('light',light);p.style.setProperty('--accent',state.accent)});window.maxmusicArtworkReady=applyArtworkTheme();try{localStorage.setItem('maxmusic-look',JSON.stringify({mode:state.mode,accent:state.accent}))}catch{}}
function notice(text){document.querySelectorAll('.toast').forEach(e=>{e.textContent=text;e.classList.add('show');setTimeout(()=>e.classList.remove('show'),2200)})}
function showSheet(phone,title,content){const el=document.createElement('div');el.className='sheetmask';el.innerHTML=`<section class="sheet"><i></i><h2>${title}</h2>${content}</section>`;el.onclick=e=>{if(e.target===el)el.remove()};phone.appendChild(el)}
function glitchSplash(el){if(!el)return;el.classList.remove('glitch-running');void el.offsetWidth;el.classList.add('glitch-running')}
function launch(){const el=document.querySelector('.splashflash');el.classList.add('visible');glitchSplash(el.querySelector('.splash'));glitchSplash(document.querySelector('#splashPhone .splash'));const start=performance.now();setTimeout(()=>{el.classList.remove('visible');window.lastSplashDuration=performance.now()-start},300)}

