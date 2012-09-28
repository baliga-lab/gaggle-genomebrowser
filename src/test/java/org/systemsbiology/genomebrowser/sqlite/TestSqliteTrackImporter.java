package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.TrackStub;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;


public class TestSqliteTrackImporter {
	private static final Logger log = Logger.getLogger("unit-test");

	@Before
	public void setup() {
		SqliteDataSource.loadSqliteDriver();
	}

	/**
	 * depends on the existence of a features_transcript_signal table in the DB
	 * and the nonexistence of any tables of the form features_transcript_signal_[n]
	 * where n is a number.
	 */
	@Test
	public void testTrackNameUniquifier() throws Exception {
		SqliteTrackImporter imp = new SqliteTrackImporter("jdbc:sqlite:test.hbgb");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:test.hbgb");
		String table = imp.toFeaturesTableName("Transcript signal");
		log.info("test table name = " + table);
		assertEquals("features_transcript_signal", table);
		table = imp.uniquifyTableName(conn, table);
		log.info("unique table name = " + table);
		assertEquals("features_transcript_signal_1", table);
	}

	@Test
	public void testCheckIfTrackExists() throws Exception {
		SqliteTrackImporter imp = new SqliteTrackImporter("jdbc:sqlite:test.hbgb");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:test.hbgb");
		TrackStub stub = imp.checkIfTrackExists(conn, "Transcription signal");

		assertNotNull(stub);
		log.info("found track: " + stub.name);
		assertEquals("Transcription signal", stub.name);
		
		// try a non-existant track
		stub = imp.checkIfTrackExists(conn, "Snorklewacker");
		assertNull(stub);
	}

	@Test
	public void testTrackImport() {
		SqliteTrackImporter imp = new SqliteTrackImporter("jdbc:sqlite:test.hbgb");
		UUID uuid = imp.importQuantitativeSegmentTrack("bogus", UUID.fromString("21676c27-782f-469d-972b-a0204ee295c9"), new TestFeatureSource());

		SqliteDataSource ds = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		TrackStub stub = ds.loadTrackStub(uuid);
		assertEquals("bogus", stub.name);
		assertEquals("quantitative.segment", stub.type);
		assertEquals(100, ds.countRows(stub.tableName));
		ds.deleteTrack(stub);
	}


	class TestFeatureSource implements FeatureSource {
		public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
			for (FeatureFields featureFields : featureFields()) {
				featureProcessor.process(featureFields);
			}
		}

		public void addProgressListener(ProgressListener progressListener) {
			// TODO Auto-generated method stub
		}

		public void removeProgressListener(ProgressListener progressListener) {
			// TODO Auto-generated method stub
		}
	}

	/**
	 * An iterable of FeatureField objects to give to the track importer
	 */
	public Iteratable<FeatureFields> featureFields() {
		return new Iteratable<FeatureFields>() {
			int MAX = 100;
			int i = 0;

			String sequenceName = "chromosome";
			Strand strand;
			int start;
			int end;
			double value;

			FeatureFields ff = new FeatureFields() {
				public double getValue() {
					return value;
				}

				public String getSequenceName() {
					return sequenceName;
				}

				public String getStrand() {
					return strand.toAbbreviatedString();
				}

				public int getStart() {
					return start;
				}

				public int getEnd() {
					return end;
				}

				public int getPosition() {
					return MathUtils.average(start, end);
				}
				
				public String getCommonName() {
					return null;
				}
				
				public String getGeneType() {
					return null;
				}
				
				public String getName() {
					return null;
				}
			};

			public boolean hasNext() {
				return i < MAX;
			}

			public FeatureFields next() {
				start = i * 20;
				end = start + 60;
				value = Math.sin(i/10.0);
				strand = value >= 0.0 ? Strand.forward : Strand.reverse;
				i++;
				return ff;
			}

			public void remove() {
				throw new UnsupportedOperationException("can't remove");
			}

			public Iterator<FeatureFields> iterator() {
				return this;
			}
		};
	}
}
