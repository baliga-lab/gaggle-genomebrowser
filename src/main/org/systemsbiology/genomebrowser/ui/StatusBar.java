package org.systemsbiology.genomebrowser.ui;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.ViewParameters.ViewParametersListener;



public class StatusBar extends JPanel implements ViewParametersListener, CrosshairsListener {

	private JTextField sequenceTextField;
//	private JTextField selectionTextField;
	private JTextField rangeTextField;
	private JTextField widthTextField;
	private JTextField coordTextField;
	private JTextField highlightedsizeTextField;
	private UI ui;
	private String seqId;


	public StatusBar(UI ui) {
		this.ui = ui;
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new FlowLayout(FlowLayout.LEFT));

		sequenceTextField = new JTextField("");
		sequenceTextField.setColumns(14);
		sequenceTextField.setEditable(false);
		sequenceTextField.setBackground(this.getBackground());
		add(sequenceTextField);
		
//		selectionTextField = new JTextField("0 genes selected");
//		selectionTextField.setColumns(8);
//		selectionTextField.setEditable(false);
//		selectionTextField.setBackground(this.getBackground());

		add(new JLabel("Displayed Range: "));
		rangeTextField = new JTextField();
		rangeTextField.setColumns(14);
		rangeTextField.addKeyListener(new RangeKeyListener(rangeTextField));
		add(rangeTextField);

		add(new JSeparator());

		add(new JLabel("Width: "));
		widthTextField = new JTextField();
		widthTextField.setColumns(6);
		widthTextField.addKeyListener(new WidthKeyListener());
		add(widthTextField);

		add(new JSeparator());

		add(new JLabel("Coordinate: "));
		coordTextField = new JTextField();
		coordTextField.setColumns(14);
		coordTextField.addKeyListener(new RangeKeyListener(coordTextField));
		add(coordTextField);
		
		// displaying the size of a highlighted region. by dmartinez & rvencio & caten Jan/2012
		add(new JSeparator());
		add(new JLabel("Highlighted size: "));
		highlightedsizeTextField = new JTextField();
		highlightedsizeTextField.setColumns(6);
		highlightedsizeTextField.addKeyListener(new RangeKeyListener(highlightedsizeTextField));
		add(highlightedsizeTextField);
		
	}

	public void viewParametersChanged(ViewParameters params) {
		rangeTextField.setText(params.getStart() + ", " + params.getEnd());
		widthTextField.setText(String.valueOf(params.getEnd() - params.getStart()));
	}

	public void crosshairsAt(int coord) {
		coordTextField.setText(String.valueOf(coord));
	}

	public void crosshairsDone() {
		//coordTextField.setText("-");
	}

	public void selectBoxAt(int start, int end) { //modified 
		coordTextField.setText(String.valueOf(start) + ", " + String.valueOf(end));
		highlightedsizeTextField.setText(String.valueOf( (end-start)+1 )); // displaying the size of a highlighted region. by dmartinez & rvencio & caten Jan/2012
	}

	public void setSequence(Sequence sequence) {
		if (sequence==null) {
			this.seqId = "";
			sequenceTextField.setToolTipText("No chromosomes.");
		}
		else {
			this.seqId = sequence.getSeqId();
			sequenceTextField.setToolTipText("Currently displaying: " + seqId);
		}
		sequenceTextField.setText(seqId);
	}

	class RangeKeyListener implements KeyListener {
		JTextField textField;

		public RangeKeyListener(JTextField textField) {
			this.textField = textField;
		}

		public void keyPressed(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.VK_ENTER) {
				try {
					// if user typed a single coordinate
					int position = Integer.parseInt(textField.getText());
					ui.centerOnPosition(position);
					return;
				}
				catch (Exception e) {}
				try {
					Segment segment = Segment.parse(seqId, textField.getText());
					ui.setViewSegment(segment);
					return;
				}
				catch (Exception e) {}
				ui.showErrorMessage("Unable to parse: " + textField.getText());
			}
		}

		public void keyReleased(KeyEvent event) {}
		public void keyTyped(KeyEvent event) {}
	}

	class WidthKeyListener implements KeyListener {
		public void keyPressed(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.VK_ENTER) {
				try {
					int width = Integer.parseInt(widthTextField.getText());
					int finalWidth = ui.setWidthInBasePairs(width);
					if (finalWidth != width)
						widthTextField.setText(String.valueOf(finalWidth));
				}
				catch (Exception e) {}
			}
		}
		public void keyReleased(KeyEvent event) {}
		public void keyTyped(KeyEvent event) {}
	}
}
