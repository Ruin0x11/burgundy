package com.ruin.psp;

import com.ruin.psp.models.*;
import java.util.ArrayList;


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

    private ArrayList<Unit> units = new ArrayList<Unit>();

    private final int objectAddress = 0x01491070;
    private final int objectSize = 2136;
    
    public void onUpdate() {
        byte[] b = readRam(objectAddress + (17*objectSize), objectSize);
        Unit u = new Unit(b);
        units = new ArrayList<Unit>();
        units.add(u);
    }
}
