package org.systemsbiology.util;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Attributes is a set of convenience methods added to a HashMap
 * for retrieving values of various types using a String key. It's
 * meant to store an arbitrary set of properties.
 *
 * @author cbare
 */
public class Attributes extends HashMap<String, Object> {
    private static final long serialVersionUID = 7921514258446479640L;

    public Attributes() {}

    public Attributes(Map<String, Object> map) {
        super(map);
    }

    public String getString(String key) {
        Object value = get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String getString(String key, String defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public int getInt(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number)value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String)value);
        } else {
            throw new ClassCastException("Can't convert a " + getClassName(value) + " to an integer");
        }
    }

    public int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value == null) return defaultValue;
        else if (value instanceof Number) return ((Number)value).intValue();
        else if (value instanceof String) return Integer.parseInt((String)value);
        else return defaultValue;
    }
	
    public float getFloat(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number)value).floatValue();
        } else if (value instanceof String) {
            return Float.parseFloat((String)value);
        } else {
            throw new ClassCastException("Can't convert a " + getClassName(value) + " to a float");
        }
    }

    public float getFloat(String key, float defaultValue) {
        Object value = get(key);
        if (value == null) return defaultValue;
        else if (value instanceof Number) {
            return ((Number)value).floatValue();
        } else if (value instanceof String) {
            return Float.parseFloat((String)value);
        } else return defaultValue;
    }

    public double getDouble(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number)value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String)value);
        } else {
            throw new ClassCastException("Can't convert a " + getClassName(value) + " to a double");
        }
    }

    public double getDouble(String key, double defaultValue) {
        Object value = get(key);
        if (value == null)  return defaultValue;
        else if (value instanceof Number) {
            return ((Number)value).doubleValue();
        } else if (value instanceof String && !"".equals(value)) {
            return Double.parseDouble((String)value);
        } else return defaultValue;
    }

    public boolean getBoolean(String key) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean)value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String)value);
        } else {
            throw new ClassCastException("Can't convert a " + getClassName(value) + " to a boolean");
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value == null) return defaultValue;
        else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String)value);
        } else return defaultValue;
    }

    public Color getColor(String key) {
        Object value = get(key);
        if (value instanceof Color) {
            return (Color)value;
        } else if (value instanceof Number) {
            return new Color(((Number)value).intValue(), true);
        } else if (value instanceof String) {
            return ColorUtils.decodeColor((String)value);
        } else {
            throw new ClassCastException("Can't convert a " + getClassName(value) + " to a Color");
        }
	}

    public Color getColor(String key, Color defaultValue) {
        Object value = get(key);
        if (value == null) return defaultValue;
        else if (value instanceof Color) {
            return (Color)value;
        } else if (value instanceof Number) {
            return new Color(((Number)value).intValue(), true);
        } else if (value instanceof String) {
            return ColorUtils.decodeColor((String)value);
        } else return defaultValue;
    }

    private String getClassName(Object o) {
        return (o == null) ? "null" : o.getClass().getName();
    }

    /**
     * @return a string representation of the map in the form "key1=value1;key2=value2;" suitable for inclusion in TSV files.
     */
    public String toAttributesString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : entrySet()) {
            sb.append(escape(entry.getKey())).append('=').append(escape(String.valueOf(entry.getValue()))).append(';');
        }
        return sb.toString();
    }

    /**
     * Parses a string of the form "key1=value1;key2=value2;".
     * TODO doesn't handle escaped delimiters ("\=", "\;").
     * @return an Attributes object populated with the key/value pairs parsed from the string.
     */
    public static Attributes parse(String attributes) {
        Attributes results = new Attributes();
        if (attributes != null) {
            String[] pairs = attributes.split(";");
            for (String pair : pairs) {
                if (pair.length() > 0) {
                    String[] fields = pair.split("=");
                    String key = unescape(fields[0]);
                    String value = unescape(fields[1]);
                    results.put(key, value);
                }
            }
        }
        return results;
    }

    private static String unescape(String string) {
        return string.replace("\\n","\n").replace("\\\\", "\\");
    }

    private static String escape(String string) {
        return string.replace("\\", "\\\\").replace("\n","\\n");
    }

    // a semi-safe EMPTY attributes object. Be nice and don't
    // add attributes to it.
    public static final Attributes EMPTY = new Attributes() {
            private static final long serialVersionUID = -7199122687556831258L;
            
            @Override
            public Object put(String key, Object value) {
                return null;
            }
            
            @Override
            public void putAll(Map<? extends String, ? extends Object> m) {}
            
            @Override
            public Set<java.util.Map.Entry<String, Object>> entrySet() {
                return Collections.emptySet();
            }
    };
}
