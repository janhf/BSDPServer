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
package de.upb.phys.bsdpd.imageDB;

import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import de.upb.phys.bsdpd.bsdppackets.BSDPOption;
import de.upb.phys.bsdpd.bsdppackets.BSDPoMachineName;
import de.upb.phys.bsdpd.bsdppackets.BSDPoShadowFilePath;
import de.upb.phys.bsdpd.bsdppackets.BSDPoShadowMountPath;
import de.upb.phys.bsdpd.bsdppackets.DHCPOption;
import de.upb.phys.bsdpd.bsdppackets.DHCPoRootPath;
import de.upb.phys.bsdpd.bsdppackets.BSDPoBootImageAttributeFilterList.BootImageFilter;
import de.upb.phys.bsdpd.imageDB.BootImage.ARCH;
import de.upb.phys.bsdpd.imageDB.BootImage.KIND;

public class BootImageDatabase {

	public static final BootImageDatabase bootImageDB = new BootImageDatabase();

	private String bootServerName;
	private String bootServerPath;
	private String httpServerURL;
	private String shadowMountPath;
	private String shadowMountPathLocal;
	private String afpServerURL;
	private String nfsServerURL;
	private String bootImageLocation;
	private boolean sanityChecks;
	private final Logger l;

	private final List<BootImage> bootImages;
	private final Preferences prefs;

