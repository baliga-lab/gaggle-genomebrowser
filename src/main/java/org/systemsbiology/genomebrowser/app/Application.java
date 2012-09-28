package org.systemsbiology.genomebrowser.app;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkCatalog;
import org.systemsbiology.genomebrowser.gaggle.CoordinateMapSelection;
import org.systemsbiology.genomebrowser.impl.TextCoordinateMap;
import org.systemsbiology.genomebrowser.impl.TextPositionalCoordinateMap;
import org.systemsbiology.genomebrowser.io.Downloader;
import org.systemsbiology.genomebrowser.model.*;
import org.systemsbiology.genomebrowser.sqlite.TrackSaver;
import org.systemsbiology.genomebrowser.ucscgb.UcscDatasetBuilder;
import org.systemsbiology.genomebrowser.ui.ConfirmUseCachedFile;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackManager;
import org.systemsbiology.util.FileUtils;


// TODO out of memory error gets logged but not reported to user
// TODO lots of cleanup needed

/**
 * Application coordinates access to application-scope objects.
 * 
 * The goals are:
 *  -to treat the major pieces of functionality as components
 *  -mediate communication and concurrency through events
 *  -allow separately developed plug-in components
 *  -perform dependency injection using java code in place of XML configuration files.
 *   
 * These ideas are very much half-baked at this point -- cleanup needed.
 *
 * @author cbare
 */
public class Application implements EventListener {
	private static final Logger log = Logger.getLogger(Application.class);

	private EventSupport eventSupport = new EventSupport();
	private ApplicationEventQueue queue = new ApplicationEventQueue(eventSupport);
	private ExternalAPI api = new ExternalApiImpl(this);

	// ----- application state -------------------------------------------------
	private Dataset dataset = Dataset.EMPTY_DATASET;
	// TODO fix ownership of current viewing area
	// Selected sequence is part of the viewing area, along with start, end and
	// zoom level (scale). As a (possibly premature) performance tweak, that's
	// thread-confined to the UI thread. So, WTF is sequence doing here?
	// I need to either undo my performance tweak, or more likely come up
	// with a working (or even threadsafe) way of reflecting the viewing area
	// up to the application level.
	//private Sequence sequence = Sequence.NULL_SEQUENCE;
	// ------------------------------------------------------------------------

	// TODO make components private
	// TODO make options constant

	// all communication between components could be mediated through
	// the application's event queue.

	// ---- components --------------------------------------------------------
	// Note: after configuration (once startup has been called) these references
	// are to be treated as immutable.
	public final Options options;
	public SearchEngine search;
	public Selections selections;
	public BookmarkCatalog bookmarkCatalog;
	public TrackManager trackManager;
	public Io io;
	private UiController ui;
	private ProjectDefaults projectDefaults;

	//	Plugins
	private List<Plugin> plugins = new ArrayList<Plugin>();

	// ------------------------------------------------------------------------

	// plugins
	// add components to UI
	// respond to UI events
	// respond to app events: startup, shutdown, new dataset, etc.


	public Application(Options options) {
		this.options = options;
	}


	public void setUi(UiController ui) {
		this.ui = ui;
	}

	public UiController getUi() {
		return this.ui;
	}

	public ProjectDefaults getProjectDefaults() {
		if (projectDefaults==null) {
			projectDefaults = new ProjectDefaults(options);
		}
		return projectDefaults;
	}

	public void startup() {
		initPlugins();
		queue.start();
		publishEvent(new Event(this, "startup"));
	}

	private void initPlugins() {
		for (Plugin plugin: plugins) {
			plugin.init();
		}
	}

	public void shutdown(int exitCode) {

		// TODO do this w/ ApplicationListener?
		if (bookmarkCatalog.isDirty()) {
			if (!ui.confirm("You have unsaved bookmarks. Quit anyway?", "Confirm Quit"))
				return;
		}

		log.info("HeebieGB Genome Browser shutting down...");
		publishEvent(new Event(this, "shutdown"));

		// TODO wait for events to propagate during shutdown
		// cheesy hack:
		try { Thread.sleep(333); } catch (InterruptedException e) {}

		queue.shutdown();

		System.exit(0);
	}

