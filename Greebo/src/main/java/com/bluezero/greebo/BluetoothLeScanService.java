package com.bluezero.greebo;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BluetoothLeScanService extends IntentService {
    private static final String TAG = BluetoothLeScanService.class.getSimpleName();

    private BluetoothAdapter _bluetoothAdapter;
    private boolean _scanning;

    private Handler _handler;

    private Object _scanLock = new Object();

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD_MS = 5000;

    public static final String PARAM_DEVICE_NAME = "DeviceName";
    public static final String PARAM_DEVICE_ADDRESS = "DeviceAddress";

    public BluetoothLeScanService() {
        super("BluetoothLeScanService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _handler = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        _bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onDestroy() {
        if (_scanning) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            _handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _scanning = false;
                    _bluetoothAdapter.stopLeScan(_leScanCallback);

                    synchronized (_scanLock) {
                        _scanLock.notifyAll();
                    }
                }
            }, SCAN_PERIOD_MS);

            _scanning = true;
            _bluetoothAdapter.startLeScan(_leScanCallback);

            synchronized (_scanLock) {
                try {
                    _scanLock.wait();
                }
                catch (InterruptedException e) {
                }
            }
        }
        else {
            _scanning = false;
            _bluetoothAdapter.stopLeScan(_leScanCallback);
        }
    }

    private void broadcastResult(String deviceName, String address) {
        Intent intent = new Intent();
        intent.setAction(MainActivity.BluetoothLeScanResultReceiver.ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(PARAM_DEVICE_NAME, deviceName);
        intent.putExtra(PARAM_DEVICE_ADDRESS, address);
        sendBroadcast(intent);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback _leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String deviceName = device.getName();
            String address = device.getAddress();

            broadcastResult(deviceName, address);
        }
    };
}