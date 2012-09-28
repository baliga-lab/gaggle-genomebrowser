package org.systemsbiology.genomebrowser.sqlite;

import org.junit.Test;
import org.systemsbiology.genomebrowser.model.Strand;


public class TestQuantitativeTrackReader {

	@Test
	public void test() throws Exception {
		String path = "classpath:/example/1/tiling_array.tsv";
		QuantitativeTrackReader reader = new QuantitativeTrackReader();
		reader.setDbFilename("temp.db");
		reader.setSeqId("chromosome_1");
		reader.setStrand(Strand.forward);
		reader.setTable("tiling_array");
		reader.read(path);
	}
}
