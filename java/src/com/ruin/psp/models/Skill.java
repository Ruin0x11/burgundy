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

    public int getID() {
        return (int) id & 0xFFFF;
    }

    public int getLevel() {
        return (int) level & 0xFF;
    }

    public int getExp() {
        return (int) exp & 0xFFFF;
    }

    public String toString() {
        return "SKILL: " + id + " " + level + " " + exp;
    }
}
