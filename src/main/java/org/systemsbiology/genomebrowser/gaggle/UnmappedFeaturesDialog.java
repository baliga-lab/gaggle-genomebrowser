package org.systemsbiology.genomebrowser.gaggle;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import org.systemsbiology.util.swing.SwingGadgets;


public class UnmappedFeaturesDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Unmapped features</h1>" +
	"<p>The program was unable to map some of the features to coordinates on the genome. </p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/coordinatemap/\">Help</a></p>" +
	"</body></html>";
	private List<String> misses;
	private int rows;

	public UnmappedFeaturesDialog(JFrame parent, List<String> misses, int rows) {
		super(parent, "Unmapped Features", true);
		this.misses = misses;
		this.rows = rows;
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {
		setSize(500, 360);
		setPreferredSize(new Dimension(500, 360));

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		
		JTextField counts = new JTextField(String.format("%d out of %d", misses.size(), rows));
		counts.setEditable(false);
		
		StringBuilder sb = new StringBuilder();
		for (String name : misses) {
			sb.append(name).append("\n");
		}
		JTextArea details = new JTextArea(sb.toString());
		details.setEditable(false);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,8,12,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Unmapped Features:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		this.add(counts, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(6,12,4,12);
		this.add(new JScrollPane(details), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(6,12,12,2);
		this.add(buttonPanel, c);

	}

	public void ok() {
		setVisible(false);
		dispose();
	}


	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);
		
		int count = 100;
		List<String> misses = Arrays.asList(new String[] {
				"blither",
				"blather",
				"bonk",
				"oink",
				"snurfle",
				"oggle",
				"splat",
				"twiddle",
				"flappy"
		});

		final UnmappedFeaturesDialog dialog = new UnmappedFeaturesDialog(frame, misses, count);
		dialog.setVisible(true);
	}
}
