const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const assets=path.resolve(process.argv[2]||path.join(__dirname,'../app/src/main/assets'));
const out=path.resolve(process.argv[3]||'ui-checks.json');
const origin='https://appassets.androidplatform.net/assets/';
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const checks=[],errors=[];
 try{
  const page=await browser.newPage({viewport:{width:393,height:780},deviceScaleFactor:1,isMobile:true,hasTouch:true});
  page.on('pageerror',e=>errors.push(e.message));
  await page.route(origin+'**',async route=>{
   const file=path.join(assets,path.basename(new URL(route.request().url()).pathname));
   await route.fulfill({path:file,contentType:file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html'});
  });
  await page.addInitScript(()=>{
   const rows=Array.from({length:150},(_,i)=>({id:'local-'+i,title:'Track '+i,artist:'Artist',seconds:180,source:'local',cover:''}));
   window.mockRows=rows;window.mockFrame=null;
   window.Android={postMessage(raw){const r=JSON.parse(raw);if(r.action==='splashFrame'){window.mockFrame=r.request;return;}
    let data={ok:true};if(r.action==='bootstrap')data={connected:true,tracks:rows,library:rows,favorites:[],playlists:[],offline:[],downloads:[],systemLight:true};
    if(r.action==='library'||r.action==='more')data={tracks:rows,hasMore:false};if(r.action==='vkPlaylists')data=[];
    setTimeout(()=>NativeApi.receive(r.request,data,null),0);
   }};
  });
  await page.goto(origin+'index.html');
  await page.waitForFunction(()=>window.mockFrame&&document.querySelector('.splashflash.visible'));
  const overlay=await page.locator('.splashflash').elementHandle();
  await page.waitForTimeout(380);
  await page.evaluate(()=>{for(let i=0;i<10;i++)MaxMusic.render();});
  assert(await overlay.evaluate(e=>e.isConnected&&e.classList.contains('visible')),'splash must survive refresh and wait for native frame');
  await page.evaluate(()=>NativeApi.receive(window.mockFrame,{ok:true},null));
  await page.waitForTimeout(100);
  assert(await page.locator('.splashflash').evaluate(e=>e.classList.contains('visible')));
  assert(await page.locator('.splashflash .splashlogo svg').first().isVisible(),'logo must be visible');
  if(process.env.MAXMUSIC_UI_SHOTS)await page.screenshot({path:path.join(process.env.MAXMUSIC_UI_SHOTS,'splash.png')});
  await page.waitForFunction(()=>window.lastSplashDuration>=285);
  const duration=await page.evaluate(()=>lastSplashDuration);
  assert(duration<600,'splash duration '+duration);checks.push({check:'splash survives bootstrap and renders logo for 300 ms after native frame',durationMs:duration});
  for(const [width,height] of [[393,780],[360,640],[412,875],[844,390]]){
   await page.setViewportSize({width,height});
   for(const view of ['library','search','downloads','settings']){
    await page.evaluate(view=>{MaxMusic.state.view=view;MaxMusic.render();},view);
    const bounds=await page.evaluate(()=>{const r=e=>{const b=e.getBoundingClientRect();return{x:b.x,y:b.y,width:b.width,height:b.height,bottom:b.bottom}};return {doc:document.documentElement.scrollHeight,body:document.body.scrollHeight,height:innerHeight,width:innerWidth,scrollWidth:document.documentElement.scrollWidth,phone:r(document.querySelector('.phone')),nav:r(document.querySelector('.nav')),header:r(document.querySelector('.appheader'))};});
    assert(bounds.doc<=height+1&&bounds.body<=height+1,JSON.stringify({view,width,height,bounds}));
    assert(bounds.scrollWidth<=width+1);assert.equal(bounds.phone.height,height);assert.equal(bounds.nav.bottom,height);
    const before=bounds.nav.y;
    await page.locator('.scroll').evaluate(e=>e.scrollTop=e.scrollHeight);
    await page.mouse.move(width/2,height-10);await page.mouse.wheel(0,800);
    await page.waitForTimeout(30);
    const after=await page.evaluate(()=>({y:document.querySelector('.nav').getBoundingClientRect().y,header:document.querySelector('.appheader').getBoundingClientRect().y,window:scrollY}));
    assert.equal(after.y,before);assert.equal(after.header,bounds.header.y);assert.equal(after.window,0);
    checks.push({check:'only content scrolls; no bottom gap',view,width,height});
   }
  }
  await page.setViewportSize({width:393,height:780});
  await page.evaluate(()=>{MaxMusic.state.view='settings';MaxMusic.setTheme('light','#ff354c');});
  if(process.env.MAXMUSIC_UI_SHOTS)await page.screenshot({path:path.join(process.env.MAXMUSIC_UI_SHOTS,'settings.png')});
  await page.evaluate(()=>{NativeApi.update({currentId:'local-0',queue:['local-0'],playing:true,duration:180000,position:60000});MaxMusic.state.view='player';MaxMusic.render();});
  for(const height of [780,420]){await page.setViewportSize({width:393,height});const b=await page.evaluate(()=>({height:innerHeight,doc:document.documentElement.scrollHeight,body:document.body.scrollHeight}));assert(b.doc<=height&&b.body<=height);await page.locator('.playerbody').evaluate(e=>e.scrollTop=e.scrollHeight);assert(await page.getByRole('button',{name:'Очередь',exact:true}).isVisible());}
  checks.push({check:'player remains within window when keyboard or small window reduces height'});
  assert.deepEqual(errors,[]);
  fs.writeFileSync(out,JSON.stringify({passed:true,checks,errors,scope:'Chromium UI with mocked native bridge, not an Android device test'},null,2));
  console.log(JSON.stringify({passed:true,checks:checks.length,splashDurationMs:duration,errors}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

