/**
 * 
 */
package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.util.Iteratable;


public class TextFileInfo {
	private File file;
	private long length = -1L;
	private List<String> previewLines = new LinkedList<String>();
	private String[] columnTitles;
	private String firstLine;


	public TextFileInfo() {}

	public TextFileInfo(File file) {
		super();
		this.file = file;
		length = file.length();
	}

	public File getFile() {
		return file;
	}

	public String getFilename() {
		return file.getName();
	}

	public long getLength() {
		return length;
	}

	public String getLengthAsString() {
		if (length < 0)
			return "?";
		if (length < 1024L)
			return String.format("%d bytes", length);
		if (length < 1048576L)
			return String.format("%.1f kb", length/1024.0);
		if (length < 1073741824L)
			return String.format("%.1f mb", length/1048576.0);
		return String.format("%.1f gb", length/1073741824.0);
	}

	public int getPreviewLineCount() {
		return previewLines.size() + (hasColumnTitles() ? 1 : 0);
	}

	public List<String> getPreviewLines() {
		return previewLines;
	}

	public Iteratable<String[]> getPreviewFields() {
		final int cols = getColumnCount();
		return new Iteratable<String[]>() {
			int i = 0;

			public boolean hasNext() {
				return i < previewLines.size();
			}

			public String[] next() {
				String[] fields =  previewLines.get(i++).split("\t");
				if (fields.length < cols) {
					String[] f2 = new String[cols];
					System.arraycopy(fields, 0, f2, 0, fields.length);
					fields = f2;
				}
				return fields;
			}

			public void remove() {
				throw new UnsupportedOperationException("can't remove");
			}

			public Iterator<String[]> iterator() {
				return this;
			}
		};
	}

	public void setPreviewLines(List<String> previewLines) {
		this.previewLines.addAll(previewLines);
	}

	public void addPreviewLine(String line) {
		previewLines.add(line);
	}

	public String[] getColumnTitles() {
		return columnTitles;
	}

	public void setColumnTitles(String[] columnTitles) {
		this.columnTitles = columnTitles;
	}

	public void setFirstLine(String line) {
		firstLine = line;
		String[] fields = line.split("\t");
		if (detectColumnTitles(fields))
			setColumnTitles(fields);
		else
			previewLines.add(line);
	}

	/**
	 * Our cheap sleazy heuristic is that if we find a field that
	 * looks like an int then it's data, otherwise it's column titles
	 */
	private boolean detectColumnTitles(String[] fields) {
		Pattern digits = Pattern.compile("\\d+");
		for (String field: fields) {
			Matcher m = digits.matcher(field);
			if (m.matches()) {
				return false;
			}
		}
		return true;
	}

	public int getColumnCount() {
		if (columnTitles != null)
			return columnTitles.length;
		else if (previewLines.size() > 0)
			return previewLines.get(0).split("\t").length;
		else
			return 1;
	}

	public boolean hasColumnTitles() {
		return (columnTitles!=null);
	}

	public void setFirstLineHoldsColumnTitles(boolean firstLineHoldsColumnTitles) {
		if (firstLineHoldsColumnTitles == (columnTitles==null)) {
			if (firstLineHoldsColumnTitles) {
				if (previewLines.size() > 0) {
					firstLine = previewLines.remove(0);
					columnTitles = firstLine.split("\t");
				}
			}
			else {
				columnTitles = null;
				if (firstLine != null) {
					previewLines.add(0, firstLine);
				}
			}
		}
	}
}
