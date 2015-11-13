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

public class BSDPoVersion extends BSDPOption {

	public static final int NO = 2;

	static {
		registerMessageType(NO, BSDPoVersion.class);
	}

	private byte versionMajor;
	private byte versionMinor;

	public BSDPoVersion() {
		versionMajor = 1;
		versionMinor = 1;
	}

	@Override
	public byte[] getEncodedData() {
		return new byte[] { versionMajor, versionMinor };
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		if (encodedData.length == 2) {
			versionMinor = encodedData[1];
			versionMajor = encodedData[0];
		} else {
			throw new IllegalArgumentException(
					"Array length does not match format!");
		}
	}

	public byte getVersionMinor() {
		return versionMinor;
	}

	public void setVersionMinor(byte versionMinor) {
		this.versionMinor = versionMinor;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[versionMinor="
				+ versionMinor + ", versionMajor=" + versionMajor + "]";
	}
}
