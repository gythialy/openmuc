package com.libnodave;

import java.io.IOException;

public final class Interface {

    static {
        //		String osName =  System.getProperty("org.osgi.framework.os.name");
        //
        //		System.out.println("osName: " + osName);
        //
        //		if (osName.contains("Windows")) {
        //			System.out.println("Loading libnodave on windows");
        //			System.loadLibrary("libnodave");
        //		}

        System.loadLibrary("libnodave-native");
    }

    private long ifaceHandle;
    private int socket = 0;

    protected static native int daveOpenSocket(String hostname, int port);

    protected static native void daveCloseSocket(int socket);

    protected static native long daveNewInterface(String name, int socket);

    protected static native void daveSetTimeout(long handle, long timeout);

    protected static native long daveNewConnection(long handle, int mpiAddr, int rack, int slot);

    protected static native int daveConnectPLC(long dc);

    protected static native int daveDisconnectPLC(long dc);

    protected static native byte[] daveReadBytes(long dc, int area, int areaNumber, int startAddress, int length) throws IOException;

    //TODO native side to be implemented
    protected static native void daveWriteBytes(long dc, int area, int areaNumber, int startAddress, int length, byte[] buffer) throws
            IOException;

    protected static native int daveSetBit(long dc, int area, int areaNumber, int byteAdr, int bitAdr) throws IOException;

    protected static native int daveClrBit(long dc, int area, int areaNumber, int byteAdr, int bitAdr) throws IOException;

    public Interface(String name, String hostname, int port) throws IOException {
        this.socket = daveOpenSocket(hostname, port);

        if (this.socket == 0) throw new IOException("Cannot create socket to " + hostname + ":" + port);

        this.ifaceHandle = daveNewInterface(name, this.socket);

        if (this.ifaceHandle == 0) throw new IOException("Cannot create interface to " + hostname + ":" + port);
    }

    protected long getHandle() {
        return this.ifaceHandle;
    }

    protected int getSocket() {
        return this.socket;
    }

    public void setTimeout(long timeout) {
        daveSetTimeout(ifaceHandle, timeout);
    }

    public void close() {
        if (this.socket != 0) {
            daveCloseSocket(this.socket);
            this.socket = 0;
        }
    }

    protected void finalize() throws Throwable {
        try {
            this.close();
        } finally {
            super.finalize();
        }
    }

}
