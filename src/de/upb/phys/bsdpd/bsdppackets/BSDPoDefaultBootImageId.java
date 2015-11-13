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

import de.upb.phys.bsdpd.imageDB.BootImage;

public class BSDPoDefaultBootImageId extends BSDPOption {

	public static final int NO = 7;

	static {
		registerMessageType(NO, BSDPoDefaultBootImageId.class);
	}

	private BootImage defaultBootImageId;

	public BSDPoDefaultBootImageId() {
		defaultBootImageId = null;
	}

	public BSDPoDefaultBootImageId(BootImage defaultBootImageId) {
		this.defaultBootImageId = defaultBootImageId;
	}

	@Override
	public byte[] getEncodedData() {
		return defaultBootImageId.getBSDPEncodedData();
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		defaultBootImageId = BootImage.setBSDPEncodedData(encodedData);
	}

	public BootImage getDefaultBootImageId() {
		return defaultBootImageId;
	}

	public void setDefaultBootImageId(BootImage defaultBootImageId) {
		this.defaultBootImageId = defaultBootImageId;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[defaultBootImageId=" + defaultBootImageId + "]";
	}

}
