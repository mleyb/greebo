package com.bluezero.greebo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableOperationCallback;

public class MainActivity extends Activity {
    private BluetoothAdapter _bluetoothAdapter;
    private boolean _scanning;
    private Handler _handler;

    private MobileServiceClient _client;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD_MS = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setTitle("Greebo");

        _handler = new Handler();

        _client = MobileServiceClientFactory.createAzureClient(this, null);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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

        //if (!_scanning) {
            //scanLeDevice(true);
        //}
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (_scanning) {
            scanLeDevice(false);
            Toast.makeText(MainActivity.this, "Scan stopped", Toast.LENGTH_SHORT).show();
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void Test() {
        Contact contact = new Contact();
        contact.DeviceName = "Test";
        contact.DeviceAddress = "Test";

        MobileServiceClient client = MobileServiceClientFactory.createAzureClient(MainActivity.this, null);

        client.getTable(Contact.class).insert(contact, new TableOperationCallback<Contact>() {
            public void onCompleted(Contact entity, Exception exception, ServiceFilterResponse response) {
                if (exception == null) {
                    Toast.makeText(MainActivity.this, "Inserted OK!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Insert FAILED!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:

                Test();

                scanLeDevice(true);
                Toast.makeText(MainActivity.this, "Scan started", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                Toast.makeText(MainActivity.this, "Scan stopped", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _scanning = false;
                    _bluetoothAdapter.stopLeScan(_leScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD_MS);

            _scanning = true;
            _bluetoothAdapter.startLeScan(_leScanCallback);
        }
        else {
            _scanning = false;
            _bluetoothAdapter.stopLeScan(_leScanCallback);
        }

        //invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback _leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String deviceName = device.getName();
                    String address = device.getAddress();

                    Toast.makeText(MainActivity.this, "Found: " + deviceName + " (" + address + ")", Toast.LENGTH_SHORT).show();

                    Contact contact = new Contact();
                    contact.DeviceName = deviceName;
                    contact.DeviceAddress = address;

                    MobileServiceClient client = MobileServiceClientFactory.createAzureClient(MainActivity.this, null);

                    client.getTable(Contact.class).insert(contact, new TableOperationCallback<Contact>() {
                        public void onCompleted(Contact entity, Exception exception, ServiceFilterResponse response) {
                            if (exception == null) {
                                Toast.makeText(MainActivity.this, "Inserted OK!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Insert FAILED!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }
    };
}
