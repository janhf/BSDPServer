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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.upb.phys.bsdpd.bsdppackets.BSDPOption;
import de.upb.phys.bsdpd.bsdppackets.BSDPoBootImageAttributeFilterList;
import de.upb.phys.bsdpd.bsdppackets.BSDPoBootImageList;
import de.upb.phys.bsdpd.bsdppackets.BSDPoDefaultBootImageId;
import de.upb.phys.bsdpd.bsdppackets.BSDPoMessageType;
import de.upb.phys.bsdpd.bsdppackets.BSDPoReplyPort;
import de.upb.phys.bsdpd.bsdppackets.BSDPoSelectedBootImageId;
import de.upb.phys.bsdpd.bsdppackets.BSDPoServerId;
import de.upb.phys.bsdpd.bsdppackets.BSDPoServerPriority;
import de.upb.phys.bsdpd.bsdppackets.DHCPOption;
import de.upb.phys.bsdpd.bsdppackets.BSDPoBootImageAttributeFilterList.BootImageFilter;
import de.upb.phys.bsdpd.bsdppackets.BSDPoMessageType.TYPES;
import de.upb.phys.bsdpd.imageDB.BootImage;
import de.upb.phys.bsdpd.imageDB.BootImageDatabase;
import de.upb.phys.bsdpd.imageDB.BootImage.ARCH;
import edu.bucknell.net.JDHCP.DHCPMessage;
import edu.bucknell.net.JDHCP.DHCPSocket;

public class BSDPServer {

	private static BSDPServer server;
	private boolean running = true;
	private Thread bsdpdThread = null;

	private static final Logger l = Logger.getLogger("bsdpd");

	public BSDPServer() {

	}

	private List<BSDPMessage> handleBSDPMessage(BSDPMessage bsdpMessage) {
		switch (bsdpMessage.getOption((byte) 0x35)[0]) { // DHCP Message Type
		case 5:// ACK
			switch (((BSDPoMessageType) bsdpMessage
					.getBDSPOption(BSDPMessage.BSDP_OPTION_MESSAGETYPE))
					.getType()) { // BSDP Message Type
			case LIST:// LIST
				break;
			case SELECT:// SELECT
				break;
			case FAILED:// FAILED
				break;
			}
			break;
		case 8:// INFORM
			switch (((BSDPoMessageType) bsdpMessage
					.getBDSPOption(BSDPMessage.BSDP_OPTION_MESSAGETYPE))
					.getType()) { // BSDP Message Type
			case LIST:// LIST
				return answerInformList(bsdpMessage);
			case SELECT:// SELECT
				return answerInformSelect(bsdpMessage);
			case FAILED:// FAILED
				break;
			}
			break;
		}
		return new LinkedList<BSDPMessage>();
	}