	public BootImageDatabase() {
		prefs = Preferences.userNodeForPackage(BootImageDatabase.class);
		l = Logger.getLogger("bsdpd");
		l.setLevel(Level.parse(prefs.get("logLevel", "ALL")));

		bootImages = new LinkedList<BootImage>();

		String myIp = "";
		try {
			// String myFqdn =
			// InetAddress.getLocalHost().getCanonicalHostName();
			myIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.err.println("Could'nt find own hostname &/or ip.");
		}

		bootServerName = prefs.get("bootServerName", myIp);
		bootServerPath = prefs.get("bootServerPath", "apple");
		httpServerURL = prefs.get("httpServerURL", "http://" + myIp
				+ "/NetBootSP0");
		afpServerURL = prefs.get("afpServerURL", "afp://" + myIp
				+ "/NetBootSP0");
		shadowMountPath = prefs.get("shadowMountPath", "afp://" + myIp
				+ "/NetBootClients0");
		nfsServerURL = prefs.get("nfsServerURL", "nfs:" + myIp
				+ ":/local/system/NetBootSP0");
		sanityChecks = prefs.getBoolean("sanityChecks", true);
		bootImageLocation = prefs.get("bootImageLocation",
				"/srv/netboot/NetbootSP0");

		l.log(Level.INFO, "Using the following database config: "
				+ "bootServerName=" + bootServerName + ",bootServerPath="
				+ bootServerPath + ",httpServerURL=" + httpServerURL
				+ ",afpServerURL=" + afpServerURL + ",bootImageLocation="
				+ bootImageLocation + ",sanityChecks=" + sanityChecks
				+ ",shadowMountPath=" + shadowMountPath);

		l.log(Level.INFO, "Loading Boot Images from disk...");
		// Loading
		File bootImageDir = new File(bootImageLocation);
		if (!bootImageDir.exists()) {
			l.log(Level.SEVERE, "bootImageLocation does not exist!");
			return;
		}
		if (!bootImageDir.isDirectory()) {
			l.log(Level.SEVERE, "bootImageLocation is not a Directory!");
			return;
		}

		// find directories with Images...
		File[] imageDirectories = bootImageDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		// Test directories for NBImageInfo.plist
		for (File imageDirectory : imageDirectories) {
			l.log(Level.INFO, "Found directory: "
					+ imageDirectory.getAbsolutePath());
			File imageInfoPlist = new File(imageDirectory, "NBImageInfo.plist");
			try {
				BootImage image = new BootImage(imageInfoPlist);
				l.log(Level.INFO, "Found image: " + image.toString());
				// Do some sanity checks before adding...
				if (sanityChecks) {
					String imageDirectoryName = image.getName() + ".nbi";
					if (!imageDirectoryName.equals(imageDirectory.getName())) {
						l
								.log(
										Level.WARNING,
										"Image "
												+ imageDirectory.getName()
												+ " does not match given image name in config file. I won't use it.");
						continue;
					}

					// Check bootFile exists
					boolean bootFileOk = true;
					for (ARCH arch : image.listSupportedArchitectures()) {
						File efiBootFile = new File(imageDirectory, arch
								.toString()
								+ File.separatorChar + image.getBootFile());
						if (!efiBootFile.exists()) {
							l.log(Level.WARNING, "Image "
									+ imageDirectory.getName() + ": Boot file "
									+ image.getBootFile()
									+ " does not exist for architecture "
									+ arch.toString());
							bootFileOk = false;
						}
					}
					if (!bootFileOk) {
						l.log(Level.WARNING, "Image "
								+ imageDirectory.getName()
								+ ": Boot Files are not ok. I won't use it.");
						continue;
					}

					// Check disk image
					if (image.getRootPath() != null
							&& image.getRootPath().trim().length() != 0) {
						File diskImageFile = new File(imageDirectory, image
								.getRootPath());
						if (!diskImageFile.exists()) {
							l.log(Level.WARNING, "Image "
									+ imageDirectory.getName()
									+ ": Disk image file "
									+ image.getRootPath()
									+ " does not exist. I won't use it.");
							bootFileOk = false;
						}
					}
				}

				bootImages.add(image);
				l.log(Level.INFO, "Image " + imageDirectory.getName()
						+ "loaded.");
			} catch (Exception e) {
				l.log(Level.WARNING, "Image loading failed. " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public BootImage findDefaultImage(ARCH arch, String systemIdentifier) {
		for (BootImage image : bootImages) {
			if (image.listSupportedArchitectures().contains(arch)
					&& image.listEnabledSystemIdentifiers().contains(
							systemIdentifier) && image.isDefault()
					&& image.isEnabled()) {
				return image;
			}
		}
		return null;
	}

	public List<BootImage> findBootableImages(ARCH arch, String systemIdentifier) {
		List<BootImage> list = new LinkedList<BootImage>();
		for (BootImage image : bootImages) {
			if (image.listSupportedArchitectures().contains(arch)
					&& image.listEnabledSystemIdentifiers().contains(
							systemIdentifier) && image.isEnabled()) {
				list.add(image);
			}
		}
		return list;
	}

	public List<BootImage> findBootableImages(ARCH arch,
			String systemIdentifier, BootImageFilter filter) {
		List<BootImage> list = new LinkedList<BootImage>();
		for (BootImage image : bootImages) {
			if (image.listSupportedArchitectures().contains(arch)
					&& image.listEnabledSystemIdentifiers().contains(
							systemIdentifier) && image.isEnabled()
					&& image.isInstall() == filter.isInstall()
					&& image.getKind() == filter.getKind()) {
				list.add(image);
			}
		}
		return list;
	}

	public BootImage findImage(int index, KIND kind, boolean isInstall) {
		for (BootImage image : bootImages) {
			if (image.getIndex() == index && image.getKind().equals(kind)
					&& image.isInstall() == isInstall) {
				return image;
			}
		}
		return null;
	}

	public BSDPOption[] generateExtraBDSPBootOptions(BootImage image,
			String macAddress) {
		if (image.isSupportsDiskless()) {
			String readableMac = macToReadableFileSystemString(macAddress);

			// Set the shadow file path and shadow mount path option...
			BSDPOption[] bsdpOptions = new BSDPOption[3];
			bsdpOptions[0] = new BSDPoShadowFilePath(prefs.get("pcSetting."
					+ macAddress + ".shadowFilePath", "mac-" + readableMac
					+ "/ShadowFile"));
			//We need to create  the directory for the shadow file...
			createShadowDirectory("mac-" + readableMac);

			bsdpOptions[1] = new BSDPoShadowMountPath(prefs.get("pcSetting."
					+ macAddress + ".shadowMountPath", shadowMountPath));
			bsdpOptions[2] = new BSDPoMachineName("mac-" + readableMac);
			return bsdpOptions;
		} else {
			return new BSDPOption[0];
		}
	}

	private void createShadowDirectory(String readableMac) {
		File shadowDir = new File(getShadowMountPathLocal());
		if (!shadowDir.isDirectory()) {
			System.err.println("Error Shadow Mount Path (Local) not found!");
			return;
		}

		//Create the directory...
		File clientSpecificDir = new File(shadowDir, readableMac);
		if (!clientSpecificDir.mkdir()) {
			System.err
					.println("Could not create the directory for the shadow file for client "
							+ readableMac + "!");
			return;
		}
		//Set the dir world writable...
		//TODO: Give away a special user which has permission to write to this dir only...
		clientSpecificDir.setWritable(true, false);

		//TODO: Create shadow file with a specific size...
	}

	public DHCPOption[] generateExtraDHCPBootOptions(BootImage image,
			String macAddress) {
		String rootPath = getRootPath(image);
		if (rootPath != null) {
			return new DHCPOption[] { new DHCPoRootPath(rootPath) };
		} else {
			return new DHCPOption[] {};
		}
	}

	public String getBootServerName(BootImage image) {
		return bootServerName;
	}

	public String getRootPath(BootImage image) {
		String url = "";
		switch (image.getType()) {
		case Classic:
			url += afpServerURL + "/";
			break;
		case HTTP:
			url += httpServerURL + "/";
			break;
		case NFS:
			url += nfsServerURL + ":";
			break;
		case BootFileOnly:
			// We do not have a "rootPath"-Option on BootFileOnly nbi's
			return null;
		}
		url += image.getName() + ".nbi" + File.separatorChar
				+ image.getRootPath();

		return url;
	}

	public String getBootServerFile(BootImage image, ARCH arch) {
		return bootServerPath + File.separatorChar + image.getName() + ".nbi"
				+ File.separatorChar + arch.toString() + File.separatorChar
				+ image.getBootFile();
	}

	public void setLastSelectedImage(String macAddress, int imageIndex) {
		prefs.putInt("pcSetting." + macAddress, imageIndex);
	}

	public BootImage getLastSelectedImage(String macAddress, ARCH arch,
			String systemIdentifer) {
		BootImage defaultImage = findDefaultImage(arch, systemIdentifer);
		int imageIndex = prefs.getInt("pcSetting." + macAddress, 0);
		for (BootImage image : bootImages) {
			if (image.listSupportedArchitectures().contains(arch)
					&& image.listEnabledSystemIdentifiers().contains(
							systemIdentifer) && image.isEnabled()
					&& imageIndex == image.getIndex()) {
				defaultImage = image;
			}
		}
		return defaultImage;
	}

	public String getBootServerName() {
		return bootServerName;
	}

	public void setBootServerName(String bootServerName) {
		prefs.put("bootServerName", bootServerName);
		this.bootServerName = bootServerName;
	}

	public String getBootServerPath() {
		return bootServerPath;
	}

	public void setBootServerPath(String bootServerPath) {
		prefs.put("bootServerPath", bootServerPath);
		this.bootServerPath = bootServerPath;
	}

	public String getHttpServerURL() {
		return httpServerURL;
	}

	public void setHttpServerURL(String httpServerURL) {
		prefs.put("httpServerURL", httpServerURL);
		this.httpServerURL = httpServerURL;
	}

	public String getAfpServerURL() {
		return afpServerURL;
	}

	public void setAfpServerURL(String afpServerURL) {
		prefs.put("afpServerURL", afpServerURL);
		this.afpServerURL = afpServerURL;
	}

	public String getNfsServerURL() {
		return nfsServerURL;
	}

	public void setNfsServerURL(String nfsServerURL) {
		prefs.put("nfsServerURL", nfsServerURL);
		this.nfsServerURL = nfsServerURL;
	}

	public String getLogLevel() {
		return prefs.get("logLevel", "ALL");
	}

	public void setLogLevel(String logLevel) {
		prefs.put("logLevel", logLevel);
		l.setLevel(Level.parse(logLevel));
	}

	public boolean isSanityChecks() {
		return sanityChecks;
	}

	public void setSanityChecks(boolean sanityChecks) {
		prefs.putBoolean("sanityChecks", sanityChecks);
		this.sanityChecks = sanityChecks;
	}

	public String getBootImageLocation() {
		return bootImageLocation;
	}

	public void setBootImageLocation(String bootImageLocation) {
		prefs.put("bootImageLocation", bootImageLocation);
		this.bootImageLocation = bootImageLocation;
	}

	public String getShadowMountPath() {
		return shadowMountPath;
	}

	public void setShadowMountPath(String shadowMountPath) {
		prefs.put("shadowMountPath", shadowMountPath);
		this.shadowMountPath = shadowMountPath;
	}

	public String getShadowMountPathLocal() {
		return shadowMountPathLocal;
	}

	public void setShadowMountPathLocal(String shadowMountPathLocal) {
		prefs.put("shadowMountPathLocal", shadowMountPathLocal);
		this.shadowMountPathLocal = shadowMountPathLocal;
	}

	public List<BootImage> listBootImages() {
		return Collections.unmodifiableList(bootImages);
	}

	private static String macToReadableFileSystemString(String macAddress) {
		macAddress = macAddress.replace(':', '-');
		macAddress = macAddress.replaceAll("\\[", "");
		macAddress = macAddress.replaceAll("\\]", "");
		macAddress = macAddress.replaceAll("-0", "");
		return macAddress;
	}
}
