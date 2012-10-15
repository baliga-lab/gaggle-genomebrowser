package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ui.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.util.HasSelections;
import org.systemsbiology.util.Selectable;

// TODO replace with TypedGeneTrackRenderer

/**
 * Renders labeled features as colored blocks with a label if it fits. Used for protein coding genes and RNA "genes".
 * @author cbare
 */
@SuppressWarnings("unchecked")
public class GeneTrackRenderer extends TrackRenderer implements HasSelections, HasTooltips {
	Color forwardStrandColor = Color.YELLOW;
	Color reverseStrandColor = Color.ORANGE;
	Color noStrandColor = Color.PINK;
	int h = 10;
	GeneFeatureImpl mouseOverGene;
	Rectangle mouseOverRectangle = new Rectangle();


	public GeneTrackRenderer() {}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y, w;

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0xFF0000));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}	     

		int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);

        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)features) {
    		x = params.toScreenX(feature.getStart());
    		w = Math.max(1, (int) ((feature.getEnd() - feature.getStart()) * params.getScale()) );

    		// forward strand
    		if (feature.getStrand() == Strand.forward) {
    			g.setColor(forwardStrandColor);
        		y = yc - 10 - h;
    		}
    		// reverse strand
    		else if (feature.getStrand() == Strand.reverse) {
    			g.setColor(reverseStrandColor);
        		y = yc + 10;
    		}
    		// no strand information
    		else {
    			g.setColor(noStrandColor);
    			y = yc - (int)(h/2);
    		}

    		g.fillRect(x, y, w, h);
    		
    		if (feature instanceof Selectable && ((Selectable)feature).selected()) {
    			g.setColor(Color.RED);
    			g.drawRect(x, y, w, h);
    		}

    		if (w > 30) {
    			g.setColor(Color.BLACK);
				Font f = g.getFont();
				Font f9 = f.deriveFont(9.0F);
				g.setFont(f9);
   
    			Rectangle2D r = f9.getStringBounds(feature.getLabel(), ((Graphics2D)g).getFontRenderContext());
    			if (w > r.getWidth())
    				g.drawString(feature.getLabel(), x, y+h);
				g.setFont(f);
    		}
        }
	}

	public String getTooltip(int x, int y) {
		GeneFeatureImpl feature = getGeneAt(x,y);
		if (feature != null) {
			return feature.getNameAndCommonName();
		}
		else {
			return null;
		}
	}

	/**
	 * returns the gene at screen coordinates x,y or null
	 * if no gene is at those coordinates.
	 */
	public GeneFeatureImpl getGeneAt(int x, int y) {
		if (track instanceof Track.Gene) {
			Track.Gene<GeneFeatureImpl> geneTrack = (Track.Gene<GeneFeatureImpl>)track;
			int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
			int yf = yc - h - 10;
			if (y >= yf && y <= (yf + h)) {
				int coord = (int)(x / params.getScale()) + params.getStart();
				return geneTrack.getFeatureAt(params.getSequence(), Strand.forward, coord);
			}
			int yr = yc + 10;
			if (y >= yr && y <= (yr + h)) {
				int coord = (int)(x / params.getScale()) + params.getStart();
				return geneTrack.getFeatureAt(params.getSequence(), Strand.reverse, coord);
			}
		}
		return null;
	}

	public void setForwardStrandColor(Color color) {
		this.forwardStrandColor = color;
	}

	public void setReverseStrandColor(Color color) {
		this.reverseStrandColor = color;
	}

	public void setNoStrandColor(Color noStrandColor) {
		this.noStrandColor = noStrandColor;
	}

	public List<Feature> getContainedFeatures(Sequence s, Rectangle r) {
		int yf = (int) (params.getDeviceHeight() * top) + 5;
		int yr = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height) - h - 5;
		List<Strand> strands = new ArrayList<Strand>();

		if (r.y < yf && r.y + r.height > yf) {
			// select genes in the forward strand
			strands.add(Strand.forward);
		}
		if (r.y < yr && r.y + r.height > yr) {
			// select genes in the reverse strand
			strands.add(Strand.reverse);
		}

//		System.out.println("yr = " + yr);
//		System.out.println("fwd = " + fwd);
//		System.out.println("rev = " + rev);

		if (strands.size() > 0) {
			List<Feature> features = new ArrayList<Feature>();
			int x1 = params.toGenomeCoordinate(r.x);
			int x2 = params.toGenomeCoordinate(r.x+r.width);
			for (Strand strand: strands) {
		        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)track.features(new FeatureFilter(s, strand, x1, x2))) {
					//System.out.println(feature);
					if (feature.getStart() >= x1 && feature.getEnd() <= x2) {
						features.add(feature);
					}
		        }
			}
			return features;
		}
		return Collections.emptyList();
	}

	public void deselect() {
        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)track.features()) {
			if (feature instanceof Selectable) {
				Selectable selectable = (Selectable)feature;
				selectable.setSelected(false);
			}
		}
	}

	public List<String> getSelections() {
		List<String> selections = new ArrayList<String>();
        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)track.features()) {
			if (feature instanceof Selectable) {
				Selectable selectable = (Selectable)feature;
				if (selectable.selected()) {
					selections.add(feature.getName());
				}
			}
		}
		return selections;
	}
}
