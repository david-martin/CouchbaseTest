package com.example.dmartin.couchbasetest;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.net.URL;
/**
 * Created by dmartin on 26/01/18.
 */


/**
 * This is a WebView Dialog which is opened to a OAuth2 sign in page and sends
 * data back to the calling activity.
 *
 */
public class CouchbaseOAuthWebViewDialog extends DialogFragment {

    private static final String TAG = CouchbaseOAuthWebViewDialog.class.getSimpleName();
    private static final String TITLE = "com.example.dmartin.couchbasetest.CouchbaseOAuthWebViewDialog.TITLE";
    private static final String AUTHORIZE_URL = "com.example.dmartin.couchbasetest.CouchbaseOAuthWebViewDialog.AUTHORIZE_URL";
    private static final String REDIRECT_URL = "com.example.dmartin.couchbasetest.CouchbaseOAuthWebViewDialog.REDIRECT_URL";

    private WebView webView;
    private ProgressBar progressBar;
    private String authorizeUrl;
    final private OAuthViewClient client = new CouchbaseOAuthWebViewDialog.OAuthViewClient() {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            progressBar.setVisibility(View.GONE);
        }

    };
    private String redirectURL;

    private class OAuthViewClient extends WebViewClient {

        private OAuthReceiver receiver;
        private String redirectURL;

        @Override
        public void onPageFinished(WebView view, final String url) {
            // Lets not follow the redirect for couchbase
            // See https://developer.couchbase.com/documentation/mobile/1.5/guides/authentication/openid/index.html#build-your-own-login-ui
            // Just finish up before redirect
            Log.d("app", String.format("onPageFinished with url %s", url));
            if (receiver != null) {
                final OAuthReceiver receiverRef = receiver;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        receiverRef.receiveLoginAttempted(url);
                    }
                });
            }
//            if (url.startsWith(redirectURL)) {

//                if (url.contains("code=")) {
//                    final String token = fetchToken(url);
//                    Log.d("TOKEN", token);
//                    if (receiver != null) {
//                        final CouchbaseOAuthWebViewDialog.OAuthReceiver receiverRef = receiver;
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                receiverRef.receiveOAuthCode(token);
//                            }
//                        });
//                    }
//                    return;
//                } else if (url.contains("error=")) {
//                    final String error = fetchError(url);
//                    Log.d("ERROR", error);
//                    if (receiver != null) {
//                        final CouchbaseOAuthWebViewDialog.OAuthReceiver receiverRef = receiver;
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                receiverRef.receiveOAuthError(error);
//                            }
//                        });
//                    }
//                    return;
//                }
            }

//            super.onPageFinished(view, url);

//        }

//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            if (url.startsWith(redirectURL)) {
//                if (url.contains("code=")) {
//                    final String token = fetchToken(url);
//                    Log.d("TOKEN", token);
//                    if (receiver != null) {
//                        final CouchbaseOAuthWebViewDialog.OAuthReceiver receiverRef = receiver;
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                receiverRef.receiveOAuthCode(token);
//                            }
//                        });
//                    }
//                    return true;
//                } else if (url.contains("error=")) {
//                    final String error = fetchError(url);
//                    Log.d("ERROR", error);
//                    if (receiver != null) {
//                        final CouchbaseOAuthWebViewDialog.OAuthReceiver receiverRef = receiver;
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                receiverRef.receiveOAuthError(error);
//                            }
//                        });
//                    }
//                    return true;
//                }
//            }
//            return false;
//
//        }

        private String fetchToken(String url) {
            return fetchURLParam(url, "code");
        }

        private String fetchError(String url) {
            return fetchURLParam(url, "error");
        }

        private String fetchURLParam(String url, String param) {
            Uri uri = Uri.parse(url);
            return uri.getQueryParameter(param);
        }
    }

    public static CouchbaseOAuthWebViewDialog newInstance(URL authorizeURL, URL redirectURL) {
        CouchbaseOAuthWebViewDialog instance = new CouchbaseOAuthWebViewDialog();
        instance.authorizeUrl = authorizeURL.toString();
        instance.redirectURL = redirectURL.toString();

        Bundle args = new Bundle();
        args.putString(AUTHORIZE_URL, instance.authorizeUrl);
        args.putString(REDIRECT_URL, instance.redirectURL);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onViewCreated(View arg0, Bundle arg1) {
        super.onViewCreated(arg0, arg1);

        client.redirectURL = redirectURL;

        webView.loadUrl(authorizeUrl);
        webView.setWebViewClient(client);

        // activates JavaScript (just in case)
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.authorizeUrl = getArguments().getString(AUTHORIZE_URL);
        this.redirectURL = getArguments().getString(REDIRECT_URL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.couchbase_oauth_web_view, container, false);

        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);

        webView = (WebView) v.findViewById(R.id.web_oauth);
        webView.setScrollContainer(true);
        getDialog().getWindow().setTitle("OAuth 2 Dialog");
        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (client.receiver != null) {
            client.receiver.receiveOAuthError(OAuthReceiver.DISMISS_ERROR);
        }
    }

    public void setReceiver(OAuthReceiver receiver) {
        client.receiver = receiver;
    }

    public void removeReceive() {
        client.receiver = null;
    }

}
