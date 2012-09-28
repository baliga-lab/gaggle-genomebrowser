package org.systemsbiology.genomebrowser.io.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.systemsbiology.util.FileUtils;


/**
 * Takes segmentation data by probes and collapses contiguous probes of
 * equal value longer segments.
 */
public class RunCollapser {
	private static final Logger log = Logger.getLogger(RunCollapser.class);
	
	double[] values;
	int[] starts;
	int[] ends;
	int size;
	
	int startColumn = 0;
	int endColumn = 1;
	int valueColumn = 2;


	public void loadData(String filename) throws IOException {
		loadData(FileUtils.getReaderFor(filename));
	}

	public void loadData(Reader reader) throws IOException {
		BufferedReader r = null;

		try {
			r = new BufferedReader(reader);

			int i = 0;
			// read first line of the file which is assumed to hold column titles
			String line = r.readLine();

			// allocate parallel arrays for data columns
			values = new double[size];
			starts = new int[size];
			ends = new int[size];

			line = r.readLine();
			while (line != null) {
				String[] fields = line.split("\t");

				starts[i] = Integer.parseInt(fields[startColumn]);
				ends[i]   = Integer.parseInt(fields[endColumn]);
				values[i] = Double.parseDouble(fields[valueColumn]);
				i++;

				line = r.readLine();
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

	public void findRuns() {
		int end;

		int i = 0;
		// overflow-safe integer average
		int start = (starts[i] + ends[i]) >>> 1;
		double value = values[i];

		for (i=1; i<size; i++) {
			if (values[i] != value || i==(size-1)) {
				// we'll take the end-point as half way between
				// the start of the previous data point and the
				// end of the the current data point (with a new
				// value).
				end = (starts[i-1] + ends[i]) >>> 1;
				System.out.println(start + "\t" + end + "\t" + value);
				start = end;
				value  = values[i];
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		RunCollapser rc = new RunCollapser();
		rc.size = 2197;
		// for example: "data/HaloTilingArrayReferenceConditions/pNRC100/segmentation.reverse.tsv.old"
		rc.loadData(args[0]);
		rc.findRuns();
	}

}
