package com.rapplogic.xbee.api.wpan;

import org.apache.log4j.Logger;

import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.util.IntArrayOutputStream;


/**
 * Constructs frame data portion of a 64-bit transmit request
 * 
 * @author andrew
 *
 */
public class TxRequest64 extends TxRequestBase {
	
	private final static Logger log = Logger.getLogger(TxRequest64.class);
	
	private XBeeAddress64 remoteAddr64;
	
	/**
	 * 16 bit Tx Request with default frame id and awk option
	 * 
	 * @param destinationAddress
	 * @param payload
	 */
	public TxRequest64(XBeeAddress64 destination, int[] payload) {
		this(destination, DEFAULT_FRAME_ID, Option.DEFAULT_OPTION, payload);
	}
	
	/**
	 * 16 bit Tx Request.
	 *   
	 * Keep in mind that if you programmed the destination address with AT commands, it is in Hex,
	 * so prepend int with 0x (e.g. 0x1234).
	 * 
	 * Payload size is limited to 100 bytes, according to MaxStream documentation.
	 * 
	 * @param destinationAddress
	 * @param awkFrameId
	 * @param payload
	 */
	public TxRequest64(XBeeAddress64 destination, int frameId, int[] payload) {
		this(destination, frameId, Option.DEFAULT_OPTION, payload);
	}
	
	/**
	 * Note: if option is DISABLE_ACK_OPTION you will not get a ack response and you must use the asynchronous send method
	 * 
	 * @param destinationAddress
	 * @param awkFrameId
	 * @param payload
	 * @param option
	 */
	public TxRequest64(XBeeAddress64 remoteAddr64, int frameId, Option option, int[] payload) {
		this.remoteAddr64 = remoteAddr64;
		this.setFrameId(frameId);
		this.setOption(option);
		this.setPayload(payload);
	}

	public int[] getFrameData() {
		super.getFrameId();
		
		// response does not imply ack
		if (remoteAddr64.equals(XBeeAddress64.BROADCAST) && this.getOption() != Option.DISABLE_ACK_OPTION) {
			throw new RuntimeException("When sending a broadcast packet you cannot get an ACK and so your option must equal: " + Option.DISABLE_ACK_OPTION);
		}

		IntArrayOutputStream out = new IntArrayOutputStream();
		
		// api id
		out.write(this.getApiId());
		// frame id (arbitrary byte that will be sent back with ack)
		out.write(this.getFrameId());
		// destination high (broadcast is 0xFFFF)
		
		// add 64-bit dest address
		out.write(remoteAddr64.getAddress());
		
		// options byte disable ack = 1, send pan id = 4
		out.write(this.getOption().getValue());		
		out.write(this.getPayload());
		
		return out.getIntArray();	
	}
	
	public int getApiId() {
		return TX_REQUEST_64;
	}

	public XBeeAddress64 getRemoteAddr64() {
		return remoteAddr64;
	}

	public void setRemoteAddr64(XBeeAddress64 remoteAddr64) {
		this.remoteAddr64 = remoteAddr64;
	}
	
	public String toString() {
		return super.toString() + 
			"remoteAddress64=" + this.remoteAddr64.toString();
	}

}
