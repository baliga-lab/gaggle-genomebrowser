package org.systemsbiology.genomebrowser.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.UUID;

import javax.swing.*;

import org.systemsbiology.genomebrowser.impl.BasicDataset;
import org.systemsbiology.genomebrowser.impl.BasicSequence;
import org.systemsbiology.genomebrowser.model.*;
import org.systemsbiology.util.swing.AttributesTableModel;
import org.systemsbiology.util.swing.ETable;


/**
 * Display properties of a dataset. Eventually this should be made editable.
 * 
 * @author cbare
 */
public class ProjectPropertiesDialog extends JDialog {
	private Dataset dataset;
	private String filename;

	public ProjectPropertiesDialog(JFrame parent, Dataset dataset, String filename) {
		super(parent, "Project Properties", true);
		this.dataset = dataset;
		this.filename = filename;
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {
//		setSize(380, 400);
//		setPreferredSize(new Dimension(380, 400));

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JTextField name = new JTextField(dataset.getName(), 42);
		name.setEditable(false);

		JTextField uuid = new JTextField(42);
		if (dataset.getUuid() != null) uuid.setText(dataset.getUuid().toString());
		uuid.setEditable(false);

		JTextField filenameTextField = new JTextField();
		if (filename!=null) filenameTextField.setText(filename);
		filenameTextField.setEditable(false);

		JTextArea sequencesTextArea = new JTextArea();
		sequencesTextArea.setEditable(false);
		
		StringBuilder sb = new StringBuilder();
		for (Sequence sequence: dataset.getSequences()) {
			sb.append(sequence.getSeqId()).append(", ").append(sequence.getLength()).append("\n");
		}
		sequencesTextArea.setText(sb.toString());
		
		AttributesTableModel tableModel = new AttributesTableModel(dataset.getAttributes());
		JTable table = new ETable(tableModel);

		JButton okButton = new JButton("OK");
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
//
//		JButton cancelButton = new JButton("Cancel");
//		cancelButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				cancel();
//			}
//		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
		buttonPanel.add(okButton);
//		buttonPanel.add(cancelButton);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Name:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(name, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("uuid:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(uuid, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("filename:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(filenameTextField, c);

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
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Attributes:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4,12,4,12);
		this.add(new JScrollPane(table), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,24,2);
		this.add(buttonPanel, c);
		
		pack();
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public void ok() {
		close();
	}
	
	public void cancel() {
		close();
	}


	public static void main(String[] args) {
		final JFrame frame = new JFrame("testing 123");
		frame.pack();
		frame.setVisible(true);
		
		BasicDataset dataset = new BasicDataset(UUID.randomUUID(), "Test Dataset");
		dataset.addSequence(new BasicSequence(UUID.randomUUID(), "Chromosome 1", 23000234, Topology.circular));
		dataset.addSequence(new BasicSequence(UUID.randomUUID(), "Chromosome 2", 19000456, Topology.circular));
		dataset.addSequence(new BasicSequence(UUID.randomUUID(), "Chromosome 3", 17000789, Topology.circular));
		dataset.getAttributes().put("created-by", "Dr. Frank N. Furter");
		dataset.getAttributes().put("created-on", "2008-01-03");
		dataset.getAttributes().put("species", "Sasquatch");
		dataset.getAttributes().put("flapdoodle", "snorklewacker");

		ProjectPropertiesDialog dialog = new ProjectPropertiesDialog(frame, dataset, "/Users/Qwerty/Documents/hbgb/MyBogusTextData.hbgb");
		dialog.setVisible(true);
	}
	
}
