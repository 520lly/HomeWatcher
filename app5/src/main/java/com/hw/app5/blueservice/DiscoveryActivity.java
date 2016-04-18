package com.hw.app5.blueservice;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hw.app5.R;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryActivity extends ListActivity {
    private static final String TAG = "HomeWatcher";

    Context mContext = null;

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();

    private List<BluetoothDevice> _devices = new ArrayList<BluetoothDevice>();

    private volatile boolean _discoveryFinished;
    private Runnable _discoveryWorkder = new Runnable() {
        public void run() {

            _bluetooth.startDiscovery();
            Log.d(TAG, "startDiscovery is "+_bluetooth.isDiscovering());
            for (; ; ) {
                if (_discoveryFinished) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    };
    public static final int ACTION_BLUETOOTH_SEARCH_AGAIN = 0;
    private final Handler _handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ACTION_BLUETOOTH_SEARCH_AGAIN:
                    Log.d(TAG,"search again");
                    _discoveryFinished = false;
                    registerReciever();
                    startSearchDevices();
                    break;
            }

        }
    };

    private BroadcastReceiver _foundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG,"_foundReceiver action = "+action);
            Log.d(TAG, "Found new device :"+device.getName());
            if(!_devices.contains(device))_devices.add(device);

            showDevices();
        }
    };
    private BroadcastReceiver _discoveryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"_discoveryReceiver action = "+action);
            unregisterReceiver(_foundReceiver);
            unregisterReceiver(this);
            _discoveryFinished = true;
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        setContentView(R.layout.discovery);

        mContext = getApplicationContext();

        if (!_bluetooth.isEnabled()) {

            _bluetooth.enable();
        }

        registerReciever();

        startSearchDevices();

    }

    public void startSearchDevices()
    {
        SamplesUtils.indeterminate(DiscoveryActivity.this, _handler, "Scanning...", _discoveryWorkder, new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG,"stop search by touching screen");
                for (; _bluetooth.isDiscovering(); ) {

                    _bluetooth.cancelDiscovery();
                    Log.d(TAG,"cancelDiscovery");
                }

                _discoveryFinished = true;
            }
        }, true);
    }

    public void registerReciever()
    {
        IntentFilter discoveryFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(_discoveryReceiver, discoveryFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
        registerReceiver(_foundReceiver, filter);
    }

    protected void showDevices() {
        List<String> list = new ArrayList<String>();
        for (int i = 0, size = _devices.size(); i < size; ++i) {
            StringBuilder b = new StringBuilder();
            BluetoothDevice d = _devices.get(i);
            b.append(d.getAddress());
            b.append('\n');
            b.append(d.getName());
            String s = b.toString();
            list.add(s);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        _handler.post(new Runnable() {
            public void run() {
                setListAdapter(adapter);
            }
        });
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {

        Intent result = new Intent();
        result.putExtra(BluetoothDevice.EXTRA_DEVICE, _devices.get(position));
        setResult(RESULT_OK, result);
        finish();
    }

    public void onSearchAgainClicked(View view)
    {
        Message msg = _handler.obtainMessage(DiscoveryActivity.ACTION_BLUETOOTH_SEARCH_AGAIN);
        _handler.sendMessage(msg);
    }
}

