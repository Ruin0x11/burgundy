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

    private boolean unequip;

    public enum EquipType {
        ANY,
        ARMED,
        UNARMED,
        COMBO,
        PASSIVE;
        public int getID() {
            return ordinal();
        }
    }

    private int equipType;

    private int manaCost;

    public SkillType(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.id = bb.getShort(0x00);
        this.spCost = bb.getShort(0x04);

        byte[] stringData = Arrays.copyOfRange(data, 0x08, 0x08 + 16);
        this.name = PSP.getStringAt(stringData);
        stringData = Arrays.copyOfRange(data, 0x1D, 0x1D + 32);
        // there are some special characters that need to be stripped
        this.desc = PSP.getStringAt(stringData);

        this.power = bb.get(0x6B);
        this.spType = bb.get(0x6C);
        // this.baseSkill = bb.get(0x69) == 1;
        this.shape = bb.get(0x6E);
        this.attackType = bb.get(0x70);
        this.range = bb.get(0x71);
        this.radius = bb.get(0x72);
        this.limitUpper = bb.get(0x73);
        this.limitLower = bb.get(0x74);

        this.manaCost = bb.getInt(0x00);

        byte[] unequipChar = new byte[] {(byte)0x87, (byte)0x66};
        this.unequip = PSP.find(stringData, unequipChar) != -1;

        int equipTypeData = bb.get(0x7A);
        if(equipTypeData == 6 || equipTypeData == 70) {
            this.equipType = EquipType.COMBO.getID();
        } else if(equipTypeData == 81){
            this.equipType = EquipType.UNARMED.getID();
        } else if(equipTypeData == 0) {
            this.equipType = EquipType.PASSIVE.getID();
        } else if(equipTypeData == 15) {
            this.equipType = EquipType.ARMED.getID();
        } else {
            this.equipType = EquipType.ANY.getID();
        }
    }

    public int getID() {
        return (int) id & 0xFFFF;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public int getPower() {
        return (int) power & 0xFF;
    }

    public int getSpType() {
        return (int) spType & 0xFF;
    }

    public int getAttackType() {
        return (int) attackType & 0xFF;
    }

    public int getShape() {
        return (int) shape & 0xFF;
    }

    public int getRange() {
        return (int) range & 0xFF;
    }

    public int getRadius() {
        return (int) radius & 0xFF;
    }

    public int getLimitUpper() {
        return (int) limitUpper & 0xFF;
    }

    public int getLimitLower() {
        return (int) limitLower & 0xFF;
    }

    public int getSpCost() {
        return (int) spCost & 0xFFFF;
    }

    public int getManaCost() {
        return manaCost;
    }

    public boolean isUnequip() {
        return unequip;
    }

    public int getEquipType() {
        return equipType;
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
