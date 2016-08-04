package com.ruin.psp.models;

import java.nio.*;
import java.util.Arrays;

public class Unit {

    float x;
    float y;
    float z;

    int curHP;
    short lv;

    String name;

    short type;
    
    int statHP;
    int statAtk;
    int statDef;
    int statInt;
    int statRes;
    int statSpd;

    int team;

    // if a unit is confined, this points to the memory location of that unit's stats
    int friendlyUnitOffset;

    public Unit(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        // System.out.println(Arrays.toString(data));

        this.x = bb.getFloat(0x84);
        this.y = bb.getFloat(0x88);
        this.z = bb.getFloat(0x8C);

        this.curHP = bb.getInt(0x5cc);

        this.team = 0;
        this.type = bb.getShort(0x40);

        // only if non-friendly
        this.lv = bb.getShort(0x520);
        byte[] b = new byte[16];
        bb.position(0x2d8);
        bb.get(b);
        try {

        this.name = new String(b, "ASCII");
        } catch (Exception e) {

        }

        // only if friendly
        this.friendlyUnitOffset = bb.getInt(0x590)

        this.statHP = bb.getInt(0x5ac);
        this.statAtk = bb.getInt(0x5b4);
        this.statDef = bb.getInt(0x5b4 + 4);
        this.statInt = bb.getInt(0x5b4 + 8);
        this.statRes = bb.getInt(0x5b4 + 12);
        this.statSpd = bb.getInt(0x5b4 + 16);
        System.out.println(this.name + " (Team: " + this.team + ")" );
        System.out.println(this.x + " " + this.y + " " + this.z);
        System.out.println(this.lv + " " + this.curHP + " " + this.statHP + " " + this.statAtk + " " + this.statDef + " " + this.statInt + " " + this.statRes + " " + this.statSpd);
        System.out.println();
    }

    public void update(byte[] data) {
        
    }
}
