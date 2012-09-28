package org.systemsbiology.genomebrowser.sqlite;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.impl.Block;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.PeptideFeature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;


public class PeptideBlock implements Block<PeptideFeature> {
	private BlockKey key;
	private int[] starts;
	private int[] ends;
	private String[] names;
	private String[] commonNames;
	private double[] scores;
	private int[] redundancy;

	public PeptideBlock(BlockKey key, int[] starts, int[] ends, String[] names, String[] commonNames, double[] scores) {
		this.key = key;
		this.starts = starts;
		this.ends = ends;
		this.names = names;
		this.commonNames = commonNames;
		this.scores = scores;
	}

	public PeptideBlock(BlockKey key, int[] starts, int[] ends, String[] names, String[] commonNames, double[] scores, int[] redundancy) {
		this.key = key;
		this.starts = starts;
		this.ends = ends;
		this.names = names;
		this.commonNames = commonNames;
		this.scores = scores;
		this.redundancy = redundancy;
	}
	
	

	@Override
	public Sequence getSequence() {
		// TODO getSequence key.getSeqId();
		return null;
	}

	@Override
	public Strand getStrand() {
		return key.getStrand();
	}

	@Override
	public Iteratable<PeptideFeature> features() {
		return new FeaturesIteratable();
	}

	@Override
	public Iteratable<PeptideFeature> features(int start, int end) {
		return new WindowedFeaturesIteratable(start, end);
	}

	@Override
	public Iterator<PeptideFeature> iterator() {
		return features();
	}

	class FeaturesIteratable implements Iteratable<PeptideFeature> {
		FlyweightFeature feature = new FlyweightFeature();
		int len = starts.length;
		int next;

		public boolean hasNext() {
			return next < len;
		}

		public PeptideFeature next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<PeptideFeature> iterator() {
			return this;
		}		
	}

	class WindowedFeaturesIteratable implements Iteratable<PeptideFeature> {
		FlyweightFeature feature = new FlyweightFeature();
		int len = starts.length;
		int start;
		int end;
		int next;

		public WindowedFeaturesIteratable(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean hasNext() {
			// features are sorted by start,end
			while (next < len && ends[next] < start) {
				next++;
			}
			return (next < len) && starts[next] < end;
		}

		public PeptideFeature next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<PeptideFeature> iterator() {
			return this;
		}		
	}

	class FlyweightFeature implements PeptideFeature {
		int i;

		public double getScore() {
			return scores[i];
		}

		public int getCentralPosition() {
			return MathUtils.average(starts[i], ends[i]);
		}

		public int getEnd() {
			return ends[i];
		}

		public String getSeqId() {
			return key.getSeqId();
		}

		public int getStart() {
			return starts[i];
		}

		public Strand getStrand() {
			return key.getStrand();
		}
		
		public String toString() {
			return String.format("(Feature: %s, %s, %d, %d, %s, %s, %.2f)", getSeqId(), getStrand(), starts[i], ends[i], names[i], commonNames[i], scores[i]); 
		}

		public String getLabel() {
			return isNullOrEmpty(getCommonName()) ? getName() : getCommonName();
		}

		@Override
		public String getCommonName() {
			return commonNames[i];
		}

		@Override
		public GeneFeatureType getType() {
			return GeneFeatureType.peptide;
		}

		@Override
		public String getName() {
			return names[i];
		}
		
		public int getRedundancy() {
			if (redundancy==null)
				return -1;
			else
				return redundancy[i];
		}

	}

}
