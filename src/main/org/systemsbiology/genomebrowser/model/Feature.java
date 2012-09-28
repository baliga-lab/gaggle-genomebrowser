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
	public String getSeqId();
	public Strand getStrand();
	public int getStart();
	public int getEnd();
	public int getCentralPosition();
	public String getLabel();

	public interface Quantitative extends Feature {
		public double getValue();
	}
	
	public interface QuantitativePvalue extends Quantitative {
		public double getPvalue();
	}

	public interface NamedFeature extends Feature {
		public String getName();
	}

	public interface ScoredFeature extends Feature {
		public double getScore();
	}

	public interface ScaledQuantitative extends Quantitative {
		public double getMin();
		public double getMax();
	}

	public interface Matrix extends Quantitative {
		public double[] getValues();
	}

	public interface Nested extends Feature {
		public List<Feature> getFeatures();
	}
}

class NullFeature implements Feature {

	public int getCentralPosition() {
		return 0;
	}

	public int getEnd() {
		return 0;
	}

	public String getLabel() {
		return null;
	}

	public String getSeqId() {
		return null;
	}

	public int getStart() {
		return 0;
	}

	public Strand getStrand() {
		return null;
	}
	
}

