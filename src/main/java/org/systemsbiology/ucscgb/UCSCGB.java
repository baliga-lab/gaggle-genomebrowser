package org.systemsbiology.ucscgb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.util.FileUtils;
import static org.systemsbiology.util.StringUtils.isNullOrEmpty;


/**
 * Programmatically grab data from the UCSC Genome Browser. Constructs HTTP
 * requests that invoke the Table Browser feature for either the main genome
 * browser for eukaryotes or the archaeal genome browser for prokaryotes.
 * 
 * Usable as a command-line utility.
 * 
 * See extra documentation here:
 * http://digitheadslabnotebook.blogspot.com/2009/02/spelunking-in-ucsc-genome-browser.html
 * 
 * @author cbare
 */
public class UCSCGB {
	private static final Logger log = Logger.getLogger(UCSCGB.class);
	private static final Pattern dbPattern = Pattern.compile(".*[\\D](\\d+)");
	private static final String UCSC_GENOMES_FILE = "ucsc.genomes.tsv";
	private static final String UCSC_GB = "http://genome.ucsc.edu";
	private static final String UCSC_MICROBIAL_GB = "http://microbes.ucsc.edu";
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();

	
	// For the time being, the source file of ucsc genomes contains only the
	// most recent assembly of each organism. Later, we may want to give the
	// user the option of using older assemblies.

	public List<Genome> loadGenomes() {
		return loadGenomes(Category.all);
	}

	/**
	 * Load (from a local file) a list of supported genomes in the given
	 * category or in all categories if category is null.
	 */
	public List<Genome> loadGenomes(Category category) {
		List<Genome> genomes = new ArrayList<Genome>();
		BufferedReader brdr = null;
		try {
			brdr = new BufferedReader(FileUtils.getReaderForResource(UCSC_GENOMES_FILE));
			String line;
			while ( (line = brdr.readLine()) != null) {
				if (line.startsWith("#")) continue;

				String[] fields = line.split("\t");
				if (fields.length < 7) continue;

				// 0:dbName, 1:description, 2:genome, 3:scientificName, 4:domain, 5:clade, 6:taxid, 7:geneTable(optional)
				Category lineCategory = null;
				try {
					lineCategory = Category.fromDomainAndClade(fields[4], fields[5]);
				}
				catch (IllegalArgumentException e) {
					log.warn(e);
					continue;
				}
				if (category==null || lineCategory.isA(category)) {
					//new Genome(dbName, description, genome, scientificName, taxid, clade, domain)
					Genome genome = new Genome(fields[0], fields[1], fields[2], fields[3], Long.parseLong(fields[6]), fields[5], fields[4]);
					if (fields.length>7)
						genome.setGeneTable(fields[7]);
					genomes.add(genome);
				}
			}
			return filterOnlyMostRecent(genomes);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (brdr!=null) {
				try {
					brdr.close();
				}
				catch (Exception e) {
					log.warn(e);
				}
			}
		}
	}

	public Genome loadGenome(Category category, String dbName) {
		List<Genome> genomes = loadGenomes(category);
		for (Genome genome: genomes) {
			if (genome.dbName.equals(dbName))
				return genome;
		}
		throw new RuntimeException("Genome " + category + ":" + dbName + " not found.");
	}

	/**
	 * The data from the UCSC genome browser contains several assemblies for some
	 * organism. Maybe we'll allow the user to pick on in the future, but for now
	 * we just filter out all but the most recent.
	 */
	List<Genome> filterOnlyMostRecent(List<Genome> genomes) {
		Map<Long, Genome> map = new HashMap<Long, Genome>(genomes.size());
		List<Genome> results = new ArrayList<Genome>();
		for (Genome g : genomes) {
			Genome g2 = map.get(g.getTaxid());
			if ((g2 == null) || (dbToInt(g2.getDbName()) < dbToInt(g.getDbName())))
				map.put(g.getTaxid(), g);
		}
		results.addAll(map.values());
		Collections.sort(results, new Comparator<Genome>() {
			public int compare(Genome g1, Genome g2) {
				return g1.getScientificName().compareTo(g2.getScientificName());
			}
		});
		return results;
	}

	private int dbToInt(String db) {
		Matcher m = dbPattern.matcher(db);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		else
			return 0;
	}
	
	/**
	 * return genes (protein coding and RNA) from the specified database.
	 * @param category domain or clade
	 * @param dbName UCSC database name (determined by species)
	 * @param removeFragments remove unassembled fragments and alternate haplotypes
	 * @return List of Genes
	 */
	public List<Gene> genes(Category category, String dbName, boolean removeFragments) throws Exception {
		List<Gene> genes = refSeqGenes(category, dbName, removeFragments);
		genes.addAll(rnas(category, dbName, removeFragments));
		return genes;
	}

