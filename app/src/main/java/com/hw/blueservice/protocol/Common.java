package com.hw.blueservice.protocol;

/**
 * Created by saic on 3/23/16.
 */
public class Common {

    public static final int MAX_PACKET_DATA_LEN = 128;
    public static final int MAX_USER_DATA_LEN_DEFAULT = 1024;
    public static final int MAX_USER_CRCI_LEN_DEFAULT = 128;


    public static final int RES_PACKET_HEADER_LEN = 8;

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

    public static String[] statePacketType =
    {
            "PT_RESERVED",
                "PT_CTR",
                "PT_DATA",
                "PT_RES",
                "PT_UNKNOWN"
     };

    public class EPacketSequenceIndex
    {
        static final public byte PSI_START       =0x00;
        static final public byte PSI_CONTINUTION =0x01;
        static final public byte PSI_END         =0x02;
        static final public byte PSI_SINGLE      =0x03;
    }

    public class EParserPacket
    {
        //public static final int DataPacketHeaderLen =
    }

    /*
    uint8_t code;                                   //match with CmdPacket code
	uint8_t identifier;                             //match with CmdPacket identifier
	uint8_t len;
	uint8_t result;
	uint16_t scid;
	uint16_t dcid;                                   //APP RFCOMM Channel or HMI UNIX Domain Socket Channel
	uint8_t data[SppCRCPacketLenMax];
     */
    public class EResponsePacketFieldIndex
    {
        static final public byte  RspPacketFieldIndexCODE             = 0X00;                                   //match with CmdPacket code
        static final public byte  RspPacketFieldIndexIDENTIFIER       = 0X01;                             //match with CmdPacket identifier
        static final public byte  RspPacketFieldIndexLEN              = 0X02;
        static final public byte  RspPacketFieldIndexRESULT           = 0X03;
        static final public byte  RspPacketFieldIndexSCID             = 0X04;
        static final public byte  RspPacketFieldIndexDCID             = 0X05;                                   //APP RFCOMM Channel or HMI UNIX Domain Socket Channel
        static final public byte  RspPacketFieldIndexPAYLOAD          = 0X06;
        static final public byte  RspPacketFieldIndexFINISH           = 0X07;
        static final public byte  RspPacketFieldIndexDETECTEDBAD      = 0X08;

    }
    public static String[] stateResPacket =
            {
                    "CODE",
                    "INDENTIFIER",
                    "LEN",
                    "RESULT",
                    "SCID",
                    "DCID",
                    "PAYLOAD",
                    "FINISH",
                    "DETECTEDBAD",
            };


}
