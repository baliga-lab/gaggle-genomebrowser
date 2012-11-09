package org.systemsbiology.genomebrowser.bookmarks

trait BookmarkCatalogListener {
	def addBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def removeBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def renameBookmarkDataSource(dataSource: BookmarkDataSource): Unit
	def updateBookmarkCatalog: Unit
}
