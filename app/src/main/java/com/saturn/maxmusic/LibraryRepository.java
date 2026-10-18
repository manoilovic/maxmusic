package com.saturn.maxmusic;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

final class LibraryRepository {
    private final SharedPreferences prefs;
    final VkClient vk;
    private final LinkedHashMap<String,JSONObject> tracks=new LinkedHashMap<>();
    LibraryRepository(Context context,VkClient vk) {
        this.vk=vk;prefs=context.getSharedPreferences("library",Context.MODE_PRIVATE);
        try {JSONArray cached=new JSONArray(prefs.getString("tracks","[]"));for(int i=0;i<cached.length();i++){JSONObject t=cached.getJSONObject(i);tracks.put(t.getString("id"),t);}}catch(Exception ignored){}
    }
    static boolean safeHttps(String url) { try {Uri u=Uri.parse(url);return "https".equals(u.getScheme())&&u.getHost()!=null&&!u.getHost().isEmpty()&&u.getUserInfo()==null;}catch(Exception e){return false;} }
    static String coverFrom(JSONObject object) {
        if(object==null)return "";for(String key:new String[]{"photo_1200","photo_600","photo_300","photo_270","photo_135","photo_68"}){String url=object.optString(key);if(safeHttps(url))return url;}return "";
    }
    static JSONObject normalize(JSONObject audio) throws Exception {
        JSONObject album=audio.optJSONObject("album");String cover=coverFrom(album==null?null:album.optJSONObject("thumb"));if(cover.isEmpty())cover=coverFrom(audio.optJSONObject("thumb"));
        String stream=audio.optString("url");if(!safeHttps(stream))stream="";
        return VkClient.params("id",audio.getLong("owner_id")+"_"+audio.getLong("id"),"owner",audio.getLong("owner_id"),"audioId",audio.getLong("id"),"title",audio.optString("title","Без названия"),"artist",audio.optString("artist","Неизвестный исполнитель"),"seconds",audio.optInt("duration"),"cover",cover,"album",album==null?"":album.optString("title"),"stream",stream,"source","vk","updated",System.currentTimeMillis());
    }
    synchronized void put(JSONObject track) throws Exception {tracks.put(track.getString("id"),track);persist();}
    private void persist(){JSONArray a=new JSONArray();for(JSONObject t:tracks.values())a.put(t);prefs.edit().putString("tracks",a.toString()).apply();}
    synchronized JSONObject get(String id) throws Exception {JSONObject t=tracks.get(id);if(t==null)throw new Exception("Трек не найден. Обновите список");return new JSONObject(t.toString());}
    static JSONObject publicTrack(JSONObject t) throws Exception {JSONObject copy=new JSONObject(t.toString());copy.remove("stream");copy.remove("updated");return copy;}
    synchronized JSONArray all() throws Exception {JSONArray a=new JSONArray();for(JSONObject t:tracks.values())a.put(publicTrack(t));return a;}
    synchronized JSONArray library() throws Exception {JSONArray ids=new JSONArray(prefs.getString("libraryIds","[]")),a=new JSONArray();for(int i=0;i<ids.length();i++){JSONObject t=tracks.get(ids.getString(i));if(t!=null)a.put(publicTrack(t));}for(JSONObject t:tracks.values())if("local".equals(t.optString("source")))a.put(publicTrack(t));return a;}
    private synchronized JSONArray ingest(JSONArray audios) throws Exception {JSONArray a=new JSONArray();for(int i=0;i<audios.length();i++){JSONObject t=normalize(audios.getJSONObject(i));tracks.put(t.getString("id"),t);a.put(publicTrack(t));}persist();return a;}
    JSONArray loadLibrary() throws Exception {
        JSONArray result;
        try {JSONObject response=(JSONObject)vk.call("audio.get",VkClient.params("count",200,"offset",0));JSONArray items=response.optJSONArray("items")==null?new JSONArray():response.getJSONArray("items");result=ingest(items);prefs.edit().putString("libraryMode","audio").putInt("libraryOffset",items.length()).putBoolean("libraryMore",response.optInt("count")>items.length()).apply();}
        catch(Exception first) {
            JSONObject response=(JSONObject)vk.call("catalog.getAudio",VkClient.params("need_blocks",1));
            JSONObject catalog=response.optJSONObject("catalog");String section=catalog==null?"":catalog.optString("default_section");
            if(catalog!=null){JSONArray sections=catalog.optJSONArray("sections");if(sections!=null)for(int i=0;i<sections.length();i++){JSONObject s=sections.getJSONObject(i);String title=s.optString("title").toLowerCase(java.util.Locale.ROOT);if(title.contains("моя музыка")||title.contains("мои треки")){section=s.optString("id");break;}}}
            if(!section.isEmpty())response=(JSONObject)vk.call("catalog.getSection",VkClient.params("section_id",section));
            result=ingest(catalogAudios(response));JSONObject sectionData=response.optJSONObject("section");String next=sectionData==null?"":sectionData.optString("next_from");prefs.edit().putString("libraryMode","catalog").putString("librarySection",section).putString("libraryNext",next).putBoolean("libraryMore",!next.isEmpty()).apply();
        }
        JSONArray ids=new JSONArray();for(int i=0;i<result.length();i++)ids.put(result.getJSONObject(i).getString("id"));prefs.edit().putString("libraryIds",ids.toString()).apply();return library();
    }
    private JSONArray catalogAudios(JSONObject response) throws Exception {
        JSONArray audios=response.optJSONArray("audios");if(audios==null)return new JSONArray();JSONObject section=response.optJSONObject("section");JSONArray blocks=section==null?null:section.optJSONArray("blocks");
        if(blocks!=null)for(int i=0;i<blocks.length();i++){JSONObject block=blocks.getJSONObject(i);if("music_audios".equals(block.optString("data_type"))&&!block.optString("url").contains("block=recent")){JSONArray ids=block.optJSONArray("audios_ids");if(ids!=null){java.util.Set<String> wanted=new java.util.HashSet<>();for(int n=0;n<ids.length();n++)wanted.add(ids.getString(n));JSONArray filtered=new JSONArray();for(int n=0;n<audios.length();n++){JSONObject a=audios.getJSONObject(n);if(wanted.contains(a.getLong("owner_id")+"_"+a.getLong("id")))filtered.put(a);}return filtered;}}}return audios;
    }
    boolean hasMore(){return prefs.getBoolean("libraryMore",false);}
    JSONArray loadMore() throws Exception {
        if(!hasMore())return library();JSONArray added;
        if("audio".equals(prefs.getString("libraryMode",""))){int offset=prefs.getInt("libraryOffset",0);JSONObject r=(JSONObject)vk.call("audio.get",VkClient.params("count",200,"offset",offset));JSONArray items=r.optJSONArray("items");if(items==null)items=new JSONArray();added=ingest(items);prefs.edit().putInt("libraryOffset",offset+items.length()).putBoolean("libraryMore",items.length()>0&&offset+items.length()<r.optInt("count")).apply();}
        else{JSONObject r=(JSONObject)vk.call("catalog.getSection",VkClient.params("section_id",prefs.getString("librarySection",""),"start_from",prefs.getString("libraryNext","")));added=ingest(catalogAudios(r));JSONObject section=r.optJSONObject("section");String next=section==null?"":section.optString("next_from");prefs.edit().putString("libraryNext",next).putBoolean("libraryMore",!next.isEmpty()).apply();}
        JSONArray old=new JSONArray(prefs.getString("libraryIds","[]"));java.util.Set<String> ids=new java.util.LinkedHashSet<>();for(int i=0;i<old.length();i++)ids.add(old.getString(i));for(int i=0;i<added.length();i++)ids.add(added.getJSONObject(i).getString("id"));JSONArray merged=new JSONArray();for(String id:ids)merged.put(id);prefs.edit().putString("libraryIds",merged.toString()).apply();return library();
    }
    JSONArray search(String query) throws Exception {JSONObject r=(JSONObject)vk.call("audio.search",VkClient.params("q",query,"count",100,"offset",0));return ingest(r.optJSONArray("items")==null?new JSONArray():r.getJSONArray("items"));}
    synchronized List<JSONObject> resolveQueue(JSONArray ids) throws Exception {
        ArrayList<JSONObject> queue=new ArrayList<>();StringBuilder refreshIds=new StringBuilder();
        for(int i=0;i<ids.length()&&i<200;i++){JSONObject t=get(ids.getString(i));if("vk".equals(t.optString("source"))&&(t.optString("stream").isEmpty()||t.optLong("updated")<System.currentTimeMillis()-1800000)){if(refreshIds.length()>0)refreshIds.append(',');refreshIds.append(t.getString("id"));}queue.add(t);}
        if(refreshIds.length()>0){Object r=vk.call("audio.getById",VkClient.params("audios",refreshIds.toString()));JSONArray a=r instanceof JSONArray?(JSONArray)r:((JSONObject)r).optJSONArray("items");if(a!=null)ingest(a);for(int i=0;i<queue.size();i++)queue.set(i,get(queue.get(i).getString("id")));}
        return queue;
    }
    synchronized JSONArray favorites() {try{return new JSONArray(prefs.getString("favorites","[]"));}catch(Exception e){return new JSONArray();}}
    synchronized JSONArray favorite(String id) throws Exception {JSONArray old=favorites(),a=new JSONArray();boolean found=false;for(int i=0;i<old.length();i++){String v=old.getString(i);if(v.equals(id))found=true;else a.put(v);}if(!found)a.put(id);prefs.edit().putString("favorites",a.toString()).apply();return a;}
    private JSONArray localPlaylists(){try{return new JSONArray(prefs.getString("playlists","[]"));}catch(Exception e){return new JSONArray();}}
    synchronized JSONArray playlists(){JSONArray result=localPlaylists();try{JSONArray remote=new JSONArray(prefs.getString("vkPlaylists","[]"));for(int i=0;i<remote.length();i++)result.put(remote.get(i));}catch(Exception ignored){}return result;}
    synchronized JSONArray createPlaylist(String title) throws Exception {title=title.trim();if(title.isEmpty()||title.length()>100)throw new Exception("Введите название до 100 символов");JSONArray all=localPlaylists();all.put(VkClient.params("id","local-"+java.util.UUID.randomUUID(),"title",title,"tracks",new JSONArray(),"source","local"));prefs.edit().putString("playlists",all.toString()).apply();return playlists();}
    synchronized void addPlaylist(String playlist,String track) throws Exception {get(track);JSONArray all=localPlaylists();for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);if(p.getString("id").equals(playlist)){JSONArray ids=p.getJSONArray("tracks");for(int n=0;n<ids.length();n++)if(ids.getString(n).equals(track))return;ids.put(track);prefs.edit().putString("playlists",all.toString()).apply();return;}}throw new Exception("Плейлист MaxMusic не найден");}
    JSONArray loadVkPlaylists() throws Exception {
        long owner=vk.profile().getLong("id");JSONObject response=(JSONObject)vk.call("audio.getPlaylists",VkClient.params("owner_id",owner,"count",100));JSONArray items=response.optJSONArray("items"),result=new JSONArray();
        if(items!=null)for(int i=0;i<items.length();i++){JSONObject p=items.getJSONObject(i);result.put(VkClient.params("id","vk-"+p.getLong("owner_id")+"_"+p.getLong("id"),"owner",p.getLong("owner_id"),"playlistId",p.getLong("id"),"accessKey",p.optString("access_key"),"title",p.optString("title"),"cover",coverFrom(p.optJSONObject("photo")),"count",p.optInt("count"),"tracks",new JSONArray(),"source","vk"));}
        prefs.edit().putString("vkPlaylists",result.toString()).apply();return playlists();
    }
    JSONArray openVkPlaylist(String id) throws Exception {
        JSONArray all=new JSONArray(prefs.getString("vkPlaylists","[]"));for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);if(id.equals(p.optString("id"))){JSONArray result=new JSONArray(),ids=new JSONArray();for(int offset=0;offset<6000;offset+=200){JSONObject response=(JSONObject)vk.call("audio.get",VkClient.params("owner_id",p.getLong("owner"),"album_id",p.getLong("playlistId"),"access_key",p.optString("accessKey"),"count",200,"offset",offset));JSONArray items=response.optJSONArray("items");if(items==null||items.length()==0)break;JSONArray page=ingest(items);for(int n=0;n<page.length();n++){JSONObject t=page.getJSONObject(n);result.put(t);ids.put(t.getString("id"));}if(offset+items.length()>=response.optInt("count"))break;}p.put("tracks",ids);prefs.edit().putString("vkPlaylists",all.toString()).apply();return result;}}throw new Exception("Плейлист VK не найден");
    }
    synchronized void clearVk() {tracks.entrySet().removeIf(e->"vk".equals(e.getValue().optString("source")));prefs.edit().remove("libraryIds").remove("vkPlaylists").putBoolean("libraryMore",false).apply();persist();}
}
