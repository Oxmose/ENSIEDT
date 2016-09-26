package com.alexytorres.ensiedt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaderFactory;
import com.bumptech.glide.load.model.LazyHeaders;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.pkmmte.view.CircularImageView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class ScheduleActivity extends AppCompatActivity {

    private String scheduleUrl;
    private String login;
    private String password;
    private Button logoutButton;
    private Button precButton;
    private Button nextButton;
    private CircularImageView userImage;

    private int week;

    final private String scheduleFrame = "https://edt.grenoble-inp.fr/2016-2017/ensimag/etudiant/" +
            "jsp/custom/modules/plannings/direct_planning.jsp?resources=";
    final private String scheduleImage = "https://edt.grenoble-inp.fr/2016-2017/ensimag/etudiant/" +
            "jsp/custom/modules/plannings/imagemap.jsp?clearTree=false&projectId=4&";
    final private String rootUrl = "https://edt.grenoble-inp.fr/";
    private String imgSrc;
    private SubsamplingScaleImageView scheduleImageView;
    private Bitmap bitmap;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        Intent usedIntent = getIntent();
        scheduleUrl   = usedIntent.getStringExtra("schedule_url");
        login    = usedIntent.getStringExtra("login");
        password = usedIntent.getStringExtra("password");

        scheduleImageView = (SubsamplingScaleImageView) findViewById(R.id.activity_schedule_image);
        userImage = (CircularImageView) findViewById(R.id.activity_schedule_user_image);

        precButton = (Button) findViewById(R.id.activity_schedule_prec_button);
        precButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextButton.setEnabled(false);
                precButton.setEnabled(false);
                logoutButton.setEnabled(false);
                progressDialog.show();
                --week;
                new AsyncScheduleGetter().execute();
            }
        });

        nextButton = (Button) findViewById(R.id.activity_schedule_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextButton.setEnabled(false);
                precButton.setEnabled(false);
                logoutButton.setEnabled(false);
                progressDialog.show();
                ++week;
                new AsyncScheduleGetter().execute();
            }
        });

        logoutButton = (Button) findViewById(R.id.activity_schedule_logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferencesManager manager =
                        SharedPreferencesManager.getInstance(getApplicationContext());
                manager.setAutoconnect(false);
                manager.reset();
                Intent startLogin = new Intent(ScheduleActivity.this, LoginActivity.class);
                startActivity(startLogin);
                finish();
            }
        });

        progressDialog = new ProgressDialog(ScheduleActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        week = 0;

        nextButton.setEnabled(false);
        precButton.setEnabled(false);
        logoutButton.setEnabled(false);

        new AsyncScheduleGetter().execute();
    }

    private void displayImage() {
        scheduleImageView.setImage(ImageSource.bitmap(bitmap));
        scheduleImageView.invalidate();

        nextButton.setEnabled(true);
        precButton.setEnabled(true);
        logoutButton.setEnabled(true);
        progressDialog.dismiss();
    }

    private class AsyncScheduleGetter extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... args) {
            final String basicAuth = "Basic " +
                    Base64.encodeToString((login + ":" + password).getBytes(), Base64.NO_WRAP);

            try {
                // First log in
                URL url = new URL(scheduleUrl);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);

                int status = conn.getResponseCode();
                if (status < 200 || status > 299)
                    return status;

                String cookie = conn.getHeaderField("Set-Cookie");

                InputStream userPageHtmlStream = conn.getInputStream();
                String userPageHtmlString = IOUtils.toString(userPageHtmlStream, "UTF-8");

                // Get resources
                String resources;
                Pattern p = Pattern.compile("value=\"(.*)\">");
                Matcher m = p.matcher(userPageHtmlString);
                if(m.find()){
                    MatchResult mr = m.toMatchResult();
                    resources = mr.group(1);
                }
                else {
                    return 2;
                }


                url = new URL(scheduleFrame + resources);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);
                conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
                conn.setRequestProperty("Cookie", cookie);

                status = conn.getResponseCode();
                if (status < 200 || status > 299)
                    return status;

                // Get screen size
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;

                url = new URL(scheduleImage + "width=" + width + "&height= " + height);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);
                conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
                conn.setRequestProperty("Cookie", cookie);


                status = conn.getResponseCode();
                if (status < 200 || status > 299)
                    return status;

                userPageHtmlStream = conn.getInputStream();
                userPageHtmlString = IOUtils.toString(userPageHtmlStream, "UTF-8");

                // Get resources
                imgSrc = "";
                p = Pattern.compile("src=\"(.*image.*)\" w");
                m = p.matcher(userPageHtmlString);
                if(m.find()){
                    MatchResult mr = m.toMatchResult();
                    imgSrc = rootUrl + mr.group(1);
                }
                else {
                    return 2;
                }

                //Get week
                if(week == 0) {
                    p = Pattern.compile("PianoWeek=(.*)&idP");
                    m = p.matcher(imgSrc);
                    if(m.find()){
                        MatchResult mr = m.toMatchResult();
                        week = Integer.parseInt(mr.group(1));
                    }
                    else {
                        return 2;
                    }
                }
                else {
                    imgSrc = imgSrc.replaceAll("PianoWeek=[0-9]*", "PianoWeek=" + week);
                }

                url = new URL(imgSrc);
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.addRequestProperty("WWW-Authenticate", "Basic realm=\"Intranet Ensimag\"");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.addRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
                conn.setRequestProperty ("Authorization", basicAuth);
                conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
                conn.setRequestProperty("Cookie", cookie);

                status = conn.getResponseCode();
                if (status != 302 && (status < 200 || status > 299))
                    return status;

                userPageHtmlStream = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(userPageHtmlStream);
            } catch (IOException e) {
                e.printStackTrace();
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result != 0) {
                switch (result) {
                    case 1:
                        Toast.makeText(getApplicationContext(),
                                R.string.exception,
                                Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(),
                                R.string.error_retrieving_schedule_url,
                                Toast.LENGTH_LONG).show();
                        break;
                    case 302:
                        Toast.makeText(getApplicationContext(),
                                R.string.redirected,
                                Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                R.string.error_code + result,
                                Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }
            else
                displayImage();
        }
    }
}
