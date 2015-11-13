/*
 *    BSDPServer - Implements Apple's Boot Service Discover Protocol
 *     in Java. "jbsdpd"
 *    Copyright (C) 2015  Jan-Philipp Hülshoff <github@bklosr.de>
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.upb.phys.bsdpd;

import java.io.UnsupportedEncodingException;

import de.upb.phys.bsdpd.bsdppackets.BSDPOption;
import de.upb.phys.bsdpd.bsdppackets.DHCPOption;
import edu.bucknell.net.JDHCP.DHCPMessage;

/**
 * BSDP Packet sent/received with/from the socket.
 * 
 * BSDP uses Vendor Specific encapsulated DHCP options for its messages. They
 * are encoded as described by the RFC 2132.
 * 
 * <pre>
 * Excerpt from RFC2132
 * 8.4. Vendor Specific Information
 *    This option is used by clients and servers to exchange vendor-
 *    specific information.  The information is an opaque object of n
 *    octets, presumably interpreted by vendor-specific code on the clients
 *    and servers.  The definition of this information is vendor specific.
 *    The vendor is indicated in the vendor class identifier option.
 *    Servers not equipped to interpret the vendor-specific information
 *    sent by a client MUST ignore it (although it may be reported).
 *    Clients which do not receive desired vendor-specific information
 *    SHOULD make an attempt to operate without it, although they may do so
 *    (and announce they are doing so) in a degraded mode.
 * 
 *    If a vendor potentially encodes more than one item of information in
 *    this option, then the vendor SHOULD encode the option using
 *    &quot;Encapsulated vendor-specific options&quot; as described below:
 * 
 *    The Encapsulated vendor-specific options field SHOULD be encoded as a
 *    sequence of code/length/value fields of identical syntax to the DHCP
 *    options field with the following exceptions:
 * 
 *       1) There SHOULD NOT be a &quot;magic cookie&quot; field in the encapsulated
 *          vendor-specific extensions field.
 * 
 *       2) Codes other than 0 or 255 MAY be redefined by the vendor within
 *          the encapsulated vendor-specific extensions field, but SHOULD
 *          conform to the tag-length-value syntax defined in section 2.
 * 
 *       3) Code 255 (END), if present, signifies the end of the
 *          encapsulated vendor extensions, not the end of the vendor
 *          extensions field. If no code 255 is present, then the end of
 *          the enclosing vendor-specific information field is taken as the
 *          end of the encapsulated vendor-specific extensions field.
 * 
 *    The code for this option is 43 and its minimum length is 1.
 * 
 *    Code   Len   Vendor-specific information
 *    +-----+-----+-----+-----+---
 *    |  43 |  n  |  i1 |  i2 | ...
 *    +-----+-----+-----+-----+---
 * 
 *    When encapsulated vendor-specific extensions are used, the
 *    information bytes 1-n have the following format:
 * 
 *     Code   Len   Data item        Code   Len   Data item       Code
 *    +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 *    |  T1 |  n  |  d1 |  d2 | ... |  T2 |  n  |  D1 |  D2 | ... | ... |
 *    +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * </pre>
 * 
 * 
 * 
 * @author jph
 * 
 */
public class BSDPMessage extends DHCPMessage {

	private BSDPOptions bsdpOptionsList = null;

	public static final byte BSDP_OPTION_MESSAGETYPE = 1;
	public static final byte BSDP_OPTION_VERSION = 2;
	public static final byte BSDP_OPTION_SERVERID = 3;
	public static final byte BSDP_OPTION_PRIORITY = 4;
	public static final byte BSDP_OPTION_REPLYPORT = 5;
	public static final byte BSDP_OPTION_BOOTLISTICONPATH = 6;
	public static final byte BSDP_OPTION_DEFAULTIMAGEID = 7;
	public static final byte BSDP_OPTION_SELECTEDBOOTIMAGEID = 8;
	public static final byte BSDP_OPTION_BOOTIMAGELIST = 9;
	public static final byte BSDP_OPTION_FIRMWAREVERSION = 10;
	public static final byte BSDP_OPTION_IMAGEATTRIBUTESFILTER = 11;

	public BSDPMessage() {
		setOption((byte) 60, "AAPLBSDPC".getBytes());
		bsdpOptionsList = new BSDPOptions();
	}

	@Override
	public synchronized byte[] externalize() {
		byte[] options = new byte[312];
		options = bsdpOptionsList.externalize();
		setOption((byte) 43, options);
		return super.externalize();
	}

	@Override
	public synchronized BSDPMessage internalize(byte[] ibuff) {
		super.internalize(ibuff);
		byte[] options = getOption((byte) 43);
		if (options != null) {
			bsdpOptionsList.internalize(options);
		}
		return this;
	}

	public void setBSDPOption(BSDPOption option) {
		bsdpOptionsList.setOption(option);
	}

	public BSDPOption getBDSPOption(byte inOptNum) {
		return bsdpOptionsList.getOption(inOptNum);
	}

	public void setDHCPOption(DHCPOption option) {
		setOption(option.getOptionNumber(), option.getEncodedData());
	}

	public DHCPOption getDHCPOption(byte inOptNum) {
		return DHCPOption.createBSDPOptionInstance(inOptNum,
				getOption(inOptNum));
	}

	/**
	 * Report whether or not the input option is set
	 * 
	 * @param inOptNum
	 *            option number
	 */
	/*
	 * DHCPMessage::IsOptSet Purpose: to return is a certain option is already set. Precondition: a option number to lookup and a output parameter to the index
	 * of it into. Postcodition: if option is found, true is returned and so is the index of that option in the options array. If it is not found, false is
	 * returned.
	 */
	public boolean IsBSDPOptSet(byte inOptNum) {
		return bsdpOptionsList.contains(inOptNum);
	}

	public String getServerName() {
		try {
			return new String(getSname(), "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Java does not support US-ASCII!?");
		}
	}

	public void setServerName(String serverName) {
		byte[] sName;
		try {
			sName = new String(serverName).getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Java does not support US-ASCII!?");
		}
		byte[] sNamePadded = new byte[64];
		if (sName.length > sNamePadded.length) {
			throw new IllegalArgumentException("Server Name too long.");
		}

		System.arraycopy(sName, 0, sNamePadded, 0, sName.length);

		setSname(sNamePadded);
	}

	public String getFilename() {
		try {
			return new String(getFile(), "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Java does not support US-ASCII!?");
		}
	}

	public void setFilename(String fileName) {
		byte[] file;
		try {
			file = new String(fileName).getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Java does not support US-ASCII!?");
		}
		byte[] filePadded = new byte[128];
		if (file.length > filePadded.length) {
			throw new IllegalArgumentException("Filename too long.");
		}

		System.arraycopy(file, 0, filePadded, 0, file.length);

		setFile(filePadded);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "{"
				+ super.toString() + "}[" + bsdpOptionsList.toString() + "]";
	}

}
