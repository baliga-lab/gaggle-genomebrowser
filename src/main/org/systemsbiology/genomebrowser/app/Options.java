package org.systemsbiology.genomebrowser.app;

import java.io.File;



/**
 * Optional settings and application scope data for the Genome Browser.
 * @author cbare
 */
public class Options {

	public String datasetUrl;
	public boolean overwrite;

	public int initialStart;
	public int initialEnd;
	public String initialChromosome;

	public String downloadGenome;

	public File workingDirectory;
	public File dataDirectory;
//	public String localCacheDirectory;

	public String version = "0.0";
	public String buildDate = "2007.10.03 12:43:04";
	public String buildNumber = "0";

	public boolean openBookmarks;

	public String[] plugins;

	// TODO if Goose is supposed to be a plugin, it's options don't seem to belong here.
	public boolean autostartBoss;
}
