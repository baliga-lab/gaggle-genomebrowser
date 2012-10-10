/**
 * 
 */
package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.genomebrowser.app.ProgressListenerWrapper;
import org.systemsbiology.genomebrowser.io.LineReader;
import org.systemsbiology.genomebrowser.io.LineReader.LineProcessor;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.MathUtils;


/**
 * Gets lines of text from a GFF file, divides them into fields, and calls a
 * FeatureProcessor with the results.
 */
public class GffLineProcessorAdapter implements FeatureSource, LineProcessor {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GffLineProcessorAdapter.class);
	private FeatureProcessor featureProcessor;
	private long fileLengthInBytes;
	private String path;
	private File file;
	private Reader reader;
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();
	private boolean hasColumnHeaders;
	private GffFeatureFields featureFields = new GffFeatureFields();
	private static Pattern comment = Pattern.compile("\\s*#.*");
	private static Pattern blank = Pattern.compile("\\s*");


	public GffLineProcessorAdapter(String path) {
		this.path = path;
		try {
			URL url = FileUtils.getUrlForResource(path);
			URLConnection conn = url.openConnection();
			conn.connect();
			fileLengthInBytes = conn.getContentLength();
			reader = new InputStreamReader(conn.getInputStream());
		}
		catch (Exception e) {
			this.file = new File(path);
			this.fileLengthInBytes = file.length();
		}
	}

	public GffLineProcessorAdapter(File file) {
		this.file = file;
		this.fileLengthInBytes = file.length();
	}

	public GffLineProcessorAdapter(Reader reader, long lengthInBytes) {
		this.reader = reader;
		this.fileLengthInBytes = lengthInBytes;
	}

	public void setHasColumnHeaders(boolean hasColumnHeaders) {
		this.hasColumnHeaders = hasColumnHeaders;
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}

	/**
	 * Calls featureProcessor.process for each feature
	 */
	public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
		this.featureProcessor = featureProcessor;
		LineReader loader = new LineReader(this);
		loader.setFileLengthInBytes(fileLengthInBytes);
		ProgressListenerWrapper plw = new ProgressListenerWrapper(progressListenerSupport);

		try {
			// forward loader's progress events
			loader.addProgressListener(plw);
	
			if (reader != null)
				loader.loadData(reader);
			else if (file != null)
				loader.loadData(file);
			else
				loader.loadData(path);
		}
		finally {
			loader.removeProgressListener(plw);
		}
	}

	/**
	 * PreviewLoader uses process() as a call-back. 
	 */
	public void process(int lineNumber, String line) throws Exception {
		// here we adapt from LineProcessor to FeatureProcessor by making a
		// feature out of the line of text.

		// ignore column headers and comments
		if ( (lineNumber==0 && hasColumnHeaders) || comment.matcher(line).matches() || blank.matcher(line).matches())
			return;

		featureProcessor.process(featureFields.digest(line));
	}
}

class GffFeatureFields implements FeatureFields {
	private static final Logger log = Logger.getLogger(GffFeatureFields.class);
	private String sequenceName;
	private Strand strand;
	private int start;
	private int end;
	private double value;
	private String name;
	private String commonName;
	private String geneType;

	// gff fields = (sequenceName, source, feature, start, end, score, strand, frame, attribute)

	public GffFeatureFields digest(String line) {
		String[] fields = line.split("\t");
		sequenceName = fields[0];
		setStrand(fields[6]);
		start = Integer.parseInt(fields[3]);
		end = Integer.parseInt(fields[4]);
		value = parseScore(fields[5]);
		// treat attributes as optional
		if (fields.length > 8) {
			Attributes attributes = parseAttributes(fields[8]);
			name = attributes.getString("ID");
			commonName = attributes.getString("Name");
		}
		geneType = fields[2];
		return this;
	}

	/**
	 * parse GFF attributes as key value pairs (see GFF3 spec)
	 */
	private Attributes parseAttributes(String string) {
		Attributes attributes = new Attributes();
		for (String kvp: string.split(";")) {
			String[] keyAndValue = kvp.split("=");
			if (keyAndValue.length == 2) {
				String key = urlDecode(keyAndValue[0]);
				String value = urlDecode(keyAndValue[1]);
				attributes.put(key, value);
			}
			else {
				// interpret just a value as an ID??
				String value = urlDecode(kvp);
				attributes.put("ID", value);
			}
		}
		return attributes;
	}

	private String urlDecode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			log.error(e);
			return s;
		}
	}

	private double parseScore(String score) {
		if (score==null || "".equals(score))
			return Double.NaN;
		if (".".equals(score))
			return Double.NaN;
		try {
			return Double.parseDouble(score);
		}
		catch (Exception e) {
			log.error(e);
			return Double.NaN;
		}
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public String getStrand() {
		return strand.toAbbreviatedString();
	}

	public void setStrand(Strand strand) {
		this.strand = strand;
	}
	
	public void setStrand(String strand) {
		this.strand = Strand.fromString(strand);
	}
	
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}

	// average of start and end.
	public int getPosition() {
		return MathUtils.average(start, end);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public String getGeneType() {
		return geneType;
	}

	public void setGeneType(String geneType) {
		this.geneType = geneType;
	}
}