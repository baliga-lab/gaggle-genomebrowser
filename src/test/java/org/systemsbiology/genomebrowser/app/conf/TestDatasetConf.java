package org.systemsbiology.genomebrowser.app.conf;

import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.Configurator.ConfStrategy;
import org.systemsbiology.genomebrowser.model.TestData;


/**
 * Configure the app to pretend to load a fake dataset. 
 * @author cbare
 */
public class TestDatasetConf extends DefaultConf implements ConfStrategy {

    public void configure(Application app) {
        super.configure(app);
        app.setDataset(new TestData().createTestDataset());
    }
}