package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.systemsbiology.util.ProgressListener;
import org.systemsbiology.genomebrowser.model.QuantitativeSegmentFeatureFields;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;

public class SegmentFeatureSource implements FeatureSource {
	private static final Logger log = Logger.getLogger(SegmentFeatureSource.class);
	private File file;
	private boolean hasColumnHeaders;


	public SegmentFeatureSource(File file) {
		this.file = file;
	}

	public SegmentFeatureSource(String filename, boolean hasColumnHeaders) {
		this.file = new File(filename);
		this.hasColumnHeaders = hasColumnHeaders;
	}

	public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
		
		QuantitativeSegmentFeatureFields feature = new QuantitativeSegmentFeatureFields();
		
		BufferedReader reader = null;
		try {
			int i = 0;
			reader = new BufferedReader(new FileReader(file));
			String line = null;

			// if we expect column headers, eat first non-comment line
			if (hasColumnHeaders) {
				while ( (line=reader.readLine()) != null ) {
					if (line.startsWith("#")) continue;
					else break;
				}
			}

			while ( (line=reader.readLine()) != null ) {
				i++;
				if (line.startsWith("#")) continue;
				String[] fields = line.split("\t");

				try {
					feature.sequence = fields[0];
					feature.strand = fields[1];
					feature.start = Integer.parseInt(fields[2]);
					feature.end = Integer.parseInt(fields[3]);
					feature.value = Double.parseDouble(fields[4]);
				}
				catch (Exception e) {
					throw new RuntimeException("Couldn't parse line " + i + " of file " + file.getName() +
							". Lines should contain the fields (Sequence, Strand, Start, End, Value) separated by tabs.", e);
				}

				featureProcessor.process(feature);
			}
		}
		finally {
			try {
				if (reader != null)
					reader.close();
			}
			catch (Exception e) {
				log.warn(e);
			}
		}
	}

	public void addProgressListener(ProgressListener progressListener) {
	}

	public void removeProgressListener(ProgressListener progressListener) {
	}
}
