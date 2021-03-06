package mikera.matrixx;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import mikera.arrayz.INDArray;
import mikera.matrixx.algo.Multiplications;
import mikera.matrixx.impl.ADenseArrayMatrix;
import mikera.matrixx.impl.AStridedMatrix;
import mikera.matrixx.impl.DenseColumnMatrix;
import mikera.matrixx.impl.StridedMatrix;
import mikera.matrixx.impl.VectorMatrixMN;
import mikera.vectorz.AVector;
import mikera.vectorz.Op;
import mikera.vectorz.Vector;
import mikera.vectorz.Vectorz;
import mikera.vectorz.impl.AStridedVector;
import mikera.vectorz.impl.ArraySubVector;
import mikera.vectorz.impl.StridedElementIterator;
import mikera.vectorz.impl.StridedVector;
import mikera.vectorz.util.DoubleArrays;
import mikera.vectorz.util.ErrorMessages;

/**
 * Standard MxN matrix class backed by a densely packed double[] array
 * 
 * This is the most efficient Vectorz type for dense 2D matrices.
 * 
 * @author Mike
 */
public final class Matrix extends ADenseArrayMatrix {
	private static final long serialVersionUID = -3260581688928230431L;

	private Matrix(int rowCount, int columnCount) {
		this(rowCount, columnCount, createStorage(rowCount, columnCount));
	}

	/**
	 * Creates a new zero-filled matrix of the specified shape.
	 */
	public static Matrix create(int rowCount, int columnCount) {
		return new Matrix(rowCount, columnCount);
	}
	
	public static Matrix create(int... shape) {
		int dims=shape.length;
		if (dims!=2) throw new IllegalArgumentException("Cannot create Matrix with dimensionality: "+dims);
		return create(shape[0],shape[1]);
	}


	public static Matrix create(AMatrix m) {
		return new Matrix(m.rowCount(), m.columnCount(), m.toDoubleArray());
	}

	/**
	 * Creates a new Matrix with a copy of all data from the source matrix
	 * 
	 * @param m
	 */
	public Matrix(AMatrix m) {
		this(m.rowCount(), m.columnCount(), m.toDoubleArray());
	}

	public static double[] createStorage(int rowCount, int columnCount) {
		long elementCount = ((long) rowCount) * columnCount;
		int ec = (int) elementCount;
		if (ec != elementCount)
			throw new IllegalArgumentException(ErrorMessages.tooManyElements(
					rowCount, columnCount));
		return new double[ec];
	}

	public static Matrix createRandom(int rows, int cols) {
		Matrix m = create(rows, cols);
		double[] d = m.data;
		for (int i = 0; i < d.length; i++) {
			d[i] = Math.random();
		}
		return m;
	}

	public static Matrix create(INDArray m) {
		if (m.dimensionality() != 2)
			throw new IllegalArgumentException(
					"Can only create matrix from 2D array");
		int rows = m.getShape(0);
		int cols = m.getShape(1);
		return Matrix.wrap(rows, cols, m.toDoubleArray());
	}

	public static Matrix createFromRows(Object... rowVectors) {
		List<AVector> vs = new ArrayList<AVector>();
		for (Object o : rowVectors) {
			vs.add(Vectorz.create(o));
		}
		AMatrix m = VectorMatrixMN.create(vs);
		return create(m);
	}

	public static Matrix create(double[][] data) {
		int rows = data.length;
		int cols = data[0].length;
		Matrix m = Matrix.create(rows, cols);
		for (int i = 0; i < rows; i++) {
			double[] drow = data[i];
			if (drow.length != cols) { throw new IllegalArgumentException(
					"Array shape is not rectangular! Row " + i + " has length "
							+ drow.length); }
			System.arraycopy(drow, 0, m.data, i * cols, cols);
		}
		return m;
	}

