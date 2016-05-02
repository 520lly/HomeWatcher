package com.hw.test.blueservice.protocol;

import java.util.ArrayList;
/**
 * Created by saic on 4/20/16.
 *
 *  uint8_t len1;
    uint8_t len2;
    uint8_t st;                     //indicate data source (Android or iOS)
    uint8_t cflag;                  //indicate packet Sequence
    uint16_t seq;
    uint16_t scid;
    uint16_t dcid;
    uint16_t dsize;
    uint16_t data[]
 *
 */
public class DataPacket {
    public byte len1;
    public byte len2;
    public byte st;
    public byte cflag;
    public int  seq;
    public int  scid;
    public int  dcid;
    public int  dsize;
    public ArrayList data;
}
