package com.ruin.psp.models;

import java.nio.*;
import java.util.Arrays;

public class Skill {
    private short id;
    private byte level;
    private short exp;

    public Skill(byte[] data){
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.id = bb.getShort(0x0);
        this.level = bb.get(0x2);
        this.exp = bb.getShort(0x4);
    }

    public short getID() {
        return id;
    }

    public byte getLevel() {
        return level;
    }

    public short getExp() {
        return exp;
    }
}
