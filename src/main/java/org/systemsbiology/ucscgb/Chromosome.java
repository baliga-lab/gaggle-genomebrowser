package org.systemsbiology.ucscgb;

/**
 * Represents an entry in the UCSC chromInfo table.
 */
public class Chromosome {
    private String name;
    private int size;

    public Chromosome(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {	return name; }
    public int getSize() { return size;	}
    public String toString() {
        return String.format("(%s, %,d)", name, size);
    }
}
