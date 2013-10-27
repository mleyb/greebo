package com.bluezero.greebo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ListActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private LeDeviceListAdapter _listAdapter;

    private BluetoothAdapter _bluetoothAdapter;

    private AlarmReceiver _alarmReceiver;
    private BluetoothLeScanResultReceiver _scanReceiver;

    private AlarmManager _am;

    private PendingIntent _scanIntent;

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

        _scanIntent = PendingIntent.getBroadcast(this, 0, new Intent(AlarmReceiver.ACTION), 0);

        _am = (AlarmManager)(this.getSystemService(Context.ALARM_SERVICE));

        _alarmReceiver = new AlarmReceiver();
        registerReceiver(_alarmReceiver, new IntentFilter(AlarmReceiver.ACTION));

        IntentFilter filter = new IntentFilter(BluetoothLeScanResultReceiver.ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        _scanReceiver = new BluetoothLeScanResultReceiver();
        registerReceiver(_scanReceiver, filter);

        _listAdapter = new LeDeviceListAdapter();
        setListAdapter(_listAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //scanLeDevice(false);
        _listAdapter.clear();
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
    protected void onDestroy() {
        descheduleScan();
        unregisterReceiver(_alarmReceiver);
        unregisterReceiver(_scanReceiver);
        super.onDestroy();
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
                scheduleScan();
                Toast.makeText(this, "Scan scheduled", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Scan scheduled");
                break;

            case R.id.menu_stop:
                descheduleScan();
                Toast.makeText(this, "Scan descheduled", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Scan desceduled");
                break;
        }

        return true;
    }

    private void scheduleScan() {
        _am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 20000, _scanIntent);
    }

    private void descheduleScan() {
        _am.cancel(_scanIntent);
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<Contact> _devices;
        private LayoutInflater _inflator;

        public LeDeviceListAdapter() {
            super();
            _devices = new ArrayList<Contact>();
            _inflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(Contact device) {
            if (!_devices.contains(device)) {
                _devices.add(device);
            }
        }

        public Contact getDevice(int position) {
            return _devices.get(position);
        }

        public void clear() {
            _devices.clear();
        }

        @Override
        public int getCount() {
            return _devices.size();
        }

        @Override
        public Object getItem(int i) {
            return _devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            // General ListView optimization code.
            if (view == null) {
                view = _inflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView)view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder)view.getTag();
            }

            Contact device = _devices.get(i);
            final String deviceName = device.DeviceName;

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            }
            else {
                viewHolder.deviceName.setText("Unknown device");
            }

            viewHolder.deviceAddress.setText(device.DeviceAddress);

            return view;
        }
    }

    public class BluetoothLeScanResultReceiver extends BroadcastReceiver {
        public static final String ACTION = "BluetoothLeScanResult";

        @Override
        public void onReceive(Context context, Intent intent) {
            final Contact contact = new Contact();
            contact.DeviceName = intent.getStringExtra(BluetoothLeScanService.PARAM_DEVICE_NAME);
            contact.DeviceAddress = intent.getStringExtra(BluetoothLeScanService.PARAM_DEVICE_NAME);

            Log.i(TAG, "Device found '" + contact.DeviceName + "' (" + contact.DeviceAddress + ")");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _listAdapter.addDevice(contact);
                    _listAdapter.notifyDataSetChanged();
                }
            });

            Toast.makeText(MainActivity.this, "Found: " + contact.DeviceName + " (" + contact.DeviceAddress + ")", Toast.LENGTH_SHORT).show();
        }
    }

    public class AlarmReceiver extends BroadcastReceiver {
        public static final String ACTION = "AlarmElapsed";

        @Override
        public void onReceive(Context c, Intent i) {
            Log.d(TAG, "Alarm triggered. Starting scan");

            Intent intent = new Intent(MainActivity.this, BluetoothLeScanService.class);
            startService(intent);

            scheduleScan();
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
