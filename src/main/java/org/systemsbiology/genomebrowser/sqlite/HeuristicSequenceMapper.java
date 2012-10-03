/**
 * 
 */
package org.systemsbiology.genomebrowser.sqlite;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.io.track.SequenceMapper;
import org.systemsbiology.util.Roman;
import static org.systemsbiology.util.StringUtils.isInteger;


/**
 * Tries to guess which sequence (chromosome, plasmid, replicon, etc.) is intended and
 * map that to a numeric identifier. Some examples of names that should be considered
 * equivalent are:
 * 
 * chr1 = Chr1 = chromosome1 = chromosome-1 = Chromosome 1 = 1
 * chr4 = IV
 * plasmid A = A
 * plasmid 1 != 1 'cause we assume bare numbers are chromosomes
 * 
 * Note: It might have been smarter to factor this as a conversion to canonical form.
 * 
 * @author cbare
 */
class HeuristicSequenceMapper implements SequenceMapper<Integer> {
    private static final Pattern singleChrPattern = Pattern.compile("chr(omosome)?[-_ ]?1?");
    private static final Pattern plasmidPattern = Pattern.compile("(plasmid[-_ ]?)(.*)");
    private static final Pattern chrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)(.*)");
    private static final Pattern numberedChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?(\\d+)");

    // Note that this will match things that aren't valid roman numerals and things like mammalian
    // sex chromosomes and chrLCD which has roman numeral characters, but isn't a roman numeral.
    // We ignore M as a roman numeral, since M usually means mitochondrial plus organisms with > 999
    // chromosomes are rare and you'd have to be insane to use roman numerals in one of those cases.
    private static final Pattern romanChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?([IVXLCD]+)");

    private Map<String, Integer> sequenceMap = new HashMap<String, Integer>();	
    private Map<String, Integer> chrMap = new HashMap<String, Integer>();
    private Map<String, Integer> plasmidMap = new HashMap<String, Integer>();
    private int chrCount;
    private int numCount;
    private int romansCount;
    private int singleChrId;


	/**
	 * @param sequenceMap a map from sequence names (names in the db) to database ID
	 */
	public HeuristicSequenceMapper(Map<String, Integer> sequenceMap) {
		Matcher m;
		this.sequenceMap.putAll(sequenceMap);

		for (String name: sequenceMap.keySet()) {
			// add lower cased keys
			if (!this.sequenceMap.containsKey(name.toLowerCase()))
				this.sequenceMap.put(name.toLowerCase(), sequenceMap.get(name));

			// when stemming chromosomes and plasmids, we need to keep the information
			// that the stem came from a chromosome or plasmid so that we don't do something
			// silly like equating plasmid A with chromosome A. So, we keep the stems in
			// separate maps.

			// add chromosome stems
			m = chrPattern.matcher(name);
			if (m.matches())
				chrMap.put(m.group(3).toLowerCase(), sequenceMap.get(name));

			// add plasmid stems
			m = plasmidPattern.matcher(name);
			if (m.matches()) {
				plasmidMap.put(m.group(2).toLowerCase(), sequenceMap.get(name));
			}

			if (name.toLowerCase().startsWith("chr")) chrCount++;

			m = romanChrPattern.matcher(name);
			if (m.matches()) {
				if (Roman.isRoman(m.group(3)))
					romansCount++;
			}
			
			m = numberedChrPattern.matcher(name);
			if (m.matches())
				numCount++;
		}

		// we have to guess about Roman numerals, since lots of things might look like
		// Roman numerals but not have that intent.
		double fraction = ((double)romansCount)/((double)sequenceMap.size());
		if (numCount==0 && fraction>0.6) {
			for (String name: sequenceMap.keySet()) {
				m = romanChrPattern.matcher(name);
				if (m.matches()) {
					if (Roman.isRoman(m.group(3)))
						chrMap.put(String.valueOf(Roman.romanToInt(m.group(3))), sequenceMap.get(name));
				}
			}
		}

		// if we have only one chromosome like most prokaryotes, we want to interpret
		// any reference to 'chromosome' or 'chromosome 1' or 'chr1' etc. to the one and only chromosome.
		if (chrCount==1) {
			for (String name: sequenceMap.keySet()) {
				if (name.trim().toLowerCase().startsWith("chr")) {
					singleChrId = sequenceMap.get(name);
					break;
				}
			}
		}
	}

	public int getId(String name) { return map(name).intValue(); }
	public Integer map(String name) {
		Matcher m;

		// look for exact matches first
		if (sequenceMap.containsKey(name))
			return sequenceMap.get(name);

		// check for matches differing only in case
		String nameLower = name.trim().toLowerCase();
		if (sequenceMap.containsKey(nameLower))
			return sequenceMap.get(nameLower);

		// if we only have one chromosome, then chr=chr1
		if (chrCount==1 && singleChrPattern.matcher(nameLower).matches()) {
			return singleChrId;
		}

		if (chrMap.containsKey(nameLower))
			return chrMap.get(nameLower);

		// try chromosome stems
		m = chrPattern.matcher(nameLower);
		if (m.matches()) {
			String key = m.group(3).trim().toLowerCase();
			if (chrMap.containsKey(key))
				return chrMap.get(key);
			if (sequenceMap.containsKey(key))
				return sequenceMap.get(key);
		}

		if (plasmidMap.containsKey(nameLower))
			return plasmidMap.get(nameLower);

		// try removing plasmid_* prefix
		m = plasmidPattern.matcher(nameLower);
		if (m.matches()) {
			String key = m.group(2).trim().toLowerCase();
			if (plasmidMap.containsKey(key))
				return plasmidMap.get(key);
			
			// if there's an unadorned integer, it's probably a chromosome
			if (!isInteger(key)) {
				if (sequenceMap.containsKey(key))
					return sequenceMap.get(key);
			}
		}

		m = romanChrPattern.matcher(name);
		if (m.matches() && Roman.isRoman(m.group(3))) {
			String key = String.valueOf(Roman.romanToInt(m.group(3)));
			if (chrMap.containsKey(key))
				return chrMap.get(key);
			if (sequenceMap.containsKey(key))
				return sequenceMap.get(key);
		}

		throw new RuntimeException("Unkown sequence: \"" + name + "\"");
	}
}