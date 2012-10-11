package org.systemsbiology.genomebrowser.ucscgb;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.impl.BasicSequence;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.ucscgb.Category;
import org.systemsbiology.ucscgb.Chromosome;
import org.systemsbiology.ucscgb.Gene;
import org.systemsbiology.ucscgb.Genome;
import org.systemsbiology.ucscgb.UCSCGB;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;
import org.systemsbiology.util.ProgressListener;

// TODO fix off-by-one error in start position

/**
 * Imports a genome from the UCSC
 * @author cbare
 */
public class ImportUcscGenome {
	private static final Logger log = Logger.getLogger(ImportUcscGenome.class);
	private JFrame frame;
	private UCSCGB ucscgb;
	private JButton okButton;
	private JComboBox organismChooser;
	private JComboBox domainChooser;
	private JCheckBox removeFragmentsCheckbox;
	private Set<DialogListener> dialogListeners = new CopyOnWriteArraySet<DialogListener>();
	private DatasetBuilder builder;
	private static final String SELECT_DOMAIN = "--- select domain ---";
	private static final String SELECT_ORGANISM = "--- select organism ---";
	private static final String INSTRUCTIONS_HTML = "<html><body>" +
		"<h1>Import a Genome</h1>" +
		"<p>Download genome data from the " +
		"<a href=\"http://genome.ucsc.edu/\">UCSC Genome Browser</a> or " +
		"<a href=\"http://microbes.ucsc.edu/\">UCSC Archaeal Genome Browser</a>.<br>" +
		"To get started, select the type of organism you would like to work with.</p>" +
		"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/import/\">Help</a></p>" +
		"</body></html>";

	public ImportUcscGenome() {
		initUi();
		ucscgb = new UCSCGB();
	}

