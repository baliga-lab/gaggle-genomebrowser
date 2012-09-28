package org.systemsbiology.genomebrowser.io;

import java.io.File;

import org.junit.Test;
import org.systemsbiology.genomebrowser.io.LineReader.LineProcessor;
import org.systemsbiology.util.DebuggingProgressListener;


/**
 * Test the LineReader's ability to report progress. Depends on the
 * existence of a file at the relative path data/sample.large.gff.
 */
public class TestLineReader {

	// TODO make TestLineReader a real unit test

	@Test
	public void testLineCount() throws Exception {
		File file = new File("data/sample.large.gff");
		LineProcessor lineProcessor = new EmptyLineProcessor();
		LineReader reader = new LineReader(lineProcessor);
		reader.setFileLengthInBytes(file.length());
		reader.setProgressInterval(10000);
		reader.addProgressListener(new DebuggingProgressListener());
		reader.loadData(file);
	}
}


class EmptyLineProcessor implements LineProcessor {
	public void process(int lineNumber, String line) throws Exception {
	}
}

class MyLineProcessor implements LineProcessor {
	long len;
	long count;

	public MyLineProcessor(long len) {
		this.len = len;
	}

	public void process(int lineNumber, String line) throws Exception {
		count += line.length();
		double cpl = count / ((double)lineNumber);
		double percent = count/((double)len) * 100;
		System.out.format("%,12d. chars=%,12d  total=%,12d chars/line=%4.1f  percent=%3.0f\n", lineNumber, count, len, cpl, percent);
	}
}