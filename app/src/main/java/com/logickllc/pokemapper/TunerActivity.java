package com.logickllc.pokemapper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SeekBar;
import android.widget.TextView;

public class TunerActivity extends AppCompatActivity {
    final String PREF_SCAN_DISTANCE = "ScanDistance";
    final String PREF_SCAN_TIME = "ScanTime";

    final int DEFAULT_SCAN_DISTANCE = 100;
    final int DEFAULT_SCAN_TIME = 30;

    final int DISTANCE_STEP = 10;
    final int TIME_STEP = 5;

    int scanDistance, scanTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuner);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        scanDistance = prefs.getInt(PREF_SCAN_DISTANCE, DEFAULT_SCAN_DISTANCE);
        scanTime = prefs.getInt(PREF_SCAN_TIME, DEFAULT_SCAN_TIME);

        final TextView distance = (TextView) findViewById(R.id.distance);
        final TextView time = (TextView) findViewById(R.id.time);
        SeekBar seekDistance = (SeekBar) findViewById(R.id.seekbarDistance);
        SeekBar seekTime = (SeekBar) findViewById(R.id.seekbarTime);
        final TextView scanSpeed = (TextView) findViewById(R.id.scanSpeed);

        seekDistance.setProgress(scanDistance / DISTANCE_STEP);
        seekTime.setProgress(scanTime / TIME_STEP);
        distance.setText(scanDistance + "m");
        time.setText(scanTime + "s");

        int scanSpeedMeters = Math.round((float) scanDistance / (scanTime / (float) (PokeFinderActivity.NUM_SCAN_SECTORS - 1)) * 3.6f);
        int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

        scanSpeed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");

        seekDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress * DISTANCE_STEP < 10) {
                    seekBar.setProgress(10 / DISTANCE_STEP);
                    return;
                }
                scanDistance = progress * DISTANCE_STEP;
                distance.setText(scanDistance + "m");

                int scanSpeedMeters = Math.round((float) scanDistance / (scanTime / (float) (PokeFinderActivity.NUM_SCAN_SECTORS - 1)) * 3.6f);
                int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

                scanSpeed.setText(scanSpeedMeters + " kph (" + scanSpeedMiles + " mph)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress * TIME_STEP < 10) {
                    seekBar.setProgress(10 / TIME_STEP);
                    return;
                }
                scanTime = progress * TIME_STEP;
                time.setText(scanTime + "s");

                int scanSpeedMeters = Math.round((float) scanDistance / (scanTime / (float) (PokeFinderActivity.NUM_SCAN_SECTORS - 1)) * 3.6f);
                int scanSpeedMiles = Math.round(scanSpeedMeters * 0.621371f);

                scanSpeed.setText(scanSpeedMeters + " km/h (" + scanSpeedMiles + " mph)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.empty_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SCAN_DISTANCE, scanDistance);
        editor.putInt(PREF_SCAN_TIME, scanTime);
        editor.commit();

        super.onPause();
    }
}
