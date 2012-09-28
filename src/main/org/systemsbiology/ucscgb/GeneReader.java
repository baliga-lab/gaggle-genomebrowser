package org.systemsbiology.ucscgb;

import java.io.BufferedReader;
import java.io.Reader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;


/**
 * Read the UCSC GB's output format for the refseq genes table.
 */
public class GeneReader {
	private static final Logger log = Logger.getLogger(GeneReader.class);
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();

	// The refeq table comes in two flavors and doesn't exist for all organisms
	// in the UCSC eukaryotic databases.
	// The ensGene (Ensemble genes) comes in the same format and is more reliably
	// present (but not always) for eukaryotes.

	//  0. bin
	//  1. name
	//  2. chrom
	//  3. strand
	//  4. txStart
	//  5. txEnd
	//  6. cdsStart
	//  7. cdsEnd
	//  8. exonCount
	//  9. exonStarts
	// 10. exonEnds
	// 11. id/score (seems to be a score column in archaeal gb?)
	// 12. name2
	// 13. cdsStartStat
	// 14. cdsEndStat
	// 15. exonFrames

	// #bin	name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonCount	exonStarts	exonEnds	id	name2	cdsStartStat	cdsEndStat	exonFrames

	// For strPur2 and monDom4 (opossum), the table is 10 fields long and is missing the
	// initial bin column.
	// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonCount	exonStarts	exonEnds
	// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonCount	exonStarts	exonEnds

	// rat:
	// GeneReader: Can't read line: NM_001099461	chr1	-	245887	267011	245887	267011	7	245887,256156,257489,258566,259804,260546,266805,	246774,256280,257714,259370,260077,260562,267011,
	// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonCount	exonStarts	exonEnds

	// refGen doesn't exist in anoCar1, anoGam1, droAna2, droMoj2, braFlo1, cavPor3

	// for e. coli K12 eschColi_W3110 all genes are named "locus_tag"
	
	// For yeast, there's an ensGene table with the same format as above and
	// sgdGene and sgdOther with the following columns:
	// #name	chrom	strand	txStart	txEnd	cdsStart	cdsEnd	exonCount	exonStarts	exonEnds	proteinID
	// #bin	chrom	chromStart	chromEnd	name	score	strand	type

	public List<Gene> readUcscTableGenes(Reader reader, boolean removeFragments) {
		int count = 0;
		Gene gene;
		List<Gene> genes = new ArrayList<Gene>();
		BufferedReader brdr = null;
		try {
			brdr = new BufferedReader(reader);
			String line = null;
			while ( (line=brdr.readLine()) != null) {
				if (line.startsWith("#")) continue;

				// catch html error messages
				if (line.startsWith("<!DOCTYPE HTML PUBLIC")) {
					StringBuilder html = new StringBuilder();
					while ( (line=brdr.readLine()) != null) {
						html.append(line).append("\n");
					}
					// extract error msg
					Matcher m = Pattern.compile("(?ms)<!-- HGERROR-START -->\n(.*)\n<!-- HGERROR-END -->").matcher(html);
					if (m.find()) {
						String msg = m.group(1);
						// a cheap and sleazy way to strip out html tags
						throw new RemoteException("Error accessing UCSC data: " + msg.replaceAll("<[a-zA-Z/][^>]*>", ""));
					}
					throw new RemoteException(html.toString());
				}

				String[] fields = line.split("\t");
				if (fields.length < 16) {
					log.warn("Can't read line: " + line);
					continue;
				}
				try {
					String name = fields[1];
					String chrom = fields[2];
					String strand = fields[3];
					int txStart = Integer.parseInt(fields[4]);
					int txEnd = Integer.parseInt(fields[5]);
					int cdsStart = Integer.parseInt(fields[6]);
					int cdsEnd = Integer.parseInt(fields[7]);
					int exonCount = Integer.parseInt(fields[8]);
					String exonStarts = fields[9];
					String exonEnds = fields[10];
					String id = fields[11];
					String name2 = "none".equals(fields[12]) ? null : fields[12];
					String cdsStartStat = fields[13];
					String cdsEndStat = fields[14];
					String exonFrames = fields[15];

					gene = new Gene(name, chrom, strand,
							txStart, txEnd,
							cdsStart, cdsEnd,
							exonCount, exonStarts, exonEnds,
							id, name2,
							cdsStartStat, cdsEndStat, exonFrames, "gene");

					if (!removeFragments || !UCSCGB.isFragment(gene.chrom))
						genes.add(gene);

					count++;
					if (count % 100 == 0)
						progressListenerSupport.fireIncrementProgressEvent(100);
				}
				catch (Exception e) {
					log.warn("can't parse line: " + line, e);
					continue;
				}
			}
			return genes;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (brdr != null) {
				try {
					brdr.close();
				}
				catch (Exception e) {
					log.warn(e);
				}
			}
			progressListenerSupport.fireDoneEvent();
		}
	}

