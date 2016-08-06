package com.ruin.psp.models;

import java.nio.*;
import com.ruin.psp.PSP;

public class Skill {
    // 128 bytes
    // starting offset: 0x010797D4

    private short spCost;

    private String name;
    private String desc;

    private short range;
    private short radius;
    private short limitUpper;
    private short limitLower;

    private int manaCost;

    private short type;

    public Skill(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.spCost = bb.getShort(0x0C);

        this.name = PSP.getStringAt(bb, 0x10, 19);
        this.desc = PSP.getStringAt(bb, 0x29, 32);

        this.range = bb.get(0x79);
        this.radius = bb.get(0x80);
        this.limitUpper = bb.get(0x81);
        this.limitLower = bb.get(0x82);

        this.manaCost = bb.getInt(0x04);
    }
}
