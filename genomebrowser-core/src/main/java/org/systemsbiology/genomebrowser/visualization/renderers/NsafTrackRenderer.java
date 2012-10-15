package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.NsafFeature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.visualization.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.ColorUtils;
import org.systemsbiology.util.HasSelections;
import org.systemsbiology.util.Hyperlink;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.Pair;
import org.systemsbiology.util.Selectable;


/**
 * Renders labeled features as colored blocks with a label if it fits. Used for protein coding genes and RNA "genes".
 * @author cbare
 */
@SuppressWarnings("unchecked")
public class NsafTrackRenderer extends TrackRenderer implements HasTooltips {
	Color color = new Color(0x80880088);
	HashMap<Pair<Strand, GeneFeatureType>, Color> colorMap = new HashMap<Pair<Strand, GeneFeatureType>, Color>();
	GeneFeature mouseOverGene;
	Rectangle mouseOverRectangle = new Rectangle();
	boolean useCommonNames;
	
	// height of bar in pixels
	final int h = 5;
	
	// offset from center of track to inner edge of box.
	int offset = 15;
	Color[] colors;


	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		initColorMap();
		offset = track.getAttributes().getInt("offset", offset);
		useCommonNames = track.getAttributes().getBoolean("use.common.names", true);

		colors = new Color[6];
		colors[0] = new Color(0xFF0000FF);
		colors[1] = new Color(0xFFFF0000);
		colors[2] = colors[1];
		colors[3] = colors[1];
		colors[4] = colors[1];
		colors[5] = new Color(0xFF00FF00);
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

	private Color getColor(GeneFeature feature) {
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

        for (NsafFeature feature : (Iterable<NsafFeature>)features) {
    		x = params.toScreenX(feature.getStart());
    		w = Math.max(1, (int) ((feature.getEnd() - feature.getStart()) * params.getScale()) );
    		
    		float[] nsaf = feature.getNsaf();
    		if (nsaf==null) {
    			continue;
    		}

    		// forward strand
    		if (feature.getStrand() == Strand.forward) {
        		y = yCenter - offset - h * nsaf.length;
    		}
    		// reverse strand
    		else if (feature.getStrand() == Strand.reverse) {
        		y = yCenter + offset;
    		}
    		// no strand information
    		else {
    			y = yCenter - (int)((h * nsaf.length)/2);
    		}

    		boolean nonzero = false;
    		
    		for (int i=0; i<nsaf.length; i++) {
    			if (nsaf[i] > 0.0) {
	    			g.setColor(ColorUtils.deriveTransparentColorFrom(colors[i], 1000.0 * Math.log10(nsaf[i] + 1.0)));
	    			g.fillRect(x, y+(h*i), w, h);
	    			nonzero = true;
    			}
    		}

    		if (nonzero) {
	    		if (feature.isDegenerate()) {
	    			g.setColor(Color.RED);
	    		}
	    		else {
	    			g.setColor(Color.GRAY);
	    		}
	    		g.drawRect(x, y-1, w, h*nsaf.length+1);
    		}

    		// draw label if it fits
//    		if (w > 30 && feature.getLabel() != null) {
//    			g.setColor(Color.BLACK);
//				Font f = g.getFont();
//				Font f9 = f.deriveFont(9.0F);
//				g.setFont(f9);
//				
//				String label = useCommonNames ? feature.getLabel() : feature.getName();
//    			Rectangle2D r = f9.getStringBounds(label, ((Graphics2D)g).getFontRenderContext());
//    			if (w > r.getWidth())
//    				g.drawString(label, x, y+h);
//				g.setFont(f);
//    		}
        }
	}

	/**
	 * returns the gene at screen coordinates x,y or null
	 * if no gene is at those coordinates.
	 */
	private GeneFeature getGeneAt(int x, int y) {
		if (track instanceof Track.Gene) {
			Track.Gene<GeneFeature> geneTrack = (Track.Gene<GeneFeature>)track;
			int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
			int yf = yc - h * colors.length - offset;
			if (y >= yf && y <= (yc - offset)) {
				int coord = params.toGenomeCoordinate(x);
				return geneTrack.getFeatureAt(params.getSequence(), Strand.forward, coord);
			}
			int yr = yc + offset;
			if (y >= yr && y <= (yr + h * colors.length)) {
				int coord = params.toGenomeCoordinate(x);
				return geneTrack.getFeatureAt(params.getSequence(), Strand.reverse, coord);
			}
		}
		return null;
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
		        for (GeneFeature feature : (Iterable<GeneFeature>)track.features(new FeatureFilter(s, strand, x1, x2))) {
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

//	public void deselect() {
//        for (GeneFeature feature : (Iterable<GeneFeature>)track.features()) {
//			if (feature instanceof Selectable) {
//				Selectable selectable = (Selectable)feature;
//				selectable.setSelected(false);
//			}
//		}
//	}

//	public List<String> getSelections() {
//		List<String> selections = new ArrayList<String>();
//        for (GeneFeature feature : (Iterable<GeneFeature>)track.features()) {
//			if (feature.selected()) {
//				selections.add(feature.getName());
//			}
//		}
//		return selections;
//	}

	public String getTooltip(int x, int y) {
		GeneFeature feature = getGeneAt(x,y);
		if (feature == null) {
			return null;
		}
		else {
			if (feature.getStrand() == Strand.forward) {
				int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
				int yf = yc - h * colors.length - offset;
				int i = (y - yf) / h;
				if (i>=0 && i<6)
					return String.format("%1.3e", ((NsafFeature)feature).getNsaf()[i]);
			}
			else if (feature.getStrand() == Strand.reverse) {
				int yc = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
				int yr = yc + offset;
				int i = (y - yr) / h;
				if (i>=0 && i<6)
					return String.format("%1.3e", ((NsafFeature)feature).getNsaf()[i]);
			}
			return feature.getName() + (feature.getCommonName()==null ? "" : (" " + feature.getCommonName()));
		}
	}
	
	
	// TODO should these link somewhere?

	public List<Hyperlink> getLinks(int x, int y) {
		GeneFeature feature = getGeneAt(x,y);
		if (feature == null) {
			return Collections.emptyList();
		}
		else {
			List<Hyperlink> links = new ArrayList<Hyperlink>();
//			if (feature.getType() == GeneFeatureType.cds) {
//				links.add(new Hyperlink("Lookup " + feature.getNameAndCommonName() + " in Pfam", "url"));
//			}
			if (feature.getType() == GeneFeatureType.pfam) {
				links.add(new Hyperlink("Lookup " + feature.getName() + (feature.getCommonName()==null ? "" : (" " + feature.getCommonName())) + " in Pfam", 
						"http://pfam.sanger.ac.uk/family/" + feature.getName()));
			}
			return links;
		}
	}
}
