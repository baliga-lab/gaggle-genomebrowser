package org.systemsbiology.genomebrowser.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Hyperlink;


public class RightClickMenu extends JPopupMenu implements ActionListener {
	private static final Logger log = Logger.getLogger(RightClickMenu.class);
	private JMenu visualPropertiesMenu;
	private JMenu externalLinksMenu;
	private Action trackVisualPropertiesEditorAction;
	private Actions actions;


	public RightClickMenu(final Actions actions) {
		this.actions = actions;
		add(new JMenuItem(actions.selectMouseToolAction));
		add(new JMenuItem(actions.scrollerMouseToolAction));
		add(new JMenuItem(actions.crosshairsMouseToolAction));

		addSeparator();
		add(new JMenuItem(actions.trackInfoAction));
		trackVisualPropertiesEditorAction = actions.trackVisualPropertiesEditorAction;
		visualPropertiesMenu = new JMenu("Track visual properties");
		visualPropertiesMenu.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				actions.trackVisualPropertiesEditorAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "RightClickMenu: Track visual properties"));
			}
		});
		add(visualPropertiesMenu);

		addSeparator();
		add(new JMenuItem(actions.deselectAllAction));
		add(new JMenuItem(actions.zoomToSelectionAction));
		add(new JMenuItem(actions.gotoSelectionAction));

		addSeparator();
		add(new JMenuItem(actions.showInUcscGenomeBrowser));

		addSeparator();
		add(new JMenuItem(actions.addBookmarkAction));
		add(new JMenuItem(actions.addBookmarkDirectAction));
		add(new JMenuItem(actions.toggleBookmarkPanelAction));

		addSeparator();
		externalLinksMenu = new JMenu("External Links");
		add(externalLinksMenu);

//		addSeparator();
//		add(new JMenuItem(app.reloadDatasetAction));
	}

	public void setTracks(List<Track<? extends Feature>> tracks) {
		visualPropertiesMenu.removeAll();
		for (Track<? extends Feature> track : tracks) {
			visualPropertiesMenu.add(new JMenuItem(new VisualPropertiesAction(track)));
		}
	}

	public void setLinks(List<Hyperlink> links) {
		externalLinksMenu.removeAll();
		for (Hyperlink link : links) {
			externalLinksMenu.add(new JMenuItem(actions.createOpenBrowserAction(link)));
		}
	}

	public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        String s = "Action event detected."
                   + System.getProperty("line.separator")
                   + "    Event source: " + source.getText()
                   + " (an instance of " + source.getClass().getName() + ")";
        log.info(s);
	}

	class VisualPropertiesAction extends AbstractAction {
		String name;
		UUID uuid;
		public VisualPropertiesAction(Track<? extends Feature> track) {
			super(track.getName());
			name = track.getName();
			uuid = track.getUuid();
		}
		public void actionPerformed(ActionEvent e) {
			trackVisualPropertiesEditorAction.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "uuid=" + uuid.toString()));
		}
	}
}
