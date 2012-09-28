package org.systemsbiology.genomebrowser.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.Hyperlink;
import org.systemsbiology.util.StringUtils;

// may want to make this dynamic w/ the ability to add or replace default actions. Replacing
// default actions would be a powerful hook to give to plugins.

@SuppressWarnings("serial")
public class Actions {

	private UI ui;

	// Actions
	AboutAction aboutAction;
	HelpAction helpAction;
	ExitAction exitAction;
	LogMemoryUsageAction logMemoryUsageAction;
	LogFeatureCountAction logFeatureCountAction;
	
	NewDatasetAction newDatasetAction;
	LoadLocalDatasetAction loadLocalDatasetAction;
	LoadRemoteDatasetAction loadRemoteDatasetAction;
	ReloadDatasetAction reloadDatasetAction;
	SaveDatasetAction saveDatasetAction;
	ProjectPropertiesAction projectPropertiesAction;
	
	AddBookmarkAction addBookmarkAction;
	AddBookmarkDirectAction addBookmarkDirectAction;
	ImportFastaToDb importFastaToDb;
	ToggleBookmarkPanelAction toggleBookmarkPanelAction;
	LoadBookmarksAction loadBookmarksAction;
	ExportBookmarksAction exportBookmarksAction;
	NewBookmarkSetAction newBookmarkSetAction;

	DeselectAllAction deselectAllAction;
	FindAction findAction;
	FindNextAction findNextAction;
	GotoSelectionAction gotoSelectionAction;
	ZoomToSelectionAction zoomToSelectionAction;
	ZoomOutAllAction zoomOutAllAction;
	TrackInfoAction trackInfoAction;

	SelectCursorToolAction selectMouseToolAction;
	SelectCursorToolAction scrollerMouseToolAction;
	SelectCursorToolAction crosshairsMouseToolAction;

	ImportTrackAction importTrackAction;
	DeleteTrackAction deleteTrackAction;
//	ImportDatasetFromNcbiAction importDatasetFromNcbiAction;
//	ImportDatasetFromUcscAction importDatasetFromUcscAction;
	TrackVisualPropertiesEditorAction trackVisualPropertiesEditorAction;
	ImportTrackWizardAction importTrackWizardAction;
	ImportCoordinateMapAction importCoordinateMapAction;

	TrackVisibilityDialogAction trackVisibilityDialogAction;

	ShowInUcscGenomeBrowser showInUcscGenomeBrowser;

	ShowSequenceAction showSequenceAction;


