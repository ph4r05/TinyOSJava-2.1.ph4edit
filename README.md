TinyOSJava-2.1.ph4edit
======================

Edit of official TinyOS 2.1 SDK for Java. 

Changes in this fork:
 * all threads spawned by this library is correctly named for easy performance testing and identification of bottlenecks
 * shutdown procedure for MoteIF didn't work well, there remained ild running threads in memory
 * packet timestaming
 * message listener tool displays times of message reception
 * serial forwarder server extraction

Packet timestamping:
Each received packet is timestamped as soon as possible in this library (after message reception by low layer). 
Timestamp is result of operation System.currentTimeMillis() in time of message reception. This record
it finally stored to new attribute added to net.tinyos.message.Message milliTime. This works for 
these types of connection to node: network, serial, serial forwarder.

Serial forwarder in server mode is modified to support transmitting message timestamp accross 
Serial Forwarder protocol. Thus if message is received on serial forwarder server,
it reads its timestamp and transmits with the message to all connected clients. Thus
client connected to remote serial forwarder gets correct packet timestamp - time when
was packet received on remote side. As closer as possible to real interface. 

Serial forwarder server extraction:
Serial forwarder class was modified in order to support running several serial forwarders
within one single application, without any GUI. Thus one can spawn for example 200 serial 
forwarders independent on each other, each connected to different node and "switch"
traffic from local testbed over IP network.