	/**
	 * Creates a new Matrix using the given vectors as row data
	 * 
	 * @param data
	 * @return
	 */
	public static Matrix create(AVector... data) {
		int rc = data.length;
		int cc = (rc == 0) ? 0 : data[0].length();
		Matrix m = create(rc, cc);
		for (int i = 0; i < rc; i++) {
			m.setRow(i, data[i]);
		}
		return m;
	}

	@Override
	public boolean isFullyMutable() {
		return true;
	}

	@Override
	public boolean isView() {
		return false;
	}

	@Override
	public boolean isBoolean() {
		return DoubleArrays.isBoolean(data, 0, data.length);
	}

	@Override
	public boolean isZero() {
		return DoubleArrays.isZero(data, 0, data.length);
	}

	@Override
	public boolean isPackedArray() {
		return true;
	}

	private Matrix(int rowCount, int columnCount, double[] data) {
		super(data, rowCount, columnCount);
	}

	public static Matrix wrap(int rowCount, int columnCount, double[] data) {
		if (data.length != rowCount * columnCount)
			throw new IllegalArgumentException("data array is of wrong size: "
					+ data.length);
		return new Matrix(rowCount, columnCount, data);
	}

	@Override
	public AStridedMatrix subMatrix(int rowStart, int rows, int colStart,
			int cols) {
		if ((rowStart < 0) || (rowStart >= this.rows) || (colStart < 0)
				|| (colStart >= this.cols))
			throw new IndexOutOfBoundsException(
					"Invalid submatrix start position");
		if ((rowStart + rows > this.rows) || (colStart + cols > this.cols))
			throw new IndexOutOfBoundsException(
					"Invalid submatrix end position");
		return StridedMatrix.wrap(data, rows, cols, rowStart * rowStride()
				+ colStart * columnStride(), rowStride(), columnStride());
	}

	@Override
	public Vector innerProduct(AVector a) {
		if (a instanceof Vector) return innerProduct((Vector) a);
		return transform(a);
	}

	@Override
	public Matrix innerProduct(Matrix a) {
		return Multiplications.multiply(this, a);
	}

	@Override
	public Matrix transposeInnerProduct(Matrix s) {
		Matrix r = toMatrixTranspose();
		return Multiplications.multiply(r, s);
	}

	@Override
	public Matrix innerProduct(AMatrix a) {
		if (a instanceof Matrix) { return innerProduct((Matrix) a); }
		if ((this.columnCount() != a.rowCount())) { throw new IllegalArgumentException(
				ErrorMessages.mismatch(this, a)); }
		return Multiplications.multiply(this, a);
	}

	@Override
	public double elementSum() {
		return DoubleArrays.elementSum(data);
	}

	@Override
	public double elementSquaredSum() {
		return DoubleArrays.elementSquaredSum(data);
	}

	@Override
	public double elementMax() {
		return DoubleArrays.elementMax(data);
	}

	@Override
	public double elementMin() {
		return DoubleArrays.elementMin(data);
	}

	@Override
	public void abs() {
		DoubleArrays.abs(data);
	}

	@Override
	public void signum() {
		DoubleArrays.signum(data);
	}

	@Override
	public void square() {
		DoubleArrays.square(data);
	}

	@Override
	public void exp() {
		DoubleArrays.exp(data);
	}

	@Override
	public void log() {
		DoubleArrays.log(data);
	}

	@Override
	public long nonZeroCount() {
		return DoubleArrays.nonZeroCount(data);
	}

	@Override
	public final void copyRowTo(int row, double[] dest, int destOffset) {
		int srcOffset = row * cols;
		System.arraycopy(data, srcOffset, dest, destOffset, cols);
	}

	@Override
	public final void copyColumnTo(int col, double[] dest, int destOffset) {
		int colOffset = col;
		for (int i = 0; i < rows; i++) {
			dest[destOffset + i] = data[colOffset + i * cols];
		}
	}

	@Override
	public Vector transform(AVector a) {
		Vector v = Vector.createLength(rows);
		double[] vdata = v.getArray();
		for (int i = 0; i < rows; i++) {
			vdata[i] = a.dotProduct(data, i * cols);
		}
		return v;
	}

