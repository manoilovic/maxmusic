package com.saturn.maxmusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.webkit.WebViewAssetLoader;
import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

@UnstableApi
public final class MainActivity extends Activity {
    private WebView web;
    private AppGraph graph;
    private MediaController controller;
    private ListenableFuture<MediaController> future;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean resumed,ready;
    private final Runnable tick=new Runnable(){public void run(){if(resumed){sendState();handler.postDelayed(this,700);}}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);graph=AppGraph.get(this);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(11,13,17));
        if(Build.VERSION.SDK_INT>=31)getSplashScreen().setOnExitAnimationListener(view->view.remove());
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        web=new WebView(this);web.setBackgroundColor(Color.rgb(11,13,17));
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMediaPlaybackRequiresUserGesture(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        graph.vk.userAgent=settings.getUserAgentString();
        WebViewAssetLoader assets=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return assets.shouldInterceptRequest(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !trusted(request.getUrl().toString());}
        });
        web.addJavascriptInterface(new Bridge(),"Android");root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html?solo=library&capture");
        future=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();
        future.addListener(()->{try{controller=future.get();controller.addListener(new Player.Listener(){@Override public void onEvents(Player p,Player.Events e){sendState();}});sendState();}catch(Exception ignored){}},getMainExecutor());
    }
    private static boolean trusted(String url){if(url==null)return false;Uri uri=Uri.parse(url);return "https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&"/assets/index.html".equals(uri.getPath());}
    private void javascript(String script){if(!isFinishing()&&web!=null)web.evaluateJavascript(script,null);}
    private void reply(String id,Object data,String error){handler.post(()->{try{javascript("window.NativeApi&&NativeApi.receive("+JSONObject.quote(id)+","+(data==null?"null":data.toString())+","+(error==null?"null":JSONObject.quote(error))+")");}catch(Exception ignored){}});}
    private JSONObject snapshot() throws Exception {
        JSONObject result=VkClient.params("connected",graph.vk.connected(),"tracks",graph.library.all(),"library",graph.library.library(),"favorites",graph.library.favorites(),"playlists",graph.library.playlists());
        result.put("offline",graph.offlineTracks()).put("downloads",graph.downloadStatus()).put("hasMore",graph.library.hasMore()).put("systemLight",(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES);return result;
    }
    private JSONObject playerState() throws Exception {
        JSONObject s=VkClient.params("ready",controller!=null,"playing",false,"position",0,"duration",0,"queue",new JSONArray());if(controller==null)return s;
        MediaItem current=controller.getCurrentMediaItem();JSONArray ids=new JSONArray();for(int i=0;i<controller.getMediaItemCount();i++)ids.put(controller.getMediaItemAt(i).mediaId);
        s.put("queue",ids).put("playing",controller.isPlaying()).put("buffering",controller.getPlaybackState()==Player.STATE_BUFFERING).put("currentId",current==null?"":current.mediaId).put("position",Math.max(0,controller.getCurrentPosition())).put("duration",Math.max(0,controller.getDuration())).put("shuffle",controller.getShuffleModeEnabled()).put("repeat",controller.getRepeatMode());
        if(controller.getPlayerError()!=null)s.put("error","Не удалось воспроизвести трек. Проверьте сеть и обновите список музыки");return s;
    }
    private void sendState(){if(!ready)return;try{JSONObject state=playerState();javascript("window.NativeApi&&NativeApi.update("+state+")");}catch(Exception ignored){}}
    private void control(String action,JSONObject request,String id){
        if(controller==null){reply(id,null,"Плеер запускается. Попробуйте ещё раз");return;}
        try{switch(action){case "toggle":if(controller.getPlayWhenReady())controller.pause();else controller.play();break;case "pause":controller.pause();break;case "next":controller.seekToNextMediaItem();break;case "prev":if(controller.getCurrentPosition()>3000)controller.seekTo(0);else controller.seekToPreviousMediaItem();break;case "seek":controller.seekTo(Math.max(0,request.optLong("position")));break;case "shuffle":controller.setShuffleModeEnabled(request.optBoolean("enabled"));break;case "repeat":controller.setRepeatMode(request.optBoolean("enabled")?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);break;}
            reply(id,playerState(),null);sendState();
        }catch(Exception error){reply(id,null,error.getMessage());}
    }
    private List<JSONObject> playbackQueue(JSONArray ids) throws Exception {
        List<JSONObject> result=new ArrayList<>();JSONArray missing=new JSONArray();java.util.Map<String,JSONObject> available=new java.util.HashMap<>();
        for(int i=0;i<ids.length()&&i<200;i++){
            String id=ids.getString(i);Download cached=graph.downloads.getDownloadIndex().getDownload(id);
            if(cached!=null&&cached.state==Download.STATE_COMPLETED)available.put(id,new JSONObject(new String(cached.request.data,StandardCharsets.UTF_8)));
            else missing.put(id);
        }
        if(missing.length()>0){try{for(JSONObject track:graph.library.resolveQueue(missing))available.put(track.getString("id"),track);}catch(Exception unavailable){if(available.isEmpty())throw unavailable;}}
        for(int i=0;i<ids.length()&&i<200;i++){JSONObject track=available.get(ids.getString(i));if(track!=null&&!track.optString("stream").isEmpty())result.add(track);}
        if(result.isEmpty())throw new Exception("Нет доступных треков для воспроизведения");return result;
    }
    private final class Bridge {
        @JavascriptInterface public void postMessage(String raw){
            if(raw==null||raw.length()>65536)return;
            handler.post(()->{if(!trusted(web.getUrl()))return;try{
                JSONObject request=new JSONObject(raw);String id=request.getString("request"),action=request.getString("action");
                if(action.equals("ready")){ready=true;reply(id,VkClient.params("ok",true),null);sendState();return;}
                if(action.equals("login")){startActivityForResult(new Intent(MainActivity.this,LoginActivity.class),91);reply(id,VkClient.params("opened",true),null);return;}
                if(action.equals("import")){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,92);reply(id,VkClient.params("opened",true),null);return;}
                if(action.equals("theme")){boolean light=request.optBoolean("light");int color=Color.parseColor(light?"#F4F5F8":"#0B0D11");getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);int flags=light?android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0;web.setSystemUiVisibility(flags);reply(id,VkClient.params("ok",true),null);return;}
                if(action.equals("notifications")){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);reply(id,VkClient.params("ok",true),null);return;}
                if(action.equals("clearCache")){web.clearCache(true);reply(id,VkClient.params("ok",true),null);return;}
                if(action.equals("exit")){moveTaskToBack(true);reply(id,VkClient.params("ok",true),null);return;}
                if(java.util.Arrays.asList("toggle","pause","next","prev","seek","shuffle","repeat").contains(action)){control(action,request,id);return;}
                graph.io.execute(()->work(action,request,id));
            }catch(Exception ignored){}});
        }
    }
    private void work(String action,JSONObject request,String id){try{
        Object result;
        switch(action){
            case "bootstrap":result=snapshot();break;
            case "library":result=VkClient.params("tracks",graph.library.loadLibrary(),"hasMore",graph.library.hasMore());break;
            case "more":result=VkClient.params("tracks",graph.library.loadMore(),"hasMore",graph.library.hasMore());break;
            case "vkPlaylists":result=graph.library.loadVkPlaylists();break;
            case "openVkPlaylist":result=graph.library.openVkPlaylist(request.getString("playlist"));break;
            case "search":String q=request.optString("query").trim();if(q.length()>150)throw new Exception("Поисковый запрос слишком длинный");result=q.isEmpty()?new JSONArray():graph.library.search(q);break;
            case "colors":result=ArtworkPalette.extract(graph.library.get(request.getString("track")).optString("cover"));break;
            case "favorite":result=graph.library.favorite(request.getString("track"));break;
            case "createPlaylist":result=graph.library.createPlaylist(request.getString("title"));break;
            case "addPlaylist":graph.library.addPlaylist(request.getString("playlist"),request.getString("track"));result=graph.library.playlists();break;
            case "downloads":result=VkClient.params("offline",graph.offlineTracks(),"downloads",graph.downloadStatus());break;
            case "logout":graph.vk.logout();handler.post(()->{if(controller!=null)controller.clearMediaItems();android.webkit.CookieManager.getInstance().removeAllCookies(null);android.webkit.CookieManager.getInstance().flush();});graph.library.clearVk();result=snapshot();break;
            case "play":{
                List<JSONObject> queue=playbackQueue(request.getJSONArray("queue"));List<MediaItem> items=new ArrayList<>();int selected=0;String wanted=request.getString("track");boolean found=false;
                for(JSONObject t:queue){if(t.getString("id").equals(wanted)){selected=items.size();found=true;}items.add(AppGraph.mediaItem(t));}
                if(!found)throw new Exception("Выбранный трек недоступен для воспроизведения");final int start=selected;
                handler.post(()->{if(controller==null){reply(id,null,"Плеер ещё запускается");return;}controller.setMediaItems(items,start,0);controller.prepare();controller.play();reply(id,VkClient.paramsQuiet("ok",true),null);sendState();});return;
            }
            case "download":{
                List<JSONObject> queue=playbackQueue(new JSONArray().put(request.getString("track")));JSONObject t=queue.get(0);
                if("local".equals(t.optString("source")))throw new Exception("Этот трек уже на телефоне");
                DownloadRequest download=new DownloadRequest.Builder(t.getString("id"),Uri.parse(t.getString("stream"))).setData(t.toString().getBytes(StandardCharsets.UTF_8)).build();
                handler.post(()->DownloadService.sendAddDownload(MainActivity.this,OfflineService.class,download,true));result=VkClient.params("queued",true);break;
            }
            case "removeDownload":DownloadService.sendRemoveDownload(this,OfflineService.class,request.getString("track"),true);result=VkClient.params("ok",true);break;
            default:throw new Exception("Неизвестное действие");
        }
        reply(id,result,null);
    }catch(Exception error){reply(id,null,error.getMessage()==null?"Не удалось выполнить действие":error.getMessage());}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK)return;
        if(request==91){javascript("window.NativeApi&&NativeApi.accountChanged()");}
        else if(request==92&&data!=null){ArrayList<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)uris.add(data.getData());
            for(Uri uri:uris){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}
            graph.io.execute(()->{for(Uri uri:uris){try(MediaMetadataRetriever metadata=new MediaMetadataRetriever()){
                metadata.setDataSource(this,uri);String title=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);String artist=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);String duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if(title==null){try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())title=cursor.getString(0);}if(title==null)title="Аудиофайл";}
                byte[] cover=metadata.getEmbeddedPicture();String image=cover!=null&&cover.length<1024*1024?"data:image/jpeg;base64,"+android.util.Base64.encodeToString(cover,android.util.Base64.NO_WRAP):"";
                graph.library.put(VkClient.params("id","local-"+Integer.toHexString(uri.toString().hashCode()),"title",title,"artist",artist==null?"На устройстве":artist,"seconds",duration==null?0:Long.parseLong(duration)/1000,"cover",image,"album","","source","local","stream",uri.toString()));
            }catch(Exception ignored){}}handler.post(()->javascript("window.NativeApi&&NativeApi.accountChanged()"));});
        }
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(tick);super.onPause();}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);boolean light=(config.uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;javascript("window.NativeApi&&NativeApi.systemThemeChanged("+light+")");}
    @Override public void onBackPressed(){if(!ready)super.onBackPressed();else javascript("window.NativeApi&&NativeApi.back()");}
    @Override protected void onDestroy(){ready=false;handler.removeCallbacksAndMessages(null);if(future!=null)MediaController.releaseFuture(future);if(web!=null){web.removeJavascriptInterface("Android");web.destroy();web=null;}super.onDestroy();}
}
