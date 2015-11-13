package edu.bucknell.net.JDHCP;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * This class represents a Socket for sending DHCP Messages
 * 
 * @author Jason Goldschmidt
 * @version 1.1.1 9/06/1999
 * @see java.net.DatagramSocket
 */

public class DHCPSocket extends DatagramSocket {

	static protected int packetSize = 1500; // default MTU for ethernet
	private final int DEFAULT_SOTIME_OUT = 3000; // 3 second socket timeout

	/**
	 * Constructor for creating DHCPSocket on a specific port on the local
	 * machine.
	 * 
	 * @param inPort
	 *            the port for the application to bind.
	 */
	public DHCPSocket(int inPort) throws SocketException {

		super(inPort);
		setSoTimeout(DEFAULT_SOTIME_OUT); // set default time out
	}

	/**
	 * Sets the Maximum Transfer Unit for the UDP DHCP Packets to be set.
	 * Default is 1500, MTU for Ethernet
	 * 
	 * @param inSize
	 *            integer representing desired MTU
	 */
	public void setMTU(int inSize) {
		packetSize = inSize;
	}

	/**
	 * Returns the set MTU for this socket
	 * 
	 * @return the Maximum Transfer Unit set for this socket
	 */
	public int getMTU() {
		return packetSize;
	}

	/**
	 * Sends a DHCPMessage object to a predifined host.
	 * 
	 * @param inMessage
	 *            well-formed DHCPMessage to be sent to a server
	 */
	public synchronized void send(DHCPMessage inMessage)
			throws java.io.IOException {
		byte data[] = new byte[packetSize];
		data = inMessage.externalize();
		InetAddress dest = null;
		try {
			dest = InetAddress.getByName(inMessage.getDestinationAddress());
		} catch (UnknownHostException e) {
		}

		DatagramPacket outgoing = new DatagramPacket(data, data.length, dest,
				inMessage.getPort());
		//gSocket.
		send(outgoing); // send outgoing message
	}

	/**
	 * Receives a datagram packet containing a DHCP Message into a DHCPMessage
	 * object.
	 * 
	 * @return true if message is received, false if timeout occurs.
	 * @param outMessage
	 *            DHCPMessage object to receive new message into
	 */
	public synchronized boolean receive(DHCPMessage outMessage) {
		try {
			DatagramPacket incoming = new DatagramPacket(new byte[packetSize],
					packetSize);
			//gSocket.
			receive(incoming); // block on receive for SO_TIMEOUT

			outMessage.internalize(incoming.getData());
		} catch (java.io.IOException e) {
			return false;
		} // end catch    
		return true;
	}

}
