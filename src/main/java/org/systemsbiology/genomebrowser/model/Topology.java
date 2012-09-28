package org.systemsbiology.genomebrowser.model;

/**
 * DNA topology can be either circular or linear.
 * @see Sequence
 */
public enum Topology {
	circular, linear, unknown;

	public static Topology fromString(String s) {
		try {
			return Topology.valueOf(s.toLowerCase());
		}
		catch (Exception e) {
			return Topology.unknown;
		}
	}
}
