package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.View;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.Io;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkCatalog;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkCatalogListener;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.bookmarks.ListBookmarkDataSource;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.util.FileUtils;


/**
 * Holds a tabbed pane in which each tab holds a set of bookmarks.
 * @author cbare
 */
@SuppressWarnings("serial")
public class BookmarksPanel extends JPanel implements BookmarkCatalogListener, ChangeListener {
	private static final Logger log = Logger.getLogger(BookmarksPanel.class);
	private BookmarkCatalog catalog;
	UI ui;
	JTabbedPane tabs;
	final static int HACKED_FIXED_WIDTH = 240;

	BookmarksPanel(BookmarkCatalog catalog, UI ui) {
		this.catalog = catalog;
		this.ui = ui;

		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.BLACK));
		tabs = new JTabbedPane();
		add(createButtonPanel(), BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
		for (BookmarkDataSource dataSource : catalog) {
			tabs.add(dataSource.getName(), new JScrollPane(new BookmarkList(dataSource, ui, this)));
		}
		
		catalog.addBookmarkCatalogListener(this);
		tabs.addChangeListener(this);
	}

	private Component createButtonPanel() {
		JToolBar panel = new JToolBar() {
			@Override
			protected void paintComponent(Graphics g) {
				Color c = g.getColor();
				int f = 0xE0;
				for (int y=0; y < this.getHeight(); y++) {
					g.setColor(new Color(f,f,f));
					f-=2;
					g.drawLine(0, y, this.getWidth(), y);
				}
				g.setColor(c);
			}
		};
		panel.setFloatable(false);
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0.0;
		c.gridx = 0;

		JButton addButton = createIconButton("add.png", "Add", "Add a new bookmark");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				add();
			}
		});
		panel.add(addButton, c);

		c.gridx++;

		JButton editButton = createIconButton("edit.png", "Edit", "Edit the selected bookmark");
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edit();
			}
		});
		panel.add(editButton, c);

		c.gridx++;

		JButton deleteButton = createIconButton("delete_edit.gif", "Delete", "Delete the selected bookmark");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});
		panel.add(deleteButton, c);

		c.gridx++;
		c.insets.left += 6;

		JButton loadBookmarksButton = createIconButton("folder_open.png", "Load Bookmark Set", "Load a collection of bookmarks");
		loadBookmarksButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadBookmarks();
			}
		});
		panel.add(loadBookmarksButton, c);

		c.gridx++;
		c.insets.left -= 6;

		JButton saveBookmarksButton = createIconButton("save.gif", "Save Bookmark Set", "Save a collection of bookmarks");
		saveBookmarksButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveBookmarks();
			}
		});
		panel.add(saveBookmarksButton, c);

		c.gridx++;
		
		JButton newSetButton = createIconButton("add_folder.png", "New Bookmark Set", "Create a new collection of bookmarks");
		newSetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newSet();
			}
		});
		panel.add(newSetButton, c);

		c.gridx++;

		JButton removeSetButton = createIconButton("delete_folder.png", "Remove Bookmark Set", "Remove the current collection of bookmarks");
		removeSetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSet();
			}
		});
		panel.add(removeSetButton, c);

		c.gridx++;
		
		// new blastnButton (dmartinez)
		JButton blastnButton = createIconButton("helix.png", "Blastn", "Blastn the selected sequence");
		blastnButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				blastn();
			}
		});
		panel.add(blastnButton, c);

		c.gridx++;
		c.insets.left += 40;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		final JButton closeButton = new JButton(new ImageIcon(BookmarksPanel.class.getResource("/icons/x.png")));
		closeButton.setBorder(BorderFactory.createEmptyBorder());
		closeButton.setOpaque(false);
		// this doesn't seem to work in a JToolBar.
		closeButton.setRolloverIcon(new ImageIcon(BookmarksPanel.class.getResource("/icons/x_highlighted.png")));
		closeButton.setRolloverEnabled(true);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeBookmarksPanel();
			}
		});

		panel.add(closeButton, c);

		return panel;
	}

	private JButton createIconButton(String iconFilename, String defaultText, String tooltip) {
		JButton button;
		try {
			ImageIcon icon = FileUtils.getIcon(iconFilename);
			button = new JButton(icon);
		} catch (IOException e) {
			log.error(e);
			button = new JButton(defaultText);
		}
		button.setToolTipText(tooltip);
		return button;
	}

	public void removeBookmarkDataSource(BookmarkDataSource dataSource) {
		for (int i=0; i<tabs.getComponentCount(); i++) {
			BookmarkList list = getInnerComponent(tabs.getComponent(i));
			if (dataSource == list.dataSource) {
				tabs.remove(i);
			}
		}
	}

	public void addBookmarkDataSource(BookmarkDataSource dataSource) {
		tabs.add(dataSource.getName(), new JScrollPane(new BookmarkList(dataSource, ui, this)));
		tabs.setSelectedIndex(tabs.getComponentCount()-1);
	}

	public void renameBookmarkDataSource(BookmarkDataSource dataSource) {
		for (int i=0; i<tabs.getComponentCount(); i++) {
			BookmarkList list = getInnerComponent(tabs.getComponent(i));
			if (dataSource == list.dataSource) {
				tabs.setTitleAt(i, dataSource.getName());
			}
		}
	}

	public void updateBookmarkCatalog() {
		tabs.removeAll();
		for (BookmarkDataSource dataSource : catalog) {
			tabs.add(dataSource.getName(), new JScrollPane(new BookmarkList(dataSource, ui, this)));
		}
	}

	/**
	 * get the component in the selected tab
	 */
	private BookmarkList getSelectedComponent() {
		return getInnerComponent(tabs.getSelectedComponent());
	}

	private BookmarkList getInnerComponent(Component component) {
		if (component instanceof JScrollPane) {
			return (BookmarkList) ((JScrollPane)component).getViewport().getView();
		}
		else {
			return (BookmarkList) component;
		}
	}
	
	public void add() {
		BookmarkList list = getSelectedComponent();
		if (list != null)
			list.add();
	}

	public void edit() {
		BookmarkList list = getSelectedComponent();
		if (list != null)
			list.edit();
	}
	
	// open configured blastn website w/ QUERY=sequence(dmartinez)
	public void blastn() {
		BookmarkList list = getSelectedComponent();
		if (list != null)
			list.blastn();
	}

	public void delete() {
		BookmarkList list = getSelectedComponent();
		if (list != null)
			list.delete();
	}

	public void copySelectedBookmarksTo(BookmarkDataSource targetDataSource) {
		BookmarkList list = getSelectedComponent();
		int[] rows = list.getSelectedIndices();
		for (int r: rows) {
			targetDataSource.add(list.dataSource.getElementAt(r));
		}
	}

	public void moveSelectedBookmarksTo(BookmarkDataSource targetDataSource) {
		BookmarkList list = getSelectedComponent();
		int[] rows = list.getSelectedIndices();
		for (int r: rows) {
			targetDataSource.add(list.dataSource.getElementAt(r));
		}
		list.delete();
	}

	public void loadBookmarks() {
		ui.loadBookmarks();
	}

	public void saveBookmarks() {
		log.info("saving bookmarks...");
		if (catalog != null && catalog.getCount() > 0) {
			ui.exportBookmarks();
		}
	}

	public void newSet() {
		String name = JOptionPane.showInputDialog(ui.mainWindow, "Name", "Enter a name for the new bookmark set", JOptionPane.QUESTION_MESSAGE);
		if (name != null && !"".equals(name)) {
			BookmarkDataSource newDataSource = new ListBookmarkDataSource(name);
			catalog.addBookmarkDataSource(newDataSource);
			selectTab(newDataSource);
		}
	}

	protected void removeSet() {
		BookmarkList list = getSelectedComponent();
		if (list != null) {
			if (list.dataSource.isDirty() && list.dataSource.getSize() > 0) {
				int result = JOptionPane.showConfirmDialog(ui.mainWindow, "Bookmark set " + list.getName() + " has unsaved changes. Proceed anyway?", "Confirm Remove Bookmark Set", JOptionPane.OK_CANCEL_OPTION);
				if (result != JOptionPane.OK_OPTION) {
					return;
				}
			}
			catalog.removeBookmarkDataSource(list.dataSource);
			// TODO also remove from DB?
		}
	}

	public void closeBookmarksPanel() {
		this.setVisible(false);
		firestatusChangedEvent(PanelStatusListener.Status.Closed);
	}

	public void openBookmarksPanel() {
		this.setVisible(true);
		firestatusChangedEvent(PanelStatusListener.Status.Open);
	}

	public boolean toggleBookmarksPanel() {
		if (this.isVisible()) {
			this.setVisible(false);
			firestatusChangedEvent(PanelStatusListener.Status.Closed);
			return false;
		}
		else {
			this.setVisible(true);
			firestatusChangedEvent(PanelStatusListener.Status.Open);
			return true;
		}
	}

	/**
	 * get notification when tab selections changes
	 */
	public void stateChanged(ChangeEvent e) {
		BookmarkList list = getSelectedComponent();
		if (list != null)
			catalog.setSelected(list.dataSource);
	}

	public void selectTab(BookmarkDataSource dataSource) {
		for (int i=0; i<tabs.getComponentCount(); i++) {
			BookmarkList list = getInnerComponent(tabs.getComponent(i));
			if (dataSource == list.dataSource) {
				tabs.setSelectedIndex(i);
			}
		}
	}

	public void setCurrentTabTitle(String title) {
		int i = tabs.getSelectedIndex();
		tabs.setTitleAt(i, title);
	}

	// listeners can be notified when the panel opens or closes
	Set<PanelStatusListener> listeners = new HashSet<PanelStatusListener>();

	public void addPanelStatusListener(PanelStatusListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removePanelStatusListener(PanelStatusListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void firestatusChangedEvent(PanelStatusListener.Status status) {
		synchronized (listeners) {
			for (PanelStatusListener listener : listeners) {
				listener.statusChanged(status);
			}
		}
	}
	
	static class BookmarkList extends JList implements ListSelectionListener {
		BookmarkDataSource dataSource;
		UI ui;
		BookmarksPanel panel;

		public BookmarkList(BookmarkDataSource dataSource, UI ui, BookmarksPanel panel) {
			super(dataSource);
			this.dataSource = dataSource;
			this.ui = ui;
			this.panel = panel;
			setCellRenderer(new BookmarkListCellRenderer(Color.WHITE, new Color(0xe0eeee)));
			addListSelectionListener(this);
			setSize(new Dimension(HACKED_FIXED_WIDTH, Integer.MAX_VALUE));
			addMouseListener(new BookmarkPanelMouseListener(ui.app, this, panel));
		}

		public Bookmark getSelectedBookmark() {
			int row = getSelectedIndex();
			if (row > -1 && row < dataSource.getSize()) {
				return dataSource.getElementAt(row);
			}
			else {
				return null;
			}
		}

		public List<Bookmark> getSelectedBookmarks() {
			int[] rows = getSelectedIndices();
			List<Bookmark> bookmarks = new ArrayList<Bookmark>(rows.length);
			for (int row : rows) {
				if (row > -1 && row < dataSource.getSize()) {
					bookmarks.add(dataSource.getElementAt(row));
				}
			}
			return bookmarks;
		}

		@Override
		public String getName() {
			// extra protection here due to:
			// Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
		    //   at org.systemsbiology.genomebrowser.ui.BookmarksPanel$BookmarkList.getName(BookmarksPanel.java:405)
		    //   at com.sun.java.swing.plaf.gtk.GTKStyleFactory.getMatchingStyles(Unknown Source)
			try {
				if (dataSource==null) return "Bookmarks";
				return dataSource.getName();
			}
			catch (Exception e) {
				return super.getName();
			}
		}

		public void edit() {
			Bookmark bookmark = getSelectedBookmark();
			if (bookmark != null) {
				ui.showBookmarksDialog(bookmark);
			}
		}
		
		public void add() {
			ui.bookmarkCurrentSelection(false);
		}
		
		public void delete() {
			int[] rows = getSelectedIndices();
			clearSelection();
			// remove the highest indices first;
			for (int i=rows.length-1; i>=0; i--) {
				dataSource.remove(rows[i]);
			}
		}
		// list.blastn (dmartinez)
		public void blastn() {
			int[] rows = getSelectedIndices();
			//List<Bookmark> bookmarks = new ArrayList<Bookmark>(rows.length);
			for (int row : rows) {
				if (row > -1 && row < dataSource.getSize()) {
					try {
              Desktop.getDesktop().browse(new java.net.URI(
                                                           "http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE=Nucleotides&DATABASE=nr&BLAST_PROGRAMS=discoMegablast&QUERY="+dataSource.getElementAt(row).getSequence()));
					} catch (Exception e) {
						ui.showErrorMessage("Can't open browser: " + e.getMessage());
					}
					//System.out.println("objeto bk = " + dataSource.getElementAt(row).getClass());
				}
		}
		}

		// There's a slight problem here if we click on the already selected
		// list item expecting it to zoom to that region of the genome. Since
		// the value hasn't changed, we don't get this event. That's why we have
		// a mouse listener as well. ValueChanged also handles events generated
		// by keyboard input.
		public void valueChanged(ListSelectionEvent e) {
			// TODO handle multiple selections
			// ListSelectionEvent reports the first and last row whose
			// select may have changed.
			if (!e.getValueIsAdjusting()) {
				ui.gotoAndSelectFeatures(getSelectedBookmarks());
			}
		}

		// called by mouse listener
		public void select(int row) {
			Feature bookmark = dataSource.getElementAt(row);
			ui.gotoAndSelectFeature(bookmark);
		}
	}

	/**
	 * Renders list cells with a title and an annotation.
	 * 
	 * This renderer is the closest I could get to having the cells wrap
	 * the text in the desc JTextArea and then size the component appropriately.
	 * It doesn't really work, but can fake it reasonably enough with the hack
	 * of setting the width of the list in advance to a value close to
	 * what its final width will be (based on the width of the titles).
	 */
	static class BookmarkListCellRenderer extends JPanel implements ListCellRenderer {
		private Color even, odd;
		private JLabel title;
		private JTextArea desc;

		public BookmarkListCellRenderer(Color even, Color odd) {
			this.even = even;
			this.odd  = odd;

			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
			setOpaque(true);

			title = new JLabel();
			title.setFont(new Font("Arial", Font.ITALIC | Font.BOLD, 11));
			add(title, BorderLayout.NORTH);

			desc = new JTextArea();
			desc.setFont(new Font("Arial", Font.PLAIN, 9));
			desc.setOpaque(false);
			desc.setWrapStyleWord(true);
			desc.setLineWrap(true);
			add(desc, BorderLayout.CENTER);
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			// do a bunch of scary math to figure out how wide the list cells should be.
			Insets insets = desc.getInsets();
            int rendererLeftRightInsets = insets.left + insets.right + 8;  // 8 from panel border
            int topDownInsets = insets.top + insets.bottom;
 
            int listWidth = list.getWidth();
            if (listWidth<=0) listWidth = HACKED_FIXED_WIDTH;
            int viewWidth = listWidth;
			int scrollPaneLeftRightInsets = 0;
			JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass( JScrollPane.class, list );
			if ( scroll != null && scroll.getViewport().getView() == list ) {
				Insets scrollPaneInsets = scroll.getBorder().getBorderInsets(scroll);
	            scrollPaneLeftRightInsets = scrollPaneInsets.left + scrollPaneInsets.right;
	            listWidth = scroll.getWidth();
	            if (listWidth<=0) listWidth = HACKED_FIXED_WIDTH;
	            listWidth -= scrollPaneLeftRightInsets;
	            JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
	            if (verticalScrollBar.isShowing()) {
	                listWidth -= verticalScrollBar.getWidth();
	            }
	            viewWidth = listWidth - rendererLeftRightInsets;
			}
			
			Bookmark bookmark = (Bookmark)value;

			// TODO could we limit the size of the title label
			// so that it is never bigger than its enclosing list?
			
			// TODO fix this
//			String name = (bookmark instanceof GeneFeature) ? ((GeneFeature)bookmark).getFullName() : bookmark.getName();
			
			title.setText("["+bookmark.getSeqId().toString() +"] - " +
					bookmark.getLabel() + " - ["+ bookmark.getStrand().toString().substring(0, 3)+"]");
			if (bookmark instanceof Bookmark) {
				if (bookmark.getSequence().toString().length() > 20){
					desc.setText((bookmark).getSequence().toString().substring(0, 20)+ "..."+"\n"+ bookmark.getAnnotation().toString());	
				} else {
					desc.setText((bookmark).getSequence().toString()+ "\n"+ bookmark.getAnnotation().toString());
				}
				
				desc.setVisible(true);

				View rootView = desc.getUI().getRootView(desc);
	            rootView.setSize( viewWidth, Float.MAX_VALUE );
	            float yAxisSpan = rootView.getPreferredSpan(View.Y_AXIS); 
				desc.setPreferredSize(new Dimension(viewWidth, (int)yAxisSpan + topDownInsets));
			}
			else {
				desc.setVisible(false);
			}

			title.setPreferredSize( new Dimension( viewWidth, title.getPreferredSize().height ) );
			title.setSize( new Dimension( viewWidth, title.getPreferredSize().height ) );

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				if (index % 2 == 0)
					setBackground(even);
				else
					setBackground(odd);
				setForeground(list.getForeground());
			}

			return this;
		}
	}



	/**
	 * Respond to mouse clicks and double clicks on a bookmark list.
	 */
	static class BookmarkPanelMouseListener implements MouseListener {
		BookmarksPanel panel;
		BookmarkList list;
		Application app;
		
		public BookmarkPanelMouseListener(Application app, BookmarkList list, BookmarksPanel panel) {
			this.list = list;
			this.panel = panel;
			this.app = app;
		}

		public void mouseClicked(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				if (event.getClickCount()==1) {
					int row = list.locationToIndex(event.getPoint());
					// the valueChanged(ListSelectionEvent) takes care of cases
					// where the selections changes. This is here to handle the
					// case where the user clicks on the already-selected cell.
					if (row != -1 && list.getSelectedIndices().length==1 && list.getSelectedIndex() == row) {
						// the check above for to see if the clicked on row is already selected is
						// supposed to differentiate clicks that will generate a valueChangedEvent from
						// those that won't. It seems to fail to do that. We apparently can't rely on the
						// mouse event arriving here before it's processed by the list itself.
						// TODO avoid duplicate bookmark select events.
						list.select(row);
					}
				}
				else if (event.getClickCount()==2) {
					list.edit();
				}
			}
		}

		public void mousePressed(MouseEvent event) {
			maybeShowPopup(event);
		}

		public void mouseReleased(MouseEvent event) {
			maybeShowPopup(event);
		}

		// right click menu
		private void maybeShowPopup(MouseEvent event) {
			if (event.isPopupTrigger()) {
				JPopupMenu popup = new JPopupMenu("Bookmarks Menu");
				popup.add(new JMenuItem(panel.ui.actions.newBookmarkSetAction));

				JMenuItem renameMenu = new JMenuItem(new RenameBookmarkCollectionAction(panel, app));
				popup.add(renameMenu);
				if (panel.catalog.getCount() < 1) {
					renameMenu.setEnabled(false);
				}

				JMenu copyToMenu = new JMenu("Copy to");
				JMenu moveToMenu = new JMenu("Move to");
				for (BookmarkDataSource dataSource : panel.catalog) {
					if (dataSource != list.dataSource) {
						copyToMenu.add(new JMenuItem(new CopyMenuAction(panel, dataSource)));
						moveToMenu.add(new JMenuItem(new MoveMenuAction(panel, dataSource)));
					}
				}
				popup.add(copyToMenu);
				popup.add(moveToMenu);
				if (panel.catalog.getCount() <= 1 || list.getSelectedIndices().length==0) {
					copyToMenu.setEnabled(false);
					moveToMenu.setEnabled(false);
				}
				JMenuItem editBookmarkMenuItem = new JMenuItem("Edit");
				editBookmarkMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						list.edit();
					}
				});
				if (list.dataSource.getSize() < 1 || list.getSelectedIndices().length==0)
					editBookmarkMenuItem.setEnabled(false);
				popup.add(editBookmarkMenuItem);
				JMenuItem deleteMenuItem = new JMenuItem("Delete");
				deleteMenuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						list.delete();
					}
				});
				if (list.dataSource.getSize() < 1 || list.getSelectedIndices().length==0)
					deleteMenuItem.setEnabled(false);
				popup.add(deleteMenuItem);

				popup.add(new JMenuItem(new SortAction(panel.catalog.getSelected())));

				JMenu dbMenu = new JMenu("Dataset");
				dbMenu.setToolTipText("Access bookmarks in the dataset hbgb file");
				JMenuItem storeInDatasetMenuItem = new JMenuItem(new StoreInDatasetAction(panel.catalog.getSelected(), app));
				storeInDatasetMenuItem.setToolTipText("Store the current collection of bookmarks in the dataset");
				dbMenu.add(storeInDatasetMenuItem);
				JMenuItem deleteFromDatasetMenuItem = new JMenuItem(new DeleteFromDatasetAction(panel.catalog.getSelected(), app));
				storeInDatasetMenuItem.setToolTipText("Delete the current collection of bookmarks in the dataset");
				dbMenu.add(deleteFromDatasetMenuItem);
				popup.add(dbMenu);

				popup.add(panel.ui.actions.toggleBookmarkPanelAction);
				popup.show(event.getComponent(), event.getX(), event.getY());
			}
		}

		public void mouseEntered(MouseEvent event) {}
		public void mouseExited(MouseEvent event) {}
	}

	static class SortAction extends AbstractAction {
		BookmarkDataSource dataSource;
		
		public SortAction(BookmarkDataSource dataSource) {
			super("Sort");
			this.dataSource = dataSource;
			putValue(Action.SHORT_DESCRIPTION, "Sort bookmarks by position in the genome.");
		}
		public void actionPerformed(ActionEvent e) {
			this.dataSource.sortByPosition();
		}
	}

	static class CopyMenuAction extends AbstractAction {
		BookmarksPanel bookmarkPanel;
		BookmarkDataSource dataSource;
		
		public CopyMenuAction(BookmarksPanel bookmarkPanel, BookmarkDataSource dataSource) {
			super(dataSource.getName());
			this.bookmarkPanel = bookmarkPanel;
			this.dataSource = dataSource;
		}

		public void actionPerformed(ActionEvent e) {
			bookmarkPanel.copySelectedBookmarksTo(dataSource);
		}
	}

	static class MoveMenuAction extends AbstractAction {
		BookmarksPanel bookmarkPanel;
		BookmarkDataSource dataSource;
		
		public MoveMenuAction(BookmarksPanel bookmarkPanel, BookmarkDataSource dataSource) {
			super(dataSource.getName());
			this.bookmarkPanel = bookmarkPanel;
			this.dataSource = dataSource;
		}

		public void actionPerformed(ActionEvent e) {
			bookmarkPanel.moveSelectedBookmarksTo(dataSource);
		}
	}

	static class StoreInDatasetAction extends AbstractAction {
		BookmarkDataSource dataSource;
		Application app;
		
		public StoreInDatasetAction(BookmarkDataSource dataSource, Application app) {
			super("Store in Dataset");
			this.dataSource = dataSource;
			this.app = app;
		}

		public void actionPerformed(ActionEvent e) {
			Io io = app.io;
			UUID datasetUuid = app.getDataset().getUuid();
			int count = io.countBookmarks(dataSource.getName(), datasetUuid);
			if (count==0 || app.getUi().confirm("OK to overwrite existing " + count + " bookmarks?", "Confirm?")) {
				io.writeBookmarks(dataSource, datasetUuid);
			}
		}
	}

	static class DeleteFromDatasetAction extends AbstractAction {
		BookmarkDataSource dataSource;
		Application app;

		public DeleteFromDatasetAction(BookmarkDataSource dataSource, Application app) {
			super("Delete from Dataset");
			this.dataSource = dataSource;
			this.app = app;
		}

		public void actionPerformed(ActionEvent e) {
			Io io = app.io;
			UUID datasetUuid = app.getDataset().getUuid(); 
			int count = io.countBookmarks(dataSource.getName(), datasetUuid);
			if (count==0 || app.getUi().confirm("OK to delete " + count + " bookmarks?", "Confirm delete?")) {
				io.deleteBookmarks(dataSource.getName(), datasetUuid);
			}
		}
	}

	static class RenameBookmarkCollectionAction extends AbstractAction {
		BookmarksPanel bookmarkPanel;
		Application app;

		public RenameBookmarkCollectionAction(BookmarksPanel bookmarkPanel, Application app) {
			super("Rename Bookmark Set");
			this.bookmarkPanel = bookmarkPanel;
			this.app = app;
		}

		public void actionPerformed(ActionEvent e) {
			String newName = JOptionPane.showInputDialog(app.getUi().getMainWindow(), "Name", "Enter a new name for the bookmark set", JOptionPane.QUESTION_MESSAGE);
			if (newName != null && !"".equals(newName)) {
				bookmarkPanel.catalog.getSelected().setName(newName);
				bookmarkPanel.setCurrentTabTitle(newName);
			}
		}
	}
}
