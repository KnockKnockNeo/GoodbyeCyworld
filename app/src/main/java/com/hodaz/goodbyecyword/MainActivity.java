package com.hodaz.goodbyecyword;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "GoodbyeCyworld";
    private static final String KEY_CYWORLD_ID = "KEY_CYWORLD_ID";
    private static final String API_GET_FOLDER = "http://cy.cyworld.com/home/%1$s/menu/?type=folder";

    private Context mContext;
    private WebView mWebView;
    private Button mCyID;
    private Button mFolder;
    private Button mBackup;
    private String mCyworldId;
    private boolean existID = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        mWebView = (WebView) findViewById(R.id.webview);
        mCyID = (Button) findViewById(R.id.cyid);
        mFolder = (Button) findViewById(R.id.folder);
        mFolder.setOnClickListener(this);
        mBackup = (Button) findViewById(R.id.backup);

        initWebView();
    }

    private void initWebView() {
        final WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setBuiltInZoomControls(true);

        mWebView.addJavascriptInterface(new HttpCrawlingInterface(this), "HtmlViewer");
        mWebView.setWebChromeClient(new WebChromeClient() {

        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                Log.e(TAG, "url : " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (!existID) {
                    view.loadUrl("javascript:window.HtmlViewer.showHTML" +
                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'<script>var currentUrl=\"startc"+url+"endc\"</script></html>');");
                }
                else if (url.contains("logout.jsp")) {
                    PreferenceUtil.getInstance().remove(mContext, KEY_CYWORLD_ID);
                }
            }
        });
        mWebView.loadUrl("http://m.cyworld.com");

//        CookieSyncManager.createInstance(this);
//        CookieManager cookieManager = CookieManager.getInstance();
//        String cookieString = "param=value";
//        cookieManager.setCookie("http://cy.cyworld.com", cookieString);
//        CookieSyncManager.getInstance().sync();
//
//        Map<String, String> abc = new HashMap<String, String>();
//        abc.put("Cookie", cookieString);
//        mWebView.loadUrl("http://abc-site.com/a1/namedFolder/file", abc);
    }

    private String getCookie() {
        String CookieValue = null;

        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie("http://cy.cyworld.com");
        if (cookies != null) {
            String[] temp = cookies.split(";");
            for (String ar1 : temp) {
                String[] temp1 = ar1.split("=");
                CookieValue = temp1[1];
                Log.e("Cyworld Cookie", ar1);
            }
        }
        return CookieValue;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.folder:
                new AsyncTask<Void, Void, Document>() {
                    private String url;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();

                        url = String.format(API_GET_FOLDER, mCyID.getText());
                        CommonLog.e(TAG, "api_get_folder : " + url);
                    }

                    @Override
                    protected Document doInBackground(Void... params) {
                        Document doc = null;
                        try {
                            doc = Jsoup.connect(url).get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return doc;
                    }

                    @Override
                    protected void onPostExecute(Document result) {
                        super.onPostExecute(result);

                        if (result != null) {
                            Elements menus = result.getElementsByClass("tree2"); //.get(0).getElementsByTag("input");
                            Elements menusTitles = result.getElementsByClass("tree2"); //.get(0).getElementsByTag("em");
                            for (Element e : menus) {
                                Elements menuidElements = e.getElementsByTag("input");
                                for (Element e2 : menuidElements) {
                                    CommonLog.e(TAG, e2.attributes().get("value"));
                                }
                            }
                            for (Element e: menus) {
                                Elements titleElements = e.getElementsByTag("em");
                                for (Element e2 : titleElements) {
                                    CommonLog.e(TAG, e2.val());
                                }
                            }
                        }
                    }
                }.execute();
                break;
        }
    }

    class HttpCrawlingInterface {
        private Context ctx;

        HttpCrawlingInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void showHTML(String html) {
//            new AlertDialog.Builder(ctx).setTitle("HTML").setMessage(html)
//                    .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();

            String url = html.substring(html.indexOf("startc")+6, html.indexOf("endc"));
            Log.e(TAG, "url : " + url);

            if (url.equals("http://m.cyworld.com/")) {
                int startPos = html.indexOf("내 싸이홈가기");
                if (startPos > 0) {
                    String homeUrl = html.substring(startPos - 37, startPos - 2);
                    Log.e(TAG, "homeUrl : " + homeUrl);

                    if (homeUrl.startsWith("http")) {
                        final String id = homeUrl.replace("http://cy.cyworld.com/home/", "");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                existID = true;
                                mCyID.setText(id);
                            }
                        });

                        PreferenceUtil.getInstance().putString(ctx, KEY_CYWORLD_ID, id);
                        Log.e(TAG, "id : " + id);
                    }
                }
            }
        }
    }
}
