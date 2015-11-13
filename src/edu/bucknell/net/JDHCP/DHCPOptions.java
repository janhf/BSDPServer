package edu.bucknell.net.JDHCP;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;

/**
 * This class represents a linked list of options for a DHCP message. Its
 * purpose is to ease option handling such as add, remove, or change.
 * 
 * @author Jason Goldschmidt
 * @version 1.1.1 9/06/1999
 */
public class DHCPOptions {

	/**
	 *This inner class represent an entry in the Option Table
	 */
	class DHCPOptionsEntry extends Object {
		protected byte code;
		protected byte length;
		protected byte content[];

		public DHCPOptionsEntry(byte entryCode, byte entryLength,
				byte entryContent[]) {
			code = entryCode;
			length = entryLength;
			content = entryContent;
		}
	}

	private Hashtable<Byte, DHCPOptionsEntry> optionsTable = null;

	public DHCPOptions() {
		optionsTable = new Hashtable<Byte, DHCPOptionsEntry>();
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

	/**
	 * Returns true if option code is set in list; false otherwise
	 * 
	 * @param entryCode
	 *            The node's option code
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
	public byte[] getOption(byte entryCode) {
		if (this.contains(entryCode)) {
			DHCPOptionsEntry ent = optionsTable.get(new Byte(entryCode));
			return ent.content;
		} else {
			return null;
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
	public void setOption(byte entryCode, byte value[]) {
		DHCPOptionsEntry opt = new DHCPOptionsEntry(entryCode,
				(byte) value.length, value);
		optionsTable.put(new Byte(entryCode), opt);
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
		int pos = 4; // ignore vendor magic cookie
		byte code, length;
		byte value[];

		while (optionsArray[pos] != (byte) 255) { // until end option
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
		for (Entry<Byte, DHCPOptionsEntry> e : optionsTable.entrySet()) {
			byteCount++; // Identifier Byte
			byteCount++; // Length Byte
			byteCount += e.getValue().content.length; // Data length
		}
		if (byteCount > 311) {
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
		int length = checkLength() + 5;
		if (length < 5) {
			length = 5;
		}
		byte[] options = new byte[length];

		options[0] = (byte) 99; // insert vendor magic cookie
		options[1] = (byte) 130;
		options[2] = (byte) 83;
		options[3] = (byte) 99;

		int position = 4;
		for (Enumeration<DHCPOptionsEntry> e = optionsTable.elements(); e
				.hasMoreElements();) {
			DHCPOptionsEntry entry = e.nextElement();
			options[position++] = entry.code;
			options[position++] = entry.length;
			for (int i = 0; i < (0x0000FF & entry.length); ++i) {
				options[position++] = entry.content[i];
			}
		}
		options[position] = (byte) 255; // insert end option
		return options;
	}

	@Override
	public String toString() {
		String entries = "";
		for (Enumeration<DHCPOptionsEntry> e = optionsTable.elements(); e
				.hasMoreElements();) {
			DHCPOptionsEntry entry = e.nextElement();
			switch (entry.code) {
			case 17:
				entries += "id=ROOT_PATH,path=" + new String(entry.content)
						+ ";";
			case 43:
				// entries += "id=VENDOR_OPTIONS,length=" + entry.length + ",data=" + Arrays.toString(entry.content) + ";";
				break;
			case 53:
				entries += "id=DHCP_MESSAGE_TYPE,data=";
				switch (entry.content[0]) {
				case 1:
					entries += "DHCPDISCOVER;";
					break;
				case 2:
					entries += "DHCPOFFER;";
					break;
				case 3:
					entries += "DHCPREQUEST;";
					break;
				case 4:
					entries += "DHCPDECLINE;";
					break;
				case 5:
					entries += "DHCPACK;";
					break;
				case 6:
					entries += "DHCPNAK;";
					break;
				case 7:
					entries += "DHCPRELEASE;";
					break;
				case 8:
					entries += "DHCPINFORM;";
					break;
				default:
					entries += entry.content[0] + ";";
					break;
				}
				break;
			case 57:
				int messageSize = 0;
				messageSize |= entry.content[0] & 0xFF;
				messageSize <<= 8;
				messageSize |= entry.content[1] & 0xFF;

				entries += "id=DHCP_MESSAGE_SIZE,data=" + messageSize + ";";
				break;
			case 60:
				entries += "id=VENDOR_CLASS_IDENTIFIER,data="
						+ new String(entry.content) + ";";
				break;
			default:
				entries += "id=" + entry.code + ",length=" + entry.length
						+ ",data=" + Arrays.toString(entry.content) + ";";
				break;
			}

		}
		return getClass().getSimpleName() + "@" + hashCode() + "[" + entries
				+ "]";
	}
}
