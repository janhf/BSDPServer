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

import java.util.HashMap;

public abstract class DHCPOption {

	public static void init() {
		String[] options = new String[] { "RootPath" };

		for (String s : options) {
			String classLoad = DHCPOption.class.getPackage().getName() + ".DHCPo" + s;
			// System.err.println("Loading " + classLoad + "...");
			try {
				Class.forName(classLoad);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	protected static void registerMessageType(int option, Class<? extends DHCPOption> c) {
		if (dhcpOptionPacketHandlers.containsValue(c)) {
			throw new IllegalStateException("Class " + c + " is already registered.");
		}
		dhcpOptionPacketHandlers.put(option, c);
	}

	public static DHCPOption createBSDPOptionInstance(int option, byte[] encodedData) {
		if (dhcpOptionPacketHandlers.containsKey(option)) {
			Class<? extends DHCPOption> optionClass = dhcpOptionPacketHandlers.get(option);
			try {
				DHCPOption optionObject = optionClass.newInstance();
				optionObject.setEncodedData(encodedData);
				return optionObject;
			} catch (InstantiationException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}

		} else
			return null;
	}

	private static HashMap<Integer, Class<? extends DHCPOption>> dhcpOptionPacketHandlers = new HashMap<Integer, Class<? extends DHCPOption>>();

	public DHCPOption() {
		super();
	}

	public abstract byte getOptionNumber();

	public abstract byte[] getEncodedData();

	public abstract void setEncodedData(byte[] encodedData);
}