	public List<Gene> genes(Genome genome, boolean removeFragments) throws Exception {
		List<Gene> genes = readGenes(genome, removeFragments);
		genes.addAll(rnas(genome.getCategory(), genome.getDbName(), removeFragments));
		return genes;
	}

	private String getUrlForGenes(Genome genome) {
		if (genome.getCategory().isProkaryotic() || genome.getCategory().isA(Category.virus)) {
			return UCSC_MICROBIAL_GB + "/cgi-bin/hgTables?" +
			"db=" + genome.getDbName() +
			"&hgta_group=genes" +
			"&hgta_track=refSeq" +
			"&hgta_table=refSeq" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";		
		}
		else {
			String table = isNullOrEmpty(genome.getGeneTable()) ? "ensGene" : genome.getGeneTable();
			return UCSC_GB + "/cgi-bin/hgTables?" +
			"db=" + genome.getDbName() +
			"&hgta_group=genes" +
			"&hgta_track=" + table +
			"&hgta_table=" + table +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
		}
	}

	public List<Gene> readGenes(Genome genome, boolean removeFragments) throws Exception {
		GeneReader geneReader = new GeneReader();
		geneReader.addProgressListener(progressListenerSupport);
		String url = getUrlForGenes(genome);
		log.info("Reading genes from: " + url);
		return geneReader.flexibleReadGenes(getReaderForPath(url), removeFragments);
	}

	/**
	 * Retrieve the refSeq genes table from Table Browser.
	 * @param category all we care about is eukaryotic vs. prokaryotic
	 * @param dbName UCSC's dbName for an organism
	 * @throws IOException 
	 */
	public List<Gene> refSeqGenes(Category category, String dbName, boolean removeFragments) throws Exception {
		String path = null;
		if (category.isEukaryotic()) {
			path = UCSC_GB + "/cgi-bin/hgTables?" +
			"db=" + dbName +
			"&hgta_group=genes" +
			"&hgta_track=refGene" +
			"&hgta_table=refGene" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
		}
		else {
			path = UCSC_MICROBIAL_GB + "/cgi-bin/hgTables?" +
			"db=" + dbName +
			"&hgta_group=genes" +
			"&hgta_track=refSeq" +
			"&hgta_table=refSeq" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
		}
		GeneReader geneReader = new GeneReader();
		geneReader.addProgressListener(progressListenerSupport);
		return geneReader.readUcscTableGenes(getReaderForPath(path), removeFragments);
	}

	public List<Gene> rnas(Category category, String dbName, boolean removeFragments) throws Exception {
		String path = null;
		if (category.isEukaryotic()) {
			// TODO get RNAs for eukaryotes from UCSC?
			return Collections.emptyList();
		}
		else {
			path = UCSC_MICROBIAL_GB + "/cgi-bin/hgTables?" +
			"db=" + dbName +
			"&hgta_group=genes" +
			"&hgta_track=gbRNAs" +
			"&hgta_table=gbRNAs" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
			log.info("reading RNAs from URL:" + path);
			RnaReader rnaReader = new RnaReader();
			return rnaReader.readRnas(getReaderForPath(path), removeFragments);
		}
	}

	/**
	 * Get the names and sizes of the organism's chromosomes.
	 * @param category eukaryotic vs prokaryotic, mostly
	 * @param dbName UCSC's dbName parameter.
	 * @throws IOException 
	 */
	public List<Chromosome> chromInfo(Category category, String dbName, boolean removeFragments) throws IOException {
		String path = null;
		if (category==null) throw new RuntimeException("Can't determine category for \"" + dbName + "\".");
		if (category.isEukaryotic()) {
			path = UCSC_GB + "/cgi-bin/hgTables?" +
			"db=" + dbName +
			"&hgta_group=allTables" +
			"&hgta_table=chromInfo" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
		}
		else {
			// 2010-5-7: added hgta_track=refSeq as suggested by pchan@soe.ucsc.edu
			path = UCSC_MICROBIAL_GB + "/cgi-bin/hgTables?" +
			"db=" + dbName +
			"&hgta_group=allTables" +
			"&hgta_track=refSeq" +
			"&hgta_table=chromInfo" +
			"&hgta_regionType=genome" +
			"&hgta_outputType=primaryTable" +
			"&hgta_doTopSubmit=";
		}
		log.info("reading chromosome info from: " + path);
		ChromInfoReader cir = new ChromInfoReader();
		return cir.readChromInfo(getReaderForPath(path), removeFragments);
	}

