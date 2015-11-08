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
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;

public class BeaconTransmitterApplication extends Application implements BootstrapNotifier {

    private static final String TAG = "BeaconTransmitterApp";
    private static final String ALTBEACON_ID = "2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6";
    private RegionBootstrap m_RegionBootstrap;
    private BackgroundPowerSaver m_PowerSaver;
    private BeaconManager m_BeaconManager;
    private boolean m_InsideActivity;

    //transmitter stuff
    private BeaconTransmitter m_Transmitter;

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
                    Beacon beacon = collection.iterator().next();
                    Log.i(TAG, "Beacon with id2 = " + beacon.getId2() + " is " + beacon.getDistance() + " meters away");
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

        //transmitter setup
        BeaconParser parser = new AltBeaconParser();
        //BeaconParser parser = new BeaconParser()
        // setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        m_Transmitter = new BeaconTransmitter(this, parser);
        m_Transmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        m_Transmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);


        AltBeacon.Builder builder = new AltBeacon.Builder();
        builder.setId1("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");
        builder.setId2("JOJO");
        builder.setId3("DIO");
        //builder.setMfgReserved(3);
        builder.setManufacturer(0);
        builder.setTxPower(-59);
        m_Transmitter.setBeacon(builder.build());

        startTransmitting();
    }



    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "I see a beacon!");
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

    public void startTransmitting() {
        if (!m_Transmitter.isStarted()) {
            m_Transmitter.startAdvertising();
            Toast.makeText(this, "Started transmitting", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTransmitting() {
        if (m_Transmitter.isStarted()) {
            m_Transmitter.stopAdvertising();
            Toast.makeText(this, "Stopped transmitting", Toast.LENGTH_SHORT).show();
        }
    }

}
