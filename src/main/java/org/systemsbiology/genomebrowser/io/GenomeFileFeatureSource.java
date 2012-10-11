package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.sqlite.FeatureFields;
import org.systemsbiology.genomebrowser.sqlite.FeatureProcessor;
import org.systemsbiology.genomebrowser.sqlite.FeatureSource;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.ProgressListener;

/**
 * 	import a tab-delimited file with these fields:
 * (Sequence, Strand, Start, End, Unique Identifier, Common Name, Gene Type)
 */
public class GenomeFileFeatureSource implements FeatureSource {
	private static final Logger log = Logger.getLogger(GenomeFileFeatureSource.class);
	private File file;
	private boolean hasColumnHeaders;


	public GenomeFileFeatureSource(File file) {
		this.file = file;
	}

	public GenomeFileFeatureSource(String filename, boolean hasColumnHeaders) {
		this.file = new File(filename);
		this.hasColumnHeaders = hasColumnHeaders;
	}

	public void processFeatures(FeatureProcessor featureProcessor) throws Exception {

		GenomeFeatureFields feature = new GenomeFeatureFields() ;

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
					feature.strand = Strand.fromString(fields[1]);
					feature.start = Integer.parseInt(fields[2]);
					feature.end = Integer.parseInt(fields[3]);
					feature.name = fields[4];
					feature.commonName = fields[5];
					feature.geneType = fields[6];
				}
				catch (Exception e) {
					throw new RuntimeException("Couldn't parse line " + i + " of file " + file.getName() +
							". Lines should contain the fields (Sequence, Strand, Start, End, Unique " +
							"Identifier, Common Name, Gene Type) separated by tabs.", e);
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
		// TODO Auto-generated method stub

	}

	public void removeProgressListener(ProgressListener progressListener) {
		// TODO Auto-generated method stub

	}

	static class GenomeFeatureFields implements FeatureFields {
		String sequence;
		Strand strand;
		int start;
		int end;
		String name;
		String commonName;
		String geneType;


		public String getName() {
			return name;
		}

		public String getCommonName() {
			return commonName;
		}

		public String getGeneType() {
			return geneType;
		}

		public String getSequenceName() {
			return sequence;
		}

		public String getStrand() {
			return strand.toAbbreviatedString();
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
			return 0;
		}
	}
}
