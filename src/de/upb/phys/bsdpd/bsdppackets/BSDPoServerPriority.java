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

public class BSDPoServerPriority extends BSDPOption {

	public static final int NO = 4;

	static {
		registerMessageType(NO, BSDPoServerPriority.class);
	}

	private int priority;

	public BSDPoServerPriority() {
		priority = 0;
	}

	public BSDPoServerPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public byte[] getEncodedData() {
		return new byte[] { (byte) ((priority >> 8) & 0xFF),
				(byte) (priority & 0xFF) };
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		if (encodedData.length == 2) {
			priority = 0;
			priority |= encodedData[0] & 0xFF;
			priority <<= 8;
			priority |= encodedData[1] & 0xFF;
		} else {
			throw new IllegalArgumentException(
					"Array length does not match expected size.");
		}
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		if (priority <= 65535 && priority >= 0) {
			this.priority = priority;
		} else {
			throw new IllegalArgumentException(
					" 0<=priority<=65535 is not fulfilled!");
		}
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[priority="
				+ priority + "]";
	}
}
