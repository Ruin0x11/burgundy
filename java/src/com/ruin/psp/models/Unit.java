package com.ruin.psp.models;

import java.nio.*;
import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ruin.psp.PSP;

public class Unit {
    private byte[] data;

    public static final int TEAM_FRIENDLY = 0;
    public static final int TEAM_ENEMY = 1;
    public static final int TEAM_NEUTRAL = 2;

    private int id;
    private int heldId;

    private float x;
    private float y;
    private float z;
    private float velX;
    private float velY;
    private float velZ;

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

    private int numSkills;

    private int mana;

    private boolean isItem;
    private boolean isBeingHeld;
    private int team;

    private float maxMove;
    private float remainingMove;
    private int remove;
    private boolean hasAttacked;

    // if a unit is confined (friendly), this points to the memory location of that unit's stats
    private int friendlyUnitOffset;

    private ArrayList<Skill> skills;

    public Unit(byte[] data, int id) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.id = id;

        this.x = bb.getFloat(0x64);
        this.y = bb.getFloat(0x68);
        this.z = bb.getFloat(0x6C);
        this.velX = bb.getFloat(0x80);
        this.velY = bb.getFloat(0x84);
        this.velZ = bb.getFloat(0x88);

        this.currentHP = bb.getInt(0x5AC);

        short teamFlagA = bb.getShort(0x142);
        short teamFlagB = bb.getShort(0x144);
        short teamFlagC = bb.getShort(0x146);

        if(teamFlagA == 2) {
            this.team = TEAM_NEUTRAL;
        } else if(teamFlagA == 1) {
            this.team = TEAM_ENEMY;
        } else {
            this.team = TEAM_FRIENDLY;
        }

        this.unitType = bb.getShort(0x20);
        this.lv = bb.getShort(0x500);

        this.isItem = bb.getShort(0x14a) == 1;

        this.name = "";

        if(this.isFriendly()) {
            this.friendlyUnitOffset = bb.getInt(0x574) - 0x8800000;
            // 0x58c : pointer to held item's info
            // 0x594 : pointer to a skill
            // 0x10 : pointer to something
            // 0x1d8 : pointer to something

            if(this.friendlyUnitOffset != 0) {
                this.mana = PSP.readRAMU32(this.friendlyUnitOffset + 148);

                byte[] nameData = PSP.readRam(this.friendlyUnitOffset + 16, 24);
                this.name = PSP.getStringAt(nameData);

                this.numSkills = PSP.readRAMU16(this.friendlyUnitOffset + 616);

                this.skills = new ArrayList<Skill>();
                for(int i = 0; i < this.numSkills; i++) {
                    int skillOffset = 0x100 + (0x8 * i);
                    byte[] skillData = PSP.readRam(this.friendlyUnitOffset + skillOffset, 8);
                    Skill skill = new Skill(skillData);
                    skills.add(skill);
                }
            }

            int itemOffset = bb.getInt(0x58c);
            if(itemOffset != 0) {
                // this.numSkills += PSP.readRAMU16(itemOffset + 616);
            }
        }
        else {
            // get 16 bytes at 0x2C8
            byte[] nameData = new byte[16];
            bb.position(0x2B8);
            bb.get(nameData);

            try {
                this.name = new String(nameData, "SHIFT-JIS");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(this.isItem()) {
            // 0x56c : item details pointer - 704 bytes

            // inside:
            // 0xfa : 32 slots for 8-byte skills - 256 bytes
            // u2 level, u4 exp, u2 identifier
        }

        this.statHP = bb.getInt(0xEC);
        this.statAtk = bb.getInt(0x594);
        this.statDef = bb.getInt(0x594 + 4);
        this.statInt = bb.getInt(0x594 + 8);
        this.statRes = bb.getInt(0x594 + 12);
        this.statSpd = bb.getInt(0x594 + 16);

        this.isBeingHeld = bb.getInt(0x180) == 0;

        this.maxMove = bb.getFloat(0x600);
        this.remainingMove = bb.getFloat(0x604);

        this.hasAttacked = bb.getInt(0x848) == 1;

        float rotation = bb.getFloat(0x15C);
    }

    public void update(byte[] data) {

    }

    public int getID() {
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

    public float getVelX() {
        return velX;
    }

    public float getVelY() {
        return velY;
    }

    public float getVelZ() {
        return velZ;
    }

    public int getCurrentHp() {
        return currentHP;
    }

    public int getMaxHp() {
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

    public float getMaxMove() {
        return maxMove;
    }

    public float getRemainingMove() {
        return remainingMove;
    }

    public int getMana() {
        return mana;
    }

    public int getNumSkills() {
        return numSkills;
    }

    public boolean isItem() {
        return isItem;
    }

    public boolean isBeingHeld() {
        return isBeingHeld;
    }

    public boolean hasAttacked() {
        return hasAttacked;
    }

    public boolean isFriendly() {
        return this.team == TEAM_FRIENDLY;
    }

    public boolean isEnemy() {
        return this.team == TEAM_ENEMY;
    }

    public boolean isNeutral() {
        return this.team == TEAM_NEUTRAL;
    }

    public List<Skill> getSkills() {
        return Collections.unmodifiableList(this.skills);
    }

    public void dump() {
        try {
            FileOutputStream fos = new FileOutputStream("/home/prin/dump/" + this.name + ".dump");
            fos.write(this.data);
            fos.close();
            if(this.friendlyUnitOffset != 0) {
                byte[] detailData = PSP.readRam(this.friendlyUnitOffset, 704);

                fos = new FileOutputStream("/home/prin/dump/" + this.name + "_detail.dump");
                fos.write(detailData);
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
