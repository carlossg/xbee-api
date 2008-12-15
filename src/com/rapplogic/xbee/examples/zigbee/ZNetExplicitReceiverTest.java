/**
 * Copyright (c) 2008 Andrew Rapp. All rights reserved.
 *  
 * This file is part of XBee-API.
 *  
 * XBee-API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * XBee-API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with XBee-API.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rapplogic.xbee.examples.zigbee;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetExplicitRxResponse;

public class ZNetExplicitReceiverTest {

	private final static Logger log = Logger.getLogger(ZNetExplicitReceiverTest.class);
	
	private ZNetExplicitReceiverTest() throws Exception {
		XBee xbee = new XBee();		

		try {			
			// replace with the com port or your receiving XBee
			// my coordinator com/baud
			//xbee.open("/dev/tty.usbserial-A6005v5M", 9600);
			xbee.open("COM7", 9600);
			// this is the com port of my end device on my mac
			//xbee.open("/dev/tty.usbserial-A6005uRz", 9600);
			
			while (true) {

				try {
					// we wait here until a packet is received.
					XBeeResponse response = xbee.getResponse();
					
					if (response.getApiId() == ApiId.ZNET_EXPLICIT_RX_RESPONSE) {
						ZNetExplicitRxResponse rx = (ZNetExplicitRxResponse) response;
					
						log.info("received explicit packet response " + response.toString());
					} else {
						log.debug("received unexpected packet " + response.toString());
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		} finally {
			xbee.close();
		}
	}

	public static void main(String[] args) throws Exception {
		// init log4j
		PropertyConfigurator.configure("log4j.properties");
		new ZNetExplicitReceiverTest();
	}
}
