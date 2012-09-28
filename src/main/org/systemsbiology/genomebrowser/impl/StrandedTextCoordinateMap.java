package org.systemsbiology.genomebrowser.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;

/**
 * Map strings of the form "chr+:12345000-12345678". Strand is determined by
 * appending a + or - to the end of the sequence name. Note that we need to
 * test if this mapper will work *before* TextCoordinateMap.
 *
 * @author cbare
 */
public class StrandedTextCoordinateMap implements CoordinateMap {
	private static final Pattern pattern = Pattern.compile("(.*)([+-]):(\\d+)-(\\d+)");

	public Coordinates getCoordinates(String name) {
		Matcher m = pattern.matcher(name);
		if (m.matches()) {
			int start = Integer.parseInt(m.group(3));
			int end = Integer.parseInt(m.group(4));
			Strand strand = Strand.fromString(m.group(2));
			if (start>end) {
				int temp = start;
				start = end;
				end = temp;
			}
			return new Coordinates(m.group(1), strand, start, end);
		}
		return null;
	}

	public boolean isPositional() {
		return false;
	}

	/**
	 * Test if names match the pattern <sequence-name>:<start>-<end>.
	 * If 90% of the first 100 match, we call it a match.
	 */
	public static boolean checkNames(String[] names) {
		if (names.length==0) return false;
		int len = Math.min(100, names.length);
		int count = 0;
		for (int i=0; i<len; i++) {
			Matcher m = pattern.matcher(names[i]);
			if (m.matches()) {
				count++;
			}
		}
		return (((double)count) / ((double)len) > 0.90);
	}
}
