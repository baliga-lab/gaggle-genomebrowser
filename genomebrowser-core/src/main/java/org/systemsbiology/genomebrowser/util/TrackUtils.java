package org.systemsbiology.genomebrowser.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.*;


public class TrackUtils {
	private static final Logger log = Logger.getLogger(TrackUtils.class);

	/**
	 * Takes a list of names and tries to find a track matching one of those
	 * names. Names are searched in order.
	 * @Return a track matching one of the given names or null if none are found
	 */
	public static Track<? extends Feature> findTrack(Dataset dataset, String... names) {
		List<String> actualNames = new ArrayList<String>();
		for (Track<Feature> track : dataset.getTracks()) {
			actualNames.add(track.getName());
		}
		for (String name : names) {
			if (actualNames.contains(name))
				return dataset.getTrack(name);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Track<GeneFeature> findGenomeTrack(Dataset dataset) {
		for (Track<? extends Feature> track: dataset.getTracks()) {
			if (track instanceof Track.Gene<?> && "Genome".equals(track.getName()) || "Genes".equals(track.getName())) {
				log.info("Genome Track found: " + track.getName()); 
				return (Track<GeneFeature>)track;
			}
		}

		// if we haven't found a genome track, pick the first track with a gene renderer
		for (Track<? extends Feature> track: dataset.getTracks()) {
			if (track instanceof Track.Gene<?> && "Gene".equals(track.getAttributes().getString("viewer"))) {
				log.info("Genome Track guessed by renderer type: " + track.getName()); 
				return (Track<GeneFeature>)track;
			}
		}

		// if we still haven't found a genome track, pick the first gene track
		for (Track<? extends Feature> track: dataset.getTracks()) {
			if (track instanceof Track.Gene<?>) {
				log.info("Genome Track guessed by track type: " + track.getName()); 
				return (Track<GeneFeature>)track;
			}
		}

		log.warn("Couldn't find genome track.");
		return null;
	}

	/**
	 * @return genes within the given coordinates or an empty list if none can be found.
	 */
	public static List<GeneFeature> findGenesIn(Dataset dataset, Sequence sequence, Strand strand, int start, int end) {
      if (dataset==null || dataset==Datasets.EMPTY_DATASET())
			return Collections.emptyList();
		List<GeneFeature> results = new ArrayList<GeneFeature>();
		Track<GeneFeature> track = findGenomeTrack(dataset);
		if (track != null) {
			for (GeneFeature feature : track.features(new FeatureFilter(sequence, strand, start, end))) {
				results.add(feature);
			}
		}
		return results;
		
	}
	
	public static <T extends Track<?>> List<T> sortedByName(List<T> list) {
		List<T> result = new ArrayList<T>(list);
		Collections.sort(result, new TrackNameComparator());
		return result;
	}

	public static class TrackNameComparator implements Comparator<Track<?>> {
		public int compare(Track<?> t1, Track<?> t2) {
			if (t1==null) {
				return (t2==null) ? 0 : -1;
			}
			if (t2==null) return 1;
			if (t1.getName()==null) {
				return (t2.getName()==null) ? 0 : -1; 
			}
			if (t2.getName()==null) return 1;
			return t1.getName().compareTo(t2.getName());
		}
	}

	public static List<String> getGroups(List<Track<Feature>> tracks) {
		Set<String> groups = new HashSet<String>();
		for (Track<Feature> track : tracks) {
			String groupList = track.getAttributes().getString("groups");
			if (groupList != null) {
				String[] groupArray = groupList.split(",");
				for (String group : groupArray) {
					groups.add(group.trim());
				}
			}
		}
		List<String> list = new ArrayList<String>(groups.size());
		list.addAll(groups);
		Collections.sort(list);
		return list;
	}
	
	public static List<String> getGroups(Track<? extends Feature> track) {
		Set<String> groups = new HashSet<String>();
		String groupList = track.getAttributes().getString("groups");
		if (groupList != null) {
			String[] groupArray = groupList.split(",");
			for (String group : groupArray) {
				groups.add(group.trim());
			}
		}
		List<String> list = new ArrayList<String>(groups.size());
		list.addAll(groups);
		Collections.sort(list);
		return list;
	}
	
	private static Set<String> _getGroups(Track<? extends Feature> track) {
		Set<String> groups = new HashSet<String>();
		String groupList = track.getAttributes().getString("groups");
		if (groupList != null) {
			String[] groupArray = groupList.split(",");
			for (String group : groupArray) {
				groups.add(group.trim());
			}
		}
		return groups;
	}

	public static List<Track<Feature>> getTracksByGroup(String group, List<Track<Feature>> tracks) {
		List<Track<Feature>> results = new ArrayList<Track<Feature>>();
		for (Track<Feature> track : tracks) {
			if (_getGroups(track).contains(group)) {
				results.add((Track<Feature>)track);
			}
		}
		return results;
	}

	public static String toString(List<Track<Feature>> tracks) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (Track<Feature> track : tracks) {
			if (first) { first = false; } else { sb.append(", "); } 
			sb.append(track.getName());
		}
		sb.append("]");
		return sb.toString();
		
	}
}
