/**
 * 
 */
package org.systemsbiology.genomebrowser.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.io.track.SequenceMapper;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.Roman;
import static org.systemsbiology.util.StringUtils.isInteger;


/**
 * Tries to guess which sequence (chromosome, plasmid, replicon, etc.) is intended and
 * map that to a standard form. Some examples of names that should be considered
 * equivalent are:
 * 
 * chr1 = Chr1 = chromosome1 = chromosome-1 = Chromosome 1 = 1
 * chr4 = IV
 * plasmid A = A
 * plasmid 1 != 1 'cause we assume bare numbers are chromosomes
 * 
 * @author cbare
 */
public class HeuristicSequenceMapper implements SequenceMapper {
	private static final Pattern singleChrPattern = Pattern.compile("chr(omosome)?[-_ ]?1?");
	private static final Pattern plasmidPattern = Pattern.compile("(plasmid[-_ ]?)(.*)");
	private static final Pattern chrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)(.*)");
	private static final Pattern numberedChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?(\\d+)");

	// Note that this will match things that aren't valid roman numerals and things like mammalian
	// sex chromosomes and chrLCD which has roman numeral characters, but isn't a roman numeral.
	// We ignore M as a roman numeral, since M usually means mitochondrial plus organisms with > 999
	// chromosomes are rare and you'd have to be insane to use roman numerals in one of those cases.
	private static final Pattern romanChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?([ivxlcd]+)");

	
	private Map<String, String> sequenceMap = new HashMap<String, String>();
	private Map<String, String> chrMap = new HashMap<String, String>();
	private Map<String, String> plasmidMap = new HashMap<String, String>();

	private int chrCount;
	private int numCount;
	private int romansCount;
	private String singleChrName;


	public HeuristicSequenceMapper() {}


	public void setStandardSequenceNames(List<String> sequenceNames) {
		Matcher m;

		for (String name : sequenceNames) {
			// insert exact match
			sequenceMap.put(name, name);

			// insert trimmed and lowercased name
			String lc = name.trim().toLowerCase();
			if (!sequenceMap.containsKey(lc))
				this.sequenceMap.put(lc, name);

			// when stemming chromosomes and plasmids, we need to keep the information
			// that the stem came from a chromosome or plasmid so that we don't do something
			// silly like equating plasmid A with chromosome A. So, we keep the stems in
			// separate maps.

			// add chromosome stems
			m = chrPattern.matcher(lc);
			if (m.matches())
				chrMap.put(m.group(3).toLowerCase(), name);

			// add plasmid stems
			m = plasmidPattern.matcher(lc);
			if (m.matches()) {
				plasmidMap.put(m.group(2).toLowerCase(), name);
			}

			m = numberedChrPattern.matcher(lc);
			if (m.matches())
				numCount++;

			m = romanChrPattern.matcher(lc);
			if (m.matches()) {
				if (Roman.isRoman(m.group(3)))
					romansCount++;
			}
		}

		// we have to guess about Roman numerals, since lots of things might look like
		// Roman numerals but not have that intent.
		double fraction = ((double)romansCount)/((double)sequenceNames.size());
		if (numCount==0 && fraction>0.6) {
			for (String name : sequenceNames) {
				m = romanChrPattern.matcher(name.trim().toLowerCase());
				if (m.matches()) {
					if (Roman.isRoman(m.group(3)))
						chrMap.put(String.valueOf(Roman.romanToInt(m.group(3))), name);
				}
			}
		}

		// how many chromosomes does it look like we have?
		chrCount = MathUtils.max(romansCount, numCount, chrMap.size());

		// if we have only one chromosome like most prokaryotes, we want to interpret
		// any reference to 'chromosome' or 'chromosome 1' or 'chr1' etc. to the one and only chromosome.
		if (chrCount==1) {
			for (String name: sequenceNames) {
				if (name.trim().toLowerCase().startsWith("chr")) {
					singleChrName = name;
					if (!chrMap.containsKey("1")) {
						chrMap.put("1", singleChrName);
					}
					break;
				}
			}
		}
	}

	/**
	 * Map name to standard name, if possible.
	 * @param string A string representing a chromosome, plasmid, or other sequence
	 * @return the standard name of the sequence
	 * @throws RuntimeException if name can't be matched with a standard sequence
	 */
	public String map(String name) {
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
			return singleChrName;
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

		m = romanChrPattern.matcher(nameLower);
		if (m.matches() && Roman.isRoman(m.group(3))) {
			int num = Roman.romanToInt(m.group(3));
			String key = String.valueOf(num);
			if (chrMap.containsKey(key))
				return chrMap.get(key);
			if (sequenceMap.containsKey(key))
				return sequenceMap.get(key);
			if (chrCount==1 && num==1)
				return singleChrName;
		}

		throw new RuntimeException("Unkown sequence: \"" + name + "\"");
	}
}