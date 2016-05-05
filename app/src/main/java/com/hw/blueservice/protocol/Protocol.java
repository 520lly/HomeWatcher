package com.hw.blueservice.protocol;

import android.util.Log;

import com.hw.blueservice.protocol.Common.*;
/**
 * Created by saic on 3/23/16.
 */
public class Protocol {
    private static final String TAG = "HomeWatcher";

    static public byte curIdentifier = 0;
    static public int BasicPacketHeaderLen = 7;
    static public int CommandPacketHeaderLen = 6;
    static public int DataPacketHeaderLen = 10;
    static public int DataPacketDefaultLen = 30 - BasicPacketHeaderLen -DataPacketHeaderLen;
    static public byte DataPacketSourceType = 0;           //Android source type
    static public char scid = 0;                           //Default scid
    static public char seq= 0;                             //Default seq
    static public char dcid = 0;                           //Default dcid
    static public byte seqIndex = 3;                       //Default packet sequence index
    static public final int MAX_DATA_PACKET_PAYLOAD_SIZE = 65536 - BasicPacketHeaderLen -DataPacketHeaderLen;
    static public final int MIN_DATA_PACKET_PAYLOAD_SIZE = BasicPacketHeaderLen -DataPacketHeaderLen + 1;



    public class XSBType
    {
        static public final byte HLSB = 0;
        static public final byte HMSB = 1;
        static public final byte LLSB = 2;
        static public final byte LMSB = 3;
    }

