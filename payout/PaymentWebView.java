package com.yourbrand.casino.payment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

/**
 * Jazpays Payment Gateway Integration
 * 
 * Merchant ID: 100222099
 * API Key: 25aa23a6200008a506628fa5f971fc1d
 * Endpoint: https://api.jazpays.com/v1/create
 * 
 * Signature: MD5( sorted params + "&key=API_KEY" )
 * Sort: amount -> callback_url -> merchant_id -> merchant_order_no
 */
public class PaymentWebView extends Activity {

    private static final String MERCHANT_ID = "100222099";
    private static final String API_KEY = "25aa23a6200008a506628fa5f971fc1d";
    private static final String API_URL = "https://api.jazpays.com/v1/create";
    private static final String CALLBACK_URL = "https://your-server.com/payment/callback";

    private WebView webView;
    private LinearLayout amountLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Title
        TextView title = new TextView(this);
        title.setText("Buy Coins");
        title.setTextSize(24);
        title.setPadding(20, 40, 20, 20);
        mainLayout.addView(title);

        // Amount input
        amountLayout = new LinearLayout(this);
        amountLayout.setOrientation(LinearLayout.VERTICAL);
        amountLayout.setPadding(40, 20, 40, 20);

        final EditText amountInput = new EditText(this);
        amountInput.setHint("Enter amount (INR)");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        amountLayout.addView(amountInput);

        // Pay button
        Button payBtn = new Button(this);
        payBtn.setText("Pay via Jazpays");
        payBtn.setOnClickListener(v -> {
            String amount = amountInput.getText().toString();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }
            startPayment(amount);
        });
        amountLayout.addView(payBtn);

        // Quick amounts
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String amt : new String[]{"100", "500", "1000", "5000"}) {
            Button b = new Button(this);
            b.setText("₹" + amt);
            b.setOnClickListener(v -> {
                amountInput.setText(amt);
                startPayment(amt);
            });
            quickRow.addView(b);
        }
        amountLayout.addView(quickRow);

        mainLayout.addView(amountLayout);

        // WebView (hidden initially)
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        webView.setVisibility(android.view.View.GONE);
        webView.addJavascriptInterface(new JsBridge(), "Android");
        mainLayout.addView(webView);

        setContentView(mainLayout);
    }

    private void startPayment(String amount) {
        amountLayout.setVisibility(android.view.View.GONE);
        webView.setVisibility(android.view.View.VISIBLE);

        // Generate signature
        String orderNo = "ORD" + System.currentTimeMillis();
        String signStr = "amount=" + amount + 
                        "&callback_url=" + CALLBACK_URL + 
                        "&merchant_id=" + MERCHANT_ID + 
                        "&merchant_order_no=" + orderNo + 
                        "&key=" + API_KEY;
        String signature = md5(signStr);

        // Build payment page HTML with Jazpays form
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<style>body{font-family:sans-serif;padding:20px;background:#1a1a2e;color:#fff}" +
            ".container{max-width:400px;margin:0 auto;padding:20px}" +
            "h2{text-align:center;color:#ffd700}" +
            ".info{background:#16213e;padding:15px;border-radius:10px;margin:10px 0}" +
            ".btn{background:#ffd700;color:#000;padding:12px 24px;border:none;" +
            "border-radius:8px;font-size:16px;width:100%;cursor:pointer}" +
            ".btn:hover{background:#ffed4a}</style></head><body>" +
            "<div class='container'>" +
            "<h2>💰 Jazpays Payment</h2>" +
            "<div class='info'><p><strong>Amount: ₹" + amount + "</strong></p>" +
            "<p>Order: " + orderNo + "</p>" +
            "<p>Merchant: " + MERCHANT_ID + "</p></div>" +
            "<form id='payForm' action='" + API_URL + "' method='POST'>" +
            "<input type='hidden' name='merchant_id' value='" + MERCHANT_ID + "'>" +
            "<input type='hidden' name='amount' value='" + amount + "'>" +
            "<input type='hidden' name='merchant_order_no' value='" + orderNo + "'>" +
            "<input type='hidden' name='callback_url' value='" + CALLBACK_URL + "'>" +
            "<input type='hidden' name='api_key' value='" + API_KEY + "'>" +
            "<input type='hidden' name='signature' value='" + signature + "'>" +
            "</form>" +
            "<button class='btn' onclick='submitForm()'>💳 Pay Now</button>" +
            "<p style='text-align:center;margin-top:20px;font-size:12px;color:#888'>" +
            "Powered by Jazpays</p>" +
            "</div>" +
            "<script>function submitForm(){document.getElementById('payForm').submit();}" +
            "</script></body></html>";

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private class JsBridge {
        @JavascriptInterface
        public void onPaymentSuccess(String orderNo, String amount) {
            runOnUiThread(() -> {
                Toast.makeText(PaymentWebView.this, 
                    "Payment successful! ₹" + amount, Toast.LENGTH_LONG).show();
                finish();
            });
        }

        @JavascriptInterface
        public void onPaymentFailed(String error) {
            runOnUiThread(() -> {
                Toast.makeText(PaymentWebView.this, 
                    "Payment failed: " + error, Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
