package com.cse190sc.beacontransmitter;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

public class LaunchActivity extends AppCompatActivity {

    private static final String TAG = "LaunchActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BeaconTransmitterApplication m_Application;
    private BeaconTransmitter m_Transmitter;
    private Switch transSwitch;
    private SharedPreferences m_Prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        m_Application = (BeaconTransmitterApplication) this.getApplicationContext();
        m_Prefs = this.getSharedPreferences("com.cse190sc.beacontransmitter", Context.MODE_PRIVATE);

        boolean b = this.getIntent().getBooleanExtra("arrivedFromNotification", false);
        if (b) {
            Toast.makeText(this, "Arrived from notification", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Came from notification");
        }
        else {
            Toast.makeText(this, "Opened app manually", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Opened app manually");
        }

        transSwitch = (Switch) findViewById(R.id.launch_trans_switch);
        Switch scanSwitch = (Switch) findViewById(R.id.launch_scan_switch);

        //transSwitch.setChecked(m_Prefs.getBoolean("transSwitch", false));
        //scanSwitch.setChecked(m_Prefs.getBoolean("scanSwitch", false));

        transSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    BeaconParser parser = new AltBeaconParser();
                    //BeaconParser parser = new BeaconParser()
                    // setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
                    m_Transmitter = new BeaconTransmitter(LaunchActivity.this, parser);
                    m_Transmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
                    m_Transmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);


                    AltBeacon.Builder builder = new AltBeacon.Builder();
                    builder.setId1("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");
                    builder.setId2("1");
                    builder.setId3("2");
                    //builder.setMfgReserved(3);
                    builder.setManufacturer(0);
                    builder.setTxPower(-59);
                    m_Transmitter.setBeacon(builder.build());
                    m_Transmitter.startAdvertising();
                }
                else {
                    m_Transmitter.stopAdvertising();
                }

                //m_Prefs.edit().putBoolean("transSwitch", isChecked).commit();
            }
        });

        scanSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    doPermissionChecks();
                    m_Application.startBackgroundMonitoring();
                    Toast.makeText(LaunchActivity.this, "Scanning on...", Toast.LENGTH_SHORT).show();
                }
                else {
                    m_Application.stopBackgroundMonitoring();
                    Toast.makeText(LaunchActivity.this, "Scanning off...", Toast.LENGTH_SHORT).show();
                }

                //m_Prefs.edit().putBoolean("scanSwitch", isChecked).apply();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        m_Application.setInsideActivity(false);
        if (m_Transmitter != null && m_Transmitter.isStarted())
            m_Transmitter.stopAdvertising();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        m_Application.setInsideActivity(true);
        if (m_Transmitter != null && transSwitch.isChecked() && !m_Transmitter.isStarted()) {
            m_Transmitter.startAdvertising();
        }
    }

    private void doPermissionChecks() {
        //this huge block of code is to let this work on phones running
        //Android 6.0, like my Nexus 5
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Location permission granted!");
            }
            else {
                //inform the user here that they will not be able to use the app properly
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "LaunchActivity destroyed");
    }
}
