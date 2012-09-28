package org.systemsbiology.genomebrowser.model;

/**
 * Take a String and return a set of coordinates on the genome.
 */
public interface CoordinateMap {
	public Coordinates getCoordinates(String name);
	
	// do the coordinates map to positions (a single location on the genome) as opposed
	// to segments (a range of locations)?
	public boolean isPositional();
}
