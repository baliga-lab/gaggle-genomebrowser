package org.systemsbiology.genomebrowser.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;


public class FeatureUtils {
	private static Logger log = Logger.getLogger(FeatureUtils.class); 

	/**
	 * Reflectively call the track's getFeatureClass() method, if it exists. If
	 * the track doesn't have a getFeatureClass method, we return check whether
	 * the track is a subtype of Track.Quantitative or Track.Gene. Failing that,
	 * we return Feature.class.
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends Feature> getFeatureClass(Track<? extends Feature> track) {
		if (track!=null) {
			try {
				Method m = track.getClass().getMethod("getFeatureClass");
				if (m != null) {
					Object result = m.invoke(track);
					return (Class<? extends Feature>)result;
				}
			}
			catch (Exception e) {
				log.error("Error invoking track.getFeatureClass() on " + track.getClass().getName(), e);
			}
			if (track instanceof Track.Quantitative)
				return Feature.Quantitative.class;
			if (track instanceof Track.Gene)
				return GeneFeature.class;
		}
		return (Class<Feature>)Feature.class;
	}

	public static String[] extractNames(Collection<Feature> features) {
		List<String> names = new ArrayList<String>(features.size());
		for (Feature feature : features) {
			if (feature instanceof NamedFeature)
				names.add(((NamedFeature) feature).getName());
		}
		return names.toArray(new String[names.size()]);
	}

	public static String toString(Feature feature) {
		if (feature==null) return "null";
		return String.format("%s(%s%s:%d-%d;%d)", feature.getLabel(), feature.getSeqId(), feature.getStrand().toAbbreviatedString(), feature.getStart(), feature.getEnd(), feature.getCentralPosition());
	}
}
