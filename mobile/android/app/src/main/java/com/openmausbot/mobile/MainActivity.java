package com.openmausbot.mobile;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String PREFS = "openmausbot_mobile";
    private static final String SERVER_KEY = "server_url";
    private static final int FILE_REQUEST = 4107;

    private LinearLayout root;
    private WebView webView;
    private TextView statusView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private String serverUrl = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17, 23, 34));
        getWindow().setNavigationBarColor(Color.rgb(17, 23, 34));
        serverUrl = getPreferences(MODE_PRIVATE).getString(SERVER_KEY, "");
        if (state != null) {
            String restored = state.getString("server_url");
            if (restored != null) serverUrl = restored;
        }
        if (serverUrl.isEmpty()) showConnectScreen(); else showWebScreen();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("server_url", serverUrl);
        super.onSaveInstanceState(out);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(48, 65, 83)));
        return b;
    }

    private void createRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 23, 34));
        setContentView(root);
    }

    private void showConnectScreen() {
        destroyWebView();
        createRoot();

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(48), dp(24), dp(32));
        scroll.addView(panel);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = text("OpenMausBot", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(title);

        TextView subtitle = text("Connect to your always-on OpenMausBot server", 15,
                Color.rgb(184, 198, 213));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(10);
        panel.addView(subtitle, subtitleParams);

        TextView hint = text("Enter the HTTPS address of your server. Pair or sign in on the page that opens. Do not paste an API key or password here.",
                13, Color.rgb(151, 166, 184));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(24);
        panel.addView(hint, hintParams);

        EditText address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.rgb(125, 139, 156));
        address.setHint("https://your-openmausbot-domain.example");
        address.setText(serverUrl);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setSelectAllOnFocus(true);
        address.setPadding(dp(14), dp(12), dp(14), dp(12));
        address.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(155, 180, 201)));
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(-1, -2);
        addressParams.topMargin = dp(18);
        panel.addView(address, addressParams);

        Button connect = button("Connect");
        LinearLayout.LayoutParams connectParams = new LinearLayout.LayoutParams(-1, -2);
        connectParams.topMargin = dp(16);
        panel.addView(connect, connectParams);
        connect.setOnClickListener(v -> saveAndConnect(address.getText().toString()));

        if (!serverUrl.isEmpty()) {
            Button clear = button("Forget saved server");
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, -2);
            clearParams.topMargin = dp(8);
            panel.addView(clear, clearParams);
            clear.setOnClickListener(v -> {
                serverUrl = "";
                getPreferences(MODE_PRIVATE).edit().remove(SERVER_KEY).apply();
                address.setText("");
                Toast.makeText(this, "Saved server removed", Toast.LENGTH_SHORT).show();
            });
        }

        TextView foot = text("Requires a reachable OpenMausBot server. The agent runs on the server, not on this phone.",
                12, Color.rgb(130, 145, 162));
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(-1, -2);
        footParams.topMargin = dp(28);
        panel.addView(foot, footParams);
    }

    private void saveAndConnect(String raw) {
        String candidate = raw == null ? "" : raw.trim();
        if (candidate.isEmpty()) {
            Toast.makeText(this, "Enter a server address", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse(candidate);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null) {
            Toast.makeText(this, "Use a valid HTTPS URL without username or password", Toast.LENGTH_LONG).show();
            return;
        }
        if (uri.getQuery() != null) {
            Toast.makeText(this, "Use the server base URL without query parameters", Toast.LENGTH_LONG).show();
            return;
        }
        if (candidate.endsWith("/")) candidate = candidate.substring(0, candidate.length() - 1);
        serverUrl = candidate;
        getPreferences(MODE_PRIVATE).edit().putString(SERVER_KEY, serverUrl).apply();
        showWebScreen();
    }

    private void showWebScreen() {
        destroyWebView();
        createRoot();

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(6), dp(2), dp(6), dp(2));
        toolbar.setBackgroundColor(Color.rgb(24, 34, 48));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(52)));

        Button back = button("‹");
        Button reload = button("↻");
        Button settings = button("Server");
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(52), dp(44)));
        statusView = text("Connecting…", 12, Color.rgb(210, 220, 230));
        toolbar.addView(statusView, new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(54), dp(44)));
        toolbar.addView(settings, new LinearLayout.LayoutParams(dp(82), dp(44)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)) return false;
                if ("mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) { }
                    return true;
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (statusView != null) statusView.setText("Loading server…");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (statusView != null) statusView.setText(Uri.parse(url).getHost() == null
                        ? "OpenMausBot" : Uri.parse(url).getHost());
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && statusView != null) statusView.setText("Connection error · tap reload");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                        params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(intent, FILE_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileChooserCallback = null;
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setDescription("Downloading from OpenMausBot");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(false);
                request.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                if (mimeType != null) request.setMimeType(mimeType);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType));
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not start download", Toast.LENGTH_LONG).show();
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        back.setOnClickListener(v -> { if (webView != null && webView.canGoBack()) webView.goBack(); });
        reload.setOnClickListener(v -> { if (webView != null) webView.reload(); });
        settings.setOnClickListener(v -> showConnectScreen());
        webView.loadUrl(serverUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileChooserCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    results = new Uri[] { data.getData() };
                }
            }
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private void destroyWebView() {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }
        if (webView != null) {
            if (root != null) root.removeView(webView);
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }
}
