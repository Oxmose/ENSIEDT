package com.alexytorres.ensiedt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity {

    private EditText loginField;
    private EditText passwordField;
    private AppCompatButton loginButton;
    private Switch rememberMeSwitch;
    private ProgressDialog progressDialog;

    private String scheduleUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        passwordField = (EditText) findViewById(R.id.login_activity_password_input);
        passwordField.setTransformationMethod(new PasswordTransformationMethod());

        loginField = (EditText) findViewById(R.id.login_activity_login_input);

        loginButton = (AppCompatButton) findViewById(R.id.login_activity_login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        final SharedPreferencesManager manager = SharedPreferencesManager
                .getInstance(getApplicationContext());
        rememberMeSwitch = (Switch) findViewById(R.id.login_activity_autoconect_switch);
        rememberMeSwitch.setChecked(manager.getAutoconnect());
        rememberMeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                manager.setAutoconnect(compoundButton.isChecked());
                manager.commit();
            }
        });

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.auth));
        progressDialog.setCancelable(false);

        if(rememberMeSwitch.isChecked() &&
                !manager.getLogin().equals("") &&
                !manager.getPassword().equals("")) {

            progressDialog.show();
            loginField.setText(manager.getLogin());
            passwordField.setText(manager.getPassword());
            attemptLogin();
        }
    }

    private void attemptLogin() {
        loginButton.setEnabled(false);
        rememberMeSwitch.setEnabled(false);

        String login = loginField.getText().toString();
        String password = passwordField.getText().toString();

        int passwordLength = password.length();
        int loginLength = login.length();
        if(loginLength == 0 && passwordLength == 0) {
            onValidFailed(2);
        }
        else if(loginLength == 0) {
            onValidFailed(3);
        }
        else if(passwordLength == 0) {
            onValidFailed(4);
        }
        else if(8 > passwordLength || 24 < passwordLength) {
            onValidFailed(1);
        }
        else {
            progressDialog.show();
            new ASyncLoginProcedure().execute(login, password);
        }
    }

    private void onValidFailed(int error) {
        switch (error) {
            case 1:
                passwordField.setError(getString(R.string.error_password_format));
                break;
            case 2:
                passwordField.setError(getString(R.string.error_invalid_password));
                loginField.setError(getString(R.string.error_invalid_login));
                break;
            case 3:
                loginField.setError(getString(R.string.error_invalid_login));
                break;
            case 4:
                passwordField.setError(getString(R.string.error_invalid_password));
                break;
            default:
                passwordField.setError(getString(R.string.error_invalid_password));
                loginField.setError(getString(R.string.error_invalid_login));
                break;
        }

        loginButton.setEnabled(true);
        rememberMeSwitch.setEnabled(true);

        SharedPreferencesManager.getInstance(getApplicationContext()).reset();
    }

    private void onLoginSuccess() {
        SharedPreferencesManager manager =
                SharedPreferencesManager.getInstance(getApplicationContext());
        manager.setLogin(loginField.getText().toString());
        manager.setPassword(passwordField.getText().toString());
        manager.commit();

        Intent startSchedule = new Intent(LoginActivity.this, ScheduleActivity.class);
        startSchedule.putExtra("schedule_url", scheduleUrl);
        startSchedule.putExtra("login", loginField.getText().toString());
        startSchedule.putExtra("password", passwordField.getText().toString());
        startActivity(startSchedule);

        finish();
    }

    private class ASyncLoginProcedure extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... strings) {
            final String basicAuth = "Basic " +
                    Base64.encodeToString((strings[0]+":" +
                            strings[1]).getBytes(), Base64.NO_WRAP);

            try {
                // First log in
                String ZENITH_URL = "https://intranet.ensimag.fr/Zenith2";
                URL url = new URL(ZENITH_URL);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);


                int status = conn.getResponseCode();
                if (status < 200 || status > 299)
                    return status;

                // Get schedule URL
                String USERPAGE_URL = "https://intranet.ensimag.fr/Zenith2/Utilisateur/home?login=";
                url = new URL(USERPAGE_URL + strings[0]);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);

                status = conn.getResponseCode();
                if (status < 200 || status > 299)
                    return status;

                InputStream userPageHtmlStream = conn.getInputStream();
                String userPageHtmlString = IOUtils.toString(userPageHtmlStream, "UTF-8");

                // Parse HTML
                Pattern p = Pattern.compile("(https:\\/\\/edt.*)\"");
                Matcher m = p.matcher(userPageHtmlString);
                if(m.find()){
                    MatchResult mr = m.toMatchResult();
                    scheduleUrl = mr.group(1);
                }
                else {
                    return 3;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return 2;
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {

            loginButton.setEnabled(true);
            rememberMeSwitch.setEnabled(true);

            progressDialog.dismiss();

            SharedPreferencesManager manager =
                    SharedPreferencesManager.getInstance(getApplicationContext());

            if(result == 0)
            {
                onLoginSuccess();
                return;
            }
            else if(result == 401) {
                passwordField.setError(getString(R.string.error_invalid_password));
                loginField.setError(getString(R.string.error_invalid_login));
            }
            else if(result == 2) {
                Toast.makeText(getApplicationContext(),
                        R.string.exc_conn,
                        Toast.LENGTH_LONG).show();
            }
            else if(result == 3) {
                Toast.makeText(getApplicationContext(),
                        R.string.error_retrieving_schedule_url,
                        Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getApplicationContext(),
                        R.string.error_code + result,
                        Toast.LENGTH_LONG).show();
            }
            manager.reset();
        }
    }
}

