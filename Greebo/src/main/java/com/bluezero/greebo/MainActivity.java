package com.bluezero.greebo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {
    private BluetoothAdapter _bluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setTitle("Greebo");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        _bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (_bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!_bluetoothAdapter.isEnabled()) {
            if (!_bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:

                //Test();

                //scanLeDevice(true);
                Toast.makeText(MainActivity.this, "Scan started", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, BluetoothLeScanService.class);
                startService(intent);

                break;

            case R.id.menu_stop:
                //scanLeDevice(false);
                //Toast.makeText(MainActivity.this, "Scan stopped", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    public class BluetoothLeScanResultReceiver extends BroadcastReceiver {
        public static final String ACTION = "BluetoothLeScanResult";

        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceName = intent.getStringExtra(BluetoothLeScanService.PARAM_DEVICE_NAME);
            String address = intent.getStringExtra(BluetoothLeScanService.PARAM_DEVICE_NAME);

            Toast.makeText(MainActivity.this, "Found: " + deviceName + " (" + address + ")", Toast.LENGTH_SHORT).show();
        }
    }
}
