package com.ruin.psp;

import com.ruin.psp.models.*;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.*;
import java.io.*;
import java.util.Arrays;
import java.lang.Math;

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

    synchronized public static native void saveSaveState(String filename);

    synchronized public static native void setFramelimit(boolean framelimit);

    synchronized public static native int readRAMU8(int address);

    synchronized public static native int readRAMU16(int address);

    synchronized public static native int readRAMU32(int address);

    synchronized public static native float readRAMU32Float(int address);

    synchronized public static native byte[] readRam(int address, int size);

    /**
     * Returns the number of units counted as summoned.
     */
    public int summonedUnits() {
        return readRAMU16(0x0012F384);
    }

    public Unit getUnit(int unitID) {
        return units.get(unitID);
    }

    public Unit getActiveUnit() {
        int activeID = readRAMU16(0x0012F388);
        return getUnit(activeID);
    }

    private static int getDialogType() {
        return readRAMU16(0x001bc010);
    }

    public static boolean isStageClear() {
        int type = getDialogType();
        return type == 0x07D0 || type == 0x07D1;
    }

    /**
     * Returns true if the "Stage ----" banner is active.
     */
    private static boolean isStageBannerUp() {
        return readRAMU16(0x0012e920) == 1;
    }

    /**
     * Returns true if a special level banner is active.
     */
    public static boolean isSpecialStageScreenUp() {
        return getDialogType() == 0x7E1;
    }

    public static boolean hasStageStarted() {
        return getDialogType() == 0x271A;
    }

    /**
     * Indicates whether or not a non-friendly unit is acting.
     * Always 0x0f during the non-friendly unit's turn.
     */
    private static int transitionFromEnemyFrames() {
        return readRAMU16(0x001BC0C8);
    }

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
        return readRAMU8(0x001BC054);
    }

    /**
     * Returns true if the cursor can be moved on the map.
     */
    public static boolean canMoveInMap() {
        int menuLayer = getMenuLayer();
        int transitionFrames = transitionFrames();
        int enemyFrames = transitionFromEnemyFrames();
        boolean stageBanner = isStageBannerUp();
        // System.out.println(menuLayer + " " + transitionFrames + " " + enemyFrames + " " + stageBanner + " " + getDialogType());
        // System.out.println(isStageClear());
        
        return menuLayer == 0 &&
            (transitionFrames == 0 || transitionFrames > 0x40) &&
            enemyFrames != 0x0f &&
            !stageBanner;
    }

    public static void printFlags() {
        System.out.println(getMenuLayer() + " " + transitionFrames() + " " + transitionFromEnemyFrames());
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

    /**
     * When selecting a confine target, returns true if it can be confined to.
     * Intended to have no effect elsewhere.
     */
    public static boolean canConfine() {
        // 0x1C if not
        return readRAMU16(0x0012e964) == 0x18;
    }

    public static boolean hasAttacked() {
        return ((readRAMU16(0x0012e94c) >> 1) & 1) == 1;
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
        int friendlyUnits = readRAMU16(0x0012F37C);
        int friendlyUnitsAndItems = readRAMU16(0x0012F384);
        int otherUnits = readRAMU16(0x0012F388);
        // including marona
        return (friendlyUnitsAndItems - friendlyUnits) + otherUnits + 1;
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
        int cursorPos = readRAMU16(0x0014AC94);
        int pageScroll = readRAMU16(0x0014AC98);
        return cursorPos + pageScroll;
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

    public List<Unit> getUnits() {
        return Collections.unmodifiableList(new ArrayList<Unit>(units.values()));
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

    private static final int numSkills = 577;
    private static final int skillOffset = 0x010797DC;

    public void loadSkillTypes() {
        skillTypes.clear();
        for(int i = 0; i < numSkills; i++) {
            byte[] skillTypesRam = readRam(skillOffset + (i*128), 128);
            SkillType skill = new SkillType(skillTypesRam);
            skillTypes.put(skill.getID(), skill);
        }
    }

    /**
     * Returns the units beneath the cursor, ordered from lowest to highest.
     * @return <doc>
     */
    public List<Unit> getUnitsUnderCursor() {
        ArrayList<Unit> cursorUnits = new ArrayList<Unit>();
        int numberOfUnits = readRAMU16(0x0012F49C);
        for(int i = 0; i < numberOfUnits; i++) {
            // deal with little endian ordering of pairs of shorts
            int id = readRAMU16(0x0012F398 + i*0x02);
            cursorUnits.add(units.get(id));
        }
        return Collections.unmodifiableList(cursorUnits);
    }

    public int getSelectedUnitIndex() {
        return readRAMU16(0x0012F498);
    }

    public Unit getSelectedUnit() {
        List<Unit> cursorUnits = getUnitsUnderCursor();
        int index = getSelectedUnitIndex();
        if(index >= cursorUnits.size()) {
            return null;
        }
        return cursorUnits.get(index);
    }

    private HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();
    private ArrayList<Unit> friendlyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> enemyUnits = new ArrayList<Unit>();
    private ArrayList<Unit> neutralUnits = new ArrayList<Unit>();
    private ArrayList<Unit> itemUnits = new ArrayList<Unit>();
    private ArrayList<Unit> deadUnits = new ArrayList<Unit>();

    private HashMap<Integer, SkillType> skillTypes = new HashMap<Integer, SkillType>();

    public SkillType getSkillType(int skillID) { return skillTypes.get(skillID); }
    public Collection<SkillType> getSkillTypes() { return skillTypes.values(); }

    private final int objectAddress = 0x01491090;
    // private final int objectAddress = 0x0144e440;
    private final int objectSize = 2136;

    /**
     * Returns the 0-terminated string in the data.
     */
    public static String getStringAt(byte[] data) {
        int stringEnd = 0;
        for(int i = 0; i < data.length; i++) {
            if((data[i] & 0xFF) == 0) {
                stringEnd = i;
                break;
            }
        }
        data = Arrays.copyOfRange(data, 0, stringEnd);

        String str = "";
        try {
            str = new String(data, "SHIFT-JIS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static int find(byte[] source, byte[] match) {
        // sanity checks
        if(source == null || match == null)
            return -1;
        if(source.length == 0 || match.length == 0)
            return -1;
        int ret = -1;
        int spos = 0;
        int mpos = 0;
        byte m = match[mpos];
        for( ; spos < source.length; spos++ ) {
            if(m == source[spos]) {
                // starting match
                if(mpos == 0)
                    ret = spos;
                // finishing match
                else if(mpos == match.length - 1)
                    return ret;
                mpos++;
                m = match[mpos];
            }
            else {
                ret = -1;
                mpos = 0;
                m = match[mpos];
            }
        }
        return ret;
    }

    boolean typesLoaded = false;

    public void onUpdate() {
        if (!typesLoaded) {
            loadSkillTypes();
            typesLoaded = true;
        }
        units.clear();
        friendlyUnits.clear();
        enemyUnits.clear();
        neutralUnits.clear();
        itemUnits.clear();
        deadUnits.clear();
        // System.out.println(getTotalUnits());
        int total = getTotalUnits();
        for(int i = 0; i < total; i++) {
            int offset = objectAddress + (i*objectSize);
            byte[] unitRam = readRam(offset, objectSize);

            // the unit exists if one of the two bytes at 0x844 are not 0
            int unitSentinel = readRAMU16(offset + 0x844);
            if(unitSentinel != 0x0000) {
                Unit unit = new Unit(unitRam);
                units.put(unit.getID(), unit);
                if(unit.getCurrentHp() == 0) {
                    deadUnits.add(unit);
                    // don't count the dead unit against the total
                    total++;
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
            System.out.println(cur.getName() + " [" + cur.getID() + "]");
        }
        System.out.println();
        System.out.println("=== Enemy Units ===");
        for(Unit cur : enemyUnits) {
            System.out.println(cur.getName() + " [" + cur.getID() + "]");
        }
        System.out.println();
        System.out.println("=== Neutral Units ===");
        for(Unit cur : neutralUnits) {
            System.out.println(cur.getName() + " [" + cur.getID() + "]");
        }
        System.out.println();
        System.out.println("=== Item Units ===");
        for(Unit cur : itemUnits) {
            System.out.println(cur.getName() + " [" + cur.getID() + "]");
        }
        System.out.println();
    }
}
