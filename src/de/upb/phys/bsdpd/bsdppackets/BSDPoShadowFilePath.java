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
 * Is something like:<br/>
 * NetBoot001/Shadow <br/>
 * 
 * Where: <br/>
 * NetBoot001 - ?<br/>
 * Shadow - ?<br/>
 * 
 * @author jph-local
 * 
 */
public class BSDPoShadowFilePath extends BSDPOption {

	public static final byte NO = (byte) 0x81;

	static {
		registerMessageType(NO, BSDPoShadowFilePath.class);
	}

	private String shadowFilePath;

	public BSDPoShadowFilePath() {
		shadowFilePath = null;
	}

	public BSDPoShadowFilePath(String shadowFilePath) {
		this.shadowFilePath = shadowFilePath;
	}

	@Override
	public byte[] getEncodedData() {
		try {
			return shadowFilePath.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		try {
			shadowFilePath = new String(encodedData, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public String getShadowFilePath() {
		return shadowFilePath;
	}

	public void setShadowFilePath(String shadowFilePath) {
		this.shadowFilePath = shadowFilePath;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[shadowFilePath=" + shadowFilePath + "]";
	}
}
