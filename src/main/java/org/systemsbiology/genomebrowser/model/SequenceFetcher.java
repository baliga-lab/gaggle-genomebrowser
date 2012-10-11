package org.systemsbiology.genomebrowser.model;

import java.io.File;
import org.systemsbiology.util.ProgressListener;

/**
 * An interface through which to retrieve sequence from a specific region of
 * the genome.
 */
public interface SequenceFetcher {

	// We might need the name of the organism for a web service based implementation. 

	/**
	 * Get sequence from the given region of the genome.
	 */
	public String getSequence(String sequenceName, Strand strand, int start, int end);
	public void readFastaFile(File file, Sequence sequence) throws Exception;

	// progress listener methods
	public void addProgressListener(ProgressListener listener);
	public void removeProgressListener(ProgressListener listener);
}
