package org.systemsbiology.genomebrowser.util;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.io.LineReader;
import org.systemsbiology.genomebrowser.io.StartEndValueDataLoader;
import org.systemsbiology.genomebrowser.io.dataset.DatasetFileParser;
import org.systemsbiology.genomebrowser.io.dataset.DatasetFileParser.ChromosomeInfo;
import org.systemsbiology.genomebrowser.io.dataset.DatasetFileParser.DatasetInfo;
import org.systemsbiology.genomebrowser.io.dataset.DatasetFileParser.TrackInfo;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.sqlite.FeatureFields;
import org.systemsbiology.genomebrowser.sqlite.FeatureProcessor;
import org.systemsbiology.genomebrowser.sqlite.FeatureSource;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource;
import org.systemsbiology.genomebrowser.sqlite.SqliteDatasetBuilder;
import org.systemsbiology.util.CaseInsensitiveKey;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.MultiHashMap;

import static org.systemsbiology.util.StringUtils.underline;
import static org.systemsbiology.util.StringUtils.line;


/**
 * Utility program to support converting old text format for datasets to the
 * spiffy new Sqlite DB based format. Written in only-has-to-run-once mode.
 * 
 * @author cbare
 */
public class TextDatasetConverter {
	private static final Logger log = Logger.getLogger(TextDatasetConverter.class);
	private static final Pattern forwardRegex = Pattern.compile("(?i).*((forward)|(fwd)).*");
	private static final Pattern reverseRegex = Pattern.compile("(?i).*((reverse)|(rev)).*");
	DatasetBuilder datasetBuilder;
	Options options;
	Map<String, String> trackTypes = new HashMap<String, String>();

	{
		trackTypes.put("StartEndToPointDataLoader","quantitative.segment");
		trackTypes.put("SegmentDataLoader","quantitative.segment");
		trackTypes.put("PositionDataLoader","quantitative.positional");
		trackTypes.put("GeneCoordinateLoader","gene");
		trackTypes.put("NcbiRnaCoordinateLoader","gene");
		trackTypes.put("MicroarrayDataLoader", "quantitative.segment");
		trackTypes.put("PointDataLoader", "quantitative.segment");
	}

	public static void main(String[] args) throws Exception {
		TextDatasetConverter converter = new TextDatasetConverter();

		// parse and set options
		Options options = converter.parse(args);
		converter.options = options;
		
		if (options.printTracks) {
			converter.printTrackNames(converter.readDataset(options.inputFileName));
		}
		else {
			converter.convert(options.inputFileName, options.outputFilename);
		}
	}
	
	private DatasetInfo readDataset(String inputFilename) throws Exception {
		DatasetFileParser parser = new DatasetFileParser();
		return parser.parse(inputFilename);
	}

