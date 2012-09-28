package org.systemsbiology.genomebrowser.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;


// unused - to be deleted soon

/**
 * Allow the user to edit the set of sequences associated with a dataset. If the user clicks OK,
 * the dialog returns a List<SequenceDescription> holding names of sequences, lengths in base-pairs
 * and topologies.
 * 
 * @author cbare
 */
public class EditSequencesDialog  extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Edit Sequences</h1>" +
	"<p>The Genome Browser plots data against coordinates on the genome or some other sequence. This " +
	"dialog allows you to manually define or edit the sequences to be plotted against. Enter a list of" +
	"sequences and sizes in base-pairs, separated by a tab or comma (or ; or :) - one sequence per line.</p>" +
	"</body></html>";

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextArea sequencesTextArea;
	private JButton okButton;

	private ButtonGroup topologyButtonGroup;


	public EditSequencesDialog(JFrame owner) {
		super(owner, "Edit Sequences");
		initGui();
		setLocationRelativeTo(owner);
	}

	public void initGui() {
		setSize(380, 500);
		setPreferredSize(new Dimension(380, 500));

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		sequencesTextArea = new JTextArea();

		JRadioButton topoLinear = new JRadioButton("linear", true);
		topoLinear.setActionCommand("linear");
		JRadioButton topoCircular = new JRadioButton("circular");
		topoCircular.setActionCommand("circular");
		JRadioButton topoUnknown = new JRadioButton("unknown");
		topoUnknown.setActionCommand("unknown");

		topologyButtonGroup = new ButtonGroup();
		topologyButtonGroup.add(topoLinear);
		topologyButtonGroup.add(topoCircular);
		topologyButtonGroup.add(topoUnknown);

		Box topologyBox = Box.createVerticalBox();
		topologyBox.add(topoLinear);
		topologyBox.add(topoCircular);
		topologyBox.add(topoUnknown);

		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		okButton.getActionMap().put("press-OK", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		im = okButton.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "press-OK");

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

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
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Topology:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(topologyBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Sequences:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4,12,4,12);
		this.add(new JScrollPane(sequencesTextArea), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,24,2);
		this.add(buttonPanel, c);
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	public List<SequenceDescription> parseSequences() {
		String text = sequencesTextArea.getText();
		List<SequenceDescription> results = new ArrayList<SequenceDescription>();
		Topology topology = Topology.fromString(topologyButtonGroup.getSelection().getActionCommand());
		for (String line : text.split("\n")) {
			line = line.trim();
			if (line.length()>0) {
				String[] fields = line.split("\\s*[\\t,;:]\\s*");
				if (fields.length==2) {
					int len = Integer.parseInt(fields[1]);
					results.add(new SequenceDescription(fields[0], len, topology));
				}
				else {
					throw new RuntimeException("Can't parse: \"" + line + "\".");
				}
			}
		}
		if (results.size()==0) {
			throw new RuntimeException("Enter at least one sequence.");
		}
		return results;
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void ok() {
		try {
			List<SequenceDescription> results = parseSequences();
			close();
			for (DialogListener listener: listeners) {
				listener.ok("ok", results);
			}
		}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
		}
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);
		
		EditSequencesDialog dialog = new EditSequencesDialog(frame);
		dialog.addDialogListener(new DialogListener() {
			public void ok(String action, Object result) {
				System.out.println("action=" + action);
				System.out.println("results=" + result);
			}
			public void cancel() {
				System.out.println("cancel");
			}
			public void error(String message, Exception e) {
				System.out.println("error: " + message);
				e.printStackTrace();
			}
		});
		dialog.setVisible(true);
	}


	public static class SequenceDescription {
		public final String name;
		public final int length;
		public final Topology topology;

		public SequenceDescription(String name, int length, Topology topology) {
			this.name = name;
			this.length = length;
			this.topology = topology;
		}

		@Override
		public String toString() {
			return "(" + name + ", " + length + ", " + topology + ")";
		}
	}
}
