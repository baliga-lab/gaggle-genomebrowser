package org.systemsbiology.util;

import java.io.Serializable;
import java.util.List;

import org.systemsbiology.gaggle.core.datatypes.Single;
import org.systemsbiology.gaggle.core.datatypes.Tuple;


// tuples might be used in (at least) a couple of ways. The obvious is to
// assign key-value pairs that describe the object that owns the Tuple. So, if
// a track owns a tuple, the tuple holds its attributes (color, position, etc.)
// A tuple could also be used as an RDF triple. The tuple could have three
// values - subject, attribute, value - or more generally - object, relation,
// object. As in RDF then, a list of tuples can describe an object graph.


/**
 * Extends the Gaggle Tuple datatype with some convenience functions.
 * 
 * The get and set functions are to be used with unique keys (the names of
 * the Singles). This differs from add which will add any number of Singles
 * with the same name to the Tuple.
 *  
 * @author cbare
 */
public class NiceTuple extends Tuple {

	public NiceTuple () {}

	public NiceTuple(String name) {
		super(name);
	}

	public NiceTuple(String name, List<Single> singleList) {
		super(name, singleList);
	}

	public NiceTuple(Tuple tuple) {
		super(tuple.getName(), tuple.getSingleList());
	}


	public void add(Serializable value) {
		super.addSingle(new Single(value));
	}

	public void add(String name, Serializable value) {
		super.addSingle(new Single(name, value));
	}

	/**
	 * Sets the first existing single with the given name to
	 * the given value. If no entry exists with the specified
	 * name, create one.
	 * @return true if the key already existed.
	 */
	public boolean set(String name, Serializable value) {
		List<Single> singleList = getSingleList();
		for (Single single: singleList) {
			if (name.equals(single.getName())) {
				single.setValue(value);
				return true;
			}
		}
		add(name, value);
		return false;
	}

	/**
	 * @return the value associated with the given key. (The first such
	 * value, if there is more than one)
	 */
	public Serializable get(String name) {
		List<Single> singleList = getSingleList();
		for (Single single: singleList) {
			if (name.equals(single.getName())) {
				return single.getValue();
			}
		}
		return null;
	}

	public Serializable get(String name, Serializable defaultValue) {
		Serializable value = get(name);
		return value==null ? defaultValue : value;
	}


	// ---- convenience methods for basic data types --------------------------
	
	public Serializable getNotNull(String name) {
		Serializable value = get(name);
		if (value == null)
			throw new RuntimeException("No entry with name: " + name);
		return value;
	}

	public String getString(String name) {
		Serializable value = get(name);
		return value == null ? null : String.valueOf(value);
	}

	public String getString(String name, String defaultValue) {
		Serializable value = get(name);
		if (value==null)
			return defaultValue;
		else
			return String.valueOf(value);
	}

	public int getInt(String name) {
		Serializable value = getNotNull(name);
		if (value instanceof Number) {
			return ((Number)value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String)value);
		}
		else
			throw new ClassCastException("Can't convert a " + value.getClass().getName() + " to an integer");
	}

	public int getInt(String name, int defaultValue) {
		Serializable value = get(name);
		if (value instanceof Number) {
			return ((Number)value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String)value);
		}
		else
			return defaultValue;
	}

	public double getDouble(String name) {
		Serializable value = getNotNull(name);
		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}
		else if (value instanceof String) {
			return Double.parseDouble((String)value);
		}
		else
			throw new ClassCastException("Can't convert a " + value.getClass().getName() + " to a double");
	}

	public double getDouble(String name, double defaultValue) {
		Serializable value = get(name);
		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}
		else if (value instanceof String) {
			return Double.parseDouble((String)value);
		}
		else
			return defaultValue;
	}

	public boolean getBoolean(String name) {
		Serializable value = getNotNull(name);
		if (value instanceof Boolean) {
			return (Boolean)value;
		}
		else if (value instanceof String) {
			return Boolean.parseBoolean((String)value);
		}
		else
			throw new ClassCastException("Can't convert a " + value.getClass().getName() + " to a boolean");
	}

	public boolean getBoolean(String name, boolean defaultValue) {
		Serializable value = get(name);
		if (value instanceof Boolean) {
			return (Boolean)value;
		}
		else if (value instanceof String) {
			return Boolean.parseBoolean((String)value);
		}
		else
			return defaultValue;
	}

	/**
	 * convert back to Gaggle Tuple
	 */
	public Tuple toTuple() {
		return new Tuple(this.getName(), this.getSingleList());
	}

	
}
