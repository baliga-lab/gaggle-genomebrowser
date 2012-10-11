package org.systemsbiology.ncbi.ui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.log4j.Logger;
import org.systemsbiology.ncbi.NcbiApi;
import org.systemsbiology.ncbi.NcbiGenome;
import org.systemsbiology.ncbi.NcbiSequence;
import org.systemsbiology.ncbi.EUtilitiesGenomeProjectSummary;
import org.systemsbiology.ncbi.NcbiGenomeProjectSummary;
import org.systemsbiology.ncbi.ui.actions.*;
import org.systemsbiology.util.ProgressListener;
import org.systemsbiology.util.swing.Spinner;

/**
 * Allows the user to download summary information and locations of genes from
 * NCBI's genome projects database. Don't forget to call dispose()!
 * @author cbare
 */
public class NcbiQueryDialog extends JFrame {
    private static final Logger log = Logger.getLogger(NcbiQueryDialog.class);
    private JButton prokaryoteButton;
    private JButton archaeaButton;
    private JButton bacteriaButton;
    private JTextField searchText;
    private JButton searchButton;
    private JComboBox speciesChooser;
    private JButton cancelButton;
    private JButton downloadButton;
    private Spinner spinner;
    private NcbiApi ncbi;
    private DownloadGenomeAction downloadGenomeAction;

    private NcbiGenomeProjectSummary summary;
    // TODO what's the purpose of this lock?
    private Object lock = new Object();

    public NcbiQueryDialog() {
        super("Grab NCBI Genome");
        ncbi = new NcbiApi();
        createGui();
    }

    private void createGui() {

        JPanel searchBox = new JPanel();
        searchBox.setLayout(new GridBagLayout());
        searchBox.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints c = new GridBagConstraints(); 
        int y = 0;

        prokaryoteButton = new JButton(new RetrieveProkaryoticGenomesAction(this));
        archaeaButton = new JButton(new RetrieveArchaealGenomesAction(this));
        bacteriaButton = new JButton(new RetrieveBacterialGenomesAction(this));
		
        JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
		
        toolbar.add(prokaryoteButton);
        toolbar.add(archaeaButton);
        toolbar.add(bacteriaButton);

        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0,0,9,0);
        searchBox.add(toolbar, c);

        spinner = new Spinner(20);
        c.gridx = 3;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(0,0,0,0);
        searchBox.add(spinner, c);

        ncbi.addProgressListener(new ProgressListener() {
                public void init(int totalExpectedProgress, String message) {
                    spinner.setSpinning(true);
                }
                public void init(int totalExpectedProgress) {
                    spinner.setSpinning(true);
                }
                public void incrementProgress(int amount) {
                    spinner.setProgress(true);
                }
                public void setProgress(int progress) {
                    spinner.setProgress(true);
                }
                public void done() {
                    spinner.setSpinning(false);
                }
                // ignore these for now
                public void setMessage(String message) { }
                public void setExpectedProgress(int expected) { }
            });

        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.WEST;
        searchBox.add(new JLabel("Search for organisms:"), c);

