package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;

import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.ViewParameters.ViewParametersListener;


/**
 * A panel with a scrollbar at the bottom to hold the genome view panel.
 */
public class HorizontalScrollPanel extends JPanel {
	private GenomeViewPanel genomeViewPanel;
	private JScrollBar scrollBar;
	private ViewParameters p;

	// when the view parameters change, we want to update the scrollbar
	// and when the scrollbar changes, we want to update the view params.
	// this breaks the loop that otherwise forms.
	private boolean suppressAdjustmentEvent;

	
	// Having the genomeViewPanel contained by a scrollPane causes
	// GenomeViewPanel.setBounds(...) (and then ViewParameters.setSize) to be
	// called for each scroll event, which probably isn't very efficient.
	// It might be worthwhile to change this to avoid containing gvp in
	// a scrollpane.

	public HorizontalScrollPanel(GenomeViewPanel gvp) {
		this.genomeViewPanel = gvp;
		setLayout(new BorderLayout());
		add(genomeViewPanel, BorderLayout.CENTER);
		
		p = genomeViewPanel.params;
		scrollBar = new JScrollBar(SwingConstants.HORIZONTAL, p.getStart(), (p.getEnd() - p.getStart()), 0, p.getSequenceLength());
		scrollBar.setBlockIncrement(10000);
		scrollBar.setUnitIncrement(1000);
		p.addViewParametersListener(new ViewParametersListener() {

			public void viewParametersChanged(ViewParameters p) {
				try {
					suppressAdjustmentEvent = true;
					
					// adjust scroll bar for changes in chromosome size
					if (scrollBar.getMaximum() != p.getSequenceLength()) {
						scrollBar.setMaximum(p.getSequenceLength());
					}

					// hack - Mac OS X makes the scrollbar disappear if its extent is equal to
					//        its whole range, so here we keep it at least one short.
					scrollBar.setVisibleAmount(Math.min(scrollBar.getMaximum() - 1, p.getEnd() - p.getStart()));
					scrollBar.setValue(p.getStart());
				}
				finally {
					suppressAdjustmentEvent = false;
				}
			}
			
		});

		scrollBar.addAdjustmentListener(new AdjustmentListener() {

			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (!suppressAdjustmentEvent) {
					p.setStart(e.getValue());
				}
			}
			
		});
		add(scrollBar, BorderLayout.SOUTH);
	}


}