	private List<BSDPMessage> answerInformList(BSDPMessage bsdpMessage) {
		List<BSDPMessage> answerPackages = new LinkedList<BSDPMessage>();

		String[] vendorclass = new String(bsdpMessage.getOption((byte) 60))
				.split("/");
		if (vendorclass.length != 3) {
			l.log(Level.INFO, " ~> vendorclass format not known.");
			return null;
		}
		String vendor = vendorclass[0];
		String architectureString = vendorclass[1];
		ARCH arch = ARCH.valueOf(architectureString);
		String systemIdentifier = vendorclass[2];

		// Acquire information from database...
		List<BootImage> bootImagesToSend = new LinkedList<BootImage>();
		if (bsdpMessage
				.IsBSDPOptSet(BSDPMessage.BSDP_OPTION_IMAGEATTRIBUTESFILTER)) {
			BSDPoBootImageAttributeFilterList filterList = (BSDPoBootImageAttributeFilterList) bsdpMessage
					.getBDSPOption(BSDPMessage.BSDP_OPTION_IMAGEATTRIBUTESFILTER);

			for (BootImageFilter filter : filterList.listFilters()) {
				List<BootImage> filteredBootImages = BootImageDatabase.bootImageDB
						.findBootableImages(arch, systemIdentifier, filter);
				bootImagesToSend.addAll(filteredBootImages);
			}
		} else {
			bootImagesToSend = BootImageDatabase.bootImageDB
					.findBootableImages(arch, systemIdentifier);
		}

		String macAddress = macAddressToString(bsdpMessage.getChaddr());
		BootImage defaultBootImage = BootImageDatabase.bootImageDB
				.getLastSelectedImage(macAddress, arch, systemIdentifier);

		byte[] senderAddress = bsdpMessage.getCiaddr();

		BSDPoReplyPort replyPortOption = ((BSDPoReplyPort) bsdpMessage
				.getBDSPOption(BSDPMessage.BSDP_OPTION_REPLYPORT));
		int answerPort = replyPortOption == null ? DHCPMessage.CLIENT_PORT
				: replyPortOption.getReplyPort();

		// Counter for placing the BootImage records.
		int bootImageListPos = 0;

		/*
		 * Construct packet with: - BSDP Boot Image List Path - BSDP Default Boot Image - BSDP Server Identifier
		 */
		{
			BSDPMessage answerMessage1 = new BSDPMessage();
			answerMessage1.setDestinationHost(DHCPMessage
					.ipaddrToString(senderAddress));
			answerMessage1.setPort(answerPort);// Set replyport/answerport
			answerMessage1.setOp((byte) 0x2);
			answerMessage1.setHtype(bsdpMessage.getHtype());
			answerMessage1.setHlen(bsdpMessage.getHlen());
			answerMessage1.setHops(bsdpMessage.getHops());
			answerMessage1.setXid(bsdpMessage.getXid());
			answerMessage1.setSecs(bsdpMessage.getSecs());
			answerMessage1.setFlags(bsdpMessage.getFlags());
			answerMessage1.setCiaddr(bsdpMessage.getCiaddr());
			answerMessage1.setYiaddr(new byte[4]);
			answerMessage1.setSiaddr(new BSDPoServerId().getId().getAddress());
			answerMessage1.setGiaddr(bsdpMessage.getGiaddr());
			answerMessage1.setChaddr(bsdpMessage.getChaddr());

			answerMessage1.setOption((byte) 53, new byte[] { DHCPMessage.ACK });
			answerMessage1.setOption((byte) 54, new BSDPoServerId().getId()
					.getAddress());
			try {
				answerMessage1.setOption((byte) 60, new String("AAPLBSDPC")
						.getBytes("US-ASCII"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}

			answerMessage1.setBSDPOption(new BSDPoMessageType(TYPES.LIST));// Set BSDP Message Type to LIST
			answerMessage1.setBSDPOption(new BSDPoServerPriority(500));
			answerMessage1.setBSDPOption(new BSDPoServerId());

			BSDPoBootImageList bootImagesSent = new BSDPoBootImageList();
			if (defaultBootImage != null) {
				answerMessage1.setBSDPOption(new BSDPoDefaultBootImageId(
						defaultBootImage)); // BSDP Default Boot Image ID (Section 3.4.2)
				bootImagesSent.addBootImage(defaultBootImage, defaultBootImage
						.getDescription());
				bootImagesToSend.remove(defaultBootImage);
			}

			int i = 0;
			for (; i < 1 && i + bootImageListPos < bootImagesToSend.size(); i++) {
				bootImagesSent.addBootImage(bootImagesToSend
						.get(bootImageListPos + i), bootImagesToSend.get(
						bootImageListPos + i).getDescription());
			}

			if (bootImagesSent.listBootImages().size() != 0) {
				answerMessage1.setBSDPOption(bootImagesSent);
			}
			bootImageListPos += i;

			answerPackages.add(answerMessage1);
		}

		// Send all other BootImages in another packet with 2 images per Packet...
		while (bootImageListPos < bootImagesToSend.size()) {
			BSDPMessage answerMessage2 = new BSDPMessage();
			answerMessage2.setDestinationHost(DHCPMessage
					.ipaddrToString(senderAddress));
			answerMessage2.setPort(answerPort);// Set replyport/answerport
			answerMessage2.setOp((byte) 0x2);
			answerMessage2.setHtype(bsdpMessage.getHtype());
			answerMessage2.setHlen(bsdpMessage.getHlen());
			answerMessage2.setHops(bsdpMessage.getHops());
			answerMessage2.setXid(bsdpMessage.getXid());
			answerMessage2.setSecs(bsdpMessage.getSecs());
			answerMessage2.setFlags(bsdpMessage.getFlags());
			answerMessage2.setCiaddr(bsdpMessage.getCiaddr());
			answerMessage2.setYiaddr(new byte[4]);
			answerMessage2.setSiaddr(new BSDPoServerId().getId().getAddress());
			answerMessage2.setGiaddr(bsdpMessage.getGiaddr());
			answerMessage2.setChaddr(bsdpMessage.getChaddr());

			answerMessage2.setOption((byte) 53, new byte[] { DHCPMessage.ACK });
			answerMessage2.setOption((byte) 54, new BSDPoServerId().getId()
					.getAddress());
			try {
				answerMessage2.setOption((byte) 60, new String("AAPLBSDPC")
						.getBytes("US-ASCII"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}

			answerMessage2.setCiaddr(bsdpMessage.getCiaddr());
			answerMessage2.setXid(bsdpMessage.getXid());
			answerMessage2.setBSDPOption(new BSDPoMessageType(TYPES.LIST));// Set BSDP Message Type to LIST
			answerMessage2.setBSDPOption(new BSDPoServerId());

			BSDPoBootImageList bootImageList2 = new BSDPoBootImageList();
			int i = 0;
			for (; i < 2 && i + bootImageListPos < bootImagesToSend.size(); i++) {
				bootImageList2.addBootImage(bootImagesToSend
						.get(bootImageListPos + i), bootImagesToSend.get(
						bootImageListPos + i).getDescription());
			}
			bootImageListPos += i;
			answerMessage2.setBSDPOption(bootImageList2);
			answerPackages.add(answerMessage2);
		}

		return answerPackages;
	}

	private List<BSDPMessage> answerInformSelect(BSDPMessage bsdpMessage) {
		String[] vendorclass = new String(bsdpMessage.getOption((byte) 60))
				.split("/");
		if (vendorclass.length != 3) {
			l.log(Level.INFO, " ~> vendorclass format not known.");
			return null;
		}
		String vendor = vendorclass[0];
		String architectureString = vendorclass[1];
		ARCH arch = ARCH.valueOf(architectureString);
		String systemIdentifier = vendorclass[2];

		// Acquire information from database...
		List<BootImage> bootImages = BootImageDatabase.bootImageDB
				.findBootableImages(arch, systemIdentifier);
		BootImage selectedImage = ((BSDPoSelectedBootImageId) bsdpMessage
				.getBDSPOption(BSDPMessage.BSDP_OPTION_SELECTEDBOOTIMAGEID))
				.getSelectedBootImageId();
		for (BootImage image : bootImages) {
			if (image.getIndex() == selectedImage.getIndex()) {
				return answerInformSelectACK(bsdpMessage);
			}
		}
		return answerInformSelectFAILED(bsdpMessage);
	}

	private List<BSDPMessage> answerInformSelectACK(BSDPMessage bsdpMessage) {
		List<BSDPMessage> answerMessages = new LinkedList<BSDPMessage>();
		BSDPoReplyPort replyPortOption = ((BSDPoReplyPort) bsdpMessage
				.getBDSPOption(BSDPMessage.BSDP_OPTION_REPLYPORT));
		int answerPort = replyPortOption == null ? DHCPMessage.CLIENT_PORT
				: replyPortOption.getReplyPort();

		String[] vendorclass = new String(bsdpMessage.getOption((byte) 60))
				.split("/");
		if (vendorclass.length != 3) {
			l.log(Level.INFO, " ~> vendorclass format not known.");
			return null;
		}
		String vendor = vendorclass[0];
		String architectureString = vendorclass[1];
		ARCH arch = ARCH.valueOf(architectureString);
		String systemIdentifier = vendorclass[2];

		// Save settings in the database...
		String macAddress = macAddressToString(bsdpMessage.getChaddr());
		BootImage selectedImage = ((BSDPoSelectedBootImageId) bsdpMessage
				.getBDSPOption(BSDPMessage.BSDP_OPTION_SELECTEDBOOTIMAGEID))
				.getSelectedBootImageId();
		BootImageDatabase.bootImageDB.setLastSelectedImage(macAddress,
				selectedImage.getIndex());

		BSDPMessage answerMessage1 = new BSDPMessage();
		{
			answerMessage1.setDestinationHost(DHCPMessage
					.ipaddrToString(bsdpMessage.getCiaddr()));
			answerMessage1.setPort(answerPort);// Set replyport/answerport
			answerMessage1.setOp((byte) 0x2);
			answerMessage1.setHtype(bsdpMessage.getHtype());
			answerMessage1.setHlen(bsdpMessage.getHlen());
			answerMessage1.setHops(bsdpMessage.getHops());
			answerMessage1.setXid(bsdpMessage.getXid());
			answerMessage1.setSecs(bsdpMessage.getSecs());
			answerMessage1.setFlags(bsdpMessage.getFlags());
			answerMessage1.setCiaddr(bsdpMessage.getCiaddr());
			answerMessage1.setYiaddr(new byte[4]);
			answerMessage1.setSiaddr(new BSDPoServerId().getId().getAddress());
			answerMessage1.setGiaddr(bsdpMessage.getGiaddr());
			answerMessage1.setChaddr(bsdpMessage.getChaddr());
			answerMessage1.setServerName(BootImageDatabase.bootImageDB
					.getBootServerName(selectedImage));
			answerMessage1.setFilename(BootImageDatabase.bootImageDB
					.getBootServerFile(selectedImage, arch));

			answerMessage1.setOption((byte) 53, new byte[] { DHCPMessage.ACK });
			answerMessage1.setOption((byte) 54, new BSDPoServerId().getId()
					.getAddress());
			try {
				answerMessage1.setOption((byte) 60, new String("AAPLBSDPC")
						.getBytes("US-ASCII"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
			for (DHCPOption option : BootImageDatabase.bootImageDB
					.generateExtraDHCPBootOptions(selectedImage, macAddress)) {
				answerMessage1.setDHCPOption(option);
			}

			answerMessage1.setBSDPOption(new BSDPoMessageType(TYPES.SELECT));// Set BSDP Message Type to LIST
			answerMessage1
					.setBSDPOption(bsdpMessage
							.getBDSPOption(BSDPMessage.BSDP_OPTION_SELECTEDBOOTIMAGEID));
			for (BSDPOption option : BootImageDatabase.bootImageDB
					.generateExtraBDSPBootOptions(selectedImage, macAddress)) {
				answerMessage1.setBSDPOption(option);
			}
		}
		answerMessages.add(answerMessage1);

		return answerMessages;
	}

	private List<BSDPMessage> answerInformSelectFAILED(BSDPMessage bsdpMessage) {
		List<BSDPMessage> answerMessages = new LinkedList<BSDPMessage>();
		BSDPoReplyPort replyPortOption = ((BSDPoReplyPort) bsdpMessage
				.getBDSPOption(BSDPMessage.BSDP_OPTION_REPLYPORT));
		int answerPort = replyPortOption == null ? DHCPMessage.CLIENT_PORT
				: replyPortOption.getReplyPort();

		BSDPMessage answerMessage1 = new BSDPMessage();
		{
			answerMessage1.setDestinationHost(DHCPMessage
					.ipaddrToString(bsdpMessage.getCiaddr()));
			answerMessage1.setPort(answerPort);// Set replyport/answerport
			answerMessage1.setOp((byte) 0x2);
			answerMessage1.setHtype(bsdpMessage.getHtype());
			answerMessage1.setHlen(bsdpMessage.getHlen());
			answerMessage1.setHops(bsdpMessage.getHops());
			answerMessage1.setXid(bsdpMessage.getXid());
			answerMessage1.setSecs(bsdpMessage.getSecs());
			answerMessage1.setFlags(bsdpMessage.getFlags());
			answerMessage1.setCiaddr(bsdpMessage.getCiaddr());
			answerMessage1.setYiaddr(new byte[4]);
			answerMessage1.setSiaddr(new BSDPoServerId().getId().getAddress());
			answerMessage1.setGiaddr(bsdpMessage.getGiaddr());
			answerMessage1.setChaddr(bsdpMessage.getChaddr());

			answerMessage1.setOption((byte) 53, new byte[] { DHCPMessage.ACK });
			answerMessage1.setOption((byte) 54, new BSDPoServerId().getId()
					.getAddress());
			try {
				answerMessage1.setOption((byte) 60, new String("AAPLBSDPC")
						.getBytes("US-ASCII"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}

			answerMessage1.setBSDPOption(new BSDPoMessageType(TYPES.FAILED));// Set BSDP Message Type to LIST
			answerMessages.add(answerMessage1);
		}

		return answerMessages;
	}

	private void mainLoop() throws SocketException {
		l.log(Level.FINE, "In Main loop");
		DHCPSocket mySocket = new DHCPSocket(DHCPMessage.SERVER_PORT); // create socket
		l.log(Level.INFO, "Receiving...");
		while (running) {
			BSDPMessage bsdpMessage = new BSDPMessage();
			if (mySocket.receive(bsdpMessage)) {
				try {
					l.log(Level.INFO, "--> " + bsdpMessage.toString());
					if (bsdpMessage
							.IsBSDPOptSet(BSDPMessage.BSDP_OPTION_VERSION)) {
						l.log(Level.INFO, " ~> Got a BSDP Packet handling...");
						List<BSDPMessage> bsdpAnswers = handleBSDPMessage(bsdpMessage);
						if (bsdpAnswers != null && bsdpAnswers.size() != 0) {
							for (BSDPMessage bsdpAnswer : bsdpAnswers) {
								if (bsdpAnswer != null) {
									try {
										mySocket.send(bsdpAnswer);
										l.log(Level.INFO, "<-- "
												+ bsdpAnswer.toString());
									} catch (IOException e) {
										l.log(Level.INFO,
												" ~> Couldn't send packet.");
										e.printStackTrace();
									}
								}
							}
						} else {
							l.log(Level.INFO, " ~> Couldn't handle packet.");
						}
					}
				} catch (Exception e) {
					l.log(Level.SEVERE, "Error while handling receive!");
					e.printStackTrace();
				}
			} else {
				l.log(Level.FINEST, "Timeout!");
			}
		}
	}

	private static String macAddressToString(byte[] addr) {
		String ret = "[";
		for (byte b : addr) {
			ret += Integer.toHexString((0xFF & b)) + ":";
		}
		return ret.substring(0, ret.length() - 1) + "]";
	}

	public static void startMainLoop() throws ClassNotFoundException,
			SocketException, SocketTimeoutException {
		l.log(Level.INFO, "Starting bsdpd...");
		server = new BSDPServer();
		l.log(Level.FINE, "Setting running flag...");
		BSDPServer.server.running = true;
		l.log(Level.FINE, "Getting handle to current thread...");
		BSDPServer.server.bsdpdThread = Thread.currentThread();

		l.log(Level.FINE, "Loading classes...");
		Class.forName(BSDPOptions.class.getCanonicalName());
		Class.forName(BootImageDatabase.class.getCanonicalName());
		l.log(Level.FINE, "Calling main loop...");
		BSDPServer.server.mainLoop();

		// java.net.BindException l.log(Level.SEVERE,
		// "Socket Bind Error: Another process is bound to this port or you do not have access to bind a process to this port."
	}

	public static void stopMainLoop() {
		BSDPServer.server.running = false;
		BSDPServer.server.bsdpdThread.interrupt();
	}

}
