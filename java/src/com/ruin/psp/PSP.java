package com.ruin.psp;

public class PSP {
    public PSP(){}

    public static native void greetSelf();

    public static void loadPspLibrary() {
        System.loadLibrary("psp");
    }
}
