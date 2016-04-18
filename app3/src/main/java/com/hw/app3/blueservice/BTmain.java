package com.hw.app3.blueservice;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hw.app3.R;

import java.util.logging.Logger;


/**
 * Created by saic on 1/6/16.
 */
public class BTmain extends ActionBarActivity {

    private static final String TAG = "HomeWatcher";

    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();

    private static final int REQUEST_ENABLE = 0x1;

    private static final int REQUEST_DISCOVERABLE = 0x2;

    private BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothService mBluetoothService = null;

    protected static final int MESSAGE_BT_DEVICE_SELECTED = 0;//

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_BT_DEVICE_SELECTED:
                    //mBluetoothService.connect();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();

        }


        Intent serviceIntent = new Intent(this, BluetoothService.class);
        boolean flag = this.getApplicationContext().bindService(serviceIntent,
                new ServiceConnection() {
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        mBluetoothService = null;
                    }

                    @Override
                    public void onServiceConnected(ComponentName name,
                                                   IBinder service) {
                        mBluetoothService = ((BluetoothService.BluetoothBinder) service)
                                .getService();
                    }
                }, Context.BIND_AUTO_CREATE);



        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECT_SUCCESSED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECT_FAIL);
//        filter.addAction(BluetoothService.ACTION_BLUETOOTH_STATEDATA_RECEIVED);
//        filter.addAction(BluetoothService.ACTION_BLUETOOTH_WORKINGSTATEDATA_RECEIVED);
//        filter.addAction(BluetoothService.ACTION_BLUETOOTH_SAMPLEDATA_RECEIVED);
//        filter.addAction(OrderActivity.ACTION_CORROSION_START);
        registerReceiver(messageReceiver, filter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Logger.getAnonymousLogger().info("settings");
            Toast.makeText(this, "Settings selected", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.btactivity_main, container, false);
            return rootView;
        }
    }


    /* enable bluetooth*/
    public void onEnableButtonClicked(View view) {

        //Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(enabler, REQUEST_ENABLE);

        _bluetooth.enable();
    }


    /* dienable bluetooth*/
    public void onDisableButtonClicked(View view) {

        _bluetooth.disable();
    }


    /*search for devices*/
    public void onMakeDiscoverableButtonClicked(View view) {

        Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(enabler, REQUEST_DISCOVERABLE);
    }


    public void onStartDiscoveryButtonClicked(View view) {

        Intent enabler = new Intent(this, DiscoveryActivity.class);
        startActivity(enabler);
    }


    public void onOpenClientSocketButtonClicked(View view) {

        Intent enabler = new Intent(this, ClientSocketActivity.class);
        startActivity(enabler);
    }


    public void onOpenServerSocketButtonClicked(View view) {

        Intent enabler = new Intent(this, ServerSocketActivity.class);
        startActivity(enabler);
    }


    public void onOpenOBEXServerSocketButtonClicked(View view) {

        Intent enabler = new Intent(this, OBEXActivity.class);
        startActivity(enabler);
    }






    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        private String StringExtra = "";
        private Bundle bundle = new Bundle();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action
                    .equals(BluetoothService.ACTION_BLUETOOTH_CONNECT_SUCCESSED)) {
                StringExtra = intent.getStringExtra("BT_NAME");
                bundle.putString("SELECTED_DEVICE", StringExtra);
//                Message msg3 = handler
//                        .obtainMessage(MainActivity.MESSAGE_BT_CONNECT_SUCCESS);
//                msg3.setData(bundle);
//                handler.sendMessage(msg3);
            }



        }

    };

    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(messageReceiver);
    }



}

