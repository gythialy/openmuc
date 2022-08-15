package org.openmuc.framework.driver.modbus.rtutcp.bonino;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.io.BytesInputStream;
import com.ghgande.j2mod.modbus.io.BytesOutputStream;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransport;
import com.ghgande.j2mod.modbus.msg.ModbusMessage;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.util.ModbusUtil;

/**
 * @author bonino
 * 
 *         https://github.com/dog-gateway/jamod-rtu-over-tcp
 * 
 */
public class ModbusRTUTCPTransport implements ModbusTransport {

    private static final Logger logger = LoggerFactory.getLogger(ModbusRTUTCPTransport.class);

    public static final String logId = "[ModbusRTUTCPTransport]: ";

    // The input stream from which reading the Modbus frames
    private DataInputStream inputStream;

    // The output stream to which writing the Modbus frames
    private DataOutputStream outputStream;

    // The Bytes output stream to use as output buffer for Modbus frames
    private BytesOutputStream outputBuffer;

    // The BytesInputStream wrapper for the transport input stream
    private BytesInputStream inputBuffer;

    // The last request sent over the transport ?? useful ??
    private byte[] lastRequest = null;

    // the socket used by this transport
    private Socket socket;

    // the read timeout timer
    private Timer readTimeoutTimer;

    // the read timout
    private final int readTimeout = 5000; // ms

    // the timeou flag
    private boolean isTimedOut;

    private RTUTCPMasterConnection m_Master = null;
    private final Socket m_Socket = null;

    /**
     * @param socket
     *            the client socket to close
     * 
     * @throws IOException
     *             if a I/O exception occurs
     * 
     */
    public ModbusRTUTCPTransport(Socket socket) throws IOException {
        // prepare the input and output streams...
        if (socket != null) {
            setSocket(socket);
        }

        // set the timed out flag at false
        this.isTimedOut = false;
    }

