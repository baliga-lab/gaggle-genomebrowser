package org.systemsbiology.ucscgb;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import cbare.util.AlphanumericComparator;

/**
 * Read the UCSC GB's output format for the ChromInfo table.
 */
public class ChromInfoReader {
    private static final Logger log = Logger.getLogger(ChromInfoReader.class);

    /**
     * Filters the unassembled fragments out of a list of chromosomes.
     * @return a new list
     */
    public List<Chromosome> removeUnassembledFragments(List<Chromosome> chromosomes) {
        List<Chromosome> results = new ArrayList<Chromosome>();
        for (Chromosome c : chromosomes) {
            if (!UCSCGB.isFragment(c.getName())) results.add(c);
        }
        return results;
    }

    /**
     * Read a chromInfo table from the given reader.
     */
    public List<Chromosome> readChromInfo(Reader reader, boolean removeFragments) {
        List<Chromosome> chromosomes = new ArrayList<Chromosome>();
        BufferedReader brdr = null;
        String line = null;
        try {
            brdr = new BufferedReader(reader);
            while ( (line=brdr.readLine()) != null) {
                log.debug(line);
                try {
                    if (line.startsWith("#")) continue;
                    String[] fields = line.split("\t");
                    if (!removeFragments || !UCSCGB.isFragment(fields[0]))
                        chromosomes.add(new Chromosome(fields[0], Integer.parseInt(fields[1])));
                }	catch (Exception e) {
                    log.error(e);
                }
            }
			
            Collections.sort(chromosomes, new Comparator<Chromosome>() {
                    AlphanumericComparator comp = new AlphanumericComparator();
                    public int compare(Chromosome c1, Chromosome c2) {
                        return comp.compare(c1.getName(), c2.getName());
                    }
                });
            return chromosomes;
        }	catch (Exception e) {
            log.error("exception parsing line: \"" + line + "\"", e);
            throw new RuntimeException(e);
        }	finally {
            if (brdr != null) {
                try {
                    brdr.close();
                }	catch (Exception e) {
                    log.warn(e);
                }
            }
        }
    }
}
