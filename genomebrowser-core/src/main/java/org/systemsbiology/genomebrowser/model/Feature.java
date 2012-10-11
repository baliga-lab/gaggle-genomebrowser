package org.systemsbiology.genomebrowser.model;

import java.util.List;

// TODO need to implement a plain feature that has nothing but position (for example, breaks)

/**
 * A Feature is anything that has a start and end coordinate on a sequence.
 * The feature may pertain to a particular strand or not.
 * 
 * @author cbare
 */
public interface Feature {
    public static final Feature NULL = new NullFeature();
    String getSeqId();
    Strand getStrand();
    int getStart();
    int getEnd();
    int getCentralPosition();
    String getLabel();

    public interface Quantitative extends Feature {
        double getValue();
    }
	
    public interface QuantitativePvalue extends Quantitative {
        double getPvalue();
    }

    public interface NamedFeature extends Feature {
        String getName();
    }

    public interface ScoredFeature extends Feature {
        double getScore();
    }

    public interface ScaledQuantitative extends Quantitative {
        double getMin();
        double getMax();
    }

    public interface Matrix extends Quantitative {
        double[] getValues();
    }

    public interface Nested extends Feature {
        List<Feature> getFeatures();
    }
}

class NullFeature implements Feature {

    public int getCentralPosition() { return 0; }
    public int getEnd() { return 0; }
    public String getLabel() { return null; }
    public String getSeqId() {  return null; }
    public int getStart() { return 0; }
    public Strand getStrand() { return null; }	
}
