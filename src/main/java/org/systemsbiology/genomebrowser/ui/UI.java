package org.systemsbiology.genomebrowser.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.io.CoordinateMapFileIterator;
import org.systemsbiology.genomebrowser.io.GenomeFileFeatureSource;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ncbi.NcbiGenomeToDataset;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.app.ProjectDescription;
import org.systemsbiology.genomebrowser.app.ProjectDescription.SequenceDescription;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.sqlite.SqliteDatasetBuilder;
import org.systemsbiology.genomebrowser.sqlite.TrackSaver;
import org.systemsbiology.genomebrowser.transcript.TranscriptBoundaryPlugin;
import org.systemsbiology.genomebrowser.ucscgb.ImportUcscGenome;
import org.systemsbiology.genomebrowser.ucscgb.UcscDatasetBuilder;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.ImportTrackWizard;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.TrackLoaderRegistry;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.WizardMainWindow;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.genomebrowser.util.InvertionUtils;
import org.systemsbiology.genomebrowser.visualization.TrackRendererScheduler;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.tracks.FeatureCounter;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRendererRegistry;
import org.systemsbiology.ncbi.NcbiGenome;
import org.systemsbiology.ncbi.ui.NcbiQueryDialog;
import org.systemsbiology.ucscgb.Category;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.Hyperlink;
import org.systemsbiology.util.ProgressReporter;
import org.systemsbiology.util.swing.Dialogs;
import org.systemsbiology.util.swing.SwingThreadProgressListenerWrapper;
import static org.systemsbiology.genomebrowser.app.ProjectDescription.LOCAL_DATA_LABEL;
import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

// TODO click on genes for more info
// TODO links to UCSC/NCBI/Blast/etc.

/**
 * A facade layer for the UI.
 * 
 * The UI should not be accessed from threads other than the swing event thread.
 * To call UI functionality from other threads, use ExternalUiController.
 * 
 * @author cbare
 */
public class UI {
	private static final Logger log = Logger.getLogger(UI.class);
	static final int MOVE_SMALL = -1;
	static final int MOVE_BIG = -2;
	Application app;
	MainWindow mainWindow;

	/**
	 * maintains the coordinates of the visible region of the current sequence
	 */
	private final ViewParameters viewParameters;

	/**
	 * References to actions are needed when UI components (including temporary
	 * popups) are created and when Actions must be enables and disabled.
	 * Plugins may need access, too?
	 */
	Actions actions = new Actions(this);

	/**
	 * When the app is starting up, we may want to do other things in other
	 * threads while the UI is getting set up. This latch provides a means for
	 * other threads to wait until the UI is finished.
	 */
	private CountDownLatch guiCreatedLatch = new CountDownLatch(1);

	// set in MainWindow.init ??
	public TrackRendererScheduler scheduler;

