package org.systemsbiology.genomebrowser.sqlite;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.model.AbstractHeuristicSequenceMapper;
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
class HeuristicSequenceMapper extends AbstractHeuristicSequenceMapper<Integer> {

    // Note that this will match things that aren't valid roman numerals and things like mammalian
    // sex chromosomes and chrLCD which has roman numeral characters, but isn't a roman numeral.
    // We ignore M as a roman numeral, since M usually means mitochondrial plus organisms with > 999
    // chromosomes are rare and you'd have to be insane to use roman numerals in one of those cases.
    private static final Pattern romanChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?([IVXLCD]+)");

    protected Pattern romanChrPattern() { return romanChrPattern; }
    protected Matcher romanMatcher(String name) {
        return romanChrPattern().matcher(name);
    }

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
          addChromosomeStems(name, sequenceMap.get(name));
          addPlasmidStems(name, sequenceMap.get(name));
          if (name.toLowerCase().startsWith("chr")) chrCount++;
          checkIfRomanNumberedChromosome(name);
          checkIfNumberedChromosome(name);
      }

      // we have to guess about Roman numerals, since lots of things might look like
      // Roman numerals but not have that intent.
      double fraction = ((double)romansCount)/((double)sequenceMap.size());
      if (numCount == 0 && fraction > 0.6) {
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

}