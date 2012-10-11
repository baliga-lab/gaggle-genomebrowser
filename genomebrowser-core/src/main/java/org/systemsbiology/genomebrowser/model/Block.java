package org.systemsbiology.genomebrowser.model;

import org.systemsbiology.util.Iteratable;

/**
 * A block is a bunch of contiguous features on the same sequence and
 * strand that can be loaded and cached as a unit. Blocks are implemented
 * in some cases (PositionalBlock and SegmentBlock) by returning flyweight
 * features. So, features returned from a Block's iterators should be used
 * immediately and not stored or passed to functions that might store them.
 * @author cbare
 * @see org.systemsbiology.genomebrowser.sqlite.PositionalBlock
 * @see org.systemsbiology.genomebrowser.sqlite.SegmentBlock
 */
public interface Block<F extends Feature> extends Iterable<F> {
    Sequence getSequence();
    Strand getStrand();
    Iteratable<F> features();
    Iteratable<F> features(int start, int end);
}
