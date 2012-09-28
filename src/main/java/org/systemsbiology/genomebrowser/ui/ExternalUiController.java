package org.systemsbiology.genomebrowser.ui;

import java.util.UUID;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Event;
import org.systemsbiology.genomebrowser.app.EventListener;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.app.ProgressReporter;
import org.systemsbiology.genomebrowser.app.UiController;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;


/**
 * Allow external threads to safely access UI functionality. All activity in
 * the UI package should take place on the swing event queue. This class helps
 * external classes running in non-swing threads to easily and safely access
 * the UI by wrapping all calls in InvokeLater.
 * 
 * @author cbare
 */
public class ExternalUiController implements UiController, EventListener {
	private static final Logger log = Logger.getLogger(ExternalUiController.class);
	public UI ui;


	public ExternalUiController(UI ui) {
		this.ui = ui;
	}

	public void startup(Options options) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
//				// TODO this needs a lock?
//				log.info("startup");
//				Dataset dataset = ui.app.getDataset();
//				if (dataset!=null) {
//					ui.setDataset(dataset);
//				}
			}
		});
	}

	public void setDataset(final Dataset dataset) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.setDataset(dataset);
			}
		});
	}

	public void sequenceSelected(final Sequence sequence) {
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				ui.sequenceSelected(sequence);
//			}
//		});
	}

	public void setViewArea(final Segment segment) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.setViewSegment(segment);
			}
		});
	}

	public void centerOnSegment(final Segment segment) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.centerOnSegment(segment);
			}
		});
	}

	public void showErrorMessage(String message, Throwable t) {
		ui.showErrorMessage(message, t);
	}

	public void showErrorMessage(String message) {
		ui.showErrorMessage(message);
	}

	// TODO allow plugins to have mainWindow as their parent without breaking encapsulation
	public JFrame getMainWindow() {
		return ui.mainWindow;
	}

	public void receiveEvent(Event event) {
		log.info("got event: " + event.getAction());
		if (event.getAction().equals("startup")) {
			startup((Options)event.getData());
		}
		else if (event.getAction().equals("set dataset")) {
			setDataset((Dataset)event.getData());
		}
		else if (event.getAction().equals("sequence selected")) {
			//sequenceSelected((Sequence)event.getData());
		}
		else if (event.getAction().equals("load started")) {
			showProgressPopup("Dataset loading...", (ProgressReporter)event.getData());
		}
		else if (event.getAction().equals("download started")) {
			showProgressPopup("Downloading...", (ProgressReporter)event.getData());
		}
		else if (event.getAction().equals("import genome")) {
			showProgressPopup("Importing Genome...", (ProgressReporter)event.getData());
		}
		else if (event.getAction().equals("goto")) {
			setViewArea((Segment)event.getData());
		}
		else if (event.getAction().equals("center on segment")) {
			centerOnSegment((Segment)event.getData());
		}
		else if (event.getAction().equals("error")) {
			Throwable wrapper = ((Throwable)event.getData());
			String message = wrapper.getMessage();
			Throwable t = wrapper.getCause();
			if (t==null)
				showErrorMessage(wrapper.getMessage(), wrapper);
			else
				showErrorMessage(message, t);
		}
		else if (event.getAction().equals("message")) {
			showErrorMessage((String)event.getData());
		}
		else if (event.getAction().equals("selections.changed")) {
			// TODO respond to selections.changed event in UI.
			// Right now parts of the code directly manipulate what's
			// selected in the UI. We should update the selections object
			// and use events to redraw the UI as necessary.
		}
		else if (event.getAction().equals("search-multiple-results")) {
			openBookmarksPanel();
		}
		else if (event.getAction().equals("open.bookmarks")) {
			openBookmarksPanel();
		}
		else if (event.getAction().equals("track-added")) {
			trackAdded((UUID)event.getData());
		}
		if (event.requiresRepaint())
			refresh();
	}

	// TODO handle with events (make this private?)
	public void refresh() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.refresh();
			}
		});
	}

	public void showProgressPopup(final String message, final ProgressReporter progressReporter) {
		// be careful that we can't miss the done event
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.showProgressPopup(message, progressReporter);
			}
		});
	}

	public void insertMenu(final String title, final Action[] actions) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.insertMenu(title, actions);
			}
		});
	}

	public void addToolbar(final String title, final JToolBar toolbar, final Action action) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.addToolbar(title, toolbar, action);
			}
		});
	}

	public void setVisibleToolbar(final String title, final boolean visible) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.setVisibleToolbar(title, visible);
			}
		});
	}

	public void bringToFront() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.bringToFront();
			}
		});
	}

	public void minimize() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.minimize();
			}
		});
	}

	public void openBookmarksPanel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.openBookmarksPanel();
			}
		});
	}

	public void trackAdded(final UUID trackUuid) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ui.trackAdded(trackUuid);
			}
		});
	}

	public Segment getVisibleSegment() {
		if (SwingUtilities.isEventDispatchThread()) {
			return ui.getVisibleSegment();
		}
		else {
			try {
				GetVisibleSegment g = new GetVisibleSegment(ui);
				SwingUtilities.invokeAndWait(g);
				return g.getSegment();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean confirm(String message, String title) {
		// Ug, all these hoops just to make sure ui.showConfirmDialog runs
		// on the swing event thread!
		try {
			Confirmer confirmer = new Confirmer(message, title);
			if (SwingUtilities.isEventDispatchThread()) {
				confirmer.run();
			}
			else {
				SwingUtilities.invokeAndWait(confirmer);
			}
			return confirmer.isOk();
		}
		catch (Exception e) {
			return false;
		}
	}

	private class Confirmer implements Runnable {
		private boolean ok;
		public String message;
		public String title;

		public Confirmer(String message, String title) {
			this.message = message;
			this.title = title;
		}

		public synchronized void run() {
			ok = ui.showConfirmDialog(message, title);
		}

		public synchronized boolean isOk() {
			return ok;
		}
	}

	public static class GetVisibleSegment implements Runnable {
		Segment segment;
		UI ui;
		
		public GetVisibleSegment(UI ui) {
			this.ui = ui;
		}

		// acquire lock in UI thread
		public synchronized void run() {
			segment = ui.getVisibleSegment();
		}

		// acquire lock in receiving thread
		public synchronized Segment getSegment() {
			return segment;
		}
	}
}
