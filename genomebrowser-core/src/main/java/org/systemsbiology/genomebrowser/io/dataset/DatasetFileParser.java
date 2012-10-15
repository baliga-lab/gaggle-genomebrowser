package org.systemsbiology.genomebrowser.io.dataset;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.FileUtils;
import static org.systemsbiology.util.StringUtils.toStringSeparateLines;


/**
 * NOTE: This is a relic of an older version of the genome browser. The file
 * format has been changed to a Sqlite DB. This class remains to support
 * conversion of older data files to the new format.
 * 
 * An ad-hoc-alicious recursive descent parser for the dataset file format.
 * Reads a dataset file from the given stream and uses that
 * to build a >>incomplete<< Dataset object. It is incomplete in that the
 * tracks are not populated with features. That's left for later. See
 * DataLoaderStrategy.
 * 
 * Tokens are:
 * <ul>
 * <li><b>#</b> marks a comment until the line ends</li>
 * <li><b>:</b> separates key value pairs</li>
 * <li><b>{</b> opens a section</li>
 * <li><b>}</b> closes a section</li>
 * <li>strings which may contain any characters besides
 * ':', '{', '}', '\r' and '\n'</li>
 * <li><b>Chromosomes</b> marks the Chromosomes section</li>
 * <li><b>Tracks</b> marks a Tracks section</li>
 * </ul>
 * 
 * An example of the dataset format can be found in the resources directory.
 * 
 * Why make up my own file format with it's own kooky parser, you ask? The
 * dataset format aspires to be more user readable and writable than the
 * equivalent XML.
 * 
 * My apologies if anyone ever has to maintain this.
 * 
 * @author cbare
 */
public class DatasetFileParser {
	enum TokenType { colon, rbrace, lbrace, newline, cr };
	private static final Logger log = Logger.getLogger(DatasetFileParser.class);
	private StringBuilder buffer = new StringBuilder();
	private PushbackReader pbr;
	private int PUSH_BACK_BUFFER_SIZE = 4096;


	/**
	 * 
	 */
	public DatasetInfo parse(String path) throws IOException {
		return parse(FileUtils.getReaderFor(path));
	}

	/**
	 * Parse the stream from the given reader and build a
	 * DataSet object. Parsing errors will result in an
	 * IOException with a somewhat helpful message.
	 * 
	 * @throws IOException
	 */
	public DatasetInfo parse(Reader r) throws IOException {
		pbr = new PushbackReader(r, PUSH_BACK_BUFFER_SIZE);

		Map<String, Object> attrs = keyValuePairs();

		DatasetInfo ds = new DatasetInfo();
		ds.name = attrs.get("name").toString();
		ds.attrs.putAll(attrs);
		ds.chromosomes = chromosomes();

		return ds;
	}

	/**
	 * Parse the list of chromosomes in this dataset.
	 */
	private List<ChromosomeInfo> chromosomes() throws IOException {
		expect("Chromosomes");

		// beginning of chromosomes list
		expect(TokenType.lbrace);
		List<ChromosomeInfo> list = new ArrayList<ChromosomeInfo>();

		while (!peek(TokenType.rbrace)) {
			list.add(chromosome());
		}

		// end of chromosomes list
		expect(TokenType.rbrace);

		return list;
	}

	/**
	 * Parse a chromosome which can have several attributes and
	 * several associated data tracks.
	 */
	private ChromosomeInfo chromosome() throws IOException {
		String name = string();
		log.debug("chromosome: " + name);

		expect(TokenType.lbrace);
		Map<String, Object> attrs = keyValuePairs();

		// length is a required field
		int length;
		try {
        length = Integer.parseInt(removeCommas(attrs.get("length").toString()));
		}
		catch (NumberFormatException e) {
			throw new IOException("Parsing failed: length missing for chromosome: " + name + ", length = " + attrs.get("length"));
		}

		ChromosomeInfo c = new ChromosomeInfo();
		c.name = name;
		c.length = length;
		c.attrs.putAll(attrs);

		// expect Tracks keyword
		expect("Tracks");

		// beginning of tracks list
		expect(TokenType.lbrace);

		while (!peek(TokenType.rbrace)) {
			TrackInfo track = track();
			// add link up to chromosome
			track.attrs.put("chromosome", name);
			track.chromosome = c;
			c.tracks.add(track);
		}

		// end of tracks list
		expect(TokenType.rbrace);

		// end of chromosome
		expect(TokenType.rbrace);

		return c;
	}