	public void convert(String inputFilename, String outputFilename) throws Exception {
		DatasetInfo datasetInfo = readDataset(inputFilename);
		
		File parentDir = (new File(inputFilename)).getParentFile();
		
		File outFile = new File(outputFilename);
		if (outFile.exists() && options.overwrite) {
			outFile.delete();
			System.out.println("deleted file: " + outFile.getName());
		}

		// check for existing datasets in the output DB
		SqliteDataSource ds = new SqliteDataSource(outFile);
		if (ds.getDatasets().size() > 0 && !options.multipleDatasetsOk) {
			System.out.println("datasets already exist in file: " + outputFilename);
			System.out.println("use option --multiple-datasets-ok if this is OK.");
			System.out.println("exiting...");
			return;
		}
		
		datasetBuilder = new SqliteDatasetBuilder(new File(outputFilename));

		UUID dsuuid = datasetBuilder.beginNewDataset(datasetInfo.getName());
		for (String key: datasetInfo.getAttributes().keySet()) {
			Object value = datasetInfo.getAttributes().get(key);
			datasetBuilder.setAttribute(dsuuid, key, value);
		}

		// copy chromosomes -> sequences and bring along their attributes
		for (ChromosomeInfo c: datasetInfo.getChromosomes()) {
			UUID sequuid = datasetBuilder.addSequence(c.getName(), c.getLength(), options.topology);
			for (String key: c.getAttributes().keySet()) {
				Object value = c.getAttributes().get(key);
				datasetBuilder.setAttribute(sequuid, key, value);
			}
		}

		// In the old model, chromosomes have tracks associated with them. This was a
		// heinous mistake, which was corrected so that tracks now hold features for all
		// sequences. Here, we need to group equivalent tracks from different chromosomes
		// together to make one new track. A simple way to do it is to group all tracks
		// with the same name. We may need a more robust way of doing this later. In any
		// case, the result is a map from the name of a new track to a set of old tracks
		// whose features and attributes are imported into the new track.
		MultiHashMap<CaseInsensitiveKey, TrackInfo> tracks = new MultiHashMap<CaseInsensitiveKey, TrackInfo>(); 
		for (ChromosomeInfo c: datasetInfo.getChromosomes()) {
			for (TrackInfo t: c.getTracks()) {
				String name = t.getName().replaceAll("(?i)(forward|reverse)", "").replaceAll("  ", " ").trim();
				tracks.add(new CaseInsensitiveKey(name), t);
			}
		}

		// if there is a separate RNA track, merge that with the gene track
		List<CaseInsensitiveKey> geneTrackKeys = new ArrayList<CaseInsensitiveKey>();
		CaseInsensitiveKey geneTrackKey = null;
		CaseInsensitiveKey rnaTrackKey = null;
		for (CaseInsensitiveKey key: tracks.keySet()) {
			List<TrackInfo> trackInfos = tracks.get(key);
			TrackInfo t = trackInfos.get(0);
			String trackType = getTrackType(t.getAttributes().getString("reader"));
			if ("gene".equals(trackType))
				geneTrackKeys.add(key);
			String reader = t.getAttributes().getString("reader");
			if ("GeneCoordinateLoader".equals(reader))
				geneTrackKey = key;
			else if ("NcbiRnaCoordinateLoader".equals(reader))
				rnaTrackKey = key;
		}
		if (geneTrackKeys.size()==2 && geneTrackKey!=null && rnaTrackKey!=null) {
			List<TrackInfo> rnaTracks = tracks.remove(rnaTrackKey);
			tracks.addAll(geneTrackKey, rnaTracks);
		}
		else if (geneTrackKeys.size()>1) {
			log.warn("Couldn't figure out how to merge gene tracks: " + geneTrackKeys.toString());
		}

		// for each group of related tracks, add a new track to the new dataset
		for (CaseInsensitiveKey name: tracks.keySet()) {
			List<TrackInfo> trackInfos = tracks.get(name);
			TrackInfo t = trackInfos.get(0);
			String trackType = getTrackType(t.getAttributes().getString("reader"));

			if ("unknown".equals(trackType)) {
				System.out.println("Skipping track " + t.getName() + " reader=" + t.getAttributes().getString("reader"));
				continue;
			}

			UUID tuuid = datasetBuilder.addTrack(trackType, name.toString(), new ReaderFeatureSource(trackInfos, parentDir));
			for (String key: t.getAttributes().keySet()) {
				if ("filename".equals(key) || "chromosome".equals(key) || "reader".equals(key)) continue;
				Object value = t.getAttributes().get(key);
				datasetBuilder.setAttribute(tuuid, key, value);
			}

			// update renderer
			if ("gene".equals(trackType)) {
				datasetBuilder.setAttribute(tuuid, "viewer", "Gene");
			}
		}

		log.info("Import complete");
	}

	class ReaderFeatureSource implements FeatureSource {
		private List<TrackInfo> trackInfos;
		private File parentDir;

		public ReaderFeatureSource(List<TrackInfo> trackInfos, File parentDir) {
			this.trackInfos = trackInfos;
			this.parentDir = parentDir;
		}

