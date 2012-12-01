// $Id: Packetizer.java,v 1.7 2007/08/20 23:50:04 idgay Exp $

/*									tab:4
 * "Copyright (c) 2000-2003 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Copyright (c) 2002-2003 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE     
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA, 
 * 94704.  Attention:  Intel License Inquiry.
 */
package net.tinyos.packet;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.tinyos.message.LowlvlTimeSyncMsg32;
import net.tinyos.message.LowlvlTimeSyncMsg64;
import net.tinyos.message.SerialPacket;
import net.tinyos.util.*;

/**
 * The Packetizer class implements the new mote-PC protocol, using a ByteSource
 * for low-level I/O
 */
public class Packetizer extends AbstractSource implements Runnable, TimestampedPacketSource {
  /*
   * Protocol inspired by, but not identical to, RFC 1663. There is
   * currently no protocol establishment phase, and a single byte
   * ("packet type") to identify the kind/target/etc of each packet.
   * 
   * The protocol is really, really not aiming for high performance.
   * 
   * There is however a hook for future extensions: implementations
   * are required to answer all unknown packet types with a P_UNKNOWN
   * packet.
   * 
   * To summarise the protocol: 
   * - the two sides (A & B) are connected by a (potentially
   *   unreliable) byte stream
   *
   * - the two sides exchange packets framed by 0x7e (SYNC_BYTE) bytes
   *
   * - each packet has the form 
   *     <packet type> <data bytes 1..n> <16-bit crc> 
   *   where the crc (see net.tinyos.util.Crc) covers the packet type
   *   and bytes 1..n
   *
   * - bytes can be escaped by preceding them with 0x7d and their
   *   value xored with 0x20; 0x7d and 0x7e bytes must be escaped,
   *   0x00 - 0x1f and 0x80-0x9f may be optionally escaped
   *
   * - There are currently 5 packet types: 
   *   P_PACKET_NO_ACK: A user-packet, with no ack required
   *   P_PACKET_ACK: A user-packet with a prefix byte, ack
   *   required. The receiver must send a P_ACK packet with the 
   *   prefix byte as its contents.  
   *   P_ACK: ack for a previous P_PACKET_ACK packet 
   *   P_UNKNOWN: unknown packet type received. On reception of an
   *   unknown packet type, the receicer must send a P_UNKNOWN packet,
   *   the first byte must be the unknown packet type. 
   *
   * - Packets that are greater than a (private) MTU are silently
   *   dropped.
   */
  final static boolean DEBUG = false;

  final static int SYNC_BYTE = Serial.HDLC_FLAG_BYTE;

  final static int ESCAPE_BYTE = Serial.HDLC_CTLESC_BYTE;

  final static int MTU = 256;

  final static int ACK_TIMEOUT = 1000; // in milliseconds

  final static int P_ACK = Serial.SERIAL_PROTO_ACK;

  final static int P_PACKET_ACK = Serial.SERIAL_PROTO_PACKET_ACK;

  final static int P_PACKET_NO_ACK = Serial.SERIAL_PROTO_PACKET_NOACK;

  final static int P_UNKNOWN = Serial.SERIAL_PROTO_PACKET_UNKNOWN;

  private ByteSource io;

  private boolean inSync;

  private byte[] receiveBuffer = new byte[MTU];

  private int seqNo;

  // Packets are received by a separate thread and placed in a
  // per-packet-type queue. If received[x] is null, then x is an
  // unknown protocol (but P_UNKNOWN and P_PACKET_ACK are handled
  // specially)
  private Thread reader;

  private LinkedList[] received;
  
  private LinkedList<Long>[] receivedTimes;
  
  private long lastTimestamp=0;
  
  private boolean running=true;
  
  /**
   * Packetizers are built using the makeXXX methods in BuildSource
   */
  Packetizer(String name, ByteSource io) {
    super(name);
    this.io = io;
    inSync = false;
    seqNo = 13;
    reader = new Thread(this);
    received = new LinkedList[256];
    received[P_ACK] = new LinkedList();
    received[P_PACKET_NO_ACK] = new LinkedList();
    
    receivedTimes = new LinkedList[256];
    receivedTimes[P_ACK] = new LinkedList<Long>();
    receivedTimes[P_PACKET_NO_ACK] = new LinkedList<Long>();
    
    // reader thread name
    this.reader.setName(name + "; Packetizer");
  }

