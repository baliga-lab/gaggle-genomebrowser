package org.systemsbiology.genomebrowser.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.ProgressListener;


/**
 * <p>Configurable reader for tab delimited files. Columns in the file
 * can be returned as arrays of primitive data types or Strings.</p>
 * 
 * <p>Usage: Arrays are pre-allocated, so it's necessary to specify
 * the size of the arrays in advance. All columns must be the same
 * length with no missing data. Call addColumn for each column
 * you want to read. It is not necessary to read all columns. Then,
 * call one of the loadData(...) methods. Lastly, retrieve the
 * arrays with getIntColumn, getFloatColumn, getDoubleColumn, or
 * getStringColumn.</p>
 * 
 * <p>First row of file must contain column headers.</p>
 * 
 * <p>This class is the result of generalizing a bunch of similar loaders
 * that were specific to a particular arrangement of columns. It's not
 * really clear to me whether generalizing several 75 line classes into
 * a ~400 line class is a net gain or loss.</p>
 * 
 * @author cbare
 */
public class TabDelimitedFileLoader {
	private static final Logger log = Logger.getLogger(TabDelimitedFileLoader.class);
	int size;
	String[] columnHeaders;
	Set<ProgressListener> listeners = new HashSet<ProgressListener>();

	/**
	 * Generally, we only want one column object (or one column of a particular
	 * type) stored per index. That way, at most one array will be created to
	 * store each column. We use a HashSet and define the columns to be equal if
	 * they have the same index and are of the same type.
	 */
	Set<Column> columns = new HashSet<Column>();


	/**
	 * Construct a loader, initializing the size of the arrays.
	 */
	public TabDelimitedFileLoader(int size) {
		this.size = size;
	}

	/**
	 * loads a three column file with start, end and value.
	 */
	public static TabDelimitedFileLoader createSegmentDataPointLoader(int size) {
		TabDelimitedFileLoader loader = new TabDelimitedFileLoader(size);
		loader.addIntColumn(0);
		loader.addIntColumn(1);
		loader.addDoubleColumn(2);
		return loader;
	}

	/**
	 * loads a 2 column file with position and value columns.
	 */
	public static TabDelimitedFileLoader createPositionDataPointLoader(int size) {
		TabDelimitedFileLoader loader = new TabDelimitedFileLoader(size);
		loader.addIntColumn(0);
		loader.addDoubleColumn(1);
		return loader;
	}

	/**
	 * Creates a reader for a file with starts, ends and values which converts
	 * the start and end coordinates to a single central coordinate. Central
	 * coordinate is returned in column 0 and the value is in column 2;
	 */
	public static TabDelimitedFileLoader createSegmentToPositionDataPointLoader(int size) {
		TabDelimitedFileLoader loader = new TabDelimitedFileLoader(size);
		loader.columns.add(new IntAverageColumn("position",0,1));
		loader.addDoubleColumn(2);
		return loader;
	}

	/**
	 * loads a file with these columns: sequence, strand, start, end and value.
	 */
	public static TabDelimitedFileLoader createDataPointFeatureLoader(int size) {
		TabDelimitedFileLoader loader = new TabDelimitedFileLoader(size);
		loader.addIntColumn(2);
		loader.addIntColumn(3);
		loader.addDoubleColumn(4);
		return loader;
	}

	/**
	 * Configure the loader to record integer values from the specified column in the file.
	 * @param index a zero based index of the column in the tab delimited file.
	 */
	public void addDoubleColumn(int index) {
		columns.add(new DoubleColumn(index));
	}

	/**
	 * Configure the loader to record integer values from the specified column in the file.
	 * @param index a zero based index of the column in the tab delimited file.
	 */
	public void addFloatColumn(int index) {
		columns.add(new FloatColumn(index));
	}

	/**
	 * Configure the loader to record integer values from the specified column in the file.
	 * @param index a zero based index of the column in the tab delimited file.
	 */
	public void addStringColumn(int index) {
		columns.add(new StringColumn(index));
	}

	/**
	 * Configure the loader to record integer values from the specified column in the file.
	 * @param index a zero based index of the column in the tab delimited file.
	 */
	public void addIntColumn(int index) {
		columns.add(new IntColumn(index));
	}

	/**
	 * @return the column casted to an array of ints
	 * @throws ClassCastException if the column doesn't hold ints
	 */
	public int[] getIntColumn(String name) {
		return ((int[])getColumn(name).getArray());
	}

	/**
	 * @return the column casted to an array of doubles
	 * @throws ClassCastException if the column doesn't hold doubles
	 */
	public double[] getDoubleColumn(String name) {
		return ((double[])getColumn(name).getArray());
	}

	/**
	 * @return the column casted to an array of floats
	 * @throws ClassCastException if the column doesn't hold floats
	 */
	public float[] getFloatColumn(String name) {
		return ((float[])getColumn(name).getArray());
	}

	/**
	 * @return the column casted to an array of Strings
	 * @throws ClassCastException if the column doesn't hold Strings
	 */
	public String[] getStringColumn(String name) {
		return ((String[])getColumn(name).getArray());
	}

	/**
	 * @return the column casted to an array of ints
	 * @throws ClassCastException if the column doesn't hold ints
	 */
	public int[] getIntColumn(int column) {
		return ((int[])getColumn(column).getArray());
	}

	/**
	 * @return the column casted to an array of doubles
	 * @throws ClassCastException if the column doesn't hold doubles
	 */
	public double[] getDoubleColumn(int column) {
		return ((double[])getColumn(column).getArray());
	}

