package com.ruin.psp;

public class SimpleClient implements PSPListener {
    PSP psp;

    public SimpleClient() {
        psp = new PSP(this);
        psp.startEmulator("/home/prin/game/phantom-brave-us.iso");
        // psp.loadSaveState(3);
        psp.step();
        psp.step();
        while(true) {
            psp.step();
            psp.onUpdate();
            psp.listUnits();
        }
    }

    public void onFrameStep() {}
    public void onGameUpdate() {}

    public static void main(String[] args) {
        new SimpleClient();
    }
}