	public void reportException(String message, Exception e) {
		ui.showErrorMessage(message, e);
	}

	public void showErrorMessage(String string) {
		ui.showErrorMessage(string);
	}


	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
		publishEvent(new Event(this, "set dataset", dataset));
//		selectInitialSequence();
	}

	// hack: need to unify new dataset code
	public void setDataset(Dataset dataset, File file) {
		setDataset(dataset);
		io.setDatasetFile(file);
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void loadDataset(final String path) {
		log.info("app.loadDataset(\"" + path + "\")");
		try {
			if ("file".equals(FileUtils.getUrlScheme(path))) {
				loadDataset(new File(path));
			}
			else {
				queue.enqueue(new Runnable() {
					public void run() {
						File file;
						try {
							file = download(new URL(path));
							_loadDataset(file);
						} catch (IOException e) {
							reportException("Problem downloading dataset: " + path, e);
						}
					}
				});
				
			}
		}
		catch (Exception e) {
			reportException("Error loading dataset: \"" + path + "\"", e);
		}
	}

	public void loadDataset(final File file) {
		try {
			queue.enqueue(new Runnable() {
				public void run() {
					_loadDataset(file);
				}
			});
		}
		catch (InterruptedException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	private void _loadDataset(File file) {
		log.info("loadDataset(\"" + file + "\")");
		try {
			// for files with relative paths, also look in working directory and data directory
			if (!file.exists() && !file.isAbsolute()) {
				log.info("Can't find file \"" + file + "\"");
				File guess = new File(options.workingDirectory, file.toString());
				log.info("trying file \"" + guess + "\"");
				if (!guess.exists()) {
					guess = new File(options.dataDirectory, file.toString());
					log.info("trying file \"" + guess + "\"");
				}
				if (guess.exists()) {
					file = guess;
				}
			}
			if (!file.exists()) {
				throw new RuntimeException("Can't find file \"" + file + "\"");
			}
			setDataset(io.loadDataset(file));
		}
		catch (Exception e) {
			reportException("Problem loading dataset: " + file.getName(), e);
		}
	}

	/**
	 * @return an implementation of SequenceFetcher
	 */
	public SequenceFetcher getSequenceFetcher() {
		// We may want to allow for different implementations of
		// SequenceFetcher, in particular a web based method through NCBI
		// or UCSC, which might not be associated with a particular dataset.
		return this.dataset.getSequenceFetcher();
	}

	public void updateTrack(Track<Feature> track) {
		TrackSaver ts = io.getTrackSaver();
		ts.updateTrack(track);
	}

	public void reloadDataset() {
		if (options.datasetUrl != null) {

			// TODO do this w/ ApplicationListener?
			if (bookmarkCatalog.isDirty()) {
				if (!ui.confirm("You have unsaved bookmarks. Continue anyway?", "Confirm Reload"))
					return;
			}
			
			log.info("reloading dataset: " + this.options.datasetUrl);
			this.loadDataset(options.datasetUrl);
		}
	}

	/**
	 * If there is a cached local copy of the dataset, use that. Otherwise,
	 * download the dataset from the given URL.
	 */
//	private File downloadIfNotCached(URL url) throws IOException {
//		File file = toCachedLocalFile(url);
//		if (file.exists()) {
//			// TODO download updated datasets
//			// check a hash of the remote file against the local file
//			// to detect updates to the remote file?
//			log.info("Using cached dataset file: " + file);
//			return file;
//		}
//		else
//			return download(url);
//	}

	/**
	 * Download a dataset from the given URL and store it in the
	 * user's hbgb data directory.
	 * @see org.systemsbiology.genomebrowser.app.Options
	 */
	private File download(URL url) throws IOException {
		log.info("downloading " + url);
		RunnableProgressReporter progressReporter = null;
		try {
			File file = FileUtils.urlToLocalFile(url, options.dataDirectory);

			if (file.exists()) {
				if (options.overwrite) {
					log.warn("--overwrite -> overwritting file: " + file);
				}
				else {
					String result = ConfirmUseCachedFile.confirmUseCachedFile(file.getPath());
					if (ConfirmUseCachedFile.NEW_FILE.equals(result)) {
						file = FileUtils.uniquify(file);
					}
					else if (ConfirmUseCachedFile.OVERWRITE.equals(result)) {
						log.warn("user chose to overwrite file: " + file);
					}
					else {
						log.info("using cached file: " + file);
						return file;
					}
				}
			}

			log.info("downloading " + url + " to " + file);

			Downloader downloader = new Downloader();
			progressReporter = new RunnableProgressReporter(downloader.getProgress());
			progressReporter.start();
			publishEvent(new Event(this, "download started", progressReporter));
			downloader.download(url, file);
			return file;
		}
		finally {
			if (progressReporter != null)
				progressReporter.done();
		}
	}

	// TODO should be unified with UI.showNewProjectDialog()

	/**
	 * Download genome for the given organism from UCSC or other source.
	 * For use when app starts w/ command line param --download-genome.
	 * If there already exists a hbgb file for the organism, we just use that.
	 * Otherwise try to download.
	 * @param genome
	 */
	public void downloadGenome(final String genome) {
		try {
			queue.enqueue(new Runnable() {
				public void run() {
					
					File file = getProjectDefaults().getDefaultFile(genome);

					if (file.exists()) {
						if (options.overwrite) {
							log.warn("--overwrite -> overwritting file: " + file);
						}
						else {
							String result = ConfirmUseCachedFile.confirmUseCachedFile(file.getPath());
							if (ConfirmUseCachedFile.NEW_FILE.equals(result)) {
								file = FileUtils.uniquify(file);
							}
							else if (ConfirmUseCachedFile.OVERWRITE.equals(result)) {
								log.warn("user chose to overwrite file: " + file);
							}
							else {
								log.info("using cached file: " + file);
								loadDataset(file);
								return;
							}
						}
					}

					ProjectDescription projectDescription = new ProjectDescription(genome);
					getProjectDefaults().apply(projectDescription);
					projectDescription.setFile(file);
					log.info("Creating new project: " + projectDescription);

					try {
						if ("UCSC".equals(projectDescription.getDataSource())) {
							UcscDatasetBuilder builder = new UcscDatasetBuilder();
							builder.drive(projectDescription, Application.this);
						}
						else if ("NCBI".equals(projectDescription.getDataSource())) {
							showErrorMessage("Import from NCBI not implemented yet");
						}
						else if ("local files".equals(projectDescription.getDataSource())) {
							showErrorMessage("Import from local files not implemented yet");
						}
						else {
							showErrorMessage("Unknown datasource " + projectDescription.getDataSource() + " Can't get here. This shouldn't happen.");
						}
					}
					catch (Exception e) {
						reportException("Error creating dataset", e);
					}
				}
			});
		}
		catch (InterruptedException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}
	

//	public void selectInitialSequence() {
//		Sequence seq;
//		if (dataset == null || dataset.getSequences().size() == 0)
//			seq = Sequence.NULL_SEQUENCE;
//		else {
//			String initialSeq = dataset.getAttributes().getString("initial.sequence");
//			if (initialSeq != null) {
//				seq = dataset.getSequence(initialSeq);
//			}
//			else
//				seq = dataset.getSequences().get(0);
//		}
//		selectSequence(seq);
//	}
//
//	public void selectSequence(String seqId) {
//		Sequence seq;
//		if (dataset == null)
//			seq = Sequence.NULL_SEQUENCE;
//		if (seqId==null || seqId.equals(sequence.getSeqId()))
//			return;
//		// may throw runtime exception if seqId doesn't exist
//		seq = dataset.getSequence(seqId);
//		selectSequence(seq);
//	}
//
//	public void selectSequence(Sequence sequence) {
//		this.sequence = sequence == null ? Sequence.NULL_SEQUENCE : sequence;
//		publishEvent(new Event(this, "sequence selected", this.sequence));
//	}
//
//	/**
//	 * @return the currently selected sequence or NULL_SEQUENCE if there is none.
//	 */
//	public Sequence getSelectedSequence() {
//		return sequence;
//	}

	// ---- Plugins -----------------------------------------------------------

	public void registerPlugin(Plugin plugin) {
		plugin.setExternalApi(api);
		plugins.add(plugin);
		log.info("registered plugin: " + plugin.getClass().getName());
	}

	public void registerPlugin(Class<? extends Plugin> pluginClass) {
		try {
			registerPlugin(pluginClass.newInstance());
		}
		catch (Exception e) {
			log.error("Failed to create instance of plugin " + pluginClass.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void registerPlugin(String pluginClassName) {
		try {
			registerPlugin((Class<? extends Plugin>)Class.forName(pluginClassName));
		}
		catch (Exception e) {
			log.error("Failed to create instance of plugin " + pluginClassName, e);
		}
	}

	// TODO this seems potentially unsafe
	public Plugin getPlugin(String type) {
		if (type==null) return null;
		for (Plugin plugin : plugins) {
			if (type.equals(plugin.getClass().getSimpleName()))
				return plugin;
		}
		return null;
	}

	// ---- Events ------------------------------------------------------------

	public void publishEvent(Event event) {
		try {
			queue.enqueue(event);
		}
		catch (InterruptedException e) {
			log.error("Event dropped due to interruption: " + event.getAction(), e);
		}
	}


	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}


	// TODO should App listen to events from components, rather than components calling publishEvent?

	/**
	 * receive and forward events from components
	 */
	public void receiveEvent(Event event) {
		publishEvent(event);
	}

	// ---- Coordinate Maps ---------------------------------------------------

	// TODO does findCoordinateMap belong in Dataset?
//	public CoordinateMap findCoordinateMap(String[] names) {
//		// test for names of the form <sequence><+/->:<start>-<end>
//		if (StrandedTextCoordinateMap.checkNames(names))
//			return new StrandedTextCoordinateMap();
//
//		// test for names of the form <sequence>:<start>-<end> where strand is
//		// indicated by which of start or end is greater
//		if (TextCoordinateMap.checkNames(names))
//			return new TextCoordinateMap();
//
//		// test for names matching the identifiers in the genes track
//		return io.findCoordinateMap(names);
//	}

	public List<CoordinateMapSelection> findCoordinateMaps(String[] names) {
		List<CoordinateMapSelection> maps = new ArrayList<CoordinateMapSelection>();

		// test for names of the form <sequence>[+/-]:<start>-<end> where strand is
		// indicated by which of start or end is greater
		maps.add(new CoordinateMapSelection("Identifiers encode coordinates", TextCoordinateMap.checkNames(names)));
		
		// do the same for names of the form <sequence>[+/-]:<position>
		maps.add(new CoordinateMapSelection("Identifiers encode positions", TextPositionalCoordinateMap.checkNames(names)));

		maps.addAll(io.findCoordinateMaps(names));

		return maps;
	}

	public CoordinateMap loadCoordinateMap(String name) {
		if ("Identifiers encode coordinates".equals(name)) {
			return new TextCoordinateMap();
		}
		if ("Identifiers encode positions".equals(name)) {
			return new TextPositionalCoordinateMap();
		}
		return io.loadCoordinateMap(name);
	}


	public boolean datasetIsLoaded() {
		return options.datasetUrl != null;
	}


	// TODO deleteTrack belongs on Dataset
	public void deleteTrack(UUID uuid) {
		io.deleteTrack(uuid);
		reloadDataset();
	}
	public void deleteTracks(List<UUID> uuids) {
		for (UUID uuid : uuids) {
			io.deleteTrack(uuid);
		}
		reloadDataset();
	}
}
