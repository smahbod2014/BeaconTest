package com.cse190sc.beacontransmitter;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;

public class BeaconTransmitterApplication extends Application implements BootstrapNotifier {

    private static final String TAG = "BeaconTransmitterApp";
    private RegionBootstrap m_RegionBootstrap;
    private BackgroundPowerSaver m_PowerSaver;
    private BeaconManager m_BeaconManager;
    private boolean m_InsideActivity;
    private boolean m_AlreadySeenABeacon;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BeaconTransmitterApplication started up!");
        //this should add an AltBeacon parser automatically
        m_BeaconManager = BeaconManager.getInstanceForApplication(this);

        m_BeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                if (collection.size() > 0) {
                    Beacon beacon = collection.iterator().next();
                    Log.d(TAG, "Completed analysis on the discovered beacon. It is " +
                        beacon.getDistance() + " meters away and has an ID of " + beacon.getId2());
                }

                try {
                    Log.i(TAG, "Attempting to stop ranging (collection size = " + collection.size() + ")");
                    m_BeaconManager.stopRangingBeaconsInRegion(region);
                }
                catch (RemoteException e) {
                    Log.e(TAG, "Unable to stop ranging: " + e.getMessage());
                }
            }
        });

        //wake up the app when any beacon is seen
        //startBackgroundMonitoring();
        m_PowerSaver = new BackgroundPowerSaver(this);
        m_AlreadySeenABeacon = false;
    }

    @Override
    public void didEnterRegion(Region region) {
        if (!m_AlreadySeenABeacon) {
            Log.d(TAG, "Got in range of a beacon...");
            m_AlreadySeenABeacon = true;

            try {
                //get more information on this beacon
                m_BeaconManager.startRangingBeaconsInRegion(region);
            }
            catch (RemoteException e) {
                Log.e(TAG, "Unable to start ranging: " + e.getMessage());
            }

            //if the user is not in this app, send a notification
            if (!m_InsideActivity) {
                createNotification();
            }

            //this call will make it so the notification only appears once until the app is relaunched
            //however, there doesn't seem to be an enable() function, so I would try and avoid this
            //function if possible
            //m_RegionBootstrap.disable();
        }
        else {
            Log.v(TAG, "In range of beacon, but we've already seen one");
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "Went out of range of beacon " + region.getUniqueId());
        m_AlreadySeenABeacon = false;
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        //don't care?
    }

    public void setInsideActivity(boolean insideActivity) {
        m_InsideActivity = insideActivity;
    }

    public void startBackgroundMonitoring() {
        if (m_RegionBootstrap == null) {
            Region region = new Region("com.cse190sc.BeaconTransmitter", null, null, null);
            m_RegionBootstrap = new RegionBootstrap(this, region);
            m_AlreadySeenABeacon = false;
        }
    }

    public void stopBackgroundMonitoring() {
        if (m_RegionBootstrap != null) {
            m_RegionBootstrap.disable();
            m_RegionBootstrap = null;
            m_AlreadySeenABeacon = false;
        }
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
}
