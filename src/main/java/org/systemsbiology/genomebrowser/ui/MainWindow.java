package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import org.systemsbiology.genomebrowser.visualization.TrackRendererScheduler;


/**
 * Main application window.
 */
public class MainWindow extends JFrame {

	GenomeViewPanel genomeView;
	SideBar sideBar;
	StatusBar status;
	BookmarksPanel bookmarksPanel;
	JMenuBar menuBar;
	private Box toolBarBox;
	private Map<String, JComponent> toolbars = new HashMap<String,JComponent>();
	private UI ui;
	private RightClickMenu rightClickMenu;


	public MainWindow(UI ui) {
		super("Genome Browser");
		this.ui = ui;
		this.setLayout(new BorderLayout());
		this.addWindowListener(new MyWindowListener(ui));

		menuBar = buildMenu(ui.getActions());
		setJMenuBar(menuBar);

		genomeView = new GenomeViewPanel(ui);
		rightClickMenu = new RightClickMenu(ui.getActions());
		genomeView.setRightClickMenu(rightClickMenu);
		genomeView.setPreferredSize(new Dimension(800,600));
		genomeView.init();
		this.addWindowFocusListener(genomeView);

		add(new HorizontalScrollPanel(genomeView), BorderLayout.CENTER);

		sideBar = new SideBar(ui);
		genomeView.params.addViewParametersListener(sideBar);
		add(sideBar, BorderLayout.WEST);

		status = new StatusBar(ui);
		add(status, BorderLayout.SOUTH);
		genomeView.params.addViewParametersListener(status);
		genomeView.addCrosshairsListener(status);

		bookmarksPanel = new BookmarksPanel(ui.app.bookmarkCatalog, ui);
		bookmarksPanel.addPanelStatusListener(ui.actions.toggleBookmarkPanelAction);
		bookmarksPanel.closeBookmarksPanel();
		add(bookmarksPanel, BorderLayout.EAST);

		toolBarBox = Box.createVerticalBox();
		add(toolBarBox, BorderLayout.NORTH);

		pack();
	}

