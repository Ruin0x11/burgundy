package com.ruin.psp;

public class PSP {
    public PSP(){}

    synchronized public static native void greetSelf();

    synchronized public static native void startEmulator(String path);

    synchronized public static native void step();

    synchronized public static native void nstep(int key);

    synchronized public static native void shutdown();

    synchronized public static native void loadSaveState(int slot);

    synchronized public static native int readRAMU8(int address);

    synchronized public static native int readRAMU16(int address);

    synchronized public static native long readRAMU32(int address);
}
