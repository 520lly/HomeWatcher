package com.hw.blueservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.appcompat.R;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.os.Build;
import com.hw.blueservice.protocol.*;

/**
 * Created by saic on 3/14/16.
 */
public class BluetoothService extends Service {
    private static final String TAG = "HomeWatcher";
    private BluetoothAdapter mAdapter;
    private final IBinder binder = new BluetoothBinder();
    private static BluetoothSocket btSocket = null;
    private BluetoothDevice connectedDevice;
    private int lastSeq = -1;

    private int count = 0;
    private int correctCount = 0;

    public static final String ACTION_BLUETOOTH_CONNECT_SUCCESSED = "action_bluetooth_connect_successed";
    public static final String ACTION_BLUETOOTH_CONNECT_FAIL = "action_bluetooth_connect_fail";
    public static final String ACTION_BLUETOOTH_WRITE_FAIL = "action_bluetooth_write_failed";
    public static final String ACTION_BLUETOOTH_WRITE_SUCCESS = "action_bluetooth_write_successed";
    public static final String ACTION_BLUETOOTH_WRITE_BULL_SOCKET = "action_bluetooth_write_null_socket";
    public static final String ACTION_BLUETOOTH_DATA_RECIEVED = "action_bluetooth_data_recieved";
    public static final String ACTION_BLUETOOTH_CONNECTION_LOST= "action_bluetooth_connection_lost";


