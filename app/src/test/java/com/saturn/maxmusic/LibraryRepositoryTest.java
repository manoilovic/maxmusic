package com.saturn.maxmusic;

import static org.junit.Assert.*;
import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=29)
public class LibraryRepositoryTest {
    Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("library",0).edit().clear().commit();context.getSharedPreferences("session",0).edit().clear().commit();}
    @Test public void metadataSelectsRealCoverAndDoesNotExposePlaybackUrl() throws Exception {
        JSONObject audio=VkClient.params("id",12,"owner_id",34,"title","<test>","artist","Artist","duration",123,"url","https://cdn.example/stream.m3u8","album",VkClient.params("title","Album","thumb",VkClient.params("photo_600","https://cdn.example/cover.jpg")));
        JSONObject normalized=LibraryRepository.normalize(audio);
        assertEquals("34_12",normalized.getString("id"));assertEquals(123,normalized.getInt("seconds"));assertEquals("https://cdn.example/cover.jpg",normalized.getString("cover"));
        assertFalse(LibraryRepository.publicTrack(normalized).has("stream"));assertTrue(normalized.has("stream"));
    }
    @Test public void rejectsJavascriptAndCleartextMediaAddresses(){
        assertFalse(LibraryRepository.safeHttps("javascript:alert(1)"));assertFalse(LibraryRepository.safeHttps("http://example.com/a.mp3"));assertFalse(LibraryRepository.safeHttps("https://user:secret@example.com/a.mp3"));assertTrue(LibraryRepository.safeHttps("https://cdn.example/a.mp3?sig=123"));
    }
    @Test public void loginCannotNavigateToSpoofedVkDomain(){
        assertTrue(LoginActivity.isVkAddress(Uri.parse("https://id.vk.ru/auth")));assertTrue(LoginActivity.isVkAddress(Uri.parse("https://vk.com/")));
        assertFalse(LoginActivity.isVkAddress(Uri.parse("https://vk.ru.evil.example/login")));assertFalse(LoginActivity.isVkAddress(Uri.parse("http://vk.ru/")));
    }
    @Test public void favoritesAndPlaylistsSurviveRestartWithoutDuplicates() throws Exception {
        LibraryRepository repo=new LibraryRepository(context,new VkClient(context));repo.put(VkClient.params("id","local-1","source","local","stream","content://media/audio/1"));
        repo.favorite("local-1");JSONArray playlists=repo.createPlaylist("My list");String id=playlists.getJSONObject(0).getString("id");repo.addPlaylist(id,"local-1");repo.addPlaylist(id,"local-1");
        LibraryRepository restored=new LibraryRepository(context,new VkClient(context));assertEquals(1,restored.favorites().length());assertEquals(1,restored.playlists().getJSONObject(0).getJSONArray("tracks").length());
        assertEquals(0,restored.favorite("local-1").length());
    }
    @Test public void loggingOutPreservesImportedFiles() throws Exception {
        LibraryRepository repo=new LibraryRepository(context,new VkClient(context));repo.put(VkClient.params("id","local-1","source","local"));repo.put(VkClient.params("id","1_2","source","vk"));repo.clearVk();assertEquals(1,repo.all().length());assertEquals("local-1",repo.all().getJSONObject(0).getString("id"));
    }
    @Test public void playbackRetainsLocalUriAndMetadata() throws Exception {
        MediaItem item=AppGraph.mediaItem(VkClient.params("id","local-2","source","local","stream","content://media/audio/2","title","Title","artist","Artist"));assertEquals("local-2",item.mediaId);assertEquals("Title",item.mediaMetadata.title);assertEquals("content://media/audio/2",item.localConfiguration.uri.toString());
    }
    @Test(expected=Exception.class) public void remoteTrackCannotInjectLocalContentUri() throws Exception {AppGraph.mediaItem(VkClient.params("id","1_2","source","vk","stream","content://private/data"));}
}
