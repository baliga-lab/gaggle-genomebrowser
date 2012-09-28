package org.systemsbiology.ncbi;

import java.util.List;

import org.apache.log4j.Logger;
import org.systemsbiology.ncbi.commandline.NcbiOptions;

/**
 * Command line utility for NCBI genomes
 * @author cbare
 */
public class Ncbi {
	private static final Logger log = Logger.getLogger(Ncbi.class);

	public static void main(String[] args) throws Exception {
		log.info("Fetching genome information from NCBI...");
		
		NcbiApi api = new NcbiApi();

		NcbiOptions options = new NcbiOptions();
		if (options.parse(args)) {
			
			// projects
			if (options.command == NcbiOptions.Command.PROJECTS) {
				List<EUtilitiesGenomeProjectSummary> summaries = api.retrieveGenomeProjectSummaries(options.organism);
				for (EUtilitiesGenomeProjectSummary summary : summaries) {
					System.out.println(summary);
				}
			}

			else if (options.command == NcbiOptions.Command.SEQUENCES) {
				List<NcbiSequence> sequences = api.retrieveSequences(options.genomeProjectId);
				for (NcbiSequence sequence : sequences) {
					System.out.println(sequences);
				}
			}
		}
	}

}
