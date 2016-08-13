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

    public enum SpTypes {
        PHYSICAL,
        ENERGY,
        ELEMENTAL,
        NATURAL,
        SPACETIME,
        ALTERATION,
        HEALING;
        public int getID() {
            return ordinal();
        }
    }

    private int identifier;

    private int id;
    private int heldID;
    private int holdingID;

    private float x;
    private float y;
    private float z;
    private float velX;
    private float velY;
    private float velZ;

    private int currentHP;
    private int level;

    private String name;

    private short classType;

    private int statHP;
    private int statAtk;
    private int statDef;
    private int statInt;
    private int statRes;
    private int statSpd;

    private int numSkills;

    private int mana;

    private short[] sp;
    private short[] spMax;
    private short[] spAffinity;

    private boolean isItem;
    private boolean isBeingHeld;
    private int team;

    private float move;
    private float remainingMove;
    private int jump;
    private int remove;
    private int maxRemove;
    private boolean hasAttacked;
    private boolean isVisible;
    private boolean isOB;

    // if a unit is friendly, this points to the memory location of that unit's stats
    private int friendlyUnitOffset;

    private Title title;
    private ArrayList<Skill> skills;

    public Unit(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.isVisible = bb.get(0x17F) == 1;
        if(!this.isVisible) {
            return;
        }

        this.id = bb.getShort(0x842);

        this.x = bb.getFloat(0x64);
        this.y = bb.getFloat(0x68);
        this.z = bb.getFloat(0x6C);
        this.velX = bb.getFloat(0x80);
        this.velY = bb.getFloat(0x84);
        this.velZ = bb.getFloat(0x88);

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

        this.classType = bb.getShort(0x20);
        this.level = bb.getShort(0x500) & 0xFFFF;

        this.isItem = bb.getShort(0x14a) == 1;

        this.name = "";

        if(this.isFriendly()) {
            this.friendlyUnitOffset = bb.getInt(0x574) - 0x8800000;
            // 0x57c : pointer to held item's info
            byte[] unitStatusDat = PSP.readRam(this.friendlyUnitOffset, 704);

            UnitStatus unitStatus = new UnitStatus(unitStatusDat);
            loadFromUnitStatus(unitStatus);
        }
        else {
            // get 27 bytes at 0x2B8
            byte[] nameData = new byte[27];
            bb.position(0x2B8);
            bb.get(nameData);
            this.name = PSP.getStringAt(nameData);

            byte[] titleData = new byte[Title.titleSize];
            bb.position(0x53C);
            bb.get(titleData);
            this.title = new Title(titleData);

        }

        int itemOffset = bb.getInt(0x57c);
        if(itemOffset != 0) {
            this.heldID = PSP.readRAMU16((itemOffset - 0x8800000) + 0x842);
        } else {
            this.heldID = -1;
        }

        this.currentHP = bb.getInt(0x5AC);

        this.statHP = bb.getInt(0xEC);
        this.statAtk = bb.getInt(0x594);
        this.statDef = bb.getInt(0x594 + 4);
        this.statInt = bb.getInt(0x594 + 8);
        this.statRes = bb.getInt(0x594 + 12);
        this.statSpd = bb.getInt(0x594 + 16);

        this.isVisible = bb.get(0x17F) == 1;

        this.isOB = bb.get(0x184) == 1;

        this.isBeingHeld = bb.getInt(0x180) == 0;

        this.jump = bb.getInt(0x81a);
        this.move = bb.getFloat(0x600);
        this.remainingMove = bb.getFloat(0x604);
        this.remove = bb.getShort(0x5B2);

        this.hasAttacked = bb.getInt(0x848) == 1;

        float rotation = bb.getFloat(0x15C);
    }

    private void loadFromUnitStatus(UnitStatus unitStatus) {
        this.identifier = unitStatus.getIdentifier();
        
        this.mana = unitStatus.getMana();
        this.name = unitStatus.getName();
        this.numSkills = unitStatus.getNumSkills();

        this.title = unitStatus.getTitle();
        this.skills = unitStatus.getSkills();
        this.level = unitStatus.getLevel();

        this.sp = unitStatus.getSp();
        this.spMax = unitStatus.getMaxSp();
        this.spAffinity = unitStatus.getSpAffinity();

        this.currentHP = unitStatus.getCurrentHP();
        this.statHP = unitStatus.getHP();
        this.statAtk = unitStatus.getAtk();
        this.statDef = unitStatus.getDef();
        this.statInt = unitStatus.getInt();
        this.statRes = unitStatus.getRes();
        this.statSpd = unitStatus.getSpd();

        this.jump = unitStatus.getJump();
        this.move = unitStatus.getMove();
        this.data = unitStatus.getData();

        this.isItem = unitStatus.isItem();
        this.heldID = unitStatus.getHeldID();
    }

    public Unit(UnitStatus status) {
        loadFromUnitStatus(status);
    }

    public int getID() {
        return id;
    }

    public int getIdentifier() {
        return identifier;
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

    public int getCurrentHP() {
        return currentHP;
    }

    public int getHP() {
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

    public int getClassType() {
        return classType;
    }

    public float getMaxMove() {
        return move;
    }

    public float getRemainingMove() {
        return remainingMove;
    }

    public int getJump() {
        return jump;
    }

    public int getRemove() {
        return remove;
    }

    public int getMana() {
        return mana;
    }

    public short[] getSp() {
        return sp;
    }

    public short[] getMaxSp() {
        return spMax;
    }

    public short[] getSpAffinity() {
        return spAffinity;
    }

    public int getNumSkills() {
        return numSkills;
    }

    public int getHeldItemID() {
        return heldID;
    }

    public boolean isItem() {
        return isItem;
    }

    /**
     * True if the unit is an item or has remove remaining.
     */
    public boolean hasConfineTarget() {
        return isItem() || getRemove() > 0;
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

    public boolean isMarona() {
        // TODO: is this a flag?
        return this.name.equals("Marona");
    }

    public List<Skill> getSkills() {
        return Collections.unmodifiableList(this.skills);
    }

    public Title getTitle() {
        return title;
    }

    public boolean isOB() {
        return isOB;
    }
    public boolean isVisible() {
        return isVisible;
    }

    public boolean isDead() {
        return currentHP == 0;
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
        s = s + "Item: " + this.isItem + "\n";
        s = s + "Ally: " + this.isFriendly() + "\n";
        s = s + this.x + " " + this.y + " " + this.z + "\n";
        s = s + "Level: " + this.level + "\n";
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
