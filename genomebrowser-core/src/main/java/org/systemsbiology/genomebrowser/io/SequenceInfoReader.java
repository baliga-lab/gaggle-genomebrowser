package org.systemsbiology.genomebrowser.io;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.genomebrowser.model.Topology;

public class SequenceInfoReader {
	List<SequenceInfo> sequences = new ArrayList<SequenceInfo>();

	public List<SequenceInfo> read(File file) throws Exception {
		return read(new FileReader(file));
	}

	public List<SequenceInfo> read(Reader reader) throws Exception {
		LineReader lineReader = new LineReader(new LineReader.LineProcessor() {
			public void process(int lineNumber, String line) throws Exception {
				String[] fields = line.split("\t");
				if (fields.length==2)
					sequences.add(new SequenceInfo(fields[0], Integer.parseInt(fields[1])));
				else if (fields.length>=3)
					sequences.add(new SequenceInfo(fields[0], Integer.parseInt(fields[1]), Topology.valueOf(fields[2])));
			}
		});
		lineReader.loadData(reader);
		return sequences;
	}

	public static class SequenceInfo {
		public final String name;
		public final int length;
		public final Topology topology;

		public SequenceInfo(String name, int length, Topology topology) {
			this.name = name;
			this.length = length;
			this.topology = topology;
		}

		public SequenceInfo(String name, int length) {
			this.name = name;
			this.length = length;
			this.topology = null;
		}
	}
}
