package com.hw.blueservice.protocol;

import com.hw.blueservice.protocol.*;

import java.util.ArrayList;

/**
 * Created by saic on 4/19/16.
 *
 *  uint8_t code;                                   //match with CmdPacket code
    uint8_t identifier;                             //match with CmdPacket identifier
    uint8_t len;
    uint8_t result;
    uint16_t scid;
    uint16_t dcid;                                   //APP RFCOMM Channel or HMI UNIX Domain Socket Channel
    uint8_t data[];

 */
public class ResPacket {
    public byte code;
    public byte identifier;
    public byte len;
    public byte result;
    public byte type;
    public byte scidMsb;
    public byte scidLsb;
    public byte dcidMsb;
    public byte dcidLsb;
    public ArrayList data;

}
