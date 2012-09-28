package org.systemsbiology.genomebrowser.app.conf;

import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.EventListener;
import org.systemsbiology.genomebrowser.app.Selections;
import org.systemsbiology.genomebrowser.app.Configurator.ConfStrategy;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkCatalog;
import org.systemsbiology.genomebrowser.gaggle.GenomeBrowserGoose;
import org.systemsbiology.genomebrowser.search.HackySearchEngine;
import org.systemsbiology.genomebrowser.sqlite.SqliteIo;
import org.systemsbiology.genomebrowser.transcript.TranscriptBoundaryPlugin;
import org.systemsbiology.genomebrowser.ui.ExternalUiController;
import org.systemsbiology.genomebrowser.ui.FilterToolBar;
import org.systemsbiology.genomebrowser.ui.UI;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackManager;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRendererRegistry;


/**
 * Default configuration for the Genome Browser app.
 * 
 * @author cbare
 */
public class DefaultConf implements ConfStrategy {

	public void configure(Application app) {
		try {
			app.bookmarkCatalog = new BookmarkCatalog();
			app.addEventListener(app.bookmarkCatalog);

			app.selections = new Selections();
			app.selections.addEventListener(app);
			app.addEventListener(app.selections);

			app.io = new SqliteIo(app);

			HackySearchEngine search = new HackySearchEngine();
			app.search = search;
			search.addEventListener(app);
			search.automaticallyAddWildcardSuffix(true);

			ViewParameters viewParameters = new ViewParameters();

			app.trackManager = new TrackManager();
			TrackRendererRegistry registry = TrackRendererRegistry.newInstance();
			registry.setViewParameters(viewParameters);
			app.trackManager.setTrackRendererRegistry(registry);

			ExternalUiController uiController = new ExternalUiController(UI.newInstance(app, viewParameters));
			app.setUi(uiController);
			app.addEventListener(uiController);

			// wire components to receive application events
			if (app.search instanceof EventListener) {
				app.addEventListener((EventListener)app.search);
			}
			app.addEventListener(uiController);

			// register goose as a plugin
			GenomeBrowserGoose goose = new GenomeBrowserGoose();
			app.registerPlugin(goose);

			FilterToolBar filterToolBar = new FilterToolBar();
			app.registerPlugin(filterToolBar);
			
			TranscriptBoundaryPlugin transcriptBoundaryPlugin = new TranscriptBoundaryPlugin();
			app.registerPlugin(transcriptBoundaryPlugin);
		}
		catch (Exception e) {
			throw new RuntimeException("Exception during configuration...", e);
		}
	}
}
