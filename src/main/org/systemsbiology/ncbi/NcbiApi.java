package org.systemsbiology.ncbi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.util.HashCounter;
import org.systemsbiology.util.ProgressListener;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

// network connectivity assumed

public class NcbiApi {
	private static final Logger log = Logger.getLogger(NcbiApi.class);
	private static String eutils = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
	private Pattern ncbiESearchIdPattern = Pattern.compile("\\s*<Id>(.*?)<\\/Id>\\s*");
	
	// this is a totally unofficial hack, but it gives a very convenient list of prokaryotic genomes
	private String prokUrl = "http://www.ncbi.nlm.nih.gov/genomes/lproks.cgi?view=1&dump=selected&p3=";
	private int progressInitialized;
	private Object progressLock = new Object();


	String[][] ncbiOrganismCodes = {
			{"6:|7:", "-- All Prokaryotes --"},
			{"6:Archaea|7:", "-- All Archaea --"},
			{"6:|7:Crenarchaeota", "Crenarchaeota"},
			{"6:|7:Euryarchaeota", "Euryarchaeota"},
			{"6:|7:Nanoarchaeota", "Nanoarchaeota"},
			{"6:|7:Other Archaea", "Other Archaea"},
			{"6:Bacteria|7:", "-- All Bacteria --"},
			{"6:|7:Acidobacteria", "Acidobacteria"},
			{"6:|7:Actinobacteria", "Actinobacteria"},
			{"6:|7:Aquificae", "Aquificae"},
			{"6:|7:Bacteroidetes/Chlorobi", "Bacteroidetes/Chlorobi"},
			{"6:|7:Chlamydiae/Verrucomicrobia", "Chlamydiae/Verrucomicrobia"},
			{"6:|7:Chloroflexi", "Chloroflexi"},
			{"6:|7:Cyanobacteria", "Cyanobacteria"},
			{"6:|7:Deinococcus-Thermus", "Deinococcus-Thermus"},
			{"6:|7:Firmicutes", "Firmicutes"},
			{"6:|7:Fusobacteria", "Fusobacteria"},
			{"6:|7:Planctomycetes", "Planctomycetes"},
			{"6:|7:%proteobacteria", "Proteobacteria"},
			{"6:|7:Alphaproteobacteria", "Alphaproteobacteria"},
			{"6:|7:Betaproteobacteria", "Betaproteobacteria"},
			{"6:|7:Gammaproteobacteria", "Gammaproteobacteria"},
			{"6:|7:Deltaproteobacteria", "Deltaproteobacteria"},
			{"6:|7:Epsilonproteobacteria", "Epsilonproteobacteria"},
			{"6:|7:Spirochaetes", "Spirochaetes"},
			{"6:|7:Thermotogae", "Thermotogae"},
			{"6:|7:Other Bacteria", "Other Bacteria"}};

	/**
	 * Translate from human readable menu options to ncbi organism code.
	 * @param organism
	 * @return the organism code or null if none is found
	 */
	public String getNcbiOrganismCode(String organism) {
		for (String[] entry : ncbiOrganismCodes) {
			if (entry[1].equals(organism)) {
				return entry[0];
			}
		}
		return null;
	}
	
	/**
	 * Use the lproks.cgi script to get info on prokaryotic genome projects.
	 * @param p3code the cgi script takes a parameter called p3 which determines the set of organisms returned (see ncbiOrganismCodes)
	 */
	public List<ProkaryoticGenomeProjectSummary> retrieveProkaryoticGenomeProjects(String ncbiOrganismCode) throws Exception {
		URL url = new URL(prokUrl + ncbiOrganismCode);
		log.info("prok url = " + url);
		return readProkaryoticGenomeProjects(new InputStreamReader(url.openStream()));
	}

	public List<ProkaryoticGenomeProjectSummary> readProkaryoticGenomeProjects(Reader reader) throws Exception {
		fireProgressInitEvent();
		List<ProkaryoticGenomeProjectSummary> projects = new ArrayList<ProkaryoticGenomeProjectSummary>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(reader);
			String line = br.readLine();
			int i=0;
			while (line != null) {
				if (!line.startsWith("##")) {
					String[] fields = line.split("\t");
					try {
						ProkaryoticGenomeProjectSummary project = new ProkaryoticGenomeProjectSummary();
						project.setProjectId(fields[0]);
						project.setTaxId(Integer.parseInt(fields[1]));
						project.setOrganismName(fields[2]);
						project.setSuperKingdom(fields[3]);
						project.setGroup(fields[4]);
						project.setGenomeSize(Float.parseFloat(fields[5]));
						project.setGcContent(toFloat(fields[6], null));
						project.setNumberOfChromosomes(Integer.parseInt(fields[7]));
						project.setNumberOfPlasmids(toInt(fields[8], 0));
						project.setReleasedDate(fields[9]);
						project.setAccessions(fields[11].split(","));
						projects.add(project);
						System.out.println(project);
					}
					catch (NumberFormatException e) {
						log.warn("Error reading prokarotic genome projects in line: " + line);
					}
					
					if (i++ % 10 == 0)
						fireIncrementProgressEvent();
				}
				line = br.readLine();
			}
			return projects;
		} catch (Exception e) {
			log.error("Error reading genome projects from NCBI", e);
			throw e;
		}
		finally {
			try {
				if (br != null)
					br.close();
			}
			catch (Exception e) {
				log.warn(e);
			}
			fireProgressDoneEvent();
		}
	}