	/**
	 * If path is a URL, open a connection and get a reader.
	 */
	public Reader getReaderForPath(String path) throws IOException {
		URL url = new URL(path);
		URLConnection conn = url.openConnection();
		
		// got socket timeouts downloading the 
//		conn.setConnectTimeout(3000);
//		conn.setReadTimeout(5000);
		conn.connect();
		
		Charset charset = getCharset(conn);

		return new InputStreamReader(conn.getInputStream(), charset);
	}

	/**
	 * A simple hack to try and guess whether a chromosome name is an
	 * unassembled fragment or a real chromosome or plasmid.
	 * @return true if the name contains an underscore and is not a plasmid
	 */
	public static boolean isFragment(String name) {
		name = name.toLowerCase();
		return (name.startsWith("chrun")) || name.startsWith("scaffold") || (name.contains("_") && !name.contains("plasmid"));
	}

	/**
	 * Get the charset for the given URLConnection looking first in the
	 * content-encoding header, then in the content-type header, and failing
	 * both of those returning the local default character set.
	 */
	public static Charset getCharset(URLConnection conn) {
		// I wanted to use conn.getContentEncoding() here, but it returns null. We
		// do get Content-Type=[text/plain; charset=ISO-8859-1] in the headers. So,
		// let's first try getContentEncoding, failing that, try contentType, and
		// failing that, use the default Charset. Not sure this does anything useful.

		try {
			String encoding = conn.getContentEncoding();
			if (encoding == null) {
				String type = conn.getContentType();
				if (type != null) {
					Matcher m = Pattern.compile("text/plain; charset=(.*)").matcher(type);
					if (m.matches()) {
						encoding = m.group(1);
					}
				}
			}
			return Charset.forName(encoding); 
		}
		catch (Exception e) {
			return Charset.defaultCharset();
		}
	}


	/**
	 * If we're given the organism name, we need to figure out its dbName and
	 * its category (Eukaryotic vs. Prokaryotic, mostly) so we can properly
	 * access the UCSC's Table Browser. We may be given the dbName, in which
	 * case, we still need to figure out the category.
	 */
	private Category deriveMissingValues(Category category, Options options) {
		if (category==null || options.dbName==null) {
			List<Genome> genomes = loadGenomes(category);
			
			if (options.dbName==null) {
				for (Genome genome: genomes) {
					if (options.organism.equals(genome.getScientificName()) || options.organism.equals(genome.getGenome())) {
						options.dbName = genome.getDbName();
						return Category.valueOf(genome.getDomain());
					}
				}
				throw new RuntimeException("Organism " + options.organism + " not found.");
			}
			else {
				for (Genome genome: genomes) {
					if (options.dbName.equals(genome.getDbName())) {
						options.organism = genome.getScientificName();
						return Category.valueOf(genome.getDomain());
					}
				}
				throw new RuntimeException("db " + options.dbName + " not found.");
			}
		}
		return category;
	}

	private static void print(Object object) {
		if (object instanceof Collection<?>) {
			Collection<?> col = (Collection<?>)object;
			for (Object item : col) {
				System.out.println(item.toString());
			}
		}
		else if (object instanceof Genome) {
			System.out.println(((Genome)object).toDebugString());
		}
		else {
			System.out.println(object.toString());
		}
	}


	/**
	 * UCSCGB can be run as a command line utility.
	 * @see Options.usage() for command line options.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.parseArgs(args);
		UCSCGB gb = new UCSCGB();

		if (!options.validate() || "help".equals(options.cmd)) {
			System.out.println(options.usage());
			return;
		}
			
		Category category = null;
		try {
			category = Category.valueOf(options.category);
		}
		catch(Exception e) {}

		if ("list".equals(options.cmd)) {
			print(gb.loadGenomes(category));
		}
		else {
			// figure out category from organism or dbName
			// figure out dbName from organism
			try {
				category = gb.deriveMissingValues(category, options);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
				return;
			}

			if ("chromInfo".equals(options.cmd)) {
				print(gb.chromInfo(category, options.dbName, options.removeAlternateHaplotypes));
			}
			else if ("genes".equals(options.cmd)) {
				print(gb.genes(category, options.dbName, options.removeAlternateHaplotypes));
			}
			else if ("genome".equals(options.cmd)) {
				print(gb.loadGenome(category, options.dbName));
			}
			else {
				System.out.println("Error: Unknown command: " + options.cmd);
			}
		}
	}

	public boolean hasSpecies(String organism) {
		// TODO make this more efficient - shouldn't need to go to disk.
		List<Genome> genomes = loadGenomes();
		for (Genome genome: genomes) {
			if (organism.equals(genome.getGenome()) || organism.equals(genome.getScientificName()))
				return true;
		}
		return false;
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}
}