	private void initUi() {
		frame = new JFrame("Import UCSC Genome");

		Box vbox = Box.createVerticalBox();
		vbox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		vbox.setOpaque(false);
		frame.add(vbox);

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(frame, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		vbox.add(instructions);

		Box hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Domain:"));
		domainChooser = new JComboBox();
		domainChooser.addItem(SELECT_DOMAIN);
		for (Category category : Category.values()) {
			domainChooser.addItem(category.toString());
		}
		domainChooser.setSelectedItem(SELECT_DOMAIN);
		domainChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					populateOrganismChooser((String)e.getItem());
				}
			}
		});
		hbox.add(domainChooser);
		vbox.add(hbox);

		hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Organism:"));
		organismChooser = new JComboBox();
		organismChooser.addItem(SELECT_ORGANISM);
		organismChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					if (SELECT_ORGANISM.equals(e.getItem())) return;
					okButton.setEnabled(true);
				}
			}
		});
		hbox.add(organismChooser);
		vbox.add(hbox);
		
		hbox = Box.createHorizontalBox();
		removeFragmentsCheckbox = new JCheckBox("remove unassembled fragments");
		removeFragmentsCheckbox.setSelected(true);
		hbox.add(removeFragmentsCheckbox);
		vbox.add(hbox);

		okButton = new JButton("Import");
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String domain = (String)domainChooser.getSelectedItem();
				Matcher m = Pattern.compile("(.*) \\((\\w*)\\)").matcher((String)organismChooser.getSelectedItem());
				if (m.matches()) {
					doImport(domain, m.group(2));
				}
				else {
					showErrorMessage("Can't parse: " + organismChooser.getSelectedItem());
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		vbox.add(Box.createVerticalStrut(16));
		vbox.add(buttonPanel);

		frame.pack();
	}

	private void populateOrganismChooser(String domain) {
		if (SELECT_DOMAIN.equals(domain)) return;
		List<Genome> genomes = ucscgb.loadGenomes(Category.valueOf(domain));
		organismChooser.removeAllItems();
		organismChooser.addItem(SELECT_ORGANISM);
		List<String> items = new ArrayList<String>();
		for (Genome genome : genomes) {
			items.add(String.format("%s (%s)", genome.getScientificName(), genome.getDbName()));
		}
		Collections.sort(items);
		for (String item: items) {
			organismChooser.addItem(item);
		}
		organismChooser.setSelectedItem(SELECT_ORGANISM);
	}

	private void doImport(String domain, String dbName) {
		Category category = Category.valueOf(domain);
		try {
			// TODO put this in a new thread
			// TODO use a spinner for progress
			Genome genome = ucscgb.loadGenome(category, dbName);
			List<Sequence> sequences = toSequences(ucscgb.chromInfo(category, dbName, removeFragmentsCheckbox.isSelected()), category);
			List<Gene> genes = ucscgb.genes(category, dbName, removeFragmentsCheckbox.isSelected());
			Dataset dataset = buildDataset(genome, sequences, genes);
			fireDatasetEvent(dataset);
		}
		catch (Exception e) {
			showErrorMessage("Error importing genome", e);
		}
	}

	/**
	 * Convert a list of Chromosomes to a list of Sequences. Chromosomes is a simple object
	 * representing an entry in the UCSC chromInfo table, with a name and a length. We
	 * create a UUID for the sequence here and try (not very hard) to guess its topology.
	 */
	private List<Sequence> toSequences(List<Chromosome> chromosomes, Category category) {
		// TODO how can we more accurately guess topology of a sequence?
		Topology topology = category.isProkaryotic() ? Topology.circular : Topology.linear;
		List<Sequence> sequences = new ArrayList<Sequence>(chromosomes.size());
		for (Chromosome chr : chromosomes) {
			UUID uuid = UUID.randomUUID();
			BasicSequence seq = new BasicSequence(uuid, chr.getName(), chr.getSize(), topology);
			seq.getAttributes().put("ucsc.name", chr.getName());
			sequences.add(seq);
		}
		return sequences;
	}


	// dependency
	/**
	 * 
	 */
	public void setDatasetBuilder(DatasetBuilder datasetBuilder) {
		this.builder = datasetBuilder;
	}

	private Dataset buildDataset(Genome genome, List<Sequence> sequences, final List<Gene> genes) {
		UUID datasetUuid = builder.beginNewDataset(genome.getScientificName());

		builder.setAttribute(datasetUuid, "created-on", new Date());
		builder.setAttribute(datasetUuid, "created-by", System.getProperty("user.name"));
		builder.setAttribute(datasetUuid, "species", genome.getScientificName());
		builder.setAttribute(datasetUuid, "ucsc.db.name", genome.getDbName());
		builder.setAttribute(datasetUuid, "domain", genome.getDomain());
		builder.setAttribute(datasetUuid, "ucsc.clade", genome.getClade());
		if (genome.getTaxid() > 0)
			builder.setAttribute(datasetUuid, "ncbi.taxonomy.id", genome.getTaxid());

		// add sequences (chromosomes, plasmids, etc.) to dataset
//		Topology topology = category.isProkaryotic() ? Topology.circular : Topology.linear;
//		for (Chromosome chr : chromosomes) {
//			builder.addSequence(chr.getName(), chr.getSize(), topology);
//		}
		builder.addSequences(sequences);

		// create a feature source for the gene track
		FeatureSource featureSource = new FeatureSource() {
			public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
				GeneFeatureFields fields = new GeneFeatureFields();
				for (Gene gene: genes) {
					fields.gene = gene;
					featureProcessor.process(fields);
				}
			}
			public void addProgressListener(ProgressListener progressListener) {}
			public void removeProgressListener(ProgressListener progressListener) {}
		};

		UUID trackUUID = builder.addTrack("gene", "Genes", featureSource);
		builder.setAttribute(trackUUID, "top", "0.42");
		builder.setAttribute(trackUUID, "height", "0.16");
		builder.setAttribute(trackUUID, "viewer", "Gene");

		return builder.getDataset();
	}

	private void showErrorMessage(String message) {
		if (message == null)
			message = "Error";
		log.warn(message);
		JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	private void showErrorMessage(String message, Exception e) {
		if (message == null)
			message = "Error";
		log.warn(message, e);
		JOptionPane.showMessageDialog(frame, message + "\n" + 
				e.getClass().getName() + "\n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	private void fireDatasetEvent(Dataset dataset) {
		close();
		for (DialogListener listener: dialogListeners) {
			listener.ok("ok", dataset);
		}
	}
	
	private void cancel() {
		close();
		for (DialogListener listener: dialogListeners) {
			listener.cancel();
		}
	}
	
	private void close() {
		frame.setVisible(false);
		frame.dispose();
	}

	public void addDialogListener(DialogListener listener) {
		dialogListeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		dialogListeners.remove(listener);
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}

	public void dispose() {
		frame.dispose();
	}

	public static void main(String[] args) {
		final ImportUcscGenome dialog = new ImportUcscGenome();
		dialog.addDialogListener(new DialogListener() {
			public void ok(String action, Object result) {
				System.out.println(action + " -> " + result);
			}
			public void cancel() {
			}
			public void error(String message, Exception e) {
				System.out.println(message);
				e.printStackTrace();
			}
		});
		dialog.setVisible(true);
	}
}
