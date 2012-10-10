package org.systemsbiology.genomebrowser.sqlite;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.Attributes$;

/**
 * A collection of bookmarks backed by a sqlite table.
 */
public class SqliteBookmarkDataSource implements BookmarkDataSource {
	String name;
	private Set<ListDataListener> listeners = new CopyOnWriteArraySet<ListDataListener>();


	public void add(Bookmark feature) {
//		String sql = "insert into bookmarks values(?, ?, ?, ?, ?, ?, ?)";
	}
/*
	id integer primary key autoincrement,
	collectionName text not null,
	sequences_id integer not null,
	strand text not null,
	start integer not null,
	end integer not null,
	name text not null,
	annotation text);
*/

	public void addAll(List<? extends Bookmark> bookmarks) {
		for (Bookmark b : bookmarks) {
			add(b);
		}
	}

	public Bookmark getElementAt(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return name;
	}

	public boolean isDirty() {
		return false;
	}

	public Iterator<Bookmark> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean remove(Bookmark feature) {
		// TODO Auto-generated method stub
		return false;
	}

	public Bookmark remove(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDirty(boolean dirty) {
		// TODO Auto-generated method stub
		
	}

	public void setName(String name) {
		this.name = name;
	}

	public void update(Bookmark oldBookmark, Bookmark newBookmark) {
		// TODO Auto-generated method stub
		
	}

	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
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

	public void fireDeleteEvent(int index) {
		for (ListDataListener listener : listeners) {
			listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
		}
	}

	public Attributes getAttributes() {
      return Attributes$.MODULE$.EMPTY();
	}

	public void sortByPosition() {
		// TODO Auto-generated method stub
		
	}
}
