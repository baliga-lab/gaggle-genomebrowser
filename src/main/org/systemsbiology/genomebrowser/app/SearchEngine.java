package org.systemsbiology.genomebrowser.app;

import java.util.List;

import org.systemsbiology.genomebrowser.model.Feature;


// TODO make search interface more general
// how would this change if searching was done in a DB?

public interface SearchEngine {

	public int search(String keyword);

	public int search(String[] keywords);
	
	public int search(List<String> keywords);
	
	// TODO do we need a find by name method?
	public Feature findByName(String name);

	public void clear();

	public void addSearchTerm(String term, Feature feature);

	public int getTermCount();

	public Feature getNext();

	public List<Feature> getResults();

}
