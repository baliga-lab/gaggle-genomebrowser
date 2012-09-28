package org.systemsbiology.genomebrowser.io.dataset;


/**
 * Used by the dataset parser.
 * 
 * @author cbare
 */
public class KeyValuePair {
	String key;
	String value;
	
	static final KeyValuePair NULL = new KeyValuePair(null, null);
	
	public KeyValuePair() {}

	public KeyValuePair(String key, String value) {
		this.key = key;
		this.value = value;
	}

}