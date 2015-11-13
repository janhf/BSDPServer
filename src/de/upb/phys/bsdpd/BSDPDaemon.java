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

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParserWithHelp;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

import de.upb.phys.bsdpd.imageDB.BootImage;
import de.upb.phys.bsdpd.imageDB.BootImageDatabase;

public class BSDPDaemon implements Daemon {
	private static final Logger l = Logger.getLogger("bsdpd");
	private DaemonContext context;
	private boolean startServer = false;
	private boolean acceptSocketTimeoutException = false;
	private Thread bsdpdServerThread;

	public BSDPDaemon() {

	}

	public void usage(CmdLineParserWithHelp parser) {
		l
				.log(
						Level.INFO,
						"Usage: bsdpd [--server] [-s] [-m serverName] [-p serverPath] [-t httpURL] [-a afpURL] [-n httpURL] [-o shadowURL]"
								+ "[--nosanitychecks [true|false]] [-d [OFF|INFO|WARNING|SEVERE|<numeric value>]]");
		l.log(Level.INFO, parser.getUsage());
		l
				.log(
						Level.INFO,
						"Start the server with --server to start the BSDP-Server. "
								+ "If you start the server with -s it displays the current settings and loadable images.");
		l.log(Level.INFO, "The settings are saved across restarts.");
	}

	@Override
	public void init(DaemonContext context) throws IllegalArgumentException {
		this.context = context;
		l
				.log(
						Level.INFO,
						"BSDP-Server Version 1.0, Copyright 2010 Theoretische Physik, Universität Paderborn");

		CmdLineParserWithHelp parser = new CmdLineParserWithHelp();

		CmdLineParser.Option serverOption = parser.addHelp(parser
				.addBooleanOption("server"), "Start the BSDP-Server");
		CmdLineParser.Option sanitychecksOption = parser.addHelp(parser
				.addStringOption("sanitychecks"),
				"Disables checking of the Boot-Images files");
		CmdLineParser.Option settingsOption = parser.addHelp(parser
				.addBooleanOption('s', "settings"),
				"Set the settings for the BSDP-Server");
		CmdLineParser.Option bootServerNameOption = parser.addHelp(parser
				.addStringOption('m', "serverName"),
				"Boot Server Name (TFTP-Server)");
		CmdLineParser.Option bootServerPathOption = parser
				.addHelp(
						parser.addStringOption('p', "serverPath"),
						"Boot Server Path Component (Directory with Apple's TFTP-Data). It is used to construct the path to the efi-Bootloader.");
		CmdLineParser.Option httpServerURLOption = parser.addHelp(parser
				.addStringOption('t', "httpURL"),
				"HTTP Server serving NetBootSP0 (e.g. http://srvr/NetBootSP0)");
		CmdLineParser.Option afpServerURLOption = parser.addHelp(parser
				.addStringOption('a', "afpURL"),
				"AFP Server serving NetBootSP0 (e.g. afp://srvr/NetBootSP0)");
		CmdLineParser.Option nfsServerURLOption = parser
				.addHelp(parser.addStringOption('n', "nfsURL"),
						"NFS Server serving NetBootSP0 (e.g. nfs:srvr:/local/NetBootSP0)");
		CmdLineParser.Option bootImageLocationOption = parser
				.addHelp(
						parser.addStringOption('l', "imageLocation"),
						"Directory containing the *.nbi directories (NetBootSP0) on the local pc. (e.g. /local/NetBootSP0)");
		CmdLineParser.Option shadowMountPathOption = parser.addHelp(parser
				.addStringOption('o', "shadowURL"),
				"Directory for the files for the diskless client.");
		CmdLineParser.Option shadowMountPathLocalOption = parser
				.addHelp(parser.addStringOption('j', "shadowPath"),
						"Directory for the files for the diskless client in the local filesystem.");
		CmdLineParser.Option logLevelOption = parser
				.addHelp(
						parser.addStringOption('d', "logLevel"),
						"Defines the verbosity of this Server. One of INFO, WARNING, SEVERE, or a numeric value.");

		CmdLineParser.Option help = parser.addHelp(parser.addBooleanOption('h',
				"help"), "Show this help message");

		try {
			parser.parse(context.getArguments());
		} catch (CmdLineParser.OptionException e) {
			throw new IllegalArgumentException("Unknown commandline options.",
					e);
		}

		if (Boolean.TRUE.equals(parser.getOptionValue(help))) {
			usage(parser);
			return;
		}

		Boolean serverOptionValue = (Boolean) parser
				.getOptionValue(serverOption);
		boolean server = serverOptionValue == null ? false : true;
		Boolean settingsOptionValue = (Boolean) parser
				.getOptionValue(settingsOption);
		boolean settings = settingsOptionValue == null ? false : true;

		// Extract the values entered for the various options -- if the
		// options were not specified, the corresponding values will be
		// null.
		String bootServerNameOptionValue = (String) parser
				.getOptionValue(bootServerNameOption);
		if (bootServerNameOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setBootServerName(bootServerNameOptionValue);
		}
		String bootServerPathOptionValue = (String) parser
				.getOptionValue(bootServerPathOption);
		if (bootServerPathOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setBootServerPath(bootServerPathOptionValue);
		}
		String bootImageLocationOptionValue = (String) parser
				.getOptionValue(bootImageLocationOption);
		if (bootImageLocationOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setBootImageLocation(bootImageLocationOptionValue);
		}
		String httpServerURLOptionValue = (String) parser
				.getOptionValue(httpServerURLOption);
		if (httpServerURLOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setHttpServerURL(httpServerURLOptionValue);
		}
		String afpServerURLOptionValue = (String) parser
				.getOptionValue(afpServerURLOption);
		if (afpServerURLOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setAfpServerURL(afpServerURLOptionValue);
		}
		String nfsServerURLOptionValue = (String) parser
				.getOptionValue(nfsServerURLOption);
		if (nfsServerURLOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setNfsServerURL(nfsServerURLOptionValue);
		}
		String shadowMountPathOptionValue = (String) parser
				.getOptionValue(shadowMountPathOption);
		if (shadowMountPathOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setShadowMountPath(shadowMountPathOptionValue);
		}
		String shadowMountPathLocalOptionValue = (String) parser
				.getOptionValue(shadowMountPathLocalOption);
		if (shadowMountPathLocalOptionValue != null) {
			BootImageDatabase.bootImageDB
					.setShadowMountPathLocal(shadowMountPathLocalOptionValue);
		}
		String logLevelOptionValue = (String) parser
				.getOptionValue(logLevelOption);
		if (logLevelOptionValue != null) {
			BootImageDatabase.bootImageDB.setLogLevel(logLevelOptionValue);
		}
		String sanitychecksOptionValue = (String) parser
				.getOptionValue(sanitychecksOption);
		if (sanitychecksOptionValue != null) {
			BootImageDatabase.bootImageDB.setSanityChecks(Boolean
					.parseBoolean(sanitychecksOptionValue));
		}

		if (settings) {
			printSettings();
		}

		startServer = server;
	}

