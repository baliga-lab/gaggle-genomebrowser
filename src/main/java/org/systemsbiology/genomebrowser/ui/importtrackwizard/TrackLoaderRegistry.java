package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.genomebrowser.io.DataPointFeatureSource;
import org.systemsbiology.genomebrowser.io.GenomeFileFeatureSource;
import org.systemsbiology.genomebrowser.io.SegmentFeatureSource;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.genomebrowser.sqlite.GffLineProcessorAdapter;

public class TrackLoaderRegistry {
	private List<TrackLoaderDescription> loaders = new ArrayList<TrackLoaderDescription>();

	private void init() {
		loaders.add(new TrackLoaderDescription("GFF",
				"<html><p>Imports the GFF format described " +
				"<a href=\"http://www.sanger.ac.uk/Software/formats/GFF/GFF_Spec.shtml\">here</a>." +
				" GFF3 is described <a href=\"http://www.bioperl.org/wiki/GFF3\">here and " +
				"<a href=\"http://www.sequenceontology.org/gff3.shtml\">here</a>.</p></html>",
				new String[] {"seqid", "source", "type", "start", "end", "score", "strand", "phase", "attributes"}));
		loaders.add(new TrackLoaderDescription("Genome",
				"<html><p>Imports genome features such as coding regions and rnas. " +
				"Requires a tab-delimited text file with the columns: (sequence, strand, start, end, " +
				"name, common name, gene type).</p>" +
				"</html>",
				new String[] {"sequence", "strand", "start", "end", "name", "common name", "gene type"}
				));
		loaders.add(new TrackLoaderDescription("Data Segments",
				"<html><p>Imports quantitative features with start and end coordinates and a value from tab-" +
				"delimited text files with the columns (sequence, strand, start, end, value).</p>" +
				"</html>",
				new String[] {"sequence", "strand", "start", "end", "value"}
				));
		loaders.add(new TrackLoaderDescription("Data Points",
				"<html><p>Imports quantitative features with a position and a value from tab-" +
				"delimited text files with the columns (sequence, strand, position, value).</p>" +
				"</html>",
				new String[] {"sequence", "strand", "position", "value"}
				));
	}


	public FeatureSource getFeatureSource(String fileType, String filename, boolean hasColumnHeaders) {
		if ("GFF".equals(fileType)) {
			GffLineProcessorAdapter gff = new GffLineProcessorAdapter(filename);
			gff.setHasColumnHeaders(hasColumnHeaders);
			return gff;
		}
		else if ("Genome".equals(fileType)) {
			return new GenomeFileFeatureSource(filename, hasColumnHeaders);
		}
		else if ("Data Segments".equals(fileType)) {
			return new SegmentFeatureSource(filename, hasColumnHeaders);
		}
		else if ("Data Points".equals(fileType)) {
			return new DataPointFeatureSource(filename, hasColumnHeaders);
		}
		else
			throw new RuntimeException("Unknown file type: " + fileType);
	}

	public List<TrackLoaderDescription> getLoaders() {
		return loaders;
	}

	public static TrackLoaderRegistry newInstance() {
		TrackLoaderRegistry registry = new TrackLoaderRegistry();
		registry.init();
		return registry;
	}
}
