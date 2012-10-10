package org.systemsbiology.genomebrowser.sqlite;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.impl.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.impl.Block;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;


public class PositionalQuantitativePvalueBlockTrack implements Track.Quantitative<Feature.QuantitativePvalue> {
	private BlockIndex index;
	private UUID uuid;
	private String name;
	private Range range;
	private Attributes attr = new Attributes();
	private SqliteDataSource dataSource;


	public PositionalQuantitativePvalueBlockTrack(UUID uuid, String name, BlockIndex index, Range range, SqliteDataSource dataSource) {
		this.uuid = uuid;
		this.name = name;
		this.range = range;
		this.index = index;
		this.dataSource = dataSource;
	}

	public Class<? extends Feature.QuantitativePvalue> getFeatureClass() {
		return Feature.QuantitativePvalue.class;
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

	public Iteratable<Feature.QuantitativePvalue> features() {
		return new BlockIteratable(index.keys());
	}

	public Iteratable<Feature.QuantitativePvalue> features(FeatureFilter filter) {
		return new FilteredBlockIteratable(index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end), filter);
	}

	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		for (BlockKey key: index.keys(filter.sequence.getSeqId(), filter.strand, filter.start, filter.end)) {
			callback.consumeFeatures(getBlock(key).features(filter.start, filter.end), new FeatureFilter(filter.sequence, key.getStrand(), filter.start, filter.end));
		}
	}
	
	private Block<Feature.QuantitativePvalue> getBlock(BlockKey key) {
		return dataSource.loadPositionalQuantitativePvalueBlock(key);
	}

	class BlockIteratable implements Iteratable<Feature.QuantitativePvalue> {
		Iterator<BlockKey> keys;
		Iterator<Feature.QuantitativePvalue> features;
		
		public BlockIteratable(Iterator<BlockKey> keys) {
			this.keys = keys;
		}

		public boolean hasNext() {
			while ((features==null || !features.hasNext()) && keys.hasNext()) {
				features = getBlock(keys.next()).features();
			}
			return (features!= null && features.hasNext());
		}

		public Feature.QuantitativePvalue next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<Feature.QuantitativePvalue> iterator() {
			return this;
		}
	}


	class FilteredBlockIteratable implements Iteratable<Feature.QuantitativePvalue> {
		Iterator<BlockKey> keys;
		Iterator<Feature.QuantitativePvalue> features;
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

		public Feature.QuantitativePvalue next() {
			return features.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("can't remove");
		}

		public Iterator<Feature.QuantitativePvalue> iterator() {
			return this;
		}
	}

	public Range getRange() {
		return range;
	}
}
