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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import plistreader.AbstractReader;
import plistreader.PlistFactory;
import plistreader.PlistProperties;
import plistreader.PlistReaderException;

public class BootImage {

	public static enum ARCH {
		i386, ppc, x86_64, ia64;
	}

	public static enum TYPE {
		NFS, Classic, HTTP, BootFileOnly
	}

	public static enum KIND {
		MACOS9(0x00), MACOSX(0x01), MACOSXSERVER(0x02), HARDWAREDIAGNOSTICS(
				0x03), EFI_PROGRAMM(0x0D);

		public int value;

		KIND(int value) {
			this.value = value;
		}

		public byte calcByte0(boolean install) {
			byte ret = install ? (byte) 0x1 : (byte) 0x0;
			ret <<= 7;
			ret |= (byte) (value & 0x7F);
			return ret;
		}

		public static KIND getFromByte(byte b) {
			b = (byte) (b & 0x7F);
			switch (b) {
			case 0x00:
				return MACOS9;
			case 0x01:
				return MACOSX;
			case 0x02:
				return MACOSXSERVER;
			case 0x03:
				return HARDWAREDIAGNOSTICS;
			case 0x0D:
				return EFI_PROGRAMM;
			default:
				throw new IllegalArgumentException(
						"Cannot parse BootImageType!");
			}
		}
	}

	private final Logger l;
	private final List<ARCH> supportedArchitectures;
	private String bootFile;
	private String description;
	private final List<String> disabledSystemIdentifiers;
	private final List<String> enabledSystemIdentifiers;
	private int index;
	private boolean isDefault;
	private boolean isEnabled;
	private boolean isInstall;
	private KIND kind;
	private String language;
	private String name;
	private String rootPath;
	private boolean supportsDiskless;
	private TYPE type;
	private String osVersion;

	public BootImage(File bootImageInfoPlist) throws PlistReaderException {
		l = Logger.getLogger("bsdpd");
		AbstractReader reader = PlistFactory.createReader();
		PlistProperties props = reader.parse(bootImageInfoPlist);

		supportedArchitectures = new LinkedList<ARCH>();
		for (Object o : ((Vector<?>) props.getProperty("Architectures"))) {
			supportedArchitectures.add(ARCH.valueOf((String) o));
		}
		bootFile = (String) props.getProperty("BootFile");
		enabledSystemIdentifiers = new LinkedList<String>();
		disabledSystemIdentifiers = new LinkedList<String>();
		for (Object o : ((Vector<?>) props
				.getProperty("EnabledSystemIdentifiers"))) {
			addEnabledSystemIdentifier((String) o);
		}
		for (Object o : ((Vector<?>) props
				.getProperty("DisabledSystemIdentifiers"))) {
			addDisabledSystemIdentifier((String) o);
		}

		index = (Integer) props.getProperty("Index");
		isDefault = (Boolean) props.getProperty("IsDefault");
		isEnabled = (Boolean) props.getProperty("IsEnabled");
		isInstall = (Boolean) props.getProperty("IsInstall");
		kind = KIND.getFromByte((byte) (int) (Integer) props
				.getProperty("Kind"));
		language = (String) props.getProperty("Language");
		name = (String) props.getProperty("Name");
		supportsDiskless = (Boolean) props.getProperty("SupportsDiskless");
		type = TYPE.valueOf((String) props.getProperty("Type"));

		if (!type.equals(TYPE.BootFileOnly)) {
			rootPath = (String) props.getProperty("RootPath");
		}

		osVersion = (String) props.getProperty("osVersion");
	}

	private BootImage() {
		super();
		l = Logger.getLogger("bsdpd");
		supportedArchitectures = null;
		bootFile = "";
		description = "";
		disabledSystemIdentifiers = new LinkedList<String>();
		enabledSystemIdentifiers = new LinkedList<String>();
		index = 0;
		isDefault = false;
		isEnabled = false;
		isInstall = false;
		kind = null;
		language = "";
		name = "";
		rootPath = "";
		supportsDiskless = false;
		type = null;
		osVersion = "";
	}

