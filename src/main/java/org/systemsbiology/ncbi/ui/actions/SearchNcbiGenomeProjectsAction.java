package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import org.systemsbiology.ncbi.ui.NcbiQueryDialog;

public class SearchNcbiGenomeProjectsAction extends AbstractAction {
    private NcbiQueryDialog ncbiQueryDialog;

    public SearchNcbiGenomeProjectsAction(NcbiQueryDialog ncbiQueryDialog) {
        super("Search");
        this.ncbiQueryDialog = ncbiQueryDialog;
        putValue(Action.SHORT_DESCRIPTION, "Search for genome projects.");
    }

    public SearchNcbiGenomeProjectsAction(Icon icon, NcbiQueryDialog ncbiQueryDialog) {
        super("Search", icon);
        this.ncbiQueryDialog = ncbiQueryDialog;
    }

    public void actionPerformed(ActionEvent e) {
        ncbiQueryDialog.search();
    }
}
