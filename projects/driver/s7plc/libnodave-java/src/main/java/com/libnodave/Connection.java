package com.libnodave;

import java.io.IOException;

/*
 * Bundle-NativeCode: lib/i386/libnodave.so;lib/i386/liblibnodave-native.so;osname=Linux;processor=i386,lib/arm/libnodave.so;lib/arm/liblibnodave-native.so;osname=Linux;processor=arm
 */

public class Connection {
    private long conHandle;

    public static final int AREA_DB = 0x84;
    public static final int AREA_INPUTS = 0x81;
    public static final int AREA_OUTPUTS = 0x82;
    public static final int AREA_FLAGS = 0x82;
    public static final int AREA_DIRECT_PERIPHERAL = 0x80;

    public Connection(Interface ifc, int mpiAddr, int rack, int slot) {
        this.conHandle = Interface.daveNewConnection(ifc.getHandle(), mpiAddr, rack, slot);
    }

    public synchronized boolean connectPLC() {

        if (Interface.daveConnectPLC(this.conHandle) == 0)
            return true;
        else
            return false;
    }

    public synchronized void disconnectPLC() {
        Interface.daveDisconnectPLC(this.conHandle);
    }

    public synchronized void writeBytes(int area,
                                        int areaNumber,
                                        int startAddress,
                                        int length,
                                        byte[] buffer) throws IOException {
        Interface.daveWriteBytes(this.conHandle, area, areaNumber, startAddress, length, buffer);
    }

    public synchronized void setBit(int area, int areaNumber, int byteAddr, int bitAddr)
            throws IOException {
        Interface.daveSetBit(this.conHandle, area, areaNumber, byteAddr, bitAddr);
    }

    public synchronized void clrBit(int area, int areaNumber, int byteAddr, int bitAddr)
            throws IOException {
        Interface.daveClrBit(this.conHandle, area, areaNumber, byteAddr, bitAddr);
    }

    public synchronized byte[] readBytes(int area, int areaNumber, int startAddress, int length)
            throws IOException {
        return Interface.daveReadBytes(this.conHandle, area, areaNumber, startAddress, length);
    }
}
