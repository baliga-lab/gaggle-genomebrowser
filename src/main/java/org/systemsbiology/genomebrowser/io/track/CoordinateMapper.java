package org.systemsbiology.genomebrowser.io.track;

import org.systemsbiology.genomebrowser.model.Coordinates;

/**
 * Map a name to coordinates on the genome.
 */
public interface CoordinateMapper {
	public Coordinates map(String name);
}