	/**
	 * Parse a track, which can have several attributes.
	 */
	private TrackInfo track() throws IOException {
		String name = string();
		log.info("track: " + name);

		expect(TokenType.lbrace);
		Map<String, Object> attrs = keyValuePairs();
		expect(TokenType.rbrace);

		// create track
		TrackInfo track = new TrackInfo();
		track.name = name;
		track.attrs.putAll(attrs);

		return track;
	}

	/**
	 * Parse a series of key value pairs.
	 */
	private Map<String, Object> keyValuePairs() throws IOException {
		Map<String, Object> attrs = new HashMap<String, Object>();
		KeyValuePair p = keyValuePair();
		while (p != KeyValuePair.NULL) {
			attrs.put(p.key, p.value);
			p = keyValuePair();
		}
		return attrs;
	}

	/**
	 * Parse a key value pair.
	 * 
	 * Key value pairs are two strings separated by a colon. The strings
	 * can contain any characters except line-end characters (\r and \n) and
	 * colon, and left and right curly braces. Internal space is OK. 
	 */
	private KeyValuePair keyValuePair() throws IOException {
		KeyValuePair p = new KeyValuePair();
		
		// get the key
		p.key = string();
		if (p.key.length() == 0)
			return KeyValuePair.NULL;

		// get the colon
		if (!peek(TokenType.colon)) {
			unread(p.key);
			return KeyValuePair.NULL;
		}
		expect(TokenType.colon);

		// get the value
		p.value = string();
		if (p.value.length() == 0) {
			throw new IOException("Parsing failed: expected a value for key \"" + p.key + "\"");
		}
		
		log.debug("key value pair: " + p.key + " : " + p.value);
		return p;
	}

	/**
	 * Pull a string off the input stream. White space is treated a
	 * little strangely. We ignore any leading white space. Once we
	 * have at least one non-whitespace character, then spaces can be
	 * part of the string, but line-end characters (\r and \n)
	 * terminate the string as do :, {, and }. A comment character will
	 * effectively terminate the string also because it will cause
	 * everything up to the end of the line to be ignored.
	 */
	private String string() throws IOException {
		buffer.setLength(0);
		int c = read();
		while (c > -1) {
			if (buffer.length() == 0 && isWhiteSpace(c)) {
				// ignore white space at the beginning of a string
			}
			else if (c == ':'
			|| c == '{'
			|| c == '}'
			|| c == '\r'
			|| c == '\n') {
				// any of these characters terminate a string
				unread(c);
				break;
			}
			else {
				// add any other chars onto the string
				buffer.append((char)c);
			}
			c = read();
		}
		return buffer.toString().trim();
	}

	/**
	 * Consume a literal string token. For example "Chromosomes" or
	 * "Tracks".
	 */
	private void expect(String expected) throws IOException{
		String s = string();
		log.debug("keyword: " + s);

		if (!expected.equals(s))
			throw new IOException("Parsing failed: expected \"" + expected + "\" but found \"" + s + "\".");
	}

	/**
	 * Consume a single-character token of the given type. Open and close
	 * brackets are insensitive to surrounding whitespace.
	 */
	private void expect(TokenType tokenType) throws IOException {
		int c;
		switch (tokenType) {
		case colon:
			c = read();
			if (c != ':')
				throw new IOException("Parsing failed: expected ':' but found '" + (char)c + "'.");
			break;
		case lbrace:
			do {
				c = read();
			} while (isWhiteSpace(c));
			if (c != '{')
				throw new IOException("Parsing failed: expected '{' but found '" + (char)c + "'.");
			break;
		case rbrace:
			do {
				c = read();
			} while (isWhiteSpace(c));
			if (c != '}')
				throw new IOException("Parsing failed: expected '}' but found '" + (char)c + "'.");
			break;
		case newline:
			c = read();
			if (c != '\n')
				throw new IOException("Parsing failed: expected '\n' but found '" + (char)c + "'.");
			break;
		case cr:
			c = read();
			if (c != '\r')
				throw new IOException("Parsing failed: expected '\r' but found '" + (char)c + "'.");
			break;
		}
	}

	// made these non-private just to get rid of the damn warning
	
