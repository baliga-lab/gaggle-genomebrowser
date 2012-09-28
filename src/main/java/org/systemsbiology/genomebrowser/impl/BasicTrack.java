package org.systemsbiology.genomebrowser.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.Iteratable;


/**
 * Simple but slow implementation of track.
 */
public class BasicTrack<F extends Feature> implements Track<F> {
	private UUID uuid;
	private String name;
	private Attributes attr = new Attributes();
	private List<F> features;
	private Class<? extends Feature> featureClass;


	public BasicTrack(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
		features = new ArrayList<F>();
	}

	public BasicTrack(UUID uuid, String name, List<F> features) {
		this.uuid = uuid;
		this.name = name;
		this.features = features;
	}


	public Attributes getAttributes() {
		return attr;
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
		for (Feature feature: features) {
			strands.add(feature.getStrand());
		}
		return strands.toArray(new Strand[strands.size()]);
	}

	public void setFeatureClass(Class<? extends Feature> featureClass) {
		this.featureClass = featureClass;
	}

	public Class<? extends Feature> getFeatureClass() {
		return featureClass==null ? Feature.class : featureClass;
	}

	public void addFeature(F feature) {
		features.add(feature);
	}

	public Iteratable<F> features() {
		return new Iteratable.Wrapper<F>(features.iterator());
	}

	public Iteratable<F> features(final FeatureFilter filter) {
		return new Iteratable<F>() {
			int next;
			int len = features.size();

			public boolean hasNext() {
				while (next < len && !filter.passes(features.get(next))) {
					next++;
				}
				return next < len;
			}

			public F next() {
				int i = next++;
				return features.get(i);
			}

			public void remove() {
				throw new UnsupportedOperationException("remove() not implemented");
			}

			public Iterator<F> iterator() {
				return this;
			}
		};
	}

	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		callback.consumeFeatures(features(filter), filter);
	}

}
