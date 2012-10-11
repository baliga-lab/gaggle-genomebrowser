package org.systemsbiology.genomebrowser.model;

/**
 * Map a name to coordinates on the genome.
 */
public interface CoordinateMapper {
    Coordinates map(String name);
}
