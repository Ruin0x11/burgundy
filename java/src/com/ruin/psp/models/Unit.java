package com.ruin.psp.models;

import java.nio.*;
import java.util.Arrays;

public class Unit {

    float x;
    float y;
    float z;

    int curHP;
    int maxHP;
    int lv;
    
    int curAtk;
    int curDef;
    int curInt;
    int curRes;
    int curSpd;

    public Unit(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        // System.out.println(Arrays.toString(data));

        this.x = bb.getFloat(0x84);
        this.y = bb.getFloat(0x88);
        this.z = bb.getFloat(0x8C);

        this.curHP = bb.getInt(0x110);
        this.curInt = bb.getInt(0x5bc);
        System.out.println(this.x + " " + this.y + " " + this.z);
        System.out.println(this.curHP + " " + this.curInt);
    }

    public void update(byte[] data) {
        
    }
}