        Action action = new SearchNcbiGenomeProjectsAction(this);
        searchText = new JTextField();
        searchText.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), action);
        searchButton = new JButton(action);
        JPanel searchTextPanel = new JPanel();
        searchTextPanel.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 100.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        searchTextPanel.add(searchText, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.EAST;
        searchTextPanel.add(searchButton, c);

        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 3;
        c.weightx = 100.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5,0,0,0);
        searchBox.add(searchTextPanel, c);

        JLabel speciesLabel = new JLabel("Select an organism:");
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 3;
        c.insets = new Insets(15,0,0,0);
        searchBox.add(speciesLabel, c);

        speciesChooser = new JComboBox();
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0,0,0,0);
        searchBox.add(speciesChooser, c);

        String warningHtml = "<html>" +
            "<h2>Warning</h2>" +
            "Downloading will fetch the locations of genes and RNAs on " +
            "each chromosome from NCBI. Downloads from NCBI's web service can be slow, so " +
            "expect this to take some time. " +
            "Our parser for NCBI GB XML works for prokaryotes and yeast, but <b>doesn't work</b> " +
            "for other eukaryotes.<br>" +
            "<a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/ncbi/\">Click here for more information</a>.</html>" +
            "</html>";

        JEditorPane warningText = new JEditorPane();
        warningText.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        warningText.setEditable(false);
        warningText.setBackground(getBackground());
        warningText.setText(warningHtml);
        warningText.setPreferredSize(new Dimension(300,160));
        warningText.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    if (event.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(new java.net.URI(event.getURL().toString()));
                        } catch (Exception e) {
                            log.error("Failed to open browser: " + event, e);
                        }
                    }
                }
            });
        //		JScrollPane warningPane = new JScrollPane(warningText);
        //		warningPane.setPreferredSize(new Dimension(250,200));
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15,0,0,0);
        searchBox.add(warningText, c);
		
        JPanel okCancelButtonPanel = new JPanel();
        cancelButton = new JButton(new CancelAction(this));
        downloadGenomeAction = new DownloadGenomeAction(this);
        downloadGenomeAction.setEnabled(false);
        downloadButton = new JButton(downloadGenomeAction);
        okCancelButtonPanel.add(cancelButton);
        okCancelButtonPanel.add(downloadButton);
		
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 3;
        c.insets = new Insets(15,0,0,0);
        searchBox.add(okCancelButtonPanel, c);
		
        add(searchBox);
        pack();
        centerOnScreen();

        searchText.requestFocusInWindow();
    }

    public void centerOnScreen() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((d.width - this.getWidth()) / 2, (d.height - this.getHeight()) / 2 );
    }

    public void retrieveAllProkaryoticGenomes() {
        searchText.setText("");
        retrieveProkaryoticGenomes(ncbi.getNcbiOrganismCode("-- All Prokaryotes --"));
    }

    public void retrieveBacterialGenomes() {
        searchText.setText("");
        retrieveProkaryoticGenomes(ncbi.getNcbiOrganismCode("-- All Bacteria --"));
    }

    public void retrieveArchaealGenomes() {
        searchText.setText("");
        retrieveProkaryoticGenomes(ncbi.getNcbiOrganismCode("-- All Archaea --"));
    }

    private void retrieveProkaryoticGenomes(final String ncbiOrganismCode) {
        Thread thread = new Thread() {
                public void run() {
                    try {
                        populateSpeciesChooser(ncbi.retrieveProkaryoticGenomeProjects(ncbiOrganismCode));
                    }	catch (Exception e) {
                        log.error(e);
                    }
                }
            };
        thread.start();
    }

    public void search() {
        final String searchTerm = searchText.getText();
        Thread thread = new Thread() {
                public void run() {
                    try {
                        List<EUtilitiesGenomeProjectSummary> summaries = ncbi.retrieveGenomeProjectSummaries(searchTerm);
                        if (summaries.size() == 0)
                            showErrorMessage("No results found for: \"" + searchTerm + "\"");
                        populateSpeciesChooser(summaries);
                    }	catch (Exception e) {
                        log.error(e);
                    }
                }
            };
        thread.start();
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE);
    }

    private void populateSpeciesChooser(final List<? extends EUtilitiesGenomeProjectSummary> summaries) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    speciesChooser.removeAllItems();
                    for (NcbiGenomeProjectSummary summary : summaries) {
                        speciesChooser.addItem(new NcbiGenomeProjectSummaryWrapper(summary));
                    }
                    downloadGenomeAction.setEnabled(summaries.size() > 0);
                }
            });
    }

    public void setSpecies(final String species) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    searchText.setText(species);
                }
            });
    }

    /**
     * For each chromosome in the genome, download the locations of the genes.
     */
    public void downloadGenome() {
        if (speciesChooser.getItemCount() == 0) return;
        Thread thread = new Thread() {
                public void run() {
                    try {
                        synchronized (lock) {
                            summary = ((NcbiGenomeProjectSummaryWrapper)speciesChooser.getSelectedItem()).getNcbiGenomeProjectSummary();
                            
                            // check for incomplete genome projects with no related sequences
                            List<String> genomeIds = ncbi.retrieveGenomeIds(summary.getProjectId());
                            if (genomeIds.size() == 0) {
                                fireErrorEvent("Warning", String.format("Oops, there are no sequences for this project (id=%s); Its status is: %s", summary.getProjectId(), summary.getStatus()), null);
                                return;
                            }
                            fireDownloadedEvent(ncbi.retrieveGenome(summary));
                        }
                    }	catch (Exception e) {
                        log.error("Error importing data from NCBI", e);
                        if (e.getMessage().contains("GBSeq_contig")) {
                            String html = "<html>The NCBI GB XML records you requested contain <b>GBSeq_contig</b> " +
                                "elements which are not handled " +
                                "by this simple parser. <b>GBSeq_contig</b> elements seem to be present the " +
                                "records for many Eukaryotes but not in records for Prokaryotes. " + 
                                "<a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/ncbi/\">Click here for more information</a>.</html>";
                            fireErrorEvent("Error importing data from NCBI", html, e);
                        } else {
                            fireErrorEvent("Error importing data from NCBI", e.getMessage(), e);
                        }
                    }
                }
            };
        thread.start();
    }

    public void cancel() {
        spinner.setSpinning(false);
        fireCanceledEvent();
    }

    public NcbiGenomeProjectSummary getGenomeProjectSummary() {
        synchronized (lock) {
            return summary;
        }
    }

    // listener support -------------------------------------------------------

    /**
     * Respond to events based on the exit status of the dialog
     */
    public interface NcbiQueryDialogListener {
        public void genomeProjectDownloaded(NcbiGenome genome);
        public void canceled();
        public void error(String title, String message, Exception e);
    }

    Set<NcbiQueryDialogListener> listeners = new CopyOnWriteArraySet<NcbiQueryDialogListener>();

    public void addNcbiQueryDialogListener(NcbiQueryDialogListener listener) {
        listeners.add(listener);
    }

    public void removeNcbiQueryDialogListener(NcbiQueryDialogListener listener) {
        listeners.remove(listener);
    }

    public void fireCanceledEvent() {
        for (NcbiQueryDialogListener listener : listeners) {
            listener.canceled();
        }
    }

    public void fireErrorEvent(String title, String message, Exception e) {
        for (NcbiQueryDialogListener listener : listeners) {
            listener.error(title, message, e);
        }
    }

    public void fireDownloadedEvent(NcbiGenome genome) {
        for (NcbiQueryDialogListener listener : listeners) {
            listener.genomeProjectDownloaded(genome);
        }
    }

    @Override
    public void dispose() {
        spinner.setSpinning(false);
        super.dispose();
    }

    // ------------------------------------------------------------------------

    /**
     * Basic implementation of NcbiQueryDialogListener for debugging.
     */
    private static class DefaultNcbiQueryDialogListener implements NcbiQueryDialogListener {
        NcbiQueryDialog dialog;
		
        public DefaultNcbiQueryDialogListener(NcbiQueryDialog dialog) {
            this.dialog = dialog;
        }

        public void canceled() {
            dialog.setVisible(false);
            dialog.dispose();
            System.exit(1);
        }

        public void error(String title, String message, Exception e) {
            System.err.println(title);
            System.err.println(message);
            e.printStackTrace();
        }

        public void genomeProjectDownloaded(NcbiGenome genome) {
            dialog.setVisible(false);
            dialog.dispose();
            log.info("downloaded genome project (" + genome.getProjectId() + ")" + genome.getOrganismName());
            for (NcbiSequence sequence : genome.getSequences()) {
                log.info(sequence);
            }
            System.exit(1);
        }
    }

    /**
     * Open the NcbiQueryDialog for testing
     */
    public static void main(String[] args) {
        NcbiQueryDialog dialog = new NcbiQueryDialog();
        dialog.addNcbiQueryDialogListener(new DefaultNcbiQueryDialogListener(dialog));
        dialog.setVisible(true);
    }
}
