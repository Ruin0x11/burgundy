package com.ruin.psp.models;

import java.io.*;
import java.nio.*;
import com.ruin.psp.PSP;
import java.util.Arrays;

public class SkillType {
    // 256 bytes
    // starting offset: 0x010797D4

    byte[] data;

    private short id;
    private short spCost;

    private String name;
    private String desc;

    private byte power;
    private byte range;
    private byte radius;
    private byte limitUpper;
    private byte limitLower;
    private byte shape;
    private byte spType;
    private byte attackType;

    private int manaCost;

    public SkillType(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.id = bb.getShort(0x04);
        this.spCost = bb.getShort(0x08);

        byte[] stringData = Arrays.copyOfRange(data, 0x0C, 0x0C + 16);
        this.name = PSP.getStringAt(stringData);
        stringData = Arrays.copyOfRange(data, 0x22, 0x22 + 32);
        // there are some special characters that need to be stripped
        this.desc = PSP.getStringAt(stringData);

        this.power = bb.get(0x6B);
        this.spType = bb.get(0x70);
        this.shape = bb.get(0x72);
        this.attackType = bb.get(0x74);
        this.range = bb.get(0x75);
        this.radius = bb.get(0x76);
        this.limitUpper = bb.get(0x77);
        this.limitLower = bb.get(0x78);

        this.manaCost = bb.getInt(0x00);
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public byte getPower() {
        return power;
    }

    public byte getSpType() {
        return spType;
    }

    public byte getAttackType() {
        return attackType;
    }

    public byte getShape() {
        return shape;
    }

    public byte getRange() {
        return range;
    }

    public byte getRadius() {
        return radius;
    }

    public byte getLimitUpper() {
        return limitUpper;
    }

    public byte getLimitLower() {
        return limitLower;
    }

    public short getSpCost() {
        return spCost;
    }

    public int getManaCost() {
        return manaCost;
    }

    public void dump() {
        try {
            FileOutputStream fos = new FileOutputStream("/home/prin/dump/" + this.name + ".dump");
            fos.write(this.data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        String s = "";
        s = s + "Name: " + this.name + "[" + this.id + "]\n";
        s = s + "Desc: " + this.desc + "\n";
        s = s + "Range: " + this.range + "\n";
        s = s + "Radius: " + this.radius + "\n";
        s = s + "Upper: " + this.limitUpper + "\n";
        s = s + "Lower: " + this.limitLower + "\n";
        return s;
    }
}
