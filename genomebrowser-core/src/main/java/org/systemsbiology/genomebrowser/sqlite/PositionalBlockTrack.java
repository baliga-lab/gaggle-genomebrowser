package org.systemsbiology.genomebrowser.sqlite;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.model.Block;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;


public class PositionalBlockTrack implements Track.Quantitative<Feature.Quantitative> {
	private BlockIndex index;
	private UUID uuid;
	private String name;
	private Range range;
	private Attributes attr = new Attributes();
	private SqliteDataSource dataSource;


	public PositionalBlockTrack(UUID uuid, String name, BlockIndex index, Range range, SqliteDataSource dataSource) {
		this.uuid = uuid;
		this.name = name;
		this.range = range;
		this.index = index;
		this.dataSource = dataSource;
	}

	public Class<? extends Feature.Quantitative> getFeatureClass() {
		return Feature.Quantitative.class;
	}

	public Attributes getAttributes() {
		return attr;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
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

	public BlockIndex getBlockIndex() {
		return index;
	}

	public Iteratable<Feature.Quantitative> features() {
		return new PositionalBlockIteratable(index.keys());
	}

	public Iteratable<Feature.Quantitative> features(FeatureFilter filter) {
		return new FilteredPositionalBlockIteratable(index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end), filter);
	}

	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		for (BlockKey key: index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end)) {
			callback.consumeFeatures(getBlock(key).features(filter.start, filter.end), new FeatureFilter(filter.sequence, key.getStrand(), filter.start, filter.end));
		}
	}
	
	private Block<Feature.Quantitative> getBlock(BlockKey key) {
		return dataSource.loadPositionalBlock(key);
	}

	class PositionalBlockIteratable implements Iteratable<Feature.Quantitative> {
		Iterator<BlockKey> keys;
		Iterator<Feature.Quantitative> features;
		
		public PositionalBlockIteratable(Iterator<BlockKey> keys) {
			this.keys = keys;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && keys.hasNext()) {
				features = getBlock(keys.next()).features();
			}
			return (features!= null && features.hasNext());
		}

		public Feature.Quantitative next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}
	}


	class FilteredPositionalBlockIteratable implements Iteratable<Feature.Quantitative> {
		Iterator<BlockKey> keys;
		Iterator<Feature.Quantitative> features;
		FeatureFilter filter;

		public FilteredPositionalBlockIteratable(Iterator<BlockKey> keys, FeatureFilter filter) {
			this.keys = keys;
			this.filter = filter;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && keys.hasNext()) {
				features = getBlock(keys.next()).features(filter.start, filter.end);
			}
			return (features!= null && features.hasNext());
		}

		public Feature.Quantitative next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}
	}

	public Range getRange() {
		return range;
	}
}
