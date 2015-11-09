package com.cse190sc.beacontransmitter;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class BeaconTransmitterApplication extends Application implements BootstrapNotifier {

    private static final String TAG = "BeaconTransmitterApp";
    private static final String ALTBEACON_ID = "2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6";
    private static final long TIMEOUT = 10l * 1000l;
    private RegionBootstrap m_RegionBootstrap;
    private BackgroundPowerSaver m_PowerSaver;
    private BeaconManager m_BeaconManager;
    private boolean m_InsideActivity;
    private LaunchActivity m_LaunchActivity;
    private final HashMap<Identifier, Long> m_BeaconMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BeaconTransmitterApplication started up!");
        //this should add an AltBeacon parser automatically
        m_BeaconManager = BeaconManager.getInstanceForApplication(this);
        m_BeaconManager.setBackgroundBetweenScanPeriod(0l);
        m_BeaconManager.setBackgroundScanPeriod(1100l);
        m_BeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                if (collection.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (Beacon beacon : collection) {
                        synchronized (m_BeaconMap) {
                            m_BeaconMap.put(beacon.getId2(), System.currentTimeMillis());
                        }
                        sb.append("Beacon with id2 = " + beacon.getId2() + " is " + beacon.getDistance() + " meters away");
                        sb.append("\n");
                    }
                    logToDisplay(sb.toString());
                }
            }
        });


        //wake up the app when any beacon is seen
        //startBackgroundMonitoring();
        //m_PowerSaver = new BackgroundPowerSaver(this);
        Log.d(TAG, "Begin monitoring");
        Region region = new Region("com.cse190sc.BeaconTransmitter", Identifier.parse(ALTBEACON_ID), null, null);
        m_RegionBootstrap = new RegionBootstrap(this, region);
        m_PowerSaver = new BackgroundPowerSaver(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (m_BeaconMap) {
                        ArrayList<Identifier> idsToRemove = new ArrayList<>();
                        for (Identifier id : m_BeaconMap.keySet()) {
                            Long time = m_BeaconMap.get(id);
                            if (System.currentTimeMillis() - time >= TIMEOUT) {
                                idsToRemove.add(id);
                            }
                        }

                        for (int i = 0; i < idsToRemove.size(); i++) {
                            m_BeaconMap.remove(idsToRemove.get(i));
                        }
                    }

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "I see a beacon!");
        logToDisplay("I see a beacon!");
        try {
            m_BeaconManager.startRangingBeaconsInRegion(region);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        //createNotification();
        if (m_InsideActivity) {
            Log.d(TAG, "No need to send a notification, app is in foreground");
        }
        else {
            Log.d(TAG, "Sending notification!");
            createNotification();
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "Went out of range of beacon with Id2 = " + region.getId2());
        logToDisplay("Went outside range");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        //don't care?
        if (i == BootstrapNotifier.INSIDE) {
            Log.d(TAG, "Went INSIDE a beacon range");
        }
        else if (i == BootstrapNotifier.OUTSIDE) {
            Log.d(TAG, "Went OUTSIDE a beacon range");
        }
    }

    public void setInsideActivity(boolean insideActivity) {
        m_InsideActivity = insideActivity;
        Log.d(TAG, "m_InsideActivity is now " + m_InsideActivity);
    }

    public void startBackgroundMonitoring() {

    }

    public void stopBackgroundMonitoring() {
    }

    /**
     * This will create an Android notification that will appear at the top of the phone.
     * It's also configured so that when the user opens and clicks on the notification,
     * the LaunchActivity activity will be launched.
     */
    private void createNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Transmitter Application")
                        .setContentText("Congratulations! Found a beacon while not in-app!")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setAutoCancel(true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        Intent i = new Intent(this, LaunchActivity.class);
        i.putExtra("arrivedFromNotification", true);
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setLaunchActivity(LaunchActivity activity) {
        m_LaunchActivity = activity;
        Log.d(TAG, "Launch activity has been set!");
        logToDisplay("Launch activity set");
    }

    public void logToDisplay(String s) {
        if (m_LaunchActivity != null)
            m_LaunchActivity.logToDisplay(s);
    }
}
