package org.systemsbiology.genomebrowser.visualization.tracks;

import org.systemsbiology.genomebrowser.impl.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Iteratable;



/**
 * A utility class to help count features in a frame for performance measurements
 */
public class FeatureCounter {
	private long counter;

	public void count(Iterable<TrackRenderer> renderers, Sequence sequence, int start, int end) {
		for (TrackRenderer renderer: renderers) {
			final Track<?> track = renderer.getTrack();
			final FeatureFilter filter = new FeatureFilter(sequence, Strand.any, start, end);

			track.featuresAsync(filter, new AsyncFeatureCallback() {
				public void consumeFeatures(Iteratable<? extends Feature> features, FeatureFilter filter) {
					count(features, filter.strand);
				}
			});
			System.out.println(track.getName() + " ==> " + counter);
		}

	}

	public void count(Iterable<? extends Feature> features, Strand strand) {
		for (Feature f : features) {
			if (strand.encompasses(f.getStrand()))
				counter++;
		}
	}

	public long getCount() {
		return counter;
	}
}
