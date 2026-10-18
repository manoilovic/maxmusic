package com.saturn.maxmusic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

final class ArtworkPalette {
    private static final Map<String,JSONObject> cache=new ConcurrentHashMap<>();
    static JSONObject extract(String cover) throws Exception {
        if(cover.isEmpty())return new JSONObject();if(cache.containsKey(cover))return cache.get(cover);
        byte[] bytes;
        if(cover.startsWith("data:image/")){int comma=cover.indexOf(',');bytes=Base64.decode(cover.substring(comma+1),Base64.DEFAULT);}
        else {
            if(!LibraryRepository.safeHttps(cover))return new JSONObject();
            HttpURLConnection connection=(HttpURLConnection)new URL(cover).openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(12000);
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>5*1024*1024)throw new Exception("Обложка слишком большая");}bytes=out.toByteArray();}finally{connection.disconnect();}
        }
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;
        Bitmap decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(decoded==null)return new JSONObject();
        Bitmap image=Bitmap.createScaledBitmap(decoded,40,40,true);Map<Integer,double[]> buckets=new HashMap<>();
        for(int y=0;y<40;y++)for(int x=0;x<40;x++){int p=image.getPixel(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255,hi=Math.max(r,Math.max(g,b)),lo=Math.min(r,Math.min(g,b));if(hi<28||lo>235)continue;int key=(r>>5)*64+(g>>5)*8+(b>>5);double[] c=buckets.computeIfAbsent(key,k->new double[4]);double weight=1+(hi-lo)/255.0*.65;c[0]+=weight;c[1]+=r*weight;c[2]+=g*weight;c[3]+=b*weight;}
        image.recycle();if(decoded!=image)decoded.recycle();
        double[] primary=null;for(double[] c:buckets.values())if(primary==null||c[0]>primary[0])primary=c;
        if(primary==null)return new JSONObject();for(double[] c:buckets.values()){c[1]/=c[0];c[2]/=c[0];c[3]/=c[0];}
        double[] secondary=primary;double best=0;for(double[] c:buckets.values()){double dist=Math.sqrt(Math.pow(c[1]-primary[1],2)+Math.pow(c[2]-primary[2],2)+Math.pow(c[3]-primary[3],2));double score=dist*c[0];if(dist>45&&c[0]>=primary[0]*.08&&score>best){best=score;secondary=c;}}
        JSONObject result=VkClient.params("primary",color(primary),"secondary",color(secondary));cache.put(cover,result);return result;
    }
    private static String color(double[] c){return "rgb("+Math.round(c[1])+" "+Math.round(c[2])+" "+Math.round(c[3])+")";}
}
