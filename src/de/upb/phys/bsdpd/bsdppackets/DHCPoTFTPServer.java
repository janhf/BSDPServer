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
package de.upb.phys.bsdpd.bsdppackets;

import java.io.UnsupportedEncodingException;

/**
 * Stores an icon for display in the apple bootloader
 * 
 * @author jph-local
 * 
 */
public class DHCPoTFTPServer extends DHCPOption {

	public static final int NO = 66;

	static {
		registerMessageType(NO, DHCPoTFTPServer.class);
	}

	private String tftpServerName;

	public DHCPoTFTPServer() {
		tftpServerName = null;
	}

	public DHCPoTFTPServer(String tftpServerName) {
		this.tftpServerName = tftpServerName;
	}

	@Override
	public byte[] getEncodedData() {
		try {
			return tftpServerName.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		try {
			tftpServerName = new String(encodedData, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public String getTftpServerName() {
		return tftpServerName;
	}

	public void setTftpServerName(String tftpServerName) {
		this.tftpServerName = tftpServerName;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[tftpServerName=" + tftpServerName + "]";
	}
}
