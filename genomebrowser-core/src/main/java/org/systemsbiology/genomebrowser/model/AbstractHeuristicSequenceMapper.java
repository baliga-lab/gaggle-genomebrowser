package org.systemsbiology.genomebrowser.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import org.systemsbiology.util.Roman;
import static org.systemsbiology.util.StringUtils.isInteger;

public abstract class AbstractHeuristicSequenceMapper<T> implements SequenceMapper<T> {
    protected static final Pattern singleChrPattern = Pattern.compile("chr(omosome)?[-_ ]?1?");
    protected static final Pattern plasmidPattern = Pattern.compile("(plasmid[-_ ]?)(.*)");
    protected static final Pattern chrPattern = Pattern.compile("(chr(omosome)?[-_ ]?)(.*)");
    protected static final Pattern numberedChrPattern =
        Pattern.compile("(chr(omosome)?[-_ ]?)?(\\d+)");

    protected Map<String, T> sequenceMap = new HashMap<String, T>();
    protected Map<String, T> chrMap = new HashMap<String, T>();
    protected Map<String, T> plasmidMap = new HashMap<String, T>();

    protected int chrCount;
    protected int numCount;
    protected int romansCount;
    protected T singleChrId;

    protected abstract Pattern romanChrPattern();
    protected Matcher romanMatcher(String name) {
        return romanChrPattern().matcher(name.trim().toLowerCase());
    }

    protected void addChromosomeStems(String searchstring, T id) {
        Matcher m = chrPattern.matcher(searchstring);
        if (m.matches()) chrMap.put(m.group(3).toLowerCase(), id);
    }
    protected void addPlasmidStems(String searchstring, T id) {
        Matcher m = plasmidPattern.matcher(searchstring);
        if (m.matches()) plasmidMap.put(m.group(2).toLowerCase(), id);
    }
    protected void checkIfNumberedChromosome(String searchstring) {
        if (numberedChrPattern.matcher(searchstring).matches()) numCount++;
    }
    protected void checkIfRomanNumberedChromosome(String searchstring) {
        Matcher m = romanChrPattern().matcher(searchstring);
        if (m.matches() && Roman.isRoman(m.group(3))) romansCount++;
    }

    /**
     * Map name to standard name, if possible.
     * @param string A string representing a chromosome, plasmid, or other sequence
     * @return the standard name of the sequence
     * @throws RuntimeException if name can't be matched with a standard sequence
     */
    public T map(String name) {
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

        m = romanMatcher(name);
        if (m.matches() && Roman.isRoman(m.group(3))) {
            int num = Roman.romanToInt(m.group(3));
            String key = String.valueOf(num);
            if (chrMap.containsKey(key))
                return chrMap.get(key);
            if (sequenceMap.containsKey(key))
                return sequenceMap.get(key);
            if (chrCount==1 && num==1)
                return singleChrId;
        }
        throw new RuntimeException("Unknown sequence: \"" + name + "\"");
    }
}