	@Override
	public Vector transform(Vector a) {
		Vector v = Vector.createLength(rows);
		transform(a, v);
		return v;
	}

	@Override
	public void transform(AVector source, AVector dest) {
		if ((source instanceof Vector) && (dest instanceof Vector)) {
			transform((Vector) source, (Vector) dest);
			return;
		}
		if (rows != dest.length())
			throw new IllegalArgumentException(
					ErrorMessages.wrongDestLength(dest));
		if (cols != source.length())
			throw new IllegalArgumentException(
					ErrorMessages.wrongSourceLength(source));
		for (int i = 0; i < rows; i++) {
			dest.unsafeSet(i, source.dotProduct(data, i * cols));
		}
	}

	@Override
	public void transform(Vector source, Vector dest) {
		int rc = rowCount();
		int cc = columnCount();
		if (source.length() != cc)
			throw new IllegalArgumentException(
					ErrorMessages.wrongSourceLength(source));
		if (dest.length() != rc)
			throw new IllegalArgumentException(
					ErrorMessages.wrongDestLength(dest));
		int di = 0;
		double[] sdata = source.getArray();
		double[] ddata = dest.getArray();
		for (int row = 0; row < rc; row++) {
			double total = 0.0;
			for (int column = 0; column < cc; column++) {
				total += data[di + column] * sdata[column];
			}
			di += cc;
			ddata[row] = total;
		}
	}

	@Override
	public ArraySubVector getRowView(int row) {
		return ArraySubVector.wrap(data, row * cols, cols);
	}

	@Override
	public AStridedVector getColumnView(int col) {
		if (cols == 1) {
			if (col != 0)
				throw new IndexOutOfBoundsException("Column does not exist: "
						+ col);
			return Vector.wrap(data);
		} else {
			return StridedVector.wrap(data, col, rows, cols);
		}
	}

	@Override
	public void swapRows(int i, int j) {
		if (i == j) return;
		int a = i * cols;
		int b = j * cols;
		int cc = columnCount();
		for (int k = 0; k < cc; k++) {
			int i1 = a + k;
			int i2 = b + k;
			double t = data[i1];
			data[i1] = data[i2];
			data[i2] = t;
		}
	}

	@Override
	public void swapColumns(int i, int j) {
		if (i == j) return;
		int rc = rowCount();
		int cc = columnCount();
		for (int k = 0; k < rc; k++) {
			int x = k * cc;
			double t = data[i + x];
			data[i + x] = data[j + x];
			data[j + x] = t;
		}
	}

	@Override
	public void multiplyRow(int i, double factor) {
		int offset = i * cols;
		DoubleArrays.multiply(data, offset, cols, factor);
	}

	@Override
	public void addRowMultiple(int src, int dst, double factor) {
		int soffset = src * cols;
		int doffset = dst * cols;
		for (int j = 0; j < cols; j++) {
			data[doffset + j] += factor * data[soffset + j];
		}
	}

	@Override
	public Vector asVector() {
		return Vector.wrap(data);
	}

	@Override
	public Vector toVector() {
		return Vector.create(data);
	}

	@Override
	public final Matrix toMatrix() {
		return this;
	}

	@Override
	public final double[] toDoubleArray() {
		return DoubleArrays.copyOf(data);
	}

	@Override
	public Matrix toMatrixTranspose() {
		int rc = rowCount();
		int cc = columnCount();
		Matrix m = Matrix.create(cc, rc);
		double[] targetData = m.data;
		for (int j = 0; j < cc; j++) {
			copyColumnTo(j, targetData, j * rc);
		}
		return m;
	}

	@Override
	public void toDoubleBuffer(DoubleBuffer dest) {
		dest.put(data);
	}

	@Override
	public double[] asDoubleArray() {
		return data;
	}

