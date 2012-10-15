package org.systemsbiology.genomebrowser.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Selections;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.View;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.ViewParameters.ViewParametersListener;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackManager;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.visualization.renderers.TypedGeneTrackRenderer;
import org.systemsbiology.util.Hyperlink;


/**
 * A JPanel subclass that plots data against coordinates along a stretch
 * of nucleic acid, usually position on a chromosome or plasmid.
 */
public class GenomeViewPanel extends JPanel
implements View, ViewParametersListener, WindowFocusListener {
	public static final Logger log = Logger.getLogger(GenomeViewPanel.class);
	//private TrackRendererScheduler scheduler;

	ViewParameters params;
	Point popupCoordinates;

	private UI ui;
	private TrackManager trackManager;
	Selections selections;

	Crosshairs crosshairs = new Crosshairs();
	SelectBox select = new SelectBox();

	protected Color highlight = new Color(0x406699dd, true);

	// mouse listeners for various mouse cursor tools
	private CursorToolMouseListener scrollerMouseListener = new ScrollerMouseListener();
	private CursorToolMouseListener crosshairsMouseListener = new CrosshairsMouseListener();
	private CursorToolMouseListener selectMouseListener = new SelectMouseListener();
	private CursorToolMouseListener currentCursorTool = scrollerMouseListener;

	// background image used for double buffering
	private Image img;
	private Object imgLock = new Object();
	
	private long gainedFocusAt;


	// dependencies:
	// ui
	// ViewParameters
	// -- store viewport coordinates, translate between pixels and sequence coordinates
	// TrackManager
	// -- loop through track renderers during drawing and mouse/selections operations


	// refactored (partially)
	public GenomeViewPanel(UI ui) {
		this.setBackground(Color.WHITE);
		this.ui = ui;
		this.selections = ui.app.selections;
		this.trackManager = ui.app.trackManager;
		params = ui.getViewParameters();

		// start with a scroller tool
		this.setCursorTool(CursorTool.hand);

		// need this to get this component registered with the
		// tooltip manager. see JComponent.setToolTipText(String text)
		this.setToolTipText("The Genome Browser");
	}

	public void init() {
		params.addViewParametersListener(this);
		this.addMouseListener(new GeneSelectionMouseListener());
	}

	// dependency
//	public void setTrackRendererScheduler(TrackRendererScheduler scheduler) {
//		this.scheduler = scheduler;
//	}

	public void setRightClickMenu(JPopupMenu rightClickMenu) {
		// listener for right-click menu
		this.addMouseListener(new PopupListener(rightClickMenu));
	}

	/**
	 * allows the tracks to provide tooltips on mouseover
	 */
	@Override
	public String getToolTipText(MouseEvent event) {
		for (TrackRenderer tr : trackManager) {
			if (tr instanceof HasTooltips) {
				String tip = ((HasTooltips)tr).getTooltip(event.getX(), event.getY());
				tip = WordUtils.wrap(tip, 60, "<br>", false);  //dmartinez 03-06-12 / 04-09-12 Dealing with html annotation already in db 
				if( tip!=null ){
					if(tip.toLowerCase().contains("<html>") ){return tip;}
					else{tip = "<html>" + tip + "</html>";return tip;}
				}
			}
		}
		return null;
	}


	@Override
	public void paintComponent(Graphics g) {
		
		synchronized (imgLock) {
			g.drawImage(img, 0, 0, this);
		}

//		Graphics2D g2 = (Graphics2D)g;
		Dimension size = getSize();

		//p.setSize(size.width, size.height);

		// Paint selected areas
		for (Segment segment : selections.getSegments()) {
			if (segment.seqId.equals(ui.getSelectedSequenceName()) && (segment.start < params.getEnd()) && (segment.end > params.getStart())) {
				// toScreenCoordinates();
				int x1 = params.toScreenX(segment.start);
				int x2 = params.toScreenX(segment.end);
				Color c = g.getColor();
				g.setColor(highlight);
				g.fillRect(x1, 0, Math.max(1, x2-x1), params.getDeviceHeight());
				g.setColor(c);
			}
		}
		
		// don't paint the tracks here. Schedule them to be repainted separately.
//		scheduler.schedule(trackManager, params.getSequence(), params.start, params.end);

		// TODO: make an axis track?
		int yCenter = size.height/2 - 6;

		int w = Math.min(size.width, (int)((params.getSequenceLength()-params.getStart()) * params.getScale()));

		g.setColor(Color.GRAY);
		g.drawLine(0, yCenter, w, yCenter);

		// figure out how far apart to place tick marks based on scale
		int tickInterval = (int)Math.pow(10, ((int)Math.ceil(Math.log10(size.width / params.getScale())))-1);
		if (tickInterval>20) {
			for (int tick=(params.getStart()/tickInterval+1)*tickInterval; tick<params.getEnd(); tick += tickInterval) {
				int xt = params.toScreenX(tick);
				g.drawLine(xt, yCenter-3, xt, yCenter+3);
				Font f = g.getFont();
				g.setFont(f.deriveFont(9.0F));
				g.drawString(String.valueOf(tick), xt, yCenter+16);
				g.setFont(f);
			}
		}

		// paint genome track
		TrackRenderer renderer = trackManager.getGenomeTrackRenderer();
		if (renderer != null) {
			FeatureFilter filter = new FeatureFilter(params.getSequence(), Strand.any, params.getStart(), params.getEnd());
			renderer.draw(g, renderer.getTrack().features(filter), filter.strand);
		}

		// paint crosshairs, if necessary
		if (crosshairs.visible) {
			g.setColor(Color.GRAY);
			g.drawLine(crosshairs.x, 0, crosshairs.x, size.height);
			g.drawLine(0, crosshairs.y, size.width, crosshairs.y);
		}

		// paint select box, if necessary
		if (select.visible) {
			g.setColor(Color.GRAY);
			g.drawRect(select.x, select.y, select.width, select.height);
		}
	}



	// when the component changes size, change ViewParams to reflect new size
	// setBounds is called whenever the zoom slider or horizontal scrollbar
	// are used. Not sure why, 'cause the panel hasn't moved or resized.
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		if (width>0 && height>0) {
			synchronized (imgLock) {
				if (img == null || width != img.getWidth(this) || height != img.getHeight(this)) {
					img = this.createImage(width, height);
				}
			}
			//log.info("--setBounds(" + x + ", " + y + ", " + width + ", " + height + ")");
			params.setDeviceSize(width, height);
		}
		else {
			log.warn(String.format("Negative values in call to GenomeViewPanel.setBounds(%d, %d, %d, %d).", x, y, width, height));
		}
	}

