package com.cse190sc.beacontransmitter;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.ArrayList;
import java.util.List;

public class TransmitActivity extends AppCompatActivity {

    private BeaconTransmitter m_Transmitter;
    private TextView m_Log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmit);

        BeaconParser parser = new AltBeaconParser();
        //BeaconParser parser = new BeaconParser()
               // setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        m_Transmitter = new BeaconTransmitter(this, parser);
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

        m_Log = (TextView) findViewById(R.id.transmit_log);
    }

    public void transmitClicked(View v) {
        startTransmitting();
    }

    public void stopClicked(View v) {
        stopTransmitting();
    }

    public void clearClicked(View v) {
        m_Log.setText("");
    }

    public void startTransmitting() {
        if (!m_Transmitter.isStarted()) {
            m_Transmitter.startAdvertising();
            Toast.makeText(this, "Started transmitting", Toast.LENGTH_SHORT).show();
            logMessage("Started transmitting...");
        }
        else {
            logMessage("Already transmitting!");
        }
    }

    public void stopTransmitting() {
        if (m_Transmitter.isStarted()) {
            m_Transmitter.stopAdvertising();
            Toast.makeText(this, "Stopped transmitting", Toast.LENGTH_SHORT).show();
            logMessage("Stopped transmitting");
        }
        else {
            logMessage("Not transmitting!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (m_Transmitter.isStarted())
            m_Transmitter.stopAdvertising();
    }

    private void logMessage(final String s) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_Log.append(s + "\n");
            }
        });
    }


    //simply checks whether your device can transmit as a beacon
    @TargetApi(21)
    private boolean checkPrerequisites() {

        if (android.os.Build.VERSION.SDK_INT < 18) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not supported by this device's operating system");
            builder.setMessage("You will not be able to transmit as a Beacon");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }

            });
            builder.show();
            return false;
        }
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not supported by this device");
            builder.setMessage("You will not be able to transmit as a Beacon");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }

            });
            builder.show();
            return false;
        }
        if (!((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth not enabled");
            builder.setMessage("Please enable Bluetooth and restart this app.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }

            });
            builder.show();
            return false;

        }

        try {
            // Check to see if the getBluetoothLeAdvertiser is available.  If not, this will throw an exception indicating we are not running Android L
            ((BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeAdvertiser();
        }
        catch (Exception e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE advertising unavailable");
            builder.setMessage("Sorry, the operating system on this device does not support Bluetooth LE advertising.  As of July 2014, only the Android L preview OS supports this feature in user-installed apps.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }

            });
            builder.show();
            return false;

        }

        return true;
    }
}