	/**
	 * Check on the input stream for an optional String. If the String is present,
	 * consume it and return true, otherwise consume nothing and return false.
	 */
	boolean optional(String string) throws IOException {
		String s = string();
		if (string.equals(s)) {
			return true;
		}
		else {
			pbr.unread(s.toCharArray());
			return false;
		}
	}

	/**
	 * Check on the input stream for an expected String without consuming anything.
	 */
	boolean peek(String string) throws IOException {
		String s = string();
		pbr.unread(s.toCharArray());
		return string.equals(s);
	}

	/**
	 * Check on the input stream for an expected token, but don't consume it.
	 * @return true if the specified token was found.
	 */
	private boolean peek(TokenType tokenType) throws IOException {
		int c;
		switch (tokenType) {
		case colon:
			c = read();
			unread(c);
			return c == ':';
		case lbrace:
			// absorb any whitespace before { token
			do {
				c = read();
			} while (isWhiteSpace(c));
			unread(c);
			return c == '{';
		case rbrace:
			// absorb any whitespace before } token
			do {
				c = read();
			} while (isWhiteSpace(c));
			unread(c);
			return c == '}';
		case newline:
			c = read();
			unread(c);
			return c == '\n';
		case cr:
			c = read();
			unread(c);
			return c == '\r';
		default:
			throw new IOException("Unexpected token type in peek: " + tokenType);
		}
	}

	// reader functions -----------------------------------------------

	/**
	 * Hide the reader behind this function so we can easily filter
	 * out comments.
	 */
	private int read() throws IOException {
		int c = pbr.read();
		
		// if we get a comment character, consume 'til the
		// end of the line
		if (c == '#') {
			do {
				c = pbr.read();
			} while (c != '\n');
		}
		
		return c;
	}

	private void unread(int c) throws IOException {
		pbr.unread(c);
	}

	private void unread(String s) throws IOException {
		pbr.unread(s.toCharArray());
	}

	// helper functions -----------------------------------------------

	private boolean isWhiteSpace(int c) {
		return c == '\r'
			|| c == '\n'
			|| c == ' '
			|| c == '\t';
	}

	/**
	 * If we get a string that looks like this "123,456,789" return "123456789"
	 * so it can be parsed into an integer.
	 */
	private String removeCommas(String string) {
		if (string==null) return null;
		return string.replaceAll(",","");
	}

	// ---- inner classes -----------------------------------------------------

	class AttributeFilter {
		private Map<String, String> attr;
		private String container = "";

		public AttributeFilter(Map<String, String> attr, String container) {
			this.attr = attr;
			this.container = container;
		}

		public void setContainer(String container) {
			this.container = container;
		}

		public String require(String key) {
			String value = attr.get(key);
			if (value == null)
				throw new RuntimeException("Missing required parameter \"" + key + "\" " + container);
			return value;
		}
	}

	public static class DatasetInfo {
		String name;
		Attributes attrs = new Attributes();
		List<ChromosomeInfo> chromosomes = new ArrayList<ChromosomeInfo>();

		public String getName() {
			return name;
		}
		public Attributes getAttributes() {
			return attrs;
		}
		public List<ChromosomeInfo> getChromosomes() {
			return chromosomes;
		}

		public String toString() {
			return String.format("DatasetInfo: name=%s,\n  attrs=%s,\n  chromosomes=%s", name, attrs.toString(), toStringSeparateLines(chromosomes));
		}
	}

	public static class ChromosomeInfo {
		String name;
		int length;
		Attributes attrs = new Attributes();
		List<TrackInfo> tracks = new ArrayList<TrackInfo>();

		public String getName() {
			return name;
		}
		public int getLength() {
			return length;
		}
		public Attributes getAttributes() {
			return attrs;
		}
		public List<TrackInfo> getTracks() {
			return tracks;
		}

		public String toString() {
			return String.format("Chromosome: name=%s, length=%d,\n    attrs=%s,\n    tracks=%s", name, length, attrs.toString(), toStringSeparateLines(tracks));
		}
	}

	public static class TrackInfo {
		String name;
		ChromosomeInfo chromosome;
		Attributes attrs = new Attributes();

		public String getName() {
			return name;
		}
		public ChromosomeInfo getChromosome() {
			return chromosome;
		}
		public Attributes getAttributes() {
			return attrs;
		}
		
		public String toString() {
			return String.format("Track: name=%s\n    attrs=%s", name, attrs.toString());
		}
	}
}

