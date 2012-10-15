package org.systemsbiology.genomebrowser.visualization.tracks;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.NsafFeature;
import org.systemsbiology.genomebrowser.model.PeptideFeature;
import org.systemsbiology.genomebrowser.model.ScoredNamedFeature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.genomebrowser.visualization.ColorScaleRegistry;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.visualization.renderers.*;

/**
 * Maps tracks to track renderers in a configurable manner.
 */
@SuppressWarnings("unchecked")
public class TrackRendererRegistry {
	private static final Logger log = Logger.getLogger(TrackRendererRegistry.class);
	
	/**
	 * Maps the name of a renderer to its description, which it's class along with
	 * the track and feature subtype it works with.
	 */
	private Map<String, RendererDescription> rendererMap = new HashMap<String, RendererDescription>();
	
	/**
	 * Maps track types to names of applicable renderers. Track types are strings such as "quantitative.segment",
	 * "quantitative.positional", or "gene". These are used by the embedded DB and by the
	 * track import wizard to identify the types of features held by a track.
	 */
	private Map<String, List<String>> trackTypeToRenderersMap = new java.util.HashMap<String, List<String>>();

	private ViewParameters viewParameters;
	private ColorScaleRegistry colorScaleRegistry;


	/**
	 * TrackRendererRegistry injects the ViewParameters object into each
	 * renderer it creates. Seems slightly out of place here, but necessary.
	 */
	public void setViewParameters(ViewParameters viewParameters) {
		this.viewParameters = viewParameters;
	}

	/**
	 * @param name name of the renderer
	 * @param rendererClass
	 * @param trackClass
	 * @param featureClass
	 */
	public void registerRenderer(String name, Class<? extends TrackRenderer> rendererClass, Class<? extends Track> trackClass, Class<? extends Feature> featureClass) {
		rendererMap.put(name, new RendererDescription(name, 
				(Class<? extends TrackRenderer>)rendererClass, 
				(Class<? extends Track<? extends Feature>>)trackClass,
				(Class<? extends Feature>)featureClass));
	}

	/**
	 * A renderer may declare what track and feature type it requires through
	 * static methods called "_getTrackType" and "_getFeatureType".
	 * @param name
	 * @param rendererClass
	 */
	public void registerRenderer(String name, Class<?> rendererClass) {
		rendererMap.put(name, new RendererDescription(name,
				(Class<? extends TrackRenderer>)rendererClass,
				(Class<? extends Track<? extends Feature>>)getTrackType((Class<? extends TrackRenderer>)rendererClass),
				getFeatureType((Class<? extends TrackRenderer>)rendererClass)));
	}

	private Class<? extends Track> getTrackType(Class<? extends TrackRenderer> rendererClass) {
		try {
			Method m = rendererClass.getDeclaredMethod("_getTrackType");
			if (m != null && (m.getModifiers() & Modifier.STATIC) > 0) {
				Object result = m.invoke(null);
				if (result instanceof Class)
					return (Class<? extends Track<? extends Feature>>)result;
			}
		}
		catch (Exception e) {
			log.error("Error getting track and feature type information from renderer: " + rendererClass.getName());
		}
		return Track.class;
	}

	private Class<? extends Feature> getFeatureType(Class<? extends TrackRenderer> rendererClass) {
		try {
			Method m = rendererClass.getDeclaredMethod("_getFeatureType");
			if (m != null && (m.getModifiers() & Modifier.STATIC) > 0) {
				Object result = m.invoke(null);
				if (result instanceof Class)
					return (Class<? extends Feature>)result;
			}
		}
		catch (Exception e) {
			log.error("Error getting track and feature type information from renderer: " + rendererClass.getName());
		}
		return (Class<? extends Feature>)Feature.class;
	}

	public void registerRenderer(String name, String rendererClassName, String trackClassName, String featureClassName) {
		Class<? extends TrackRenderer> rendererClass;
		Class<? extends Track> trackClass;
		Class<? extends Feature> featureClass;
		try {
			rendererClass = (Class<? extends TrackRenderer>)Class.forName(rendererClassName);
			trackClass = (Class<? extends Track>)Class.forName(trackClassName);
			featureClass = (Class<? extends Feature>)Class.forName(featureClassName);
			registerRenderer(name, rendererClass, trackClass, featureClass);
		}
		catch (ClassNotFoundException e) {
			throw new TypeNotPresentException(trackClassName, e);
		}
	}

