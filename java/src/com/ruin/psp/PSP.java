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

    synchronized public static native void nstep(int keys);

    synchronized public static native void shutdown();

    synchronized public static native void loadSaveState(int slot);

    synchronized public static native int readRAMU8(int address);

    synchronized public static native int readRAMU16(int address);

    synchronized public static native long readRAMU32(int address);

    synchronized public static native float readRAMU32Float(int address);

    synchronized public static native byte[] readRam(int address, int size);

    public static int getTotalUnits() {
        // return readRAMU8(0x0014AC6B);
        return 128;
    }

    public static int getIslandMenuCursorPos() {
        return readRAMU16(0x0011E644);
    }

    public static int getBattleMenuCursorPos() {
        return readRAMU16(0x001BC0AC);
    }

    public static int getBattleUnitMenuCursorPos() {
        return readRAMU16(0x001BC0A8);
    }

    public static int getStatusMenuCursorPos() {
        int cursorPos = readRAMU16(0x0011E65C);
        int pageScroll = readRAMU16(0x0011E660);
        return cursorPos + pageScroll;
    }

    public static int getConfineMenuCursorPos() {
        int cursorPos = readRAMU16(0x0011CA28);
        int pageScroll = readRAMU16(0x0011CA2C);
        return cursorPos + pageScroll;
    }

    public List<Unit> getFriendlyUnits() {
        return Collections.unmodifiableList(friendlyUnits);
    }

    private HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();
    private ArrayList<Unit> friendlyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> neutralUnits = new ArrayList<Unit>();
    private ArrayList<Unit> itemUnits = new ArrayList<Unit>();

    private final int objectAddress = 0x01491080;
    // private final int objectAddress = 0x0144e440;
    private final int objectSize = 2136;

    public void onUpdate() {
        units.clear();
        friendlyUnits.clear();
        enemyUnits.clear();
        neutralUnits.clear();
        itemUnits.clear();
        for(int i = 0; i < getTotalUnits(); i++) {
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
                else if(unit.isNeutral()) {
                    neutralUnits.add(unit);
                }
                else {
                    enemyUnits.add(unit);
                }
            }
        }

        listUnits();

        System.out.println();
    }

    private void listUnits() {
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
        System.out.println("=== Neutral Units ===");
        for(Unit cur : neutralUnits) {
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
