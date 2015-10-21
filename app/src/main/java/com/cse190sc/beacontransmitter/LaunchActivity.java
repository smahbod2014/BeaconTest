package com.cse190sc.beacontransmitter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
    }

    public void goToTransmitActivityClicked(View v) {
        Intent i = new Intent(this, TransmitActivity.class);
        this.startActivity(i);
    }

    public void goToScanActivityClicked(View v) {
        Intent i = new Intent(this, ScanActivity.class);
        this.startActivity(i);
    }
}
