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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import de.upb.phys.bsdpd.imageDB.BootImage;

/**
 * Encapsulates a list with Bootable Images
 * 
 * @author jph-local
 * 
 */
public class BSDPoBootImageList extends BSDPOption {

	public static final int NO = 9;

	static {
		registerMessageType(NO, BSDPoBootImageList.class);
	}

	private final HashMap<BootImage, String> bootImageList;

	public BSDPoBootImageList() {
		bootImageList = new HashMap<BootImage, String>();
	}

	public void addBootImage(BootImage id, String description) {
		try {
			if (description.getBytes("UTF-8").length > 254) {
				throw new IllegalArgumentException("Description too long.");
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Java does not support UTF-8!?", e);
		}
		bootImageList.put(id, description);
	}

	@Override
	public byte[] getEncodedData() {
		LinkedList<Byte> listOfBytesReturn = new LinkedList<Byte>();

		for (Entry<BootImage, String> e : bootImageList.entrySet()) {
			// Write out bootImageId
			for (byte b : e.getKey().getBSDPEncodedData()) {
				listOfBytesReturn.add(b);
			}

			// Convert string...
			byte[] bytesString;
			try {
				bytesString = e.getValue().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e1) {
				throw new IllegalStateException(
						"Java does not support UTF-8!?", e1);
			}
			listOfBytesReturn.add((byte) bytesString.length);
			for (byte b : bytesString) {
				listOfBytesReturn.add(b);
			}
		}

		byte[] bytesReturn = new byte[listOfBytesReturn.size()];
		for (int i = 0; i < bytesReturn.length; i++) {
			bytesReturn[i] = listOfBytesReturn.get(i);
		}

		return bytesReturn;
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		bootImageList.clear();
		int pos = 0;
		while (pos < encodedData.length) {
			byte[] bootImageIdBytes = new byte[4];
			bootImageIdBytes[0] = encodedData[pos + 0];
			bootImageIdBytes[1] = encodedData[pos + 1];
			bootImageIdBytes[2] = encodedData[pos + 2];
			bootImageIdBytes[3] = encodedData[pos + 3];
			BootImage bootImageId = BootImage
					.setBSDPEncodedData(bootImageIdBytes);
			pos += 4;

			int lengthString = encodedData[pos];
			pos++;

			String bootImageDescription = null;
			try {
				bootImageDescription = new String(encodedData, pos,
						lengthString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(
						"UTF-8 Encoding in Java not supported!?", e);
			}
			pos += lengthString;

			bootImageList.put(bootImageId, bootImageDescription);

			if (encodedData.length - pos < 6 && encodedData.length - pos > 0) {
				throw new IllegalArgumentException(
						"There is something wrong. There is not enough space for a next BSDP ListItem!");
			}
		}
	}

	public HashMap<BootImage, String> listBootImages() {
		return bootImageList;
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode()
				+ "[bootImageList=" + bootImageList.toString() + "]";
	}
}
