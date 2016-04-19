package com.hw.blueservice.protocol;

import com.hw.blueservice.protocol.*;

/**
 * Created by saic on 4/19/16.
 */
public class ResPacket {
    public byte code;
    public byte identifier;
    public byte len;
    public byte result;
    public int scid;
    public int dcid;
    public byte data[];

}
