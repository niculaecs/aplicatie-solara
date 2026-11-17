package eu.puhoi.productie;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** A trusted WebView shell around the existing Puhoi V8 dashboard. */
public class MainActivity extends Activity {
    private static final String BASE_URL = "https://solar.puhoi.eu/";
    private static final String TRUSTED_HOST = "solar.puhoi.eu";
    private static final int NAVY = Color.rgb(6, 22, 40);
    private static final int NAVY2 = Color.rgb(11, 39, 64);
    private static final int GOLD = Color.rgb(255, 196, 57);
    private static final int MUTED = Color.rgb(159, 181, 200);
    private WebView web;
    private FrameLayout webFrame;
    private ProgressBar progress;
    private TextView status;
    private LinearLayout offline;
    private LinearLayout splash;
    private SecureCredentials credentials;
    private boolean triedStoredCredentials = false;
    private boolean showingAuthDialog = false;

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private TextView text(String label, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        credentials = new SecureCredentials(this);
        showApp();
        web.loadUrl(BASE_URL);
    }
    private void showApp() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(NAVY);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(17), dp(9), dp(12), dp(9));
        header.setBackgroundColor(NAVY);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));

        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.brand_icon);
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(7), 0, 0, 0);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        titles.addView(text("Producție Puhoi", 20, Color.WHITE, true));
        titles.addView(text("SOLAR & LIVE MONITORING", 9, MUTED, false));

        status = text("● ONLINE", 10, Color.rgb(86, 231, 139), true);
        header.addView(status);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));
        progress.setIndeterminate(false);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        webFrame = new FrameLayout(this);
        root.addView(webFrame, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this);
        web.setBackgroundColor(NAVY);
        webFrame.addView(web, new FrameLayout.LayoutParams(-1, -1));
        configureWebView();

        offline = new LinearLayout(this);
        offline.setOrientation(LinearLayout.VERTICAL);
        offline.setGravity(Gravity.CENTER);
        offline.setPadding(dp(28), dp(18), dp(28), dp(18));
        offline.setBackgroundColor(NAVY2);
        TextView errorTitle = text("Conexiunea nu este disponibilă", 19, Color.WHITE, true);
        errorTitle.setGravity(Gravity.CENTER);
        offline.addView(errorTitle);
        TextView errorMsg = text("Verifică internetul și încearcă din nou.", 13, MUTED, false);
        errorMsg.setGravity(Gravity.CENTER);
        offline.addView(errorMsg);
        Button retry = new Button(this);
        retry.setText("REÎNCEARCĂ");
        retry.setTextColor(NAVY);
        retry.setBackground(bg(GOLD, 12));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(180), dp(48));
        retryParams.topMargin = dp(20);
        offline.addView(retry, retryParams);
        retry.setOnClickListener(v -> reload());
        webFrame.addView(offline, new FrameLayout.LayoutParams(-1, -1));
        offline.setVisibility(View.GONE);

        splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(NAVY);
        android.widget.ImageView splashLogo = new android.widget.ImageView(this);
        splashLogo.setImageResource(R.drawable.brand_icon);
        splash.addView(splashLogo, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView splashTitle = text("Producție Puhoi", 27, Color.WHITE, true);
        splashTitle.setGravity(Gravity.CENTER);
        splash.addView(splashTitle);
        TextView splashSub = text("SOLAR & LIVE MONITORING", 12, GOLD, true);
        splashSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-2, -2);
        subParams.topMargin = dp(9);
        splash.addView(splashSub, subParams);
        ProgressBar splashLoading = new ProgressBar(this);
        LinearLayout.LayoutParams loadParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        loadParams.topMargin = dp(32);
        splash.addView(splashLoading, loadParams);
        webFrame.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(NAVY);
        nav.setPadding(dp(4), dp(5), dp(4), dp(5));
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(67)));
        addNav(nav, "⌂", "Acasă", "acasa");
        addNav(nav, "ϟ", "Energie", "energie");
        addNav(nav, "▣", "Camere", "camere");
        addNav(nav, "◉", "LIVE", "monitor");
        addNav(nav, "☰", "Meniu", "meniu");
    }
    private void addNav(LinearLayout nav, String symbol, String caption, String action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        TextView icon = text(symbol, 21, action.equals("acasa") ? GOLD : MUTED, true);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon);
        TextView label = text(caption, 10, action.equals("acasa") ? GOLD : MUTED, false);
        label.setGravity(Gravity.CENTER);
        item.addView(label);
        item.setOnClickListener(v -> navigate(action));
    }
    private void navigate(String action) {
        if (web == null) return;
        if ("meniu".equals(action)) {
            // Original sidebar remains the authoritative source of navigation.
            web.evaluateJavascript("(function(){var n=document.querySelector('.nav');if(n){var x=n.querySelector('[data-screen]');if(x){x.focus();} n.scrollIntoView({behavior:'smooth',block:'start'});return true;}return false;})()", null);
            Toast.makeText(this, "Folosește meniul din dashboard", Toast.LENGTH_SHORT).show();
            return;
        }
        final String term = action.equals("energie") ? "energie" :
                action.equals("camere") ? "camer" : action.equals("monitor") ? "monitor" : "acas";
        String js = "(function(){"+
                "var links=[].slice.call(document.querySelectorAll('[data-screen]'));"+
                "var q='"+term+"';"+
                "var a=links.find(function(x){return ((x.textContent||'')+' '+(x.getAttribute('data-screen')||'')).toLowerCase().includes(q)});"+
                "if(!a&&q==='acas')a=links.find(function(x){return /home|overview|dashboard|principal/i.test(x.getAttribute('data-screen')||'')});"+
                "if(a){a.click();return true;}return false;})()";
        web.evaluateJavascript(js, result -> {
            if ("false".equals(result)) {
                if ("monitor".equals(action)) web.loadUrl(BASE_URL + "#monitor");
                else Toast.makeText(this, "Deschide secțiunea din meniul site-ului", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private boolean permitted(Uri uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && TRUSTED_HOST.equalsIgnoreCase(uri.getHost());
    }
    private void openExternally(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "Nu există o aplicație pentru această adresă", Toast.LENGTH_SHORT).show();
        }
    }
    private void configureWebView() {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.INVISIBLE : View.VISIBLE);
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (permitted(uri)) return false;
                if (request.isForMainFrame()) { openExternally(uri); return true; }
                return false; // Allow HTTPS map tiles, weather data, etc. as subresources.
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // One stored credential attempt per page; failed credentials trigger user prompt.
                triedStoredCredentials = false;
                offline.setVisibility(View.GONE);
                status.setText("● CONECTARE");
                status.setTextColor(GOLD);
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (splash != null) splash.setVisibility(View.GONE);
                status.setText("● ONLINE");
                status.setTextColor(Color.rgb(86,231,139));
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    if (splash != null) splash.setVisibility(View.GONE);
                    offline.setVisibility(View.VISIBLE);
                    status.setText("● OFFLINE");
                    status.setTextColor(Color.rgb(255,110,110));
                }
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); // Never bypass certificate errors.
                if (splash != null) splash.setVisibility(View.GONE);
                offline.setVisibility(View.VISIBLE);
                status.setText("● EROARE HTTPS");
                status.setTextColor(Color.rgb(255,110,110));
            }
            @Override public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler,
                                                             String host, String realm) {
                if (!TRUSTED_HOST.equalsIgnoreCase(host)) { handler.cancel(); return; }
                if (!triedStoredCredentials) {
                    triedStoredCredentials = true;
                    String[] saved = credentials.read();
                    if (saved != null) { handler.proceed(saved[0], saved[1]); return; }
                } else {
                    credentials.clear(); // Stored password was rejected.
                }
                if (showingAuthDialog) { handler.cancel(); return; }
                showLogin(handler);
            }
        });
    }
    private void showLogin(HttpAuthHandler handler) {
        showingAuthDialog = true;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(10), dp(22), dp(4));
        EditText user = new EditText(this);
        user.setSingleLine(true);
        user.setHint("Utilizator");
        user.setText("puhoi");
        content.addView(user);
        EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setHint("Parolă");
        pass.setInputType(129);
        content.addView(pass);
        CheckBox remember = new CheckBox(this);
        remember.setText("Memorează autentificarea pe acest dispozitiv");
        remember.setChecked(true);
        content.addView(remember);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Producție Puhoi · Acces securizat")
                .setMessage("Conectare HTTPS la solar.puhoi.eu")
                .setView(content)
                .setPositiveButton("Intră", (d, which) -> {
                    String username = user.getText().toString();
                    String password = pass.getText().toString();
                    if (remember.isChecked()) {
                        try { credentials.save(username, password); }
                        catch (Exception e) { credentials.clear(); }
                    } else credentials.clear();
                    handler.proceed(username, password);
                    showingAuthDialog = false;
                })
                .setNegativeButton("Renunță", (d, which) -> {
                    handler.cancel(); showingAuthDialog = false;
                }).create();
        dialog.setOnCancelListener(d -> { handler.cancel(); showingAuthDialog = false; });
        dialog.show();
    }
    private void reload() {
        offline.setVisibility(View.GONE);
        web.reload();
    }
    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else new AlertDialog.Builder(this).setMessage("Închizi Producție Puhoi?")
                .setPositiveButton("Închide", (d,w)->finish())
                .setNegativeButton("Rămân", null).show();
    }
    @Override protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }
    @Override protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
