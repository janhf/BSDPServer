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

public class BSDPoSelectedBootImageId extends BSDPOption {

	public static final int NO = 8;

	static {
		registerMessageType(NO, BSDPoSelectedBootImageId.class);
	}

	private BootImage selectedBootImageId;

	public BSDPoSelectedBootImageId() {
		selectedBootImageId = null;
	}

	public BSDPoSelectedBootImageId(BootImage selectedBootImageId) {
		this.selectedBootImageId = selectedBootImageId;
	}

	@Override
	public byte[] getEncodedData() {
		return selectedBootImageId.getBSDPEncodedData();
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		selectedBootImageId = BootImage.setBSDPEncodedData(encodedData);
	}

	public BootImage getSelectedBootImageId() {
		return selectedBootImageId;
	}

	public void setSelectedBootImageId(BootImage selectedBootImageId) {
		this.selectedBootImageId = selectedBootImageId;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[selectedBootImageId=" + selectedBootImageId + "]";
	}
}
