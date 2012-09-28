package org.systemsbiology.genomebrowser.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboPopup;

import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.ViewParameters.ViewParametersListener;

// TODO update sequence menu when sequence changes

/**
 * The panel on the left hand side of the window. Holds buttons that select
 * the cursor tool, a slider for zooming in and out, and a button that opens
 * the chromosome menu. 
 * 
 * @author cbare
 */
public class SideBar extends JPanel implements ActionListener, ViewParametersListener {

	private ViewParameters params;
	private UI ui;

	private JSlider zoomSlider;
//	private List<ZoomListener> zoomListeners = new ArrayList<ZoomListener>();
	private ButtonGroup buttonGroup;
	private boolean suppressZoomEvents;
	private JPopupMenu sequenceMenu;

	private Map<CursorTool, JRadioButton> cursorToolButtons = new EnumMap<CursorTool, JRadioButton>(CursorTool.class);

	private JButton sequenceMenuButton;
	private JComboBox sequenceComboBox;


	public SideBar(UI ui) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.ui = ui;
		this.params = ui.getViewParameters();
		this.sequenceComboBox = new JComboBox();

		buttonGroup = new ButtonGroup();
		add(createRadioButton(CursorTool.select, "/icons/cursor.png", false));
		add(createRadioButton(CursorTool.hand, "/icons/hand.png", true));
		add(createRadioButton(CursorTool.crosshairs, "/icons/crosshairs.png", false));

		zoomSlider = new JSlider(0,1000);
		zoomSlider.setOrientation(JSlider.VERTICAL);
		zoomSlider.setAlignmentX(0.5f);
		zoomSlider.setToolTipText("Zoom");

		// remove these key mappings assigned by the UI peer so they don't
		// interfere with using left and right arrows for scrolling. Up and
		// down arrows remain mapped and additionally more keys are mapped
		// to control zoom in MainWindow.
		List<KeyStroke> remove = Arrays.asList(new KeyStroke[] {
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false),
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false),
				KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0, false),
				KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0, false)
		});
		InputMap im = zoomSlider.getInputMap();
		while (im!=null) {
			KeyStroke[] ks = im.keys();
			if (ks!=null) {
				for (KeyStroke k : ks) {
					if (remove.contains(k))
						im.remove(k);
				}
			}
			im = im.getParent();
		}
/*
   		Key mappings from Java5/Mac OS X. Other plafs likely vary.

		ctrl pressed F1
		pressed ESCAPE
		pressed PAGE_UP
		pressed PAGE_DOWN
		pressed END
		pressed HOME
		pressed LEFT
		pressed KP_UP
		pressed KP_DOWN
		pressed UP
		pressed RIGHT
		pressed KP_LEFT
		pressed DOWN
		pressed KP_RIGHT
*/

		zoomSlider.setValue(params.inverseTweak((params.getEnd() - params.getStart()) / ((double)params.getSequenceLength())));
		zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateZoom(zoomSlider.getValue());
			}
		});
		add(zoomSlider);

		sequenceMenuButton = new JButton(new ImageIcon(SideBar.class.getResource("/icons/chromosome.png")));
		sequenceMenuButton.setToolTipText("Select replicon, chromosome, or sequence");
		sequenceMenuButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sequenceMenu.show(sequenceMenuButton, sequenceMenuButton.getX(), sequenceMenu.getY());
			}
		});
		sequenceMenuButton.setAlignmentX(0.5f);
		sequenceMenuButton.setBorder(BorderFactory.createEmptyBorder());
		add(sequenceMenuButton);
		setSequences(null);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	}

	private JPopupMenu createSequenceMenu(List<Sequence> sequences) {
		sequenceComboBox.removeAllItems();
		if (sequences != null) {
			for (Sequence seq : sequences) {
				sequenceComboBox.addItem(seq.getSeqId());
			}
		}
		sequenceComboBox.addActionListener(new SequenceMenuActionListener());

		BasicComboPopup popupMenu = new BasicComboPopup(sequenceComboBox);
		return popupMenu;
	}

	/**
	 * build the sequence menu
	 */
	public void setSequences(List<Sequence> list) {
		sequenceMenu = createSequenceMenu(list);
		
		// this magic juju seems to be required for the menu button
		// to redraw itself properly. Dunno exactly why.
		sequenceMenu.revalidate();
	}

	public void setSelectedSequence(String sequenceName) {
		sequenceComboBox.setSelectedItem(sequenceName);
	}

	public void setSelectedSequence(Sequence sequence) {
		if (sequence != null)
			setSelectedSequence(sequence.getSeqId());
		else
			setSelectedSequence((String)null);
	}

	private JRadioButton createRadioButton(CursorTool cursor, String path, boolean selected) {
		JRadioButton button = new JRadioButton(new ImageIcon(SideBar.class.getResource(path)), selected) {
			/**
			 * hack the radio buttons to give the selected button a dark background.
			 */
			public Color getBackground() {
				if (model.isSelected())
					return Color.DARK_GRAY;
				else
					return super.getBackground();
			}
		};
		buttonGroup.add(button);
		button.setAlignmentX(0.5f);
		button.addActionListener(this);
		button.setActionCommand(cursor.toString());
		button.setToolTipText("Select cursor tool: " + cursor);
		button.setOpaque(true);
		cursorToolButtons.put(cursor, button);
		return button;
	}

	public void setCursorTool(CursorTool cursor) {
		JRadioButton button = cursorToolButtons.get(cursor);
		button.setSelected(true);
	}

//	private JButton createImageButton(String path) {
//		JButton button = new JButton(new ImageIcon(SideBar.class.getResource(path)));
//		button.setAlignmentX(0.5f);
//		return button;
//	}

	// handle events from tool selections buttons
	public void actionPerformed(ActionEvent e) {
		ui.setCursorTool(CursorTool.valueOf(e.getActionCommand()));
	}


/*
	public void fireZoomEvent(double zoom) {
		synchronized (zoomListeners) {
			// suppressZoomEvents is necessary to avoid a cycle of
			// events when the window is resized.
			if (!suppressZoomEvents) {
				for (ZoomListener listener : zoomListeners) {
					listener.setZoom(zoom);
				}
			}
		}
	}

	public void addZoomListener(ZoomListener listener) {
		synchronized (zoomListeners) {
			zoomListeners.add(listener);
		}
	}

	public void removeZoomListener(ZoomListener listener) {
		synchronized (zoomListeners) {
			zoomListeners.remove(listener);
		}
	}
*/

	private void updateZoom(int sliderValue) {
		// suppressZoomEvents is necessary to avoid a cycle of
		// events when the window is resized.
		if (!suppressZoomEvents) {
			params.setZoom(params.tweak(zoomSlider.getValue()));
		}
	}

	public void viewParametersChanged(ViewParameters p) {
		try {
			suppressZoomEvents = true;
			zoomSlider.setValue(params.inverseTweak( ((double)p.getWidth()) / ((double)p.getSequenceLength()) ));
		}
		finally {
			suppressZoomEvents = false;
		}
	}


	class SequenceMenuActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JComboBox comboBox = (JComboBox)e.getSource();
			ui.setSelectedSequence((String)comboBox.getSelectedItem(), true);
			sequenceMenu.setVisible(false);
		}
	}
}
