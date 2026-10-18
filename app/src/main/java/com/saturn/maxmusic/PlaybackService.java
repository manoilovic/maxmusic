package com.saturn.maxmusic;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

@UnstableApi
public final class PlaybackService extends MediaSessionService {
    private MediaSession session;
    @Override public void onCreate(){
        super.onCreate();AppGraph graph=AppGraph.get(this);
        ExoPlayer player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(graph.playbackSource)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true);
        player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_LOCAL);
        PendingIntent activity=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        session=new MediaSession.Builder(this,player).setSessionActivity(activity).setCallback(new MediaSession.Callback(){
            @Override public MediaSession.ConnectionResult onConnect(MediaSession mediaSession,MediaSession.ControllerInfo info){
                if(info.isTrusted()||getPackageName().equals(info.getPackageName()))return MediaSession.Callback.super.onConnect(mediaSession,info);
                return MediaSession.ConnectionResult.reject();
            }
        }).build();
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo info){return session;}
    @Override public void onTaskRemoved(Intent rootIntent){if(session==null||!session.getPlayer().getPlayWhenReady()||session.getPlayer().getMediaItemCount()==0||session.getPlayer().getPlaybackState()==Player.STATE_ENDED)stopSelf();}
    @Override public void onDestroy(){if(session!=null){session.getPlayer().release();session.release();session=null;}super.onDestroy();}
}
