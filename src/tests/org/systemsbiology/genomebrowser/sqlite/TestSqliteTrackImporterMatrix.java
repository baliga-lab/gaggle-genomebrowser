package org.systemsbiology.genomebrowser.sqlite;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.TrackStub;
import org.systemsbiology.util.LoggingProgressListener;
import org.systemsbiology.util.MathUtils;


public class TestSqliteTrackImporterMatrix {
	private static final Logger log = Logger.getLogger("unit-test");

	@Before
	public void setup() {
		SqliteDataSource.loadSqliteDriver();
	}

	@Test
	public void test() {
		SqliteTrackImporter imp = new SqliteTrackImporter("jdbc:sqlite:test.hbgb");
		imp.addProgressListener(new LoggingProgressListener(log));

		int columns = 10;
		UUID datasetUuid = UUID.fromString("21676c27-782f-469d-972b-a0204ee295c9");
		TestMatrixFeatureSource featureSource = new TestMatrixFeatureSource();
		loadBogusFeatures(featureSource, columns);

		UUID uuid = imp.importQuantitativeSegmentMatrixTrack("Test Matrix Track", datasetUuid, featureSource, columns);
		log.info("track uuid = " + uuid);

		SqliteDataSource ds = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		TrackStub stub = ds.loadTrackStub(uuid);
		assertEquals("Test Matrix Track", stub.name);
		assertEquals("quantitative.segment.matrix", stub.type);
		assertEquals(5000, ds.countRows(stub.tableName));
		ds.deleteTrack(stub);
	}

	public void loadBogusFeatures(TestMatrixFeatureSource tmfs, int columns) {
		for (int i=0; i<5000; i++) {
			double sin = Math.sin(i/1000.0);
			double[] data = new double[columns];
			for (int j=0; j<columns; j++) {
				data[j] = sin + (j*(0.5/columns));
			}
			String strand = Math.random() > 0.5 ? "+" : "-";
			int start = 1 + i*400;
			int end = 200;
			String name = String.format("mf%04d", i);
			tmfs.addFeature(new TestMatrixFeatureFields("chromosome",strand,start,end,name,data));
		}
	}


	class TestMatrixFeatureSource implements FeatureSource {
		List<MatrixFeatureFields> features = new ArrayList<MatrixFeatureFields>();

		public void addFeature(MatrixFeatureFields mff) {
			features.add(mff);
		}
	
		public void addProgressListener(ProgressListener progressListener) {
			// TODO Auto-generated method stub
			
		}
		public void removeProgressListener(ProgressListener progressListener) {
			// TODO Auto-generated method stub
			
		}

		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
			for (MatrixFeatureFields mff: features) {
				featureProcessor.process(mff);
			}
		}
	}

	class TestMatrixFeatureFields implements MatrixFeatureFields {
		double[] data;
		String name, seqName, strand;
		int start, end;

		public TestMatrixFeatureFields(String seqName, String strand, int start, int end, String name, double[] data) {
			this.seqName = seqName;
			this.strand = strand;
			this.start = start;
			this.end = end;
			this.name = name;
			this.data = data;
		}

		public double[] getValues() {
			return data;
		}

		public String getCommonName() {
			return null;
		}

		public int getEnd() {
			return end;
		}

		public String getGeneType() {
			return null;
		}

		public String getName() {
			return name;
		}

		public int getPosition() {
			return MathUtils.average(start, end);
		}

		public String getSequenceName() {
			return seqName;
		}

		public int getStart() {
			return start;
		}

		public String getStrand() {
			return strand;
		}

		public double getValue() {
			return 0;
		}
		
	}
}