	public void printSettings() {
		l.log(Level.INFO, "Current Configuration:");
		l.log(Level.INFO, "TFTP-Server:  "
				+ BootImageDatabase.bootImageDB.getBootServerName());
		l.log(Level.INFO, "Boot Server Name:  "
				+ BootImageDatabase.bootImageDB.getBootServerName());
		l.log(Level.INFO, "Boot Server Path:  "
				+ BootImageDatabase.bootImageDB.getBootServerPath());
		l.log(Level.INFO, "Image location:    "
				+ BootImageDatabase.bootImageDB.getBootImageLocation());
		l.log(Level.INFO, "HTTP Server:       "
				+ BootImageDatabase.bootImageDB.getHttpServerURL());
		l.log(Level.INFO, "AFP Server:        "
				+ BootImageDatabase.bootImageDB.getAfpServerURL());
		l.log(Level.INFO, "NFS Server:        "
				+ BootImageDatabase.bootImageDB.getNfsServerURL());
		l.log(Level.INFO, "Shadow Mount Path: "
				+ BootImageDatabase.bootImageDB.getShadowMountPath());
		l.log(Level.INFO, "Shadow Mount Path (Local): "
				+ BootImageDatabase.bootImageDB.getShadowMountPathLocal());
		l.log(Level.INFO, "SanityChecks:      "
				+ BootImageDatabase.bootImageDB.isSanityChecks());
		l.log(Level.INFO, "Log Level:         "
				+ BootImageDatabase.bootImageDB.getLogLevel());
		l.log(Level.INFO, "");
		l.log(Level.INFO, "Images:");
		l.log(Level.INFO, CmdLineParserWithHelp.pad("Name", 25) + "\t"
				+ CmdLineParserWithHelp.pad("Id", 5) + "\t"
				+ CmdLineParserWithHelp.pad("Kind", 7) + "\t"
				+ CmdLineParserWithHelp.pad("Install", 8) + "\t"
				+ CmdLineParserWithHelp.pad("Default", 8) + "\t"
				+ CmdLineParserWithHelp.pad("Enabled", 8) + "\t"
				+ CmdLineParserWithHelp.pad("Architectures", 12));
		l
				.log(
						Level.INFO,
						"=====================================================================================================================");
		for (BootImage image : BootImageDatabase.bootImageDB.listBootImages()) {
			l.log(Level.INFO, CmdLineParserWithHelp.pad(image.getName(), 25)
					+ "\t"
					+ CmdLineParserWithHelp.pad(Integer.toString(image
							.getIndex()), 5)
					+ "\t"
					+ CmdLineParserWithHelp.pad(image.getKind().toString(), 7)
					+ "\t"
					+ CmdLineParserWithHelp.pad(Boolean.toString(image
							.isInstall()), 8)
					+ "\t"
					+ CmdLineParserWithHelp.pad(Boolean.toString(image
							.isDefault()), 8)
					+ "\t"
					+ CmdLineParserWithHelp.pad(Boolean.toString(image
							.isEnabled()), 8)
					+ "\t"
					+ CmdLineParserWithHelp.pad(image
							.listSupportedArchitectures().toString(), 12));
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	public void start() throws Exception {
		l.log(Level.INFO, "Starting service...");
		if (startServer) {
			l.log(Level.INFO, "Creating service thread...");
			bsdpdServerThread = new Thread(new Runnable() {
				public void run() {
					try {
						BSDPServer.startMainLoop();
					} catch (SocketTimeoutException e) {
						if (!acceptSocketTimeoutException) {
							throw new RuntimeException(e);
						}
					} catch (SocketException e) {
						throw new RuntimeException(e.getMessage(), e);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			}, "BSDP Server Thread");
			bsdpdServerThread
					.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
						@Override
						public void uncaughtException(Thread t, Throwable e) {
							l.log(Level.FINE, "BSDP Server crashed. "
									+ e.getMessage(), e);
							context.getController().fail(
									new RuntimeException(e.getMessage(), e));
						}
					});
			bsdpdServerThread.start();
		} else {
			l.log(Level.INFO, "BSDP Server is not enabled.");
			throw new RuntimeException("BSDP Server is not enabled.");
		}

	}

	@Override
	public void stop() throws Exception {
		acceptSocketTimeoutException = true;
		BSDPServer.stopMainLoop();
		bsdpdServerThread.join();
	}
}
