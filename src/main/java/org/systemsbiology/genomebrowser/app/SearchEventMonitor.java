package org.systemsbiology.genomebrowser.app;

import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.event.Event;

/**
 * When a search is done, we want the results to appear in the bookmarks panel.
 * So we create an obect that listens for a search event and propogates the
 * results to the bookmarks catalog.
 * 
 * @author cbare
 */
public class SearchEventMonitor implements EventListener {
	Application app;

	public void setApplication(Application app) {
		this.app = app;
	}

	public void receiveEvent(Event event) {
		if ("search".equals(event.getAction())) {
//			app.bookmarkCatalog.replaceBookmarkDataSource(app.search.getResultsAsBookmarks());
		}
	}
}
