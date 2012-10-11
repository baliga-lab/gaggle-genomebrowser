package org.systemsbiology.genomebrowser.bookmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

// These Swing dependencies are event-related, so we can keep
// them in core, since they do not involve Swing UI rendering
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.systemsbiology.genomebrowser.util.FeaturePositionComparator;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * An in-memory Bookmark data source based on an ArrayList.
 * @author cbare
 */
public class ListBookmarkDataSource implements BookmarkDataSource {
	private Set<ListDataListener> listeners = new CopyOnWriteArraySet<ListDataListener>();
	private String name;
	private List<Bookmark> bookmarks = new ArrayList<Bookmark>();
	private boolean dirty;
	private Attributes attributes;

	public ListBookmarkDataSource(String name) {
		this.name = name;
	}

	public ListBookmarkDataSource(String name, List<Bookmark> bookmarks) {
		this.name = name;
		this.bookmarks = bookmarks;
	}

	public ListBookmarkDataSource(String name, List<Bookmark> bookmarks, Attributes attributes) {
		this.name = name;
		this.bookmarks = bookmarks;
		this.attributes = attributes;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Bookmark getElementAt(int index) {
		return bookmarks.get(index);
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void add(Bookmark bookmark) {
		bookmarks.add(bookmark);
		dirty = true;
		fireAddEvent(bookmarks.size() - 1);
	}

	public void addAll(List<? extends Bookmark> bookmarks) {
		this.bookmarks.addAll(bookmarks);
//		int start = this.bookmarks.size();
//		int end = start;
//		for (Bookmark b : bookmarks) {
//			this.bookmarks.add(b);
//			end++;
//		}
//		// this is a cheat to cover the case where we loaded
//		// bookmarks from a file and that's it.
//		if (start > 0)
//			dirty = true;
//		fireAddEvent(start, end);
	}

	// implementations might not have reference equality between references.
	public void update(Bookmark oldBookmark, Bookmark newBookmark) {
		int i = bookmarks.indexOf(oldBookmark);
		if (i>=0) {
			bookmarks.set(i, newBookmark);
			dirty = true;
			fireUpdateEvent(i);
		}
		else {
			add(newBookmark);
		}
	}

	public boolean remove(Bookmark bookmark) {
		int i = bookmarks.indexOf(bookmark);
		if (i<0) return false;
		bookmarks.remove(i);
		dirty = true;
		fireDeleteEvent(i);
		return true;
	}

	public Bookmark remove(int i) {
		Bookmark bookmark = bookmarks.remove(i);
		dirty = true;
		fireDeleteEvent(i);
		return bookmark;
	}

	/**
	 * Sort bookmarks by position on the chromosome. Specifically, by sequence, start, end
	 */
	public void sortByPosition() {
		Collections.sort(bookmarks, new FeaturePositionComparator());
		fireUpdateEvent(0,bookmarks.size());
	}

	public Attributes getAttributes() {
		if (attributes==null) {
			attributes = new Attributes();
		}
		return attributes;
	}

	public int getSize() {
		return bookmarks.size();
	}

	public void addListDataListener(ListDataListener listener) {
		listeners.add(listener);
	}

	public void removeListDataListener(ListDataListener listener) {
		listeners.remove(listener);
	}

	public void fireAddEvent(int index) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index));
		}
	}

	public void fireAddEvent(int start, int end) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, start, end));
		}
	}

	public void fireUpdateEvent(int index) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index));
		}
	}

	public void fireUpdateEvent(int index1, int index2) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index1, index2));
		}
	}

	public void fireDeleteEvent(int index) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
		}
	}

	public Iterator<Bookmark> iterator() {
		return bookmarks.iterator();
	}

}
