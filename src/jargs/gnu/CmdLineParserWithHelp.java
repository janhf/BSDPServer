package jargs.gnu;

import java.util.HashMap;
import java.util.Map.Entry;

public class CmdLineParserWithHelp extends CmdLineParser {
	private int longestLongForm = 0;
	private final HashMap<Option, String> optionHelpStrings = new HashMap<Option, String>();

	public Option addHelp(Option option, String helpString) {
		optionHelpStrings.put(option, helpString);
		if (option.longForm().length() > longestLongForm) {
			longestLongForm = option.longForm().length();
		}
		return option;
	}

	public String getUsage() {
		String out = "";
		if ("long Form".length() > longestLongForm) {
			longestLongForm = "long Form".length();
		}
		String header = "short\t" + pad("long Form", longestLongForm + 2)
				+ "\tDescription";
		out += header + "\n";
		out += pad("", header.length() + 20, '-') + "\n";
		for (Entry<Option, String> e : optionHelpStrings.entrySet()) {
			if (e.getKey().shortForm() != null) {
				out += " -" + e.getKey().shortForm() + "\t--"
						+ pad(e.getKey().longForm(), longestLongForm + 2)
						+ e.getValue() + "\n";
			} else {
				out += "   \t--"
						+ pad(e.getKey().longForm(), longestLongForm + 2)
						+ e.getValue() + "\n";
			}
		}
		out += "\n";
		return out;
	}

	public static String pad(String s, int size) {
		return pad(s, size, ' ');
	}

	public static String pad(String s, int size, char padChar) {
		if (s.length() > size) {
			return s.substring(0, size);
		} else if (s.length() < size) {
			while (s.length() < size) {
				s += padChar;
			}
			return s;
		} else {
			return s;
		}
	}
}
