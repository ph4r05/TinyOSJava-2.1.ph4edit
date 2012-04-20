/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tinyos.packet;

/**
 *
 * @author ph4r05
 */
public interface TimestampedPacketSource extends PacketSource {
    /**
     * Returns timestamp in milliseconds for last received packet
     * @return 
     */
    public long getLastTimestamp();
    
    /**
     * Whether current negotiated session supports timestamping (can be negotiated base version)
     * @return 
     */
    public boolean supportsTimestamping();
}