    /**
     * Stores the given {@link Socket} instance and prepares the related streams to use them for Modbus RTU over TCP
     * communication.
     * 
     * @param socket
     *            the client socket
     * @throws IOException
     *             if a I/O exception occurs
     */
    public void setSocket(Socket socket) throws IOException {
        if (this.socket != null) {
            // TODO: handle clean closure of the streams
            this.outputBuffer.close();
            this.inputBuffer.close();
            this.inputStream.close();
            this.outputStream.close();
        }

        // store the socket used by this transport
        this.socket = socket;

        // get the input and output streams
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(this.socket.getOutputStream());

        // prepare the buffers
        this.outputBuffer = new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);
        this.inputBuffer = new BytesInputStream(Modbus.MAX_MESSAGE_LENGTH);
    }

    /**
     * writes the given ModbusMessage over the physical transport handled by this object.
     * 
     * @param msg
     *            the {@link ModbusMessage} to be written on the transport.
     */
    @Override
    public synchronized void writeMessage(ModbusMessage msg) throws ModbusIOException {
        try {
            // atomic access to the output buffer
            synchronized (this.outputBuffer) {

                // reset the output buffer
                this.outputBuffer.reset();

                // prepare the message for "virtual" serial transport
                msg.setHeadless();

                // write the message to the output buffer
                msg.writeTo(this.outputBuffer);

                // compute the CRC
                int[] crc = ModbusUtil.calculateCRC(this.outputBuffer.getBuffer(), 0, this.outputBuffer.size());

                // write the CRC on the output buffer
                this.outputBuffer.writeByte(crc[0]);
                this.outputBuffer.writeByte(crc[1]);

                // store the buffer length
                int bufferLength = this.outputBuffer.size();

                // store the raw output buffer reference
                byte rawBuffer[] = this.outputBuffer.getBuffer();

                // write the buffer on the socket
                this.outputStream.write(rawBuffer, 0, bufferLength); // PDU +
                                                                     // CRC
                this.outputStream.flush();

                if (logger.isTraceEnabled()) {
                    logger.trace("Sent: " + ModbusUtil.toHex(rawBuffer, 0, bufferLength));
                }

                // store the written buffer as the last request
                this.lastRequest = new byte[bufferLength];
                System.arraycopy(rawBuffer, 0, this.lastRequest, 0, bufferLength);

                // sleep for the time needed to receive the request at the other
                // point of the connection
                this.outputBuffer.wait(bufferLength);
            }

        } catch (Exception ex) {
            throw new ModbusIOException("I/O failed to write");
        }

    }// writeMessage

    // This is required for the slave that is not supported
    @Override
    public synchronized ModbusRequest readRequest() throws ModbusIOException {
        throw new RuntimeException("Operation not supported.");
    } // readRequest

    @Override
    /**
     * Lazy implementation: avoid CRC validation...
     */
    public synchronized ModbusResponse readResponse() throws ModbusIOException {
        // the received response
        ModbusResponse response = null;

        // reset the timed out flag
        this.isTimedOut = false;

        // init and start the timeout timer
        this.readTimeoutTimer = new Timer();
        this.readTimeoutTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                isTimedOut = true;
            }
        }, this.readTimeout);

        try {
            // atomic access to the input buffer
            synchronized (inputBuffer) {
                // clean the input buffer
                inputBuffer.reset(new byte[Modbus.MAX_MESSAGE_LENGTH]);

                // sleep for the time needed to receive the first part of the
                // response
                int available = this.inputStream.available();
                while ((available < 4) && (!this.isTimedOut)) {
                    Thread.yield(); // 1ms * #bytes (4bytes in the worst case)
                    available = this.inputStream.available();

                    // if (logger.isTraceEnabled()) {
                    // logger.trace("Available bytes: " + available);
                    // }

                }

                // check if timedOut
                if (this.isTimedOut) {
                    throw new ModbusIOException("I/O exception - read timeout.\n");
                }

                // get a reference to the inner byte buffer
                byte inBuffer[] = this.inputBuffer.getBuffer();

                // read the first 2 bytes from the input stream
                this.inputStream.read(inBuffer, 0, 2);
                // this.inputStream.readFully(inBuffer);

                // read the progressive id
                int packetId = inputBuffer.readUnsignedByte();

                if (logger.isTraceEnabled()) {
                    logger.trace(ModbusRTUTCPTransport.logId + "Read packet with progressive id: " + packetId);
                }

                // read the function code
                int functionCode = inputBuffer.readUnsignedByte();

                if (logger.isTraceEnabled()) {
                    logger.trace(" uid: " + packetId + ", function code: " + functionCode);
                }

                // compute the number of bytes composing the message (including
                // the CRC = 2bytes)
                int packetLength = computePacketLength(functionCode);

                // sleep for the time needed to receive the first part of the
                // response
                while ((this.inputStream.available() < (packetLength - 3)) && (!this.isTimedOut)) {
                    try {
                        inputBuffer.wait(10);
                    } catch (InterruptedException ie) {
                        // do nothing
                        System.err.println("Sleep interrupted while waiting for response body...\n" + ie);
                    }
                }

                // check if timedOut
                if (this.isTimedOut) {
                    throw new ModbusIOException("I/O exception - read timeout.\n");
                }

                // read the remaining bytes
                this.inputStream.read(inBuffer, 3, packetLength);

                if (logger.isTraceEnabled()) {
                    logger.trace(" bytes: " + ModbusUtil.toHex(inBuffer, 0, packetLength) + ", desired length: "
                            + packetLength);
                }

                // compute the CRC
                int crc[] = ModbusUtil.calculateCRC(inBuffer, 0, packetLength - 2);

                // check the CRC against the received one...
                if (ModbusUtil.unsignedByteToInt(inBuffer[packetLength - 2]) != crc[0]
                        || ModbusUtil.unsignedByteToInt(inBuffer[packetLength - 1]) != crc[1]) {
                    throw new IOException("CRC Error in received frame: " + packetLength + " bytes: "
                            + ModbusUtil.toHex(inBuffer, 0, packetLength));
                }

                // reset the input buffer to the given packet length (excluding
                // the CRC)
                this.inputBuffer.reset(inBuffer, packetLength - 2);

                // create the response
                response = ModbusResponse.createModbusResponse(functionCode);
                response.setHeadless();

                // read the response
                response.readFrom(inputBuffer);
            }
        } catch (IOException e) {
            // debug
            System.err.println(ModbusRTUTCPTransport.logId + "Error while reading from socket: " + e);

            // clean the input stream
            try {
                while (this.inputStream.read() != -1) {

                }
            } catch (IOException e1) {
                // debug
                System.err.println(ModbusRTUTCPTransport.logId + "Error while emptying input buffer from socket: " + e);
            }

            // wrap and re-throw
            throw new ModbusIOException("I/O exception - failed to read.\n" + e);
        }

        // reset the timeout timer
        this.readTimeoutTimer.cancel();

        // return the response read from the socket stream
        return response;

        /*-------------------------- SERIAL IMPLEMENTATION -----------------------------------
        
        try
        {
        	do
        	{
        		// block the input stream
        		synchronized (byteInputStream)
        		{
        			// get the packet uid
        			int uid = inputStream.read();
        			
        			if (Modbus.debug)
        				System.out.println(ModbusRTUTCPTransport.logId + "UID: " + uid);
        			
        			// if the uid is valid (i.e., > 0) continue
        			if (uid != -1)
        			{
        				// get the function code
        				int fc = inputStream.read();
        				
        				if (Modbus.debug)
        					System.out.println(ModbusRTUTCPTransport.logId + "Function code: " + uid);
        				
        				//bufferize the response
        				byteOutputStream.reset();
        				byteOutputStream.writeByte(uid);
        				byteOutputStream.writeByte(fc);
        				
        				// create the Modbus Response object to acquire length of message
        				response = ModbusResponse.createModbusResponse(fc);
        				response.setHeadless();
        				
        				// With Modbus RTU, there is no end frame. Either we
        				// assume the message is complete as is or we must do
        				// function specific processing to know the correct length.
        				
        				//bufferize the response according to the given function code
        				getResponse(fc, byteOutputStream);
        				
        				//compute the response length without considering the CRC
        				dlength = byteOutputStream.size() - 2; // less the crc
        				
        				//debug
        				if (Modbus.debug)
        					System.out.println("Response: "
        							+ ModbusUtil.toHex(byteOutputStream.getBuffer(), 0, dlength + 2));
        				
        				//TODO: check if needed (restore the buffer state, cursor at 0, same content)
        				byteInputStream.reset(inputBuffer, dlength);
        				
        				// cmopute the buffer CRC
        				int[] crc = ModbusUtil.calculateCRC(inputBuffer, 0, dlength);
        				
        				// check the CRC against the received one...
        				if (ModbusUtil.unsignedByteToInt(inputBuffer[dlength]) != crc[0]
        						|| ModbusUtil.unsignedByteToInt(inputBuffer[dlength + 1]) != crc[1])
        				{
        					throw new IOException("CRC Error in received frame: " + dlength + " bytes: "
        							+ ModbusUtil.toHex(byteInputStream.getBuffer(), 0, dlength));
        				}
        			}
        			else
        			{
        				throw new IOException("Error reading response");
        			}
        			
        			// restore the buffer state, cursor at 0, same content
        			byteInputStream.reset(inputBuffer, dlength);
        			
        			//actually read the response
        			if (response != null)
        			{
        				response.readFrom(byteInputStream);
        			}
        			
        			//flag completion...
        			done = true;
        			
        		}// synchronized
        	}
        	while (!done);
        	return response;
        }
        catch (Exception ex)
        {
        	System.err.println("Last request: " + ModbusUtil.toHex(lastRequest));
        	System.err.println(ex.getMessage());
        	throw new ModbusIOException("I/O exception - failed to read");
        }
        
        ------------------------------------------------------------------------------*/
    }// readResponse

    private int computePacketLength(int functionCode) throws IOException {
        // packet length by function code:
        int length = 0;

        switch (functionCode) {
        case 0x01:
        case 0x02:
        case 0x03:
        case 0x04:
        case 0x0C:
        case 0x11: // report slave ID version and run/stop state
        case 0x14: // read log entry (60000 memory reference)
        case 0x15: // write log entry (60000 memory reference)
        case 0x17: {
            // get a reference to the inner byte buffer
            byte inBuffer[] = this.inputBuffer.getBuffer();
            this.inputStream.read(inBuffer, 2, 1);
            int dataLength = this.inputBuffer.readUnsignedByte();
            length = dataLength + 5; // UID+FC+CRC(2bytes)
            break;
        }
        case 0x05:
        case 0x06:
        case 0x0B:
        case 0x0F:
        case 0x10: {
            // read status: only the CRC remains after address and
            // function code
            length = 6;
            break;
        }
        case 0x07:
        case 0x08: {
            length = 3;
            break;
        }
        case 0x16: {
            length = 8;
            break;
        }
        case 0x18: {
            // get a reference to the inner byte buffer
            byte inBuffer[] = this.inputBuffer.getBuffer();
            this.inputStream.read(inBuffer, 2, 2);
            length = this.inputBuffer.readUnsignedShort() + 6;// UID+FC+CRC(2bytes)
            break;
        }
        case 0x83: {
            // error code
            length = 5;
            break;
        }
        }

        return length;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
    }// close

    @Override
    public ModbusTransaction createTransaction() {
        if (m_Master == null) {
            m_Master = new RTUTCPMasterConnection(m_Socket.getInetAddress(), m_Socket.getPort());
        }
        ModbusRTUTCPTransaction trans = new ModbusRTUTCPTransaction(m_Master);
        return trans;
    }

    /*
     * private void getResponse(int fn, BytesOutputStream out) throws IOException { int bc = -1, bc2 = -1, bcw = -1; int
     * inpBytes = 0; byte inpBuf[] = new byte[256];
     * 
     * try { switch (fn) { case 0x01: case 0x02: case 0x03: case 0x04: case 0x0C: case 0x11: // report slave ID version
     * and run/stop state case 0x14: // read log entry (60000 memory reference) case 0x15: // write log entry (60000
     * memory reference) case 0x17: // read the byte count; bc = inputStream.read(); out.write(bc); // now get the
     * specified number of bytes and the 2 CRC bytes setReceiveThreshold(bc + 2); inpBytes = inputStream.read(inpBuf, 0,
     * bc + 2); out.write(inpBuf, 0, inpBytes); m_CommPort.disableReceiveThreshold(); if (inpBytes != bc + 2) {
     * System.out.println("Error: looking for " + (bc + 2) + " bytes, received " + inpBytes); } break; case 0x05: case
     * 0x06: case 0x0B: case 0x0F: case 0x10: // read status: only the CRC remains after address and // function code
     * setReceiveThreshold(6); inpBytes = inputStream.read(inpBuf, 0, 6); out.write(inpBuf, 0, inpBytes);
     * m_CommPort.disableReceiveThreshold(); break; case 0x07: case 0x08: // read status: only the CRC remains after
     * address and // function code setReceiveThreshold(3); inpBytes = inputStream.read(inpBuf, 0, 3); out.write(inpBuf,
     * 0, inpBytes); m_CommPort.disableReceiveThreshold(); break; case 0x16: // eight bytes in addition to the address
     * and function codes setReceiveThreshold(8); inpBytes = inputStream.read(inpBuf, 0, 8); out.write(inpBuf, 0,
     * inpBytes); m_CommPort.disableReceiveThreshold(); break; case 0x18: // read the byte count word bc =
     * inputStream.read(); out.write(bc); bc2 = inputStream.read(); out.write(bc2); bcw = ModbusUtil.makeWord(bc, bc2);
     * // now get the specified number of bytes and the 2 CRC bytes setReceiveThreshold(bcw + 2); inpBytes =
     * inputStream.read(inpBuf, 0, bcw + 2); out.write(inpBuf, 0, inpBytes); m_CommPort.disableReceiveThreshold();
     * break; } } catch (IOException e) { m_CommPort.disableReceiveThreshold(); throw new IOException(
     * "getResponse serial port exception"); } }// getResponse
     */

}
