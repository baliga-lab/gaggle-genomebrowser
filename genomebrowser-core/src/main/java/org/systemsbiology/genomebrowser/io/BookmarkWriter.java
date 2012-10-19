package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.util.Attributes;

// TODO write associated features?

/**
 * Write bookmarks out to a tab delimited file.
 */
public class BookmarkWriter {
    private static final Logger log = Logger.getLogger(BookmarkWriter.class);

    public void writeBookmarks(File file, BookmarkDataSource dataSource) throws IOException {
        writeBookmarks(new FileWriter(file), dataSource);
    }

    public void writeBookmarks(String filename, BookmarkDataSource dataSource) throws IOException {
        writeBookmarks(new FileWriter(filename), dataSource);
    }

    public void writeBookmarks(Writer writer, BookmarkDataSource dataSource) {
        PrintWriter out = new PrintWriter(writer);
		
        try {
            out.println(">name: " + dataSource.getName());
            Attributes attr = dataSource.getAttributes();
            for (String key : attr.keySet()) {
                out.println(String.format(">%s: %s", key, attr.getString(key)));
            }
            out.println("Chromosome\tStart\tEnd\tStrand\tName\tAnnotation\tSequence"); //(dmartinez)
            for (Bookmark bookmark : dataSource) {
                StringBuilder sb = new StringBuilder();
                sb.append(bookmark.getSeqId()).append("\t");
                sb.append(bookmark.getStart()).append("\t");
                sb.append(bookmark.getEnd()).append("\t");
                sb.append(bookmark.getStrand()).append("\t");
                sb.append(bookmark.getLabel()).append("\t");
                sb.append(bookmark.getAnnotation()).append("\t");
                if (bookmark.hasAttributes()) {
                    sb.append(bookmark.getAttributesString());
                }
                sb.append("\t");
                sb.append(bookmark.getSequence()); //(dmartinez)
                out.println(sb.toString());
            }
            dataSource.setDirty(false);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    log.warn("Error closing bookmark file", e);
                }
            }
        }
    }

    public String process(String string) {
        // strip out return characters and tabs
        return string.replaceAll("\\n", "\\\\n").replaceAll("\\t\\s*", "; ");
    }
}