  synchronized public void open(Messenger messages) throws IOException {
    super.open(messages);
    if (!reader.isAlive()) {
      reader.start();
    }
  }

  protected void openSource() throws IOException {
    io.open();
  }

  protected void closeSource() {
      // close all running threads
      this.running=false;
      
      io.close();
            
      /**
       * Try to close running thread here
       */
      while (this.reader.isAlive()) {
          synchronized (this.reader) {
              this.reader.notify();
              this.reader.notifyAll();
              this.reader.interrupt();
          }
          try {
              Thread.sleep(10);
          } catch (InterruptedException ex) {
              Logger.getLogger(Packetizer.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
  }

  protected byte[] readProtocolPacket(int packetType, long deadline)
      throws IOException {
    LinkedList inPackets = received[packetType];
    LinkedList<Long> inPacketsTimes = receivedTimes[packetType];
    
    // Wait for a packet on inPackets
    synchronized (inPackets) {
        // if inPackets is empty, clean received times to fix possible
        // problems with synchronization
        synchronized(inPacketsTimes){
            if (inPackets.isEmpty()){
                inPacketsTimes.clear();
            }
        }
        
      while (inPackets.isEmpty()) {
        long now = System.currentTimeMillis();
        if (deadline != 0 && now >= deadline) {
          return null;
        }
        try {
          inPackets.wait(deadline != 0 ? deadline - now : 0);
        } catch (InterruptedException e) {
          throw new IOException("interrupted");
        }
      }
      
      if (inPacketsTimes.isEmpty()==false) this.lastTimestamp = inPacketsTimes.removeFirst();
      return (byte[]) inPackets.removeFirst();
    }
  }

  // Place a packet in its packet queue, or reject unknown packet
  // types (which don't have a queue)
  // 
  // time critical place, add time information about packet here for each packet
  // queueing could mallform packet arrival times
  protected void pushProtocolPacket(int packetType, byte[] packet) {
    LinkedList inPackets = received[packetType];
    LinkedList<Long> inPacketsTime = receivedTimes[packetType];
    if (inPackets != null) {
      synchronized (inPackets) {
        inPacketsTime.add(System.currentTimeMillis());
        inPackets.add(packet);
        inPackets.notify();
      }
    } else if (packetType != P_UNKNOWN) {
      try {
        writeFramedPacket(P_UNKNOWN, packetType, ackPacket, 0);
      } catch (IOException e) {
      }
      message(name + ": ignoring unknown packet type 0x"
          + Integer.toHexString(packetType));
    }
  }

    @Override
  protected byte[] readSourcePacket() throws IOException {
    // Packetizer packet format is identical to PacketSource's
    for (;;) {
      byte[] packet = readProtocolPacket(P_PACKET_NO_ACK, 0);
      if (packet.length >= 1) {
        return packet;
      }
    }
  }

  // Write an ack-ed packet
    @Override
  protected boolean writeSourcePacket(byte[] packet) throws IOException {
    for (int retries = 0; retries < 25; retries++) {
      preSendSerialPacket(packet);
      writeFramedPacket(P_PACKET_ACK, ++seqNo, packet, packet.length);

      long deadline = System.currentTimeMillis() + ACK_TIMEOUT;

      byte[] ack = readProtocolPacket(P_ACK, deadline);
      if (ack == null) {
        if (DEBUG) {
          message(name + ": ACK timed out");
        }
        continue;
      }
      if (ack[0] == (byte) seqNo) {
        if (DEBUG) {
          message(name + ": Rcvd ACK");
        }
        return true;
      }
    }

    return false;
  }

  static private byte ackPacket[] = new byte[0];

  /**
   * Called just before framing and escaping packet before sending.
   * Is utilized for precise packet UART-to-node time synchronization.
   * 
   * Warning! Abstraction violation!!!
   * 1. packetizer knows that serial packet exists and may be transported in byte[]
   * 2. packetizer knows that serial packet can contain LowlvlTimeSyncMsg and 
   *    behaves specifically if this message is detected (current time+offset).
   * 
   * This should be done in different manner. Current state is quick fix, but clean
   * way would require to extend basic message type to include callback to class
   * that takes care about this pre-sending modifications. Since here is no Msg instance
   * anymore, it would be needed to add some meta data for write packet.
   * 
   * Alternatively, there can be another write packet method that could notify some
   * class just before packet sending.
   * 
   * Another modifications would be needed to pass packet type (e.g. SerialPacket)
   * to packetizer along with byte[] packet.
   * @param packet 
   */
  private void preSendSerialPacket(byte[] packet){
      try{
        // expect serial packet here
        if (packet[0]!=Serial.TOS_SERIAL_ACTIVE_MESSAGE_ID){
            return;
        }
          
        // serial packet will be initialized with packet as backing store
        // => modifs made to tmp will be reflected to byte[] packet
        // first element is SerialPacket AM type, thus real serial packet starts 
        // at packet[1]
        SerialPacket tmp = new SerialPacket(packet, 1);
System.err.println(this.getName() + "; preSend; isSerial; AM: " + tmp.amType());

        // check if it is low level time sync message
        if (LowlvlTimeSyncMsg32.AM_TYPE!=tmp.get_header_type()){
            return;
        }
System.err.println(this.getName() + "; preSend; isLowLevel; offdata: " + tmp.offset_data(0)
        + "; size: " + tmp.get_header_length() 
        + "; tmpbase: " + tmp.baseOffset()
        + "; total: " + (tmp.baseOffset()+tmp.offset_data(0)));
System.err.println(this.getName() + "; preSend; isLowLevel; Serial: " + tmp);

        // it is llts message, set correct time
        // instantiate message with data 
        // offset_data(0) is static method -> no base_offset here. Need to take into account now
        LowlvlTimeSyncMsg32 lltsm = new LowlvlTimeSyncMsg32(packet, tmp.baseOffset()+tmp.offset_data(0), tmp.get_header_length());
        
        // determine from message flag whether this is 64 or 32 bit message
        long curTime;
        if ((lltsm.get_flags() & (short)1) == 0){
            // set correct time with offset added
            curTime = lltsm.get_offset() + System.currentTimeMillis();
            // this will change byte[] packet directly since lltsm uses SerialPacket's data
            // as backing store and same does SerialPacket with byte[] packet
            lltsm.set_low((curTime & 0xFFFFFFFF));
            lltsm.set_high((curTime >> 32) & 0xFFFFFFFF);
        } else {
            // it is 64 bit message, reinit again
            LowlvlTimeSyncMsg64 lltsm64 = new LowlvlTimeSyncMsg64(packet, tmp.baseOffset()+tmp.offset_data(0), tmp.get_header_length());
            curTime = lltsm64.get_offset() + System.currentTimeMillis();
            lltsm64.set_globalTime(curTime);
        }
        
System.err.println(this.getName() + "; preSend; altered; curTime: " + curTime
        + "; MSG: " + lltsm);        
      } catch(Exception e){
System.err.println(this.getName() + "; preSend; fuckedup: " + e.getLocalizedMessage()); 
e.printStackTrace();
          message(name + ": problem with pre-send packet modiffication: " + e.getMessage());
      }
  }
  
    @Override
  public void run() {
    try {
      for (;this.running;) {
        byte[] packet = readFramedPacket();
        if (running==false || packet==null) return;
        
        int packetType = packet[0] & 0xff;
        int pdataOffset = 1;

        if (packetType == P_PACKET_ACK) {
          // send ack
          writeFramedPacket(P_ACK, packet[1], ackPacket, 0);
          // And merge with un-acked packets
          packetType = P_PACKET_NO_ACK;
          pdataOffset = 2;
        }
        int dataLength = packet.length - pdataOffset;
        byte[] dataPacket = new byte[dataLength];
        System.arraycopy(packet, pdataOffset, dataPacket, 0, dataLength);
        pushProtocolPacket(packetType, dataPacket);
      }
System.err.println("Packetizer finish | " + this.getName());       
    } catch (IOException e) {
    }
  }

  // Read system-level packet. If inSync is false, we currently don't
  // have sync
  private byte[] readFramedPacket() throws IOException {
    int count = 0;
    boolean escaped = false;

    for (;this.running;) {
      if (!inSync) {
        message(name + ": resynchronising");
        // re-synchronise
        while (io.readByte() != SYNC_BYTE)
          ;
        inSync = true;
        count = 0;
        escaped = false;
      }

      if (count >= MTU) {
        // Packet too long, give up and try to resync
        message(name + ": packet too long");
        inSync = false;
        continue;
      }

      byte b = io.readByte();
      if (escaped) {
        if (b == SYNC_BYTE) {
          // sync byte following escape is an error, resync
          message(name + ": unexpected sync byte");
          inSync = false;
          continue;
        }
        b ^= 0x20;
        escaped = false;
      } else if (b == ESCAPE_BYTE) {
        escaped = true;
        continue;
      } else if (b == SYNC_BYTE) {
        if (count < 4) {
          // too-small frames are ignored
          count = 0;
          continue;
        }
        byte[] packet = new byte[count - 2];
        System.arraycopy(receiveBuffer, 0, packet, 0, count - 2);

        int readCrc = (receiveBuffer[count - 2] & 0xff)
            | (receiveBuffer[count - 1] & 0xff) << 8;
        int computedCrc = Crc.calc(packet, packet.length);

        if (DEBUG) {
          System.err.println("received: ");
          Dump.printPacket(System.err, packet);
          System.err.println(" rcrc: " + Integer.toHexString(readCrc)
              + " ccrc: " + Integer.toHexString(computedCrc));
        }

        if (readCrc == computedCrc) {
          return packet;
        } else {
          message(name + ": bad packet");
          /*
           * We don't lose sync here. If we did, garbage on the line at startup
           * will cause loss of the first packet.
           */
          count = 0;
          continue;
        }
      }

      receiveBuffer[count++] = b;
    }
    
    return null;
  }

    @Override
    public long getLastTimestamp() {
        return this.lastTimestamp;
    }

    @Override
    public boolean supportsTimestamping() {
        return true;
    }

  // Class to build a framed, escaped and crced packet byte stream
  static class Escaper {
    byte[] escaped;

    int escapePtr;

    int crc;

    // We're building a length-byte packet
    Escaper(int length) {
      escaped = new byte[2 * length];
      escapePtr = 0;
      crc = 0;
      escaped[escapePtr++] = SYNC_BYTE;
    }

    static private boolean needsEscape(int b) {
      return b == SYNC_BYTE || b == ESCAPE_BYTE;
    }

    void nextByte(int b) {
      b = b & 0xff;
      crc = Crc.calcByte(crc, b);
      if (needsEscape(b)) {
        escaped[escapePtr++] = ESCAPE_BYTE;
        escaped[escapePtr++] = (byte) (b ^ 0x20);
      } else {
        escaped[escapePtr++] = (byte) b;
      }
    }

    void terminate() {
      escaped[escapePtr++] = SYNC_BYTE;
    }
  }

  // Write a packet of type 'packetType', first byte 'firstByte'
  // and bytes 2..'count'+1 in 'packet'
  private synchronized void writeFramedPacket(int packetType, int firstByte,
      byte[] packet, int count) throws IOException {
    if (DEBUG) {
      System.err.println("sending: ");
      Dump.printByte(System.err, packetType);
      Dump.printByte(System.err, firstByte);
      Dump.printPacket(System.err, packet);
      System.err.println();
    }

    Escaper buffer = new Escaper(count + 6);

    buffer.nextByte(packetType);
    buffer.nextByte(firstByte);
    for (int i = 0; i < count; i++) {
      buffer.nextByte(packet[i]);
    }

    int crc = buffer.crc;
    buffer.nextByte(crc & 0xff);
    buffer.nextByte(crc >> 8);

    buffer.terminate();

    byte[] realPacket = new byte[buffer.escapePtr];
    System.arraycopy(buffer.escaped, 0, realPacket, 0, buffer.escapePtr);

    if (DEBUG) {
      Dump.dump("encoded", realPacket);
    }
    io.writeBytes(realPacket);
  }
}
