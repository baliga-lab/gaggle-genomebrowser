package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.model.BasicDataset;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.genomebrowser.sqlite.SqliteTrackImporter;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRendererRegistry;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.ProgressListener;

/**
 * TrackImportWizard collects the data and resources needed to import a
 * track. It invokes the import and also handles cancel.
 * 
 * The import track wizard proceeds in four steps:
 * 1. selecting a file (or URL)
 * 2. previewing + selecting a loader and track type
 * 3. importing features
 * 4. setting track attributes - name, color, renderer, and overlay (more?)
 * There is an implementation of WizardPanel for each step.
 * This class, ImportTrackWizard, is responsible for the logic of the
 * import process. MainWindow and the implementations of WizardPanel
 * constitute the UI.
 * 
 * Inputs to the wizard:
 * Options
 * Dataset UUID
 * Dataset URL (for database connection - we shouldn't need this!)
 * An implementation of TrackImporter
 * A set of loaders
 * A list of track types
 * Maybe a mapping of loaders to track types?
 * Mapping of track types to lists of renderers
 */
public class ImportTrackWizard {
	private static final Logger log = Logger.getLogger(ImportTrackWizard.class);
	private static final TrackLoaderRegistry trackLoaderRegistry = TrackLoaderRegistry.newInstance();
	private Options options;
	private boolean scanned;
	private boolean loaded;
	private boolean loading;
	private String filename;
	private int featureCount;
	private String trackName;
	private String loader;
	private String trackType;
	private boolean hasColumnHeaders;
	private TrackImporter trackImporter;
	private Attributes attributes = new Attributes();
	private Dataset dataset;
	private UUID trackUuid;
	private List<String> trackTypes;
	private List<TrackLoaderDescription> trackLoaderDescriptions;
	private ExceptionReporter exceptionReporter;
	private Map<String, List<String>> trackTypeToRenderersMap;


	public ImportTrackWizard() {
		log.info("import wizard starting...");
	}

	// dependency
	public void setExceptionReporter(ExceptionReporter exceptionReporter) {
		this.exceptionReporter = exceptionReporter;
	}

	// dependency
	public void setTrackTypeToRenderersMap(Map<String, List<String>> trackTypeToRenderersMap) {
		this.trackTypeToRenderersMap = trackTypeToRenderersMap;
	}

	// dependency
	public void setTrackTypes(List<String> trackTypes) {
		this.trackTypes = trackTypes;
	}

	// dependency
	public void setTrackReaderInfos(List<TrackLoaderDescription> trackLoaderDescriptions) {
		this.trackLoaderDescriptions = trackLoaderDescriptions;
	}

	// dependency
	public void setOptions(Options options) {
		this.options = options;
	}

	/**
	 * TrackImporter must be set before track is imported (importTrack is called).
	 */
	public void setTrackImporter(TrackImporter trackImporter) {
		this.trackImporter = trackImporter;
	}

	public TrackImporter getTrackImporter() {
		return trackImporter;
	}

	public String getLoader() {
		return loader;
	}

	public void setLoader(String loader) {
		this.loader = loader;
	}