	// This static initializer makes sure the OSX specific tweaks
	// to the system properties are made early enough to count. It
	// needs to happen before we touch any Swing classes.
	static {
		if (MacOsx.isOSX())
			MacOsx.setSystemProperties();
		else {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				log.warn("Error setting Swing PLaF", e);
			}
		}
	}

	/**
	 * Constructs the UI using an externally supplied viewParameters object or
	 * creating a new instance of ViewParameters if the parameter is null. See
	 * docs for the ViewParameters object.
	 */
	private UI(Application application, ViewParameters viewParameters)
			throws Exception {
		this.app = application;
		this.viewParameters = viewParameters == null ? new ViewParameters()
				: viewParameters;
	}

	public static UI newInstance(Application application) throws Exception {
		return newInstance(application, null);
	}

	public static UI newInstance(Application application,
			ViewParameters viewParameters) throws Exception {
		final UI ui = new UI(application, viewParameters);

		// create the UI components on the Swing event thread. If we use
		// invokeAndWait here (rather than invokeLater), we prevent the new
		// instance from being accessed before the GUI is finished being
		// created.

		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				ui.createUi();
			}
		});

		return ui;
	}

	private void createUi() {
		try {
			// make OSX specific UI tweaks, if we're on the apple JVM
			if (MacOsx.isOSX())
				MacOsx.createApplicationListener(this);

			mainWindow = new MainWindow(this);
			mainWindow.init();

			// center on screen
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			mainWindow.setLocation((d.width - mainWindow.getWidth()) / 2,
					(d.height - mainWindow.getHeight()) / 2);

			mainWindow.setVisible(true);

			log.info("done creating gui");
		} finally {
			// signal that we're finished creating the GUI
			guiCreatedLatch.countDown();
		}
	}

	// ---- ui ----------------------------------------------------------------

	public void addToolbar(String title, JToolBar toolbar, Action action) {
		mainWindow.addToolbar(title, toolbar, action);
	}

	public void setVisibleToolbar(String title, boolean visible) {
		mainWindow.setVisibleToolbar(title, visible);
	}

	public void insertMenu(JMenu menu) {
		mainWindow.insertMenu(menu);
	}

	public void insertMenu(String title, Action[] actions) {
		mainWindow.insertMenu(title, actions);
	}

	Actions getActions() {
		return actions;
	}

	// for about dialog
	Options getOptions() {
		return app.options;
	}

	// called by GenomeViewPanel and SideBar
	ViewParameters getViewParameters() {
		return viewParameters;
	}

	Segment getVisibleSegment() {
		return viewParameters.getVisibleSegment();
	}

	void refresh() {
		app.trackManager.refresh();
		repaint();
	}

	void repaint() {
		scheduler.schedule(app.trackManager, viewParameters.getSequence(),
				viewParameters.getStart(), viewParameters.getEnd());
	}

	void repopulateVisualPropertiesMenu(Point popupCoordinates) {
		List<Track<? extends Feature>> tracks = app.trackManager
				.getTracksAt(popupCoordinates);
		 mainWindow.getRightClickMenu().setTracks(tracks);
	}

	void repopulateLinksMenu(List<Hyperlink> links) {
		 mainWindow.getRightClickMenu().setLinks(links);
	}

	// ---- navigation and selection ------------------------------------------

	public void moveRight(int x) {
		if (x == MOVE_SMALL)
			viewParameters.moveRight((int) (viewParameters.getWidth() * 0.10));
		else if (x == MOVE_BIG)
			viewParameters.moveRight((int) (viewParameters.getWidth() * 0.66));
		else
			viewParameters.moveRight(x);
	}

	public void moveLeft(int x) {
		if (x == MOVE_SMALL)
			viewParameters.moveLeft((int) (viewParameters.getWidth() * 0.10));
		else if (x == MOVE_BIG)
			viewParameters.moveLeft((int) (viewParameters.getWidth() * 0.66));
		else
			viewParameters.moveLeft(x);
	}

	public void centerOnPosition(int position) {
		viewParameters.centerOnPosition(position);
	}

	public int setWidthInBasePairs(int width) {
		log.info("setWidthInBasePairs()");
		return viewParameters.setWidthInBasePairs(width);
	}

	// SEQ
	public void centerOnSegment(Segment segment) {
		if (segment != null) {
			// set sequence, and center on start and end
			try {
				setSelectedSequence(segment.seqId, false);
				centerOnPosition(segment.center());
			} catch (Exception e) {
				showErrorMessage("Error navigating to " + segment, e);
			}
		}
	}

	// SEQ
	public void centerOnSelection() {
		centerOnSegment(app.selections.getEnclosingSegment(viewParameters.getSequence().getSeqId()));
	}

	// SEQ
	public void setViewSegment(Segment segment) {
		log.info("setViewSegment()");
		setSelectedSequence(segment.seqId, false);
		viewParameters.setRange(segment.start, segment.end);
	}

	// SEL
	public void gotoAndSelectFeature(Feature feature) {
		if (feature == null)
			return;
		log.debug("goto and select feature: " + feature);
		Segment segment = new Segment(feature.getSeqId(), feature.getStart(),
				feature.getEnd());
		centerOnSegment(segment);

		// select the segment
		app.selections.replaceSelection(segment, false);
		app.selections.selectFeature(feature, false);

		// if bookmarks have associated named features, look them up and select
		// them too
		if (feature instanceof Bookmark) {
			String[] names = ((Bookmark) feature).getAssociatedFeatureNames();
			if (names != null) {
				for (String name : names) {
					app.selections.selectFeature(app.search.findByName(name), false);
				}
			}
		}
	}

	// SEL
	@SuppressWarnings("unchecked")
	public void gotoAndSelectFeatures(List<? extends Feature> features) {
		log.debug("goto and select features");
		if (features.size() > 0) {
			Feature f = features.get(0);
			log.debug("goto and select feature: " + f);
			// log.debug("position = " + f.getCentralPosition());
			Segment segment = new Segment(f.getSeqId(), f.getStart(), f
					.getEnd());
			centerOnSegment(segment);
			app.selections.clear(false);
			// selectFeatures takes an Iterable<Feature>
			app.selections.selectFeatures((List<Feature>) features, false);

			// if bookmarks have associated named features, look them up and
			// select them too
			for (Feature feature : features) {
				if (feature instanceof Bookmark) {
					// log.debug("bookmarks w/ associated features:");
					String[] names = ((Bookmark) feature)
							.getAssociatedFeatureNames();
					if (names != null) {
						for (String name : names) {
							app.selections.selectFeature(app.search.findByName(name), false);
						}
					}
				}
			}
		}
	}

	// SEL
	public void deselect() {
		app.selections.clear(false);
		// TODO repaint shouldn't be needed here
		mainWindow.genomeView.repaint();
	}

	public Sequence getSelectedSequence() {
		return viewParameters.getSequence();
	}

	public String getSelectedSequenceName() {
		return viewParameters.getSequence().getSeqId();
	}

	public void previousSequence() {
		int i;
		List<Sequence> sequences = app.getDataset().getSequences();
		if (sequences.size() == 0)
			return;
		Sequence selected = viewParameters.getSequence();
		for (i = 0; i < sequences.size(); i++) {
			if (sequences.get(i).equals(selected)) {
				i--;
				break;
			}
		}
		if (i <= -1)
			i = sequences.size() - 1;
		setSelectedSequence(sequences.get(i), true);
	}

	public void nextSequence() {
		int i;
		List<Sequence> sequences = app.getDataset().getSequences();
		if (sequences.size() == 0)
			return;
		Sequence selected = viewParameters.getSequence();
		for (i = 0; i < sequences.size(); i++) {
			if (sequences.get(i).equals(selected)) {
				i++;
				break;
			}
		}
		if (i >= sequences.size())
			i = 0;
		setSelectedSequence(sequences.get(i), true);
	}

	/**
	 * Respond to "sequence selected" event. Notify the UI that a new sequence
	 * has been selected
	 */
	// public void sequenceSelected(Sequence seq) {
	// }

	public void setSelectedSequence(String sequenceName, boolean restore) {
		if (sequenceName != null)
			setSelectedSequence(app.getDataset().getSequence(sequenceName),
					restore);
	}

	public void setSelectedSequence(Sequence seq, boolean restore) {
		storeSequenceViewArea();
		mainWindow.status.setSequence(seq);
		mainWindow.sideBar.setSelectedSequence(seq);

		if (restore) {
			// if start and end have been saved for this sequence, apply them
			if (seq != null
					&& seq.getAttributes().containsKey("_display.start")) {
				int start = seq.getAttributes().getInt("_display.start");
				int end = seq.getAttributes().getInt("_display.end");
				viewParameters.initViewParams(seq, start, end);
			} else {
				viewParameters.initViewParams(seq);
			}
		}
	}

	/**
	 * Store previous view area in sequence settings.
	 */
	private void storeSequenceViewArea() {
		Sequence seq = viewParameters.getSequence();
		if (seq != null) {
			seq.getAttributes()
					.put("_display.start", viewParameters.getStart());
			seq.getAttributes().put("_display.end", viewParameters.getEnd());
		}
	}

	public void zoomToSelection() {
		String seqId = viewParameters.getSequence().getSeqId();
		Segment segment = app.selections.getEnclosingSegment(seqId);
		if (segment != null)
			setViewSegment(segment);
	}

	public void zoomOutAll() {
		for (Sequence seq : app.getDataset().getSequences()) {
			seq.getAttributes().put("_display.start", 0);
			seq.getAttributes().put("_display.end", seq.getLength());
		}
		Sequence seq = viewParameters.getSequence();
		setViewSegment(new Segment(seq.getSeqId(), 0, seq.getLength()));
	}

	public void zoomIn() {
		viewParameters.zoomIn();
	}

	public void zoomOut() {
		viewParameters.zoomOut();
	}

	// ---- search ------------------------------------------------------------

	public void showFindDialog() {
		find(JOptionPane.showInputDialog(mainWindow, "Search terms:", "Find",
				JOptionPane.QUESTION_MESSAGE));
	}

	public void find(String searchString) {
		if (searchString == null)
			return;
		log.info("find: " + searchString);
		find(searchString.split("\\s*,\\s*|\\s*;\\s*|\\s+"));
	}

	public void find(String[] keywords) {
		if (keywords == null)
			return;
		int resultCount = app.search.search(keywords);
		if (resultCount > 0) {
			actions.findNextAction.setEnabled(resultCount > 1);
			gotoAndSelectFeatures(app.search.getResults());
		} else {
			showStatusMessage("No matches found for " + keywords.length
					+ " search terms", "Nothing found");
			actions.findNextAction.setEnabled(false);
		}
	}

	public void findNext() {
		gotoAndSelectFeature(app.search.getNext());
	}

	// ---- bookmarks ---------------------------------------------------------

	public void bookmarkCurrentSelection(boolean direct) {
		if (viewParameters.getSequence() == null || viewParameters.getSequence() == Sequence.NULL_SEQUENCE)
			return;
		// get current selection and create a bookmark
		Segment segment = app.selections.getSingleSelection();
		Bookmark newBookmark = (segment == null) ? 
			new Bookmark(
					viewParameters.getSequence().getSeqId(),
					viewParameters.getStart(),
					viewParameters.getEnd())
		:
			new Bookmark(
					segment.seqId,
					app.selections.getStrandHint(),
					segment.start,
					segment.end);
		newBookmark.setAssociatedFeatureNames(FeatureUtils.extractNames(app.selections.getSelectedFeatures()));
		
		
		String seq = "--not available--";
		if (app.getSequenceFetcher()!=null) { 
			seq = app.getSequenceFetcher().getSequence(newBookmark.getSeqId(), newBookmark.getStrand(), newBookmark.getStart(), newBookmark.getEnd());  	//getSql(bookmark.getStart(), bookmark.getEnd(), String.valueOf(bookmark.getSeqId()), String.valueOf(bookmark.getStrand())); //getting SQL if possible
		}
		//String strand = newBookmark.getStrand().toString();
		if (newBookmark.getStrand().toString().equals("reverse")){
			newBookmark.setSequence(InvertionUtils.inversion(seq));
		}else {
			newBookmark.setSequence(seq);
		}
		
		if (direct) {
			BookmarkDataSource ds = app.bookmarkCatalog.getSelected();
			ds.add(newBookmark);
		}
		else {
			showBookmarksDialog(newBookmark);
		}
	}

	public boolean toggleBookmarksPanel() {
		return mainWindow.bookmarksPanel.toggleBookmarksPanel();
	}

	public void closeBookmarksPanel() {
		mainWindow.bookmarksPanel.closeBookmarksPanel();
	}

	public void openBookmarksPanel() {
		mainWindow.bookmarksPanel.openBookmarksPanel();
	}

	public void showBookmarksDialog(Bookmark bookmark) {
		BookmarkDataSource ds = app.bookmarkCatalog.getSelected();
		
		// TODO fix hacky transcript boundary bookmarks hook
		if (TranscriptBoundaryPlugin.TRANSCRIPT_TYPE.equals(ds.getAttributes().getString("type"))) {
			TranscriptBoundaryPlugin transcriptBoundaryPlugin = (TranscriptBoundaryPlugin)app.getPlugin("TranscriptBoundaryPlugin");
			if (transcriptBoundaryPlugin!=null) {
				transcriptBoundaryPlugin.edit(ds, bookmark);
				return;
			}
		}

		BookmarkDialog bmw = new BookmarkDialog(mainWindow, ds, bookmark);
		bmw.setVisible(true);
	}

	public void newBookmarkSet() {
		mainWindow.bookmarksPanel.newSet();
	}

	public void loadBookmarks() {
		BookmarkFileDialogs dialogs = new BookmarkFileDialogs();
		dialogs.setBookmarkCatalog(app.bookmarkCatalog);
		dialogs.setOptions(app.options);
		dialogs.setParentComponent(mainWindow);
		try {
			dialogs.loadBookmarkFile();
		} catch (IOException e) {
			showErrorMessage("Error loading bookmarks", e);
		}
	}

	public void exportBookmarks() {
		BookmarkFileDialogs dialogs = new BookmarkFileDialogs();
		dialogs.setBookmarkCatalog(app.bookmarkCatalog);
		dialogs.setOptions(app.options);
		dialogs.setParentComponent(mainWindow);
		try {
			dialogs.exportBookmarksToFile();
		} catch (IOException e) {
			showErrorMessage("Error saving bookmarks", e);
		}
	}

	// ---- datasets ----------------------------------------------------------

	public void showLoadDatasetFromUrlDialog() {
		// unsaved bookmarks?
		if (app.bookmarkCatalog.isDirty()) {
			if (!showConfirmDialog("You have unsaved bookmarks. Continue anyway?", "Confirm?"))
				return;
		}
		String path = JOptionPane.showInputDialog(mainWindow, "URL:",
				"Enter URL to dataset", JOptionPane.QUESTION_MESSAGE);
		if (path == null || "".equals(path))
			return;
		loadDataset(path);
	}

	public void showLoadLocalDatasetDialog() {
		// unsaved bookmarks?
		if (app.bookmarkCatalog.isDirty()) {
			if (!showConfirmDialog("You have unsaved bookmarks. Continue anyway?", "Confirm?"))
				return;
		}

		JFileChooser chooser = DatasetFileChooser
				.getDatasetFileChooser(app.options.dataDirectory);

		int returnVal = chooser.showOpenDialog(mainWindow);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			loadDataset(chooser.getSelectedFile());
		}
	}

	public void loadDataset(File file) {
		app.loadDataset(file);
	}

	public void loadDataset(String path) {
		app.loadDataset(path);
	}

	public void reloadDataset() {
		app.reloadDataset();
	}

	public void setDataset(Dataset newDataset) {
		try {
			app.trackManager.setDataset(newDataset);
			mainWindow.setTitle("Genome Browser - " + newDataset.getName());

			// fill chromosome menu and select first chromosome
			// and initialize ViewParams
			List<Sequence> sequences = newDataset.getSequences();
			mainWindow.sideBar.setSequences(sequences);
			if (sequences.size() > 0) {
				setSelectedSequence(sequences.get(0), true);
			} else {
				setSelectedSequence((Sequence) null, true);
			}
			
			log.info("On event dispatch thread? " + SwingUtilities.isEventDispatchThread());

			// ditch bookmarks.
			// What if they're unsaved? Need to confirm before we get here.
			// See Application.reloadDataset()
			app.bookmarkCatalog.clear();
			
			// load any associated bookmarks
			// TODO fix this smelly hack
			if (newDataset != Dataset.EMPTY_DATASET) {
				List<String> bookmarkCollectionNames = app.io.getBookmarkCollectionNames(newDataset.getUuid());
				log.info("Loading bookmark sets: " + bookmarkCollectionNames);
				for (String name : bookmarkCollectionNames) {
					app.bookmarkCatalog.addBookmarkDataSource(app.io.loadBookmarks(name));
				}
				if (app.options.openBookmarks && bookmarkCollectionNames.size() > 0) {
					//app.publishEvent(new Event(this, "open.bookmarks"));
					openBookmarksPanel();
				}
			}

			actions.reloadDatasetAction.setEnabled(true);
		} catch (Exception e) {
			showErrorMessage("Error loading dataset", e);
		}
	}

	public void showNewProjectWizard() {
		// unsaved bookmarks?
		if (app.bookmarkCatalog.isDirty()) {
			if (!showConfirmDialog("You have unsaved bookmarks. Continue anyway?", "Confirm?"))
				return;
		}
		log.info("show new project wizard");
		NewProjectWizard dialog = new NewProjectWizard(mainWindow, app);
		dialog.addDialogListener(new DialogListener() {
			public void cancel() {
			}

			public void error(String message, Exception e) {
				showErrorMessage(message, e);
			}

			public void ok(String action, Object result) {
				final ProjectDescription projectDescription = (ProjectDescription) result;
				log.info("Creating new project: " + projectDescription);
				try {
					if ("UCSC".equals(projectDescription.getDataSource())) {
						UcscDatasetBuilder builder = new UcscDatasetBuilder();
						builder.drive(projectDescription, app);
					} else if ("NCBI"
							.equals(projectDescription.getDataSource())) {
						showStatusMessage(
								"Import from NCBI not implemented yet",
								"Not implemented yet");
					} else if (LOCAL_DATA_LABEL.equals(projectDescription
							.getDataSource())) {
						SqliteDatasetBuilder builder = new SqliteDatasetBuilder(
								projectDescription.getFile());
						UUID datasetUuid = builder
								.beginNewDataset(projectDescription
										.getProjectName());
						builder.setAttribute(datasetUuid, "species",
								projectDescription.getOrganism());
						builder.setAttribute(datasetUuid, "created-on",
								new Date());
						builder.setAttribute(datasetUuid, "created-by", System
								.getProperty("user.name"));
						List<SequenceDescription> sequences = projectDescription
								.getSequenceDescriptions();
						for (SequenceDescription sequence : sequences) {
							builder.addSequence(sequence.name, sequence.length,
									sequence.topology);
						}
						if (projectDescription.getGenomeFile() != null) {
							UUID uuid = builder
									.addTrack("gene", "Genome",
											new GenomeFileFeatureSource(
													projectDescription
															.getGenomeFile()));
							builder.setAttribute(uuid, "viewer", "Gene");
							builder.setAttribute(uuid, "top", 0.46);
							builder.setAttribute(uuid, "height", 0.08);
							builder.setAttribute(uuid, "imported-from-file",
									projectDescription.getFile()
											.getAbsoluteFile());
						}
						// TODO unify new dataset code
						app.setDataset(builder.getDataset(), projectDescription.getFile());
//						app.options.datasetUrl = projectDescription.getFile()
//								.getAbsolutePath();
					} else {
						log.error("unrecognized datasource: "
								+ projectDescription.getDataSource());
						throw new RuntimeException(
								"Unrecognized datasource from New Project Wizard.");
					}
					// TODO popup success message
				} catch (Exception e) {
					showErrorMessage("Error creating dataset", e);
				}
			}
		});
		dialog.setVisible(true);
	}

	// ---- information -------------------------------------------------------

	public void showAbout() {
		log.info("showAbout()");
		Point p = mainWindow.getLocation();
		p.translate(80, 60);

		AboutDialog dialog = new AboutDialog(mainWindow, true, this);
		dialog.setLocation(p);
		dialog.setVisible(true);
	}

	public boolean showConfirmDialog(String message, String title) {
		int result = JOptionPane.showConfirmDialog(mainWindow, message, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		return result == JOptionPane.YES_OPTION;
	}

	public void showExtendedErrorDialog(String title, String message,
			Exception e) {
		ExtendedErrorDialog errorDialog = new ExtendedErrorDialog(mainWindow,
				title, message, e);
		errorDialog.setVisible(true);
	}

	public void showErrorMessage(String message, Throwable t) {
		log.warn(message, t);
		StringBuilder errorMessage = new StringBuilder(message);

		while (t != null) {
			errorMessage.append("\n");
			if (!t.getClass().equals(RuntimeException.class)
					&& !t.getClass().equals(Exception.class)) {
				errorMessage.append(t.getClass().getSimpleName()).append(":");
			}
			errorMessage.append(t.getMessage());
			t = t.getCause();
		}
		Dialogs.showMessageDialog(mainWindow, errorMessage.toString(), "Error");
	}

	public void showErrorMessage(String message) {
		Dialogs.showMessageDialog(mainWindow, message, "Error", FileUtils
				.getIconOrBlank("error_icon.png"));
	}

	public void showStatusMessage(String message, String title) {
		Dialogs.showMessageDialog(mainWindow, message, title, FileUtils
				.getIconOrBlank("warning_icon.png"));
	}

	public void showProgressPopup(String message, ProgressReporter progressReporter) {
		ProgressPopup progressPopup = new ProgressPopup(mainWindow);
		progressPopup.init(message);
		progressReporter
				.addProgressListener(new SwingThreadProgressListenerWrapper(
						progressPopup));
	}

	public void showTrackInfo() {
		log.info("showTrackInfo()");
		if (mainWindow.genomeView.popupCoordinates != null) {
			List<Track<? extends Feature>> tracks = app.trackManager
					.getTracksAt(mainWindow.genomeView.popupCoordinates);
			TrackInfoDialog dialog = new TrackInfoDialog(mainWindow, tracks);
			Point p = mainWindow.getLocation();
			p.translate(80, 60);
			dialog.setLocation(p);
			dialog.setVisible(true);
		}
	}

	public void logFeatureCount() {
		FeatureCounter counter = new FeatureCounter();

		counter.count(app.trackManager, viewParameters.getSequence(),
				viewParameters.getStart(), viewParameters.getEnd());
		
		log.info("feature count = " + counter.getCount());
	}

	public void showHelp() {
		log.info("show help");
		openBrowser("http://gaggle.systemsbiology.net/docs/geese/genomebrowser/");
	}

	public void showTrackEditorHelp() {
		log.info("show track editor help");
		openBrowser("http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/track_visual_properties/");
	}

	public void showIsbWebsite() {
		log.info("show ISB website");
		openBrowser("http://www.systemsbiology.org/");
	}

	public void showProjectPropertiesDialog() {
		ProjectPropertiesDialog dialog = new ProjectPropertiesDialog(
				mainWindow, app.getDataset(), app.options.datasetUrl);
		dialog.setVisible(true);
	}

	public void openBrowser(String url) {
		try {
        Desktop.getDesktop().browse(new java.net.URI(url));
		} catch (Exception e) {
			log.error("Failed to open browser", e);
			showErrorMessage("Failed to open browser. Please see: " + url, e);
		}
	}

	/**
	 * Take the currently highlighted (or displayed) coordinates and show them
	 * in the UCSC genome browser
	 */
	public void showInUcscGenomeBrowser() {
		// TODO move URL construction to UCSC package
		// ucsc.db.name, domain, ucsc.clade, ucsc.gene.table
		Dataset dataset = app.getDataset();
		if (dataset != null) {
			String db = dataset.getAttributes().getString("ucsc.db.name");
			String domain = dataset.getAttributes().getString("domain");
			if (!isNullOrEmpty(db) && !isNullOrEmpty(domain)) {
				Sequence sequence = viewParameters.getSequence();
				String seq = sequence.getAttributes().getString("ucsc.name",
						sequence.getSeqId());
				int start = viewParameters.getStart();
				int end = viewParameters.getEnd();
				Segment selectedSegment = app.selections
						.getEnclosingSegment(sequence.getSeqId());
				if (selectedSegment != null
						&& selectedSegment.overlaps(viewParameters
								.getVisibleSegment())) {
					start = selectedSegment.start;
					end = selectedSegment.end;
				}

				String urlBase = null;
				if (Category.fromString(domain).isEukaryotic()) {
					urlBase = "http://genome.ucsc.edu/";
				} else {
					urlBase = "http://microbes.ucsc.edu/";
				}

				String url = String.format(
						"%scgi-bin/hgTracks?db=%s&position=%s:%d-%d", urlBase,
						db, seq, start, end);
				openBrowser(url);
			} else {
				showErrorMessage("In order to link to the UCSC genome browser their must be "
						+ "property values for both <i>ucsc.db.name</i> and <i>domain</i>. At least "
						+ "one of these is not currently set, so we can't continue.");
			}
		}
	}

	// ------------------------------------------------------------------------

	public void setCursorTool(CursorTool tool) {
		mainWindow.genomeView.setCursorTool(tool);
		mainWindow.sideBar.setCursorTool(tool);
	}

	public void exit(int status) {
		app.shutdown(status);
	}

	public void bringToFront() {
		mainWindow.setExtendedState(JFrame.NORMAL);
		mainWindow.setAlwaysOnTop(true);
		mainWindow.setAlwaysOnTop(false);
	}

	public void minimize() {
		mainWindow.setExtendedState(JFrame.ICONIFIED);
	}

	public void showImportNcbiGenomeDialog(final String filename) {
		log.info("showImportNcbiGenomeDialog()");
		final NcbiQueryDialog dialog = new NcbiQueryDialog();
		dialog
				.addNcbiQueryDialogListener(new NcbiQueryDialog.NcbiQueryDialogListener() {
					public void genomeProjectDownloaded(NcbiGenome genome) {
						dialog.setVisible(false);
						dialog.dispose();
						log.info("downloaded genome project ("
								+ genome.getProjectId() + ")"
								+ genome.getOrganismName());
						NcbiGenomeToDataset ngtd = new NcbiGenomeToDataset(
								app.io.getDatasetBuilder(new File(filename)));
						app.setDataset(ngtd.convert(genome), new File(filename));
//						app.options.datasetUrl = filename;
					}

					public void canceled() {
						dialog.setVisible(false);
						dialog.dispose();
					}

					public void error(String title, String message, Exception e) {
						dialog.setVisible(false);
						dialog.dispose();
						showExtendedErrorDialog(title, message, e);
					}
				});
		dialog.setVisible(true);
	}

	// no longer used? see showNewProjectWizard instead
	public void showImportUcscGenomeDialog(final String filename) {
		log.info("showImportUcscGenomeDialog");
		final ImportUcscGenome dialog = new ImportUcscGenome();
		dialog.setDatasetBuilder(app.io.getDatasetBuilder(new File(filename)));
		dialog.addDialogListener(new DialogListener() {
			public void ok(String action, Object result) {
				dialog.setVisible(false);
				dialog.dispose();
				Dataset dataset = (Dataset) result;
				log.info("downloaded genome from UCSC " + dataset.getName()
						+ " (" + dataset.getAttributes().getString("dbName")
						+ ")");
				// TODO unify new dataset code
				app.setDataset(dataset, new File(filename));
//				app.options.datasetUrl = filename;
			}

			public void cancel() {
				dialog.setVisible(false);
				dialog.dispose();
			}

			public void error(String message, Exception e) {
				dialog.setVisible(false);
				dialog.dispose();
				showErrorMessage(message, e);
			}
		});
		dialog.setVisible(true);
	}

	public void showImportFileGenomeDialog(final String filename) {
		log.info("showImportFileGenomeDialog");
		final ImportFileGenome dialog = new ImportFileGenome(mainWindow,
				app.options.workingDirectory);
		dialog.setDatasetBuilder(app.io.getDatasetBuilder(new File(filename)));
		dialog.addDialogListener(new DialogListener() {
			public void cancel() {
			}

			public void error(String message, Exception e) {
				showErrorMessage(message, e);
			};

			public void ok(String action, Object result) {
				Dataset dataset = (Dataset) result;
				app.setDataset(dataset, new File(filename));
//				app.options.datasetUrl = filename;
			}
		});
		dialog.setVisible(true);
	}

	public void showImportTrackWizard() {
		log.info("showImportTrackWizard()");

		// TODO better way to get trackRendererRegistry?
		TrackRendererRegistry trackRendererRegistry = app.trackManager
				.getTrackRendererRegistry();

		// TODO add track to empty dataset
		if (!app.datasetIsLoaded()) {
			showErrorMessage("Create a new dataset first.");
			return;
		}

		final ImportTrackWizard wiz = new ImportTrackWizard();
		wiz.setOptions(getOptions());
		wiz.setTrackImporter(app.io.getTrackImporter());
		wiz.setDataset(app.getDataset());
		wiz.setTrackReaderInfos(TrackLoaderRegistry.newInstance().getLoaders());
		wiz.setTrackTypes(trackRendererRegistry.getTrackTypes());
		wiz.setTrackTypeToRenderersMap(trackRendererRegistry
				.getTrackTypeTpRenderersMap());

		WizardMainWindow wizardWindow = new WizardMainWindow(this.mainWindow,
				wiz);
		wiz.setExceptionReporter(wizardWindow);

		wizardWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if ("cancel".equals(event.getActionCommand())) {
					log.info("track import canceled");
					log.info(wiz.toString());
				} else if ("ok".equals(event.getActionCommand())) {
					log.info("track import done");
					log.info(wiz.toString());
					app.trackManager.refresh();
					repaint();
				}
			}
		});
	}

	public void importNcbiGenomeAfterGuiCreated(String species) {
		// TODO importNcbiGenomeAfterGuiCreated
		log.info("importNcbiGenomeAfterGuiCreated()");
	}

	public void importTrack() {
		// TODO importTrack
		log.info("importTrack()");
	}

	public void showDeleteTrackDialog() {
		log.info("show delete tracks dialog");
		DeleteTracksDialog dialog = new DeleteTracksDialog(mainWindow);
		dialog.setTracks(app.trackManager.getTracks());
		dialog.addDialogListener(new DialogListener() {
			public void cancel() {
				log.info("cancel");
			}

			public void error(String message, Exception e) {
				showExtendedErrorDialog("Error", message, e);
			}

			@SuppressWarnings("unchecked")
			public void ok(String action, Object result) {
				List<UUID> tracksToDelete = (List<UUID>) result;
				showConfirmDialog("Are you sure you want to delete "
						+ tracksToDelete.size() + " tracks?", "Confirm delete");
				try {
					app.deleteTracks(tracksToDelete);
				} catch (Exception e) {
					showExtendedErrorDialog("Error deleting tracks", e
							.getMessage(), e);
				}
			}
		});
		dialog.setVisible(true);
	}

	public void selectDirAndSaveDataset() {
		// TODO selectDirAndSaveDataset
		log.info("selectDirAndSaveDataset()");
	}

	public void showTrackEditor(UUID uuid) {
		log.info("showTrackEditor()");
		TrackVisualPropertiesEditor trackEditor = new TrackVisualPropertiesEditor(
				app.trackManager, mainWindow, uuid);
		trackEditor.setTrackSaver(app.io.getTrackSaver());
		trackEditor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if ("update".equals(event.getActionCommand()))
					repaint();
				else if ("ok".equals(event.getActionCommand())) {
					repaint();
				} else if ("cancel".equals(event.getActionCommand())) {
					repaint();
				} else if ("help".equals(event.getActionCommand()))
					showTrackEditorHelp();
			}
		});
	}

	public void showTrackVisibilityDialog() {
		final TrackVisibilityDialog dialog = new TrackVisibilityDialog(
				mainWindow, app.getDataset().getTracks());
		dialog.addEventListener(new EventListener() {
			public void receiveEvent(Event event) {
				if ("update".equals(event.getAction())) {
					app.trackManager.refresh();
					repaint();
				} else if ("cancel".equals(event.getAction())) {
					log.info("track visibility dialog canceled");
					dialog.close();
					app.trackManager.refresh();
					repaint();
				} else if ("error".equals(event.getAction())) {
					showErrorMessage("Error setting visibility:",
							(Exception) event.getData());
				} else if ("ok".equals(event.getAction())) {
					app.trackManager.refresh();
					repaint();

					// TODO DB stuff doesn't belong here!
					// write track attributes back to DB
					TrackSaver trackSaver = app.io.getTrackSaver();
					if (trackSaver != null) {
						for (Track<? extends Feature> track : app.trackManager
								.getTracks()) {
							trackSaver.updateTrack(track);
						}
					}

					dialog.close();
				}
			}
		});
		dialog.setVisible(true);
	}

	public void importCoordinateMap() {
		LoadCoordinateMapDialog dialog = new LoadCoordinateMapDialog(
				mainWindow, app.options);
		dialog.addDialogListener(new DialogListener() {

			public void cancel() {
			}

			public void error(String message, Exception e) {
				showErrorMessage(message);
			}

			public void ok(String action, Object result) {
				File file = (File) result;
				log.info(action + " -> " + String.valueOf(file));
				CoordinateMapFileIterator cmfi = null;
				try {
					cmfi = new CoordinateMapFileIterator(file);
					app.io.createCoordinateMapping(app.getDataset().getUuid(),
							FileUtils.stripExtension(file.getName()), cmfi);
				} catch (Exception e) {
					showErrorMessage("Error loading file " + file.getName()
							+ ": " + e.getMessage());
				} finally {
					if (cmfi != null) {
						cmfi.cleanup();
					}
				}
			}
		});
		dialog.setVisible(true);
	}

	/**
	 * UI responds to track added event
	 */
	public void trackAdded(UUID trackUuid) {
		// is some of the just-added data visible?
		List<Segment> segments = app.io.getTrackCoordinateRange(trackUuid);
		Segment visibleSegment = viewParameters.getVisibleSegment();
		for (Segment segment : segments) {
			if (segment.overlaps(visibleSegment)) {
				// if we can see some of the data, we're OK
				return;
			}
		}

		// if none of the new data is visible, ask if we want to move to it.
		if (showConfirmDialog("Move viewport to location of new data?",
				"New Track Added")) {
			for (Segment segment : segments) {
				if (segment.seqId.equals(visibleSegment.seqId)) {
					centerOnSegment(segment);
					return;
				}
			}
			if (segments.size() > 0)
				centerOnSegment(segments.get(0));
		}
	}

	public void showFastaDialog() {
		SequenceFetcher sequenceFetcher = app.getSequenceFetcher();
		List<Sequence> sequences = app.getDataset().getSequences();
		ImportFastaDialog dialog = new ImportFastaDialog(mainWindow, app.options, sequences, sequenceFetcher);
		dialog.setVisible(true);
	}

	public void showSequenceDialog() {
		String sequenceName = null;
		Strand strand = Strand.any;
		int start = -1;
		int end = -1;
		String seq = "--not available--";
		
		Segment segment = app.selections.getSingleSelection();
		if (segment==null) {
			sequenceName = viewParameters.getSequence().getSeqId();
			strand = Strand.forward;
			start = viewParameters.getStart();
			end = viewParameters.getEnd();
		}
		else {
			sequenceName = segment.seqId;
			strand = app.selections.getStrandHint();
			start = segment.start;
			end = segment.end;
		}

		SequenceDialog dialog = new SequenceDialog(mainWindow, sequenceName, strand, start, end, app.getSequenceFetcher());
		dialog.setVisible(true);
	}
}
