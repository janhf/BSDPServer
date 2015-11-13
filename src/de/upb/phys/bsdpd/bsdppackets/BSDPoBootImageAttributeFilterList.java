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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.upb.phys.bsdpd.imageDB.BootImage.KIND;

/**
 * Stores an icon for display in the apple bootloader
 * 
 * @author jph-local
 * 
 */
public class BSDPoBootImageAttributeFilterList extends BSDPOption {

	public static final int NO = 11;

	static {
		registerMessageType(NO, BSDPoBootImageAttributeFilterList.class);
	}

	private final LinkedList<BootImageFilter> filters;

	public BSDPoBootImageAttributeFilterList() {
		filters = new LinkedList<BootImageFilter>();
	}

	@Override
	public byte[] getEncodedData() {
		byte[] filterBytes = new byte[filters.size() * 2];
		int posBytes = 0;
		int posList = 0;
		while (posBytes < filterBytes.length) {
			byte[] currentFilter = filters.get(posList).getBSDPEncodedData();
			filterBytes[posBytes] = currentFilter[0];
			filterBytes[posBytes + 1] = currentFilter[1];

			posList++;
			posBytes += 2;
		}
		return filterBytes;
	}

	@Override
	public void setEncodedData(byte[] encodedData) {
		if (encodedData.length % 2 != 0) {
			throw new IllegalArgumentException("Data length mod 2 must be 0");
		}

		int pos = 0;
		while (pos < encodedData.length) {
			byte[] filterBytes = new byte[2];
			filterBytes[0] = encodedData[pos + 0];
			filterBytes[1] = encodedData[pos + 1];
			BootImageFilter filter = new BootImageFilter();
			filter.setBSDPEncodedData(filterBytes);
			filters.add(filter);

			pos += 2;
		}
	}

	public List<BootImageFilter> listFilters() {
		return Collections.unmodifiableList(filters);
	}

	@Override
	public byte getOptionNumber() {
		return NO;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + hashCode() + "[filters="
				+ Arrays.toString(filters.toArray()) + "]";
	}

	public static class BootImageFilter {
		private boolean isInstall;
		private KIND kind;

		public BootImageFilter() {
			super();
			this.isInstall = false;
			this.kind = null;
		}

		public BootImageFilter(boolean isInstall, KIND kind) {
			super();
			this.isInstall = isInstall;
			this.kind = kind;
		}

		public void setBSDPEncodedData(byte[] filterBytes) {
			if (filterBytes.length == 2) {
				isInstall = ((filterBytes[0] & 0x80) >> 7) != 0;
				kind = KIND.getFromByte((byte) (filterBytes[0] & 0x7F));
			} else {
				throw new IllegalArgumentException("Length of data is  not 2!");
			}
		}

		public byte[] getBSDPEncodedData() {
			byte[] data = new byte[4];
			data[0] = kind.calcByte0(isInstall);
			data[1] = 0x00;
			return data;
		}

		public boolean isInstall() {
			return isInstall;
		}

		public void setInstall(boolean isInstall) {
			this.isInstall = isInstall;
		}

		public KIND getKind() {
			return kind;
		}

		public void setKind(KIND kind) {
			this.kind = kind;
		}
	}
}
