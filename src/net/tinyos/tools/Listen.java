// $Id: Listen.java,v 1.4 2006/12/12 18:23:00 vlahan Exp $

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


package net.tinyos.tools;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.tinyos.packet.*;
import net.tinyos.util.*;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

public class Listen {
    // receives other command line parameters than options
    @Argument
    private final List<String> arguments = new ArrayList<String>(8);
    
    @Option(name = "--tstamp", aliases = {"-t"}, usage = "Enables timestamp output.")
    private boolean tstamp=false;
    
    @Option(name = "--one-line", aliases = {"-o"}, usage = "Whole packet dump on one line")
    private boolean oneLine=false;
    
    @Option(name = "--ascii", aliases = {"-a"}, usage = "ASCII conversion added.")
    private boolean ascii=false;
    
    @Option(name = "--comm", aliases={"-c"}, usage = "Node to attach.\n(default packet source from MOTECOM environment variable).")
    private String comm = null;
    
    @Option(name = "--printf", aliases={"-p"}, usage = "Redirects printf messages to a given file.")
    private String printf = null;
    
    private static Listen runningInstance;
    public static void main(String[] args) {
        try {
            // do main on instance
            runningInstance = new Listen();

            // do the main
            runningInstance.doMain(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doMain(String args[]) throws IOException {
        String source = null;
        PacketSource reader;
        
        // command line argument parser
        CmdLineParser parser = new CmdLineParser(this);

        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);
        try {
            // parse the arguments.
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java net.tinyos.tools.Listen  [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println(" Example: java net.tinyos.tools.Listen " + parser.printExample(ExampleMode.ALL));
            return;
        }
        
        source = this.comm;
        if (source == null) {	
  	  reader = BuildSource.makePacketSource();
        }
        else {
  	  reader = BuildSource.makePacketSource(source);
        }
	if (reader == null) {
	    System.err.println("Invalid packet source (check your MOTECOM environment variable)");
	    System.exit(2);
	}
        
        boolean havePrintf=false;
        FileOutputStream fos = null;
        if (this.printf!=null){
            fos = new FileOutputStream(new File(this.printf));
            havePrintf = true;
        }

        // date formater for human readable date format
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        StringBuilder tstampString = null;

	try {
	  reader.open(PrintStreamMessenger.err);
	  for (;;) {
	    byte[] packet = reader.readPacket();
            long timestamp = 0;
            tstampString = new StringBuilder(512);
            
            // timestamped?
            boolean timestampOK = false;
            if (this.tstamp){
                if (reader instanceof TimestampedPacketSource){
                    TimestampedPacketSource tReader = (TimestampedPacketSource) reader;
                    timestamp = tReader.getLastTimestamp();
                    if (tReader.supportsTimestamping()){
                        calendar.setTimeInMillis(timestamp);
                        tstampString.append("# TS[")
                                .append(timestamp)
                                .append("]; F[")
                                .append(formatter.format(calendar.getTime()))
                                .append("] ");

                        timestamp = System.currentTimeMillis();
                        calendar.setTimeInMillis(timestamp);
                        tstampString.append("Now[")
                                .append(timestamp)
                                .append("]; FN[")
                                .append(formatter.format(calendar.getTime()))
                                .append("]");
                        tstampString.append(oneLine ? " " : "\n");
                        
                        timestampOK=true;
                    }
                }

                if(timestampOK==false){
                    timestamp = System.currentTimeMillis();
                    calendar.setTimeInMillis(timestamp);
                    tstampString.append("# Now[").append(timestamp).append("]; FN[").append(formatter.format(calendar.getTime())).append("]");
                    tstampString.append(oneLine ? " " : "\n");
                }
            }
            
            // Do we have printf?
            // If yes, dump it to separate file.
            if (havePrintf && packet.length>=8 && packet[7]==0x64){
                int len = packet[5];
                int maxLen = Math.min(len, packet.length-8);
                
                for (int i = 0; i < maxLen; i++) {
                    char nextChar = (char) packet[8+i];
                    if (nextChar != 0) {
                        fos.write(nextChar);
                    }
                }
                
                fos.flush();
                continue;
            }
            
            System.out.print(tstampString);
	    Dump.printPacket(System.out, packet);
            if (this.ascii){
                System.out.print("ASCII: ");
                printPacket(System.out, packet, 7, packet.length-7);
            }
            
	    System.out.println();
	    System.out.flush();
	  }
	}
	catch (IOException e) {
	    System.err.println("Error on " + reader.getName() + ": " + e);
	}
    }
    
    public static void printByte(PrintStream p, int b) {
        if (b>=0x20 && b <=0x7E){
            char c = (char) (b & 0xff);
            p.print(c);
        } else {
            String bs = Integer.toHexString(b & 0xff).toUpperCase();
            if (b >=0 && b < 16)
                p.print("0");
            p.print(bs + " ");
        }
    }

    public static void printPacket(PrintStream p, byte[] packet, int from, int count) {
	for (int i = from; i < count; i++)
	    printByte(p, packet[i]);
    }

    public static void printPacket(PrintStream p, byte[] packet) {
	printPacket(p, packet, 0, packet.length);
    }
}

