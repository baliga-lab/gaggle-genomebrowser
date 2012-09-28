package org.systemsbiology.util;


public class StringUtils {
	
	public static String quote(String s) {
		if (s==null) return "\"null\"";
		return "\"" + s + "\"";
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static String join(String sep, String... strings) {
		StringBuilder sb = new StringBuilder();
		if (strings.length > 0) {
			sb.append(strings[0]);
		}
		for (int i=1; i<strings.length; i++) {
			while (sb.length() > sep.length() && sep.equals(sb.substring(sb.length() - sep.length())))
				sb.setLength(sb.length() - sep.length());
			sb.append(sep);
			String s = strings[i];
			while (s.startsWith(sep))
				s = s.substring(sep.length());
			sb.append(s);
		}
		return sb.toString();
	}

	/**
	 * A simple-minded method of capitalizing the first letter of
	 * words in a string. Will transform "a title of something" into
	 * "A Title Of Something".
	 */
	public static String toTitleCase(String string) {
		if (string == null) return null;
		StringBuilder sb = new StringBuilder(string);
		boolean capitalize = true;
		for (int i=0; i<sb.length(); i++) {
			if (Character.isWhitespace(sb.charAt(i))) {
				capitalize = true;
			}
			else {
				if (capitalize) {
					sb.setCharAt(i, Character.toTitleCase(sb.charAt(i)));
					capitalize = false;
				}
			}
		}
		return sb.toString();
	}

	public static String toStringSeparateLines(Iterable<? extends Object> list) {
		StringBuilder sb = new StringBuilder("[\n");
		for (Object o : list) {
			sb.append(String.valueOf(o)).append("\n");
		}
		sb.append("]\n");
		return sb.toString();
	}


	/**
	 * Returns the given string if it can be parsed as a double. Throws NumberFormatException otherwise.
	 */
	public static String validateDouble(String string) {
		string = string.trim();
		Double.parseDouble(string);
		return string;
	}

	public static String nullToEmptyString(String string) {
		if (string == null) 
			return "";
		else
			return string;
	}

	public static boolean isNullOrEmpty(String string) {
		return string==null || "".equals(string.trim());
	}

	public static String underline(String string) {
		if (string==null) return null;
		StringBuilder sb = new StringBuilder(string);
		sb.append("\n");
		sb.append(line(string.length()));
		return sb.toString();
	}
	
	public static String line(int len) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<len; i++) {
			sb.append("-");
		}
		return sb.toString();
	}

	public static String htmlEscape(CharSequence s) {
		StringBuffer sb = new StringBuffer(s.length());
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"')
				sb.append("&quot;");
			else if (c == '\'')
				sb.append("&#39;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '&')
				sb.append("&amp;");
			// nothing special
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Trim each member of an array of Strings
	 * @return a new array holding trimmed strings or null if input was null
	 */
	public static String[] trim(String[] strings) {
		if (strings==null) return null;
		String[] results = new String[strings.length];
		for (int i=0; i<strings.length; i++) {
			results[i] = strings[i]==null ? null : strings[i].trim();
		}
		return results;
	}

	/**
	 * Test if the target string is in the array of strings.
	 */
	public static boolean in(String target, String[] strings) {
		if (target==null || strings==null) return false;
		for (String s : strings) {
			if (s!=null && target.equals(s)) return true;
		}
		return false;
	}
}
