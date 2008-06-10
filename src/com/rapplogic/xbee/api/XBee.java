package com.rapplogic.xbee.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.rapplogic.xbee.RxTxSerialComm;
import com.rapplogic.xbee.util.ExceptionHandler;

/**
 *  Copyright (c) 2008 Andrew Rapp
 *  
 *  This file is part of XBee-API.
 *  
 *  XBee-API is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  XBee-API is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with XBee-API.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  
 *  
 *  
 * This is an API for XBee 802.15.4 and ZNet radios
 * 
 * Objectives: 
 * 	Focus on support for a single version of firmware for both ZNet and 802.15.4 XBee radios; this would likely be the latest stable. 
 * 	Implement functionality to meet an expected 80% of usage
 *  Strive for correctness and reliability over percentage of features implemented
 *  
 * Disclaimers: 
 *  This software should be considered experimental/untested.
 *  I can't commit to supporting for any length of time (that's one reason it's open source).
 *  I also can't commit to providing technical support but will attempt to provide help where possible.
 *  I recommend you are familiar with Java and basic electronics before investing in this API such that
 *  you are comfortable with maintaining it to support your objectives; although it is my intention to support this project
 *  I won't be able to commit to backwards compatibility until possibly once the API reaches a level of maturity.
 *  
 * Notes:
 * This API has been developed and tested with rxtx-2.1-7r2 on windows and mac. Other versions/OSes may
 * work.
 * 
 * The API mode classes are designed for escaped byte mode (AP=2).  This applies to both varieties of XBee (ZigBee and 802.14.5).
 * Use of API without correct AP mode may result in some errors, or lots of errors.
 *  
 * Unfortunately I don't have a good solution in place for regression testing.  Since this API depends on hardware,
 * there can be significant work in configuration/setup/test iterations required to test all functionality.  That said
 * I may have broken previously working stuff.
 * 
 * Please send feedback to email address listed below
 * 
 * TODO add gpl license to all source code files
 * TODO XBee Server to share api/radio with multiple apps
 * TODO simple ant build to create JAR file/dist
 * TODO ZNet Cluster ID
 * TODO Test on linux
 * TODO testNG framework for unit tests
 * 
 * Windows users: To locate your COM port on windows, go to Start->(right-click)My Computer->Manage, then Select Device Manager and Ports
 * 
 * This is disappointing: "The WR command should be used sparingly. The EM250 supports a limited number of write cycles.� How many is limited??

 * @author Andrew Rapp a l r a p p [4t] yahoo
 * 
 */
public class XBee extends RxTxSerialComm implements XBeePacketHandler {

	private final static Logger log = Logger.getLogger(XBee.class);

	// Default timeout of 5 seconds
	public final int DEFAULT_TIMEOUT = 5000;
	
	private Object newPacketNotification = new Object();

	private int timeout = DEFAULT_TIMEOUT;

	// TODO clear this list after it reaches a certain size
	private List<XBeeResponse> packetList = new ArrayList<XBeeResponse>();

	private List<XBeeResponse> synchronousSendPacketList = new ArrayList<XBeeResponse>();

	private List<Throwable> errorList = new ArrayList<Throwable>();

	private XBeePacketParser parser;
	
	private long packetCount;

	private int sequentialFrameId = 0xff;
	
	public XBee() {

	}

	public void open(String port, int baudRate) throws XBeeException {
		try {
			this.openSerialPort(port, baudRate);
			parser = new XBeePacketParser(this.getInputStream(), this, newPacketNotification);			
		} catch (Exception e) {
			throw new XBeeException(e);
		}
	}

	public XBeeResponse sendAtCommand(AtCommand command) throws XBeeException {
		return this.sendSynchronous(command);
	}

	public void sendPacket(XBeePacket packet) throws IOException {
		log.debug("sending packet " + packet.toString());
		
		for (int i = 0; i < packet.getPacket().length; i++) {
			this.getOutputStream().write(packet.getPacket()[i]);
		}

		this.getOutputStream().flush();
	}

	public void sendAsynchronous(XBeeRequest frameData) throws XBeeException {

		try {
			// still need this in case multiple threads are sending packets.. they could get mixed up
			synchronized (newPacketNotification) {
				XBeePacket packet = frameData.getXBeePacket();
				this.sendPacket(packet);
			}			
		} catch (Exception e) {
			throw new XBeeException(e);
		}
	}

