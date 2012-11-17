/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tinyos.message;

/**
 * Low level time sync message - time is set 
 * @author ph4r05
 */
public class LowlevelTimeSyncMessage extends net.tinyos.message.Message {
        /** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 12;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 234;

    /** Create a new LowlevelTimeSyncMessage of size 12. */
    public LowlevelTimeSyncMessage() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /** Create a new LowlevelTimeSyncMessage of the given data_length. */
    public LowlevelTimeSyncMessage(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage with the given data_length
     * and base offset.
     */
    public LowlevelTimeSyncMessage(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage using the given byte array
     * as backing store.
     */
    public LowlevelTimeSyncMessage(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage using the given byte array
     * as backing store, with the given base offset.
     */
    public LowlevelTimeSyncMessage(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage using the given byte array
     * as backing store, with the given base offset and data length.
     */
    public LowlevelTimeSyncMessage(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage embedded in the given message
     * at the given base offset.
     */
    public LowlevelTimeSyncMessage(net.tinyos.message.Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new LowlevelTimeSyncMessage embedded in the given message
     * at the given base offset and length.
     */
    public LowlevelTimeSyncMessage(net.tinyos.message.Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
    /* Return a String representation of this message. Includes the
     * message type name and the non-indexed field values.
     */
    public String toString() {
      String s = "Message <LowlevelTimeSyncMessage> \n";
      try {
        s += "  [counter=0x"+Long.toHexString(get_counter())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [offset=0x"+Long.toHexString(get_offset())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [high=0x"+Long.toHexString(get_high())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [low=0x"+Long.toHexString(get_low())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [flags=0x"+Long.toHexString(get_flags())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }

    // Message-type-specific access methods appear below.

    /////////////////////////////////////////////////////////
    // Accessor methods for field: counter
    //   Field type: short, unsigned
    //   Offset (bits): 0
    //   Size (bits): 8
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'counter' is signed (false).
     */
    public static boolean isSigned_counter() {
        return false;
    }

    /**
     * Return whether the field 'counter' is an array (false).
     */
    public static boolean isArray_counter() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'counter'
     */
    public static int offset_counter() {
        return (0 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'counter'
     */
    public static int offsetBits_counter() {
        return 0;
    }

    /**
     * Return the value (as a short) of the field 'counter'
     */
    public short get_counter() {
        return (short)getUIntBEElement(offsetBits_counter(), 8);
    }

    /**
     * Set the value of the field 'counter'
     */
    public void set_counter(short value) {
        setUIntBEElement(offsetBits_counter(), 8, value);
    }

    /**
     * Return the size, in bytes, of the field 'counter'
     */
    public static int size_counter() {
        return (8 / 8);
    }

    /**
     * Return the size, in bits, of the field 'counter'
     */
    public static int sizeBits_counter() {
        return 8;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: offset
    //   Field type: int, unsigned
    //   Offset (bits): 8
    //   Size (bits): 16
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'offset' is signed (false).
     */
    public static boolean isSigned_offset() {
        return false;
    }

    /**
     * Return whether the field 'offset' is an array (false).
     */
    public static boolean isArray_offset() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'offset'
     */
    public static int offset_offset() {
        return (8 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'offset'
     */
    public static int offsetBits_offset() {
        return 8;
    }

    /**
     * Return the value (as a int) of the field 'offset'
     */
    public int get_offset() {
        return (int)getUIntBEElement(offsetBits_offset(), 16);
    }

    /**
     * Set the value of the field 'offset'
     */
    public void set_offset(int value) {
        setUIntBEElement(offsetBits_offset(), 16, value);
    }

    /**
     * Return the size, in bytes, of the field 'offset'
     */
    public static int size_offset() {
        return (16 / 8);
    }

    /**
     * Return the size, in bits, of the field 'offset'
     */
    public static int sizeBits_offset() {
        return 16;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: high
    //   Field type: long, unsigned
    //   Offset (bits): 24
    //   Size (bits): 32
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'high' is signed (false).
     */
    public static boolean isSigned_high() {
        return false;
    }

    /**
     * Return whether the field 'high' is an array (false).
     */
    public static boolean isArray_high() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'high'
     */
    public static int offset_high() {
        return (24 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'high'
     */
    public static int offsetBits_high() {
        return 24;
    }

    /**
     * Return the value (as a long) of the field 'high'
     */
    public long get_high() {
        return (long)getUIntBEElement(offsetBits_high(), 32);
    }

    /**
     * Set the value of the field 'high'
     */
    public void set_high(long value) {
        setUIntBEElement(offsetBits_high(), 32, value);
    }

    /**
     * Return the size, in bytes, of the field 'high'
     */
    public static int size_high() {
        return (32 / 8);
    }

    /**
     * Return the size, in bits, of the field 'high'
     */
    public static int sizeBits_high() {
        return 32;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: low
    //   Field type: long, unsigned
    //   Offset (bits): 56
    //   Size (bits): 32
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'low' is signed (false).
     */
    public static boolean isSigned_low() {
        return false;
    }

    /**
     * Return whether the field 'low' is an array (false).
     */
    public static boolean isArray_low() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'low'
     */
    public static int offset_low() {
        return (56 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'low'
     */
    public static int offsetBits_low() {
        return 56;
    }

    /**
     * Return the value (as a long) of the field 'low'
     */
    public long get_low() {
        return (long)getUIntBEElement(offsetBits_low(), 32);
    }

    /**
     * Set the value of the field 'low'
     */
    public void set_low(long value) {
        setUIntBEElement(offsetBits_low(), 32, value);
    }

    /**
     * Return the size, in bytes, of the field 'low'
     */
    public static int size_low() {
        return (32 / 8);
    }

    /**
     * Return the size, in bits, of the field 'low'
     */
    public static int sizeBits_low() {
        return 32;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: flags
    //   Field type: short, unsigned
    //   Offset (bits): 88
    //   Size (bits): 8
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'flags' is signed (false).
     */
    public static boolean isSigned_flags() {
        return false;
    }

    /**
     * Return whether the field 'flags' is an array (false).
     */
    public static boolean isArray_flags() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'flags'
     */
    public static int offset_flags() {
        return (88 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'flags'
     */
    public static int offsetBits_flags() {
        return 88;
    }

    /**
     * Return the value (as a short) of the field 'flags'
     */
    public short get_flags() {
        return (short)getUIntBEElement(offsetBits_flags(), 8);
    }

    /**
     * Set the value of the field 'flags'
     */
    public void set_flags(short value) {
        setUIntBEElement(offsetBits_flags(), 8, value);
    }

    /**
     * Return the size, in bytes, of the field 'flags'
     */
    public static int size_flags() {
        return (8 / 8);
    }

    /**
     * Return the size, in bits, of the field 'flags'
     */
    public static int sizeBits_flags() {
        return 8;
    }
}
