package org.systemsbiology.genomebrowser.visualization;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ui.GenomeViewPanel;
import org.systemsbiology.genomebrowser.visualization.ViewParameters.ViewParametersListener;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackManager;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;


/**
 * Coordinate rendering to an off screen image.
 * 
 * The scheduler is responsible for maintaining a queue of tasks which access
 * and render track data. The scheduler keeps a frame counter which the tasks
 * can check to avoid unnecessary work.
 */
public class TrackRendererScheduler implements ViewParametersListener {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TrackRendererScheduler.class);
	private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	// counts frames to avoid drawing blocks from previous frames.
	private AtomicInteger counter = new AtomicInteger();

	private TrackManager trackManager;
	
	private GenomeViewPanel panel;
	private TaskRunner taskRunner;



	// dependency
//	public void setQueue(Queue<Runnable> queue) {
//		this.queue = queue;
//	}

	// dependency
	public void setGenomeViewPanel(GenomeViewPanel panel) {
		this.panel = panel;
	}

	// dependency
	public void setTrackManager(TrackManager trackManager) {
		this.trackManager = trackManager;
	}

	public int getCurrentFrame() {
		return counter.get();
	}

	public void startTaskRunnerThread() {
		taskRunner = new TaskRunner();
		taskRunner.setQueue(queue);
		new Thread(taskRunner).start();
	}

	/**
	 * Schedule the given renderers to do their thing on the given region of the genome.
	 */
	public void schedule(Iterable<TrackRenderer> renderers, Sequence sequence, int start, int end) {

		// rendering timing
		// final long startMillis = System.currentTimeMillis();
		//log.info(String.format("rendering: %s:%,d-%,d", sequence.getSeqId(), start, end));

		// increment frame counter
		final int frame = counter.incrementAndGet();

		// remove all pending BlockKeys from the queue, we may not need to load them anymore.
		queue.clear();
		
		// the queue can't ever get too full, 'cause we clear it here. So, at most,
		// we'll have one frame worth of data tasks plus whatever task was in-progress
		// at the time.

		if (sequence==null) return;
		final FeatureFilter filter = new FeatureFilter(sequence, Strand.any, start, end);

		// creating this buffer every time is probably slow. Maybe we could just recreate when
		// panel size changes? Or just create one big enough for the whole screen?
		final Image offScreenImage = panel.createImage(panel.getWidth(), panel.getHeight());
		Graphics g = offScreenImage.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, panel.getWidth(), panel.getHeight());


		for (TrackRenderer renderer: renderers) {
			final Track<?> track = renderer.getTrack();
			// genome track is rendered in the swing thread so skip rendering it here
			if ("Genome".equals(track.getName())) continue;

			final RenderingContext context = new RenderingContext(frame, renderer, filter, this, offScreenImage);
			queue.add(new Runnable() {
				public void run() {
					if (frame!=getCurrentFrame()) return;
					track.featuresAsync(filter, new FeatureCallback(context));
				}
			});

//			// queue block keys for all visible blocks of track data
//			else if (track instanceof BlockTrack) {
//				BlockIndex index = ((BlockTrack)track).getBlockIndex();
//				for (Strand strand: Strand.all) {
//					for (BlockKey key : index.blocks(sequence.getSeqId(), strand, start, end)) {
//						queue.add(new BlockDataTask(frame, key, this, blockDataSource, component, renderer));
//					}
//				}
//			}

			// for regular (non-block based) tracks, queue up a TrackDataTask with the appropriate filter
//			else {
//				for (Strand strand: Strand.all) {
//					queue.add(new TrackDataTask(frame, this, component, renderer, track, new FeatureFilter(sequence, strand, start, end)));
//				}
//			}
		}
		
		queue.add(new Runnable() {
			public void run() {
				if (frame!=getCurrentFrame()) return;
				panel.updateImage(offScreenImage);
				
				// rendering timing
				//log.info("frame rendered in " + (System.currentTimeMillis()-startMillis) + " milliseconds");
			}
		});
//		queue.add(new Runnable() {
//			public void run() {
//				try {
//					SwingUtilities.invokeAndWait(new Runnable() {
//						public void run() {
//							if (frame!=getCurrentFrame()) return;
//							panel.updateImage(offScreenImage);
//						}
//					});
//				} catch (Exception e) {
//					log.warn(e);
//				}
//			}
//		});
	}

	public void viewParametersChanged(ViewParameters p) {
		schedule(trackManager, p.getSequence(), p.getStart(), p.getEnd());
	}
}

class TaskRunner implements Runnable {
	private static final Logger log = Logger.getLogger(TaskRunner.class);
	private boolean done = false;
	private BlockingQueue<Runnable> queue;

	// dependency
	public void setQueue(BlockingQueue<Runnable> queue) {
		this.queue = queue;
	}

	public void shutdown() {
		done = true;
	}

	public void run() {
		while (!done) {
			try {
				Runnable task = queue.take();

				try {
					// TODO figure out if invoke and wait is what i want here.
//					SwingUtilities.invokeAndWait(task);
					task.run();
				}
				catch (Exception e) {
					log.warn("Exception in task: ", e);
				}

				if (Thread.interrupted()) {
					done = true;
				}
			}
			catch (InterruptedException e) {
				done = true;
			}
		}
	}
}