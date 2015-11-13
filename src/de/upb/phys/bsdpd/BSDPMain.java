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

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;

public class BSDPMain implements DaemonController {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		BSDPDaemon daemon = new BSDPDaemon();
		daemon.init(new DaemonContext() {

			@Override
			public DaemonController getController() {
				return new BSDPMain();
			}

			@Override
			public String[] getArguments() {
				return args;
			}
		});
		daemon.start();

		Thread.sleep(3600000);

		daemon.stop();

	}

	@Override
	public void fail() throws IllegalStateException {
		System.err.println("FAIL CALLED!");
		System.exit(2);
	}

	@Override
	public void fail(String message) throws IllegalStateException {
		System.err.println("FAIL CALLED: " + message + "!");
		System.exit(2);
	}

	@Override
	public void fail(Exception exception) throws IllegalStateException {
		System.err.println("FAIL CALLED: " + exception.getMessage() + "!");
		exception.printStackTrace();
		System.exit(2);
	}

	@Override
	public void fail(String message, Exception exception)
			throws IllegalStateException {
		System.err.println("FAIL CALLED: " + message + "/"
				+ exception.getMessage() + "!");
		exception.printStackTrace();
		System.exit(2);
	}

	@Override
	public void reload() throws IllegalStateException {
		System.err.println("RELOAD?");
	}

	@Override
	public void shutdown() throws IllegalStateException {
		System.err.println("SHUTDOWN?");
	}
}
