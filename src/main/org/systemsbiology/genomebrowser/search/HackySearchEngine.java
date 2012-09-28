package org.systemsbiology.genomebrowser.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Event;
import org.systemsbiology.genomebrowser.app.EventListener;
import org.systemsbiology.genomebrowser.app.EventSupport;
import org.systemsbiology.genomebrowser.app.SearchEngine;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.util.MultiHashMap;

import cbare.stringsearch.Pattern;
import cbare.stringsearch.WildcardPattern;


// replace HackySearchEngine with DB drive search

/**
 * Searching for features in tracks implemented as a MultiHashMap (in which a
 * key maps to a list of values).
 */
public class HackySearchEngine implements SearchEngine, EventListener {
	private static final Logger log = Logger.getLogger(HackySearchEngine.class);
	private MultiHashMap<String, Feature> keywordsToFeatures = new MultiHashMap<String, Feature>();
	private boolean addWildcardSuffix;
	// TODO access to results may need to be synchronized
	private List<Feature> results = new ArrayList<Feature>();
	private int index;
	private EventSupport eventSupport = new EventSupport();


	public void automaticallyAddWildcardSuffix(boolean b) {
		addWildcardSuffix = b;
	}

	public void addSearchTerm(String term, Feature feature) {
		if (term != null)
			keywordsToFeatures.add(term, feature);
	}

	public int search(String[] keywords) {
		if (keywords == null || keywords.length==0)
			return 0;
		return search(Arrays.asList(keywords));
	}

	public int search(String keywords) {
		// TODO splitting on white space is a bug if search terms contain whitespace
		keywords = keywords.trim();
		// splitting an empty string on whitespace returns an array of strings
		// holding one empty string.
		return search(keywords.split("\\s+|\\s*,\\s*|\\s*;\\s*"));
	}

	public int search(List<String> keywords) {
		results.clear();
		Set<Feature> temp = new HashSet<Feature>();
		index = 0;

		if (keywords == null)
			return 0;
		
		for (String keyword : keywords) {
			
			// watch out for null of empty keywords
			if (keyword==null) continue;
			keyword = keyword.trim();
			if (keyword.length()==0) continue;

			if (addWildcardSuffix && !keyword.endsWith("*"))
				keyword = keyword + "*";
			
			// TODO why don't we make one pattern out of all the keys?

			Pattern pattern = new WildcardPattern(keyword);

			for(String key : keywordsToFeatures.keySet()) {
				if (pattern.match(key)) {
					temp.addAll(keywordsToFeatures.get(key));
				}
			}
		}
		
		// copy results into list
		results.addAll(temp);

		// sort by chromosomal coordinates
		Collections.sort(results, new Comparator<Feature>() {
			public int compare(Feature feature1, Feature feature2) {
				// TODO properly order the chromosomes
				int comp = feature1.getSeqId().compareTo(feature2.getSeqId());
				if (comp == 0)
					return feature1.getStart() - feature2.getStart();
				else
					return comp;
			}
		});

		fireSearchEvent();

		log.info("Search found " + results.size() + " results.");
		return results.size();
	}

	// these aren't super efficient, but it is a hacky search engine.
	public Feature findByName(String name) {
		Pattern pattern = new WildcardPattern(name);

		for(String key : keywordsToFeatures.keySet()) {
			if (pattern.match(key)) {
				for(Feature feature : keywordsToFeatures.get(key)) {
					if (feature instanceof NamedFeature) {
						if (((NamedFeature)feature).getName().equals(name))
							return feature;
					}
				}
			}
		}
		return null;
	}

	public List<Feature> findByName(List<String> names) {
		List<Feature> results = new ArrayList<Feature>(names.size());
		for (String name : names) {
			Feature f = findByName(name);
			if (f != null)
				results.add(f);
		}
		return results;
	}

	public Feature getNext() {
		if (results.size() < 1)
			return null;
		Feature feature = results.get(index);
		index = (index+1) % results.size();
		return feature;
	}

	public void clear() {
		keywordsToFeatures.clear();
		results.clear();
	}

	public int getTermCount() {
		return keywordsToFeatures.size();
	}

	public List<Feature> getResults() {
		return results;
	}

//	public BookmarkDataSource getResultsAsBookmarks() {
//		List<Bookmark> list = new ArrayList<Bookmark>();
//		for (Feature feature : results) {
//			list.add(new Bookmark(
//						feature.getSeqId(),
//						feature.getStart(),
//						feature.getEnd(),
//						feature.getStrand(),
//						"[" + feature.getStart() + ", " + feature.getEnd() + "]",
//						null));
//		}
//		ListBookmarkDataSource dataSource = new ListBookmarkDataSource("Search Results");
//		dataSource.addAll(list);
//		return dataSource;
//	}


	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}

	private void fireSearchEvent() {
		eventSupport.fireEvent(this, "search", results);
		if (results.size() > 1)
			eventSupport.fireEvent(this, "search-multiple-results", results);
	}

	/**
	 * When a new dataset is loaded, scour it for search terms
	 */
	@SuppressWarnings("unchecked")
	public void newDataset(Dataset dataset) {
		log.info("Search engine initializing for dataset: " + dataset.getName());
		clear();
		for (Track<? extends Feature> track: dataset.getTracks()) {
			if (track instanceof Track.Gene) {
				Track.Gene<GeneFeature> genes = (Track.Gene<GeneFeature>)track;
				for (GeneFeature feature: genes.features()) {
					addSearchTerm(feature.getName(), feature);
					addSearchTerm(feature.getCommonName(), feature);
				}
			}
//			else if (track instanceof Track.Labeled) {
//				for (Feature.Labeled feature: ((Track.Labeled)track).features()) {
//					addSearchTerm(feature.getLabel(), feature);
//				}
//			}
		}
		log.info("Search engine initialized with " + keywordsToFeatures.size() + " terms.");
	}

	public void receiveEvent(Event event) {
		if (event.getAction().equals("set dataset")) {
			newDataset((Dataset)event.getData());
		}
	}
	
}
