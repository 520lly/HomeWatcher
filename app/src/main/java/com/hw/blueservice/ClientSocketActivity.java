package com.hw.blueservice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.hw.main.AppPeference;

import com.hw.main.R;

import java.util.UUID;
import com.hw.blueservice.protocol.*;


public class ClientSocketActivity extends Activity {
    private static final String TAG = "HomeWatcher";
    private static final int REQUEST_DISCOVERY = 0x1;
    public static BluetoothService mBluetoothService = null;
    private BluetoothDevice device;
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();
    public static Context sContext;
    private EditText dataSendET,payloadSizeET,packetNumET;
    private TextView dataRevTV,successTV,errorTV;
    private CheckBox multiPacketsCB;
    private ScrollView scrollView;
    private StringBuilder LogDataSB = new StringBuilder();
    private boolean isMultiPackets = false;
    private int testPacketSize = 10;                                  //10packets start from 1





    public static final int ACTION_BLUETOOTH_CONNECT_SUCCESSED = 0;
    public static final int ACTION_BLUETOOTH_CONNECT_FAIL = 1;
    public static final int ACTION_BLUETOOTH_WRITE_FAIL = 2;
    public static final int ACTION_BLUETOOTH_WRITE_SUCCESS = 3;
    public static final int ACTION_BLUETOOTH_DATA_RECIEVED = 4;
    public static final int ACTION_BLUETOOTH_REQ_SEND_DATA = 5;
    public static final int ACTION_BLUETOOTH_REQ_CLEAR_SEND_DATA = 6;
    public static final int ACTION_BLUETOOTH_REQ_CLEAR_REV_DATA = 7;
    public static final int ACTION_BLUETOOTH_WRITE_NULL_SOCKET = 8;
    public static final int ACTION_BLUETOOTH_UPDATE_CHANNLE_INFO = 9;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ACTION_BLUETOOTH_CONNECT_SUCCESSED:
                    LogDataSB.append("connect to "+device.getName()+" successed!\n");
                    dataRevTV.setText(LogDataSB.toString());
                    Toast.makeText(sContext, "connect successed!" ,Toast.LENGTH_SHORT).show();
                    if(mBluetoothService != null)
                    {
                        mBluetoothService.WriteCMD(device, Protocol.createCommandReq(Common.EProtocolName.PN_A2S_CREATE_RFCOMMN_CH,
                                Protocol.getCurIdentifier(), AppPeference.AppName.getBytes()));
                    }

