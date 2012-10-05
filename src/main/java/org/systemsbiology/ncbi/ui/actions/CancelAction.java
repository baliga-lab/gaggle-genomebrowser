package org.systemsbiology.ncbi.ui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.systemsbiology.ncbi.ui.NcbiQueryDialog;

public class CancelAction extends AbstractAction {
    private NcbiQueryDialog ncbiQueryDialog;

    public CancelAction(NcbiQueryDialog ncbiQueryDialog) {
        super("Cancel");
        this.ncbiQueryDialog = ncbiQueryDialog;
    }

    public void actionPerformed(ActionEvent e) {
        ncbiQueryDialog.cancel();
    }
}
