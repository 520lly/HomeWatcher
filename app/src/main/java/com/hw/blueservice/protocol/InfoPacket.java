package com.hw.blueservice.protocol;

/**
 * Created by saic on 4/23/16.
 *
 * uint8_t code;                                   //match with CmdPacket code
   uint8_t len;                                    //Payload length
   uint8_t count;                                  //Info Items count
   uint8_t data[1];                                //Info Items
 */
public class InfoPacket {
    public byte code;
    public byte len;
    public byte count;
    public byte[] data;
}
