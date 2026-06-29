package com.yourbrand.casino.payment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Jazpays Payment Gateway - Simple Redirect
 * 
 * User clicks deposit → amount set → direct redirect to Jazpays URL
 * NO custom UI. Just builds URL and opens in browser/WebView.
 */
public class PaymentWebView extends Activity {

    private static final String MERCHANT_ID = "100222099";
    private static final String API_KEY = "25aa23a6200008a506628fa5f971fc1d";
    private static final String API_URL = "https://api.jazpays.com/v1/create";
    private static final String CALLBACK_URL = "https://your-server.com/payment/callback";

    private WebView webView;
    private boolean paymentDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String amount = getIntent().getStringExtra("amount");
        if (amount == null || amount.isEmpty()) amount = "100";

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        setContentView(webView);

        String orderNo = "ORD" + System.currentTimeMillis();
        String signStr = "amount=" + amount +
                        "&callback_url=" + CALLBACK_URL +
                        "&merchant_id=" + MERCHANT_ID +
                        "&merchant_order_no=" + orderNo +
                        "&key=" + API_KEY;
        String signature = md5(signStr);

        // Build Jazpays payment URL with params
        String paymentUrl = API_URL +
            "?merchant_id=" + MERCHANT_ID +
            "&amount=" + amount +
            "&merchant_order_no=" + orderNo +
            "&callback_url=" + CALLBACK_URL +
            "&api_key=" + API_KEY +
            "&signature=" + signature;

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("callback") || url.contains("success")) {
                    paymentDone = true;
                    Toast.makeText(PaymentWebView.this,
                        "Payment processing...", Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(() -> finish(), 2000);
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(paymentUrl);
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