	// perform additional initialization that needs to happen after
	// the constructor returns.
	public void init() {

		// map left and right arrow keys to scroll
		this.getRootPane().getActionMap().put("scroll-right-small", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.moveRight(UI.MOVE_SMALL);
			}
		});
		this.getRootPane().getActionMap().put("scroll-right-big", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.moveRight(UI.MOVE_BIG);
			}
		});
		this.getRootPane().getActionMap().put("scroll-left-small", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.moveLeft(UI.MOVE_SMALL);
			}
		});
		this.getRootPane().getActionMap().put("scroll-left-big", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.moveLeft(UI.MOVE_BIG);
			}
		});
		this.getRootPane().getActionMap().put("zoom-in", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.zoomIn();
			}
		});
		this.getRootPane().getActionMap().put("zoom-out", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.zoomOut();
			}
		});
		this.getRootPane().getActionMap().put("previous-sequence", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.previousSequence();
			}
		});
		this.getRootPane().getActionMap().put("next-sequence", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ui.nextSequence();
			}
		});
		this.getRootPane().getActionMap().put("log-memory-usage", ui.actions.logMemoryUsageAction);
		this.getRootPane().getActionMap().put("log-feature-count", ui.actions.logFeatureCountAction);

		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "scroll-right-small");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_MASK, false), "scroll-right-big");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "scroll-left-small");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_MASK, false), "scroll-left-big");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "zoom-out");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "zoom-in");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_MASK + Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "previous-sequence");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK + Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "next-sequence");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK + Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "log-memory-usage");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK + Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "log-feature-count");
		
		// TODO move initialization of scheduler
		// this seems like totally the wrong place to do this
		TrackRendererScheduler scheduler = new TrackRendererScheduler();
		scheduler.setGenomeViewPanel(genomeView);
		scheduler.setTrackManager(ui.app.trackManager);
		ui.scheduler = scheduler;
		ui.getViewParameters().addViewParametersListener(scheduler);
		//genomeView.setTrackRendererScheduler(scheduler);
		scheduler.startTaskRunnerThread();
	}

	// Firefox's menu structure:
	// File, Edit, View, History, Bookmarks, Tools, Window, Help
	
	
	// TextMate's menu structure:
	// File, Edit, View, ... Windows, Help
	
	// Safari's menu structure
	// File, Edit, View, History, Bookmarks, Tools, Window, Help

	// In all cases, Find and Selections are under Edit, Zoom and Toolbars are under View

	private JMenuBar buildMenu(Actions actions) {
		JMenu menu, sub;
		JMenuBar menuBar = new JMenuBar();

		menu = new JMenu("File");
		menu.add(new JMenuItem(actions.newDatasetAction));
		menu.add(new JMenuItem(actions.loadLocalDatasetAction));
		menu.add(new JMenuItem(actions.loadRemoteDatasetAction));
//		menu.add(new JMenuItem(actions.saveDatasetAction));
//		menu.addSeparator();
//		menu.add(new JMenuItem(actions.importDatasetFromNcbiAction));
//		menu.add(new JMenuItem(actions.importDatasetFromUcscAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.reloadDatasetAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.projectPropertiesAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.exitAction));
		menuBar.add(menu);

		menu = new JMenu("Find");
		menu.add(new JMenuItem(actions.findAction));
		menu.add(new JMenuItem(actions.findNextAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.deselectAllAction));
		menu.add(new JMenuItem(actions.gotoSelectionAction));
		menu.add(new JMenuItem(actions.zoomToSelectionAction));
		menuBar.add(menu);

		menu = new JMenu("View");
		// @see MainWindow.addToolbar
		sub = new JMenu("Toolbars");
		menu.add(sub);
		menu.add(new JMenuItem(actions.zoomToSelectionAction));
		menu.add(new JMenuItem(actions.zoomOutAllAction));
		menuBar.add(menu);

		menu = new JMenu("Tracks");
		menu.add(new JMenuItem(actions.importTrackWizardAction));
		menu.add(new JMenuItem(actions.deleteTrackAction));
		menu.add(new JMenuItem(actions.trackVisualPropertiesEditorAction));
		menu.add(new JMenuItem(actions.trackVisibilityDialogAction));
		menu.add(new JMenuItem(actions.importCoordinateMapAction));
		menuBar.add(menu);

		menu = new JMenu("Bookmarks");
		menu.add(new JMenuItem(actions.addBookmarkAction));
		menu.add(new JMenuItem(actions.addBookmarkDirectAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.loadBookmarksAction));
		menu.add(new JMenuItem(actions.exportBookmarksAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.toggleBookmarkPanelAction));
		menuBar.add(menu);
		
		menu = new JMenu("Sequence");
		menu.add(new JMenuItem(actions.showSequenceAction));
		menu.add(new JMenuItem(actions.importFastaToDb));
		menuBar.add(menu);

		// @see MainWindow.insertMenu
		menu = new JMenu("Tools");
		menu.add(new JMenuItem(actions.selectMouseToolAction));
		menu.add(new JMenuItem(actions.scrollerMouseToolAction));
		menu.add(new JMenuItem(actions.crosshairsMouseToolAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.showInUcscGenomeBrowser));
		menuBar.add(menu);

		menu = new JMenu("Help");
		menu.add(new JMenuItem(actions.helpAction));
		menu.addSeparator();
		menu.add(new JMenuItem(actions.aboutAction));
		menuBar.add(menu);

		return menuBar;
	}

	/**
	 * insert a menu to the left of the help menu
	 */
	public void insertMenu(JMenu menu) {
		int position = menuBar.getMenuCount() - 1;
		menuBar.add(menu, position);
	}

	public void insertMenu(String title, Action... actions) {
		JMenu newMenu = null;
		String[] titles = null;
		int t=0;

		// title can be a simple menu title like "Tools" or a multilevel
		// menu like "Tools|Foo|Bar"
		if (title != null)
			titles = title.split("\\|");

		// by default, add things to the tools menu
		if (titles==null || titles.length < 1 || (titles.length==1 && "".equals(titles[0])))
			titles = new String[] {"Tools"};

		// recursively search down the menu hierarchy for a matching path
		JMenu menu = null;
		MenuElement[] elements = menuBar.getSubElements();
		int i = 0;
		while (i < elements.length && t < titles.length) {
			MenuElement element = elements[i++];
			if (element instanceof JMenu) {
				JMenu testMenu = (JMenu)element;
				if (titles[t].equals(testMenu.getText())) {
					t++;
					menu = testMenu;
					elements = element.getSubElements();
					// submenus appear to be nested inside a popupmenu?
					if (elements.length==1 && elements[0] instanceof JPopupMenu)
						elements = elements[0].getSubElements();
					i = 0;
				}
			}
		}

		// add all menus not already present
		if (menu==null) {
			if (t<titles.length)
				menu = new JMenu(titles[t++]);
			else
				// pure paranoia - totally shouldn't happen
				menu = new JMenu("Extra Tools");
			newMenu = menu;
		}
		while (t<titles.length) {
			JMenu nextMenu = new JMenu(titles[t++]);
			menu.add(nextMenu);
			menu = nextMenu;
		}

		// add actions to menu
		for (Action action: actions) {
			if (action==null)
				menu.addSeparator();
			else
				menu.add(action);
		}
		if (newMenu != null)
			insertMenu(menu);
	}


	public RightClickMenu getRightClickMenu() {
		return rightClickMenu;
	}

	public void addToolbar(String title, JToolBar toolbar, Action action) {
		toolbars.put(title, toolbar);
		toolBarBox.add(toolbar, BorderLayout.NORTH);
		this.insertMenu("View|Toolbars", action);
	}

	public void setVisibleToolbar(String title, boolean visible) {
		JComponent tb = toolbars.get(title);
		if (tb != null) {
			tb.setVisible(visible);
			toolBarBox.doLayout();
		}
		
	}


	static class MyWindowListener implements WindowListener {
		UI ui;
		
		MyWindowListener(UI ui) {
			this.ui = ui;
		}

		public void windowClosing(WindowEvent e) {
			ui.exit(0);
		}

		public void windowActivated(WindowEvent e) {}
		public void windowClosed(WindowEvent e) {}
		public void windowDeactivated(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowOpened(WindowEvent e) {}
	}
}