	public Actions(UI ui) {
		this.ui = ui;
		aboutAction = new AboutAction(ui);
		helpAction = new HelpAction(ui);
		exitAction = new ExitAction(ui);
		newDatasetAction = new NewDatasetAction(ui);
		loadLocalDatasetAction = new LoadLocalDatasetAction(ui);
		loadRemoteDatasetAction = new LoadRemoteDatasetAction(ui);
		reloadDatasetAction = new ReloadDatasetAction(ui);
		addBookmarkAction = new AddBookmarkAction(ui);
		addBookmarkDirectAction = new AddBookmarkDirectAction(ui);
		showSequenceAction = new ShowSequenceAction(ui);
		importFastaToDb = new ImportFastaToDb(ui);
		deselectAllAction = new DeselectAllAction(ui);
		zoomToSelectionAction = new ZoomToSelectionAction(ui);
		toggleBookmarkPanelAction = new ToggleBookmarkPanelAction(ui);
		loadBookmarksAction = new LoadBookmarksAction(ui);
		exportBookmarksAction = new ExportBookmarksAction(ui);
		findAction = new FindAction(ui);
		findNextAction = new FindNextAction(ui);
		gotoSelectionAction = new GotoSelectionAction(ui);
		selectMouseToolAction = new SelectCursorToolAction(ui, CursorTool.select, "cursor16.png", KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
		scrollerMouseToolAction = new SelectCursorToolAction(ui, CursorTool.hand, "hand16.png", KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
		crosshairsMouseToolAction = new SelectCursorToolAction(ui, CursorTool.crosshairs, "crosshairs16.png", KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
		trackInfoAction = new TrackInfoAction(ui);
		importTrackAction = new ImportTrackAction(ui);
		deleteTrackAction = new DeleteTrackAction(ui);
//		importDatasetFromUcscAction = new ImportDatasetFromUcscAction(ui);
//		importDatasetFromNcbiAction = new ImportDatasetFromNcbiAction(ui);
		newBookmarkSetAction = new NewBookmarkSetAction(ui);
		trackVisualPropertiesEditorAction = new TrackVisualPropertiesEditorAction(ui);
		importTrackWizardAction = new ImportTrackWizardAction(ui);
		saveDatasetAction = new SaveDatasetAction(ui);
		zoomOutAllAction = new ZoomOutAllAction(ui);
		importCoordinateMapAction = new ImportCoordinateMapAction(ui);
		projectPropertiesAction = new ProjectPropertiesAction(ui);
		showInUcscGenomeBrowser = new ShowInUcscGenomeBrowser(ui);
		trackVisibilityDialogAction = new TrackVisibilityDialogAction(ui);
		logMemoryUsageAction = new LogMemoryUsageAction();
		logFeatureCountAction = new LogFeatureCountAction(ui);
	}

	public OpenBrowserAction createOpenBrowserAction(Hyperlink link) {
		return new OpenBrowserAction(ui, link.getText(), link.getUrl());
	}

	static class NotImplementedAction extends AbstractAction {

		public NotImplementedAction() {
			super("Not Implemented");
			putValue(Action.SHORT_DESCRIPTION, "This action has not yet been implemented");
		}

		public NotImplementedAction(String name) {
			super(name);
			putValue(Action.SHORT_DESCRIPTION, "This action has not yet been implemented");
		}


		public NotImplementedAction(String name, Icon icon) {
			super(name, icon);
		}

		public void actionPerformed(ActionEvent e) {
			String name = (String)getValue(Action.NAME);
			JOptionPane.showMessageDialog(null, "The action \"" + name + "\" has not yet been implemented.", name + " not implemented", JOptionPane.WARNING_MESSAGE);
		}
	}

	static class AboutAction extends AbstractAction {
		UI app;

		public AboutAction(UI app) {
			super("About");
			this.app = app;
			putValue(Action.SHORT_DESCRIPTION, "Show information about this program.");
		}

		public AboutAction(Icon icon, UI app) {
			super("About", icon);
			this.app = app;
		}

		public void actionPerformed(ActionEvent e) {
			app.showAbout();
		}
	}

	static class AddBookmarkAction extends AbstractAction {
		UI ui;

		public AddBookmarkAction(UI ui) {
			super("Add Bookmark");
			this.ui = ui;
			putValue(Action.SHORT_DESCRIPTION, "Create a bookmark for the currently selected position the genome.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("add.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.bookmarkCurrentSelection(false);
		}
	}

	static class AddBookmarkDirectAction extends AbstractAction {
		UI ui;

		public AddBookmarkDirectAction(UI ui) {
			super("Add Bookmark directly into the sidebar");
			this.ui = ui;
			putValue(Action.SHORT_DESCRIPTION, "Create a bookmark for the currently selected position the genome.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("add.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.bookmarkCurrentSelection(true);
		}
	}
	
	static class ShowSequenceAction extends AbstractAction {
		UI ui;

		public ShowSequenceAction(UI ui) {
			super("Show sequence");
			this.ui = ui;
			putValue(Action.SHORT_DESCRIPTION, "Show sequence for currently selected region.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("helix.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showSequenceDialog();
		}
	}
	
	static class ImportFastaToDb extends AbstractAction {
		UI ui;

		public ImportFastaToDb(UI ui) {
			super("Import Fasta to the current DataSet");
			this.ui = ui;
			putValue(Action.SHORT_DESCRIPTION, "Import FASTA file to the current DataSet.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("add.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showFastaDialog();
			//ImportFastaDialog dialog = new ImportFastaDialog(parent, options)
		}
	}

	static class DeselectAllAction extends AbstractAction {
		UI ui;

		public DeselectAllAction(UI ui) {
			super("Deselect");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Clear all selections.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			ui.deselect();
		}
	}

	static class ExitAction extends AbstractAction {
		UI ui;
		
		public ExitAction(UI ui) {
			this.ui = ui;
			putValue(AbstractAction.NAME, "Exit");
			putValue(AbstractAction.SHORT_DESCRIPTION, "Shut down the program");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("power.png"));		
		}

		public void actionPerformed(ActionEvent e) {
			ui.exit(0);
		}
	}

	static class ExportBookmarksAction extends AbstractAction {
		UI ui;

		public ExportBookmarksAction(UI ui) {
			super("Export Bookmarks");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Save bookmarks to a file.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("save.gif"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.exportBookmarks();
		}
	}

	static class FindAction extends AbstractAction {
		UI ui;

		public FindAction(UI ui) {
			super("Find");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Find features based on keywords (gene name, etc).");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("find.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showFindDialog();
		}
	}

	static class FindNextAction extends AbstractAction {
		UI ui;

		public FindNextAction(UI ui) {
			super("Find Next");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Find next matching item.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("find_next.png"));
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			ui.findNext();
		}
	}

	static class GotoSelectionAction extends AbstractAction {
		UI ui;

		public GotoSelectionAction(UI ui) {
			super("Goto Selections");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Centers the viewport on the current selections.");
		}

		public void actionPerformed(ActionEvent e) {
			ui.centerOnSelection();
		}
	}

	static class HelpAction extends AbstractAction {
		UI ui;

		public HelpAction(UI ui) {
			super("Help");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Open a help page for this program in a browser.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("help.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showHelp();
		}
	}

//	static class ImportDatasetFromNcbiAction extends AbstractAction {
//		UI ui;
//
//		public ImportDatasetFromNcbiAction(UI ui) {
//			super("Import NCBI genome");
//			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK));
//			this.ui = ui;
//		}
//
//		public void actionPerformed(ActionEvent e) {
//			ui.showImportNcbiGenomeDialog();
//		}
//	}
//	
//	static class ImportDatasetFromUcscAction extends AbstractAction {
//		UI ui;
//
//		public ImportDatasetFromUcscAction(UI ui) {
//			super("Import UCSC genome");
//			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK));
//			this.ui = ui;
//		}
//
//		public void actionPerformed(ActionEvent e) {
//			ui.showImportUcscGenomeDialog();
//		}
//	}

	static class ImportTrackAction extends AbstractAction {
		UI ui;

		public ImportTrackAction(UI ui) {
			super("Import Track");
			this.ui = ui;
		}

		public void actionPerformed(ActionEvent e) {
			ui.importTrack();
		}
	}

	static class DeleteTrackAction extends AbstractAction {
		UI ui;

		public DeleteTrackAction(UI ui) {
			super("Delete Track");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Select tracks and delete them");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showDeleteTrackDialog();
		}
	}

	static class LoadBookmarksAction extends AbstractAction {
		UI ui;

		public LoadBookmarksAction(UI ui) {
			super("Load Bookmarks");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Load bookmarks from a tab delimited file.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("folder_open.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.loadBookmarks();
		}
	}

	static class LoadLocalDatasetAction extends AbstractAction {
		UI ui;

		public LoadLocalDatasetAction(UI ui) {
			super("Open dataset");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Load a dataset from the local file system.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("file.png"));
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showLoadLocalDatasetDialog();
		}
	}

	static class LoadRemoteDatasetAction extends AbstractAction {
		UI ui;

		public LoadRemoteDatasetAction(UI ui) {
			super("Load dataset from URL");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Load a remote dataset from a URL.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("fileweb.png"));
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showLoadDatasetFromUrlDialog();
		}
	}

	static class NewBookmarkSetAction extends AbstractAction {
		UI ui;

		public NewBookmarkSetAction(UI ui) {
			super("New Bookmark Set");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Create a new collection of bookmarks.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("add_folder.png"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.newBookmarkSet();
		}
	}

	static class ReloadDatasetAction extends AbstractAction {
		UI ui;

		public ReloadDatasetAction(UI ui) {
			super("Reload dataset");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Reload the current dataset - useful when editing the dataset manually.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("view-refresh.png"));
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			this.setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			ui.reloadDataset();
		}
	}

	/**
	 * Select one of the mouse cursor tools.
	 * @author cbare
	 */
	static class SelectCursorToolAction extends AbstractAction {
		UI ui;
		private CursorTool cursorTool;

		public SelectCursorToolAction(UI ui, CursorTool cursorTool, String iconName, KeyStroke acceleratorKey) {
			super(StringUtils.toTitleCase(cursorTool.toString()) + " tool");
			this.cursorTool = cursorTool;
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Select the " + cursorTool + " cursor tool.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank(iconName));
			putValue(AbstractAction.ACCELERATOR_KEY, acceleratorKey);
		}

		public void actionPerformed(ActionEvent e) {
			ui.setCursorTool(cursorTool);
		}
	}

	static class ToggleBookmarkPanelAction extends AbstractAction implements PanelStatusListener {
		UI ui;

		public ToggleBookmarkPanelAction(UI ui) {
			super("Show Bookmarks");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Open a panel showing bookmarks.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("bookmark.gif"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.toggleBookmarksPanel();
		}

		public void statusChanged(Status status) {
			if (status == PanelStatusListener.Status.Open) {
				putValue(AbstractAction.NAME, "Hide Bookmarks");
			}
			else {
				putValue(AbstractAction.NAME, "Show Bookmarks");
			}
		}
	}

	static class TrackInfoAction extends AbstractAction {
		UI ui;

		public TrackInfoAction(UI ui) {
			super("Track Info");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Show information about a track.");
		}

		public void actionPerformed(ActionEvent e) {
			ui.showTrackInfo();
		}
	}

	static class ZoomToSelectionAction extends AbstractAction {
		UI ui;

		public ZoomToSelectionAction(UI ui) {
			super("Zoom to selections");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Make the viewing area cover all selected segments.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.zoomToSelection();
		}
	}

	static class TrackVisualPropertiesEditorAction extends AbstractAction {
		UI ui;

		public TrackVisualPropertiesEditorAction(UI ui) {
			super("Track Visual Properties Editor");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Edit the layout and arrangement of data tracks.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			UUID uuid = null;
			if (e.getActionCommand().startsWith("uuid=")) {
				uuid = UUID.fromString(e.getActionCommand().substring(5));
			}
			ui.showTrackEditor(uuid);
		}
	}


	static class ImportTrackWizardAction extends AbstractAction {
		UI ui;

		public ImportTrackWizardAction(UI ui) {
			super("Import Track");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Import tracks into the current data set.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showImportTrackWizard();
		}
	}

	static class SaveDatasetAction extends AbstractAction {
		UI ui;

		public SaveDatasetAction(UI ui) {
			super("Save Dataset");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Saves dataset to the local file system.");
		}

		public void actionPerformed(ActionEvent e) {
			ui.selectDirAndSaveDataset();
		}
	}

	static class ZoomOutAllAction extends AbstractAction {
		UI ui;

		public ZoomOutAllAction(UI ui) {
			super("Zoom Out All");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Zoom all sequences out as much as possible.");
		}

		public void actionPerformed(ActionEvent e) {
			ui.zoomOutAll();
		}
	}

	static class NewDatasetAction extends AbstractAction {
		UI ui;

		public NewDatasetAction(UI ui) {
			super("New Dataset");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Create a new dataset.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showNewProjectWizard();
		}
	}

	static class ImportCoordinateMapAction extends AbstractAction {
		UI ui;

		public ImportCoordinateMapAction(UI ui) {
			super("Import Coordinate Map");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Import a mapping from names to coordinates on the genome.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("coordinate-map.jpg"));
		}

		public void actionPerformed(ActionEvent e) {
			ui.importCoordinateMap();
		}
	}


	static class ProjectPropertiesAction extends AbstractAction {
		UI ui;

		public ProjectPropertiesAction(UI ui) {
			super("Project Properties");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Display properties for the currently open project.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		public void actionPerformed(ActionEvent e) {
			ui.showProjectPropertiesDialog();
		}
	}

	static class ShowInUcscGenomeBrowser extends AbstractAction {
		UI ui;
		
		public ShowInUcscGenomeBrowser(UI ui) {
			super("Show in UCSC Genome Browser");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Open the currently displayed section of genome in the UCSC genome browser.");
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("ucsc.gif"));
		}
		
		public void actionPerformed(ActionEvent e) {
			ui.showInUcscGenomeBrowser();
		}
	}

	static class TrackVisibilityDialogAction extends AbstractAction {
		UI ui;
		
		public TrackVisibilityDialogAction(UI ui) {
			super("Visibility");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Toggle the visibility of tracks as track groups.");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
		}
		
		public void actionPerformed(ActionEvent e) {
			ui.showTrackVisibilityDialog();
		}
	}

	static class OpenBrowserAction extends AbstractAction {
		private UI ui;
		private String url;

		public OpenBrowserAction(UI ui, String text, String url) {
			super(text);
			this.url = url;
			this.ui = ui;
		}

		public void actionPerformed(ActionEvent e) {
			ui.openBrowser(url);
		}
	}

	static class LogMemoryUsageAction extends AbstractAction {

		public LogMemoryUsageAction() {
			super("Log memory usage");
			putValue(AbstractAction.SHORT_DESCRIPTION, "Dump memory usage statistics to the log.");
		}

		public void actionPerformed(ActionEvent e) {
			Logger log = Logger.getLogger(UI.class);
			Runtime runtime = Runtime.getRuntime();
			log.info(String.format(" Memory usage\n" +
					"===========================================================\n" +
					"   Free heap: %,d\n" +
					"  Total heap: %,d\n" +
					"    Max heap: %,d\n" +
					"===========================================================\n\n",
					runtime.freeMemory(),
					runtime.totalMemory(),
					runtime.maxMemory()));
		}
	}

	static class LogFeatureCountAction extends AbstractAction {
		UI ui;
		
		public LogFeatureCountAction(UI ui) {
			super("Log feature count");
			this.ui = ui;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Write a count of features in the current viewing area to the log.");
		}
		
		public void actionPerformed(ActionEvent e) {
			ui.logFeatureCount();
		}
	}

	
}
