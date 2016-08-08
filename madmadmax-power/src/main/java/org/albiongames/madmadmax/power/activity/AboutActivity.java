package org.albiongames.madmadmax.power.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import org.albiongames.madmadmax.power.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView webView = (WebView)findViewById(R.id.webView);

        webView.loadUrl("file:///android_asset/about.html");
    }
}