//	public Image getOffScreenImage() {
//		return img;
//	}

	public void updateImage(Image image) {
		synchronized (imgLock) {
			Graphics g = img.getGraphics();
			g.drawImage(image, 0, 0, this);
			g.dispose();
		}
		repaint();
	}

	// TODO setTrackManager never called
//	public void setTrackManager(TrackManager newTrackManager) {
//		if (trackManager != null) {
//			// remove existing tracks that are mouse listeners
//			for (TrackRenderer renderer : trackManager) {
//				if (renderer instanceof MouseMotionListener) {
//					removeMouseMotionListener((MouseMotionListener)renderer);
//				}
//				if (renderer instanceof MouseListener) {
//					removeMouseListener((MouseListener)renderer);
//				}
//			}
//		}
//
//		this.trackManager = newTrackManager;
//		for (TrackRenderer renderer : trackManager) {
//			renderer.setViewParameters(params);
//			// tracks may want to get mouse events?
//			if (renderer instanceof MouseMotionListener) {
//				addMouseMotionListener((MouseMotionListener)renderer);
//				System.out.println("MouseMotionListener: " + renderer.getClass().getName());
//			}
//			if (renderer instanceof MouseListener) {
//				addMouseListener((MouseListener)renderer);
//				System.out.println("MouseListener: " + renderer.getClass().getName());
//			}
//		}
//	}

	public void viewParametersChanged(ViewParameters p) {
		synchronized (imgLock) {
			Graphics g = img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		repaint();
	}



	private void drawCrosshairsAt(int x, int y) {
		crosshairs.x = x;
		crosshairs.y = y;
		crosshairs.visible = true;
		repaint();
	}

	private void removeCrosshairs() {
		crosshairs.visible = false;
		repaint();
	}

	private void drawSelectBox(int x, int y, int w, int h) {
		select.x = x;
		select.y = y;
		select.width = w;
		select.height = h;
		select.visible = true;
		repaint();
	}

	private void selectLassoedFeatures() {
		select.visible = false;
		for (TrackRenderer track : trackManager) {
			selections.selectFeatures(track.getContainedFeatures(ui.getSelectedSequence(), select), true);
		}
		repaint();
	}


	/**
	 * select the tool controlled by the mouse cursor
	 */
	public void setCursorTool(CursorTool tool) {

		log.info("Selected tool: " + tool);

		// just in case...
		select.visible = false;
		crosshairs.visible = false;

		// we implement the cursor tools as a mouse listener. We
		// switch cursor tools by changing which listener is
		// registered.
		this.removeMouseListener(currentCursorTool);
		this.removeMouseMotionListener(currentCursorTool);

		switch (tool) {
		case hand:
			currentCursorTool = scrollerMouseListener;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			break;
		case select:
			currentCursorTool = selectMouseListener;
			setCursor(Cursor.getDefaultCursor());
			break;
		case crosshairs:
			currentCursorTool = crosshairsMouseListener;
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		}
		
		this.addMouseMotionListener(currentCursorTool);
		this.addMouseListener(currentCursorTool);
	}


	// ---- handle selections -------------------------------------------------

	/**
	 * returns the currently selected segment in genome coordinates
	 * @return
	 */
	public Segment getSelectedCoordinates() {
		if (currentCursorTool == selectMouseListener) {
			// toGenomeCoordinate
			Segment segment = selections.getSingleSelection();
			if (segment != null)
				return segment;
			else
				return new Segment(ui.getSelectedSequenceName(), params.getStart(), params.getEnd());
		}
		else if (currentCursorTool == crosshairsMouseListener) {
			int coord = params.toGenomeCoordinate(crosshairs.x);
			return new Segment(ui.getSelectedSequenceName(), coord, coord+1);
		}
		else {
			return new Segment(ui.getSelectedSequenceName(), params.getStart(), params.getEnd());
		}
	}





	// ---- handle listeners to crosshairs tool -----------------

	private Set<CrosshairsListener> listeners = new CopyOnWriteArraySet<CrosshairsListener>();

	public void addCrosshairsListener(CrosshairsListener listener) {
		listeners.add(listener);
	}

	public void removeCrosshairsListener(CrosshairsListener listener) {
		listeners.remove(listener);
	}

	public void fireCrosshairsAt(int coord) {
		for (CrosshairsListener listener : listeners) {
			listener.crosshairsAt(coord);
		}
	}

	public void fireSelectBoxEvent(int start, int end) {
		for (CrosshairsListener listener : listeners) {
			listener.selectBoxAt(start, end);
		}
	}

	public void fireCrosshairsDone() {
		for (CrosshairsListener listener : listeners) {
			listener.crosshairsDone();
		}
	}

	// ---- mouse cursor tools ----------------------------------------

	private static class Crosshairs {
		boolean visible;
		int x, y;
	}

	private static class SelectBox extends Rectangle {
		boolean visible;
		//int x, y;
		//int width, height;
	}

	/**
	 * Mouse cursor tools each respond to mouse input in their own way
	 */
	public interface CursorToolMouseListener extends MouseListener, MouseMotionListener {}


	/**
	 * mouse control for hand scrolling tool
	 */
	public class ScrollerMouseListener implements CursorToolMouseListener {
		int startPosition;
		int start0, end0;

		public void mouseDragged(MouseEvent event) {
			// apparently, getButton doesn't work like you'd think it would on the Sun JVM. It
			// always returns zero here, regardless of whether a button is being pressed. WTF?
//			if (event.getButton()==MouseEvent.BUTTON1) {
			if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
				int x = event.getX();
				int move = (int) ( (startPosition - x) / params.getScale());
				int newStart = start0 + move;
				if (newStart < 0) {
					move -= newStart;
					newStart = 0;
				}
				int newEnd = end0 + move;
	
				if (newEnd > params.getSequenceLength()) {
					move -= newEnd - params.getSequenceLength();
					newEnd = params.getSequenceLength();
					newStart = start0 + move;
				}
				
				params.setStartAndEnd(newStart, newEnd);
	
				repaint();
			}
		}

		public void mousePressed(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				startPosition = event.getX();
				start0 = params.getStart();
				end0 = params.getEnd();
			}
		}

		public void mouseMoved(MouseEvent event) {}
		public void mouseClicked(MouseEvent event) {}
		public void mouseEntered(MouseEvent event) {}
		public void mouseExited(MouseEvent event) {}
		public void mouseReleased(MouseEvent event) {}
	}


	/**
	 * mouse control for crosshairs tool
	 */
	public class CrosshairsMouseListener implements CursorToolMouseListener {

		public void mousePressed(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				drawCrosshairsAt(event.getX(), event.getY());
				fireCrosshairsAt(params.toGenomeCoordinate(event.getX()));
			}
		}

		public void mouseReleased(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				removeCrosshairs();
				fireCrosshairsDone();
			}
		}

		public void mouseDragged(MouseEvent event) {
			if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
				drawCrosshairsAt(event.getX(), event.getY());
				fireCrosshairsAt(params.toGenomeCoordinate(event.getX()));
			}
		}

		public void mouseClicked(MouseEvent event) {}
		public void mouseEntered(MouseEvent event) {}
		public void mouseExited(MouseEvent event) {}
		public void mouseMoved(MouseEvent event) {}

	}	


	/**
	 * mouse control of select box cursor
	 */
	public class SelectMouseListener implements CursorToolMouseListener {
		int x, y;

		public void mousePressed(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				x = event.getX();
				y = event.getY();
			}
		}

		public void mouseReleased(MouseEvent event) {
			if (event.getButton()==MouseEvent.BUTTON1) {
				if (event.getX() != x || event.getY() != y) {

					Strand strand;
					int m = params.getDeviceHeight() / 2;
					boolean f1 = y < m;
					boolean f2 = event.getY() < m;
					if (f1 == f2) {
						strand = f1 ? Strand.forward : Strand.reverse;
					}
					else {
						strand = Strand.none;
					}
					
					Segment segment = new Segment(
							ui.getSelectedSequenceName(),
							params.toGenomeCoordinate(Math.max(0,select.x)),
							params.toGenomeCoordinate(Math.min(params.getDeviceWidth(), (select.x + select.width + 1))));
					// accommodate both Mac and Windows style multiple selections
					if ((event.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK))>0) {
						selections.addSegment(segment, true);
					}
					else {
						selections.replaceSelection(segment, true);
					}
					selections.setStrandHint(strand);
					selectLassoedFeatures();
					fireSelectBoxEvent(segment.start, segment.end);
				}
			}
		}

		public void mouseDragged(MouseEvent event) {
			if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
				int x2 = event.getX();
				int y2 = event.getY();
				drawSelectBox(
						Math.min(x, x2),
						Math.min(y, y2),
						Math.abs(x2 - x),
						Math.abs(y2 - y));
				fireSelectBoxEvent(params.toGenomeCoordinate(Math.max(0,select.x)), params.toGenomeCoordinate(Math.min(params.getDeviceWidth(), (select.x + select.width + 1))));
			}
		}

		public void mouseMoved(MouseEvent event) {}
		public void mouseClicked(MouseEvent event) {}
		public void mouseEntered(MouseEvent event) {}
		public void mouseExited(MouseEvent event) {}
	}


	/**
	 * handle mouse clicks on gene features, which select the features.
	 */
	class GeneSelectionMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			// ignore mouse clicks that changed focus to this app
			if (System.currentTimeMillis() - gainedFocusAt < 200)
				return;

			// TODO fix method of selecting labeled features.

			if (e.getButton() == MouseEvent.BUTTON1) {
				TypedGeneTrackRenderer renderer = (TypedGeneTrackRenderer)trackManager.getGenomeTrackRenderer();
				if (renderer != null) {
					GeneFeatureImpl clickedGene = renderer.getGeneAt(e.getX(), e.getY());
					if (clickedGene != null) {
						boolean selected = clickedGene.selected();
						log.info("clicked on gene: " + clickedGene + " selected=" + selected);
						selections.setStrandHint(clickedGene.getStrand());
						//System.out.println(clickedGene.getName());
						//System.out.println(clickedGene.getNameAndCommonName());
						//System.out.println(clickedGene.getLabel());
						//System.out.println(clickedGene.getCommonName());
						//log.info("clickedGene.getStrand() = " + clickedGene.getStrand() + "dmartinez");
						// if we're not holding down shift, ctrl or command key, deselect previously selected genes
						if ((e.getModifiersEx() & (MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) == 0) {
							selections.clear(true);
						}
						// Clicking repeatedly on a feature toggles its selection state.
						// Don't use selections.toggleFeature here 'cause we just cleared the selections above.
						if (selected)
							selections.deselectFeature(clickedGene, true);
						else {
							selections.selectFeature(clickedGene, true);
							fireSelectBoxEvent(clickedGene.getStart(), clickedGene.getEnd());
						}
					}
					else {
						// mouse tap with no modifiers clears selections
						if ((e.getModifiersEx() & (MouseEvent.ALT_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK | MouseEvent.META_DOWN_MASK)) == 0) {
							selections.clear(true);
							fireCrosshairsDone();
						}
					}
					repaint();
				}
			}
		}
	}


	/**
	 * display right-click menu
	 */
	class PopupListener extends MouseAdapter {
		JPopupMenu popup;

		PopupListener(JPopupMenu popupMenu) {
			popup = popupMenu;
		}

		public void mousePressed(MouseEvent event) {
			maybeShowPopup(event);
		}

		public void mouseReleased(MouseEvent event) {
			maybeShowPopup(event);
		}

		private void maybeShowPopup(MouseEvent event) {
			if (event.isPopupTrigger()) {
				List<Hyperlink> links = new ArrayList<Hyperlink>();
				for (TrackRenderer tr : trackManager) {
					links.addAll(tr.getLinks(event.getX(), event.getY()));
				}
				popupCoordinates = event.getPoint();
				ui.repopulateVisualPropertiesMenu(popupCoordinates);
				ui.repopulateLinksMenu(links);
				popup.show(event.getComponent(), event.getX(), event.getY());
			}
		}
	}


	// Normally, clicking on the genomeViewPanel clears any selections. But,
	// we don't want to clear selections if the intent of the click was to
	// change focus from another app to this one. So, we use gainedFocusAt
	// in the mouse listener to ignore mouse clicks that changed focus.
	public void windowGainedFocus(WindowEvent e) {
		gainedFocusAt = System.currentTimeMillis();
	}

	public void windowLostFocus(WindowEvent e) {}
}

