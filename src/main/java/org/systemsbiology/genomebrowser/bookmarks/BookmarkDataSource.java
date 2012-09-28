package org.systemsbiology.genomebrowser.bookmarks;

import java.util.Iterator;
import java.util.List;

import javax.swing.ListModel;

import org.systemsbiology.util.Attributes;


/**
 * Represents a collection of bookmarks.
 */
public interface BookmarkDataSource extends ListModel, Iterable<Bookmark> {
//  ListModel methods:
//	  int getSize();
//	  Object getElementAt(int index);
//	  void addListDataListener(ListDataListener l);
//	  void removeListDataListener(ListDataListener l);

	public String getName();
	public void setName(String name);
	public Bookmark getElementAt(int i);
	public void add(Bookmark feature);
	public void addAll(List<? extends Bookmark> bookmarks);
	public void update(Bookmark oldBookmark, Bookmark newBookmark);
	public boolean remove(Bookmark feature);
	public Bookmark remove(int i);
	public Iterator<Bookmark> iterator();
	public boolean isDirty();
	public void setDirty(boolean dirty);
	public Attributes getAttributes();
	public void sortByPosition();
}
