package org.systemsbiology.genomebrowser.bookmarks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.util.StringUtils;

/**
 * Holds several sets of bookmarks. A BookmarkDataSource represents a collection of bookmarks.
 * A BookmarkCatalog holds several BookmarkDataSources. Each BookmarkDataSource shows up as
 * a tab in BookmarksPanel.
 * @author cbare
 */
public class BookmarkCatalog implements Iterable<BookmarkDataSource>, EventListener {
	private Set<BookmarkCatalogListener> listeners = new CopyOnWriteArraySet<BookmarkCatalogListener>();
	private List<BookmarkDataSource> list = new ArrayList<BookmarkDataSource>();
	private BookmarkDataSource selected;


	public void addBookmarkDataSource(BookmarkDataSource dataSource) {
		list.add(dataSource);
		fireAddBookmarkDataSourceEvent(dataSource);
	}

	public void replaceBookmarkDataSource(BookmarkDataSource dataSource) {
		BookmarkDataSource old = findByName(dataSource.getName());
		if (old !=null && old.isDirty()) {
			old.setName(old.getName() + " modified");
			fireRenameBookmarkDataSourceEvent(old);
		}
		else {
			removeBookmarkDataSource(old);
		}
		addBookmarkDataSource(dataSource);
	}

	/**
	 * Finds the first set of bookmarks with the specified name. Unique names
	 * are not enforced.
	 */
	public BookmarkDataSource findByName(String name) {
		if (name == null) return null;
		for (BookmarkDataSource ds : list) {
			if (name.equals(ds.getName())) {
				return ds;
			}
		}
		return null;
	}

	public BookmarkDataSource findOrCreate(String name) {
		BookmarkDataSource ds = findByName(name);
		if (ds==null) {
			ds = new ListBookmarkDataSource(name);
			addBookmarkDataSource(ds);
		}
		return ds;
	}

	public void removeBookmarkDataSource(BookmarkDataSource dataSource) {
		if (dataSource == null) return;
		list.remove(dataSource);
		fireRemoveBookmarkDataSourceEvent(dataSource);
	}

	public void clear() {
		list.clear();
		fireUpdateBookmarkCatalogEvent();
	}

	public Iterator<BookmarkDataSource> iterator() {
		return list.iterator();
	}

	public int getCount() {
		return list.size();
	}

	public BookmarkDataSource getSelected() {
		if (list.size() == 0)
			addBookmarkDataSource(new ListBookmarkDataSource("Bookmarks"));
		if (selected == null)
			selected = list.get(0);
		return selected;
	}

	public void addBookmarkCatalogListener(BookmarkCatalogListener listener) {
		listeners.add(listener);
	}

	public void removeBookmarkCatalogListener(BookmarkCatalogListener listener) {
		listeners.remove(listener);
	}

	public void fireAddBookmarkDataSourceEvent(BookmarkDataSource dataSource) {
		for (BookmarkCatalogListener listener : listeners) {
			listener.addBookmarkDataSource(dataSource);
		}
	}

	public void fireUpdateBookmarkCatalogEvent() {
		for (BookmarkCatalogListener listener : listeners) {
			listener.updateBookmarkCatalog();
		}
	}

	public void fireRemoveBookmarkDataSourceEvent(BookmarkDataSource dataSource) {
		for (BookmarkCatalogListener listener : listeners) {
			listener.removeBookmarkDataSource(dataSource);
		}
	}

	public void fireRenameBookmarkDataSourceEvent(BookmarkDataSource dataSource) {
		for (BookmarkCatalogListener listener : listeners) {
			listener.renameBookmarkDataSource(dataSource);
		}
	}

	public void setSelected(BookmarkDataSource dataSource) {
		selected = dataSource;
	}

	public boolean isDirty() {
		for (BookmarkDataSource dataSource : list) {
			if (dataSource.isDirty())
				return true;
		}
		return false;
	}

	public BookmarkDataSource getResultsAsBookmarks(Iterable<Feature> features) {
		List<Bookmark> list = new ArrayList<Bookmark>();
		for (Feature feature : features) {
			list.add(new Bookmark(feature));
		}
		ListBookmarkDataSource dataSource = new ListBookmarkDataSource("Search Results");
		dataSource.addAll(list);
		return dataSource;
	}


	/**
	 * Bookmarks listens for search events and creates a tab of bookmarks for
	 * search results.
	 */
	@SuppressWarnings("unchecked")
	public void receiveEvent(Event event) {
		if ("search".equals(event.getAction())) {
			replaceBookmarkDataSource(getResultsAsBookmarks((Iterable<Feature>)event.getData()));
		}
		else if ("open.bookmarks".equals(event.getAction())) {
			String name = (String) event.getData();
			if (StringUtils.isNullOrEmpty(name))
				name = "bookmarks";
			setSelected(findOrCreate(name));
		}
	}
}