	/**
	 * @return the column casted to an array of floats
	 * @throws ClassCastException if the column doesn't hold floats
	 */
	public float[] getFloatColumn(int column) {
		return ((float[])getColumn(column).getArray());
	}

	/**
	 * @return the column casted to an array of Strings
	 * @throws ClassCastException if the column doesn't hold Strings
	 */
	public String[] getStringColumn(int column) {
		return ((String[])getColumn(column).getArray());
	}

	private Column getColumn(int index) {
		for (Column column : columns) {
			if (column.index == index) {
				return column;
			}
		}
		throw new ArrayIndexOutOfBoundsException("Column " + index + " does not exist.");
	}

	private Column getColumn(String name) {
		for (Column column : columns) {
			if (column instanceof ComputedColumn && name.equals(((ComputedColumn)column).name)) {
				return column;
			}
		}
		throw new RuntimeException("Column " + name + " does not exist.");
	}

	public String getColumnHeader(int column) {
		return columnHeaders[column];
	}

	/**
	 * @return the index of the column with the given name or 
	 */
	public int getIndexOfColumn(String name) {
		for (int i=0; i< columnHeaders.length; i++) {
			if (name.equals(columnHeaders[i]))
				return i;
		}
		return -1;
	}

	public void loadData(String filename) throws IOException {
		loadData(FileUtils.getReaderFor(filename));
	}

	public void loadData(Reader reader) throws IOException {
		BufferedReader r = null;

		// allocate arrays
		for (Column column : columns) {
			column.allocateArray(size);
		}
		log.debug("Allocated " + columns.size() + " columns");

		try {
			r = new BufferedReader(reader);

			// read first line of the file, which we assume holds column headers
			String line = r.readLine();
			columnHeaders = line.split("\t");

			int i = 0;
			line = r.readLine();
			while (line != null) {
				String[] fields = line.split("\t");

				for (Column column : columns) {
					column.processLine(i, fields);
				}

				i++;
				if (i % 1000 == 0)
					fireIncrementProgressEvent(1);

				line = r.readLine();
			}
			log.info("read " + i + " lines.");
		}
		finally {
			try {
				if (r != null) {
					r.close();
				}
			}
			catch (Exception e) {
				log.error(e);
			}
		}

	}

	public void readHeaders(String filename) throws IOException {
		readHeaders(FileUtils.getReaderFor(filename));
	}

	public void readHeaders(Reader reader) throws IOException {
		BufferedReader r = null;
		try {
			r = new BufferedReader(reader);

			// read first line of the file, which we assume holds column headers
			String line = r.readLine();
			columnHeaders = line.split("\t");
		}
		finally {
			try {
				if (r != null) {
					r.close();
				}
			}
			catch (Exception e) {
				log.error(e);
			}
		}
	}


	public void addProgressListener(ProgressListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeProgressListener(ProgressListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void fireIncrementProgressEvent(int p) {
		synchronized (listeners) {
			for (ProgressListener listener : listeners) {
				listener.incrementProgress(p);
			}
		}
	}

	/**
	 * A subclass of column exists for each of: int, double, float, and String.
	 * Holds an array of that type along with some functionality dependant of
	 * the type of the column.
	 * @author cbare
	 */
	static abstract class Column {
		int index;
		
		public abstract Object getArray();
		public abstract void allocateArray(int size);
		public abstract void processLine(int i, String[] fields);

		@Override
		public int hashCode() {
			return index;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Column other = (Column) obj;
			return (index == other.index);
		}
	}

	static abstract class ComputedColumn extends Column {
		String name;
	}
	
	static class IntColumn extends Column {
		int[] array;

		public IntColumn(int index) {
			this.index = index;
		}
		public int[] getArray() {
			return array;
		}
		public void allocateArray(int size) {
			array = new int[size];
		}
		public void processLine(int i, String[] fields) {
			array[i] = Integer.parseInt(fields[index]);
		}
	}
	
	static class DoubleColumn extends Column {
		double[] array;

		public DoubleColumn(int index) {
			this.index = index;
		}
		public double[] getArray() {
			return array;
		}
		public void allocateArray(int size) {
			array = new double[size];
		}
		public void processLine(int i, String[] fields) {
			array[i] = Double.parseDouble(fields[index]);
		}
	}
	
	static class FloatColumn extends Column {
		float[] array;

		public FloatColumn(int index) {
			this.index = index;
		}
		public float[] getArray() {
			return array;
		}
		public void allocateArray(int size) {
			array = new float[size];
		}
		public void processLine(int i, String[] fields) {
			array[i] = Float.parseFloat(fields[index]);
		}
	}
	
	static class StringColumn extends Column {
		String[] array;

		public StringColumn(int index) {
			this.index = index;
		}
		public String[] getArray() {
			return array;
		}
		public void allocateArray(int size) {
			array = new String[size];
		}
		public void processLine(int i, String[] fields) {
			array[i] = fields[index];
		}
	}

	static class IntAverageColumn extends ComputedColumn {
		int index1;
		int index2;
		int[] array;

		public IntAverageColumn(String name, int index1, int index2) {
			// computed columns have index -1
			this.index = -1;
			this.name = name;
			this.index1 = index1;
			this.index2 = index2;
		}

		public void allocateArray(int size) {
			array = new int[size];
		}

		public Object getArray() {
			return array;
		}

		public void processLine(int i, String[] fields) {
			array[i] = (Integer.parseInt(fields[index1]) + Integer.parseInt(fields[index2])) >>>1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + index1;
			result = prime * result + index2;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			final IntAverageColumn other = (IntAverageColumn) obj;
			return (index1 == other.index1) && (index2 == other.index2);
		}
	}
}
