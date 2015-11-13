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
 * Stores the path to the shadow mount (stores client specific data)<br/>
 * <br/>
 * It contains something like this:<br/>
 * afp://netboot001:10d7c947@10.0.1.4/NetBootClients3<br/>
 * <br/>
 * Were: netboot001 - Machine Accountname <br/>
 * 10d7c947 - Is the Machine Password <br/>
 * 10.0.1.4 - Is the Server <br/>
 * NetBootClients3 - Is the AFP Share<br/>
 * 
 * @author jph-local
 * 
 */
public class BSDPoShadowMountPath extends BSDPOption {

	public static final byte NO = (byte) 0x80;

	static {
		registerMessageType(NO, BSDPoShadowMountPath.class);
	}

	private String shadowMountPath;

	public BSDPoShadowMountPath() {
		shadowMountPath = null;
	}

	public BSDPoShadowMountPath(String shadowMountPath) {
		this.shadowMountPath = shadowMountPath;
	}

	@Override
	public byte[] getEncodedData() {
		try {
			return shadowMountPath.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		try {
			shadowMountPath = new String(encodedData, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public String getShadowMountPath() {
		return shadowMountPath;
	}

	public void setShadowMountPath(String shadowMountPath) {
		this.shadowMountPath = shadowMountPath;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[shadowMountPath=" + shadowMountPath + "]";
	}
}
