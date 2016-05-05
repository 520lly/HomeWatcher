package com.hw.blueservice.protocol;

/**
 * Created by saic on 5/3/16.
 *
 *  public byte packetType;
    public int packetLen;           //4bytes
    public byte headerChk;
    public StringBuffer data;
    public byte dataChk;

 */

public class BasicPacket {

   public class parserIndex
    {
        static final public byte  parserIndexTYPE            = 0X00;
        static final public byte  parserIndexLEN0            = 0X01;
        static final public byte  parserIndexLEN1            = 0X02;
        static final public byte  parserIndexLEN2            = 0X03;
        static final public byte  parserIndexLEN3            = 0X04;
        static final public byte  parserIndexHEADERCHK       = 0X05;
        static final public byte  parserIndexDATA            = 0X06;
        static final public byte  parserIndexDATACHK         = 0X07;
        static final public byte  parserIndexFINISH          = 0X08;
        static final public byte  parserIndexBADDETECTED     = 0X09;
    }

    public static String[] stateBasicPacket =
            {
                    "TYPE",
                    "LEN0",
                    "LEN1",
                    "LEN2",
                    "LEN3",
                    "HEADER_CHK",
                    "DATA",
                    "DATA_CHK",
                    "FINISH",
                    "DETECTEDBAD",
            };

    static public final int basicPacketHeaderLen = 7;


    public byte packetType;
    public int packetLen;           //4bytes
    public byte headerChk;
    public StringBuffer data;
    public byte dataChk;
}
