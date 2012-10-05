package org.systemsbiology.ncbi.ui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.systemsbiology.util.swing.Spinner;

/**
 * simple UI to test Spinner
 */
public class TestSpinner {
    JFrame frame;
    private JButton button;
    boolean started;
    private Spinner spinner;

    public void testSpinner() {
        frame = new JFrame("Test Spinner");
        Container c = frame.getContentPane();
        c.setLayout(new FlowLayout());
        c.add(new JLabel("Spinner!"));
        button = new JButton("Start");
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (started) {
                        button.setText("Start");
                        started=false;
                        spinner.setSpinning(false);
                    } else {
                        button.setText("Stop");
                        started=true;
                        spinner.setSpinning(true);
                    }
                }
            });
        c.add(button);
        spinner = new Spinner(20);
        c.add(spinner);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        TestSpinner t = new TestSpinner();
        t.testSpinner();
    }
}