	public Options getOptions() {
		return options;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public int getFeatureCount() {
		return featureCount;
	}

	public void setFeatureCount(int features) {
		this.featureCount = features;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public boolean hasColumnHeaders() {
		return hasColumnHeaders;
	}

	public void setHasColumnHeaders(boolean hasColumnHeaders) {
		this.hasColumnHeaders = hasColumnHeaders;
	}

	public void deriveTrackNameFromFilename() {
		File file = new File(getFilename());
		setTrackName(file.getName());
	}

	public void addTrackAttribute(String key, Object value) {
		attributes.put(key, value);
	}

	public void removeTrackAttribute(String key) {
		attributes.remove(key);
	}

	public void saveAttributes() {
		trackImporter.storeAttributes(trackUuid, attributes);
	}

	public void setTrackType(String trackType) {
		this.trackType = trackType;
	}

	public String getTrackType() {
		return trackType;
	}

	public List<String> getTrackTypes() {
		return trackTypes;
	}

	public List<TrackLoaderDescription> getTrackReaderInfos() {
		return trackLoaderDescriptions;
	}

	// TODO where are track types defined? where is mapping defined?

	/**
	 * Look up a list of renderers that are applicable to the given
	 * track type.
	 */
	public List<String> getRenderersForTrackType(String trackType) {
		List<String> renderers = trackTypeToRenderersMap.get(trackType);
		if (renderers == null)
			return Collections.emptyList();
		else
			return renderers;
	}

	public boolean isScanned() {
		return scanned;
	}

	public void setScanned(boolean scanned) {
		this.scanned = scanned;
	}

	public synchronized boolean isLoaded() {
		return loaded;
	}

	public synchronized boolean isLoadedOrLoading() {
		return loaded || loading;
	}

	// what is the purpose of synchronization
	// why delete track?
	public synchronized void setLoaded(boolean loaded) {
		if (loaded==false) {
			deleteTrackIfLoaded();
		}
		this.loaded = loaded;
	}

	private synchronized boolean checkIfAlreadyLoading() {
		if (loading) return true;
		loaded = false;
		loading = true;
		return false;
	}

	private synchronized void setLoadedFlag(boolean loaded) {
		loading = false;
		this.loaded = loaded;
	}

	public void cancel() {
		// TODO handle cancel in importTrack thread
		log.debug("import track wizard canceled.");
		deleteTrackIfLoaded();
	}

	private void deleteTrackIfLoaded() {
		if (isLoaded()) {
			trackImporter.deleteTrack(trackUuid);
		}
	}

	public String toString() {
		return "WizardState [ filename=" + StringUtils.quote(filename) + ", scanned=" + scanned
			+ ", loaded=" + isLoaded() + ", features=" + featureCount
			+ "]";
	}

	public void setTrackUuid(UUID uuid) {
		this.trackUuid = uuid;
	}
	
	private void setDefaultAttributes(UUID trackUuid) {
		// if track type is Gene the place it in the middle of the viewport
		if ("Gene".equals(trackType)) {
			attributes.put("top", 0.45);
			attributes.put("height", 0.10);
		}
		else {
			attributes.put("top", 0.10);
			attributes.put("height", 0.15);
		}
	}

	/**
	 * Check for all the required information. If any is missing, throw a
	 * RuntimeException. Otherwise, import a track.
	 */
	public void importTrackIfReady(ProgressListener progressListener) {
		if (StringUtils.isNullOrEmpty(getFilename()))
			throw new RuntimeException("Can't import yet, we need a filename");
		if (StringUtils.isNullOrEmpty(getTrackName()))
			throw new RuntimeException("Can't import yet, we need a track name");
		if (dataset == null)
			throw new RuntimeException("Can't import yet, we need a dataset");
		if (trackImporter == null)
			throw new RuntimeException("Can't import yet, missing a trackImporter");
		if (loader == null)
			throw new RuntimeException("Can't import yet, missing a loader type");
		if (trackType == null)
			throw new RuntimeException("Can't import yet, missing a trackType");
		importTrack(progressListener);
	}

	// TODO pass configured progress listener to featureSource
	// we need to allocate a portion of the total progress for loading
	// features and previewloader needs to provide progress events for
	// that portion.
	public void importTrack(final ProgressListener progressListener) {
		if (checkIfAlreadyLoading()) return;
		Runnable runnable = new Runnable() {
			// TODO handle cancel in importTrack thread
			public void run() {
				try {
					trackImporter.addProgressListener(progressListener);
					FeatureSource featureSource = trackLoaderRegistry.getFeatureSource(getLoader(), getFilename(), hasColumnHeaders);
					UUID trackUuid = null;
					if ("quantitative.segment".equals(trackType))
						trackUuid = trackImporter.importQuantitativeSegmentTrack(getTrackName(), dataset.getUuid(), featureSource);
					else if ("quantitative.positional".equals(trackType))
						trackUuid = trackImporter.importQuantitativePositionalTrack(getTrackName(), dataset.getUuid(), featureSource);
					else if ("gene".equals(trackType))
						trackUuid = trackImporter.importGeneTrack(getTrackName(), dataset.getUuid(), featureSource);
					else
						throw new RuntimeException("Unsupported track type: " + trackType);
					trackImporter.removeProgressListener(progressListener);
					setTrackUuid(trackUuid);
					setDefaultAttributes(trackUuid);
					saveAttributes();
					dataset.addTrack(trackImporter.loadTrack(trackUuid));
					setLoadedFlag(true);
					exceptionReporter.updateStatus();
				}
				catch (Exception e) {
					setLoadedFlag(false);
					exceptionReporter.reportException("Error loading track:", e);
				}
			}
		};
		new Thread(runnable).start();
	}



	// ---- main --------------------------------------------------------------

	// TODO make ImportTrackWizard useful as a utility program

	/**
	 * main method for testing
	 */
	public static void main(String[] args) {
		Options options = new Options();
		options.workingDirectory = new File(System.getProperty("user.home"));
		options.datasetUrl = "test.hbgb";

		TrackRendererRegistry trackRendererRegistry = TrackRendererRegistry.newInstance();

		// TODO unify new dataset code?
		BasicDataset dataset = new BasicDataset();
		dataset.setName("Test Dataset");
		dataset.setUuid(UUID.fromString("21676c27-782f-469d-972b-a0204ee295c9"));

		final ImportTrackWizard wiz = new ImportTrackWizard();
		wiz.setOptions(options);
		wiz.setTrackImporter(new SqliteTrackImporter(SqliteTrackImporter.getConnectStringForFile(options.datasetUrl)));
		wiz.setDataset(dataset);
		wiz.setTrackReaderInfos(trackLoaderRegistry.getLoaders());
		wiz.setTrackTypes(trackRendererRegistry.getTrackTypes());
		wiz.setTrackTypeToRenderersMap(trackRendererRegistry.getTrackTypeTpRenderersMap());

		WizardMainWindow mainWindow = new WizardMainWindow(null, wiz);
		wiz.setExceptionReporter(mainWindow);

		mainWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if ("cancel".equals(event.getActionCommand())) {
					log.info("track import canceled");
					log.info(wiz.toString());
					System.exit(0);
				}
				else if ("ok".equals(event.getActionCommand())) {
					log.info("track import done");
					log.info(wiz.toString());
					System.exit(0);
				}
			}
		});
	}
}