	@Override
	public double get(int i, int j) {
		if ((j < 0) || (j >= cols)) throw new IndexOutOfBoundsException();
		return data[(i * cols) + j];
	}

	@Override
	public void unsafeSet(int i, int j, double value) {
		data[(i * cols) + j] = value;
	}

	@Override
	public double unsafeGet(int i, int j) {
		return data[(i * cols) + j];
	}

	@Override
	public void addAt(int i, int j, double d) {
		data[(i * cols) + j] += d;
	}

	@Override
	public void addAt(int i, double d) {
		data[i] += d;
	}

	@Override
	public void subAt(int i, double d) {
		data[i] -= d;
	}

	@Override
	public void divideAt(int i, double d) {
		data[i] /= d;
	}

	@Override
	public void multiplyAt(int i, double d) {
		data[i] *= d;
	}

	@Override
	public void set(int i, int j, double value) {
		if ((j < 0) || (j >= cols)) throw new IndexOutOfBoundsException();
		data[(i * cols) + j] = value;
	}

	@Override
	public void applyOp(Op op) {
		op.applyTo(data);
	}

	public void addMultiple(Matrix m, double factor) {
		checkSameShape(m);
		DoubleArrays.addMultiple(data,m.data,factor);
	}
	
	public void setMultiple(Matrix m, double factor) {
		checkSameShape(m);
		DoubleArrays.scaleCopy(data,m.data,factor);
	}

	@Override
	public void add(AMatrix m) {
		checkSameShape(m);
		m.addToArray(data, 0);
	}
	
	@Override
	public Matrix addCopy(Matrix a) {
		checkSameShape(a);
		Matrix r=Matrix.create(rows, cols);
		Matrix.add(r,this,a);
		return r;
	}
	
	@Override
	public void add2(AMatrix a, AMatrix b) {
		if (a instanceof ADenseArrayMatrix) {
			if ((a instanceof Matrix)&&(b instanceof Matrix)) {
				add2((Matrix)a,(Matrix)b);
				return;
			}
			if (b instanceof ADenseArrayMatrix) {
				super.add((ADenseArrayMatrix)a,(ADenseArrayMatrix)b);
				return;
			}
		}
		checkSameShape(a);
		checkSameShape(b);
		a.addToArray(data, 0);
		b.addToArray(data, 0);
	}
	
	public static void add(Matrix dest, Matrix a, Matrix b) {
		dest.checkSameShape(a);
		dest.checkSameShape(b);
		DoubleArrays.addResult(dest.data, a.data, b.data);
	}
	
	public static void scale(Matrix dest, Matrix src, double factor) {
		dest.checkSameShape(src);
		dest.setMultiple(src, factor);
	}
	
	public static void scaleAdd(Matrix dest, Matrix a, Matrix b, double bFactor) {	
		dest.checkSameShape(a);
		dest.checkSameShape(b);
		int len=dest.data.length;
		for (int i=0; i<len; i++) {
			dest.data[i]=a.data[i]+(bFactor*b.data[i]);
		}
	}
	
	public void add2(Matrix a, Matrix b) {
		checkSameShape(a);
		checkSameShape(b);
		DoubleArrays.add2(data, a.data,b.data);
	}

	public void add(Matrix m) {
		checkSameShape(m);
		DoubleArrays.add(data, m.data);
	}

	@Override
	public void addMultiple(AMatrix m, double factor) {
		if (m instanceof Matrix) {
			addMultiple((Matrix) m, factor);
			return;
		}
		int rc = rowCount();
		int cc = columnCount();
		m.checkShape(rc, cc);
		
		for (int i = 0; i < rc; i++) {
			m.getRow(i).addMultipleToArray(factor, 0, data, i * cols, cc);
		}
	}

	@Override
	public void add(double d) {
		DoubleArrays.add(data, d);
	}

	@Override
	public void multiply(double factor) {
		DoubleArrays.multiply(data, factor);
	}