	public List<Gene> flexibleReadGenes(Reader reader, boolean removeFragments) {
		int count = 0;
		Gene gene;
		List<Gene> genes = new ArrayList<Gene>();
		BufferedReader brdr = null;
		try {
			brdr = new BufferedReader(reader);
			String line = brdr.readLine();
			if (!line.startsWith("#"))
				throw new RuntimeException("Expected a line of column headers starting with a #.");

			// build a hash from column name to its index
			Map<String, Integer> positions = new HashMap<String, Integer>();
			String[] columns = line.substring(1).split("\t");
			for (int i=0; i<columns.length; i++) {
				positions.put(columns[i], i);
			}

			// got socket timeouts reading Strongylocentrotus purpuratus genome, which only has 551 rows??
			while ( (line=brdr.readLine()) != null) {
				//log.debug("line=" + line);
				if (line.startsWith("#")) continue;

				// catch html error messages
				if (line.startsWith("<!DOCTYPE HTML PUBLIC")) {
					StringBuilder html = new StringBuilder();
					while ( (line=brdr.readLine()) != null) {
						html.append(line).append("\n");
					}
					// extract error msg
					Matcher m = Pattern.compile("(?ms)<!-- HGERROR-START -->\n(.*)\n<!-- HGERROR-END -->").matcher(html);
					if (m.find()) {
						String msg = m.group(1);
						// a cheap and sleazy way to strip out html tags
						throw new RemoteException("Error accessing UCSC data: " + msg.replaceAll("<[a-zA-Z/][^>]*>", ""));
					}
					throw new RemoteException(html.toString());
				}

				String[] fields = line.split("\t");
				try {
					String name = fields[positions.get("name")];
					String chrom = fields[positions.get("chrom")];
					String strand = fields[positions.get("strand")];
					int txStart = Integer.parseInt(fields[positions.get("txStart")]);
					int txEnd = Integer.parseInt(fields[positions.get("txEnd")]);
					int cdsStart = Integer.parseInt(fields[positions.get("cdsStart")]);
					int cdsEnd = Integer.parseInt(fields[positions.get("cdsEnd")]);
					int exonCount = Integer.parseInt(fields[positions.get("exonCount")]);
					String exonStarts = fields[positions.get("exonStarts")];
					String exonEnds = fields[positions.get("exonEnds")];
					
					String id=null;
					if (positions.containsKey("id"))
						id = fields[positions.get("id")];
					else if (positions.containsKey("score"))
						id = fields[positions.get("score")];

					String name2 = null;
					if (positions.containsKey("name2"))
						name2 = fields[positions.get("name2")];
					if ("none".equals(name2))
						name2 = null;

					String cdsStartStat = null;
					if (positions.containsKey("cdsStartStat"))
						cdsStartStat = fields[positions.get("cdsStartStat")];

					String cdsEndStat = null;
					if (positions.containsKey("cdsEndStat"))
						cdsEndStat = fields[positions.get("cdsEndStat")];

					String exonFrames = null;
					if (positions.containsKey("exonFrames"))
						exonFrames = fields[positions.get("exonFrames")];

					gene = new Gene(name, chrom, strand,
							txStart, txEnd,
							cdsStart, cdsEnd,
							exonCount, exonStarts, exonEnds,
							id, name2,
							cdsStartStat, cdsEndStat, exonFrames, "gene");
					
					if (!removeFragments || !UCSCGB.isFragment(gene.chrom))
						genes.add(gene);

					count++;
					if (count % 100 == 0)
						progressListenerSupport.fireIncrementProgressEvent(100);
				}
				catch (Exception e) {
					log.warn("can't parse line: " + line, e);
					continue;
				}
			}
			return genes;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (brdr != null) {
				try {
					brdr.close();
				}
				catch (Exception e) {
					log.warn(e);
				}
			}
			progressListenerSupport.fireDoneEvent();
		}
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}
}
