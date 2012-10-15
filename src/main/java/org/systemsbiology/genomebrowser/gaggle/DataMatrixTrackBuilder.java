package org.systemsbiology.genomebrowser.gaggle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.systemsbiology.gaggle.core.datatypes.DataMatrix;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.gaggle.ReceiveBroadcastDialog.Result;
import org.systemsbiology.genomebrowser.model.QuantitativeSegmentFeatureFields;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.genomebrowser.sqlite.MatrixFeatureFields;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.TrackImporter;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.LoggingProgressListener;
import org.systemsbiology.util.ProgressListener;

/**
 * Steps to add a track are:
 * 1.) Import raw table (in any order and with sequence names that may not
 *     exactly match those in the dataset. The raw table should be of the form
 *     (sequence_name, strand, start, end, value) or (sequence_name, strand,
 *     position, value). More forms might be added later?
 * 2.) Copy from temp table into final table, mapping sequence names to IDs
 *     and sorting features in the process.
 * 3.) Insert entry into tracks table (generating UUID)
 * 4.) Insert entry into datasets_tracks table
 * 5.) Insert attributes
 */
public class DataMatrixTrackBuilder {
	private static final Logger log = Logger.getLogger(DataMatrixTrackBuilder.class);
	ExternalAPI api;

	public void setApi(ExternalAPI api) {
		this.api = api;
	}

	public void handleMatrix(final String source, final DataMatrix matrix) {
		// if the matrix is one column, should we make a quantitative.segment track?

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ReceiveBroadcastDialog dialog = new ReceiveBroadcastDialog(api);
				dialog.setGaggleData(source, matrix);
				dialog.addDialogListener(new DialogListener() {
					public void cancel() {}

					public void error(String message, Exception e) {
						api.showErrorMessage(message, e);
					}

					public void ok(String action, Object result) {
						try {
							Result r = (Result)result;
							// TODO do this DB access off the Swing thread?
							CoordinateMap coordinateMap = api.loadCoordinateMap(r.coordinateMap);
							BuildTrackResults results = buildTrack(r.name, matrix, coordinateMap, r.isMatrix, source);
							log.info("imported matrix as track uuid " + results.uuid);
							if (results.misses.size() > 0) {
								log.info("Unable to map " + results.misses.size() + " features out of " + matrix.getRowCount() + ".");
								reportUnmappedFeatures(results.misses, results.count);
							}
						}
						catch (Exception e) {
							api.showErrorMessage("Error importing DataMatrix data", e);
						}
					}
				});
				dialog.setVisible(true);
			}
		});
	}

	private void reportUnmappedFeatures(final List<String> misses, final int rows) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UnmappedFeaturesDialog dialog = new UnmappedFeaturesDialog(api.getMainWindow(), misses, rows);
				dialog.setVisible(true);
			}
		});
	}

	public class BuildTrackResults {
		public final UUID uuid;
		public final int count;
		public final List<String> misses;
		public BuildTrackResults(UUID uuid, int count, List<String> misses) {
			this.uuid = uuid;
			this.count = count;
			this.misses = misses;
		}
	}

	public BuildTrackResults buildTrack(String name, DataMatrix matrix, CoordinateMap coordinateMap, boolean isMatrix, String source) {
		TrackImporter importer = api.getTrackImporter();

		// note: if matrix comes from R, species defaults to human, which is probably wrong

		Attributes attributes = new Attributes();
		attributes.put("source", "DataMatrix broadcast from " + source);
		attributes.put("created-on", String.format("%TF %<TT", new Date()));
		attributes.put("created-by", System.getProperty("user.name"));
		attributes.put("matrix.species", matrix.getSpecies());
		attributes.put("matrix.full.name", matrix.getFullName());
		attributes.put("top", 0.10);
		attributes.put("height", 0.15);

		ProgressListener lpl = new LoggingProgressListener(log);
		importer.addProgressListener(lpl);

		// coordinates might be either positional or segments.

		// a data matrix can be converted into a matrix track or individual
		// tracks for each column

		// for a 1-column matrix, make a regular track.
		// for more columns, make a matrix track, then make views if necessary

		UUID trackUuid = null;

		// features that coordinateMapper failed to map
		List<String> misses = null;

		if (matrix.getColumnCount()==1) {
			attributes.put("viewer", "Scaling");
			OneColumnMatrixFeatureSource featureSource = new OneColumnMatrixFeatureSource(matrix, coordinateMap);
			if (coordinateMap.isPositional()) {
				trackUuid = importer.importQuantitativePositionalTrack(name, api.getDatasetUuid(), featureSource);
			}
			else {
				trackUuid = importer.importQuantitativeSegmentTrack(name, api.getDatasetUuid(), featureSource);
			}
			misses = featureSource.misses;
		}
		else {
			// TODO handle positional matrix tracks?
			// TODO create views so individual conditions can be a track?
			DataMatrixFeatureSource featureSource = new DataMatrixFeatureSource(matrix, coordinateMap);
			trackUuid = importer.importQuantitativeSegmentMatrixTrack(name, api.getDatasetUuid(), featureSource, matrix.getColumnCount());
			misses = featureSource.misses;
			
			attributes.put("viewer", "MatrixHeatmap");

			if (featureSource.detectBothStrands()) {
				attributes.put("split.strands", "true");
			}

			attributes.put("overlap", featureSource.detectOverlap());
		}

		importer.storeAttributes(trackUuid, attributes);

		importer.removeProgressListener(lpl);

		api.getDataset().addTrack(importer.loadTrack(trackUuid));
		api.refresh();

		// medichi broadcasts tracks that only cover a limited range. The UI
		// should receive an event that the track has been added and do the
		// right thing - move the viewport to make the new data visible after
		// confirming with the user.
		api.publishEvent(new Event(this, "track-added", trackUuid));

		return new BuildTrackResults(trackUuid, matrix.getRowCount(), misses);
	}


	/**
	 * If we receive a broadcast of a matrix of one column, make a normal quantitative
	 * track out of it.
	 */
	class OneColumnMatrixFeatureSource implements FeatureSource {
		DataMatrix matrix;
		CoordinateMap coordinateMap;
		List<String> misses = new ArrayList<String>();

		public OneColumnMatrixFeatureSource(DataMatrix matrix, CoordinateMap coordinateMap) {
			this.matrix = matrix;
			this.coordinateMap = coordinateMap;
			if (coordinateMap==null) throw new NullPointerException("can't pass null coordinateMap to DataMatrixTrackBuilder");
		}

		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
			misses.clear();
			QuantitativeSegmentFeatureFields fields = new QuantitativeSegmentFeatureFields();
			String[] rowTitles = matrix.getRowTitles();
			for (int row=0; row<matrix.getRowCount(); row++) {
				double value = matrix.get()[row][0];
				Coordinates co = coordinateMap.getCoordinates(rowTitles[row]);
				if (co==null) {
					log.warn("Couldn't find coordinates. Skipping feature " + rowTitles[row]);
					misses.add(rowTitles[row]);
					continue;
				}
				fields.set(co.getSeqId(), co.getStrand().toAbbreviatedString(), co.getStart(), co.getEnd(), value);
				featureProcessor.process(fields);
			}
		}

		public void addProgressListener(ProgressListener progressListener) {
			
		}
		public void removeProgressListener(ProgressListener progressListener) {
			
		}
	}

	
	/**
	 * If we receive a broadcast of a matrix of one column, make a normal quantitative
	 * track out of it.
	 */
