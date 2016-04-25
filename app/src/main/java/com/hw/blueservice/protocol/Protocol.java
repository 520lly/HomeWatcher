package com.hw.blueservice.protocol;

import android.util.Log;

import com.hw.blueservice.protocol.Common.*;
/**
 * Created by saic on 3/23/16.
 */
public class Protocol {
    private static final String TAG = "HomeWatcher";

    static public byte curIdentifier = 0;
    static public int BasicPacketHeaderLen = 1;
    static public int CommandPacketHeaderLen = 5;
    static public int DataPacketHeaderLen = 12;
    static public int DataPacketDefaultLen = 30 - BasicPacketHeaderLen -DataPacketHeaderLen;
    static public byte DataPacketSourceType = 0;           //Android source type
    static public char scid = 0;                           //Default scid
    static public byte len1 = 0;                           //Default len1
    static public byte len2 = 0;                           //Default len2
    static public char seq= 0;                             //Default seq
    static public char dcid = 0;                           //Default dcid
    static public byte seqIndex = 3;                       //Default packet sequence index
    static public final int MAX_DATA_PACKET_PAYLOAD_SIZE = 65536 - BasicPacketHeaderLen -DataPacketHeaderLen;
    static public final int MIN_DATA_PACKET_PAYLOAD_SIZE = BasicPacketHeaderLen -DataPacketHeaderLen + 1;



    public class XSBType
    {
        static public final byte LSB = 0;
        static public final byte MSB = 1;
    }

    /*
    struct BasicPacket
    {
        uint8_t packetType;
        uint8_t *packet;
    }
     */

    public class CommandPacket
    {
        public byte code;
        public byte identifier;
        public char scid;
        public byte len;
        public byte data[];
    }
    /*
    struct CommandPacket
    {
        uint8_t code;
        uint8_t identifier;
        uint16_t scid;
        uint8_t len;
        uint8_t *data;

    }
    CommandPacketHeaderLen = 6
     */
    static public byte[] createCommandReq(byte code, byte id, byte[] data)
    {
        int len = data.length;
        int offset = CommandPacketHeaderLen + BasicPacketHeaderLen;

        //BasicPacketHeaderLen
        byte[] cmdPacket = new byte[len+offset];
        cmdPacket[0] = EPacketType.PT_CTR;
        Log.d(TAG,"packetType cmdPacket[0] = "+(int)cmdPacket[0]);

        //CommandPacketHeader
        cmdPacket[1] = code;
        Log.d(TAG,"code cmdPacket[1] = "+(int)cmdPacket[1]);
        cmdPacket[2] = id;
        Log.d(TAG,"identifier cmdPacket[2] = "+(int)cmdPacket[2]);
        cmdPacket[3] = (byte)0;                                     //scid lsb
        Log.d(TAG,"len cmdPacket[4] = "+(int)cmdPacket[3]);
        cmdPacket[4] = (byte)0;                                     //scid msb
        Log.d(TAG,"len cmdPacket[5] = "+(int)cmdPacket[4]);
        cmdPacket[5] = (byte)len;                                   //Payload length
        Log.d(TAG,"len cmdPacket[3] = "+(int)cmdPacket[5]);

        //CommandPacketPayload
        for(int i = 0; i < len; i++)
        {
            cmdPacket[i+offset] = data[i];
            Log.d(TAG,"payload cmdPacket["+i+offset+"] = "+(int)cmdPacket[i+offset]);
        }

        return cmdPacket;
    }

    public class PacketData
    {
        public byte len1;
        public byte len2;
        public byte st;
        public byte cflag;
        public char seq;
        public char scid;
        public char dcid;
        public char dsize;
        public byte data[];
    }
    public class DataControlFlag
    {
        static public final byte DCF_RESERVED         = 0;
        static public final byte DCF_START_OF_DATA    = 1;
        static public final byte DCF_CONTINUE_OF_DATA = 2;
        static public final byte DCF_END_OF_DATA      = 3;
        static public final byte DCF_UNKNOWN          = 4;
    }

    /*
    struct DataPacket
    {
        uint8_t len1;
        uint8_t len2;
        uint8_t st;
        uint8_t cflag;
        uint16_t seq;
        uint16_t scid;
        uint16_t dcid;
        uint16_t dsize;
        uint8_t *data;

    }
    DataPacketHeaderLen = 12
     */
    static public byte[] createDataPacket(byte len1, byte len2, byte cflag, char seq, char dcid, byte[] data)
    {
        char len = (char)data.length;
        int offset = DataPacketHeaderLen + BasicPacketHeaderLen;

        //BasicPacketHeaderLen
        byte[] dataPacket = new byte[len+offset];
        dataPacket[0] = EPacketType.PT_DATA;
        Log.d(TAG,"packetType dataPacket[0] = "+(int)dataPacket[0]);

        //DataPacketHeader
        dataPacket[1] = len1;
        Log.d(TAG,"len1 dataPacket[1] = "+(int)dataPacket[1]);
        dataPacket[2] = len2;
        Log.d(TAG,"len2 dataPacket[2] = "+(int)dataPacket[2]);
        dataPacket[3] = DataPacketSourceType;                         //Source Type
        Log.d(TAG,"st dataPacket[3] = "+(int)dataPacket[3]);
        dataPacket[4] = cflag;                                        //cflag
        Log.d(TAG,"cflag dataPacket[4] = "+(int)dataPacket[4]);

        dataPacket[5] = getByteOfChar(seq,XSBType.LSB);                //seq lsb
        Log.d(TAG,"seq lsb dataPacket[5] = "+(int)dataPacket[5]);
        dataPacket[6] = getByteOfChar(seq,XSBType.MSB);                //seq msb
        Log.d(TAG,"seq msb dataPacket[6] = "+(int)dataPacket[6]);

        dataPacket[7] = getByteOfChar(scid,XSBType.LSB);                //scid lsb
        Log.d(TAG,"scid lsb dataPacket[7] = "+(int)dataPacket[7]);
        dataPacket[8] = getByteOfChar(scid,XSBType.MSB);                //scid msb
        Log.d(TAG,"scid msb dataPacket[8] = "+(int)dataPacket[8]);

        dataPacket[9] = getByteOfChar(dcid,XSBType.LSB);                //dcid lsb
        Log.d(TAG,"dcid lsb dataPacket[9] = "+(int)dataPacket[9]);
        dataPacket[10] = getByteOfChar(dcid,XSBType.MSB);                //dcid msb
        Log.d(TAG,"dcid msb dataPacket[10] = "+(int)dataPacket[10]);

        dataPacket[11] = getByteOfChar(len,XSBType.LSB);                //dsize lsb
        Log.d(TAG,"dsize lsb dataPacket[11] = "+(int)dataPacket[11]);
        dataPacket[12] = getByteOfChar(len,XSBType.MSB);                //dsize msb
        Log.d(TAG,"dsize msb dataPacket[12] = "+(int)dataPacket[12]);

        //CommandPacketPayload
        for(int i = 0; i < len; i++)
        {
            dataPacket[i+offset] = data[i];
            Log.d(TAG,"payload dataPacket["+(i+offset)+"] = "+(int)dataPacket[(i+offset)]);
        }

        return dataPacket;
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

    static public byte getByteOfChar(char c, byte xsb)
    {
        byte b[] = new byte[2];

        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        if(xsb == XSBType.LSB)
        {
            return b[1];
        }
        else
        {
            return b[0];
        }
    }
}
