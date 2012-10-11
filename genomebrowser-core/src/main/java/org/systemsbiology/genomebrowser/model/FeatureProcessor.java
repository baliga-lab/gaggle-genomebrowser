package org.systemsbiology.genomebrowser.model;

public interface FeatureProcessor {
    void process(FeatureFields fields) throws Exception;
    int getCount();
    void cleanup();
}