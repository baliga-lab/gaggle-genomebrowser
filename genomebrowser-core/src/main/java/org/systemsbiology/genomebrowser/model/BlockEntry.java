package org.systemsbiology.genomebrowser.model;

/**
 * An entry in a list that can be quickly searched for blocks
 * overlapping a given feature filter.
 * @author cbare
 */
class BlockEntry<F extends Feature> {
    public final FeatureFilter key;
    public final Block<F> block;

    public BlockEntry(FeatureFilter key, Block<F> block) {
        this.key = key;
        this.block = block;
    }
}