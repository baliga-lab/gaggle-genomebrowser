package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.util.FileUtils;


/**
 * Read a text file line-by-line. Each line is processed by an
 * implementation of LineProcessor.
 * @author cbare
 */
public class LineReader {
	private static final Logger log = Logger.getLogger(LineReader.class);
	private int progressInterval = 1000;
	private LineProcessor lineProcessor;
	private int lineLimit;
	private int lineCount;
	private long charCount;
	private long fileLength = 100000L;
	private int bytesPerCharacter = 1;
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();


	public LineReader() {
		lineProcessor = new DefaultLineProcessor();
	}

	public LineReader(LineProcessor lineProcessor) {
		this.lineProcessor = lineProcessor;
	}


	public void setLineLimit(int lineLimit) {
		this.lineLimit = lineLimit;
	}

	public void setLineHandler(LineProcessor lineHandler) {
		if (lineHandler==null)
			throw new NullPointerException("Linehandler can't be null.");
		this.lineProcessor = lineHandler;
	}

	/**
	 * set file length in bytes to help measure progress
	 */
	public void setFileLengthInBytes(long fileLength) {
		this.fileLength = fileLength;
	}

	/**
	 * If the file or stream is using a multibyte encoding, set
	 * bytes per character for more accurate progress measurement.
	 * Otherwise, we assume a default of 1 byte per character.
	 */
	public void setBytesPerCharacter(int bytesPerCharacter) {
		this.bytesPerCharacter = bytesPerCharacter;
	}

	/**
	 * Report progress every n lines.
	 */
	public void setProgressInterval(int n) {
		this.progressInterval = n;
	}

	public int getLineCount() {
		return lineCount;
	}

	public long getCharCount() {
		return charCount;
	}

	public void loadData(File file) throws Exception {
		loadData(new FileReader(file));
	}

	public void loadData(String filename) throws Exception {
		loadData(FileUtils.getReaderFor(filename));
	}

	public void loadData(Reader reader) throws Exception {
		BufferedReader r = null;

		try {
			r = new BufferedReader(reader);

			lineCount = 0;
			progressListenerSupport.fireProgressEvent(0, 100);
			String line = r.readLine();

			while (line != null) {

				// handle the current line of input
				lineProcessor.process(lineCount, line);

				lineCount++;
				charCount += line.length();

				if (lineCount % progressInterval == 0) {
					int percent = (int)( ((double)(charCount * bytesPerCharacter))/((double)fileLength) * 100.0 );
					progressListenerSupport.fireProgressEvent(percent);
				}

				// check if we have a line limit and if we're over
				if (lineLimit > 0 && lineCount >= lineLimit)
					break;

				line = r.readLine();
			}
			progressListenerSupport.fireDoneEvent();
		}
		finally {
			try {
				if (r != null) {
					r.close();
				}
			}
			catch (Exception e) {
				log.error(e);
			}
		}
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}

	public static interface LineProcessor {
		public void process(int lineNumber, String line) throws Exception;
	}

	public static class DefaultLineProcessor implements LineProcessor {
		public void process(int lineNumber, String line) {
			System.out.println(line);
		}
	}
}
