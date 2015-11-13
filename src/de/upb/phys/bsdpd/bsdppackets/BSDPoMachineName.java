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
 * Stores the path to the shadow file (stores client specific data)
 * 
 * @author jph-local
 * 
 */
public class BSDPoMachineName extends BSDPOption {

	public static final byte NO = (byte) 0x82;

	static {
		registerMessageType(NO, BSDPoMachineName.class);
	}

	private String machineName;

	public BSDPoMachineName() {
		machineName = null;
	}

	public BSDPoMachineName(String machineName) {
		this.machineName = machineName;
	}

	@Override
	public byte[] getEncodedData() {
		try {
			return machineName.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		try {
			machineName = new String(encodedData, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[machineName="
				+ machineName + "]";
	}
}
