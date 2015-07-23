package com.libnodave;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Test {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        byte[] buf = null;

        // TODO Auto-generated method stub
        Interface ifc = new Interface("IF1", "10.0.0.7", 102);
        // Interface ifc = new Interface("IF1", "192.168.1.110", 102);

        ifc.setTimeout(5000000);


        System.out.println("New connection...");
        Connection con = new Connection(ifc, 2, 0, 2);

        System.out.println("connect...");
        con.connectPLC();

        System.out.println("read DB150 ...");
        try {
            buf = con.readBytes(Connection.AREA_DB, 150, 0, 24);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.printf("Byte 0: [%02x]", (short) (0xff & (int) buf[0]));

		/* Write a REAL value */
        ByteBuffer bbuf;
        bbuf = ByteBuffer.wrap(buf);
        bbuf.order(ByteOrder.BIG_ENDIAN);
        bbuf.putFloat(0, (float) 25.5);
        try {
            con.writeBytes(Connection.AREA_DB, 150, 10, 4, buf);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        System.out.println("read bytes...");
        try {
            buf = con.readBytes(Connection.AREA_DB, 18, 0, 128);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println("read bytes...");

        System.out.println("buf.length: " + buf.length);

        for (int i = 0; i < 128; i++) {
            System.out.printf("[%02x]", (short) (0xff & (int) buf[i]));
        }

        try {
            con.setBit(Connection.AREA_DB, 150, 0, 2);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
		
		
		
		
		/* Setze Bit */
        for (int i = 0; i < 10; i++) {

            System.out.println("Toggle bit");
            try {
                con.setBit(Connection.AREA_DB, 150, 0, 4);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                con.clrBit(Connection.AREA_DB, 150, 0, 4);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        try {
            con.setBit(Connection.AREA_DB, 150, 0, 3);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
		
		/* Parse data block */
        bbuf = ByteBuffer.wrap(buf);
        bbuf.order(ByteOrder.BIG_ENDIAN);
        bbuf.position(0);

        System.out.println("Betriebsstunden: " + (0xffff & (int) bbuf.getShort()));
        System.out.println("Netzspannung L1: " + bbuf.getFloat(56));
        System.out.println("Drehzahl: " + bbuf.getFloat(36));
        System.out.println("Power: " + bbuf.getFloat(100));
    }

}