	public byte[] getBSDPEncodedData() {
		byte[] data = new byte[4];
		data[0] = kind.calcByte0(isInstall);
		data[1] = 0x00;
		data[2] = (byte) ((index >> 8) & 0xFF);
		data[3] = (byte) (index & 0xFF);
		return data;
	}

	public static BootImage setBSDPEncodedData(byte[] data) {
		if (data.length == 4) {
			BootImage image = new BootImage();
			image.isInstall = ((data[0] & 0x80) >> 7) != 0;
			image.kind = KIND.getFromByte((byte) (data[0] & 0x7F));
			image.index = 0;
			image.index |= data[2] & 0xFF;
			image.index <<= 8;
			image.index |= data[3] & 0xFF;

			// Try to restore missing data from BootImageDatabase...
			BootImage imageDB = BootImageDatabase.bootImageDB.findImage(
					image.index, image.kind, image.isInstall);
			if (imageDB != null) {
				return imageDB;
			} else {
				return image;
			}
		} else {
			throw new IllegalArgumentException(
					"Wrong length for BootImageId byte data.");
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		if (index < 65535 && index >= 0) {
			this.index = index;
		} else {
			throw new IllegalArgumentException(
					"Index for BootImageId must be between 0 and 65535");
		}
	}

	public String getBootFile() {
		return bootFile;
	}

	public void setBootFile(String bootFile) {
		this.bootFile = bootFile;
	}

	public String getDescription() {
		if (description == null || description.trim().length() == 0) {
			return name;
		} else {
			return description;
		}
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
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

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public boolean isSupportsDiskless() {
		return supportsDiskless;
	}

	public void setSupportsDiskless(boolean supportsDiskless) {
		this.supportsDiskless = supportsDiskless;
	}

	public TYPE getType() {
		return type;
	}

	public void setType(TYPE type) {
		this.type = type;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public List<ARCH> listSupportedArchitectures() {
		return Collections.unmodifiableList(supportedArchitectures);
	}

	public void addSupportedArchitecture(ARCH architecture) {
		if (!supportedArchitectures.contains(architecture)) {
			supportedArchitectures.add(architecture);
		}
	}

	public List<String> listDisabledSystemIdentifiers() {
		return Collections.unmodifiableList(disabledSystemIdentifiers);
	}

	public void addDisabledSystemIdentifier(String systemIdentifier) {
		if (!disabledSystemIdentifiers.contains(systemIdentifier)) {
			disabledSystemIdentifiers.add(systemIdentifier);
		}
		enabledSystemIdentifiers.remove(systemIdentifier);
	}

	public List<String> listEnabledSystemIdentifiers() {
		return Collections.unmodifiableList(enabledSystemIdentifiers);
	}

	public void addEnabledSystemIdentifier(String systemIdentifier) {
		if (!enabledSystemIdentifiers.contains(systemIdentifier)) {
			enabledSystemIdentifiers.add(systemIdentifier);
		}
		disabledSystemIdentifiers.remove(systemIdentifier);
	}

	public void removeSystemIdentifer(String systemIdentifier) {
		if (disabledSystemIdentifiers.contains(systemIdentifier)) {
			disabledSystemIdentifiers.remove(systemIdentifier);
		}
		if (enabledSystemIdentifiers.contains(systemIdentifier)) {
			enabledSystemIdentifiers.remove(systemIdentifier);
		}
	}

	@Override
	public String toString() {
		// + ",disabledSystemIdentifiers=" + disabledSystemIdentifiers +
		// ",enabledSystemIdentifiers=" + enabledSystemIdentifiers +
		// ",isDefault=" + isDefault + ",isDefault="
		// + isEnabled + ",isInstall=" + isInstall + ",kind=" + kind
		// + ",language=" + language + ",osVersion=" + osVersion +
		// ",supportsDiskless=" + supportsDiskless + ",rootPath=" + rootPath
		// + "supportedArchitectures=" + supportedArchitectures
		// + ",type=" + type
		return getClass().getSimpleName() + "@" + hashCode() + "[index="
				+ index + ",bootFile=" + bootFile + ",description="
				+ description + ",name=" + name + "]";
	}
}
