package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.systemsbiology.util.Progress;
import org.systemsbiology.util.ProgressListener;


/**
 * Get basic info about a text file.
 * 
 * @author cbare
 */
public class TextFileScanner {
	private static final Logger log = Logger.getLogger(TextFileScanner.class);
	private Progress progressListenerSupport = new Progress();

	public TextFileInfo scanFile(String filename) throws Exception {
		log.info("Scanning file " + filename + ".");

		File file = new File(filename);
		if (!file.exists()) {
			throw new IOException("Can't find file \"" + filename + "\".");
		}
		if (!file.canRead()) {
			throw new IOException("Can't read file \"" + filename + "\".");
		}
		if (file.isDirectory()) {
			throw new IOException("File \"" + filename + "\" is a directory.");
		}

		// progress max = file length in kilobytes
		progressListenerSupport.fireInitEvent(110, "Scanning file " + file.getName());


		final TextFileInfo info = new TextFileInfo(file);

		progressListenerSupport.fireIncrementProgressEvent(10);

		// load preview data
		LineReader loader = new LineReader();
		loader.setLineLimit(100);
		loader.setLineHandler(new LineReader.LineProcessor() {
			boolean foundFirst;
			public void process(int lineNumber, String line) {
				// ignore nulls and comments
				if (line==null || line.matches("\\s*#.*"))
					return;

				if (foundFirst) {
					info.addPreviewLine(line);
				}
				else {
					info.setFirstLine(line);
					foundFirst = true;
				}

				progressListenerSupport.fireIncrementProgressEvent(1);
			}
		});
		loader.loadData(file);
		progressListenerSupport.fireDoneEvent();

		return info;
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}

	
}