//	public NcbiGenome retrieveGenome(String genomeProjectId) throws Exception {
//		return retrieveGenome()
//	}

	public NcbiGenome retrieveGenome(final NcbiGenomeProjectSummary summary) throws Exception {
		try {
			fireProgressInitEvent();
			List<NcbiSequence> sequences = this.retrieveSequences(summary.getProjectId());
			fireIncrementProgressEvent();

			// sort sequences by ncbi id?
			Collections.sort(sequences, new Comparator<NcbiSequence>() {
				public int compare(NcbiSequence o1, NcbiSequence o2) {
					return 
						(o1.getNcbiId() > o2.getNcbiId()) ? 1 :
						(o1.getNcbiId() < o2.getNcbiId()) ? -1 :
							0;
				}
			});
			fireIncrementProgressEvent();

			return new NcbiGenomeImpl(summary, sequences);
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	/**
	 * 
	 * @param genomeProjectId
	 */
	public List<NcbiSequence> retrieveSequences(String genomeProjectId) throws Exception {
		List<String> sequenceIds = retrieveGenomeIds(genomeProjectId);
		if (sequenceIds.size()==0)
			throw new RuntimeException("No sequences found for genome project " + genomeProjectId);
		List<NcbiSequence> sequences = new ArrayList<NcbiSequence>();
		for (String sequenceId : sequenceIds) {
			sequences.add(retrieveSequenceAndFeatures(sequenceId));
		}
		
		uniqifySequenceNames(sequences);

		return sequences;
	}

	/**
	 * Return the first projectId found by searching entrez for the given refseq identifier.
	 * (We're assuming there's exactly one result to the search.)
	 */
	public String retrieveIdForRefseq(String refseq) throws Exception {
		URL url = new URL(eutils + "esearch.fcgi?db=genome&term=" + refseq);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		String line = reader.readLine();
		while (line != null) {
			Matcher m = ncbiESearchIdPattern.matcher(line);
			if (m.matches()) {
				return m.group(1);
			}
			line = reader.readLine();
		}
		return null;
	}

	public List<String> retrieveGenomeProjectIds(String organism) throws Exception {
		try {
			fireProgressInitEvent();
			List<String> ids = new ArrayList<String>();
			URL url = new URL(eutils + "esearch.fcgi?db=genomeprj&term=" + URLEncoder.encode(organism, "UTF-8") + "[orgn]");
			log.info("searching genomeprj: " + url);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = reader.readLine();
			while (line != null) {
				Matcher m = ncbiESearchIdPattern.matcher(line);
				if (m.matches()) {
					ids.add(m.group(1));
				}
				line = reader.readLine();
				fireIncrementProgressEvent();
			}
			return ids;
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	/**
	 * get the summary for a genome project and parse out
	 * @param organism
	 * @return
	 * @throws Exception
	 */
	public List<EUtilitiesGenomeProjectSummary> retrieveGenomeProjectSummaries(String organism) throws Exception {
		try {
			fireProgressInitEvent();
			List<EUtilitiesGenomeProjectSummary> summaries = new ArrayList<EUtilitiesGenomeProjectSummary>();
			List<String> ids = retrieveGenomeProjectIds(organism);
			for (String id : ids) {
				summaries.add(retrieveGenomeProjectSummary(id));
				fireIncrementProgressEvent();
			}
			return summaries;
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	public EUtilitiesGenomeProjectSummary retrieveGenomeProjectSummary(String id) throws Exception {
		URL url = new URL(eutils + "esummary.fcgi?db=genomeprj&id=" + id);
		log.info("getting genomeprj summary: " + url);

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder.parse(url.openStream());

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression xname = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Organism_Name\"][1]/text()");
		XPathExpression xkindom = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Organism_Kingdom\"][1]/text()");
		XPathExpression xgroup = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Organism_Group\"][1]/text()");
		XPathExpression xchrom = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Number_of_Chromosomes\"][1]/text()");
		XPathExpression xplasmid = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Number_of_Plasmid\"][1]/text()");
		XPathExpression xmit = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Number_of_Mitochondrion\"][1]/text()");
		XPathExpression xplastid = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Number_of_Plastid\"][1]/text()");
		XPathExpression xstatus = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Sequencing_Status\"][1]/text()");

		EUtilitiesGenomeProjectSummary summary = new EUtilitiesGenomeProjectSummary(id,
				(String)xname.evaluate(doc, XPathConstants.STRING),
				(String)xkindom.evaluate(doc, XPathConstants.STRING),
				(String)xgroup.evaluate(doc, XPathConstants.STRING),
				toInt((String)xchrom.evaluate(doc, XPathConstants.STRING), 0),
				toInt((String)xplasmid.evaluate(doc, XPathConstants.STRING), 0),
				toInt((String)xmit.evaluate(doc, XPathConstants.STRING), 0),
				toInt((String)xplastid.evaluate(doc, XPathConstants.STRING), 0),
				(String)xstatus.evaluate(doc, XPathConstants.STRING));

		return summary;
	}


	/**
	 * given a genome project id, get genome ids for the individual sequences in the genome
	 */
	public List<String> retrieveGenomeIds(String projectId) throws Exception {
		try {
			fireProgressInitEvent();
			List<String> ids = new ArrayList<String>();
			URL url = new URL(eutils + "elink.fcgi?dbfrom=genomeprj&db=genome&id=" + projectId);
			
			log.debug("url = " + url);

			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(url.openStream());
			
			fireIncrementProgressEvent();

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile("/eLinkResult/LinkSet/LinkSetDb/Link/Id/text()");

			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			for (int i = 0; i < nodes.getLength(); i++) {
				ids.add(nodes.item(i).getNodeValue().trim());
				fireIncrementProgressEvent();
			}

			return ids;
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	public NcbiSequence retrieveSequenceSummary(String id) throws Exception {
		try {
			fireProgressInitEvent();

			URL url = new URL(eutils + "esummary.fcgi?db=genome&id=" + id);
	
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(url.openStream());
			
			fireIncrementProgressEvent();
	
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression xtitle = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Title\"]/text()");
			XPathExpression xlength = xpath.compile("/eSummaryResult/DocSum/Item[@Name=\"Length\"]/text()");
	
			fireIncrementProgressEvent();

			return new NcbiSequence(
					(String)xtitle.evaluate(doc, XPathConstants.STRING),
					toInt((String)xlength.evaluate(doc, XPathConstants.STRING), 0));
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	/**
	 * Retrieves the features for a chromosome. Names of the chromosomes are ugly and
	 * not unique. Use cleanupChromosomeNames(...) to fix that.
	 */
	public NcbiSequence retrieveSequenceAndFeatures(String sequenceId) throws Exception {
		URL url = new URL(eutils + "efetch.fcgi?db=genome&retmode=xml&id=" + sequenceId);
		log.debug("url = " + url);

		GbXmlSaxParser gbXmlSaxParser = new GbXmlSaxParser();
		gbXmlSaxParser.addAllProgressListeners(this.listeners);

		List<GeneFeatureImpl> features = gbXmlSaxParser.extractFeatures(url.openStream());

		NcbiSequence seq = new NcbiSequence();
		seq.setName(gbXmlSaxParser.getSequenceName());
		seq.setLength(gbXmlSaxParser.getSequenceLength());
		seq.setAccession(gbXmlSaxParser.getAccession());
		seq.setLocus(gbXmlSaxParser.getSequenceLocus());
		seq.setUpdateDate(gbXmlSaxParser.getUpdateDate());
		seq.setDefinition(gbXmlSaxParser.getDefinition());
		seq.setNcbiId(Long.parseLong(sequenceId));
		seq.setGenes(features);

		log.debug("retrieved features for " + seq.getAccession());

		return seq;
	}

	private int toInt(String string, int defaultValue) {
		try {
			return Integer.parseInt(string);
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	private Float toFloat(String string, Float defaultValue) {
		try {
			return Float.parseFloat(string);
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Should be unnecessary
	 */
	public void uniqifySequenceNames(List<NcbiSequence> sequences) {
		HashCounter counter = new HashCounter();
		for (NcbiSequence sequence: sequences) {
			counter.increment(sequence.getName());
		}

		// make the names of the chromosomes unique by adding numbers
		HashCounter numbers = new HashCounter();
		for (NcbiSequence sequence: sequences) {
			String name = sequence.getName();
			if (counter.get(name) > 1) {
				numbers.increment(name);
				sequence.setName(name + "." + numbers.get(name) + "");
			}
		}
	}


	Set<ProgressListener> listeners = new CopyOnWriteArraySet<ProgressListener>();

	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}

	void fireIncrementProgressEvent() {
		for (ProgressListener listener : listeners) {
			listener.incrementProgress(1);
		}
	}

	void fireProgressInitEvent() {
		synchronized (progressLock) {
			if (progressInitialized <= 0) {
				for (ProgressListener listener : listeners) {
					listener.init(0);
				}
			}
			progressInitialized++;
			System.out.println("+ progress init = " + progressInitialized);
		}
	}

	void fireProgressDoneEvent() {
		synchronized (progressLock) {
			if (progressInitialized > 0) {
				progressInitialized--;
				System.out.println("- progress init = " + progressInitialized);
			}
			if (progressInitialized == 0) {
				System.out.println("d progress init = " + progressInitialized);
				for (ProgressListener listener : listeners) {
					listener.done();
				}
			}
		}
	}
}