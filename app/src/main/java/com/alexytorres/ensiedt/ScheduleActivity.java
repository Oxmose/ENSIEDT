package com.alexytorres.ensiedt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Display;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

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

    final private String scheduleFrame = "https://edt.grenoble-inp.fr/2016-2017/ensimag/etudiant/" +
            "jsp/custom/modules/plannings/direct_planning.jsp?resources=";
    final private String scheduleImage = "https://edt.grenoble-inp.fr/2016-2017/ensimag/etudiant/" +
            "jsp/custom/modules/plannings/imagemap.jsp?clearTree=false&projectId=4&";
    final private String rootUrl = "https://edt.grenoble-inp.fr/";
    private String imgSrc;
    private SubsamplingScaleImageView scheduleImageView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        Intent usedIntent = getIntent();
        scheduleUrl   = usedIntent.getStringExtra("schedule_url");
        login    = usedIntent.getStringExtra("login");
        password = usedIntent.getStringExtra("password");
        scheduleImageView = (SubsamplingScaleImageView) findViewById(R.id.activity_schedule_image);

        new AsyncScheduleGetter().execute();
    }

    private void displayImage() {
        scheduleImageView.setImage(ImageSource.bitmap(bitmap));
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
                String resources = "";
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
                if (status != 302 && (status < 200 || status > 299))
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
                if (status != 302 && (status < 200 || status > 299))
                    return status;


                userPageHtmlStream = conn.getInputStream();
                userPageHtmlString = IOUtils.toString(userPageHtmlStream, "UTF-8");

                // Get image URL
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
            // TODO ERROR MANAGEMENT
            if(result == 0)
                displayImage();
        }
    }
}
