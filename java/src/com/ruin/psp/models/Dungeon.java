package com.ruin.psp.models;

import com.ruin.psp.*;
import java.nio.*;
import java.io.*;

public class Dungeon {

    public static final int dungeonOffset = 0x01574B3A;
    public static final int generatedDungeonOffset = 0x0012E742;

    private byte[] data;

    private String name;
    private byte maxFloors;
    private byte floor;
    private short lv;
    private byte size;
    private byte enemyAmount;
    private byte itemAmount;
    private byte groundType;
    private byte enemyType;
    private byte itemType;
    private byte prohibition;
    private int cost;

    // TODO: belongs in Title
    private byte exp;
    private byte bonus;

    public Dungeon(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.data = data;

        this.lv = bb.getShort(0x0E);
        this.cost = 0;
        this.floor = bb.get(0x10);
        this.maxFloors = bb.get(0x12);

        byte[] nameData = new byte[32];
        bb.position(0x14);
        bb.get(nameData);
        this.name = PSP.getStringAt(nameData);

        this.enemyAmount = bb.get(0x35);
        this.itemAmount = bb.get(0x36);
        this.size = bb.get(0x37);
        this.enemyType = bb.get(0x38);
        this.itemType = bb.get(0x39);
        this.groundType = bb.get(0x3D);
        this.prohibition = bb.get(0x3E);

        this.exp = bb.get(0x51);
        this.bonus = bb.get(0x52);
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


    public String getName() {
        return name;
    }

    public int getLevel() {
        return (int) lv & 0xFFFF;
    }

    public int getFloor() {
        return (int) floor & 0xFF;
    }

    public int getMaxFloors() {
        return (int) maxFloors & 0xFF;
    }

    public int getSize() {
        return (int) size & 0xFF;
    }

    public int getEnemyAmount() {
        return (int) enemyAmount & 0xFF;
    }

    public int getItemAmount() {
        return (int) itemAmount & 0xFF;
    }

    public int getGroundType() {
        return (int) groundType & 0xFF;
    }

    public int getEnemyType() {
        return (int) enemyType & 0xFF;
    }

    public int getItemType() {
        return (int) itemType & 0xFF;
    }

    public int getProhibition() {
        return (int) prohibition & 0xFF;
    }

    public int getCost() {
        return (int) cost;
    }

    public int getExp() {
        return (int) exp & 0xFF;
    }

    public int getBonus() {
        return (int) bonus & 0xFF;
    }
}