    private ConnectThread mConnectThread;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onBind");
        return binder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device, UUID uuid) {

        Log.d(TAG, "connect to: " + device.getName());
        if (device.getName() != null) {
            mConnectThread = new ConnectThread(device, uuid);
            mConnectThread.setName(device.getName());
            mConnectThread.start();

        } else {
            Toast.makeText(getApplicationContext(), "unavailable bluetooth!",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     *
     */
    public synchronized void disconnect() {

        Log.d(TAG, "disconnect to: " + btSocket);
        if (btSocket != null) {

            try {
                btSocket.close();
            }catch (IOException e){
                Log.e(TAG, "socket close", e);
            }
            Log.i(TAG, "socket closed");

        } else {
            Toast.makeText(getApplicationContext(), "unavailable bluesocket!",
                    Toast.LENGTH_LONG).show();
        }
    }


    /**
     *
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private String REASON = "", ErrorType = "";
        private BluetoothDevice mmDevice;
        private UUID myUUID;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            mmDevice = device;
            myUUID = uuid;
            // BluetoothServerSocket blueServerSocket = null;
            try {

                //tmp = device.createRfcommSocketToServiceRecord(myUUID);
                Log.d(TAG,"create bluesocket myUUID"+myUUID);
                int sdk = Integer.parseInt(Build.VERSION.SDK);
                Log.d(TAG, "current sdk version is "+sdk);
                if (sdk >= 10) {
                    mmSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                } else {
                    mmSocket = device.createRfcommSocketToServiceRecord(myUUID);
                }
            } catch (IOException e) {
                REASON = e.getLocalizedMessage();

                Log.e(TAG,
                        "create() failed>>>"
                                + REASON.equals("Service discovery failed"));
            }
            btSocket= mmSocket;

            Log.d(TAG, "ConnectThread  mmSocket=" + mmSocket);

        }

        @Override
        public void run() {

            Log.i(TAG, "begain ConnectThread to " + mmDevice.getName());
            mAdapter.cancelDiscovery();
            try {

                mmSocket.connect();

                connected(mmSocket, mmDevice);

            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {

                    Log.e(TAG, "socket close", e2);
                }
                REASON = e.getLocalizedMessage();

                Log.e(TAG, "connect IOExcrption=" + REASON);

                sendBroadcast(new Intent(ACTION_BLUETOOTH_CONNECT_FAIL).putExtra(
                        "CONNECTION_FAIL_REASON", REASON));
                //connectionFailed(ErrorType);
                // BluetoothService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
        }
    }


    /**
     *
     *
     * @param socket
     *            The BluetoothSocket on which the connection was made
     * @param device
     *            The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device) {
            Log.d(TAG, "connected socket "+socket);
        connectedDevice = device;
        BluetoothSocketListener mBluetoothSocketListener = new BluetoothSocketListener(
                socket, device);
        new Thread(mBluetoothSocketListener).start();

        sendBroadcast(new Intent(ACTION_BLUETOOTH_CONNECT_SUCCESSED).putExtra(
                "BT_NAME", device.getName()));
    }

    private void connectionFailed(String ErrorType) {
            Log.i(TAG, "connection failed");
        sendBroadcast(new Intent(ACTION_BLUETOOTH_CONNECT_FAIL).putExtra(
                "CONNEC_ERRORTYPE", ErrorType));

    }

    public class BluetoothSocketListener implements Runnable {
        private BluetoothSocket socket;
        InputStream instream = null;

        public BluetoothSocketListener(BluetoothSocket socket,
                                       BluetoothDevice device) {
            this.socket = socket;
            try
            {
                instream = this.socket.getInputStream();
            }catch (IOException e)
            {
                Log.e(TAG, "temp sockets not created", e);
            }
        }
        @Override
        public void run() {
            android.os.Process.setThreadPriority(-20);
            int bytes;

            byte[] buffer = new byte[1024];
            String ret = "";

            try {
                while (true) {
                    bytes = instream.read(buffer);
                    parserPacketData(buffer,bytes);
                    //WriteCMD(connectedDevice, buffer);
                }
            } catch (Exception e) {
                connectionLost();
                e.printStackTrace();
                Log.i("BLUETOOTH_COMMS", e.getMessage());
            } finally {
                Log.i(TAG, "finally");
            }

        }
    }

    public void parserPacketData(byte[] byarr, int bytes){
        byte packetType;

        packetType = byarr[0];
        switch (packetType)
        {
            case Common.EPacketType.PT_RESERVED:
                break;
            case Common.EPacketType.PT_CTR:
                break;
            case Common.EPacketType.PT_RES:



                break;
            case Common.EPacketType.PT_DATA:
                break;
        }
    }

    public void resetCounter()
    {
        count = 0;
        correctCount = 0;
    }

    public boolean WriteCMD(BluetoothDevice dest, byte[] context) {
        if(dest != null)
        {
            Log.i("msgBuffer", "send to " + dest.getName());
        }
        else
        {
            sendBroadcast(new Intent(ACTION_BLUETOOTH_WRITE_BULL_SOCKET));
            return false;
        }

        try {
            if(btSocket != null)
            {
                OutputStream outStream = btSocket.getOutputStream();
                outStream.write(context);
                sendBroadcast(new Intent(ACTION_BLUETOOTH_WRITE_SUCCESS));
                return true;
            }
            else
            {
                sendBroadcast(new Intent(ACTION_BLUETOOTH_WRITE_BULL_SOCKET));
                return false;
            }
        } catch (IOException e) {
            Log.e("WriteCommond", "ON RESUME: Exception during write.", e);
            sendBroadcast(new Intent(ACTION_BLUETOOTH_WRITE_FAIL));
            return false;

        }

    }

        /**
         * Indicate that the connection was lost and notify the UI Activity.
         */
        private void connectionLost() {
            // Send a failure message back to the Activity
            sendBroadcast(new Intent(ACTION_BLUETOOTH_CONNECTION_LOST).putExtra(
                    "DATA_REV_FROM_BT", "Device connection was lost"));
            // Start the service over to restart listening mode
//            BluestoothService.this.start();
        }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "start onUnbind~~~");
        return super.onUnbind(intent);
    }

    public class BluetoothBinder extends Binder {
        public BluetoothService getService() {
            Log.e(TAG, "BluetoothService~~~");
            return BluetoothService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}
