package com.ruin.psp.models;

import java.nio.*;
import java.util.Arrays;

public class Skill {
    private short id;
    private byte level;
    private short exp;

    private SkillType type;

    public Skill(byte[] data){
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.id = bb.getShort(0x0);
        this.level = bb.get(0x2);
        this.exp = bb.getShort(0x4);
    }

    public void setType(SkillType type) {
        this.type = type;
    }

    public int getID() {
        return (int) id & 0xFFFF;
    }

    public int getLevel() {
        return (int) level & 0xFF;
    }

    public int getExp() {
        return (int) exp & 0xFFFF;
    }


    public String getName() {
        return type.getName();
    }

    public String getDesc() {
        return type.getDesc();
    }

    public int getPower() {
        return type.getPower();
    }

    public int getSpType() {
        return type.getSpType();
    }

    public int getAttackType() {
        return type.getAttackType();
    }

    public int getShape() {
        return type.getShape();
    }

    public int getRange() {
        return type.getRange();
    }

    public int getRadius() {
        return type.getRadius();
    }

    public int getLimitUpper() {
        return type.getLimitUpper();
    }

    public int getLimitLower() {
        return type.getLimitLower();
    }

    public int getSpCost() {
        return type.getSpCost();
    }

    public int getManaCost() {
        return type.getManaCost();
    }

    public boolean isUnequip() {
        return type.isUnequip();
    }

    public int getEquipType() {
        return type.getEquipType();
    }

    public String toString() {
        return "SKILL: " + id + " " + level + " " + exp;
    }
}
