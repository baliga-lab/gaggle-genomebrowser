package org.systemsbiology.genomebrowser.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Map strings of the form "chr:12345000-12345678" or chr+:12345000-12345678.
 * Strand is determined preferably by appending a + or - to the sequence or by
 * which coordinate is greater. If the first is greater, it denotes a feature
 * in the reverse strand. Note that this convention is broken for circular
 * genomes where a feature crosses the zero point.
 * @author cbare
 */
public class TextCoordinateMap implements CoordinateMap {
    private static final Pattern pattern = Pattern.compile("(.+?)([-+]?):(\\d+)-(\\d+)");

    public Coordinates getCoordinates(String name) {
        Matcher m = pattern.matcher(name);
        if (m.matches()) {
            String seq = m.group(1);

            Strand strand = (m.group(2).length()>0) ? Strand.fromString(m.group(2)) : null;

            int first = Integer.parseInt(m.group(3));
            int second = Integer.parseInt(m.group(4));
			
            if (strand == null)
                strand = first>second ? Strand.reverse : Strand.forward;

            int start = Math.min(first, second);
            int end   = Math.max(first, second);

            return new Coordinates(seq, strand, start, end);
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