	@Override
	public void set(AMatrix a) {
		if ((rowCount() != a.rowCount()) || (columnCount() != a.columnCount())) { throw new IllegalArgumentException(
				ErrorMessages.mismatch(this, a)); }
		a.getElements(this.data, 0);
	}

	@Override
	public void set(AVector a) {
		if ((rowCount() != a.length())) { throw new IllegalArgumentException(
				ErrorMessages.incompatibleBroadcast(a, this)); }
		a.getElements(data, 0);
		for (int i = 1; i < rows; i++) {
			System.arraycopy(data, 0, data, i * cols, cols);
		}
	}

	@Override
	public void getElements(double[] dest, int offset) {
		System.arraycopy(data, 0, dest, offset, data.length);
	}

	@Override
	public Iterator<Double> elementIterator() {
		return new StridedElementIterator(data, 0, rows * cols, 1);
	}

	@Override
	public DenseColumnMatrix getTranspose() {
		return getTransposeView();
	}

	@Override
	public DenseColumnMatrix getTransposeView() {
		return DenseColumnMatrix.wrap(cols, rows, data);
	}

	@Override
	public void set(double value) {
		Arrays.fill(data, value);
	}

	@Override
	public void reciprocal() {
		DoubleArrays.reciprocal(data, 0, data.length);
	}

	@Override
	public void clamp(double min, double max) {
		DoubleArrays.clamp(data, 0, data.length, min, max);
	}

	@Override
	public Matrix clone() {
		return new Matrix(rows, cols, DoubleArrays.copyOf(data));
	}

	@Override
	public Matrix copy() {
		return clone();
	}

	@Override
	public Matrix exactClone() {
		return clone();
	}

	@Override
	public void setRow(int i, AVector row) {
		int cc = columnCount();
		row.checkLength(cc);
		row.getElements(data, i * cc);
	}

	@Override
	public void setColumn(int j, AVector col) {
		int rc = rows;
		if (col.length() != rc)
			throw new IllegalArgumentException(ErrorMessages.mismatch(
					this.getColumn(j), col));
		for (int i = 0; i < rc; i++) {
			data[index(i, j)] = col.unsafeGet(i);
		}
	}

	@Override
	public StridedVector getBand(int band) {
		int cc = columnCount();
		int rc = rowCount();
		if ((band > cc) || (band < -rc))
			throw new IndexOutOfBoundsException(ErrorMessages.invalidBand(this,
					band));
		return StridedVector.wrap(data, (band >= 0) ? band : (-band) * cc,
				bandLength(band), cc + 1);
	}

	@Override
	protected final int index(int i, int j) {
		return i * cols + j;
	}

	@Override
	public int getArrayOffset() {
		return 0;
	}

	@Override
	public double[] getArray() {
		return data;
	}

	/**
	 * Creates a Matrix which contains ones along the main diagonal and zeros
	 * everywhere else. If square, is equal to the identity matrix.
	 * 
	 * @param numRows
	 *            Number of rows in the matrix.
	 * @param numCols
	 *            NUmber of columns in the matrix.
	 * @return A matrix with diagonal elements equal to one.
	 */
	public static Matrix createIdentity(int numRows, int numCols) {
		Matrix ret = create(numRows, numCols);

		int small = numRows < numCols ? numRows : numCols;

		for (int i = 0; i < small; i++) {
			ret.unsafeSet(i, i, 1.0);
		}

		return ret;
	}

	/**
	 * <p>
	 * Creates a square identity Matrix of the specified size.<br>
	 * <br>
	 * a<sub>ij</sub> = 0 if i &ne; j<br>
	 * a<sub>ij</sub> = 1 if i = j<br>
	 * </p>
	 * 
	 * @return A new instance of an identity matrix.
	 */
	public static Matrix createIdentity(int dims) {
		Matrix ret = create(dims, dims);

		for (int i = 0; i < dims; i++) {
			ret.unsafeSet(i, i, 1.0);
		}

		return ret;
	}

}
