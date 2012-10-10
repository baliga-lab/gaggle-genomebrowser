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

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.PeptideFeature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.ui.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.ColorUtils;
import org.systemsbiology.util.HasSelections;
import org.systemsbiology.util.Hyperlink;
import org.systemsbiology.util.MathUtils;
import org.systemsbiology.util.Selectable;

// TODO nice to have peptide features:
// find redundant/degenerate peptides. If I'm on one, find the others.
// select peptides
// ability to copy/paste peptide sequence
// translate DNA-protein
// right-click to see which fractions are represented (or a pop-up dialog?)
// peptides link to?



/**
 * Renders labeled features as colored blocks with a label if it fits.
 * 
 * GeneTrackRenderer modified to draw peptides with transparency determined
 * by score (probability, p-value, etc.)
 *
 * @author cbare
 */
@SuppressWarnings("unchecked")
public class PeptideTrackRenderer extends TrackRenderer implements HasTooltips, HasSelections {
	private static final Logger log = Logger.getLogger(PeptideTrackRenderer.class);
	//private GeneFeature mouseOverGene;
	//private Rectangle mouseOverRectangle = new Rectangle();
	private boolean useCommonNames;
	private double scoreAdjustor;

	// height of box in pixels
	final int h = 10;

	// offset from center of track to inner edge of box.
	int offset = 15;


	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		offset = track.getAttributes().getInt("offset", offset);
		useCommonNames = track.getAttributes().getBoolean("use.common.names", true);
		scoreAdjustor = track.getAttributes().getDouble("score.adjustor", 0.0);
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

		for (PeptideFeature feature : (Iterable<PeptideFeature>)features) {
			//log.info(track.getName() + ": " + feature.toString());
			x = params.toScreenX(feature.getStart());
			w = Math.max(1, (int) ((feature.getEnd() - feature.getStart()) * params.getScale()) );

			g.setColor(ColorUtils.deriveTransparentColorFrom(color, MathUtils.clip(feature.getScore() + scoreAdjustor, 0.0, 1.0)));

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

			//  should peptides be selectable?
//			if (feature.selected()) {
//				g.setColor(Color.RED);
//				g.drawRect(x, y, w, h);
//			}

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
	public GeneFeature getGeneAt(int x, int y) {
		//log.info("PeptideTrackRenderer.getGeneAt(" + x + ", " + y + ")");
		if (track instanceof Track.Gene) {
			Track.Gene<GeneFeature> geneTrack = (Track.Gene<GeneFeature>)track;
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

	public void deselect() {
		for (GeneFeature feature : (Iterable<GeneFeature>)track.features()) {
			if (feature instanceof Selectable) {
				Selectable selectable = (Selectable)feature;
				selectable.setSelected(false);
			}
		}
	}

	public List<String> getSelections() {
		return Collections.emptyList();
//		List<String> selections = new ArrayList<String>();
//		for (GeneFeature feature : (Iterable<GeneFeature>)track.features()) {
//			if (feature.selected()) {
//				selections.add(feature.getName());
//			}
//		}
//		return selections;
	}

	public String getTooltip(int x, int y) {
		GeneFeature feature = getGeneAt(x,y);
		if (feature == null) {
			return null;
		}
		else {
			if (feature instanceof PeptideFeature) {
				int redundancy = ((PeptideFeature)feature).getRedundancy();
				if (redundancy > 0)
					return feature.getName() + "(" + redundancy + ")";
				else
					return feature.getName();
			}
			else
				return feature.getName();
		}
	}

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
				links.add(new Hyperlink("Lookup " + feature.getName() + " in Pfam", 
						"http://pfam.sanger.ac.uk/family/" + feature.getName()));
			}
			return links;
		}
	}
}