	/**
	 * Synchronous method for sending an XBeeApi packet. Sends packet and waits
	 * for response. Currently you will get indeterminate results if you are
	 * receiving sample data from radio while using this method.
	 * 
	 * This method should only be called with requests that receive a response of
	 * type XBeeFrameIdResponse
	 * 
	 * WARNING this method is not fully completed.  For best results use getResponse 
	 * 
	 * @param xbeeRequest
	 * @param timeout
	 * @return
	 * @throws XBeeException 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws XBeeTimeoutException 
	 */
	public XBeeResponse sendSynchronous(XBeeRequest xbeeRequest) throws XBeeException {

		if (xbeeRequest.getFrameId() == XBeeRequest.NO_RESPONSE_FRAME_ID) {
			throw new XBeeException("Frame Id cannot be 0 for a synchronous call -- it will always timeout as there is no response!");
		}

		XBeePacket txPacket = xbeeRequest.getXBeePacket();

		XBeeResponse response = null;
		
		try {
			synchronized (newPacketNotification) {
	
				// first remove any old packets
				synchronousSendPacketList.clear();
	
				this.sendPacket(txPacket);
	
				long now = System.currentTimeMillis();
				// log.debug("waiting");
				
				// releases newPacketNotification and waits for next packet
				newPacketNotification.wait(timeout);
				
				if ((System.currentTimeMillis() - now) >= timeout && synchronousSendPacketList.size() == 0) {
					throw new XBeeTimeoutException();
				} else {
					if (synchronousSendPacketList.size() == 0) {
						// didn't think this would happen?
						throw new RuntimeException("No response!");
					} else if (synchronousSendPacketList.size() > 1) {
						// TODO this is likely to occur if radio is sending back
						// samples
						// TODO need to synchronize the adding and removing of
						// packets on packetlist
	
						boolean waited = false;
						
						while (response == null) {
							for (XBeeResponse rxResponse : synchronousSendPacketList) {
								if (rxResponse instanceof XBeeFrameIdResponse && ((XBeeFrameIdResponse)rxResponse).getFrameId() == xbeeRequest.getFrameId()) {
									// frame id matches -- yay we found it
									response = rxResponse;
									break;	
								}
							}	
	
							if (response == null) {
								// we didn't get the right packet.. we will
								// wait a bit more if this is the first time around
								
								if (!waited) {
									// the radio may be receiving I/O samples at
									// a high rate.. wait just a bit longer
									
									// TODO this is still not entirely correct since this gets notified once per packet
									// we should wait an arbitrary amount of time or # packets, whichever occurs first before giving up
									newPacketNotification.wait(250);
									
									waited = true;
								} else {
									throw new RuntimeException("Packets were received but not a TX_16_STATUS_RESPONSE");
								}
							}
						}
					} else {
						response = (XBeeResponse) synchronousSendPacketList.get(0);
	
						if (response == null) {
							throw new RuntimeException("response is null");
						}
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof XBeeException) {
				throw (XBeeException) e;
			} else {
				throw new XBeeException(e);	
			}
		}
			
		return response;
	}

	/**
	 * You can synchronize on this lock to get notified of new packets, however you must call wait() 
	 * to release the lock or buffers will overrun and general chaos will ensue.
	 * 
	 * @return
	 */
	public Object getNewPacketNotification() {
		return newPacketNotification;
	}

	public void handlePacket(XBeeResponse packet) {
		packetCount++;
		synchronousSendPacketList.add(packet);
		packetList.add(packet);
	}

	public void error(Throwable th) {
		errorList.add(th);
	}

	/**
	 * Called by RXTX to notify us that data is available to be read.
	 */
	protected void handleSerialData()  {
		log.info("RXTX serialEvent");

		// alert the parser we have new data
		// parser may not be waiting
		synchronized (parser) {
			parser.notify();
		}
	}

	/**
	 * Blocks until a packet is available and removes the packet from the packetList 
	 * WARNING: Be sure to clear packlist or it will return immediately if any packets are
	 * currently in the list
	 * 
	 * @return
	 * @throws XBeeException 
	 */
	public XBeeResponse getResponse() throws XBeeException {
		return getResponse(0);
	}

	/**
	 * Blocks until a response has been received
	 * 
	 * @throws XBeeException
	 */
	public void waitForResponse() throws XBeeException {
		this.waitForResponse(0);
	}
	
	public void waitForResponse(long timeout) throws XBeeException {
		try {
			synchronized (this.getNewPacketNotification()) {
				// wait for packets
				this.getNewPacketNotification().wait(timeout);
			}			
		} catch (Exception e) {
			ExceptionHandler.handleAndThrow(e);
		}
	}
	
	public XBeeResponse getResponse(int timeout) throws XBeeException, XBeeTimeoutException {
		try {
			synchronized (this.getNewPacketNotification()) {
				
				if (this.getPacketList().size() > 0) {
					return (XBeeResponse) this.getPacketList().remove(0);
				} else {
					long now = System.currentTimeMillis();
					// wait for packets
					this.getNewPacketNotification().wait(timeout);
					
					if (timeout > 0 && (System.currentTimeMillis() - now >= timeout)) {
						throw new XBeeTimeoutException();
					}
					
					// we got notified
					if (this.getPacketList().size() > 0) {
						return (XBeeResponse) this.getPacketList().remove(0);
					} else {
						throw new XBeeException("newPacketNotification was notified but no packets are available");
					}
				}
			}			
		} catch (Exception e) {
			ExceptionHandler.handleAndThrow(e);
		}
		
		// to satisfy eclipse compiler
		return null;
	}

	/**
	 * You need to remove packets from this list either manually or through getResponse; 
	 * otherwise the list will consume memory, unbounded, as it grows,  
	 * 
	 * @return
	 */
	public List<XBeeResponse> getPacketList() {
		return packetList;
	}

	public int getTimeout() {
		return timeout;
	}

	/**
	 * This affects how long we wait for a response during a synchronous call before giving up
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public List<Throwable> getErrorList() {
		return errorList;
	}

	public long getPacketCount() {
		return packetCount;
	}
	
	/**
	 * Shuts down RXTX and input stream threads
	 */
	public void close() {
		super.close();
		// shutdown parser thread
		if (parser != null) {
			parser.setDone(true);
			// wake up if it's waiting for data
			parser.interrupt();
		}
	}
	
	public int getCurrentFrameId() {
		return sequentialFrameId;
	}
	
	public int getNextFrameId() {
		if (sequentialFrameId == 0xff) {
			// flip
			sequentialFrameId = 1;
		} else {
			sequentialFrameId++;
		}
		
		return sequentialFrameId;
	}
	
	public void updateFrameId(int val) {
		if (val <=0 || val > 0xff) {
			throw new IllegalArgumentException("invalid frame id");
		}
		
		this.sequentialFrameId = val;
	}
}
