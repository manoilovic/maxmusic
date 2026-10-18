package com.saturn.maxmusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.Scheduler;
import java.util.List;

@UnstableApi
public final class OfflineService extends DownloadService {
    public OfflineService(){super(2002,DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,"maxmusic-downloads",R.string.download_channel,0);}
    @Override protected DownloadManager getDownloadManager(){return AppGraph.get(this).downloads;}
    @Override protected Scheduler getScheduler(){return null;}
    @Override protected Notification getForegroundNotification(List<Download> downloads,int requirements){
        float percent=0;int active=0;for(Download d:downloads){if(d.state==Download.STATE_DOWNLOADING||d.state==Download.STATE_QUEUED){active++;percent+=Math.max(0,d.getPercentDownloaded());}}
        PendingIntent intent=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,"maxmusic-downloads").setSmallIcon(R.drawable.ic_music).setContentTitle("MaxMusic").setContentText(active>0?"Скачивание треков: "+active:"Завершение скачивания").setProgress(100,active>0?(int)(percent/active):0,percent<=0).setContentIntent(intent).setOngoing(true).setOnlyAlertOnce(true).build();
    }
}
