package org.systemsbiology.genomebrowser;

import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.util.FileUtils;


/**
 * Command line options for the Genome Browser
 * 
 * @author cbare
 */
public class OptionsParser {
	Logger log = Logger.getLogger(OptionsParser.class);
	Options options;

	public OptionsParser() {
		options = new Options();
	}

	/**
	 * Read options from command line and internal defaults. May eventually be extended
	 * to read a config file.
	 * 
	 * @return a fully configured options instance
	 */
	public Options initializeOptions(String[] args) {
		parseCommandLineArgs(args);
		readBuildProperties();

		options.workingDirectory = getWorkingDirectory();
		log.info("working directory  = " + options.workingDirectory);

		if (options.dataDirectory==null)
			options.dataDirectory = new File(FileUtils.findUserDocomentsDirectory(), "hbgb");
		options.dataDirectory.mkdirs();
		log.info("data directory  = " + options.dataDirectory);

		options.plugins = new String[] {"org.systemsbiology.genomebrowser.gaggle.GenomeBrowserGoose"};

		return options;
	}


	/**
	 * @return the current working directory unless it's one of the
	 * roots of the filesystems (which it seems to be in the case of
	 * webstarts). In that case, return the user's home directory.
	 */
	public static File getWorkingDirectory() {
		try {
			File currentDir = (new File(".")).getCanonicalFile();
			File[] roots = File.listRoots();
			for (File root : roots) {
				if (root.equals(currentDir)) {
					currentDir = new File(System.getProperty("user.home"));
					break;
				}
			}
			return currentDir;
		}
		catch (Exception e) {
			return new File(System.getProperty("user.home"));
		}
	}

	public Options parseCommandLineArgs(String[] args) {
		try {
			for (int i=0; i < args.length; i++) {
				String arg = args[i];
				if ("-h".equals(arg) || "--help".equals(arg) || "-?".equals(arg) || "?".equals(arg)) {
					printUsageMessage();
					System.exit(0);
				}
				else if ("-d".equals(arg) || "--dataset".equals(arg)) {
					i++;
					options.datasetUrl = args[i];
				}
				else if ("-c".equals(arg) || "--coords".equals(arg) || "--coordinates".equals(arg)) {
					i++;
					Pattern p = Pattern.compile("(\\d+):(\\d+)");
					Matcher m = p.matcher(args[i]);
					if (m.matches()) {
						options.initialStart = Integer.parseInt(m.group(1));
						options.initialEnd = Integer.parseInt(m.group(2));
					}
				}
				else if ("-r".equals(arg) || "--replicon".equals(arg)) {
					i++;
					options.initialChromosome = args[i];
				}
				else if ("--download-genome".equals(arg)) {
					i++;
					options.downloadGenome = args[i];
				}
				// TODO --ncbi for backward compatibility, will be removed
				else if ("--ncbi".equals(arg)) {
					i++;
					options.downloadGenome = args[i];
				}
				else if ("--autostart-boss".equals(arg)) {
					options.autostartBoss = true;
				}
				else if (arg.startsWith("--data-dir")) {
					i++;
					String path = args[i];
					File file = new File(path);
					if (file.exists())
						options.dataDirectory = file;
				}
				else if ("--overwrite".equals(arg)) {
					options.overwrite = true;
				}
				else if ("--open-bookmarks".equals(arg)) {
					options.openBookmarks = true;
				}
				else {
					log.warn("Unrecognized command line option \"" + arg + "\".");
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("The command line option " + args[args.length-1] + " needs an argument.");
		}
		return options;
	}

	public void readBuildProperties() {
		Properties props = new Properties();
		try {
			props.load(OptionsParser.class.getResourceAsStream("/buildNumber.properties"));
			options.buildDate = props.getProperty("build.date");
			options.version   = props.getProperty("major.version") + "." + props.getProperty("minor.version");
			options.buildNumber = props.getProperty("build.number");
		}
		catch (Exception e) {
			log.warn("Couldn't find build.properties file in classpath.");
		}
	}

	public void printUsageMessage() {
		System.out.println();
		System.out.println("================");
		System.out.println(" Genome Browser");
		System.out.println("================");
		System.out.println();
		System.out.println("version: " + options.version);
		System.out.println("build data: " + options.buildDate);
		System.out.println();
		System.out.println("Command line options:");
		System.out.println("--------------------");
		System.out.println("  -d <url>, --dataset <url>         Load a dataset from the given URL or file system path.");
		System.out.println("                                    examples:");
		System.out.println("                                    - classpath:/HaloTilingArrayReferenceConditions/halo.dataset");
		System.out.println("                                    - file:/Users/cbare/Documents/data/halo.dataset");
		System.out.println("  --data-dir <path>                 Set the data directory used to save dataset files");
		System.out.println("  -h, -?, --help                    Print help message.");
		System.out.println("  --download-genome                 Download specified genome (by UCSC scientific name) on startup.");
		System.out.println("  --autostart-boss                  Automatically start Boss on startup.");
		System.out.println("  --open-bookmarks                  Open bookmarks stored in dataset when dataset is loaded.");
		System.out.println();
	}
}
