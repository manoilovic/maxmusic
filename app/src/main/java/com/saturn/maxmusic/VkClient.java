package com.saturn.maxmusic;

import android.content.Context;
import android.webkit.CookieManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

/** VK web-session protocol adapted from ilyhalight/vk-audio (MIT), see THIRD-PARTY.txt. */
final class VkClient {
    static final String API_VERSION = "5.282";
    private final SessionStore store;
    private JSONObject session;
    String userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36";
    VkClient(Context context) { store = new SessionStore(context); session = store.read(); }
    synchronized boolean connected() { return !session.optString("token").isEmpty(); }
    synchronized void logout() { session = new JSONObject(); store.clear(); }
    static JSONObject params(Object... pairs) throws Exception {
        JSONObject result = new JSONObject(); for (int i=0;i<pairs.length;i+=2) if (pairs[i+1]!=null) result.put(String.valueOf(pairs[i]), pairs[i+1]); return result;
    }
    static JSONObject paramsQuiet(Object... pairs) {try{return params(pairs);}catch(Exception ignored){return new JSONObject();}}
    private static String encode(JSONObject values) throws Exception {
        StringBuilder out = new StringBuilder(); Iterator<String> keys = values.keys();
        while(keys.hasNext()) { String key=keys.next(); if(out.length()>0)out.append('&'); out.append(URLEncoder.encode(key,"UTF-8")).append('=').append(URLEncoder.encode(String.valueOf(values.get(key)),"UTF-8")); }
        return out.toString();
    }
    JSONObject post(String endpoint, JSONObject values, boolean cookies) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(18000); conn.setReadTimeout(25000); conn.setRequestMethod("POST"); conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("User-Agent", userAgent); conn.setRequestProperty("Origin","https://vk.ru"); conn.setRequestProperty("Referer","https://vk.ru/");
        if(cookies) {
            String cookie = CookieManager.getInstance().getCookie("https://login.vk.ru/");
            String root = CookieManager.getInstance().getCookie("https://vk.ru/");
            String legacy = CookieManager.getInstance().getCookie("https://vk.com/");
            String combined = (cookie==null?"":cookie+"; ")+(root==null?"":root+"; ")+(legacy==null?"":legacy);
            if(combined.isEmpty()) throw new Exception("Сначала войдите в VK");
            conn.setRequestProperty("Cookie", combined);
        }
        try {
            byte[] body = encode(values).getBytes(StandardCharsets.UTF_8); conn.getOutputStream().write(body);
            int status=conn.getResponseCode();
            if(status<200||status>=300) throw new Exception("VK недоступен (HTTP "+status+")");
            try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>12*1024*1024)throw new Exception("Ответ VK слишком большой");}
                String text=out.toString("UTF-8"); if(!text.trim().startsWith("{"))throw new Exception("VK вернул страницу входа. Войдите повторно");
                return new JSONObject(text);
            }
        } finally { conn.disconnect(); }
    }
    synchronized void refresh() throws Exception {
        JSONObject response=post("https://login.vk.ru/?act=web_token",params("version","1","app_id","6287487"),true);
        JSONObject data=response.optJSONObject("data");
        if(data==null||data.optString("access_token").isEmpty()) throw new Exception("VK не подтвердил вход. Завершите авторизацию на странице VK");
        long expires=data.optLong("expires",3600); if(expires<1000000000L)expires+=System.currentTimeMillis()/1000;
        session=params("token",data.getString("access_token"),"expires",expires);store.save(session);
    }
    synchronized Object call(String method, JSONObject values) throws Exception {
        if(!connected())throw new Exception("Войдите в VK в настройках");
        if(session.optLong("expires")<System.currentTimeMillis()/1000+90)refresh();
        for(int attempt=0;attempt<2;attempt++) {
            JSONObject body=new JSONObject(values.toString());body.put("v",API_VERSION).put("lang","ru").put("client_id","6287487").put("access_token",session.getString("token"));
            JSONObject response=post("https://api.vk.ru/method/"+method,body,false),error=response.optJSONObject("error");
            if(error==null) return response.get("response");
            int code=error.optInt("error_code");
            if(code==5&&attempt==0){refresh();continue;}
            if(code==14)throw new Exception("VK запросил капчу. Откройте вход в VK и подтвердите её");
            if(code==15||code==201||code==203)throw new Exception("VK ограничил доступ к этому разделу ("+code+")");
            throw new Exception("VK: "+error.optString("error_msg","ошибка "+code));
        }
        throw new Exception("Войдите в VK повторно");
    }
    JSONObject profile() throws Exception { Object r=call("users.get",params());return r instanceof JSONArray?((JSONArray)r).getJSONObject(0):new JSONObject(); }
}
