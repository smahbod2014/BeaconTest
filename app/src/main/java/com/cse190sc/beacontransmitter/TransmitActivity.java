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
        builder.setId2("JOJO");
        builder.setId3("DIO");
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



}
