package org.systemsbiology.genomebrowser.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;

/**
 * Map strings of the form "chr:12345000" or chr+:12345000.
 * Strand may be specified by appending a + or - to the sequence. If neither is
 * present, we assume no strand.
 * @author cbare
 */
public class TextPositionalCoordinateMap implements CoordinateMap {
	private static final Pattern pattern = Pattern.compile("(.+?)([-+]?):(\\d+)");

	public Coordinates getCoordinates(String name) {
		Matcher m = pattern.matcher(name);
		if (m.matches()) {
			String seq = m.group(1);

			Strand strand = (m.group(2).length()>0) ? Strand.fromString(m.group(2)) : Strand.none;

			int position = Integer.parseInt(m.group(3));

			return new Coordinates(seq, strand, position);
		}
		return null;
	}

	public boolean isPositional() {
		return  true;
	}

	/**
	 * Test if names match the pattern <sequence-name>:<start>-<end>.
	 * If 90% of the first 100 match, we call it a match.
	 */
	public static double checkNames(String[] names) {
		if (names.length==0) return 0.0;
		int len = Math.min(100, names.length);
		int count = 0;
		for (int i=0; i<len; i++) {
			Matcher m = pattern.matcher(names[i]);
			if (m.matches()) {
				count++;
			}
		}
		return ((double)count) / ((double)len);
	}
}
