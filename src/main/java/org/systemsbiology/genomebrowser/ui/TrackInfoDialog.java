package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.text.View;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;


/**
 * Right-click popup that shows information about tracks under the point
 * where the mouse was clicked.
 */
@SuppressWarnings("serial")
public class TrackInfoDialog extends JDialog {
	final static int HACKED_FIXED_WIDTH = 240;

	public TrackInfoDialog(JFrame owner, List<Track<? extends Feature>> tracks) {
		super(owner, "Track Information");

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dismiss();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		if (tracks==null || tracks.size() == 0) {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 20, 60));
			panel.add(new JLabel("No tracks here..."), BorderLayout.CENTER);
			add(panel);
		}
		else {
			add(new TrackInfoList(tracks), BorderLayout.CENTER);
		}

		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton(new OkAction());
		okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(okButton);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
	}
	
	private void dismiss() {
		setVisible(false);
		dispose();
	}

	class OkAction extends AbstractAction {
		public OkAction() {
			super("OK");
		}

		public void actionPerformed(ActionEvent e) {
			dismiss();
		}
	}

	static class TrackInfoList extends JList {
		List<Track<? extends Feature>> tracks;

		public TrackInfoList(List<Track<? extends Feature>> tracks) {
			super(new Vector<Track<? extends Feature>>(tracks));
			this.tracks = tracks;
			setCellRenderer(new TrackInfoListCellRenderer(Color.WHITE, new Color(0xe0eeee)));
			setSize(new Dimension(HACKED_FIXED_WIDTH, Integer.MAX_VALUE));
		}
	}


	/**
	 * Renders list cells with a title and an annotation.
	 * 
	 * This renderer is the closest I could get to having the cells wrap the
	 * text in the desc JTextArea and then size the component appropriately. It
	 * doesn't really work, but can fake it reasonably enough with the hack of
	 * setting the width of the list in advance to a value close to what its
	 * final width will be (based on the width of the titles).
	 */
	static class TrackInfoListCellRenderer extends JPanel implements ListCellRenderer {
		private Color even, odd;
		private JLabel title;
		private JTextArea desc;

		public TrackInfoListCellRenderer(Color even, Color odd) {
			this.even = even;
			this.odd = odd;

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

		@SuppressWarnings("unchecked")
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			// do a bunch of scary math to figure out how wide the list cells
			// should be.
			Insets insets = desc.getInsets();
			int rendererLeftRightInsets = insets.left + insets.right + 8;
			int topDownInsets = insets.top + insets.bottom;

			int listWidth = list.getWidth();
			if (listWidth <= 0)
				listWidth = HACKED_FIXED_WIDTH;
			int viewWidth = listWidth;
			int scrollPaneLeftRightInsets = 0;
			JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
			if (scroll != null && scroll.getViewport().getView() == list) {
				Insets scrollPaneInsets = scroll.getBorder().getBorderInsets(scroll);
				scrollPaneLeftRightInsets = scrollPaneInsets.left + scrollPaneInsets.right;
				listWidth = scroll.getWidth();
				if (listWidth <= 0)
					listWidth = HACKED_FIXED_WIDTH;
				listWidth -= scrollPaneLeftRightInsets;
				JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
				if (verticalScrollBar.isShowing()) {
					listWidth -= verticalScrollBar.getWidth();
				}
				viewWidth = listWidth - rendererLeftRightInsets;
			}

			Track<? extends Feature> track = (Track<? extends Feature>) value;

			title.setText(track.getName());

			// should this be a method getTrackInfo()
			StringBuilder sb = new StringBuilder();
			Iterator<String> iterator = track.getAttributes().keySet().iterator();
			String key;
			if (iterator.hasNext()) {
				key = iterator.next();
				sb.append(key).append(" = ").append(track.getAttributes().get(key));
			}
			while (iterator.hasNext()) {
				key=iterator.next();
				sb.append("\n").append(key).append(" = ").append(track.getAttributes().get(key));
			}
			desc.setText(sb.toString());

			View rootView = desc.getUI().getRootView(desc);
			rootView.setSize(viewWidth, Float.MAX_VALUE);
			float yAxisSpan = rootView.getPreferredSpan(View.Y_AXIS);
			desc.setPreferredSize(new Dimension(viewWidth, (int) yAxisSpan + topDownInsets));

			title.setPreferredSize(new Dimension(viewWidth, title.getPreferredSize().height));
			title.setSize(new Dimension(viewWidth, title.getPreferredSize().height));

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

}
