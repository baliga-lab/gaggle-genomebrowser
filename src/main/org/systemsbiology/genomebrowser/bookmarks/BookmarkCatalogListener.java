package org.systemsbiology.genomebrowser.bookmarks;


public interface BookmarkCatalogListener {
	void addBookmarkDataSource(BookmarkDataSource dataSource);
	void removeBookmarkDataSource(BookmarkDataSource dataSource);
	void renameBookmarkDataSource(BookmarkDataSource dataSource);
	void updateBookmarkCatalog();
}
