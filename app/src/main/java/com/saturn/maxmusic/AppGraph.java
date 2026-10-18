package com.saturn.maxmusic;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory;
import androidx.media3.exoplayer.offline.DefaultDownloadIndex;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadCursor;
import androidx.media3.exoplayer.offline.DownloadManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

@UnstableApi
final class AppGraph {
    private static AppGraph instance;
    final VkClient vk;
    final LibraryRepository library;
    final DownloadManager downloads;
    final CacheDataSource.Factory playbackSource;
    final ExecutorService io=Executors.newFixedThreadPool(3);
    static synchronized AppGraph get(Context c){if(instance==null)instance=new AppGraph(c.getApplicationContext());return instance;}
    private AppGraph(Context c){
        vk=new VkClient(c);library=new LibraryRepository(c,vk);
        StandaloneDatabaseProvider db=new StandaloneDatabaseProvider(c);
        SimpleCache offline=new SimpleCache(new File(c.getFilesDir(),"offline"),new NoOpCacheEvictor(),db);
        SimpleCache streaming=new SimpleCache(new File(c.getCacheDir(),"stream"),new LeastRecentlyUsedCacheEvictor(96L*1024*1024),db);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("MaxMusic/0.1.2").setConnectTimeoutMs(18000).setReadTimeoutMs(25000);
        DefaultDataSource.Factory source=new DefaultDataSource.Factory(c,http);
        CacheDataSource.Factory downloadSource=new CacheDataSource.Factory().setCache(offline).setUpstreamDataSourceFactory(source).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        CacheDataSource.Factory streamingSource=new CacheDataSource.Factory().setCache(streaming).setUpstreamDataSourceFactory(source).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        playbackSource=new CacheDataSource.Factory().setCache(offline).setCacheWriteDataSinkFactory(null).setUpstreamDataSourceFactory(streamingSource).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        downloads=new DownloadManager(c,new DefaultDownloadIndex(db),new DefaultDownloaderFactory(downloadSource,io));
        downloads.setMaxParallelDownloads(2);downloads.resumeDownloads();
    }
    static MediaItem mediaItem(JSONObject track) throws Exception {
        String source=track.optString("stream");
        if(source.isEmpty())throw new Exception("VK не выдал ссылку на «"+track.optString("title")+"»");
        if(!LibraryRepository.safeHttps(source)&&!("local".equals(track.optString("source"))&&source.startsWith("content://")))throw new Exception("Недопустимый источник трека");
        MediaMetadata.Builder metadata=new MediaMetadata.Builder().setTitle(track.optString("title")).setArtist(track.optString("artist")).setAlbumTitle(track.optString("album"));
        String cover=track.optString("cover");if(LibraryRepository.safeHttps(cover))metadata.setArtworkUri(Uri.parse(cover));
        return new MediaItem.Builder().setMediaId(track.getString("id")).setUri(source).setMediaMetadata(metadata.build()).build();
    }
    JSONArray offlineTracks() throws Exception {
        JSONArray result=new JSONArray();
        try(DownloadCursor cursor=downloads.getDownloadIndex().getDownloads()){
            while(cursor.moveToNext()){Download download=cursor.getDownload();if(download.state==Download.STATE_COMPLETED){JSONObject t=new JSONObject(new String(download.request.data,StandardCharsets.UTF_8));t.put("offline",true);result.put(LibraryRepository.publicTrack(t));}}
        }
        JSONArray all=library.all();for(int i=0;i<all.length();i++)if("local".equals(all.getJSONObject(i).optString("source")))result.put(all.get(i));
        return result;
    }
    JSONArray downloadStatus() throws Exception {
        JSONArray result=new JSONArray();try(DownloadCursor cursor=downloads.getDownloadIndex().getDownloads()){
            while(cursor.moveToNext()){Download d=cursor.getDownload();result.put(VkClient.params("id",d.request.id,"state",d.state,"percent",Math.max(0,d.getPercentDownloaded()),"bytes",d.getBytesDownloaded()));}
        }return result;
    }
}