	/**
	 * Return a list of the names of all renders.
	 */
	public List<String> getRendererNames() {
		List<String> rendererNames = new ArrayList<String>(rendererMap.keySet());
		Collections.sort(rendererNames);
		return rendererNames;
	}

	/**
	 * The name of the required renderer is assumed to be in the track's attributes. 
	 * @return A new instance of the appropriate renderer for the given track.
	 */
	public TrackRenderer createTrackRenderer(Track<? extends Feature> track) {
		if (track == null) {
			throw new NullPointerException("Can't create track renderer: Track is null!");
		}

		String view = track.getAttributes().getString("viewer");

		// TODO change viewer to renderer

		// defaulting
		if (view == null) {
			if (track instanceof Track.Gene)
				view = "Gene";
			else if (track instanceof Track.Quantitative)
				view = "Bubble";
			else
				// finally default to a super general renderer
				view = "Triangle marker";
		}

		RendererDescription record = rendererMap.get(view);

		if (record==null)
			throw new RuntimeException("Unrecognized track renderer type: \"" + view + "\"");

		try {
			TrackRenderer renderer = record.rendererClass.newInstance();
			
			// set colorScaleRegistry if a setter exists
			try {
				Method setColorScale = renderer.getClass().getMethod("setColorScaleRegistry", ColorScaleRegistry.class);
				setColorScale.invoke(renderer, colorScaleRegistry);
			}
			catch (Exception e) {
				// no setColorScaleRegistry method which is OK.
			}

			// finish configuring renderer
			renderer.setTrack(track);
			renderer.setViewParameters(viewParameters);
			renderer.configure(track.getAttributes());

			return renderer;
		}
		catch (ClassCastException e) {
			throw new RuntimeException("Possible mismatch between track and viewer: Track="
					+ track.getName() + ", reader=" + track.getAttributes().getString("reader") + ", viewer=" + track.getAttributes().getString("viewer"), e);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating renderer \"" + view + "\".", e);
		}
	}

	/**
	 * @return a sorted list of rendererClasses appropriate for the given track.
	 * @throws NullPointerException if track is null
	 */
	public <F extends Feature> List<String> getRenderersForTrack(Track<F> track) {
		return getRenderersForTrack((Class<? extends Track<F>>)track.getClass(), FeatureUtils.getFeatureClass(track));
	}

	/**
	 * @return a sorted list of rendererClasses appropriate for the given track type.
	 * @throws NullPointerException if trackClass is null
	 */
	public List<String> getRenderersForTrack(Class<? extends Track<? extends Feature>> trackClass, Class<? extends Feature> featureClass) {
		log.debug("finding renderers for track: " + trackClass.getName() + " and feature: " + featureClass.getName());
		List<String> rendererNames = new ArrayList<String>();
		for (String rendererName: rendererMap.keySet()) {
			if (isCorrectDataType(rendererName, trackClass, featureClass))
				rendererNames.add(rendererName);
		}
		Collections.sort(rendererNames);
		return rendererNames;
	}

	/**
	 * Check if the renderer is able to render tracks and features of the given classes.
	 */
	public boolean isCorrectDataType(String rendererName, Class<? extends Track<?>> trackClass, Class<? extends Feature> featureClass) {
		// check if trackClass is a subclass of the class required by the renderer
		RendererDescription record = rendererMap.get(rendererName);
		log.debug(record);
		Class<? extends Track<?>> requiredTrackClass = record.trackClass;
		Class<? extends Feature> requiredFeatureClass = record.featureClass;
		System.out.println("~~~   " + rendererName + "  " + requiredTrackClass.getName() + "  " + requiredFeatureClass.getName());
		System.out.println("   ~>              " + trackClass.getName() + "  " + featureClass.getName());
		return requiredTrackClass.isAssignableFrom(trackClass) &&
		       requiredFeatureClass.isAssignableFrom(featureClass);
	}


	/**
	 * A track renderer may end up down-casting the track or its features. The
	 * RenderMapping stores the name of the Renderer, its class and the track
	 * and feature classes that it requires.
	 * 
	 * @author cbare
	 */
	private class RendererDescription {
		final String name;
		final Class<? extends TrackRenderer> rendererClass;
		final Class<? extends Track<? extends Feature>> trackClass;
		final Class<? extends Feature> featureClass;

		public RendererDescription(String name, Class<? extends TrackRenderer> rendererClass, Class<? extends Track<? extends Feature>> trackClass, Class<? extends Feature> featureClass) {
			this.name = name;
			this.rendererClass = rendererClass;
			this.trackClass = trackClass;
			this.featureClass = featureClass;
		}
		
		public String toString() {
			return String.format("%s (%s) {%s, %s}", name, rendererClass.getName(), trackClass.getName(), featureClass.getName());
		}
	}


	/**
	 * register a track type and its applicable renderers
	 */
	public void registerTrackType(String trackType, String... rendererNames) {
		// It might not be good that this creates a fixed-size list, since we
		// might want to add renderers later. Not an issue now.
		trackTypeToRenderersMap.put(trackType, Arrays.asList(rendererNames));
	}

	/**
	 * register a track type and its applicable renderers
	 */
	public void registerTrackType(String trackType, List<String> rendererNames) {
		trackTypeToRenderersMap.put(trackType, rendererNames);
	}

	/**
	 * return a mapping of track types to applicable renderers
	 */
	public Map<String, List<String>> getTrackTypeTpRenderersMap() {
		return Collections.unmodifiableMap(trackTypeToRenderersMap);
	}

	public List<String> getTrackTypes() {
		List<String> trackTypes = new ArrayList(trackTypeToRenderersMap.size());
		trackTypes.addAll(trackTypeToRenderersMap.keySet());
		Collections.sort(trackTypes);
		return trackTypes;
	}


	public static TrackRendererRegistry newInstance() {
		TrackRendererRegistry registry = new TrackRendererRegistry();

		// register all standard renderers
		registry.registerRenderer("Bubble", BubbleTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("Gene", TypedGeneTrackRenderer.class, Track.Gene.class, GeneFeature.class);
		registry.registerRenderer("Peptide", PeptideTrackRenderer.class, Track.Gene.class, PeptideFeature.class);
		registry.registerRenderer("Block", CenteredTypedGeneTrackRenderer.class, Track.Gene.class, GeneFeature.class);
		registry.registerRenderer("Scaling", ScalingTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("Segmentation", SegmentationTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("Heatmap", HeatmapTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("MatrixHeatmap", HeatmapMatrixTrackRenderer.class, Track.Quantitative.class, Feature.Matrix.class);		
		registry.registerRenderer("ScalingMatrix", ScalingMatrixTrackRenderer.class, Track.Quantitative.class, Feature.Matrix.class);		
		registry.registerRenderer("VerticalBar", VerticalLineRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("VerticalDelimiter", VerticalDelimiterTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("ExpressionRatioRenderer", ExpressionRatioRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("Horizontal Line", HorizontalLineRenderer.class, Track.class, Feature.class);
		registry.registerRenderer("Highlight", HighlightTrackRenderer.class, Track.class, Feature.class);
		registry.registerRenderer("I beam", IBeamRenderer.class, Track.class, Feature.class);
		registry.registerRenderer("Triangle marker", TriangleMarkerRenderer.class, Track.class, Feature.class);
		registry.registerRenderer("Line", LineGraphTrackRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);
		registry.registerRenderer("NSAF", NsafTrackRenderer.class, Track.Gene.class, NsafFeature.class);
		registry.registerRenderer("Triangle p-value marker", TriangleMarkerPvalueRenderer.class, Track.Quantitative.class, Feature.QuantitativePvalue.class);
		registry.registerRenderer("Motif Cluster", MotifClusterRenderer.class, Track.Quantitative.class, Feature.Quantitative.class);

		// register track types
		registry.registerTrackType("quantitative.segment.matrix", "Bubble", "Heatmap", "MatrixHeatmap", "Scaling", "Segmentation", "ExpressionRatioRenderer");
		registry.registerTrackType("quantitative.segment", "Bubble", "Heatmap", "Scaling", "Segmentation", "ExpressionRatioRenderer", "Highlight", "VerticalBar", "VerticalDelimiter", "Triangle marker", "Motif Cluster");
		registry.registerTrackType("quantitative.positional", "Bubble", "Scaling", "Segmentation", "VerticalBar", "VerticalDelimiter", "Triangle marker", "Motif Cluster");
		registry.registerTrackType("quantitative.positional.p.value", "Bubble", "Scaling", "Segmentation", "VerticalBar", "VerticalDelimiter", "Triangle marker", "Triangle p-value marker");
		registry.registerTrackType("gene", "Gene", "NSAF");
		registry.registerTrackType("peptide", "Gene", "Peptide");

		registry.colorScaleRegistry = new ColorScaleRegistry();
		registry.colorScaleRegistry.init();

		return registry;
	}
}
