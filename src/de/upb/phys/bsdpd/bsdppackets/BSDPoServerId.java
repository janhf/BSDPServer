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

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BSDPoServerId extends BSDPOption {

	public static final int NO = 3;
	static {
		registerMessageType(NO, BSDPoServerId.class);
	}

	private InetAddress id;

	public BSDPoServerId() {
		try {
			id = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public byte[] getEncodedData() {
		return id.getAddress();
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		try {
			id = InetAddress.getByAddress(encodedData);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public InetAddress getId() {
		return id;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[id=" + id
				+ "]";
	}
}
