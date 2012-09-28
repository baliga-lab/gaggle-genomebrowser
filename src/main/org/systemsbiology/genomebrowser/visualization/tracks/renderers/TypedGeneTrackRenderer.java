package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ui.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.ColorUtils;
import org.systemsbiology.util.HasSelections;
import org.systemsbiology.util.Hyperlink;
import org.systemsbiology.util.Pair;
import org.systemsbiology.util.Selectable;


// TODO this works in an odd way: see offset.
// TODO should position boxes relative to outside of bounding box (using top and height rather than center).
// TODO see CenteredTypedGeneTrackRenderer

/**
 * Renders labeled features as colored blocks with a label if it fits. Used for protein coding genes and RNA "genes".
 * @author cbare
 */
@SuppressWarnings("unchecked")
public class TypedGeneTrackRenderer extends TrackRenderer implements HasTooltips, HasSelections {
	Color forwardStrandColor = Color.YELLOW;
	Color reverseStrandColor = Color.ORANGE;
	Color noStrandColor = Color.PINK;
	HashMap<Pair<Strand, GeneFeatureType>, Color> colorMap = new HashMap<Pair<Strand, GeneFeatureType>, Color>();
	GeneFeatureImpl mouseOverGene;
	Rectangle mouseOverRectangle = new Rectangle();
	boolean useCommonNames;
	
	// height of box in pixels
	final int h = 10;
	
	// offset from center of track to inner edge of box.
	int offset = 15;


	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		forwardStrandColor = this.color;
		reverseStrandColor = ColorUtils.darker(this.color, 0.10);
		initColorMap();
		offset = track.getAttributes().getInt("offset", offset);
		useCommonNames = track.getAttributes().getBoolean("use.common.names", true);
	}

	private void initColorMap() {
		setColor(Strand.forward, GeneFeatureType.gene, Color.YELLOW);
		setColor(Strand.reverse, GeneFeatureType.gene, Color.ORANGE);
		setColor(Strand.forward, GeneFeatureType.cds, Color.YELLOW);
		setColor(Strand.reverse, GeneFeatureType.cds, Color.ORANGE);
		setColor(Strand.forward, GeneFeatureType.rrna, new Color(0xCD5555));
		setColor(Strand.reverse, GeneFeatureType.rrna, new Color(0xFF6A6A));
		setColor(Strand.forward, GeneFeatureType.trna, new Color(0xCD5555));
		setColor(Strand.reverse, GeneFeatureType.trna, new Color(0xFF6A6A));
		setColor(Strand.forward, GeneFeatureType.rna, new Color(0xCD5555));
		setColor(Strand.reverse, GeneFeatureType.rna, new Color(0xFF6A6A));
		setColor(Strand.forward, GeneFeatureType.repeat, new Color(0x55CC55));
		setColor(Strand.reverse, GeneFeatureType.repeat, new Color(0x6aDD6A));
		setColor(Strand.forward, GeneFeatureType.ncrna, new Color(0x60DD00DD));
		setColor(Strand.reverse, GeneFeatureType.ncrna, new Color(0x60EE00EE));
		setColor(Strand.forward, GeneFeatureType.pfam, new Color(0x605086AA));
		setColor(Strand.reverse, GeneFeatureType.pfam, new Color(0x604C7FA1));
	}

	private Color getColor(GeneFeatureImpl feature) {
		Color color = colorMap.get(new Pair<Strand, GeneFeatureType>(feature.getStrand(), feature.getType()));
		if (color==null) {
			switch (feature.getStrand()) {
			case forward:
				return forwardStrandColor;
			case reverse:
				return reverseStrandColor;
			default:
				return noStrandColor;
			}
		}
		return color;
	}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y, w;

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0xFF0000));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

		int yCenter = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);

        for (GeneFeatureImpl feature : (Iterable<GeneFeatureImpl>)features) {
    		x = params.toScreenX(feature.getStart());
    		w = Math.max(1, (int) ((feature.getEnd() - feature.getStart()) * params.getScale()) );
    		
    		g.setColor(getColor(feature));

    		// forward strand
    		if (feature.getStrand() == Strand.forward) {
        		y = yCenter - offset - h;
    		}
    		// reverse strand
    		else if (feature.getStrand() == Strand.reverse) {
        		y = yCenter + offset;
    		}
    		// no strand information
    		else {
    			y = yCenter - (int)(h/2);
    		}

    		g.fillRect(x, y, w, h);

    		if (feature.selected()) {
    			g.setColor(Color.RED);
    			g.drawRect(x, y, w, h);
    		}

    		if (w > 30 && feature.getLabel() != null) {
    			g.setColor(Color.BLACK);
				Font f = g.getFont();
				Font f9 = f.deriveFont(9.0F);
				g.setFont(f9);
				
				String label = useCommonNames ? feature.getLabel() : feature.getName();
    			Rectangle2D r = f9.getStringBounds(label, ((Graphics2D)g).getFontRenderContext());
    			if (w > r.getWidth())
    				g.drawString(label, x, y+h);
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
			int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
			int yf = yc - h - offset;
			if (y >= yf && y <= (yf + h)) {
				int coord = (int)(x / params.getScale()) + params.getStart();
				return geneTrack.getFeatureAt(params.getSequence(), Strand.forward, coord);
			}
			int yr = yc + offset;
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

	public void setColor(Strand strand, GeneFeatureType type, Color color) {
		colorMap.put(new Pair<Strand, GeneFeatureType>(strand, type), color);
	}

	public List<Feature> getContainedFeatures(Sequence s, Rectangle r) {
		// y of forward strand
		int yf = (int) (params.getDeviceHeight() * top) + 5;
		// y of reverse strand
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

	public List<Hyperlink> getLinks(int x, int y) {
		GeneFeatureImpl feature = getGeneAt(x,y);
		if (feature == null) {
			return Collections.emptyList();
		}
		else {
			List<Hyperlink> links = new ArrayList<Hyperlink>();
//			if (feature.getType() == GeneFeatureType.cds) {
//				links.add(new Hyperlink("Lookup " + feature.getNameAndCommonName() + " in Pfam", "url"));
//			}
			if (feature.getType() == GeneFeatureType.pfam) {
				links.add(new Hyperlink("Lookup " + feature.getNameAndCommonName() + " in Pfam", 
						"http://pfam.sanger.ac.uk/family/" + feature.getName()));
			}
			return links;
		}
	}
}
