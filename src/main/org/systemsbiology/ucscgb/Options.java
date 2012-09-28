package org.systemsbiology.ucscgb;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;


/**
 * Parse command line arguments and set program options.
 */
public class Options {
	public boolean removeAlternateHaplotypes;
	public String category;
	public String dbName;
	public String organism;
	public String cmd;


	public String usage() {
		return  "--------\n" +
				" ucscgb\n" +
				"--------\n" +
				"A utility to fetch genome information from the UCSC " +
				"genome browser. If the last argument is not a switch or an argument " +
				"to a switch, it is interpreted as a dbName.\n\n" +
				"Options:\n" +
				"-l, --list [category];  list organisms and database names in the given category\n" +
				"--genome;               display data about the genome (requires db or organism)\n" +
				"-c, --chromInfo;        display chromosome info for the specified organism (requires db or organism)\n" +
				"-g, --genes;            fetch a table of gene locations (requires db or organism)\n" +
				"-r;                     remove alternate haplotypes and _random sequences\n" +
				"-d, --db [dbName];      specify organism database name (see --list)\n" +
				"-o, --organism [org];   specify organism (see --list)\n" +
				"\nCategories:\n" +
				"  (archaea, bacteria, prokaryotes, eukaryotes, deuterostome, insect, mammal, vertebrate, worm, yeast)\n" +
				"\nExamples:\n" +
				"ucscgb --list archaea\n" +
				"ucscgb --chromInfo metMar1\n" +
				"ucscgb -r --chromInfo danRer5\n" +
				"ucscgb --genes metMar1\n" +
				"ucscgb --genes -d danRer5 -r\n" +
				"ucscgb -g -o \"Methanococcus maripaludis S2\" -r" +
				"\n\n";
	}

	public boolean validate() {
		if (cmd == null) return false;
		if (cmd.equals("chromInfo") || cmd.equals("genes") || cmd.equals("genome")) {
			if (isNullOrEmpty(dbName) && isNullOrEmpty(organism)) return false;
		}
		return true;
	}

	public void parseArgs(String[] args) {
		int i=0;
		while (i < args.length) {
			if ("-r".equals(args[i])) {
				removeAlternateHaplotypes = true;
			}
			else if ("-l".equals(args[i]) || "--list".equals(args[i])) {
				cmd = "list";
				i++;
				if (i<args.length)
					category = args[i];
			}
			else if ("-g".equals(args[i]) || "--genes".equals(args[i])) {
				cmd = "genes";
			}
			else if ("--genome".equals(args[i])) {
				cmd = "genome";
			}
			else if ("-d".equals(args[i]) || "--db".equals(args[i])) {
				i++;
				if (i<args.length)
					dbName = args[i];
			}
			else if ("-o".equals(args[i]) || "--organism".equals(args[i])) {
				i++;
				if (i<args.length)
					organism = args[i];
			}
			else if ("-c".equals(args[i]) || "--chromInfo".equals(args[i])) {
				cmd = "chromInfo";
			}
			else if ("?".equals(args[i]) || "help".equals(args[i]) || "-?".equals(args[i]) || "-h".equals(args[i]) || "--help".equals(args[i])) {
				cmd = "help";
			}
			else if (args[i].startsWith("-")) {
				System.out.println("unknown option " + args[i]);
			}
			else {
				dbName = args[i];
				break;
			}
			i++;
		}
	}
}
