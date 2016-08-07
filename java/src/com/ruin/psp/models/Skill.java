package com.ruin.psp.models;

public class Skill {
    private int id;
    private int exp;
    private int level;

    public Skill(int id, int level, int exp){
        this.id = id;
        this.level = level;
        this.exp = exp;
    }
}
