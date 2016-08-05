package com.ruin.psp.models;

import java.nio.*;
import java.io.*;
import java.util.Arrays;
import com.ruin.psp.PSP;

public class Unit {

    public static final int TEAM_FRIENDLY = 0;
    public static final int TEAM_ENEMY = 1;
    public static final int TEAM_NEUTRAL = 2;
    public static final int TEAM_MARONA = 6;

    private short id;

    private float x;
    private float y;
    private float z;

    private int currentHP;
    private short lv;

    private String name;

    private short unitType;

    private int statHP;
    private int statAtk;
    private int statDef;
    private int statInt;
    private int statRes;
    private int statSpd;

    private int team;

    private boolean isItem;
    private short allyId;

    // if a unit is confined (friendly), this points to the memory location of that unit's stats
    private int friendlyUnitOffset;

    public Unit(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.x = bb.getFloat(0x74);
        this.y = bb.getFloat(0x78);
        this.z = bb.getFloat(0x7C);

        this.currentHP = bb.getInt(0x5BC);

        this.team = bb.getInt(0x3C);
        System.out.println(String.format("%8s", Integer.toBinaryString(bb.getInt(0x82a) & 0xFF)).replace(' ', '0'));
        // 0x82a : bitfield
        this.unitType = bb.getShort(0x30);
        this.lv = bb.getShort(0x510);

        this.isItem = bb.getShort(0x15a) == 1;
        // this.isPerson = bb.getShort(0x166) == 1;
        this.allyId = bb.getShort(0x854);

        byte[] nameData = new byte[16];
        if(this.isFriendly()) {
            this.friendlyUnitOffset = bb.getInt(0x584) - 0x8800000;
            // 0x58c : pointer to second unit info
            // 0x584 : pointer to held item's info
            // 0x594 : pointer to a skill
            // 0x10 : pointer to something
            // 0x1d8 : pointer to something
            System.out.println(friendlyUnitOffset);

            if(this.friendlyUnitOffset != 0) {
                nameData = PSP.readRam(this.friendlyUnitOffset + 16, 24);
                System.out.println(Arrays.toString(nameData));

                // split the 0-terminated string
                int stringEnd = 0;
                for(int i = 0; i < nameData.length; i++) {
                    if((nameData[i] & 0xFF) == 0) {
                        stringEnd = i;
                        break;
                    }
                }
                nameData = Arrays.copyOfRange(nameData, 0, stringEnd);
            }
        }
        else {
            // get 16 bytes at 0x2C8
            bb.position(0x2C8);
            bb.get(nameData);
        }

        try {
            this.name = new String(nameData, "ASCII");
        } catch (Exception e) {
            this.name = "u";
            e.printStackTrace();
        }
        this.name = this.name.trim();

        this.statHP = bb.getInt(0xFC);
        this.statAtk = bb.getInt(0x5A4);
        this.statDef = bb.getInt(0x5A4 + 4);
        this.statInt = bb.getInt(0x5A4 + 8);
        this.statRes = bb.getInt(0x5A4 + 12);
        this.statSpd = bb.getInt(0x5A4 + 16);

        this.id = bb.getShort(0x852);

        System.out.println(this.toString());

        try {
            FileOutputStream fos = new FileOutputStream("/home/prin/dump/" + this.name + ".dump");
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(byte[] data) {

    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return statHP;
    }

    public int getAtk() {
        return statAtk;
    }

    public int getDef() {
        return statDef;
    }

    public int getInt() {
        return statInt;
    }

    public int getRes() {
        return statRes;
    }

    public int getSpd() {
        return statSpd;
    }

    public boolean isItem() {
        return isItem;
    }

    public boolean isFriendly() {
        return !isItem && allyId != -1;
    }

    public boolean isEnemy() {
        return !isFriendly();
    }

    public String toString() {
        String s = "";
        s = s + "Name: " + this.name + "[" + this.id + "]\n";
        s = s + "Item: " + this.isItem + "\n";
        s = s + "Ally: " + this.isFriendly() + "\n";
        s = s + this.x + " " + this.y + " " + this.z + "\n";
        s = s + "Lv: " + this.lv + "\n";
        s = s + "CHP: " + this.currentHP + "\n";
        s = s + "MHP: " + this.statHP + "\n";
        s = s + "Atk: " + this.statAtk + "\n";
        s = s + "Def: " + this.statDef + "\n";
        s = s + "Int: " + this.statInt + "\n";
        s = s + "Res: " + this.statRes + "\n";
        s = s + "Spd: " + this.statSpd + "\n";
        return s;
    }
}