    /*
    struct BasicPacket
    {
        uint8_t packetType;
        uint32_t packetLen;
        uint8_t checksum;
        uint8_t *packet;
    }
     */
    static public byte[] createBasicPacket(byte type, byte[] context)
    {
        int payloadLen = context.length;
        Log.d(TAG, " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>createBasicPacket  payloadLen= "+payloadLen);
        byte HeaderChecksum = 0;
        byte dataChecksum = 0;
        int basicPacketLen = BasicPacketHeaderLen + payloadLen;

        byte[] basicPacket = new byte[basicPacketLen];

        basicPacket[0] = type;

        //little ending
        basicPacket[1] = (byte)((payloadLen >> 24) & 0xFF);
        basicPacket[2] = (byte)((payloadLen >> 16) & 0xFF);
        basicPacket[3] = (byte)((payloadLen >> 8) & 0xFF);
        basicPacket[4] = (byte)(payloadLen & 0xFF);

        for(int j = 0; j < payloadLen ;j++)
        {
            dataChecksum += context[j];
        }
        Log.d(TAG, " dataChecksum = "+dataChecksum);
        basicPacket[5] = dataChecksum;

        for(int i = 0; i < BasicPacketHeaderLen; i++)
        {
            HeaderChecksum += basicPacket[i];

        }
        Log.d(TAG, " HeaderChecksum = "+HeaderChecksum);
        basicPacket[6] = HeaderChecksum;

        StringBuffer sb = new StringBuffer();
        sb.append("  type="+(int)type);
        sb.append("  len1="+(int)basicPacket[1]);
        sb.append("  len2="+(int)basicPacket[2]);
        sb.append("  len3="+(int)basicPacket[3]);
        sb.append("  len4="+(int)basicPacket[4]);
        sb.append("  dataChecksum="+basicPacket[5]);
        sb.append("  headerChecksum="+(int)basicPacket[6]);

        System.arraycopy(context,0, basicPacket, BasicPacketHeaderLen, payloadLen);

        Log.d(TAG, "sb.toString " + sb.toString());
        return basicPacket;
    }

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
        uint8_t len;
        uint8_t type;
        uint16_t scid;
        uint8_t *data;

    }
    CommandPacketHeaderLen = 6
     */
    static public byte[] createCommandReq(byte code, byte id, byte type, byte[] data)
    {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>createCommandReq");
        int len = data.length;
        int offset = CommandPacketHeaderLen;

        //BasicPacketHeaderLen
        byte[] cmdPacket = new byte[len+offset];

        //CommandPacketHeader
        cmdPacket[0] = code;
        Log.d(TAG,"code cmdPacket[1] = "+(int)cmdPacket[0]);
        cmdPacket[1] = id;
        Log.d(TAG,"identifier cmdPacket[2] = "+(int)cmdPacket[1]);
        cmdPacket[2] = (byte)len;                                   //Payload length
        Log.d(TAG,"len cmdPacket[3] = "+(int)cmdPacket[2]);
        cmdPacket[3] = type;                                        //data type
        Log.d(TAG,"len cmdPacket[4] = "+(int)cmdPacket[3]);
        cmdPacket[4] = (byte)0;                                     //scid lsb
        Log.d(TAG,"len cmdPacket[4] = "+(int)cmdPacket[4]);
        cmdPacket[5] = (byte)0;                                     //scid msb
        Log.d(TAG,"len cmdPacket[5] = "+(int)cmdPacket[5]);

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
    static public byte[] createDataPacket(byte cflag, char seq, char dcid, byte[] data)
    {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>createDataPacket");
        char len = (char)data.length;
        int offset = DataPacketHeaderLen;

        StringBuffer sb = new StringBuffer();

        //BasicPacketHeaderLen
        byte[] dataPacket = new byte[len+offset];

        //DataPacketHeader
        dataPacket[0] = DataPacketSourceType;                           //st
        sb.append("   st dataPacket[3] = "+(int)dataPacket[0]);
        dataPacket[1] = cflag;                                          //cflag
        sb.append("   cflag dataPacket[4] = " + (int) dataPacket[1]);

        dataPacket[2] = getByteOfChar(seq,XSBType.HLSB);                //seq lsb
        sb.append("   seq lsb dataPacket[5] = "+(int)dataPacket[2]);
        dataPacket[3] = getByteOfChar(seq,XSBType.HMSB);                //seq msb
        sb.append("   seq msb dataPacket[6] = " + (int) dataPacket[3]);

        dataPacket[4] = getByteOfChar(scid,XSBType.HLSB);                //scid lsb
        sb.append("   scid lsb dataPacket[7] = "+(int)dataPacket[4]);
        dataPacket[5] = getByteOfChar(scid,XSBType.HMSB);                //scid msb
        sb.append("   scid msb dataPacket[8] = " + (int) dataPacket[5]);

        dataPacket[6] = getByteOfChar(dcid,XSBType.HLSB);                //dcid lsb
        sb.append("   dcid lsb dataPacket[9] = "+(int)dataPacket[6]);
        dataPacket[7] = getByteOfChar(dcid,XSBType.HMSB);                //dcid msb
        sb.append("   dcid msb dataPacket[10] = "+(int)dataPacket[7]);

        dataPacket[8] = getByteOfChar(len,XSBType.HLSB);                //dsize lsb
        sb.append("   dsize lsb dataPacket[11] = "+(int)dataPacket[8]);
        dataPacket[9] = getByteOfChar(len,XSBType.HMSB);                //dsize msb
        sb.append("   dsize msb dataPacket[12] = "+(int)dataPacket[9]);

        //CommandPacketPayload
        for(int i = 0; i < len; i++)
        {
            dataPacket[i+offset] = data[i];
            //Log.d(TAG,"payload dataPacket["+(i+offset)+"] = "+(int)dataPacket[(i+offset)]);
        }
        sb.append("   dataPacket length = "+dataPacket.length);
        Log.d(TAG, sb.toString());
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
        if(xsb == XSBType.HLSB)
        {
            return b[1];
        }
        else if(xsb == XSBType.HMSB)
        {
            return b[0];
        }
        else
        {
            return -1;
        }
    }

    static public byte getByteOfInt(int c, byte xsb)
    {
        byte b[] = new byte[4];

        b[0] = (byte) ((c & 0xFF000000) >> 24);
        b[1] = (byte) ((c & 0x00FF0000) >> 16);
        b[2] = (byte) ((c & 0xFF00FF00) >> 8);
        b[3] = (byte) (c & 0x000000FF);

        if(xsb == XSBType.HMSB)
        {
            return b[0];
        }
        else if(xsb == XSBType.HLSB)
        {
            return b[1];
        }
        else if(xsb == XSBType.LMSB)
        {
            return b[2];
        }
        else if(xsb == XSBType.LLSB)
        {
            return b[3];
        }
        else
        {
            return -1;
        }
    }
}