		public void processFeatures(final FeatureProcessor featureProcessor) throws Exception {
			for (TrackInfo trackInfo: trackInfos) {
				String filename = trackInfo.getAttributes().getString("filename");
				log.info("importing filename=" + filename);

				if ("StartEndToPointDataLoader".equals(trackInfo.getAttributes().get("reader"))
				|| "SegmentDataLoader".equals(trackInfo.getAttributes().get("reader"))) {

					final QuantitativeSegmentFeatureFields feature = new QuantitativeSegmentFeatureFields();
					feature.trackInfo = trackInfo;
					feature.strand = detectStrand(trackInfo);

					LineReader reader = new LineReader(new LineReader.LineProcessor() {
						public void process(int lineNumber, String line) throws Exception {
							if (lineNumber == 0) return;
							feature.fields = line.split("\t");
							featureProcessor.process(feature);
						}
					});
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					reader.loadData(file);
				}

				else if ("SegmentDataLoader".equals(trackInfo.getAttributes().get("reader"))) {

					final QuantitativePositionFeatureFields feature = new QuantitativePositionFeatureFields();
					feature.trackInfo = trackInfo;
					feature.strand = detectStrand(trackInfo);

					LineReader reader = new LineReader(new LineReader.LineProcessor() {
						public void process(int lineNumber, String line) throws Exception {
							if (lineNumber == 0) return;
							feature.fields = line.split("\t");
							featureProcessor.process(feature);
						}
					});
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					reader.loadData(file);
				}

				else if ("PositionDataLoader".equals(trackInfo.getAttributes().get("reader"))) {

					final QuantitativePositionFeatureFields feature = new QuantitativePositionFeatureFields();
					feature.trackInfo = trackInfo;
					feature.strand = detectStrand(trackInfo);

					LineReader reader = new LineReader(new LineReader.LineProcessor() {
						public void process(int lineNumber, String line) throws Exception {
							if (lineNumber == 0) return;
							feature.fields = line.split("\t");
							featureProcessor.process(feature);
						}
					});
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					reader.loadData(file);
				}
				
				else if ("GeneCoordinateLoader".equals(trackInfo.getAttributes().get("reader"))) {
					final GeneFeatureFields feature = new GeneFeatureFields();
					feature.trackInfo = trackInfo;

					LineReader reader = new LineReader(new LineReader.LineProcessor() {
						public void process(int lineNumber, String line) throws Exception {
							if (lineNumber == 0) return;
							feature.fields = line.split("\t");
							featureProcessor.process(feature);
						}
					});
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					reader.loadData(file);
				}

				else if ("NcbiRnaCoordinateLoader".equals(trackInfo.getAttributes().get("reader"))) {
					final NcbiRnaCoordinateLoader feature = new NcbiRnaCoordinateLoader();
					feature.trackInfo = trackInfo;

					LineReader reader = new LineReader(new LineReader.LineProcessor() {
						public void process(int lineNumber, String line) throws Exception {
							if (lineNumber < 2) return;
							feature.fields = line.split("\t");
							featureProcessor.process(feature);
						}
					});
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					reader.loadData(file);
				}

				else if ("MicroarrayDataLoader".equals(trackInfo.getAttributes().get("reader"))) {
					// Gene_id	oligo_sequence	start	end	strand	log_ratio	p.value
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					StartEndValueDataLoader loader = new StartEndValueDataLoader(file);
					loader.setupForMicroarrayDataFile();
					loader.setSkipFirst(true);
					loader.setSequence(trackInfo.getChromosome().getName());
					loader.setStrand(detectStrand(trackInfo).toAbbreviatedString());
					loader.processFeatures(featureProcessor);
				}

				else if ("PointDataLoader".equals(trackInfo.getAttributes().get("reader"))) {
					File file = new File(new File(parentDir, trackInfo.getChromosome().getName()), filename);
					StartEndValueDataLoader loader = new StartEndValueDataLoader(file);
					loader.setSkipFirst(true);
					loader.setSequence(trackInfo.getChromosome().getName());
					loader.setStrand(detectStrand(trackInfo).toAbbreviatedString());
					loader.processFeatures(featureProcessor);
				}
				
				else {
					System.out.println("Don't know what to do with track with reader: " + trackInfo.getAttributes().get("reader"));
				}
			}
		}

		private Strand detectStrand(TrackInfo trackInfo) {
			if (containsForward(trackInfo.getChromosome().getName()))
				return Strand.forward;
			if (containsReverse(trackInfo.getChromosome().getName()))
				return Strand.reverse;
			if (containsForward(trackInfo.getName()))
				return Strand.forward;
			if (containsReverse(trackInfo.getName()))
				return Strand.reverse;
			if (containsForward(trackInfo.getAttributes().getString("filename")))
				return Strand.forward;
			if (containsReverse(trackInfo.getAttributes().getString("filename")))
				return Strand.reverse;
			return Strand.none;
		}
		
		private boolean containsForward(String s) {
			Matcher m = forwardRegex.matcher(s);
			return m.matches();
		}
		
		private boolean containsReverse(String s) {
			Matcher m = reverseRegex.matcher(s);
			return m.matches();
		}

		class QuantitativeSegmentFeatureFields implements FeatureFields {
			TrackInfo trackInfo;
			Strand strand;
			String[] fields;

			public String getSequenceName() {
				return trackInfo.getChromosome().getName();
			}

			public String getStrand() {
				return strand.toAbbreviatedString();
			}

			public int getStart() {
				return Integer.parseInt(fields[0]);
			}

			public int getEnd() {
				return Integer.parseInt(fields[1]);
			}

			public int getPosition() {
				return MathUtils.average(getStart(), getEnd());
			}

			public String getGeneType() {
				return null;
			}

			public String getName() {
				return null;
			}

			public String getCommonName() {
				return null;
			}

			public double getValue() {
				return Double.parseDouble(fields[2]);
			}
		};

		class QuantitativePositionFeatureFields implements FeatureFields {
			TrackInfo trackInfo;
			Strand strand;
			String[] fields;

			public String getSequenceName() {
				return trackInfo.getChromosome().getName();
			}

			public String getStrand() {
				return strand.toAbbreviatedString();
			}

			public int getStart() {
				return Integer.parseInt(fields[0]);
			}

			public int getEnd() {
				return Integer.parseInt(fields[0]);
			}

