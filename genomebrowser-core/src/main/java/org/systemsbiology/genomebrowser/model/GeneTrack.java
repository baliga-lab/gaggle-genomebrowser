package org.systemsbiology.genomebrowser.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;

/**
 * An in-memory track of GeneFeatures backed by a list of Blocks.
 * 
 * Keeping GeneFeatures in memory is necessary to preserve the value of the
 * feature's selected property.
 */
public class GeneTrack<G extends GeneFeature> implements Track.Gene<G> {
	private List<BlockEntry<G>> blocks = new ArrayList<BlockEntry<G>>();
	private Attributes attributes = new Attributes();
	private String name;
	private UUID uuid;
	private Class<? extends GeneFeature> featureClass = GeneFeature.class;


	public GeneTrack(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}

	public Class<? extends Feature> getFeatureClass() {
		return featureClass;
	}

	public void setFeatureClass(Class<? extends GeneFeature> featureClass) {
		this.featureClass = featureClass;
	}

	@SuppressWarnings("unchecked")
	public void addGeneFeatures(FeatureFilter filter, Block<? extends G> block) {
		this.blocks.add(new BlockEntry<G>(filter, (Block<G>)block));
	}

	public void addGeneFeatures(Block<? extends G> block) {
		addGeneFeatures(new FeatureFilter(block.getSequence(), block.getStrand()), block);
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public String getName() {
		return name;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Strand[] strands() {
		Set<Strand> strands = new HashSet<Strand>();
		for (Feature feature: features()) {
			strands.add(feature.getStrand());
		}
		return strands.toArray(new Strand[strands.size()]);
	}

	public G getFeatureAt(Sequence sequence, Strand strand, int coord) {
		for (BlockEntry<G> entry : blocks) {
        if (entry.key().overlaps(sequence, strand, coord)) {
            for (G feature: entry.block()) {
					if (feature.getStart() <= coord && feature.getEnd() >= coord)
						return feature;
				}
			}
		}
		return null;
	}


	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		for (BlockEntry<G> entry : blocks) {
        if (entry.key().overlaps(filter)) {
            callback.consumeFeatures(entry.block().features(filter.start, filter.end),
                                     entry.key());
			}
		}
	}

	public Iteratable<G> features() {
		return new GeneFeatureIteratable();
	}

	public Iteratable<G> features(FeatureFilter filter) {
		return new FilteredGeneFeaturesIteratable(filter);
	}
	
	class GeneFeatureIteratable implements Iteratable<G> {
		Iterator<BlockEntry<G>> entries;
		Iterator<G> features;
		
		public GeneFeatureIteratable() {
			entries = blocks.iterator();
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && entries.hasNext()) {
          features = entries.next().block().features();
			}
			return (features!=null && features.hasNext());
		}

		public G next() {
			return features.next();
		}
		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<G> iterator() {
			return this;
		}		
	}

	class FilteredGeneFeaturesIteratable implements Iteratable<G> {
		Iterator<BlockEntry<G>> entries;
		Iterator<G> features;
		FeatureFilter filter;

		public FilteredGeneFeaturesIteratable(FeatureFilter filter) {
			entries = blocks.iterator();
			this.filter = filter;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && entries.hasNext()) {
				BlockEntry<G> entry = entries.next();
				if (entry.key().overlaps(filter))
            features = entry.block().features(filter.start, filter.end);
			}
			return (features!=null && features.hasNext());
		}

		public G next() {
			return features.next();
		}
		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<G> iterator() {
			return this;
		}
	}
}
