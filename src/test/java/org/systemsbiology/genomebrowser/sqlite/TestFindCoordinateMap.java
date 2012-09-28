package org.systemsbiology.genomebrowser.sqlite;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Strand;



/**
 * Test our ability to find coordinate mappings given a bunch of names.
 * @author cbare
 */
public class TestFindCoordinateMap {
	private static final Logger log = Logger.getLogger("unit-test");

	@Before
	public void setup() {
		SqliteDataSource.loadSqliteDriver();
	}

	@Test
	public void testGeneNames() throws Exception {
		SqliteDataSource db = new SqliteDataSource("jdbc:sqlite:test.hbgb");

		String[] names = new String[] {"VNG0001H","VNG0002G","VNG0003C","VNG0005H","VNG0006G","VNG0008G","VNG0009G","VNG0011C","VNG0013C","VNG0014C","VNG0015H","VNG0016H","VNG0017H","VNG0018H","VNG0019H","VNG0020H","VNG0021H","VNG0022H","VNG0023H","VNG0024H","VNG0025H","VNG0026C","VNG0027H","VNG0028C","VNG0029H","VNG0030H","VNG0031H","VNG0032H","VNG0033H","VNG0034H","VNG0035C","VNG0037H","VNG0038H","VNG0039H","VNG0040C","VNG0041C","VNG0042G","VNG0043H","VNG0045C","VNG0046G","VNG0047G","VNG0049H","VNG0050C","VNG0051G","VNG0053H","VNG0054H","VNG0055H","VNG0057H","VNG0058H","VNG0060G","VNG0061C","VNG0062G","VNG0063G","VNG0064G","VNG0065G","VNG0066H","VNG0067H","VNG0068H","VNG0069H","VNG0070H","VNG0072H","VNG0073C","VNG0075H","VNG0076H","VNG0077H","VNG0079H","VNG0080H","VNG0081G","VNG0084G","VNG0085G","VNG0086G","VNG0089G","VNG0090G","VNG0091C","VNG0094C","VNG0095G","VNG0096C","VNG0097G","VNG0098G","VNG0099G","VNG0101G","VNG0102C","VNG0104G","VNG0105H","VNG0106G"};
		
		CoordinateMap cm = db.findCoordinateMap(names);
		
		log.info("VNG0001H -> " + cm.getCoordinates("VNG0001H"));
		log.info("VNG0099G -> " + cm.getCoordinates("VNG0099G"));
		log.info("VNG1173G -> " + cm.getCoordinates("VNG1173G"));
		log.info("VNG1175G -> " + cm.getCoordinates("VNG1175G"));
		log.info("VNG1187G -> " + cm.getCoordinates("VNG1187G"));
		log.info("VNG6297C -> " + cm.getCoordinates("VNG6297C"));

		// Expected values here may need to change. I noticed that
		// the coords I have in the dataset are a little off from the
		// coords in SBEAMS.

		Coordinates coords = cm.getCoordinates("VNG0001H");
		assertEquals("chromosome", coords.getSeqId());
		assertEquals(Strand.forward, coords.getStrand());
		assertEquals(248, coords.getStart());
		assertEquals(1450, coords.getEnd());

		coords = cm.getCoordinates("VNG0099G");
		assertEquals("chromosome", coords.getSeqId());
		assertEquals(Strand.reverse, coords.getStrand());
		assertEquals(83888, coords.getStart());
		assertEquals(84415, coords.getEnd());

		coords = cm.getCoordinates("VNG1173G");
		assertEquals("chromosome", coords.getSeqId());
		assertEquals(Strand.reverse, coords.getStrand());
		assertEquals(884449, coords.getStart());
		assertEquals(884745, coords.getEnd());

		coords = cm.getCoordinates("VNG6297C");
		assertEquals("pNRC200", coords.getSeqId());
		assertEquals(Strand.forward, coords.getStrand());
		assertEquals(228687, coords.getStart());
		assertEquals(229880, coords.getEnd());
	}
}
