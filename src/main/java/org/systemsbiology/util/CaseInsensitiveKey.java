package org.systemsbiology.util;

/**
 * Used to key a map with strings in a case insensitive way
 */
public class CaseInsensitiveKey {
    private final String string;

    public CaseInsensitiveKey(String string) {
        if (string == null)
            throw new NullPointerException("CaseInsensitiveKey requires a non-null string.");
        this.string = string;
    }
    private String lower() { return string.toLowerCase(); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        String lower = this.lower();
        if (obj instanceof CaseInsensitiveKey) {
            return lower.equals(((CaseInsensitiveKey)obj).lower());
        }
        if (obj instanceof String) {
            return lower.equals(((String)obj).toLowerCase());
        }
        return lower.equals(obj.toString());
    }

    @Override
    public int hashCode() { return lower().hashCode(); }

    @Override
    public String toString() { return string; }
}
