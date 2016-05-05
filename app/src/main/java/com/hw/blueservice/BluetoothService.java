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
import com.hw.main.AppPeference;

/**
 * Created by saic on 3/14/16.
 */
public class BluetoothService extends Service {
    private static final String TAG = "HomeWatcher";
    private BluetoothAdapter mAdapter;
    private final IBinder binder = new BluetoothBinder();
    public static BluetoothSocket btSocket = null;
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
    public static final String ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO= AppPeference.AppName +"action_bluetooth_update_channel_infot";

    public static String recieveDataFlag = "RECV_DATA";

    private int curSize = 0;
    private int totSize = 0;
    private int dataChkStored = 0;
    private byte packetTypeStored = 0;
    private byte[] cachedPacket = new byte[Common.MAX_BASIC_PACKET_LEN];
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
//                    for(int i = 0; i< bytes ; i++)
//                    {
//                        Log.d(TAG,"buffer["+i+"] = "+buffer[i]);
//                    }
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
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>parserPacketData  bytes ="+bytes);
        byte packetType = 0;
        byte pt = 0;
        int ptLen = bytes;
        int curLen = 0;
        byte state =  BasicPacket.parserIndex.parserIndexTYPE;
        int dataU16 = 0;
        byte dataChk = 0;
        StringBuilder sb = new StringBuilder();

