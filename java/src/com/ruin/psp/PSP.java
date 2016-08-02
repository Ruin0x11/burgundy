package com.ruin.psp;

public class PSP {
    public PSP(){}

    public static native void greetSelf();

    public static native void startEmulator(String path);

    public static native void step();

    public static native void nstep(int key);

    public static native void shutdown();
}
