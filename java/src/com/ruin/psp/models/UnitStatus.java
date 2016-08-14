package com.ruin.psp.models;

import java.nio.*;
import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ruin.psp.PSP;

/** Represents a Unit in the status list, not just on a field. */
public class UnitStatus {
    public static final int unitStatusOffset = 0x01545EA0;
    public static final int itemUnitStatusOffset = 0x0155BEA0;
    public static final int unitStatusSize = 704;

    private String name;

    private byte[] data;

    private short level;
    private int currentHP;
    private int statHP;
    private int statAtk;
    private int statDef;
    private int statInt;
    private int statRes;
    private int statSpd;

    private int identifier;

    private int numSkills;

    private int mana;

    private short[] sp;
    private short[] spMax;
    private short[] spAffinity;

    private byte move;
    private byte jump;
    private byte steal;
    private byte remove;

    private boolean isItem;

    private short heldID;

    private Title title;
    private ArrayList<Skill> skills;

    public UnitStatus(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.identifier = bb.getInt(0x25B);

        byte[] nameData = new byte[23];
        bb.position(0x10);
        bb.get(nameData);
        this.name = PSP.getStringAt(nameData);

        this.numSkills = (int)bb.get(0x268) & 0xFF;

        byte[] titleData = new byte[Title.titleSize];
        bb.position(0x294);
        bb.get(titleData);
        this.title = new Title(titleData);

        this.skills = new ArrayList<Skill>();
        for(int i = 0; i < this.numSkills; i++) {
            int skillOffset = 0xF8 + (0x8 * i);
            byte[] skillData = new byte[8];
            bb.position(skillOffset);
            bb.get(skillData);
            Skill skill = new Skill(skillData);
            skills.add(skill);
        }

        this.mana = bb.getInt(0x94);

        this.currentHP = bb.getInt(0x44);
        this.statHP = bb.getInt(0x6C);
        this.statAtk = bb.getInt(0x74);
        this.statDef = bb.getInt(0x78);
        this.statInt = bb.getInt(0x7C);
        this.statRes = bb.getInt(0x80);
        this.statSpd = bb.getInt(0x84);

        this.level = bb.getShort(0x258);

        this.move = bb.get(0x274);
        this.jump = bb.get(0x275);
        this.steal = bb.get(0x286);

        this.sp = new short[7];
        this.spMax = new short[7];
        this.spAffinity = new short[7];

        for(int i = 0; i < 7; i++) {
            int offset = 0x1F7 + (0xC * i);
            byte[] spRam = new byte[0xC];
            bb.position(offset);
            bb.get(spRam);
            ByteBuffer spBuffer = ByteBuffer.wrap(spRam);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            short spDat = spBuffer.getShort(0x0);
            short spMaxDat = spBuffer.getShort(0x2);
            short spAffinityDat = spBuffer.getShort(0x4);
            this.sp[i] = spDat;
            this.spMax[i] = spMaxDat;
            this.spAffinity[i] = spAffinityDat;
        }

        this.applyTitle();

        this.isItem = bb.get(0x27D) == 1;

        this.heldID = bb.getShort(0x266);

        if(!this.isItem && this.heldID != -1) {
            this.applyItem();
        }
    }

    public int getTitleStat(int base, int percent) {
        int change = (percent - 100)/100;
        return base * percent;
    }

    public void applyItem() {
        int offset = itemUnitStatusOffset + (unitStatusSize * this.heldID);
        byte[] data = PSP.readRam(offset, unitStatusSize);
        UnitStatus status = new UnitStatus(data);

        this.statHP += status.getHP();
        this.statAtk += status.getAtk();
        this.statDef += status.getDef();
        this.statInt += status.getInt();
        this.statRes += status.getRes();
        this.statSpd += status.getSpd();
    }

    public void applyTitle() {
        this.move += this.title.getMove();
        this.statHP = getTitleStat(statHP, title.getHP());
        this.steal += this.title.getSteal();
    }


    public void dump() {
        try {
            FileOutputStream fos = new FileOutputStream("/home/prin/dump/" + this.name + "_detail.dump");
            fos.write(this.data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getData()
    {
        return data;
    }

    public String getName() {
        return name;
    }

    public int getIdentifier() {
        return identifier;
    }

    public int getLevel() {
        return (int) level & 0xFFFF;
    }

    public int getHeldID() {
        return (int) heldID & 0xFFFF;
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

    public float getMove() {
        return (int) move & 0xFF;
    }

    public int getJump() {
        return (int) jump & 0xFF;
    }

    public int getSteal() {
        return (int) steal & 0xFF;
    }

    public int getRemove() {
        return (int) remove & 0xFF;
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

    public Title getTitle() {
        return title;
    }

    public ArrayList<Skill> getSkills() {
        return skills;
    }

    public boolean isItem() {
        return isItem;
    }
}
