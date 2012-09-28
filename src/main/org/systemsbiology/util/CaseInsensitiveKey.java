package org.systemsbiology.util;

/**
 * Used to key a map with strings in a case insensitive way
 */
public class CaseInsensitiveKey {
	final String string;
	final String lower;

	public CaseInsensitiveKey(String string) {
		if (string==null)
			throw new NullPointerException("CaseInsensitiveKey requires a non-null string.");
		this.string = string;
		this.lower = string.toLowerCase();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (obj instanceof CaseInsensitiveKey) {
			return lower.equals(((CaseInsensitiveKey)obj).lower);
		}
		if (obj instanceof String) {
			return lower.equals(((String)obj).toLowerCase());
		}
		return lower.equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return lower.hashCode();
	}

	@Override
	public String toString() {
		return string;
	}
}
