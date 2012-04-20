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
    public long getLastTimestamp();
}
