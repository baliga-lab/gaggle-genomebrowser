/**
 * 
 */
package org.systemsbiology.genomebrowser.sqlite;

public interface FeatureProcessor {
	public void process(FeatureFields fields) throws Exception;
	public int getCount();
	public void cleanup();
}