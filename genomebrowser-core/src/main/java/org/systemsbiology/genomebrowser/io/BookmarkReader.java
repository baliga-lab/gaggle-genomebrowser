package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.bookmarks.ListBookmarkDataSource;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.FileUtils;

// TODO read associated features in BookmarkReader

/**
 * Expects a tab delimited text file.
 */
public class BookmarkReader {
    private static final Logger log = Logger.getLogger(BookmarkReader.class);
	
    // match properties of the form ">key: this is a value"
    static Pattern property = Pattern.compile("\\s*>\\s*((?:[^:\\s])(?:[^:])*(?:[^:\\s]))\\s*:\\s*(\\S.*\\S)\\s*");

    // define columns
    private static final int CHROM  = 0;
    private static final int START  = 1;
    private static final int END    = 2;
    private static final int STRAND = 3;
    private static final int NAME   = 4;
    private static final int ANNO   = 5;
    private static final int ATTR   = 6;
    private static final int SEQ    = 7;

    String name = "Unnamed Bookmarks";

    public BookmarkReader() { }

    public BookmarkDataSource loadData(File file) throws IOException {
        name = stripExtension(file.getName());
        return loadData(new FileReader(file));
    }

    public BookmarkDataSource loadData(String path) throws IOException {
        name = stripExtension(path);
        return loadData(FileUtils.getReaderFor(path));
    }

    public BookmarkDataSource loadData(Reader reader) throws IOException {
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        Attributes attributes = new Attributes();
        BufferedReader r = null;
        name = "bookmarks";
        String line = null;

        try {
            r = new BufferedReader(reader);
            int i = 0;
            line = r.readLine();
            Matcher m = property.matcher(line);
            while (m.matches()) {
                if ("name".equals(m.group(1))) name = m.group(2);
                attributes.put(m.group(1), m.group(2));
                m = property.matcher(r.readLine());
            }
            // skip a line for the column titles
            line = r.readLine();
            while (line != null) {
                String[] fields = line.split("\t");
                bookmarks.add(new Bookmark(fields[CHROM], Strand.fromString(fields[STRAND]),
                                           Integer.parseInt(fields[START]),
                                           Integer.parseInt(fields[END]),
                                           fields[NAME],
                                           optionalAnnotation(fields),
                                           optionalAttributes(fields),
                                           optionalSequence(fields)));

                i++;
                line = r.readLine();
            }
            return new ListBookmarkDataSource(name, bookmarks, attributes);
        } finally {
            try {
                if (r != null) r.close();
            } catch (Exception e) {
                log.error("Exception reading line: " + line);
                log.error(e);
            }
        }
    }

    private String optionalAnnotation(String[] fields) {
        if (fields.length > ANNO) {
            String anno = fields[ANNO];
            return anno.replaceAll("\\\\n", "\n");
        } else return null;
    }

    private String optionalAttributes(String[] fields) {
        if (fields.length > ATTR) return fields[ATTR];
        else return null;
    }
    private String optionalSequence(String[] fields) {
        if (fields.length > SEQ) return fields[SEQ];
        else return null;
    }

    private String stripExtension(String filename) {
        if (filename == null) return this.name;
        int s = filename.lastIndexOf('/');
        if (s == -1) s = filename.lastIndexOf('\\');
        if (s == -1) s = 0;
        int e = filename.lastIndexOf(".");
        if (e == -1) e = filename.length();
        return filename.substring(s, e);
    }
}
