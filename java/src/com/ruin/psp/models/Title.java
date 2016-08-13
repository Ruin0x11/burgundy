package com.ruin.psp.models;

import java.nio.*;
import java.io.*;
import com.ruin.psp.*;

public class Title {
    public static final int titleOffset = 0x1573544;
    public static final int titleSize = 44;

    private String name;

    private byte exp;
    private byte bonus;

    private byte rank;

    private byte resist;

    private byte guard;
    private byte steal;
    private byte move;

    private byte addHP;
    private byte addAtk;
    private byte addDef;
    private byte addInt;
    private byte addRes;
    private byte addSpd;

    private byte addPhysical;
    private byte addEnergy;
    private byte addElemental;
    private byte addNatural;
    private byte addSpacetime;
    private byte addAlteration;
    private byte addHealing;

    public Title(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        byte[] nameData = new byte[10];
        bb.position(0x02);
        bb.get(nameData);
        this.name = PSP.getStringAt(nameData);

        this.exp = bb.get(0xB);
        this.bonus = bb.get(0xC);
        this.rank = bb.get(0xD);
        this.resist = bb.get(0x10);
        this.guard = bb.get(0x11);
        this.steal = bb.get(0x12);
        this.move = bb.get(0x13);

        this.addHP = bb.get(0x14);
        this.addAtk = bb.get(0x15);
        this.addDef = bb.get(0x16);
        this.addInt = bb.get(0x17);
        this.addRes = bb.get(0x18);
        this.addSpd = bb.get(0x19);

        this.addPhysical = bb.get(0x1A);
        this.addEnergy = bb.get(0x1B);
        this.addElemental = bb.get(0x1C);
        this.addNatural = bb.get(0x1D);
        this.addSpacetime = bb.get(0x1E);
        this.addAlteration = bb.get(0x1F);
        this.addHealing = bb.get(0x20);
    }

    public String getName() {
        return this.name;
    }

    public int getHP() {
        return (int) addHP & 0xFF;
    }

    public int getMove() {
        return (int) move & 0xFF;
    }

    public int getExp() {
        return (int) exp & 0xFF;
    }

    public int getBonus() {
        return (int) bonus & 0xFF;
    }

    public int getRank() {
        return (int) rank & 0xFF;
    }
}