                    break;
                case ACTION_BLUETOOTH_CONNECT_FAIL:
                    LogDataSB.append("connect to "+device.getName()+" fail!\n");
                    dataRevTV.setText(LogDataSB.toString());
                    Toast.makeText(sContext, "connect fail!" ,Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_BLUETOOTH_WRITE_FAIL:

                    Toast.makeText(sContext, "write data fail!" ,Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_BLUETOOTH_WRITE_SUCCESS:
                    dataSendET.setText("");

                    Toast.makeText(sContext, "write data success!" ,Toast.LENGTH_SHORT).show();

                    break;
                case ACTION_BLUETOOTH_DATA_RECIEVED:
                    String success = String.valueOf(msg.getData().getInt("DATA_REV_FROM_BT_SUC"));
                    String error = String.valueOf(msg.getData().getInt("DATA_REV_FROM_BT_ERR"));
                    String revData = msg.getData().getString("DATA_REV_FROM_BT");

                    LogDataSB.append(device.getName()+":"+revData + "\n");
                    dataRevTV.setText(LogDataSB.toString());
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    successTV.setText(success);
                    errorTV.setText(error);
//                    Toast.makeText(sContext, revData ,Toast.LENGTH_SHORT).show();
                    break;

                case ACTION_BLUETOOTH_REQ_SEND_DATA:
                    String data = dataSendET.getText().toString();
                    String num = packetNumET.getText().toString();

                    if(!num.equals(""))
                    {
                        testPacketSize = Integer.valueOf(num).intValue();
                    }
                    else
                    {
                        testPacketSize = 10;
                    }

                    Log.d(TAG, "isMultiPackets="+isMultiPackets +"   seq="+(int)Protocol.seq+ "    testPacketSize = "
                            +testPacketSize +"   seqIndex="+(int)Protocol.seqIndex);
                    if(mBluetoothService != null)
                    {
                        if(isMultiPackets)
                        {
                            if(Protocol.seq == 0)
                            {
                                Protocol.seqIndex = Common.EPacketSequenceIndex.PSI_START;
                                Protocol.seq += 1;
                            }
                            else
                            {
                                Protocol.seqIndex = Common.EPacketSequenceIndex.PSI_CONTINUTION;
                                if(Protocol.seq == testPacketSize)
                                {
                                    Protocol.seq = 0;
                                    Protocol.seqIndex = Common.EPacketSequenceIndex.PSI_END;
                                }
                                Protocol.seq += 1;

                            }
                        }
                        else
                        {
                            Protocol.seqIndex = Common.EPacketSequenceIndex.PSI_SINGLE;
                        }
                        String size = payloadSizeET.getText().toString();
                        int setPayloadSize = 0;
                        byte[] testMaxPayload;
                        Log.d(TAG, "payloadSizeET = "+size);
                        if(!size.equals(""))
                        {

                            setPayloadSize = Integer.valueOf(size).intValue();
                        }
                        else
                        {
                            setPayloadSize = Protocol.DataPacketDefaultLen;
                        }



                        if(setPayloadSize <= Protocol.MAX_DATA_PACKET_PAYLOAD_SIZE && setPayloadSize >= Protocol.MIN_DATA_PACKET_PAYLOAD_SIZE)
                        {
                            testMaxPayload = new byte[setPayloadSize];
                        }
                        else
                        {
                            testMaxPayload = new byte[Protocol.MAX_DATA_PACKET_PAYLOAD_SIZE];
                            Toast.makeText(sContext, "max payload size = "+Protocol.MAX_DATA_PACKET_PAYLOAD_SIZE ,Toast.LENGTH_SHORT).show();

                        }

                        int userDataLen = dataSendET.getText().toString().getBytes().length;
                        Log.d(TAG,"DataPacketMaxLen = "+testMaxPayload.length + "    setPayloadSize="+setPayloadSize+"    userDataLen="+userDataLen);

                        for(int i = 0; i < testMaxPayload.length; )
                        {
                            if(testMaxPayload.length > userDataLen)
                            {

                                for(int j = 0; j < userDataLen; j++)
                                {
                                    if(i+j < testMaxPayload.length)
                                    {
                                        testMaxPayload[i+j] = dataSendET.getText().toString().getBytes()[j];
                                    }
                                }


                                i+=userDataLen;
                                Log.d(TAG,"i = "+i+"   testMaxPayload.length = "+testMaxPayload.length);
                                if(i > testMaxPayload.length)
                                {
                                    i-=userDataLen;
                                    int offset = testMaxPayload.length - i;

                                    for(int j = 0; j < offset; j++)
                                    {
                                        Log.d(TAG,"i = "+i+"   j = "+j);
                                        testMaxPayload[i+j] = dataSendET.getText().toString().getBytes()[j];
                                    }
                                    break;
                                }
                            }
                            else
                            {
                                testMaxPayload[i] = dataSendET.getText().toString().getBytes()[i];
                                i++;
                            }
                        }

                        Log.d(TAG,"DataPacketMaxLen = "+testMaxPayload.length);
                        if(mBluetoothService.WriteCMD(device, Protocol.createDataPacket(Protocol.len1, Protocol.len2,
                                Protocol.seqIndex, Protocol.seq, Protocol.dcid, testMaxPayload)))
                        {
                            LogDataSB.append("Me:"+data + "* "+testMaxPayload.length+"\n");
                            dataRevTV.setText(LogDataSB.toString());
                        }
                        else
                        {
                            Toast.makeText(sContext, "write data fail!" ,Toast.LENGTH_SHORT).show();
                        }

                    }
                    break;

                case ACTION_BLUETOOTH_REQ_CLEAR_SEND_DATA:

                    dataSendET.setText("");
                    break;
                case ACTION_BLUETOOTH_REQ_CLEAR_REV_DATA:
                    dataRevTV.setText("");
                    successTV.setText("");
                    errorTV.setText("");
                    LogDataSB.delete(0, LogDataSB.length()-1);
                    mBluetoothService.resetCounter();
                    break;

                case ACTION_BLUETOOTH_WRITE_NULL_SOCKET:
                    Toast.makeText(sContext, "not connected!" ,Toast.LENGTH_SHORT).show();
                    break;

                case ACTION_BLUETOOTH_UPDATE_CHANNLE_INFO:
                    successTV.setText(""+(int)Protocol.scid);
                    errorTV.setText(""+(int)Protocol.dcid);
                    break;
            }
        }
    };









    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        setContentView(R.layout.client_socket);
        if (!_bluetooth.isEnabled()) {
            finish();
            return;
        }
        sContext = getApplicationContext();

        dataSendET = (EditText)findViewById(R.id.dataSendET);
        payloadSizeET = (EditText)findViewById(R.id.payloadSizeET);
        packetNumET = (EditText)findViewById(R.id.packetNumET);
        dataRevTV = (TextView)findViewById(R.id.dataRevTV);

        successTV = (TextView)findViewById(R.id.tv_success);
        errorTV = (TextView)findViewById(R.id.tv_eror);
        multiPacketsCB = (CheckBox)findViewById(R.id.multiPackets_cb);
        multiPacketsCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isMultiPackets = !isMultiPackets;
                Protocol.seq = 0;
                Protocol.seqIndex = Common.EPacketSequenceIndex.PSI_SINGLE;
                Log.d(TAG, "isMultiPackets = "+isMultiPackets);
            }
        });

        bindBluetoothService();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECT_SUCCESSED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECT_FAIL);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_WRITE_SUCCESS);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_WRITE_BULL_SOCKET);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_WRITE_FAIL);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_DATA_RECIEVED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO);


        registerReceiver(messageReceiver, filter);

        scrollView = (ScrollView)findViewById(R.id.sv_revdata);


        //Intent intent = new Intent(this, DiscoveryActivity.class);

        //Toast.makeText(this, "select device to connect", Toast.LENGTH_SHORT).show();

        //startActivityForResult(intent, REQUEST_DISCOVERY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_DISCOVERY) {
            return;
        }
        if (resultCode != RESULT_OK) {
            return;
        }
        device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        new Thread() {
            public void run() {

                mBluetoothService.connect(device, AppPeference.MY_UUID);

            }

        }.start();

    }

    private void bindBluetoothService()
    {
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

    }



    public void onCancelBtnClicked(View view)
    {
        Message msg = handler.obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_REQ_CLEAR_SEND_DATA);
        handler.sendMessage(msg);
    }

    public void onSendBtnClicked(View view)
    {
        Message msg = handler.obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_REQ_SEND_DATA);
        handler.sendMessage(msg);
    }

    public void onDiscBtnClicked(View view)
    {
        mBluetoothService.disconnect();
    }

    public void onConncBtnClicked(View view)
    {
        device = _bluetooth.getRemoteDevice("FC:35:E6:85:BE:73");
        mBluetoothService.connect(device, AppPeference.MY_UUID);
    }

    public void onClearBtnClicked(View view)
    {
        Message msg = handler.obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_REQ_CLEAR_REV_DATA);
        handler.sendMessage(msg);

    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        private String StringExtra = "";
        private  int dataLength = 0;
        private Bundle bundle = new Bundle();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action
                    .equals(BluetoothService.ACTION_BLUETOOTH_CONNECT_SUCCESSED)) {
                StringExtra = intent.getStringExtra("BT_NAME");
                bundle.putString("SELECTED_DEVICE", StringExtra);
                Log.i(TAG,StringExtra);

                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_CONNECT_SUCCESSED);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if (action
                    .equals(BluetoothService.ACTION_BLUETOOTH_CONNECT_FAIL)) {
                StringExtra = intent.getStringExtra("CONNECTION_FAIL_REASON");
                bundle.putString("CONNECTION_FAIL_REASON", StringExtra);
                Log.i(TAG,StringExtra);

                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_CONNECT_FAIL);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if(action
                    .equals(BluetoothService.ACTION_BLUETOOTH_WRITE_FAIL)) {

                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_WRITE_FAIL);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if(action
                    .equals(BluetoothService.ACTION_BLUETOOTH_WRITE_SUCCESS)) {

                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_WRITE_SUCCESS);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if (action
                    .equals(BluetoothService.ACTION_BLUETOOTH_WRITE_BULL_SOCKET)){
                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_WRITE_NULL_SOCKET);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if(action
                    .equals(BluetoothService.ACTION_BLUETOOTH_DATA_RECIEVED)) {
                dataLength = intent.getIntExtra("DATA_REV_FROM_BT_LENGTH", 0);
                StringExtra = new String(intent.getStringExtra("DATA_REV_FROM_BT"));
                int suc = intent.getIntExtra("DATA_REV_FROM_BT_SUC",0);
                int err = intent.getIntExtra("DATA_REV_FROM_BT_ERR",0);
                Log.d(TAG, "data rev len: "+dataLength +" data :"+StringExtra);
                bundle.putString("DATA_REV_FROM_BT", StringExtra);
                bundle.putInt("DATA_REV_FROM_BT_SUC",suc);
                bundle.putInt("DATA_REV_FROM_BT_ERR",err);
                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_DATA_RECIEVED);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else if(action.equals
                    (BluetoothService.ACTION_BLUETOOTH_UPDATE_CHANNEL_INFO)){
                Message msg = handler
                        .obtainMessage(ClientSocketActivity.ACTION_BLUETOOTH_UPDATE_CHANNLE_INFO);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }



        }

    };

    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(messageReceiver);
    }



}