        while(ptLen > 0 && state < BasicPacket.parserIndex.parserIndexBADDETECTED)
        {
            Log.d(TAG, ">>>>>bytes = "+bytes +" ptLen = "+ptLen +" curLen = "+curLen + "  state = "+BasicPacket.stateBasicPacket[state] +
                   "  byarr["+curLen+"] = " +byarr[curLen]+"<<<<<");
            switch (state)
            {
                case  BasicPacket.parserIndex.parserIndexTYPE:
                    packetType = byarr[curLen];
                    Log.d(TAG, "byarr[curLen] = " + Common.statePacketType[byarr[curLen]]+"   bytes="+bytes);
                    ptLen--;
                    curLen++;
                    state = BasicPacket.parserIndex.parserIndexLEN0;
                    break;
                case  BasicPacket.parserIndex.parserIndexLEN0:
                    pt = byarr[curLen];
                    int a = pt;
                    a = pt&0xff;
                    dataU16 += a;
                    ptLen--;
                    curLen++;
                    Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    dataU16="+dataU16);
                    state = BasicPacket.parserIndex.parserIndexLEN1;

                    break;
                case  BasicPacket.parserIndex.parserIndexLEN1:
                    pt = byarr[curLen];
                    int b = pt;
                    b = pt&0xff;
                    dataU16 += b;
                    ptLen--;
                    curLen++;
                    Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    dataU16="+dataU16);
                    state = BasicPacket.parserIndex.parserIndexLEN2;
                    break;
                case  BasicPacket.parserIndex.parserIndexLEN2:
                    pt = byarr[curLen];
                    int c = pt;
                    c = pt&0xff;
                    dataU16 += c;
                    ptLen--;
                    curLen++;
                    Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    dataU16="+dataU16);
                    state = BasicPacket.parserIndex.parserIndexLEN3;
                    break;
                case  BasicPacket.parserIndex.parserIndexLEN3:
                    pt = byarr[curLen];
                    int d = pt;
                    d = pt&0xff;
                    dataU16 += d;
                    ptLen--;
                    curLen++;
                    Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    dataU16="+dataU16);
                    state = BasicPacket.parserIndex.parserIndexDATACHK;
                    break;
                case  BasicPacket.parserIndex.parserIndexDATACHK:
                    dataChk = byarr[curLen];
                    Log.d(TAG, "dataChk = "+dataChk);

                    ptLen--;
                    curLen++;
                    state = BasicPacket.parserIndex.parserIndexHEADERCHK;
                    break;

                case  BasicPacket.parserIndex.parserIndexHEADERCHK:
                    byte headerChk = calcChecksum(byarr, BasicPacket.basicPacketHeaderLen -1);
                    if(byarr[curLen] == headerChk || (0x100 - headerChk == byarr[curLen]))
                    {
                        ptLen -= 1;
                        curLen +=1;


                        packetTypeStored= packetType;
                        totSize = dataU16;
                        dataChkStored = dataChk;
                        Log.d(TAG, "dataChkStored = "+ dataChkStored);
                        if(dataU16 == ptLen)
                        {
                            Log.d(TAG, "checksum success ! received signle packet");
                            for(int i = 0; i < ptLen; i++)
                            {
                                cachedPacket[i] = byarr[i+BasicPacket.basicPacketHeaderLen];
                            }
                            state = BasicPacket.parserIndex.parserIndexDATA;

                        }
                        else
                        {
                            Log.d(TAG, "checksum success ! Push header packet into list");
                            for(int i = 0; i < ptLen; i++)
                            {
                                cachedPacket[i] = byarr[i+BasicPacket.basicPacketHeaderLen];
                            }
                            curSize +=  ptLen;

                            state = BasicPacket.parserIndex.parserIndexFINISH;
                        }
                    }
                    else
                    {
                        if(totSize > (curSize + bytes))
                        {
                            Log.d(TAG, "checksum failed ! Push segment packet into list");
                            for(int i = 0; i < bytes; i++)
                            {
                                cachedPacket[i+curSize] = byarr[i];
                            }
                            curSize +=  bytes;

                            state = BasicPacket.parserIndex.parserIndexFINISH;
                        }
                        else if(totSize == (curSize + bytes))
                        {
                            Log.d(TAG, "checksum failed ! Push last segment packet into list");
                            for(int i = 0; i < bytes; i++)
                            {
                                cachedPacket[i+curSize] = byarr[i];
                            }
                            curSize +=  ptLen;

                            state = BasicPacket.parserIndex.parserIndexDATA;
                        }
                        else
                        {
                            Log.d(TAG, "checksum failed ! TODO need to handle error!");
                            state = BasicPacket.parserIndex.parserIndexBADDETECTED;
                        }
                    }
                    break;

                case  BasicPacket.parserIndex.parserIndexDATA:
                    curLen +=ptLen;
                    ptLen -= ptLen;

                    if(dataChk == dataChkStored || dataChk == (0x100 - dataChkStored))
                    {
                        parserBasicPacket(packetTypeStored, cachedPacket);
                        state = BasicPacket.parserIndex.parserIndexFINISH;
                    }
                    else
                    {
                        Log.d(TAG, "data checksum failed ! TODO need to handle error!");
                        state = BasicPacket.parserIndex.parserIndexBADDETECTED;
                    }

                    break;

                case  BasicPacket.parserIndex.parserIndexFINISH:
                    ptLen -= ptLen;
                    curLen +=ptLen;
                    break;
                case  BasicPacket.parserIndex.parserIndexBADDETECTED:
                    curLen +=ptLen;
                    ptLen -= ptLen;
                    break;
            }
        }
        resetParserBasicPacket();
    }

    public void parserBasicPacket(byte packetType, byte[] cachedPacket)
    {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>parserBasicPacket  packetType = "+packetType +"    parserBasicPacket totSize = "+totSize +" cachedPacket = "+ cachedPacket.length);

            switch (packetType)
            {
                case Common.EPacketType.PT_RESERVED:
                    break;
                case Common.EPacketType.PT_CTR:
                    parserCtrPacket(cachedPacket);
                    break;
                case Common.EPacketType.PT_DATA:
                    parserDataPacket(cachedPacket);
                    break;
                case Common.EPacketType.PT_RES:
                    parserRspPacket(cachedPacket);
                    break;
                case Common.EPacketType.PT_INFO:
                    parserInfoPacket(cachedPacket);
                    break;

            }

    }

    public void resetParserBasicPacket()
    {
        curSize = 0;
        totSize = 0;
        dataChkStored = 0;
        packetTypeStored = 0;
        for(int i = 0 ; i <Common.MAX_BASIC_PACKET_LEN; i++)
        {
            cachedPacket[i] = 0;
        }
        Log.d(TAG, "resetParserBasicPacket");
    }

    public void parserCtrPacket(byte[] cachedPacket)
    {
        Log.d(TAG, "parserCtrPacket");
    }

    public void parserDataPacket(byte[] cachedPacket)
    {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>parserDataPacket");
        byte pt = 0;
        int ptLen = totSize;
        int curLen = 0;
        byte state;
        int dataU16 = 0;
        StringBuilder sb = new StringBuilder();

        DataPacket dataPacket = new DataPacket();
        int tmp;
        state = Common.EDataPacketFieldIndex.DataPacketFieldIndexST;
        while(ptLen > 0 && state < Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH)
        {
            Log.d(TAG, "ptLen = "+ptLen +" curLen = "+curLen + "  state = "+Common.stateDataPacket[state] +
                    "   pt = " + (int)pt + "  cachedPacket["+curLen+"] = " +cachedPacket[curLen]);


            switch (state)
            {
                case Common.EDataPacketFieldIndex.DataPacketFieldIndexST:
                    dataPacket.st = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexCFLAG;
                    Log.d(TAG, "st = "+(int)dataPacket.st);
                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexCFLAG:
                    dataPacket.cflag = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexSEQ;
                    Log.d(TAG, "cflag = "+(int)dataPacket.cflag);
                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexSEQ:
                    pt = cachedPacket[curLen];
                    tmp = pt;
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    a="+a);
                    ptLen--;
                    curLen++;

                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    a="+a);
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    dataPacket.seq = dataU16;
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexSCID;
                    Log.d(TAG, "seq = "+ dataPacket.seq);
                    //sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataPacket.scid).putExtra("dcid",dataPacket.dcid));
                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexSCID:
                    pt = cachedPacket[curLen];
                    tmp = pt;
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    tmp="+tmp);
                    ptLen--;
                    curLen++;

                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    tmp="+tmp);
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    dataPacket.scid = dataU16;
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDCID;
                    Log.d(TAG, "scid = "+ dataPacket.scid);

                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexDCID:
                    dataU16 = 0;
                    pt = cachedPacket[curLen];
                    tmp = pt;
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    ptLen--;
                    curLen++;

                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    dataPacket.dcid = dataU16;
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDSIZE;
                    Log.d(TAG, "dcid = "+ dataPacket.dcid);

                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexDSIZE:
                    dataU16 = 0;
                    pt = cachedPacket[curLen];
                    tmp = pt;
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    ptLen--;
                    curLen++;

                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    dataPacket.dsize = dataU16;
                    ptLen--;
                    curLen++;
                    state = Common.EDataPacketFieldIndex.DataPacketFieldIndexPAYLOAD;
                    Log.d(TAG, "dsize = "+ dataPacket.dsize);

                    break;
                case Common.EDataPacketFieldIndex.DataPacketFieldIndexPAYLOAD:
                    Log.d(TAG, "DataPacketFieldIndexPAYLOAD");
                    if(dataPacket.dsize == ptLen)
                    {
                        dataPacket.data = new StringBuffer();
                        for(int i = 1; i <= ptLen ; i++)
                        {
                            Log.d(TAG, " "+(int)cachedPacket[Common.DATA_PACKET_HEADER_LEN + i]);
                            dataPacket.data.append((char)cachedPacket[Common.DATA_PACKET_HEADER_LEN + i]);
                            //dataPacket.data.add((char)byarr[Common.DATA_PACKET_HEADER_LEN + i]);
                        }

                        ptLen -= dataPacket.dsize;
                        curLen += dataPacket.dsize;
                        state = Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH;

                    }
                    else
                    {
                        state = Common.EDataPacketFieldIndex.DataPacketFieldIndexDETECTEDBAD;
                    }

                    sb.delete(0, sb.length());
                    sb.append("[Recieved dataPacket]: ");
                    sb.append(" st="+(int)dataPacket.st);
                    sb.append(" cflag="+(int)dataPacket.cflag);
                    sb.append(" seq="+dataPacket.seq);
                    sb.append(" scid="+dataPacket.scid);
                    sb.append(" dcid="+dataPacket.dcid);
                    sb.append(" dsize="+dataPacket.dsize);
                    sb.append(" data="+dataPacket.data.toString());

                    Log.d(TAG, sb.toString());
                    sendBroadcast(new Intent(ACTION_BLUETOOTH_DATA_RECIEVED).putExtra(recieveDataFlag, sb.toString()));
                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexFINISH:
                    Log.d(TAG, "state = "+Common.stateResPacket[state]);
                    break;

                case Common.EDataPacketFieldIndex.DataPacketFieldIndexDETECTEDBAD:
                    Log.d(TAG, "state = "+Common.stateResPacket[state]);
                    break;
            }
        }

    }

    public void parserRspPacket( byte[] cachedPacket)
    {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>parserRspPacket");
        byte pt = 0;
        int ptLen = totSize;
        int curLen = 0;
        byte state;
        int dataU16 = 0;
        int tmp = 0;
        StringBuilder sb = new StringBuilder();

        ResPacket rspPacket = new ResPacket();
        state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexCODE;
        while(ptLen > 0 && state < Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH)
        {
            Log.d(TAG, "ptLen = "+ptLen +" curLen = "+curLen + "  state = "+Common.stateResPacket[state] +
                    "   pt = " + (int)pt + "  cachedPacket["+curLen+"] = " +cachedPacket[curLen]);


            switch (state)
            {
                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexCODE:
                    rspPacket.code = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexIDENTIFIER;
                    Log.d(TAG, "code = "+(int)rspPacket.code);
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexIDENTIFIER:
                    rspPacket.identifier = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexLEN;
                    Log.d(TAG, "identifier = "+(int)rspPacket.identifier);
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexLEN:
                    rspPacket.len = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexRESULT;
                    Log.d(TAG, "len = "+(int)rspPacket.len);
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexRESULT:
                    rspPacket.result = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexTYPE;
                    Log.d(TAG, "result = "+(int)rspPacket.result);
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexTYPE:
                    rspPacket.type = cachedPacket[curLen];
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCIDMSB;
                    Log.d(TAG, "type = "+(int)rspPacket.type);
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCIDMSB:
                    rspPacket.scidMsb = cachedPacket[curLen];
                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    a="+a);
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCIDLSB;
                    break;
                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexSCIDLSB:
                    rspPacket.scidLsb = cachedPacket[curLen];
                    pt = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen]+"    a="+a);
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCIDMSB;
                    Log.d(TAG, "scid = "+ dataU16);
                    Protocol.scid = (char)dataU16;
                    sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataU16).putExtra("dcid",dataU16));
                    break;
                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCIDMSB:
                    dataU16 = 0;
                    pt = cachedPacket[curLen];
                    rspPacket.dcidMsb = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += (tmp<<8 & 0x0000ffff);
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    ptLen--;
                    curLen++;
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCIDLSB;
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexDCIDLSB:
                    pt = cachedPacket[curLen];
                    rspPacket.dcidLsb = cachedPacket[curLen];
                    tmp = pt&0xff;
                    dataU16 += tmp;
                    //Log.d(TAG, "byarr["+curLen+"] = "+byarr[curLen] +"    b="+b);
                    ptLen--;
                    curLen++;
                    Protocol.dcid = (char)dataU16;
                    sendBroadcast(new Intent(ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO).putExtra("scid",dataU16).putExtra("dcid",dataU16));

                    sb.delete(0,sb.length());
                    sb.append("[Recieved rspPacket]: ");
                    sb.append("code=" + (int) rspPacket.code);
                    sb.append(" identifier="+(int)rspPacket.identifier);
                    sb.append(" len="+(int)rspPacket.len);
                    sb.append(" result="+(int)rspPacket.result);
                    sb.append(" scidMsb="+rspPacket.scidMsb);
                    sb.append(" scidLsb="+rspPacket.scidLsb);
                    sb.append(" dcidMsb="+rspPacket.dcidMsb);
                    sb.append(" dcidLsb="+rspPacket.dcidLsb);

                    Log.d(TAG, sb.toString());
                    sendBroadcast(new Intent(ACTION_BLUETOOTH_DATA_RECIEVED).putExtra(recieveDataFlag, sb.toString()));
                    state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexPAYLOAD;
                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexPAYLOAD:
                    Log.d(TAG, "RspPacketFieldIndexPAYLOAD");
                    if(rspPacket.len == ptLen)
                    {
                        rspPacket.data = new ArrayList();
                        for(int i = 1; i <= ptLen ; i++)
                        {
                            rspPacket.data.add((char)cachedPacket[Common.RES_PACKET_HEADER_LEN + i]);
                        }
                        ptLen -= rspPacket.len;
                        curLen += rspPacket.len;
                        state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH;

                        sb.delete(0,sb.length());
                        sb.append("[Recieved rspPacket]: ");
                        sb.append("code=" + (int) rspPacket.code);
                        sb.append(" identifier="+(int)rspPacket.identifier);
                        sb.append(" len="+(int)rspPacket.len);
                        sb.append(" result="+(int)rspPacket.result);
                        sb.append(" scidMsb="+rspPacket.scidMsb);
                        sb.append(" scidLsb="+rspPacket.scidLsb);
                        sb.append(" dcidMsb="+rspPacket.dcidMsb);
                        sb.append(" dcidLsb="+rspPacket.dcidLsb);

                        Log.d(TAG, sb.toString());
                        sendBroadcast(new Intent(ACTION_BLUETOOTH_DATA_RECIEVED).putExtra(recieveDataFlag, sb.toString()));

                    }
                    else
                    {
                        state = Common.EResponsePacketFieldIndex.RspPacketFieldIndexDETECTEDBAD;
                    }

                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexFINISH:
                    Log.d(TAG, "state = "+Common.stateResPacket[state]);

                    break;

                case Common.EResponsePacketFieldIndex.RspPacketFieldIndexDETECTEDBAD:
                    Log.d(TAG, "state = "+Common.stateResPacket[state]);
                    break;
            }
        }


    }
    public void parserInfoPacket(byte[] cachedPacket)
    {
        Log.d(TAG, "parserInfoPacket");
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

    public byte calcChecksum(byte[] arr, int len)
    {
        byte checksum = 0;
        for(int i = 0; i < len; i++)
        {
            checksum += arr[i];
        }
        Log.d(TAG, "calcChecksum checksum = "+checksum);
        return checksum;
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
