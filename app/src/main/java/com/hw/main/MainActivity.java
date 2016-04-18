package com.hw.main;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hw.blueservice.BTmain;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "HomeWatcher";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();

        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Logger.getAnonymousLogger().info("settings");
            Toast.makeText(this, "Settings selected", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


    public void io() {
        try {
            URL url = new URL("http://10.25.31.61:8080/HomeWatcher/ic");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            InputStream ins = con.getInputStream();
            Toast.makeText(this, "call ic success", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            ;
            Toast.makeText(this, "call ic fail", Toast.LENGTH_LONG).show();
        }
    }

    /* enable bluetooth*/
    public void onBTTestButtonClicked(View view) {

        Intent enabler = new Intent(this, BTmain.class);
        startActivity(enabler);
    }


}
