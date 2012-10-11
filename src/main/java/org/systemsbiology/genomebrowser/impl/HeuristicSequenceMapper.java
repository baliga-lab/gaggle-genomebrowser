/**
 * 
 */
package org.systemsbiology.genomebrowser.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.systemsbiology.genomebrowser.model.AbstractHeuristicSequenceMapper;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.Roman;


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
public class HeuristicSequenceMapper extends AbstractHeuristicSequenceMapper<String> {

    // Note that this will match things that aren't valid roman numerals and things like mammalian
    // sex chromosomes and chrLCD which has roman numeral characters, but isn't a roman numeral.
    // We ignore M as a roman numeral, since M usually means mitochondrial plus organisms with > 999
    // chromosomes are rare and you'd have to be insane to use roman numerals in one of those cases.
    private static final Pattern romanChrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)?([ivxlcd]+)");

    public HeuristicSequenceMapper() {}
    protected Pattern romanChrPattern() { return romanChrPattern; }

	public void setStandardSequenceNames(List<String> sequenceNames) {
      Matcher m;

      for (String name : sequenceNames) {
          // insert exact match
          sequenceMap.put(name, name);

          // insert trimmed and lowercased name
          String lc = name.trim().toLowerCase();
          if (!sequenceMap.containsKey(lc)) this.sequenceMap.put(lc, name);

          // when stemming chromosomes and plasmids, we need to keep the information
          // that the stem came from a chromosome or plasmid so that we don't do something
          // silly like equating plasmid A with chromosome A. So, we keep the stems in
          // separate maps.
          addChromosomeStems(lc, name);
          addPlasmidStems(lc, name);
          checkIfNumberedChromosome(lc);
          checkIfRomanNumberedChromosome(lc);
      }

      // we have to guess about Roman numerals, since lots of things might look like
      // Roman numerals but not have that intent.
      double fraction = ((double)romansCount)/((double)sequenceNames.size());
      if (numCount == 0 && fraction > 0.6) {
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
      if (chrCount == 1) {
          for (String name: sequenceNames) {
              if (name.trim().toLowerCase().startsWith("chr")) {
                  singleChrId = name;
                  if (!chrMap.containsKey("1")) chrMap.put("1", singleChrId);
                  break;
              }
          }
      }
	}
}