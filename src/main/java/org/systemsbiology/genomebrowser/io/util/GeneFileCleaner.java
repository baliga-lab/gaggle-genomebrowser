package org.systemsbiology.genomebrowser.io.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.systemsbiology.util.FileUtils;


public class GeneFileCleaner {
	private static final Logger log = Logger.getLogger(GeneFileCleaner.class);

	// fields in tab delimited file
	public static final int NAME  = 0;
	public static final int LABEL = 1;
	public static final int START = 2;
	public static final int STOP  = 3;
	public static final int STRAND = 4;


	
	public void loadFeatures(String filename) throws IOException {
		loadFeatures(FileUtils.getReaderFor(filename));
	}

	public void loadFeatures(Reader reader) throws IOException {

		BufferedReader r = null;
		try {
			r = new BufferedReader(reader);
			String line = r.readLine();
			System.out.println(line);
			line = r.readLine();
			while (line != null) {
				String[] fields = line.split("\t");
				StringBuilder sb = new StringBuilder();
				sb.append(fields[NAME]).append("\t");
				if (!fields[LABEL].equals(fields[NAME])) {
					sb.append(fields[LABEL]);
				}
				sb.append("\t");
				sb.append(fields[START]).append("\t");
				sb.append(fields[STOP]).append("\t");
				sb.append(fields[STRAND]);
				System.out.println(sb.toString());

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

	public static void main(String[] args) throws Exception {
		GeneFileCleaner cl = new GeneFileCleaner();
		cl.loadFeatures("data/HaloTilingArrayReferenceConditions/chromosome/chromosome_coordinates.txt");
	}
}
