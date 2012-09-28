package org.systemsbiology.ncbi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.FeatureComparator;
import org.systemsbiology.util.ProgressListener;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * A not-very-strict parser for the NCBI GB XML format.
 * 
 * This parser is missing support for at least 2 features of GB XML. We don't
 * read the GBSeq_contig, with which NCBI links to the subsequences of mammalian
 * genome assemblies (as far as I understand). Also, we don't read exons and
 * introns out of the GBFeature_intervals element. This parser was written using
 * examples and not a specification for GB XML. A spec probably exists, but if
 * so, it's well hidden.
 * 
 * @author cbare
 */
public class GbXmlSaxParser {
	private static final Logger log = Logger.getLogger(GbXmlSaxParser.class);
	String sequenceLocus;
	String sequenceName;
	int sequenceLength;
	String definition;
	String organism;
	String accession;
	String updateDate;
	int progress = 0;


	public String getSequenceLocus() {
		return sequenceLocus;
	}

	public int getSequenceLength() {
		return sequenceLength;
	}

	public String getDefinition() {
		return definition;
	}

	public String getOrganism() {
		return organism;
	}

	public String getAccession() {
		return accession;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public String getUpdateDate() {
		return updateDate;
	}

	public List<GeneFeatureImpl> extractFeatures(InputStream inputStream) throws Exception {
		try {
			fireProgressInitEvent();
			XMLReader parser = XMLReaderFactory.createXMLReader();
			GbContentHandler handler = new GbContentHandler();
			parser.setContentHandler(handler);
			parser.parse(new InputSource(inputStream));
			if (handler.errorMessage != null)
				throw new RuntimeException("Error code from NCBI: " + handler.errorMessage);
			
			List<GeneFeatureImpl> cdsFeatures = handler.getFeatures();
			Collections.sort(cdsFeatures, new FeatureComparator());
			return cdsFeatures;
		}
		finally {
			fireProgressDoneEvent();
		}
	}

	class GbContentHandler implements ContentHandler {
		List<GeneFeatureImpl> cdsFeatures = new ArrayList<GeneFeatureImpl>();
		LinkedList<String> elements = new LinkedList<String>();
		List<Interval> intervals = new LinkedList<Interval>();
		StringBuilder buffer = new StringBuilder();
		String qualifierName, qualifierValue;
		String key;
		String errorMessage;
		int start = -1;
		int end = -1;
		boolean gbset;
		boolean gbseq;
		boolean error;
		boolean gbfeaturetable;
		boolean gbfeature;
		boolean gbqualifier;
		boolean gbintervals;
		boolean gbinterval;

		String topology;
		String featureName;
		String featureCommonName;
		String ncRnaClass;
		GeneFeatureType featureType;

/*
	features are at: /GBSet/GBSeq/GBSeq_feature-table/GBFeature
	features look like this:

    <GBFeature>
      <GBFeature_key>CDS</GBFeature_key>
      <GBFeature_location>248..1453</GBFeature_location>
      <GBFeature_intervals>
        <GBInterval>
          <GBInterval_from>248</GBInterval_from>
          <GBInterval_to>1453</GBInterval_to>
          <GBInterval_accession>NC_002607.1</GBInterval_accession>
        </GBInterval>
      </GBFeature_intervals>
      <GBFeature_quals>
        <GBQualifier>
          <GBQualifier_name>locus_tag</GBQualifier_name>
          <GBQualifier_value>VNG0001H</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>codon_start</GBQualifier_name>
          <GBQualifier_value>1</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>transl_table</GBQualifier_name>
          <GBQualifier_value>11</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>product</GBQualifier_name>
          <GBQualifier_value>hypothetical protein</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>protein_id</GBQualifier_name>
          <GBQualifier_value>NP_279165.1</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>db_xref</GBQualifier_name>
          <GBQualifier_value>GI:15789341</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>db_xref</GBQualifier_name>
          <GBQualifier_value>GeneID:1446951</GBQualifier_value>
        </GBQualifier>
        <GBQualifier>
          <GBQualifier_name>translation</GBQualifier_name>
          <GBQualifier_value>MTRRSRVGAGLAAIVLALAAVSAAAPIAGAQSAGSGAVSVTIGDVDVSPANPTTGTQVLITPSINNSGSASGSARVNEVTLRGDGLLATEDSLGRLGAGDSIEVPLSSTFTEPGDHQLSVHVRGLNPDGSVFYVQRSVYVTVDDRTSDVGVSARTTATNGSTDIQATITQYGTIPIKSGELQVVSDGRIVERAPVANVSESDSANVTFDGASIPSGELVIRGEYTLDDEHSTHTTNTTLTYQPQRSADVALTGVEASGGGTTYTISGDAANLGSADAASVRVNAVGDGLSANGGYFVGKIETSEFATFDMTVQADSAVDEIPITVNYSADGQRYSDVVTVDVSGASSGSATSPERAPGQQQKRAPSPSNGASGGGLPLFKIGGAVAVIAIVVVVVRRWRNP</GBQualifier_value>
        </GBQualifier>
      </GBFeature_quals>
    </GBFeature>
		
*/

		public List<GeneFeatureImpl> getFeatures() {
			return cdsFeatures;
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			buffer.append(ch, start, length);
		}

		public void startDocument() throws SAXException {
			buffer.setLength(0);
		}

		public void endDocument() throws SAXException {
		}


		public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
			elements.addFirst(localName);
			if (gbset) {
				if (gbseq) {
					if (gbfeaturetable) {
						if (gbfeature) {
							if (gbintervals) {
								if ("GBInterval".equals(localName)) {
									gbinterval = true;
								}
							}
							else if ("GBQualifier".equals(localName)) {
								gbqualifier = true;
							}
							else if ("GBFeature_intervals".equals(localName)) {
								gbintervals = true;
							}
						}
						else if ("GBFeature".equals(localName)) {
							gbfeature = true;
						}
					}
					if ("GBSeq_feature-table".equals(localName)) {
						gbfeaturetable = true;
					}
					if ("GBSeq_contig".equals(localName)) {
						throw new GbXmlParseException("Encountered GBSeq_contig");
					}
				}
				else if ("GBSeq".equals(localName)) {
					gbseq = true;
				}
				else if ("Error".equals(localName)) {
					error = true;
				}
			}
			else if ("GBSet".equals(localName)) {
				gbset = true;
			}
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
			elements.removeFirst();
			if (gbset) {
				if (gbseq) {
					if (gbfeaturetable) {
						if (gbfeature) {
							if (gbqualifier) {
								if ("GBQualifier_name".equals(localName)) {
									qualifierName = buffer.toString();
								}
								else if ("GBQualifier_value".equals(localName)) {
									qualifierValue = buffer.toString();
								}
								else if ("GBQualifier".equals(localName)) {
									if ("locus_tag".equals(qualifierName))
										featureName = qualifierValue;
									else if ("gene".equals(qualifierName))
										featureCommonName = qualifierValue;
									else if ("ncRNA_class".equals(qualifierName))
										ncRnaClass = qualifierValue;
									gbqualifier = false;
									qualifierName = null;
									qualifierValue = null;
								}
							}
							// TODO we should use <GBFeature_location>189125..189985</GBFeature_location>?
							else if (gbintervals) {
								if (gbinterval) {
									if ("GBInterval_from".equals(localName)) {
										start = Integer.parseInt(buffer.toString());
									}
									else if ("GBInterval_to".equals(localName)) {
										end = Integer.parseInt(buffer.toString());
									}
									else if ("GBInterval".equals(localName)) {
										gbinterval = false;
										intervals.add(new Interval(start, end));
										start = 0;
										end = 0;
									}
								}
								else if ("GBFeature_intervals".equals(localName)) {
									gbintervals = false;
								}
							}
							else if ("GBFeature".equals(localName)) {

								// combine multiple intervals into one.
								// For now we ignore introns. Genes just have start and end.
								// One special case is when a feature straddles the numeric
								// "endpoints" of a circular genome. For now we deal with that
								// by returning an interval that hangs off the end of the sequence,
								// which is not really optimal. Later, we may have to introduce
								// features with multiple subintervals.
								Interval interval = Interval.enclosingIntervalCircular(intervals, sequenceLength);
								intervals.clear();

								Strand strand = interval.getStrand();
								featureType = stringToFeatureType(key);
								if (featureName == null)
									if (ncRnaClass==null)
										if (key==null)
											featureName = "feature";
										else
											featureName = key;
									else
										featureName = ncRnaClass;
								GeneFeatureImpl feature = new GeneFeatureImpl(sequenceName, strand, interval.getStart(), interval.getEnd(), featureName, featureCommonName, featureType);

								if ("CDS".equals(key) || key.contains("RNA") || "Gene".equals(key)) {
									cdsFeatures.add(feature);
								}

								gbfeature = false;
								featureName = null;
								featureCommonName = null;
								ncRnaClass = null;
								key = null;
								topology = null;
								featureType = null;
							}
							else if ("GBFeature_key".equals(localName)) {
								key = buffer.toString();
							}
						}
						else if ("GBSeq_feature-table".equals(localName)) {
							gbfeaturetable = false;
						}
					}
					else if ("GBSeq".equals(localName)) {
						gbseq = false;
					}
					else {
						if ("GBSeq_locus".equals(localName)) {
							sequenceLocus = buffer.toString();
						}
						else if ("GBSeq_length".equals(localName)) {
							sequenceLength = Integer.parseInt(buffer.toString());
						}
						else if ("GBSeq_definition".equals(localName)) {
							definition = buffer.toString();
						}
						else if ("GBSeq_organism".equals(localName)) {
							organism = buffer.toString();
							// put this here 'cause organism seems to come after definition
							sequenceName = sequenceNameFromDefinition(organism, definition);
						}
						else if ("GBSeq_primary-accession".equals(localName)) {
							accession = buffer.toString();
						}
						else if ("GBSeq_topology".equals(localName)) {
							topology = buffer.toString();
						}
						else if ("GBSeq_update-date".equals(localName)) {
							updateDate = buffer.toString();
						}
					}
				}
				else if (error) {
					if ("Error".equals(localName)) {
						errorMessage = buffer.toString();
					}
				}
				else if ("GBSet".equals(localName)) {
					gbset = false;
				}
			}
			
			buffer.setLength(0);
			
			progress++;
			if (progress % 100 == 0)
				fireIncrementProgressEvent();
		}


		public void endPrefixMapping(String prefix) throws SAXException {
		}

		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		public void processingInstruction(String target, String data) throws SAXException {
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void skippedEntity(String name) throws SAXException {
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}
	
		// --------------------------------------------------------------------
		
		//TODO stringToFeatureType can be made more complete
		private GeneFeatureType stringToFeatureType(String s) {
			if (s==null)
				return GeneFeatureType.other;
			s = s.toLowerCase();
			if ("misc_rna".equals(s))
				s = "rna";
			try {
				return GeneFeatureType.valueOf(s);
			}
			catch (Exception e) {
				log.warn("unrecognized feature type: \"" + String.valueOf(s) + "\".");
				return GeneFeatureType.other;
			}
		}

		/**
		 * Try to munge the definition field into a reasonable name for the sequence
		 */
		private String sequenceNameFromDefinition(String organismName, String definition) {
			String name = definition;
			if (name.equals(organismName + ", complete genome")) {
				return "chromosome";
			}
			else {
				int i = name.indexOf(", complete genome");
				if (i > -1) {
					name = name.substring(0, i);
				}
				i = name.indexOf(", complete sequence");
				if (i > -1) {
					name = name.substring(0, i);
				}
				if (name.startsWith(organismName + " ")) {
					name = name.substring(organismName.length() + 1);
				}
			}
			if (!"chromosome".equals(name) && name.indexOf("chromosome") > -1) {
				name = name.replace("chromosome", "");
				name = name.trim();
			}
			return name;
		}
	}


	Set<ProgressListener> listeners = new CopyOnWriteArraySet<ProgressListener>();

	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}

