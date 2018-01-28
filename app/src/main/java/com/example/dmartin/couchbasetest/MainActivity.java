package com.example.dmartin.couchbasetest;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.auth.OIDCLoginCallback;
import com.couchbase.lite.auth.OIDCLoginContinuation;
import com.couchbase.lite.auth.OpenIDConnectAuthenticatorFactory;
import com.couchbase.lite.auth.TokenStore;
import com.couchbase.lite.auth.TokenStoreFactory;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements Replication.ChangeListener {

    private static final String SYNC_GATEWAY_HOST = "http://192.168.1.5:4984";
    private static final String COUCHBASE_DB_NAME = "db";
    private static final String SYNC_GATEWAY_URL = SYNC_GATEWAY_HOST + "/" + COUCHBASE_DB_NAME;

    private Manager manager;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a manager
        try {
            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

//        Activity activity = this;
//        TokenStore store = TokenStoreFactory.build(manager.getContext());
//        Authenticator authenticator = OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(new OIDCLoginCallback() {
//            @Override
//            public void callback(URL loginURL, final URL redirectURL, final OIDCLoginContinuation loginContinuation) {
//                // Open the webview in a new activity
//                Log.d("app", String.format("OIDCLoginCallback loginURL %s redirectURL %s", loginURL, redirectURL));
//
//                final CouchbaseOAuthWebViewDialog dialog = CouchbaseOAuthWebViewDialog.newInstance(loginURL, redirectURL);
//                dialog.setReceiver(new OAuthReceiver() {
//                    @Override
//                    public void receiveLoginAttempted(String url) {
//                        Log.d("app", String.format("login redirect url %s original redirect url %s", url, redirectURL));
//                        URL redirectUrl = null;
//                        try {
//                            redirectUrl = new URL(url);
//                        } catch (MalformedURLException e) {
//                            loginContinuation.callback(null, e);
//                            return;
//                        }
//                        dialog.removeReceive();
//                        dialog.dismiss();
//                        Log.d("app", String.format("loginContinuation redirectUrl=%s", redirectUrl));
//                        loginContinuation.callback(redirectUrl, null);
//                    }
//
//                    @Override
//                    public void receiveOAuthError(String error) {
//                        dialog.removeReceive();
//                        dialog.dismiss();
//                        Log.d("app", String.format("unexpected receiveOAuthError %s", error));
//                    }
//
//                    @Override
//                    public void receiveOAuthCode(String error) {
//                        dialog.removeReceive();
//                        dialog.dismiss();
//                        Log.d("app", String.format("unexpected receiveOAuthCode %s", error));
//                    }
//                });
//                dialog.setStyle(android.R.style.Theme_Light_NoTitleBar, 0);
//                dialog.show(activity.getFragmentManager(), "TAG");
//                activity.setContentView(R.layout.couchbase_oauth_web_view);
//
//            }
//        }, store);

        // Create or open the database named
        try {
            database = manager.getDatabase("db");
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

//         The properties that will be saved on the document
        Map<String, Object> properties = new HashMap<>();
        properties.put("title", "Couchbase Mobile");
        properties.put("sdk", "Android");
// Create a new document
        Document document = database.createDocument();


        document.addChangeListener(new Document.ChangeListener() {
            @Override
            public void changed(Document.ChangeEvent event) {
                Log.d("app", String.format("document changed %s", event.getSource().getProperty("title")));
            }
        });

//         Save the document to the database
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

//         Log the document ID (generated by the database)
//         and properties
        Log.d("app", String.format("Document ID :: %s", document.getId()));
        Log.d("app", String.format("Learning %s with %s", (String) document.getProperty("title"), (String) document.getProperty("sdk")));

        // Create replicators to push & pull changes to & from Sync Gateway.
        URL url = null;
        try {
            url = new URL(SYNC_GATEWAY_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Replication push = database.createPushReplication(url);
        Replication pull = database.createPullReplication(url);

//        Authenticator authenticator = AuthenticatorFactory.createBasicAuthenticator("adminaccount", "password");
//        pull.setAuthenticator(authenticator);
//        push.setAuthenticator(authenticator);

        push.setContinuous(true);
        pull.setContinuous(true);

        push.addChangeListener(this);
        pull.addChangeListener(this);

        // Start replicators
        push.start();
        pull.start();

        setupViewAndQuery();
    }

    public void setupViewAndQuery(){
        Log.d("app", "setupViewAndQuery");


        List<String> docTitles = new ArrayList<>();
        Query query = database.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return;
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Document document = row.getDocument();
            String docDetails = String.format("%s-%s-%s", document.getId(), document.getProperty("title"), document.getProperty("sdk"));
            Log.d("app", String.format("docDetails %s", docDetails));
            docTitles.add(docDetails);
        }

        //Build the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.listview_item_row, docTitles);
        //Configure the list view
        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
    }


    @Override
    public void changed(Replication.ChangeEvent event) {
        Log.d("app", String.format("push/pull change event %s", event.toString()));

        List<String> docTitles = new ArrayList<>();
        Query query = database.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return;
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Document document = row.getDocument();
            String docDetails = String.format("%s-%s-%s", document.getId(), document.getProperty("title"), document.getProperty("sdk"));
            Log.d("app", String.format("docDetails %s", docDetails));
            docTitles.add(docDetails);
        }

        runOnUiThread(() -> {
            ListView list = findViewById(R.id.list);
            ArrayAdapter<String> adapter = (ArrayAdapter) list.getAdapter();
            adapter.clear();
            adapter.addAll(docTitles);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