			public int getPosition() {
				return Integer.parseInt(fields[0]);
			}

			public String getGeneType() {
				return null;
			}

			public String getName() {
				return null;
			}

			public String getCommonName() {
				return null;
			}

			public double getValue() {
				return Double.parseDouble(fields[1]);
			}
		};

		class GeneFeatureFields implements FeatureFields {
			TrackInfo trackInfo;
			String[] fields;
			
			// canonical_Name	Gene_Name	Start	Stop	Orientation

			public String getSequenceName() {
				return trackInfo.getChromosome().getName();
			}

			public String getStrand() {
				return Strand.fromString(fields[4]).toAbbreviatedString();
			}

			public int getStart() {
				return Integer.parseInt(fields[2]);
			}

			public int getEnd() {
				return Integer.parseInt(fields[3]);
			}

			public int getPosition() {
				return MathUtils.average(getStart(), getEnd());
			}

			public String getGeneType() {
				return "cds";
			}

			public String getName() {
				return fields[0];
			}

			public String getCommonName() {
				return fields[1];
			}

			public double getValue() {
				return 0.0;
			}
		};

		class NcbiRnaCoordinateLoader implements FeatureFields {
			TrackInfo trackInfo;
			String[] fields;
			
			// Product-Name	Start	End	Strand	Length	GeneID	Locus	Locus_tag

			public String getSequenceName() {
				return trackInfo.getChromosome().getName();
			}

			public String getStrand() {
				return Strand.fromString(fields[3]).toAbbreviatedString();
			}

			public int getStart() {
				return Integer.parseInt(fields[1]);
			}

			public int getEnd() {
				return Integer.parseInt(fields[2]);
			}

			public int getPosition() {
				return MathUtils.average(getStart(), getEnd());
			}

			public String getGeneType() {
				String name = getName().toLowerCase();
				if (name.contains("trna"))
					return "trna";
				if (name.contains("rrna") || name.contains("ribosomal"))
					return "rrna";
				return "rna";
			}

			public String getName() {
				return fields[0];
			}

			public String getCommonName() {
				return fields[6];
			}

			public double getValue() {
				return 0.0;
			}
		};

		
		public void addProgressListener(ProgressListener progressListener) {}
		public void removeProgressListener(ProgressListener progressListener) {}
	}


	public void printTrackNames(DatasetInfo datasetInfo) {
		for (ChromosomeInfo c: datasetInfo.getChromosomes()) {
			System.out.println(line(80));
			System.out.println(underline(c.getName()));
			for (TrackInfo t: c.getTracks()) {
				System.out.println(t.getName());
			}
		}
	}

	
	private String getTrackType(String reader) {
		String type = trackTypes.get(reader);
		if (type==null) return "unknown";
		return type;
	}

	public Options parse(String[] args) {
		Options options = new Options();
		
		for (int i=0; i<args.length; i++) {

			if ("-i".equals(args[i]) || "--input".equals(args[i])) {
				i++;
				options.inputFileName = args[i];
			}
			else if ("-o".equals(args[i]) || "--output".equals(args[i])) {
				i++;
				options.outputFilename = args[i];
			}
			else if ("-t".equals(args[i]) || "--topology".equals(args[i])) {
				i++;
				options.topology = Topology.fromString(args[i]);
			}
			else if ("--multiple-datasets-ok".equals(args[i])) {
				options.multipleDatasetsOk = true;
			}
			else if ("--print-track-names".equals(args[i])) {
				options.printTracks = true;
			}
			else if ("--overwrite".equals(args[i])) {
				options.overwrite = true;
			}
			else if ("".equals(args[i])) {
				
			}
			else if ("".equals(args[i])) {
				
			}

		}
		
		if (options.inputFileName==null || (options.outputFilename==null && !options.printTracks)) {
			usage();
			System.exit(-1);
		}

		return options;
	}

	public void usage() {
		System.out.println("\n=====================\n Text Dataset Format\n=====================\n");
		System.out.println("A tool for converting old text format datasets into Sqlite DBs.");
		System.out.println();
		System.out.println("usage: TextDatasetFormat -i <input-file> -o <output-file>");
		System.out.println("-i, --input             input file name");
		System.out.println("-o, --output            output file name");
		System.out.println("-t, --topology          set topology (linear or circular)");
		System.out.println("--print-track-names     print track names from input dataset and exit");
		System.out.println("--multipleDatasetsOk    OK to add multiple datasets to a Sqlite DB");
		System.out.println("--overwrite             if output file already exists, overwrite it");
		System.out.println("\n");		
	}

	class Options {
		String inputFileName;
		String outputFilename;
		Topology topology = Topology.linear;
		boolean printTracks;
		boolean multipleDatasetsOk;
		boolean overwrite;
	}
}

