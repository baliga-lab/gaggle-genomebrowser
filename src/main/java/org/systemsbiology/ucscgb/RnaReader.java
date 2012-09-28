package org.systemsbiology.ucscgb;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.io.LineReader;
import org.systemsbiology.genomebrowser.io.LineReader.LineProcessor;


// TODO get RNA from UCSC microbes
public class RnaReader {
	private static final Logger log = Logger.getLogger(RnaReader.class);

//	Database: metMar1    Primary Table: gbRNAs    Row Count: 61
//	
//	field                       example                     SQL type                    info
//----------------------------------------------------------------------------------------------
//	bin                         585                         int(10) unsigned            range
//	chrom                       chr                         varchar(255)                values
//	chromStart                  67                          int(10) unsigned            range
//	chromEnd                    182                         int(10) unsigned            range
//	name                        RNA_46                      varchar(255)                values
//	score                       1000                        int(10) unsigned            range
//	strand                      -                           char(1)                     values
//	product                     5S ribosomal RNA            varchar(255)                values
//	intron                                                  varchar(255)                values


	/**
	 * 
	 * @param reader
	 * @param removeFragments
	 * @return
	 * @throws Exception 
	 */
	public List<Gene> readRnas(Reader reader, boolean removeFragments) throws Exception {
		final List<Gene> genes = new ArrayList<Gene>();

		LineReader lineReader = new LineReader();
		lineReader.setLineHandler(new LineProcessor() {
			public void process(int lineNumber, String line) throws Exception {
				if (line.startsWith("#")) return;

				String[] fields = line.split("\t");
				if (fields.length<7) {
					log.warn("can't parse: " + line);
					return;
				}
				int start = Integer.parseInt(fields[2]);
				int end = Integer.parseInt(fields[3]);
				genes.add(new Gene(
						fields[4],    // name
						fields[1],    // chrom
						fields[6],    // strand
						start, end,   // tx start/end
						start, end,   // cds start/end
						ifExists(fields, 5),    // id/score
						ifExists(fields, 7),    // name2
						getType(fields[4],ifExists(fields, 7))));
			}
		});

		lineReader.loadData(reader);
		return genes;
	}

	/**
	 * Return the array element i if it exists, otherwise return an
	 * empty string.
	 */
	private String ifExists(String[] strings, int i) {
		if (strings==null || i < 0) return "";
		if (strings.length > i)
			return strings[i];
		else
			return "";
	}

	private String getType(String name, String product) {
		name = name.toLowerCase();
		product = product.toLowerCase();
		if (name.contains("trna"))
			return "trna";
		if (name.contains("rrna"))
			return "rrna";
		if (product.contains("trna"))
			return "trna";
		if (product.contains("rrna") || product.contains("ribosomal"))
			return "rrna";
		return "rna";
	}
}
