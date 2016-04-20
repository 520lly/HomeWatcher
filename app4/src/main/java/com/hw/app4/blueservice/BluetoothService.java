package com.hw.app4.blueservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import com.hw.app4.blueservice.protocol.*;
import com.hw.app4.main.AppPeference;

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

    public static final String ACTION_BLUETOOTH_CONNECT_SUCCESSED = AppPeference.AppName +"action_bluetooth_connect_successed";
    public static final String ACTION_BLUETOOTH_CONNECT_FAIL = AppPeference.AppName +"action_bluetooth_connect_fail";
    public static final String ACTION_BLUETOOTH_WRITE_FAIL = AppPeference.AppName +"action_bluetooth_write_failed";
    public static final String ACTION_BLUETOOTH_WRITE_SUCCESS = AppPeference.AppName +"action_bluetooth_write_successed";
    public static final String ACTION_BLUETOOTH_WRITE_BULL_SOCKET = AppPeference.AppName +"action_bluetooth_write_null_socket";
    public static final String ACTION_BLUETOOTH_DATA_RECIEVED = AppPeference.AppName +"action_bluetooth_data_recieved";
    public static final String ACTION_BLUETOOTH_CONNECTION_LOST= AppPeference.AppName +"action_bluetooth_connection_lost";
    public static final String ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO= "action_bluetooth_update_channel_infot";


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
        byte pt = 0;
        int ptLen = bytes;
        int curLen = 0;
        byte state;
        int dataU16 = 0;

        packetType = byarr[curLen];
        Log.d(TAG, "packetType = " + Common.statePacketType[packetType]);
        ptLen--;
        curLen++;

        switch (packetType)
        {
            case Common.EPacketType.PT_RESERVED:
                break;
            case Common.EPacketType.PT_CTR:
                break;
            case Common.EPacketType.PT_RES:
                ResPacket rspPacket = new ResPacket();
                state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexCODE;
                while(ptLen > 0 && state < Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH)
                {
                    Log.d(TAG, "ptLen = "+ptLen +" curLen = "+curLen + "  state = "+Common.stateResPacket[state] +
                            "   pt = " + (int)pt + "  byarr["+curLen+"] = " +byarr[curLen]);


                    switch (state)
                    {
                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexCODE:
                            rspPacket.code = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexIDENTIFIER;
                            Log.d(TAG, "code = "+(int)rspPacket.code);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexIDENTIFIER:
                            rspPacket.identifier = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexLEN;
                            Log.d(TAG, "identifier = "+(int)rspPacket.identifier);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexLEN:
                            rspPacket.len = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexRESULT;
                            Log.d(TAG, "len = "+(int)rspPacket.len);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexRESULT:
                            rspPacket.result = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCID;
                            Log.d(TAG, "result = "+(int)rspPacket.result);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCID:
                            pt = byarr[curLen];
                            int a = pt;
                            a = pt&0xff;
                            dataU16 += a;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    a="+a);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            a = pt&0xff;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    a="+a);
                            dataU16 += (a<<8 & 0x0000ffff);
                            rspPacket.scid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCID;
                            Log.d(TAG, "scid = "+ rspPacket.scid);
                            Protocol.scid = (char)rspPacket.scid;
                            sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",rspPacket.scid).putExtra("dcid",rspPacket.dcid));
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCID:
                            dataU16 = 0;
                            pt = byarr[curLen];
                            int b = pt;
                            b = pt&0xff;
                            dataU16 += b;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            b = pt&0xff;
                            dataU16 += (b<<8 & 0x0000ffff);
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            rspPacket.dcid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexPAYLOAD;
                            Log.d(TAG, "dcid = "+ rspPacket.dcid);
                            Protocol.dcid = (char)rspPacket.dcid;
                            sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",rspPacket.scid).putExtra("dcid",rspPacket.dcid));
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexPAYLOAD:
                            Log.d(TAG, "RspPacketFieldIndexPAYLOAD");
                            if(rspPacket.len == ptLen)
                            {
                                for(int i = 0; i < ptLen ; i++)
                                {
                                    rspPacket.data[i] = byarr[Common.RES_PACKET_HEADER_LEN + i];
                                }
                                ptLen -= rspPacket.len;
                                curLen += rspPacket.len;
                                state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH;

                            }
                            else
                            {
                                state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexDETECTEDBAD;
                            }

                            Log.d(TAG, "respacket code="+(int)rspPacket.code+"  indentifier="+(int)rspPacket.identifier+
                                    "  len="+(int)rspPacket.len+"  result="+(int)rspPacket.result+"  scid="+rspPacket.scid+
                                    "  dcid="+rspPacket.dcid+"  data="+rspPacket.data);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH:
                            Log.d(TAG, "state = "+Common.stateResPacket[state]);
                            break;

                        case Common.EResponsePacketFieldIndex.RspPacketFieldIndexDETECTEDBAD:
                            Log.d(TAG, "state = "+Common.stateResPacket[state]);
                            break;
                    }
                }


                break;
            case Common.EPacketType.PT_DATA:
                DataPacket dataPacket = new DataPacket();
                int tmp;
                state = Common.EDataPacketFieldIndex.DataPacketFieldIndexLEN1;
                while(ptLen > 0 && state < Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH)
                {
                    Log.d(TAG, "ptLen = "+ptLen +" curLen = "+curLen + "  state = "+Common.stateDataPacket[state] +
                            "   pt = " + (int)pt + "  byarr["+curLen+"] = " +byarr[curLen]);


                    switch (state)
                    {
                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexLEN1:
                            dataPacket.len1 = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexLEN2;
                            Log.d(TAG, "code = "+(int)dataPacket.len1);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexLEN2:
                            dataPacket.len2 = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexST;
                            Log.d(TAG, "identifier = "+(int)dataPacket.len2);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexST:
                            dataPacket.st = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexCFLAG;
                            Log.d(TAG, "len = "+(int)dataPacket.st);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexCFLAG:
                            dataPacket.cflag = byarr[curLen];
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexSEQ;
                            Log.d(TAG, "result = "+(int)dataPacket.cflag);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexSEQ:
                            pt = byarr[curLen];
                            tmp = pt;
                            tmp = pt&0xff;
                            dataU16 += tmp;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    a="+a);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            tmp = pt&0xff;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    a="+a);
                            dataU16 += (tmp<<8 & 0x0000ffff);
                            dataPacket.scid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexSCID;
                            Log.d(TAG, "scid = "+ dataPacket.scid);
                            //sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataPacket.scid).putExtra("dcid",dataPacket.dcid));
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexSCID:
                            pt = byarr[curLen];
                            tmp = pt;
                            tmp = pt&0xff;
                            dataU16 += tmp;
                            Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    tmp="+tmp);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            tmp = pt&0xff;
                            Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    tmp="+tmp);
                            dataU16 += (tmp<<8 & 0x0000ffff);
                            dataPacket.scid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDCID;
                            Log.d(TAG, "scid = "+ dataPacket.scid);
                            sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataPacket.scid).putExtra("dcid",dataPacket.dcid));
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexDCID:
                            dataU16 = 0;
                            pt = byarr[curLen];
                            tmp = pt;
                            tmp = pt&0xff;
                            dataU16 += tmp;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            tmp = pt&0xff;
                            dataU16 += (tmp<<8 & 0x0000ffff);
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            dataPacket.dcid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDSIZE;
                            Log.d(TAG, "dcid = "+ dataPacket.dcid);
                            sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataPacket.scid).putExtra("dcid",dataPacket.dcid));
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexDSIZE:
                            dataU16 = 0;
                            pt = byarr[curLen];
                            tmp = pt;
                            tmp = pt&0xff;
                            dataU16 += tmp;
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            ptLen--;
                            curLen++;

                            pt = byarr[curLen];
                            tmp = pt&0xff;
                            dataU16 += (tmp<<8 & 0x0000ffff);
                            //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                            dataPacket.dcid = dataU16;
                            ptLen--;
                            curLen++;
                            state = Common.EDataPacketFieldIndex.DataPacketFieldIndexPAYLOAD;
                            Log.d(TAG, "dsize = "+ dataPacket.dsize);

                            break;
                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexPAYLOAD:
                            Log.d(TAG, "DataPacketFieldIndexPAYLOAD");
                            if(dataPacket.dsize == ptLen)
                            {
                                for(int i = 0; i < ptLen ; i++)
                                {
                                    dataPacket.data[i] = byarr[Common.RES_PACKET_HEADER_LEN + i];
                                }
                                ptLen -= dataPacket.dsize;
                                curLen += dataPacket.dsize;
                                state = Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH;

                            }
                            else
                            {
                                state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDETECTEDBAD;
                            }

                            Log.d(TAG, "respacket len1="+(int)dataPacket.len1+"  len2="+(int)dataPacket.len2+
                                    "  st="+(int)dataPacket.st+"  cflag="+(int)dataPacket.cflag+"  scid="+dataPacket.scid+
                                    "  dcid="+dataPacket.dcid+"  dsize="+dataPacket.dsize+"  data="+dataPacket.data);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH:
                            Log.d(TAG, "state = "+Common.stateResPacket[state]);
                            break;

                        case Common.EDataPacketFieldIndex.DataPacketFieldIndexDETECTEDBAD:
                            Log.d(TAG, "state = "+Common.stateResPacket[state]);
                            break;
                    }
                }

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
