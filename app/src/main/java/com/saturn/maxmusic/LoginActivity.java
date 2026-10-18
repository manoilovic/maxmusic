package com.saturn.maxmusic;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.common.util.UnstableApi;

/** Separate VK-owned sign-in page; it has no JavaScript-to-Android bridge. */
@UnstableApi
public final class LoginActivity extends Activity {
    private WebView browser;
    private Button done;
    private TextView address;
    private boolean verifying;
    static boolean isVkAddress(Uri uri){String host=uri.getHost();return "https".equals(uri.getScheme())&&host!=null&&(host.equals("vk.ru")||host.endsWith(".vk.ru")||host.equals("vk.com")||host.endsWith(".vk.com")||host.equals("vk.id")||host.endsWith(".vk.id")||host.equals("vkid.ru")||host.endsWith(".vkid.ru"));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(11,13,17));
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout bar=new LinearLayout(this);bar.setPadding(8,8,8,8);bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("‹");back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(52,52));
        address=new TextView(this);address.setText("Вход на vk.ru");address.setTextColor(Color.WHITE);address.setTextSize(13);bar.addView(address,new LinearLayout.LayoutParams(0,-2,1));
        done=new Button(this);done.setText("Готово");done.setOnClickListener(v->verify());bar.addView(done);root.addView(bar);
        browser=new WebView(this);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setAllowFileAccess(false);browser.getSettings().setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true);
        browser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !isVkAddress(request.getUrl());}
            @Override public void onPageFinished(WebView view,String url){Uri uri=Uri.parse(url);address.setText(uri.getHost()==null?"Вход в VK":uri.getHost());CookieManager.getInstance().flush();}
        });root.addView(browser,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);browser.loadUrl("https://vk.ru/");
    }
    private void verify(){if(verifying)return;verifying=true;done.setEnabled(false);address.setText("Проверка входа…");
        AppGraph graph=AppGraph.get(this);graph.vk.userAgent=browser.getSettings().getUserAgentString();CookieManager.getInstance().flush();
        graph.io.execute(()->{try{graph.vk.refresh();runOnUiThread(()->{setResult(RESULT_OK);finish();});}catch(Exception error){runOnUiThread(()->{verifying=false;done.setEnabled(true);address.setText(error.getMessage());});}});
    }
    @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(browser!=null){browser.stopLoading();browser.destroy();}super.onDestroy();}
}
