package com.logickllc.pokemapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.logickllc.pokemapper.api.Features;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;

public class AndroidFeatures extends Features {
    private Activity act;
    public static final String PREF_TOKEN = "Token";
    public static final String PREF_USERNAME = "ProfileName";
    public static final String PREF_PASSWORD = "Nickname";
    private ProgressDialog progressDialog;

    public AndroidFeatures(Activity act) {
        this.act = act;
    }

    public synchronized void lockLogin() {
        if (!loginLocked()) loggingIn = true;
    }

    public synchronized void unlockLogin() {
        loggingIn = false;
    }

    public synchronized boolean loginLocked() {
        return loggingIn;
    }

    public void login() {
        if (!loginLocked()) lockLogin();
        else return;
        Thread loginThread = new Thread() {
            public void run() {
                Log.d(TAG, "Attempting to login...");
                try {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                    token = preferences.getString(PREF_TOKEN, "");
                    /*if (token != "") {
                        final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                        boolean trying = true;
                        int failCount = 0;
                        final int MAX_TRIES = 3;
                        while (trying) {
                            try {
                                Log.d(TAG, "Attempting to login with token: " + token);

                                OkHttpClient httpClient = new OkHttpClient();
                                go = new PokemonGo(auth, httpClient);
                                tryTalkingToServer(); // This will error if we can't reach the server
                                shortMessage(R.string.loginSuccessfulMessage);
                                unlockLogin();
                                progressDialog.dismiss();
                                return;
                            } catch (Exception e) {
                                if (++failCount < MAX_TRIES) {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    e.printStackTrace();
                                    token = "";
                                    Log.d(TAG, "Erasing token because it seems to be expired.");
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(PREF_TOKEN, token);
                                    editor.commit();
                                    //longMessage(R.string.loginFailedMessage);
                                    unlockLogin();
                                    progressDialog.dismiss();
                                    login();
                                    return;
                                }
                            }
                        }
                    } else {*/
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            String pastUsername = preferences.getString(PREF_USERNAME, "");
                            String pastPassword = preferences.getString(PREF_PASSWORD, "");

                            if (!pastUsername.equals("") && !pastPassword.equals("")) {
                                final String username = decode(pastUsername);
                                final String password = decode(pastPassword);

                                if (username.equals("") || password.equals("")) {
                                    // Erase username and pass and prompt for login again
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(PREF_USERNAME, "");
                                    editor.putString(PREF_PASSWORD, "");
                                    editor.commit();
                                    unlockLogin();
                                    login();
                                    return;
                                }

                                Thread thread = new Thread() {
                                    @Override
                                    public void run() {
                                        final ProgressDialog tryingDialog = (ProgressDialog) showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                        boolean trying = true;
                                        int failCount = 0;
                                        final int MAX_TRIES = 10;
                                        while (trying) {
                                            OkHttpClient httpClient = new OkHttpClient();
                                            //RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
                                            try {
                                                //Log.d(TAG, "Attempting to login with Username: " + username + " and password: " + password);

                                                PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username, password);
                                                go = new PokemonGo(provider, httpClient);
                                                shortMessage(R.string.loginSuccessfulMessage);
                                                token = provider.getTokenId();
                                                //Log.d(TAG, "Token: " + token);
                                                SharedPreferences.Editor editor = preferences.edit();
                                                editor.putString(PREF_TOKEN, token);
                                                editor.commit();
                                                unlockLogin();
                                                progressDialog.dismiss();
                                                return;
                                            } catch (Exception e) {
                                                if (++failCount < MAX_TRIES) {
                                                    try {
                                                        Thread.sleep(3000);
                                                    } catch (InterruptedException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                } else {
                                                    e.printStackTrace();
                                                    longMessage(R.string.loginFailedMessage);
                                                    unlockLogin();
                                                    progressDialog.dismiss();
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                };
                                thread.start();
                            } else {

                                // Show login screen
                                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                                builder.setTitle(R.string.loginTitle);
                                //builder.setMessage(R.string.loginMessage);
                                View view = act.getLayoutInflater().inflate(R.layout.login, null);
                                builder.setView(view);

                                final EditText username = (EditText) view.findViewById(R.id.username);
                                final EditText password = (EditText) view.findViewById(R.id.password);
                                final CheckBox rememberLogin = (CheckBox) view.findViewById(R.id.rememberLogin);
                                final TextView createAccount = (TextView) view.findViewById(R.id.createAccountLink);
                                createAccount.setText(act.getResources().getText(R.string.createAccountMessage));
                                createAccount.setMovementMethod(LinkMovementMethod.getInstance());

                                builder.setPositiveButton(R.string.loginButton, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Thread thread = new Thread() {
                                            @Override
                                            public void run() {
                                                if (rememberLogin.isChecked()) {
                                                    // Boss gave us permission to store the credentials
                                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(act);
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    editor.putString(PREF_USERNAME, encode(username.getText().toString()));
                                                    editor.putString(PREF_PASSWORD, encode(password.getText().toString()));
                                                    editor.commit();
                                                }
                                                final ProgressDialog tryingDialog = (ProgressDialog) showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                                                boolean trying = true;
                                                int failCount = 0;
                                                final int MAX_TRIES = 10;
                                                while (trying) {
                                                    OkHttpClient httpClient = new OkHttpClient();
                                                    try {
                                                        //Log.d(TAG, "Attempting to login with Username: " + username.getText().toString() + " and password: " + password.getText().toString());

                                                        PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username.getText().toString(), password.getText().toString());
                                                        go = new PokemonGo(provider, httpClient);
                                                        shortMessage(R.string.loginSuccessfulMessage);
                                                        token = provider.getTokenId();
                                                        //Log.d(TAG, "Token: " + token);
                                                        SharedPreferences.Editor editor = preferences.edit();
                                                        editor.putString(PREF_TOKEN, token);
                                                        editor.commit();
                                                        unlockLogin();
                                                        progressDialog.dismiss();
                                                        return;
                                                    } catch (Exception e) {
                                                        if (++failCount < MAX_TRIES) {
                                                            try {
                                                                Thread.sleep(3000);
                                                            } catch (InterruptedException e1) {
                                                                e1.printStackTrace();
                                                            }
                                                        } else {
                                                            e.printStackTrace();
                                                            longMessage(R.string.loginFailedMessage);
                                                            unlockLogin();
                                                            progressDialog.dismiss();
                                                            return;
                                                        }
                                                    }
                                                }
                                            }
                                        };
                                        thread.start();
                                    }
                                });
                                builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        unlockLogin();
                                    }
                                });

                                builder.create().show();
                            }
                        }
                    };
                    runOnMainThread(runnable);


                } catch (Exception e) {
                    Log.d(TAG, "Login failed...");
                    e.printStackTrace();
                    unlockLogin();
                }
            }
        };
        loginThread.start();
    }

    public void logout() {
        final Context con = act;

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(R.string.logoutTitle);
        builder.setMessage(R.string.logoutMessage);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Erase login creds so we can try again
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_TOKEN, "");
                editor.putString(PREF_USERNAME, "");
                editor.putString(PREF_PASSWORD, "");
                editor.apply();

                login();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        builder.create().show();
    }

    @Override
    public void runOnMainThread(Runnable r) {
        act.runOnUiThread(r);
    }

    public Object showProgressDialog(int titleid, int messageid) {
        return showProgressDialog(act.getResources().getString(titleid), act.getResources().getString(messageid));
    }

    public Object showProgressDialog(final String title, final String message) {
        final Context con = act;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(con);
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }
        };
        runOnMainThread(runnable);
        return progressDialog;
    }

    public void shortMessage(int resid) {
        shortMessage(act.getResources().getString(resid));
    }

    public void shortMessage(final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
            }
        };
        runOnMainThread(r);
    }

    public void longMessage(int resid) {
        longMessage(act.getResources().getString(resid));
    }

    public void longMessage(final String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, message, Toast.LENGTH_LONG).show();
            }
        };
        runOnMainThread(r);
    }
}
