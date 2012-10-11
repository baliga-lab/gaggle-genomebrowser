package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Graphics;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.ScalingQuantitativeIteratable;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * A track that groups together points into blocks. As we zoom
 * out, the blocks aggregate more individual data points, which
 * makes drawing a lot more efficient and doesn't loose much if
 * the resolution of the points is smaller than a pixel.
 */
public class ScalingTrackRenderer extends QuantitativeTrackRenderer {
	private static final Logger log = Logger.getLogger(ScalingTrackRenderer.class);

	/**
	 * average distance between data points
	 */
	double aveDistanceBps = 20.0;

	/**
	 * density of data points in datapoints per base pair
	 */
	double dataPointDensity = 0.05;

	/**
	 * average width of a data point.
	 */
	double aveWidth = 60.0;



	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		initBlockScaling(attr);
	}

	/**
	 * We need to find out how far apart the data points are
	 * so we can configure scaling.
	 */
	@SuppressWarnings("unchecked")
	private void initBlockScaling(Attributes attr) {
		Iterator<? extends Feature.Quantitative> iter = (Iterator<? extends Feature.Quantitative>)track.features();
		if (iter.hasNext()) {
			Feature.Quantitative feature = iter.next();
			int prevCenter = feature.getCentralPosition();
			int i = 1;
			double distanceBetweenCenters = 0.0;
			double sumOfWidths = 0.0;
			int count = 0;
			int start = feature.getStart();
			int end = feature.getEnd();
			int replicates = 0;
			String seqId = feature.getSeqId();
			Strand strand = feature.getStrand();
			while (iter.hasNext()) {
				feature = iter.next();

				// end after first sequence and strand are done
				if (!seqId.equals(feature.getSeqId()) || strand!=feature.getStrand())
					break;

				// sometimes people put more than one replicate in a single
				// track (ex. the 500bp tiling array)
				if (feature.getCentralPosition() == prevCenter) {
					replicates++;
				}
				else {
					distanceBetweenCenters += feature.getCentralPosition() - prevCenter;
					prevCenter = feature.getCentralPosition();
					i++;
				}

				end = Math.max(end, feature.getEnd());
				sumOfWidths += (feature.getEnd() - feature.getStart() + 1);
				count++;
				
				if (count > 10000) break;
			}

			// this computes average distance between centers of data points
			// it excludes replicates (points with the same coordinates)
			aveDistanceBps = distanceBetweenCenters / i;
			
			// In units of Data points per base pair.
			dataPointDensity = ((double)count) / ((double)(end - start + 1));
			log.debug("count, start, end = " + count + ", " + start + ", " + end);
			log.debug("dataPointDensity = " + dataPointDensity);
			// ok  dataPointDensity = 0.9999498796537793
			// bad dataPointDensity = 181666.0
			
			aveWidth = sumOfWidths / count;
			
//			double aveReplicates = ((double)count) / (count - replicates);

//			System.out.println("-------------------------------------------------------------------");
//			System.out.println(track.getName()
//					+ "\n   sequence= " + seqId
//					+ "\n   strand= " + strand
//					+ "\n   aveDistanceBps=" + aveDistanceBps
//					+ "\n   dataPointDensity=" + dataPointDensity 
//					+ "\n   aveWidth=" + aveWidth
//					+ "\n   aveReplicates=" + aveReplicates
//					+ "\n   replicates=" + replicates
//					+ "\n   end=" + end);
//			System.out.println("-------------------------------------------------------------------");
			
		}
		else {
			log.warn("initBlockScaling found no data points");
		}
	}

	
	private int scaleBlockSize(double dataPointsPerPixel) {
		return Math.max(1, ((int)Math.floor(dataPointsPerPixel/2.0)) * 2);
	}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		double top = (strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;
		double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);

		@SuppressWarnings("unchecked")
		Iterable<Feature.Quantitative> quantitativeFeatures = (Iterable<Feature.Quantitative>)features;

		// pnts/pixel <- pnts/bp * bp/pixel <- pnts per bp / pixels per bp
		double dataPointsPerPixel = dataPointDensity / params.getScale();

		double aveWidthPixels = (aveWidth * params.getScale());

		// outline is a debugging tool to show where the boundaries of the track are
//		if (outline) {
//			int _t = (int) (top * params.height);
//			int _h = (int) (height * params.height);
//			g.setColor(new Color(0x3300FF));
//			g.drawRect(0, _t+1, params.width-1, _h-1);
//		}

		g.setColor(color);

		// compress multiple data points into blocks shown by a
		// vertical bar that represents the range of values in each
		// block.
		if (dataPointsPerPixel >= 4.0) {
			int blockSize = scaleBlockSize(dataPointsPerPixel);
			for (Feature.ScaledQuantitative feature : new ScalingQuantitativeIteratable(quantitativeFeatures, blockSize)) {
				int x = params.toScreenX(feature.getCentralPosition());
				int min = (int) (y0 - feature.getMin() * yScale);
				int max = (int) (y0 - feature.getMax() * yScale);
				g.drawLine(x,min,x,max);
			}
		}
		
		// TODO add parameters scale.by.probe.width and scale.by.probe.separation

		// if we are sufficiently zoomed in, show an
		// "I-beam" that represents the size of the probe
		else if (aveWidthPixels > 9.0) {
			for (Feature.Quantitative feature : quantitativeFeatures) {
				int y = (int) (y0 - feature.getValue() * yScale);
				int s = params.toScreenX(feature.getStart());
				int e = params.toScreenX(feature.getEnd());
				g.drawLine(s, y, e, y);
				g.drawLine(s, y-2, s, y+2);
				g.drawLine(e, y-2, e, y+2);
			}
		}

		// otherwise, plot a circle for each data point
		else {
			int ccc = 0;
			for (Feature.Quantitative feature : quantitativeFeatures) {
				int x = params.toScreenX(feature.getCentralPosition());
				int y = (int) (y0 - feature.getValue() * yScale);
				g.drawOval(x-2, y-2, 4, 4);
				ccc++;
			}
			// System.out.println("scaling track renderer ----  rendered: " + ccc);
		}
	}
}
