package com.ruin.psp;

import com.ruin.psp.models.*;
import java.util.ArrayList;
import java.util.HashMap;


public class PSP {

    static {
        try {
            String javaLibPath = System.getProperty("java.library.path");
            System.out.println(javaLibPath);
            System.loadLibrary("psp");
            System.out.println("Loaded client bridge library.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
        }
    }

    private PSPListener listener;

    public PSP(PSPListener listener){
        this.listener = listener;
    }

    synchronized public static native void greetSelf();

    synchronized public static native void startEmulator(String path);

    synchronized public static native void step();

    synchronized public static native void nstep(int key);

    synchronized public static native void shutdown();

    synchronized public static native void loadSaveState(int slot);

    synchronized public static native int readRAMU8(int address);

    synchronized public static native int readRAMU16(int address);

    synchronized public static native long readRAMU32(int address);

    synchronized public static native float readRAMU32Float(int address);

    synchronized public static native byte[] readRam(int address, int size);

    private HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();
    private ArrayList<Unit> friendlyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> itemUnits = new ArrayList<Unit>();

    private final int objectAddress = 0x01491080;
    // private final int objectAddress = 0x0144e440;
    private final int objectSize = 2136;

    public void onUpdate() {
        units.clear();
        friendlyUnits.clear();
        enemyUnits.clear();
        itemUnits.clear();
        for(int i = 0; i < 128; i++) {
        // int i = 12;
            byte[] unitRam = readRam(objectAddress + (i*objectSize), objectSize);

            // the unit exists if one of the two bytes at 0x854 are not 0
            if((unitRam[0x854] & 0xFF) != 0x0 || (unitRam[0x855] & 0xFF) != 0x0) {
                Unit unit = new Unit(unitRam);
                units.put(unit.getId(), unit);
                if(unit.isItem()) {
                    itemUnits.add(unit);
                }
                else if(unit.isFriendly()) {
                    friendlyUnits.add(unit);
                }
                else {
                    enemyUnits.add(unit);
                }
            }
        }

        System.out.println("=== Friendly Units ===");
        for(Unit cur : friendlyUnits) {
            System.out.println(cur.getName());
        }
        System.out.println();
        System.out.println("=== Enemy Units ===");
        for(Unit cur : enemyUnits) {
            System.out.println(cur.getName());
        }
        System.out.println();
        System.out.println("=== Item Units ===");
        for(Unit cur : itemUnits) {
            System.out.println(cur.getName());
        }
        System.out.println();
    }
}
