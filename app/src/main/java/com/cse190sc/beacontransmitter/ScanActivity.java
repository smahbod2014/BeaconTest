package com.cse190sc.beacontransmitter;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScanActivity extends AppCompatActivity implements BeaconConsumer {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String TAG = "ScanActivity";

    private BeaconManager m_Manager;
    private TextView m_Log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        m_Manager = BeaconManager.getInstanceForApplication(this);
        m_Log = (TextView) findViewById(R.id.scan_log);

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

    public void scanClicked(View v) {
        if (!m_Manager.isBound(this)) {
            m_Manager.bind(this);
            logMessage("Scanning started...");
        }
    }

    public void stopClicked(View v) {
        if (m_Manager.isBound(this)) {
            m_Manager.unbind(this);
            logMessage("Scanning stopped.");
        }
    }

    public void clearClicked(View v) {
        m_Log.setText("");
    }

    @Override
    public void onBeaconServiceConnect() {
        m_Manager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon beacon = beacons.iterator().next();
                    //logMessage("First beacon found (" + beacon.toString() + ") is "
                    //        + beacon.getDistance() + " meters away!");
                    logMessage("Found beacon (" + beacon.getDistance() + " meters away)");

                    String data = "";
                    List<Long> fields = beacon.getDataFields();
                    for (int i = 0; i < fields.size(); i++) {
                        data += fields.get(i).toString();
                        if (i < fields.size() - 1) {
                            data += " ";
                        }
                    }

                    logMessage("Its data (" + fields.size() + "): " + data);
                }
            }
        });

        try {
            m_Manager.startRangingBeaconsInRegion(new Region("StreetClashUniqueID", null, null, null));
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void logMessage(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_Log.append(s + "\n");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (m_Manager.isBound(this)) {
            m_Manager.unbind(this);
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
}
