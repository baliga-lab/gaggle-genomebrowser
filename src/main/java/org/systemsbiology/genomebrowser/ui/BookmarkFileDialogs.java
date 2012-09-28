package org.systemsbiology.genomebrowser.ui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkCatalog;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.io.BookmarkReader;
import org.systemsbiology.genomebrowser.io.BookmarkWriter;


/**
 * Handles dialogs for loading and saving bookmarks.
 */
public class BookmarkFileDialogs {
	private BookmarkCatalog bookmarkCatalog;
	private Component parentComponent;
	private Options options;

	
	public void setBookmarkCatalog(BookmarkCatalog bookmarkCatalog) {
		this.bookmarkCatalog = bookmarkCatalog;
	}


	public void setParentComponent(Component parent) {
		this.parentComponent = parent;
	}


	public void setOptions(Options options) {
		this.options = options;
	}


	public void loadBookmarkFile() throws IOException {
		// get filename through file dialog
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Load Bookmarks");

		if (options.workingDirectory != null)
			chooser.setCurrentDirectory(options.workingDirectory);

		int returnVal = chooser.showOpenDialog(parentComponent);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();

			// update working directory
			options.workingDirectory = file.getParentFile();

			// read bookmarks
			BookmarkReader reader = new BookmarkReader();
			BookmarkDataSource bds = reader.loadData(file);
			bookmarkCatalog.addBookmarkDataSource(bds);
		}
	}

	public void exportBookmarksToFile() throws IOException {
		BookmarkDataSource bookmarkDataSource = bookmarkCatalog.getSelected();
		if (bookmarkDataSource == null) {
			throw new RuntimeException("Can't export bookmarks. No bookmarks selected.");
		}

		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export Bookmarks");

		if (options.workingDirectory != null)
			chooser.setCurrentDirectory(options.workingDirectory);

		int returnVal = chooser.showSaveDialog(parentComponent);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();

			// update working directory
			options.workingDirectory = file.getParentFile();

			// ask permission to overwrite
			if (file.exists()) {
				int result = JOptionPane.showConfirmDialog(parentComponent, file.getName() + " already exists. OK to overwrite?", "Overwrite file?", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.NO_OPTION)
					return;
			}
			BookmarkWriter writer = new BookmarkWriter();
			writer.writeBookmarks(file, bookmarkDataSource);
		}
	}
}
