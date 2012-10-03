package org.systemsbiology.genomebrowser.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.InvertionUtils;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class SequenceDialog extends JDialog {
	//private static final Logger log = Logger.getLogger(ImportFastaDialog.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Sequence</h1>" +
	"<p>Shows the (forward strand) sequence for the selected region or, if nothing is selected, the visible region.</p>" +
	"</body></html>";

	private JTextField nameTextField;
	private JTextArea sequence;
	private String seq;
	private SequenceFetcher sequenceFetcher;
	private String sequenceName;
	private Strand strand;
	private int start;
	private int end;
	private Pattern locationPattern = Pattern.compile("(.*[^+-\\.])([+-\\.]?):(\\d+)\\s*[-,]\\s*(\\d+)");

	private JCheckBox reverseComplementCheckBox;
	private boolean suppressCheckboxEvents = false;


	// TODO reverse compliment for reverse strand
	
	public SequenceDialog(Frame owner, String sequenceName, Strand strand, int start, int end, SequenceFetcher sequenceFetcher) {
		super(owner, "Sequence");
		this.sequenceFetcher = sequenceFetcher;
		this.sequenceName = sequenceName;
		this.strand = strand;
		this.start = start;
		this.end = end;

		initGui();
		setLocationRelativeTo(owner);
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

		nameTextField = new JTextField(formatDescription());
		
		// reset sequence info on enter
		im = nameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "reset-sequence");
		nameTextField.getActionMap().put("reset-sequence", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Matcher m = locationPattern.matcher(nameTextField.getText());
				if (m.matches()) {
					sequenceName = m.group(1); 
					strand = Strand.fromString(m.group(2));
					start = Integer.valueOf(m.group(3));
					end = Integer.valueOf(m.group(4));
					resetSequence(sequenceName, strand, start, end);
				}
			}
		});
		
		resetSequence(sequenceName, strand, start, end);
		
		sequence = new JTextArea();
		sequence.setLineWrap(true);
		sequence.setWrapStyleWord(true);
		sequence.setFocusTraversalKeysEnabled(true);
		Set<KeyStroke> keys2 = new HashSet<KeyStroke>();
		keys2.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		sequence.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys2);
		keys2.clear();
		keys2.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
		sequence.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys2);
		
		reverseComplementCheckBox = new JCheckBox("Reverse Complement");
		reverseComplementCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!suppressCheckboxEvents)
					reverseComplement();
			}
		});

		JButton blastButton = new JButton("Blast");
		blastButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				blast();
			}
		});

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(blastButton);
		
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
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(nameTextField, c);

		c.gridy++;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(reverseComplementCheckBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.gridheight = 1; 
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(18,12,18,12);
		this.add(new JScrollPane(sequence),c);

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

	public void close() {
		setVisible(false);
		dispose();
	}

	public void ok() {
		close();
	}
	
	public void blast() {
		try {
        Desktop.getDesktop().browse(new java.net.URI("http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE=Nucleotides&DATABASE=nr&BLAST_PROGRAMS=discoMegablast&QUERY="+sequence.getText()));
		} catch (Exception e) {
			showErrorMessage(e.getClass().getSimpleName() + " " + e.getMessage());
		}
	}
	
	public void reverseComplement() {
		seq = InvertionUtils.inversion(seq);
		sequence.setText(seq);
	}

	private void resetSequence(final String sequenceName, final Strand strand, final int start, final int end) {
		new Thread() {
			public void run() {
				if (sequenceFetcher!=null) { 
					seq = sequenceFetcher.getSequence(sequenceName, strand, start, end);
				}
				else {
					seq = "-- not available! --";
				}
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						suppressCheckboxEvents = true;
						if (strand==Strand.reverse) {
							reverseComplementCheckBox.setSelected(true);
							seq = InvertionUtils.inversion(seq);
						}
						else {
							reverseComplementCheckBox.setSelected(false);
						}
						suppressCheckboxEvents = false;
						sequence.setText(seq);
					}
				});
			}
		}.start();
	}

	private String formatDescription() {
		return String.format("%s%s:%d-%d", sequenceName, strand.toAbbreviatedString(), start, end);
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}
	
}
