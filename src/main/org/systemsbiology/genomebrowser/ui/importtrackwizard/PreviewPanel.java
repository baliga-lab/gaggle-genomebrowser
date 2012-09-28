package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.io.TextFileInfo;
import org.systemsbiology.genomebrowser.io.TextFileScanner;
import org.systemsbiology.util.ProgressListener;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.ETable;
import org.systemsbiology.util.swing.SwingGadgets;


public class PreviewPanel extends JPanel implements WizardPanel {
	private static final Logger log = Logger.getLogger(PreviewPanel.class);
	private JComboBox loaderChooser;
	private JComboBox trackTypeChooser;
	private MyTableModel previewTableModel;
	private JTextField fileSize;
	private JTextField status;
	private JLabel previewLabel;
	private JCheckBox columnHeadersPresent;
	private WizardMainWindow parent;
	private ImportTrackWizard wiz;
	private static final String SELECT_LOADER = "-- Select Loader --";
	private static final String INSTRUCTIONS_HTML = "<html><body>" +
			"<h1>File Preview</h1>" +
			"<p>Selecting the <b>file format</b> lets the " +
			"software know how to read the file (what data to expect in which" +
			"columns). Selecting the " +
			"<b>type of track</b> specifies whether the data represents genes, numeric (or quantitative) data " +
			"or some other type of information.</p>" +
			"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/import/\">Help on file" +
			"formats and track types.</a></p></body></html>";
	private TextFileInfo fileInfo;


	public PreviewPanel(WizardMainWindow parent, ImportTrackWizard wiz) {
		this.parent = parent;
		this.wiz = wiz;
		initGui();
	}

