package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.FeatureFields;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.util.ProgressListener;
import org.systemsbiology.util.MathUtils;

/**
 * Expects a tab delimited text file with (at least) three columns - start, end, and value.
 * The start and end coordinate should be integers while the value is a double. We need
 * to set sequence and strand externally, assuming that the file has features for one
 * sequence on one strand as in the old dataset format.
 * 
 * @author cbare
 */
public class StartEndValueDataLoader implements FeatureSource {
	private static final Logger log = Logger.getLogger(StartEndValueDataLoader.class);
	int start = 0;
	int end  = 1;
	int value = 2;
	boolean skipFirst;
	File file;
	MyFeatureFields feature = new MyFeatureFields();


	public StartEndValueDataLoader(String filename) {
		this.file = new File(filename);
	}

	public StartEndValueDataLoader(File file) {
		this.file = file;
	}

	public void setSkipFirst(boolean skipFirst) {
		this.skipFirst = skipFirst;
	}

	public void setupForMicroarrayDataFile() {
		start = 2;
		end  = 3;
		value = 5;
	}

	public void setSequence(String sequence) {
		feature.sequence = sequence;
	}

	public void setStrand(String strand) {
		feature.strand = strand;
	}

	public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
		BufferedReader r = new BufferedReader(new FileReader(file));

		try {
			String line;
			if (skipFirst) r.readLine();
			while ( (line = r.readLine()) != null ) {
				String[] fields = line.split("\t");

				feature.start = Integer.parseInt(fields[start]);
				feature.end = Integer.parseInt(fields[end]);
				feature.value = Double.parseDouble(fields[value]);

				featureProcessor.process(feature);
			}
		}
		finally {
			try {
				if (r != null) {
					r.close();
				}
			}
			catch (Exception e) {
				log.error(e);
			}
		}
	}


	@SuppressWarnings("unused")
	private int strandToInt(String strand) {
		if ("+".equals(strand) || "FORWARD".equals(strand))
			return 1;
		else if ("-".equals(strand) || "REVERSE".equals(strand))
			return -1;
		else
			return 0;
	}
	
	static class MyFeatureFields implements FeatureFields {
		String sequence;
		String strand;
		int start;
		int end;
		double value;


		public String getSequenceName() {
			return sequence;
		}

		public String getStrand() {
			return strand;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public int getPosition() {
			return MathUtils.average(start, end);
		}

		public double getValue() {
			return value;
		}


		public String getName() {
			return null;
		}

		public String getCommonName() {
			return null;
		}

		public String getGeneType() {
			return null;
		}
	}


	Set<ProgressListener> listeners = new HashSet<ProgressListener>();

	public void addProgressListener(ProgressListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeProgressListener(ProgressListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void fireIncrementProgressEvent() {
		synchronized (listeners) {
			for (ProgressListener listener : listeners) {
				listener.incrementProgress(1);
			}
		}
	}
}
