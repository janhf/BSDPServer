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

public class BSDPoMessageType extends BSDPOption {

	public static final int NO = 1;

	static {
		registerMessageType(NO, BSDPoMessageType.class);
	}

	public static enum TYPES {
		LIST(1), SELECT(2), FAILED(3);

		public byte i;

		TYPES(int i) {
			this.i = (byte) i;
		}

		public static TYPES valueOf(byte b) {
			switch (b) {
			case 1:
				return TYPES.LIST;
			case 2:
				return TYPES.SELECT;
			case 3:
				return TYPES.FAILED;
			default:
				return null;
			}
		}
	}

	private TYPES type;

	public BSDPoMessageType() {
		type = null;
	}

	public BSDPoMessageType(TYPES type) {
		this.type = type;
	}

	@Override
	public byte[] getEncodedData() {
		return new byte[] { type.i };
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		if (encodedData.length == 1) {
			type = TYPES.valueOf(encodedData[0]);
		} else {
			throw new IllegalArgumentException(
					"Length of MessageType must be 1");
		}
	}

	public TYPES getType() {
		return type;
	}

	public void setType(TYPES type) {
		this.type = type;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[type="
				+ type.toString() + "]";
	}
}
