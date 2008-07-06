package com.rapplogic.xbee.api;

/**
 *  Copyright (c) 2008 Andrew Rapp. All rights reserved.
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
 * TODO test
 * 
 * @author andrew
 *
 */
public class AtCommandQueue extends AtCommand {

	public AtCommandQueue(String command) {
		this(command, null, DEFAULT_FRAME_ID);
	}
	
	public AtCommandQueue(String command, int[] value, int frameId) {
		super(command, value, frameId);
	}

	public int getApiId() {
		return AT_COMMAND_QUEUE;
	}
}
