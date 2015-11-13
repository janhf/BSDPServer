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
package de.upb.phys.bsdpd;

// $Id: DHCPOptions.java,v 1.2 1999/09/07 03:00:02 jgoldsch Exp $

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;

import de.upb.phys.bsdpd.bsdppackets.BSDPOption;
import de.upb.phys.bsdpd.bsdppackets.DHCPOption;

/**
 * This class represents a linked list of options for a DHCP message. Its
 * purpose is to ease option handling such as add, remove, or change.
 * 
 * @author Jan-Philipp H�lshoff
 * @version 1.0 7/17/2009
 */
public class BSDPOptions {

	static {
		BSDPOption.init();
		DHCPOption.init();
	}

	/**
	 *This inner class represent an entry in the Option Table
	 */

	class BSDPOptionsEntry extends Object {
		protected byte code;
		protected byte length;
		protected byte content[];

		public BSDPOptionsEntry(byte entryCode, byte entryLength,
				byte entryContent[]) {
			code = entryCode;
			length = entryLength;
			content = entryContent;
		}
	}

	private Hashtable<Byte, BSDPOptionsEntry> optionsTable = null;

	public BSDPOptions() {
		optionsTable = new Hashtable<Byte, BSDPOptionsEntry>();
	}

	/**
	 * Removes option with specified bytecode
	 * 
	 * @param entryCode
	 *            The code of option to be removed
	 */

	public void removeOption(byte entryCode) {
		optionsTable.remove(new Byte(entryCode));
	}

	/*
	 * Returns true if option code is set in list; false otherwise
	 * 
	 * @param entryCode The node's option code
	 * 
	 * @return true if option is set, otherwise false
	 */
	public boolean contains(byte entryCode) {
		return optionsTable.containsKey(new Byte(entryCode));
	}

	/**
	 * Determines if list is empty
	 * 
	 * @return true if there are no options set, otherwise false
	 */
	public boolean isEmpty() {
		return optionsTable.isEmpty();
	}

	/**
	 * Fetches value of option by its option code
	 * 
	 * @param entryCode
	 *            The node's option code
	 * @return byte array containing the value of option entryCode. null is
	 *         returned if option is not set.
	 */
	private byte[] getOption(byte entryCode) {
		if (this.contains(entryCode)) {
			BSDPOptionsEntry ent = optionsTable.get(new Byte(entryCode));
			return ent.content;
		} else {
			return null;
		}
	}

	/**
	 * Fetches value of option by its option code
	 * 
	 * @param entryCode
	 *            The node's option code
	 * @return Object containing the value of option entryCode.
	 */
	public BSDPOption getOption(int inOptNum) {
		byte[] data = getOption((byte) inOptNum);
		if (data == null) {
			return null;
		} else {
			return BSDPOption.createBSDPOptionInstance(inOptNum, data);
		}
	}

	/**
	 * Changes an existing option to new value
	 * 
	 * @param entryCode
	 *            The node's option code
	 * @param value
	 *            [] Content of node option
	 */
	private void setOption(byte entryCode, byte value[]) {
		BSDPOptionsEntry opt = new BSDPOptionsEntry(entryCode,
				(byte) value.length, value);
		optionsTable.put(new Byte(entryCode), opt);
	}

	/**
	 * Changes an existing option to new value
	 * 
	 * @param option
	 *            The node's option
	 */
	public void setOption(BSDPOption option) {
		setOption(option.getOptionNumber(), option.getEncodedData());
	}

	/**
	 * Returns the option value of a specified option code in a byte array
	 * 
	 * @param length
	 *            Length of option content
	 * @param position
	 *            Location in array of option node
	 * @param options
	 *            [] The byte array of options
	 * @return byte array containing the value for the option
	 */
	private byte[] getArrayOption(int length, int position, byte options[]) {
		byte value[] = new byte[length];
		for (int i = 0; i < length; i++) {
			value[i] = options[position + i];
		}
		return value;
	}

	/**
	 * Converts an options byte array to a linked list
	 * 
	 * @param optionsArray
	 *            [] The byte array representation of the options list
	 */
	public void internalize(byte[] optionsArray) {

		/* Assume options valid and correct */
		int pos = 0;
		byte code, length;
		byte value[];

		while (optionsArray.length > pos && optionsArray[pos] != (byte) 255) { // until end option
			code = optionsArray[pos++];
			length = optionsArray[pos++];
			value = getArrayOption(length, pos, optionsArray);
			setOption(code, value);
			pos += length; // increment position pointer
		}
	}

	/**
	 * Check the length of the Option.
	 * 
	 * @return Current length with header and payload.
	 */
	public int checkLength() {
		int byteCount = 0;
		for (Entry<Byte, BSDPOptionsEntry> e : optionsTable.entrySet()) {
			byteCount++; // Identifier Byte
			byteCount++; // Length Byte
			byteCount += e.getValue().content.length; // Data length
		}
		if (byteCount > 255) {
			throw new IllegalStateException(
					"BSDP Vendor Option payload must be less or equal than 255 bytes.");
		}
		return byteCount;
	}

	/**
	 * Converts a linked options list to a byte array
	 * 
	 * @return array representation of optionsTable
	 */
	// todo provide overflow return
	public byte[] externalize() {
		int length = checkLength() + 1;
		byte[] options = new byte[length];

		int position = 0;
		for (Enumeration<BSDPOptionsEntry> e = optionsTable.elements(); e
				.hasMoreElements();) {
			BSDPOptionsEntry entry = e.nextElement();
			options[position++] = entry.code;
			options[position++] = entry.length;
			for (int i = 0; i < entry.length; ++i) {
				options[position++] = entry.content[i];
			}
		}
		options[position] = (byte) 255; // insert end option
		return options;
	}

	@Override
	public String toString() {
		String entries = "";
		for (Enumeration<BSDPOptionsEntry> e = optionsTable.elements(); e
				.hasMoreElements();) {
			BSDPOptionsEntry entry = e.nextElement();
			try {
				BSDPOption option = BSDPOption.createBSDPOptionInstance(
						entry.code, entry.content);
				if (option != null) {
					entries += BSDPOption.createBSDPOptionInstance(entry.code,
							entry.content).toString()
							+ ",";
				} else {
					entries += entry.code + ": unknown error null,";
				}
			} catch (Throwable t) {
				System.err.println(t.getMessage());
				t.printStackTrace();
				entries += "id=" + entry.code + ":length=" + entry.length
						+ ":data=" + Arrays.toString(entry.content) + ",";
			}

		}
		return getClass().getSimpleName() + "@" + hashCode() + "[" + entries
				+ "]";
	}
}