	public void addAllProgressListeners(Collection<ProgressListener> collection) {
		listeners.addAll(collection);
	}

	public void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}

	public void fireIncrementProgressEvent() {
		for (ProgressListener listener : listeners) {
			listener.incrementProgress(1);
		}
	}

	public void fireProgressInitEvent() {
		for (ProgressListener listener : listeners) {
			listener.init(0);
		}
	}

	public void fireProgressDoneEvent() {
		for (ProgressListener listener : listeners) {
			listener.done();
		}
	}

	/**
	 * An interval is a part of a sequence with a start and end coordinate.
	 * The strand is indicated by: forward => start < end, reverse => end < start.
	 * @author cbare
	 */
	static class Interval {
		int start;
		int end;

		public Interval(int start, int end) {
			this.start = start;
			this.end = end;
		}

		/**
		 * > 0 => forward
		 * < 0 => reverse
		 * = 0 => start = end
		 * @return
		 */
		public int direction() {
			return end-start;
		}

		public Strand getStrand() {
			return start<end ? Strand.forward : start>end ? Strand.reverse : Strand.none;
		}

		public int getStart() {
			return Math.min(start, end);
		}

		public int getEnd() {
			return Math.max(start, end);
		}

		
		/**
		 * Compute the enclosing interval given a list of subintervals.
		 * Use enclosingIntervalCircular(...) for circular DNA. 
		 */
		static Interval enclosingInterval(List<Interval> intervals) {
			if (intervals.size() == 0)
				return new Interval(0,0);
			Interval interval = intervals.get(0);
			Interval result = new Interval(interval.start, interval.end);
			boolean forward = interval.start < interval.end;
			for (int i=1; i<intervals.size(); i++) {
				interval = intervals.get(i);
				if (forward) {
					result.start = Math.min(result.start, interval.start);
					result.end   = Math.max(result.end, interval.end);
				}
				else {
					result.start = Math.max(result.start, interval.start);
					result.end   = Math.min(result.end, interval.end);
				}
			}
			return result;
		}

		/**
		 * Compute the enclosing interval for circular DNA taking into account
		 * the special case where features span the zero point. The list of
		 * intervals is assumed to be ordered in the same orientation as the
		 * individual intervals.
		 */
		static Interval enclosingIntervalCircular(List<Interval> intervals, int length) {
			if (intervals.size() == 0)
				return new Interval(0,0);
			Interval interval = intervals.get(0);
			Interval result = new Interval(interval.start, interval.end);

			// check for features crossing the zero point
			Interval last = intervals.get(intervals.size()-1);
			if (interval.direction() >= 0 && last.direction() >= 0 && interval.start > last.end) {
				log.warn("feature spanning zero point detected (+).");
				return new Interval(interval.start, last.end + length);
			}
			else if (interval.direction() <= 0 && last.direction() <= 0 && interval.start < last.end) {
				log.warn("feature spanning zero point detected (-).");
				return new Interval(interval.start, last.end - length);
			}

			boolean forward = interval.start < interval.end;
			for (int i=1; i<intervals.size(); i++) {
				interval = intervals.get(i);

				if (forward) {
					result.start = Math.min(result.start, interval.start);
					result.end   = Math.max(result.end, interval.end);
				}
				else {
					result.start = Math.max(result.start, interval.start);
					result.end   = Math.min(result.end, interval.end);
				}
			}
			return result;
		}
	}

	/**
	 * Signals an error in parsing the XML format from NCBI.
	 */
	public static class GbXmlParseException extends RuntimeException {
		public GbXmlParseException(String msg) {
			super(msg);
		}

		public GbXmlParseException(String message, Throwable cause) {
			super(message, cause);
		}

		public GbXmlParseException(Throwable cause) {
			super(cause);
		}
	}
}
