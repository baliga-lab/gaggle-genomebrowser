package org.systemsbiology.genomebrowser.ui.buffering;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import javax.swing.*;

// http://www.javalobby.org/forums/thread.jspa?threadID=16840&tstart=0
// http://www.javalobby.org/java/forums/m91824097.html#91824097

public class BufferingExample {
	private static final int FRAME_DELAY = 20; // 20ms. implies 50fps (1000/20) = 50
	JFrame frame;
	private Canvas canvas;


	public BufferingExample() {
		initGui();
	}

	private void initGui() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				frame = new JFrame("BufferingExample");
				canvas = new Canvas();
				canvas.setSize(500, 1000);
				frame.add(canvas);
				frame.pack();
				frame.setVisible(true);

				Thread gameThread = new Thread(new GameLoop(canvas));
				gameThread.setPriority(Thread.MIN_PRIORITY);
				gameThread.start();
			}
		});
	}

	private static class GameLoop implements Runnable {

		boolean isRunning;
		Canvas gui;
		long cycleTime;
		double theta;

		public GameLoop(Canvas canvas) {
			gui = canvas;
			isRunning = true;
			theta = 0.0;
		}

		public void run() {
			cycleTime = System.currentTimeMillis();
			gui.createBufferStrategy(2);
			BufferStrategy strategy = gui.getBufferStrategy();

			// Game Loop
			while (isRunning) {

				updateGameState();

				updateGUI(strategy);

				synchFramerate();
			}
		}

		private void updateGameState() {
			theta += 0.05;
		}

		private void updateGUI(BufferStrategy strategy) {
			Graphics g = strategy.getDrawGraphics();
			System.out.println(strategy.contentsLost());
			g.copyArea(0, 0, 500, 1000, 20, 0);

//			g.setColor(Color.WHITE);
//			g.fillRect(0, 0, gui.getWidth(), gui.getHeight());
//			g.setColor(Color.BLACK);

			int y = (int)(200.0 * (Math.sin(theta) + 1.0));

			g.setColor(Color.RED);
			g.fillOval(50, y, 80, 90);
			g.setColor(Color.BLACK);
			g.drawOval(50, y, 80, 90);

			g.dispose();
			strategy.show();
		}

		private void synchFramerate() {
			cycleTime = cycleTime + FRAME_DELAY;
			long difference = cycleTime - System.currentTimeMillis();
			try {
				Thread.sleep(Math.max(0, difference));
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		new BufferingExample();
	}
}