	private void initGui() {
		setOpaque(false);

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		columnHeadersPresent = new JCheckBox("First line holds column headers");
		columnHeadersPresent.setOpaque(false);
		columnHeadersPresent.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateColumnHeaders(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		previewTableModel = new MyTableModel();
		JTable previewTable = new ETable(previewTableModel);
		previewTable.setPreferredScrollableViewportSize(new Dimension(400,160));

		status = new JTextField();
		status.setEditable(false);
		status.setBorder(BorderFactory.createEmptyBorder());
		status.setOpaque(false);

		fileSize = new JTextField(8);
		fileSize.setEditable(false);

		loaderChooser = new JComboBox();
		loaderChooser.addItem(SELECT_LOADER);
		loaderChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.updateStatus();
				wiz.setLoaded(false);
				log.info("selected loader: " + loaderChooser.getSelectedItem());
			}});
		for (TrackLoaderDescription reader : wiz.getTrackReaderInfos()) {
			loaderChooser.addItem(reader.name);
		}

		trackTypeChooser = new JComboBox();
		for (String type : wiz.getTrackTypes()) {
			trackTypeChooser.addItem(type);
		}
		trackTypeChooser.setSelectedItem("quantitative.segment");
		trackTypeChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				wiz.setLoaded(false);
				log.info("selected track type: " + trackTypeChooser.getSelectedItem());
			}});

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(8,8,6,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,8);
		previewLabel = new JLabel("Preview (first 100 lines)");
		this.add(previewLabel, c);
		
		c.gridx = 2;
		c.gridwidth = 1;
		c.insets = new Insets(12,2,2,8);
		this.add(columnHeadersPresent, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(2,12,2,8);
		this.add(new JScrollPane(previewTable), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,12,6,8);
		this.add(status, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(6,12,2,2);
		this.add(new JLabel("File size:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(6,2,2,2);
		this.add(fileSize, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(6,12,2,2);
		this.add(new JLabel("Loader:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(6,2,2,2);
		this.add(loaderChooser, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(6,12,2,2);
		this.add(new JLabel("Track type:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(6,2,2,2);
		this.add(trackTypeChooser, c);
	}

	public void clear() {
		// clear table
		Vector<?> dataVector = previewTableModel.getDataVector();
		dataVector.clear();
		previewTableModel.setColumnIdentifiers((Vector<Object>)null);
		previewTableModel.setColumnCount(0);
		fileSize.setText("");
	}

	public void previewFile(String filename) {
		log.info("loading preview information");
		clear();
		ProgressListener progressListener = new ProgressListener() {
			int progress = 0;

			public void done() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
//						progressBar.setValue(progressBar.getMaximum());
						status.setText("ok");
					}
				});
			}

			public void init(final int totalExpectedProgress, final String message) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
//						progressBar.setValue(0);
//						progressBar.setMaximum(totalExpectedProgress);
						status.setText(message);
					}
				});
			}

			public void init(final int totalExpectedProgress) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						status.setText("inspecting file...");
					}
				});
			}

			public void incrementProgress(int amount) {
				progress += amount;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						status.setText("progress: " + progress);
					}
				});
			}

			public void setProgress(int newValue) {
				progress = newValue;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						status.setText("progress: " + progress);
					}
				});
			}
		};

		wiz.setLoaded(false);
		wiz.setScanned(false);

		try {
			TextFileScanner scanner = new TextFileScanner();
			scanner.addProgressListener(progressListener);
			fileInfo = scanner.scanFile(filename);

			updatePreviewTable();

			fileSize.setText(fileInfo.getLengthAsString());

			status.setText("ok");
			status.setForeground(Color.BLACK);

			wiz.setScanned(true);
			parent.updateStatus();
		}
		catch (final Exception e) {
			try {
				parent.showErrorMessage("Error loading file", e);
				status.setText("Error loading file");
				status.setForeground(Color.RED);
			}
			catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	private void updatePreviewTable() {
		// prevent table from updating
		previewTableModel.setSupressEvents(true);

		previewLabel.setText(String.format("Preview (first %d lines)", fileInfo.getPreviewLineCount()));
		Vector<?> dataVector = previewTableModel.getDataVector();
		dataVector.clear();

		previewTableModel.setColumnIdentifiers((Vector<Object>)null);
		previewTableModel.setColumnCount(fileInfo.getColumnCount());
		for (String[] fields: fileInfo.getPreviewFields()) {
			previewTableModel.addRow(fields);
		}

		if (fileInfo.hasColumnTitles())
			previewTableModel.setColumnIdentifiers(fileInfo.getColumnTitles());

		// ok, now that we're finished, update table
		previewTableModel.setSupressEvents(false);
		previewTableModel.fireTableStructureChanged();
	}

	private void updateColumnHeaders(boolean present) {
		if (fileInfo == null) return;
		wiz.setLoaded(false);
		fileInfo.setFirstLineHoldsColumnTitles(present);
		updatePreviewTable();
	}

	public void onLoad() {
		if (!wiz.isScanned() && !StringUtils.isNullOrEmpty(wiz.getFilename())) {
			previewFile(wiz.getFilename());
			if (wiz.getFilename().matches(".*\\.gff.?")) {
				loaderChooser.setSelectedItem("GFF");
			}
		}
	}

	public void onUnload() {
		wiz.setLoader((String)loaderChooser.getSelectedItem());
		wiz.setTrackType((String)trackTypeChooser.getSelectedItem());
		if (fileInfo != null)
			wiz.setHasColumnHeaders(fileInfo.hasColumnTitles());
		wiz.deriveTrackNameFromFilename();
	}

	public boolean getEnableDone() {
		return false;
	}

	public boolean getEnableBack() {
		return true;
	}

	public boolean getEnableNext() {
		return wiz.isScanned() && !SELECT_LOADER.equals(loaderChooser.getSelectedItem());
	}

	public static class MyTableModel extends DefaultTableModel {
		boolean supressEvents;

		/**
		 * SupressEvents prevents the table from trying to redraw while it's
		 * being updated. This seems like it shouldn't be necessary, but without
		 * it I get weird errors. See the code in previewFile(...) that populates
		 * the table.
		 */
		public void setSupressEvents(boolean supressEvents) {
			this.supressEvents = supressEvents;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
	    public void fireTableChanged(TableModelEvent e) {
			if (!supressEvents)
				super.fireTableChanged(e);
		}
	}

	public void windowGainedFocus() {
		loaderChooser.requestFocusInWindow();
	};
}