//	class OneColumnMatrixPositionalFeatureSource implements FeatureSource {
//		DataMatrix matrix;
//		CoordinateMap coordinateMap;
//		List<String> misses = new ArrayList<String>();
//
//		public OneColumnMatrixPositionalFeatureSource(DataMatrix matrix, CoordinateMap coordinateMap) {
//			this.matrix = matrix;
//			this.coordinateMap = coordinateMap;
//			if (coordinateMap==null) throw new NullPointerException("can't pass null coordinateMap to DataMatrixTrackBuilder");
//		}
//
//		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
//			misses.clear();
//			QuantitativePositionalFeatureFields fields = new QuantitativePositionalFeatureFields();
//			String[] rowTitles = matrix.getRowTitles();
//			for (int row=0; row<matrix.getRowCount(); row++) {
//				double value = matrix.get()[row][0];
//				Coordinates co = coordinateMap.getCoordinates(rowTitles[row]);
//				if (co==null) {
//					log.warn("Couldn't find coordinates. Skipping feature " + rowTitles[row]);
//					misses.add(rowTitles[row]);
//					continue;
//				}
//				fields.set(co.getSeqId(), co.getStrand().toAbbreviatedString(), co.getPosition(), value);
//				featureProcessor.process(fields);
//			}
//		}
//
//		public void addProgressListener(ProgressListener progressListener) {
//			
//		}
//		public void removeProgressListener(ProgressListener progressListener) {
//			
//		}
//	}

	/**
	 * Used to create matrix features from the a gaggle data matrix
	 */
	class DataMatrixFeatureSource implements FeatureSource {
		DataMatrix matrix;
		CoordinateMap coordinateMap;
		List<String> misses = new ArrayList<String>();

		public DataMatrixFeatureSource(DataMatrix matrix, CoordinateMap coordinateMap) {
			this.matrix = matrix;
			this.coordinateMap = coordinateMap;
			if (coordinateMap==null) throw new NullPointerException("can't pass null coordinateMap to DataMatrixTrackBuilder");
		}

		/**
		 * @return true if this matrix has features in both strands
		 */
		public boolean detectBothStrands() {
			boolean f = false;
			boolean r = false;

			String[] rowTitles = matrix.getRowTitles();
			for (int row=0; row<rowTitles.length; row++) {
				Coordinates co = coordinateMap.getCoordinates(rowTitles[row]);
				if (co==null) {
					continue;
				}
				if (co.getStrand()==Strand.forward)
					f = true;
				else if (co.getStrand()==Strand.reverse)
					r = true;
				if (f && r) return true;
			}
			return f && r;
		}

		/**
		 * Do ends of one probe overlap the starts of the next?
		 */
		public boolean detectOverlap() {
			List<Coordinates> coords = new ArrayList<Coordinates>();
			for (String rowTitle : matrix.getRowTitles()) {
				Coordinates c = coordinateMap.getCoordinates(rowTitle);
				if (c!=null) coords.add(c);
				if (coords.size() > 100) break;
			}
			Collections.sort(coords);
			int votes = 0;
			for (int i=0; i<(coords.size()-1); i++) {
				if (coords.get(i).getEnd() >= coords.get(i+1).getStart())
					votes++;
			}
			return (votes > (coords.size()/2)); 
		}

		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
			misses.clear();
			DataMatrixFeatureFields fields = new DataMatrixFeatureFields();
			String[] rowTitles = matrix.getRowTitles();
			for (int row=0; row<matrix.getRowCount(); row++) {
				double[] data = matrix.get(row);
				Coordinates co = coordinateMap.getCoordinates(rowTitles[row]);
				if (co==null) {
					log.warn("Couldn't find coordinates. Skipping feature " + rowTitles[row]);
					misses.add(rowTitles[row]);
					continue;
				}
				fields.set(co.getSeqId(), co.getStrand().toAbbreviatedString(), co.getStart(), co.getEnd(), data);
				featureProcessor.process(fields);
			}
		}

		/**
		 * Calling this after calling processFeatures returns a list of row titles
		 * that failed to map to coordinates.
		 */
		public List<String> getMisses() {
			return misses;
		}

		public void addProgressListener(ProgressListener progressListener) {
			// TODO implement progress on receiving matrix broadcasts
		}

		public void removeProgressListener(ProgressListener progressListener) {
			
		} 
	}

	/**
	 * Used to create matrix features from the a gaggle data matrix
	 */
