package org.systemsbiology.genomebrowser.sqlite;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.impl.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.PeptideFeature;
import org.systemsbiology.genomebrowser.model.ScoredNamedFeature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.Iteratable;


public class PeptideBlockTrack implements Track.Gene<PeptideFeature> {
	private BlockIndex index;
	private UUID uuid;
	private String name;
	private Attributes attributes = new Attributes();
	private SqliteDataSource dataSource;
	private Class<? extends Feature> featureClass = PeptideFeature.class;


	public PeptideBlockTrack(UUID uuid, String name, BlockIndex index, SqliteDataSource dataSource) {
		this.uuid = uuid;
		this.name = name;
		this.index = index;
		this.dataSource = dataSource;
	}

	public Class<? extends Feature> getFeatureClass() {
		return featureClass;
	}

	public void setFeatureClass(Class<PeptideFeature> featureClass) {
		this.featureClass = featureClass;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public Strand[] strands() {
		Set<Strand> strands = new HashSet<Strand>();
		for (Feature feature: features()) {
			strands.add(feature.getStrand());
		}
		return strands.toArray(new Strand[strands.size()]);
	}

	@Override
	public PeptideFeature getFeatureAt(Sequence sequence, Strand strand, int coord) {
		for (BlockKey key : index.keys(sequence.getSeqId(), strand)) {
			if (key.overlaps(sequence, strand, coord)) {
				for (PeptideFeature feature : getBlock(key).features()) {
					if (feature.getStart() <= coord && feature.getEnd() >= coord)
						return feature;
				}
			}
		}
		return null;
	}

	@Override
	public Iteratable<PeptideFeature> features() {
		return new BlockIteratable(index.keys());
	}

	@Override
	public Iteratable<PeptideFeature> features(FeatureFilter filter) {
		return new FilteredBlockIteratable(index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end), filter);
	}

	@Override
	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		for (BlockKey key: index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end)) {
			callback.consumeFeatures(getBlock(key).features(filter.start, filter.end), new FeatureFilter(filter.sequence, key.getStrand(), filter.start, filter.end));
		}
	}
	
	private PeptideBlock getBlock(BlockKey key) {
		return dataSource.loadPeptideBlock(key);
	}

	class BlockIteratable implements Iteratable<PeptideFeature> {
		Iterator<BlockKey> keys;
		Iterator<PeptideFeature> features;
		
		public BlockIteratable(Iterator<BlockKey> keys) {
			this.keys = keys;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && keys.hasNext()) {
				features = getBlock(keys.next()).features();
			}
			return (features!= null && features.hasNext());
		}

		public PeptideFeature next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<PeptideFeature> iterator() {
			return this;
		}
	}

	class FilteredBlockIteratable implements Iteratable<PeptideFeature> {
		Iterator<BlockKey> keys;
		Iterator<PeptideFeature> features;
		FeatureFilter filter;

		public FilteredBlockIteratable(Iterator<BlockKey> keys, FeatureFilter filter) {
			this.keys = keys;
			this.filter = filter;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && keys.hasNext()) {
				features = getBlock(keys.next()).features(filter.start, filter.end);
			}
			return (features!= null && features.hasNext());
		}

		public PeptideFeature next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<PeptideFeature> iterator() {
			return this;
		}
	}

}
