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

public class BSDPoReplyPort extends BSDPOption {

	public static final int NO = 5;

	static {
		registerMessageType(NO, BSDPoReplyPort.class);
	}

	private int replyPort;

	public BSDPoReplyPort() {
		replyPort = 0;
	}

	@Override
	public byte[] getEncodedData() {
		return new byte[] { (byte) ((replyPort >> 8) & 0xFF),
				(byte) (replyPort & 0xFF) };
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		if (encodedData.length == 2) {
			replyPort = 0;
			replyPort |= encodedData[0] & 0xFF;
			replyPort <<= 8;
			replyPort |= encodedData[1] & 0xFF;
		} else {
			throw new IllegalArgumentException(
					"Array length does not match expected size.");
		}
	}

	public int getReplyPort() {
		return replyPort;
	}

	public void setReplyPort(int replyPort) {
		if (replyPort < 1024 && replyPort >= 0) {
			this.replyPort = replyPort;
		} else {
			throw new IllegalArgumentException(
					" 0<=replyPort<1024 is not fulfilled!");
		}
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[replyPort="
				+ replyPort + "]";
	}
}
