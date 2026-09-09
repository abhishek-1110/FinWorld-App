
package com.finworld.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private WebView webView;
    private WebView popupWebView;
    private ProgressBar progressBar;
    private FrameLayout rootLayout;

    private static final String[] OWNED_SITE_MATCHES = {
            "finworldtaxation.com",
            "www.finworldtaxation.com"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root layout
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);

        // Main WebView
        webView = new WebView(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // Required for Firebase signInWithPopup()
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Loader
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        rootLayout.addView(
                webView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        FrameLayout.LayoutParams loaderParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );

        loaderParams.gravity = Gravity.CENTER;

        rootLayout.addView(progressBar, loaderParams);

        progressBar.setVisibility(View.VISIBLE);

        setContentView(rootLayout);

        // ---------------------------------------------------------
        // MAIN WEBVIEW CLIENT
        // ---------------------------------------------------------

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    android.graphics.Bitmap favicon
            ) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        // ---------------------------------------------------------
        // WEB CHROME CLIENT
        // ---------------------------------------------------------

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    Message resultMsg
            ) {

                // Close an old popup if one somehow remains
                closePopup();

                popupWebView = new WebView(MainActivity.this);

                WebSettings popupSettings =
                        popupWebView.getSettings();

                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);
                popupSettings.setDatabaseEnabled(true);

                popupSettings.setAllowFileAccess(true);
                popupSettings.setAllowContentAccess(true);

                popupSettings.setSupportMultipleWindows(true);
                popupSettings.setJavaScriptCanOpenWindowsAutomatically(true);

                // -------------------------------------------------
                // Popup WebView Client
                // -------------------------------------------------

                popupWebView.setWebViewClient(new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {
                        String url = request.getUrl().toString();

                        System.out.println(
                                "GOOGLE POPUP URL: " + url
                        );

                        return handlePopupUrl(url);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url
                    ) {
                        System.out.println(
                                "GOOGLE POPUP URL: " + url
                        );

                        return handlePopupUrl(url);
                    }

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            android.graphics.Bitmap favicon
                    ) {
                        super.onPageStarted(view, url, favicon);

                        System.out.println(
                                "POPUP PAGE STARTED: " + url
                        );
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        super.onPageFinished(view, url);

                        System.out.println(
                                "POPUP PAGE FINISHED: " + url
                        );
                    }
                });

                // -------------------------------------------------
                // Popup Chrome Client
                // -------------------------------------------------

                popupWebView.setWebChromeClient(this);

                // -------------------------------------------------
                // IMPORTANT:
                //
                // Firebase expects a real popup window.
                // The popup MUST be added to the Android layout.
                // -------------------------------------------------

                FrameLayout.LayoutParams popupParams =
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        );

                rootLayout.addView(
                        popupWebView,
                        popupParams
                );

                popupWebView.bringToFront();

                // Keep loader behind popup
                progressBar.bringToFront();

                // Give the popup to Android WebView
                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;

                transport.setWebView(popupWebView);

                resultMsg.sendToTarget();

                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {

                System.out.println(
                        "GOOGLE POPUP CLOSED"
                );

                closePopup();

                super.onCloseWindow(window);
            }
        });

        // ---------------------------------------------------------
        // HANDLE APP INTENT
        // ---------------------------------------------------------

        handleIncomingUrl(getIntent());

        // ---------------------------------------------------------
        // LOAD WEBSITE
        // ---------------------------------------------------------

        webView.loadUrl(
                "https://finworldtaxation.com"
        );
    }

    // -------------------------------------------------------------
    // HANDLE POPUP URL
    // -------------------------------------------------------------

    private boolean handlePopupUrl(String url) {

        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        System.out.println(
                "POPUP URL: " + url
        );

        // IMPORTANT:
        //
        // Do NOT send Google URLs to an external browser.
        //
        // Firebase signInWithPopup() needs the popup to remain
        // associated with the original WebView.
        //
        // Therefore Google/Firebase URLs are allowed to continue
        // inside popupWebView.
        //

        if (url.startsWith("tel:")) {
            return handleUrl(url);
        }

        if (url.startsWith("mailto:")) {
            return handleUrl(url);
        }

        if (url.startsWith("whatsapp:")) {
            return handleUrl(url);
        }

        return false;
    }

    // -------------------------------------------------------------
    // NORMAL MAIN WEBVIEW URL HANDLING
    // -------------------------------------------------------------

    private boolean handleUrl(String url) {

        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        System.out.println(
                "MAIN WEBVIEW URL: " + url
        );

        // PHONE
        if (url.startsWith("tel:")) {

            try {
                Intent intent =
                        new Intent(Intent.ACTION_DIAL);

                intent.setData(Uri.parse(url));

                startActivity(intent);

            } catch (ActivityNotFoundException e) {

                Toast.makeText(
                        this,
                        "No phone application found on this device",
                        Toast.LENGTH_LONG
                ).show();
            }

            return true;
        }

        // EMAIL
        if (url.startsWith("mailto:")) {

            try {
                Intent intent =
                        new Intent(Intent.ACTION_SENDTO);

                intent.setData(Uri.parse(url));

                startActivity(intent);

            } catch (ActivityNotFoundException e) {

                Toast.makeText(
                        this,
                        "No email application found",
                        Toast.LENGTH_LONG
                ).show();
            }

            return true;
        }

        // WHATSAPP
        if (url.startsWith("whatsapp:")) {

            try {
                Intent intent =
                        new Intent(Intent.ACTION_VIEW);

                intent.setData(Uri.parse(url));

                startActivity(intent);

            } catch (ActivityNotFoundException e) {

                Toast.makeText(
                        this,
                        "WhatsApp is not installed",
                        Toast.LENGTH_LONG
                ).show();
            }

            return true;
        }

        // WHATSAPP WEB
        if (url.startsWith("https://wa.me/") ||
                url.startsWith("https://api.whatsapp.com/")) {

            try {

                Intent intent =
                        new Intent(Intent.ACTION_VIEW);

                intent.setData(Uri.parse(url));

                startActivity(intent);

            } catch (ActivityNotFoundException e) {

                return false;
            }

            return true;
        }

        // ---------------------------------------------------------
        // IMPORTANT:
        //
        // All normal HTTPS website URLs remain inside WebView.
        // Google OAuth is NOT intercepted here.
        // ---------------------------------------------------------

        return false;
    }

    // -------------------------------------------------------------
    // CLOSE GOOGLE POPUP
    // -------------------------------------------------------------

    private void closePopup() {

        if (popupWebView != null) {

            try {
                popupWebView.stopLoading();
                popupWebView.loadUrl("about:blank");
                popupWebView.clearHistory();
                popupWebView.removeAllViews();
                popupWebView.destroy();
            } catch (Exception ignored) {
            }

            rootLayout.removeView(popupWebView);

            popupWebView = null;
        }
    }

    // -------------------------------------------------------------
    // HANDLE DEEP-LINK / APP INTENT
    // -------------------------------------------------------------

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        setIntent(intent);

        handleIncomingUrl(intent);
    }

    private void handleIncomingUrl(Intent intent) {

        if (intent == null || webView == null) {
            return;
        }

        Uri data = intent.getData();

        if (data != null) {

            String url = data.toString();

            if (url.contains("finworldtaxation.com")) {

                webView.loadUrl(url);
            }
        }
    }

    // -------------------------------------------------------------
    // BACK BUTTON
    // -------------------------------------------------------------

    @Override
    public void onBackPressed() {

        // If Google popup is currently open,
        // close it first.

        if (popupWebView != null) {

            closePopup();

            return;
        }

        // Otherwise navigate main WebView history

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}

