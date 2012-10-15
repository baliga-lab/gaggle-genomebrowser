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

import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ui.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.ColorUtils;
import org.systemsbiology.util.HasSelections;
import org.systemsbiology.util.Selectable;


// TODO where is this used?

/**
 * Renders labeled features as colored blocks with a label if it fits. Used for protein coding genes and RNA "genes".
 * @author cbare
 */
@SuppressWarnings("unchecked")
public class CenteredTypedGeneTrackRenderer extends TrackRenderer implements HasTooltips, HasSelections {
	Color forwardStrandColor = Color.YELLOW;
	Color reverseStrandColor = Color.ORANGE;
	Color noStrandColor = Color.PINK;
	GeneFeatureImpl mouseOverGene;
	Rectangle mouseOverRectangle = new Rectangle();
	
	// height of boxes in pixels
	final int boxHeight = 10;
	
	// offset for reverse strand to make way for ais labels
	final int offset = 15;


	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		if (attr.containsKey("color.forward") || attr.containsKey("color.reverse") || attr.containsKey("color.no.strand")) {
			setForwardStrandColor(attr.getColor("color.forward", forwardStrandColor));
			setReverseStrandColor(attr.getColor("color.reverse", reverseStrandColor));
			setNoStrandColor(attr.getColor("color.no.strand", noStrandColor));
		}
		else {
			Color color = attr.getColor("color");
			setForwardStrandColor(attr.getColor("color.forward", ColorUtils.lighter(color, 0.1)));
			setReverseStrandColor(attr.getColor("color.reverse", ColorUtils.darker(color, 0.1)));
			setNoStrandColor(attr.getColor("color.no.strand", color));
		}
	}


	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y, w;
		int _t = (int) (top * params.getDeviceHeight());
		int _h = (int) (height * params.getDeviceHeight());

		if (outline) {
			g.setColor(new Color(0xFF0000));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1+offset);
		}	     

        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)features) {
    		x = params.toScreenX(feature.getStart());
    		w = Math.max(1, (int) ((feature.getEnd() - feature.getStart()) * params.getScale()) );

    		// forward strand
    		if (feature.getStrand() == Strand.forward) {
        		y = _t + offset;
        		g.setColor(forwardStrandColor);
    		}
    		// reverse strand
    		else if (feature.getStrand() == Strand.reverse) {
        		y = _t + _h - boxHeight;
        		g.setColor(reverseStrandColor);
    		}
    		// no strand information
    		else {
    			y = (int) (_t + params.getDeviceHeight() * height / 2 - boxHeight/2);
        		g.setColor(noStrandColor);
    		}

    		g.fillRect(x, y, w, boxHeight);

    		if (feature.selected()) {
    			g.setColor(Color.RED);
    			g.drawRect(x, y, w, boxHeight);
    		}

    		if (w > 30 && feature.getLabel() != null) {
    			g.setColor(Color.BLACK);
				Font f = g.getFont();
				Font f9 = f.deriveFont(9.0F);
				g.setFont(f9);
   
    			Rectangle2D r = f9.getStringBounds(feature.getLabel(), ((Graphics2D)g).getFontRenderContext());
    			if (w > r.getWidth())
    				g.drawString(feature.getLabel(), x, y+boxHeight);
				g.setFont(f);
    		}
        }
	}

	/**
	 * returns the gene at screen coordinates x,y or null
	 * if no gene is at those coordinates.
	 */
	public GeneFeatureImpl getGeneAt(int x, int y) {
		if (track instanceof Track.Gene) {
			Track.Gene<GeneFeatureImpl> geneTrack = (Track.Gene<GeneFeatureImpl>)track;
			int _t = (int) (top * params.getDeviceHeight());

			int yf = _t + offset;
			if (y >= yf && y <= (yf + boxHeight)) {
				int coord = (int)(x / params.getScale()) + params.getStart();
				return geneTrack.getFeatureAt(params.getSequence(), Strand.forward, coord);
			}

			int _h = (int) (height * params.getDeviceHeight());
			int yr = _t + _h - boxHeight;
			if (y >= yr && y <= (yr + boxHeight)) {
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
		int yr = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height) - boxHeight - 5;
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
			if (feature.selected()) {
				selections.add(feature.getName());
			}
		}
		return selections;
	}

	public String getTooltip(int x, int y) {
		GeneFeatureImpl feature = getGeneAt(x,y);
		if (feature == null) {
			return null;
		}
		else {
			return feature.getNameAndCommonName();
		}
	}
}
