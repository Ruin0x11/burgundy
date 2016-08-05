package com.ruin.psp;

import com.ruin.psp.models.*;
import java.util.Collections;
import java.util.List;
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

    public PSP() {}

    public PSP(PSPListener listener){
        this.listener = listener;
    }

    synchronized public static native void greetSelf();

    synchronized public static native void startEmulator(String path);

    synchronized public static native void step();

    synchronized public static native void nstep(int keys, float analogX, float analogY);

    synchronized public static native void shutdown();

    // synchronized public static native void loadSaveState(int slot);
    synchronized public static native void loadSaveState(String filename);

    synchronized public static native void setFramelimit(boolean framelimit);

    synchronized public static native int readRAMU8(int address);

    synchronized public static native int readRAMU16(int address);

    synchronized public static native int readRAMU32(int address);

    synchronized public static native float readRAMU32Float(int address);

    synchronized public static native byte[] readRam(int address, int size);

    /**
     * Returns the number of frames left during the transition from one unit's turn to the next.
     * If 0, there is no transition.
     */
    private static int transitionFrames() {
        return readRAMU16(0x001BC0BC);
    }

    /**
     * Returns the current menu layer number.
     * If 0, there are no menus open, but the player still might not be in control.
     */
    private static int getMenuLayer() {
        return readRAMU16(0x001BC00C);
    }

    /**
     * Returns true if the cursor can be moved on the map.
     */
    public static boolean canMoveInMap() {
        return (getMenuLayer() == 0) && (transitionFrames() == 0);
    }

    /**
     * When selecting a position for a unit to move to, returns true if the unit can be moved there.
     * Intended to have no effect elsewhere.
     */
    public static boolean canMove() {
        // 0x1B if not
        return readRAMU16(0x0012e964) == 0x11;
    }

    /**
     * When selecting something for a unit to attack, returns true if it can be attacked.
     * Intended to have no effect elsewhere.
     */
    public static boolean canAttack() {
        // 0x1C if not
        return readRAMU16(0x0012e964) == 0x12;
    }

    public static float getPlayerX() {
        return readRAMU32Float(0x0012E89C);
    }

    public static float getPlayerY() {
        return readRAMU32Float(0x0012E8A0);
    }

    public static float getPlayerZ() {
        return readRAMU32Float(0x0012E8A4);
    }

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

    public static int getBattleAttackMenuCursorPos() {
        return readRAMU16(0x0014AC94);
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

    public static long getBol() {
        return readRAMU32(0x1545E80);
    }

    public List<Unit> getFriendlyUnits() {
        return Collections.unmodifiableList(friendlyUnits);
    }

    public List<Unit> getEnemyUnits() {
        return Collections.unmodifiableList(enemyUnits);
    }

    public List<Unit> getItemUnits() {
        return Collections.unmodifiableList(itemUnits);
    }

    private HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();
    private ArrayList<Unit> friendlyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> neutralUnits = new ArrayList<Unit>();
    private ArrayList<Unit> itemUnits = new ArrayList<Unit>();
    private ArrayList<Unit> deadUnits = new ArrayList<Unit>();

    private final int objectAddress = 0x01491080;
    // private final int objectAddress = 0x0144e440;
    private final int objectSize = 2136;

    public void onUpdate() {
        units.clear();
        friendlyUnits.clear();
        enemyUnits.clear();
        neutralUnits.clear();
        itemUnits.clear();
        deadUnits.clear();
        for(int i = 0; i < getTotalUnits(); i++) {
            byte[] unitRam = readRam(objectAddress + (i*objectSize), objectSize);

            // the unit exists if one of the two bytes at 0x854 are not 0
            if((unitRam[0x854] & 0xFF) != 0x0 || (unitRam[0x855] & 0xFF) != 0x0) {
                Unit unit = new Unit(unitRam);
                units.put(unit.getId(), unit);
                if(unit.getCurrentHp() == 0) {
                    deadUnits.add(unit);
                }
                else if(unit.isItem()) {
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
    }

    public void listUnits() {
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
