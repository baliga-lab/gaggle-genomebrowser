package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.impl.BasicQuantitativeFeature;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * Imports files in the GFF format described here:
 * http://www.sanger.ac.uk/Software/formats/GFF/
 * 
 * Fields are: seqname source feature start end score strand frame attributes comments
 * 
 * @author cbare
 *
 */
public class GffLoader {
	private static final Logger log = Logger.getLogger(GffLoader.class);
	private static final int SEQ = 0;
	private static final int STRAND = 6;
	private static final int START = 3;
	private static final int END = 4;
	private static final int SCORE = 5;


	public GffLoader() {
	}


	public BlockingQueue<Feature.Quantitative> loadData(String filename) throws IOException {
		ReaderRunner runner = new ReaderRunner(filename);
		new Thread(runner).start();
		return runner.getQueue();
	}

	public BlockingQueue<Feature.Quantitative> loadData(File file) throws IOException {
		ReaderRunner runner = new ReaderRunner(file);
		new Thread(runner).start();
		return runner.getQueue();
	}

	public BlockingQueue<Feature.Quantitative> loadData(Reader reader) throws IOException {
		ReaderRunner runner = new ReaderRunner(reader);
		new Thread(runner).start();
		return runner.getQueue();
	}
	
	
	class GffLineProcessor implements LineReader.LineProcessor {
		BlockingQueue<Feature.Quantitative> queue;

		public GffLineProcessor(BlockingQueue<Feature.Quantitative> queue) {
			this.queue = queue;
		}

		public void process(int lineNumber, String line) {
			// ignore comment lines
			if (line.startsWith("#")) return;

			String[] fields = line.split("\t");

			try {
				queue.put(new BasicQuantitativeFeature(
						fields[SEQ],
						Strand.fromString(fields[STRAND]),
						Integer.parseInt(fields[START]),
						Integer.parseInt(fields[END]),
						parseScore(fields[SCORE])));
			}
			catch (NumberFormatException e) {
				log.error("NumberFormatException reading GFF file", e);
			}
			catch (InterruptedException e) {
				log.warn("InterruptedException reading GFF file", e);
			}
			catch (ArrayIndexOutOfBoundsException e) {
				throw new RuntimeException("Couldn't read line \"" + line + "\". Expected 7 tab-delimited fields, but found only " + fields.length);
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
				return Double.NaN;
			}
		}
	}

	class ReaderRunner implements Runnable {
		private final BlockingQueue<Feature.Quantitative> queue = new ArrayBlockingQueue<Feature.Quantitative>(1000);
		Reader reader;
		File file;
		String filename;

		public ReaderRunner(String filename) {
			this.filename = filename;
		}

		public ReaderRunner(File file) throws IOException {
			this.file = file;
		}

		public ReaderRunner(Reader reader) throws IOException {
			this.reader = reader;
		}

		public BlockingQueue<Feature.Quantitative> getQueue() {
			return queue;
		}

		public void run() {
			LineReader loader = new LineReader(new GffLineProcessor(queue));
			
			try {
				if (reader != null)
					loader.loadData(reader);
				else if (filename != null)
					loader.loadData(filename);
				else if (file != null)
					loader.loadData(file);
				// how does the consumer know I'm done reading?
				// need a sentinel value in the queue?
			}
			catch (Exception e) {
				// how do we coordinate error handling across threads?
				// do we need an error element in the queue, too?
			}
		}
	}
}
