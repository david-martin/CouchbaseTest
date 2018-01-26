package com.example.dmartin.couchbasetest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String SYNC_GATEWAY_HOST = "http://192.168.1.5:4984";
    private static final String COUCHBASE_DB_NAME = "db";
    private static final String SYNC_GATEWAY_URL = SYNC_GATEWAY_HOST + "/" + COUCHBASE_DB_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        // Create a manager
        Manager manager = null;
        try {
            manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TokenStore store = TokenStoreFactory.build(manager.getContext());

        Authenticator authenticator = OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(new OIDCLoginCallback() {
            @Override
            public void callback(URL loginURL, final URL redirectURL, final OIDCLoginContinuation loginContinuation) {
                // Open the webview in a new activity

                final CouchbaseOAuthWebViewDialog dialog = CouchbaseOAuthWebViewDialog.newInstance(loginURL, redirectURL);
                dialog.setReceiver(new OAuthReceiver() {
                    @Override
                    public void receiveLoginAttempted(String url) {
                        Log.d("app", String.format("login redirect url %s original redirect url %s", url, redirectURL));
                        dialog.removeReceive();
                        dialog.dismiss();
                        loginContinuation.callback(redirectURL, null);
                    }

                    @Override
                    public void receiveOAuthError(String error) {
                        Log.d("app", String.format("unexpected receiveOAuthError %s", error));
                    }

                    @Override
                    public void receiveOAuthCode(String error) {
                        Log.d("app", String.format("unexpected receiveOAuthCode %s", error));
                    }

//                    @Override
//                    public void receiveOAuthError(final String error) {
//                        dialog.removeReceive();
//                        dialog.dismiss();
//                        loginContinuation.callback(null, new IOException(error));
//                    }
                });

            }
        }, store);

        // Create or open the database named app
        Database database = null;
        try {
            database = manager.getDatabase("app");
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        // The properties that will be saved on the document
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

        // Save the document to the database
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        // Log the document ID (generated by the database)
        // and properties
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

        //.createBasicAuthenticator("adminaccount", "password");
        pull.setAuthenticator(authenticator);
        push.setAuthenticator(authenticator);

        push.setContinuous(true);
        pull.setContinuous(true);

        push.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Log.d("app", String.format("push change event %s", event.toString()));
            }
        });

        pull.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Log.d("app", String.format("pull change event %s", event.toString()));
            }
        });

        // Start replicators
        push.start();
        pull.start();

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
