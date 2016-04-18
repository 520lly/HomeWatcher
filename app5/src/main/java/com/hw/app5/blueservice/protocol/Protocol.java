package com.hw.app5.blueservice.protocol;

import android.util.Log;

import com.hw.app5.blueservice.protocol.Common.*;

/**
 * Created by saic on 3/23/16.
 */
public class Protocol {
    private static final String TAG = "HomeWatcher";

    public final int MAX_BUF_SIZE = 128;
    static public byte curIdentifier = 0;



    public class CSppPacketData
    {
        public byte len1;
        public byte len2;
        public byte pt;
        public byte seq;
        public byte scid;
        public byte dsid;
        public byte dsize;
        public byte data[];
    }

    /*
    struct Commandpakcet
    {

    }
     */
    static public byte[] createCommandReq(byte code, byte id, byte[] data)
    {
        int len = data.length;
        byte[] cmdPacket = new byte[len+4];
        cmdPacket[0] = EPacketType.PT_CTR;
        Log.d(TAG, "packetType cmdPacket[0] = " + (int) cmdPacket[0]);
        cmdPacket[1] = code;
        Log.d(TAG,"code cmdPacket[1] = "+(int)cmdPacket[1]);
        cmdPacket[2] = id;
        Log.d(TAG,"identifier cmdPacket[2] = "+(int)cmdPacket[2]);
        cmdPacket[3] = (byte)len;
        Log.d(TAG,"len cmdPacket[3] = "+(int)cmdPacket[3]);
        for(int i = 0; i < len; i++)
        {
            cmdPacket[i+4] = data[i];
            Log.d(TAG,"payload cmdPacket["+i+4+"] = "+(int)cmdPacket[i+4]);
        }

        return cmdPacket;
    }


    static public byte getCurIdentifier()
    {
        if(curIdentifier >= 127)
        {
            curIdentifier = 0;
        }
        else
        {
            curIdentifier++;
        }
        return  curIdentifier;
    }
}