//	class DataMatrixPositionalFeatureSource implements FeatureSource {
//		DataMatrix matrix;
//		CoordinateMap coordinateMap;
//		List<String> misses = new ArrayList<String>();
//
//		public DataMatrixPositionalFeatureSource(DataMatrix matrix, CoordinateMap coordinateMap) {
//			this.matrix = matrix;
//			this.coordinateMap = coordinateMap;
//			if (coordinateMap==null) throw new NullPointerException("can't pass null coordinateMap to DataMatrixTrackBuilder");
//		}
//
//		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
//			misses.clear();
//			DataMatrixFeatureFields fields = new DataMatrixFeatureFields();
//			String[] rowTitles = matrix.getRowTitles();
//			for (int row=0; row<matrix.getRowCount(); row++) {
//				double[] data = matrix.get(row);
//				Coordinates co = coordinateMap.getCoordinates(rowTitles[row]);
//				if (co==null) {
//					log.warn("Couldn't find coordinates. Skipping feature " + rowTitles[row]);
//					misses.add(rowTitles[row]);
//					continue;
//				}
//				fields.set(co.getSeqId(), co.getStrand().toAbbreviatedString(), co.getStart(), co.getEnd(), data);
//				featureProcessor.process(fields);
//			}
//		}
//
//		/**
//		 * Calling this after calling processFeatures returns a list of row titles
//		 * that failed to map to coordinates.
//		 */
//		public List<String> getMisses() {
//			return misses;
//		}
//
//		public void addProgressListener(ProgressListener progressListener) {
//			// TODO implement progress on receiving matrix broadcasts
//		}
//
//		public void removeProgressListener(ProgressListener progressListener) {
//			
//		} 
//	}

	class DataMatrixFeatureFields implements MatrixFeatureFields {
		double[] data;
		int start, end;
		String seq, strand;
		
		public void set(String seq, String strand, int start, int end, double[] data) {
			this.seq = seq;
			this.strand = strand;
			this.start = start;
			this.end = end;
			this.data = data;
		}

		public String getSequenceName() {
			return seq;
		}

		public String getStrand() {
			return strand;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public double[] getValues() {
			return data;
		}

		public int getPosition() {
			return start;
		}

		public String getName() {
			return "MatrixFeature";
		}

		public String getCommonName() {
			return null;
		}

		public String getGeneType() {
			return null;
		}

		public double getValue() {
			return data[0];
		}
	}
}
