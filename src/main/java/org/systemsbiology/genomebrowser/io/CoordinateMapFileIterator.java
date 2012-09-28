package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.util.MathUtils;

/**
 * Read mappings between names and coordinates from a tab-delimited text file. Be sure to call
 * the cleanup method in order to close file reader.
 */
public class CoordinateMapFileIterator implements Iterable<NamedFeature>, Iterator<NamedFeature> {
	private static final Logger log = Logger.getLogger(CoordinateMapFileIterator.class);
	private BufferedReader reader;
	private String line;
	private FlyweightFeature feature = new FlyweightFeature();

	public CoordinateMapFileIterator(File file) throws IOException {
		reader = new BufferedReader(new FileReader(file));
		readLine();
	}

	public boolean hasNext() {
		return line!=null;
	}

	/**
	 * @return a flyweight feature
	 */
	public NamedFeature next() {
		if (line==null)
			throw new NoSuchElementException();
		String[] fields = line.split("\t");
		feature.name = fields[0];
		feature.seqId = fields[1];
		feature.strand = Strand.fromString(fields[2]);
		feature.start = Integer.parseInt(fields[3]);
		feature.end = Integer.parseInt(fields[4]);
		readLine();
		return feature;
	}

	public Iterator<NamedFeature> iterator() {
		return this;
	}

	public void remove() {
		throw new UnsupportedOperationException("No removing.");
	}

	public void cleanup() {
		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("Error closing reader:", e);
		}
	}

	private void readLine() {
		try {
			line = reader.readLine();
			while (line.startsWith("#"))
				line = reader.readLine();
		}
		catch (Exception e) {
			line = null;
			log.warn(e);
		}
	}

	private class FlyweightFeature implements NamedFeature {
		String name;
		String seqId;
		Strand strand;
		int start;
		int end;

		public String getName() {
			return name;
		}

		public int getCentralPosition() {
			return MathUtils.average(start, end);
		}

		public int getEnd() {
			return end;
		}

		public String getLabel() {
			return name;
		}

		public String getSeqId() {
			return seqId;
		}

		public int getStart() {
			return start;
		}

		public Strand getStrand() {
			return strand;
		}
		
	}
}
