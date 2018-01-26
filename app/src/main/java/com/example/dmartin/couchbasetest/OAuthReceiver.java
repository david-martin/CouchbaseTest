package com.example.dmartin.couchbasetest;

/**
 * Created by dmartin on 26/01/18.
 */

public interface OAuthReceiver {

    public static final String DISMISS_ERROR = "dialog_dismissed";

    void receiveLoginAttempted(String redirectURL);

    void receiveOAuthCode(String code);

    public void receiveOAuthError(String error);
}