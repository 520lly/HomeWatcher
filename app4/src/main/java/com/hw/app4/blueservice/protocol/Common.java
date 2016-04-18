package com.hw.app4.blueservice.protocol;

/**
 * Created by saic on 3/23/16.
 */
public class Common {


    public static final int MAX_PACKET_DATA_LEN = 128;
    public static final int MAX_USER_DATA_LEN_DEFAULT = 1024;

    public enum EErrorCode
    {
        EC_SUCCESS,
        EC_WRONG_PARAM,
        EC_TIME_OUT,
        EC_NOT_READY,
        EC_UNKNOWN
    }

    public class EProtocolName
    {
        //HMI to SPP
        static final public byte PN_RESERVED              = 0x00;
        static final public byte PN_A2S_CREATE_RFCOMMN_CH = 0x01;


    }

    public class EPacketType
    {
        static final public byte PT_RESERVED = 0x00;
        static final public byte PT_CTR      = 0x01;
        static final public byte PT_DATA     = 0x02;
        static final public byte PT_RES      = 0x03;
        static final public byte PT_UNKNOWN  = 0x04;
    }



}
