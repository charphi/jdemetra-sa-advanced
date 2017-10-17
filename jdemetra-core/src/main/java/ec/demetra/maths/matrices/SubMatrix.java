/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.demetra.maths.matrices;

import ec.demetra.data.DataBlock;
import ec.demetra.data.IArrayOfDoubles;
import java.util.Iterator;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public final class SubMatrix implements Cloneable {

    final double[] m_data;
    final int m_row_inc, m_col_inc;
    int m_start, m_nrows, m_ncols;

    /**
     * @since 2.2
     */
    @FunctionalInterface
    public static interface MatrixFunction {

        /**
         * Applies this function to the given arguments.
         *
         * @param row
         * @param column
         * @return the function result
         */
        double apply(int row, int column);
    }

    public static abstract class RCIterator implements Iterator<DataBlock> {

        final SubMatrix M;
        DataBlock cur;
        int pos = 0;

        public int getPosition() {
            return pos - 1;
        }

        public void reset() {
            pos = 0;
            cur = null;
        }

        RCIterator(SubMatrix M) {
            this.M = M;
        }
    }

    private static class Rows extends RCIterator {

        Rows(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_nrows;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = M.row(0);
            } else {
                cur.slide(M.m_row_inc);
            }
            ++pos;
            return cur;
        }
    }

    private static class URows extends RCIterator {

        URows(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_nrows;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = M.row(0);
            } else {
                cur.slide(M.m_row_inc).bshrink();
            }
            ++pos;
            return cur;
        }
    }

    private static class LRows extends RCIterator {

        LRows(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_nrows;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = DataBlock.of(M.m_data, M.m_start, M.m_start + M.m_col_inc, M.m_col_inc);
            } else {
                cur.slide(M.m_row_inc).eexpand();
            }
            ++pos;
            return cur;
        }

    }

    private static class Columns extends RCIterator {

        Columns(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_ncols;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = M.column(0);
            } else {
                cur.slide(M.m_col_inc);
            }
            ++pos;
            return cur;
        }
    }

    private static class LColumns extends RCIterator {

        LColumns(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_ncols;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = M.column(0);
            } else {
                cur.slide(M.m_col_inc).bshrink();
            }
            ++pos;
            return cur;
        }
    }

    private static class UColumns extends RCIterator {

        UColumns(SubMatrix M) {
            super(M);
        }

        @Override
        public boolean hasNext() {
            return pos < M.m_ncols;
        }

        @Override
        public DataBlock next() {
            if (cur == null) {
                cur = DataBlock.of(M.m_data, M.m_start, M.m_start + M.m_row_inc, M.m_row_inc);
            } else {
                cur.slide(M.m_col_inc).eexpand();
            }
            ++pos;
            return cur;
        }
    }

    /**
     * Creates a new instance of SubMatrix
     *
     * @param data
     * @param nrows
     * @param ncols
     */
    SubMatrix(final double[] data, final int nrows, final int ncols) {
        m_data = data;
        m_nrows = nrows;
        m_ncols = ncols;
        m_row_inc = 1;
        m_col_inc = nrows;
    }

    /**
     *
     * @param data
     * @param start
     * @param nrows
     * @param ncols
     * @param rowinc
     * @param colinc
     */
    SubMatrix(final double[] data, final int start, final int nrows,
            final int ncols, final int rowinc, final int colinc) {
        m_data = data;
        m_start = start;
        m_nrows = nrows;
        m_ncols = ncols;
        m_row_inc = rowinc;
        m_col_inc = colinc;
    }

    public void set(final MatrixFunction fn) {
        int c = 0;
        for (DataBlock col : columns()) {
            final int cur = c++;
            col.set(r -> fn.apply(r, cur));
        }
    }

    public void set(int row, int col, double value) {
        m_data[m_start + row * m_row_inc + col * m_col_inc] = value;
    }

    /**
     *
     * @return
     */
    public IArrayOfDoubles diagonal() {
        int n = Math.min(m_nrows, m_ncols), inc = m_row_inc + m_col_inc;
        return DataBlock.of(m_data, m_start, m_start + inc * n, inc);
    }

    /**
     *
     * @param pos
     * @return
     */
    public IArrayOfDoubles subDiagonal(int pos) {
        if (pos >= m_ncols) {
            return DataBlock.EMPTY;
        }
        if (-pos >= m_nrows) {
            return DataBlock.EMPTY;
        }
        int beg = m_start, inc = m_row_inc + m_col_inc;
        int n;
        if (pos > 0) {
            beg += pos * m_col_inc;
            n = Math.min(m_nrows, m_ncols - pos);
        } else if (pos < 0) {
            beg -= pos * m_row_inc;
            n = Math.min(m_nrows + pos, m_ncols);
        } else {
            n = Math.min(m_nrows, m_ncols);
        }
        return DataBlock.of(m_data, beg, beg + inc * n, inc);
    }

    @Override
    public SubMatrix clone() throws CloneNotSupportedException {
        return (SubMatrix) super.clone();
    }

    /**
     *
     * @return
     */
    public SubMatrix transpose() {
        return new SubMatrix(m_data, m_start, m_ncols, m_nrows, m_col_inc,
                m_row_inc);
    }

    public boolean isEmpty() {
        return getColumnsCount() <= 0 || getRowsCount() <= 0;
    }

    /**
     *
     * @param r0
     * @param r1
     * @param c0
     * @param c1
     * @return
     */
    public SubMatrix extract(final int r0, final int r1, final int c0,
            final int c1) {
        return new SubMatrix(m_data, m_start + r0 * m_row_inc + c0 * m_col_inc,
                r1 - r0, c1 - c0, m_row_inc, m_col_inc);
    }

    /**
     *
     * @param r0
     * @param c0
     * @param nrows
     * @param ncols
     * @param rowinc
     * @param colinc
     * @return
     */
    public SubMatrix extract(final int r0, final int c0, final int nrows,
            final int ncols, final int rowinc, final int colinc) {
        return new SubMatrix(m_data, m_start + r0 * m_row_inc + c0 * m_col_inc,
                nrows, ncols, m_row_inc * rowinc, m_col_inc * colinc);
    }

    /**
     *
     * @param row
     * @param col
     * @return
     */
    public double get(final int row, final int col) {
        return m_data[m_start + row * m_row_inc + col * m_col_inc];
    }

    /**
     *
     * @return
     */
    public int getColumnsCount() {
        return m_ncols;
    }

    /**
     *
     * @return
     */
    public int getRowsCount() {

        return m_nrows;
    }

    public void add(SubMatrix x) {
        if (m_col_inc == 1 && x.m_col_inc == 1) {
            DataBlock row = row(0);
            DataBlock xrow = x.row(0);
            int c = 0;
            do {
                row.add(xrow);
                row.slide(m_row_inc);
                row.slide(x.m_row_inc);
            } while (++c < m_nrows);
        } else {
            DataBlock col = column(0);
            DataBlock xcol = x.column(0);
            int c = 0;
            do {
                col.add(xcol);
                col.slide(m_col_inc);
                col.slide(x.m_col_inc);
            } while (++c < m_ncols);

        }
    }

    private static final int PROD_THRESHOLD = 6;

    /**
     *
     * @param lm
     * @param rm
     */
    public void product(final SubMatrix lm, final SubMatrix rm) {
        if (lm.getColumnsCount() < PROD_THRESHOLD * (lm.getRowsCount())) {
//            Iterator<DataBlock> rcols = rm.columns().iterator();
//            for (DataBlock col : columns()) {
//                Iterator<DataBlock> lcols = lm.columns().iterator();
//                DataBlock rcol = rcols.next();
//                int k = 0;
//                col.setAY(rcol.get(k++), lcols.next());
//                while (lcols.hasNext()) {
//                    col.addAY(rcol.get(k++), lcols.next());
//                }
//            }
            DataBlock rcol = rm.column(0), col = column(0);
            for (int i = 0; i < m_ncols; ++i) {
                DataBlock lcol = lm.column(0);
                col.setAY(rcol.get(0), lcol);
                for (int j = 1; j < lm.m_ncols; ++j) {
                    lcol.slide(lm.m_col_inc);
                    col.addAY(rcol.get(j), lcol);
                }
                rcol.slide(rm.m_col_inc);
                col.slide(m_col_inc);
            }
        } else {
//            Iterator<DataBlock> rcols = rm.columns().iterator();
//            for (DataBlock col : columns()) {
//                int k = 0;
//                DataBlock rcol = rcols.next();
//                for (DataBlock row : lm.rows()) {
//                    col.set(k++, row.dot(rcol));
//                }
//            }
            DataBlock rcol = rm.column(0), col = column(0);
            for (int i = 0; i < m_ncols; ++i) {
                DataBlock lrow = lm.row(0);
                for (int j = 0; j < lm.m_nrows; ++j) {
                    col.set(j, lrow.dot(rcol));
                    lrow.slide(lm.m_row_inc);
                }
                rcol.slide(rm.m_col_inc);
                col.slide(m_col_inc);
            }
        }
    }

    /**
     *
     * @param c
     * @return
     */
    public DataBlock column(final int c) {
        int beg = m_start + c * m_col_inc, end = beg + m_row_inc * m_nrows;
        return DataBlock.of(m_data, beg, end, m_row_inc);
    }

    /**
     *
     * @param r
     * @return
     */
    public DataBlock row(final int r) {
        int beg = m_start + r * m_row_inc, end = beg + m_col_inc * m_ncols;
        return DataBlock.of(m_data, beg, end, m_col_inc);
    }

    public Iterable<DataBlock> rows() {
        return () -> new Rows(SubMatrix.this);
    }

    public RCIterator lrows() {
        return new LRows(this);
    }

    public RCIterator urows() {
        return new URows(this);
    }

    public Iterable<DataBlock> columns() {
        return () -> new Columns(SubMatrix.this);
    }

    public RCIterator lcolumns() {
        return new LColumns(this);
    }

    public RCIterator ucolumns() {
        return new UColumns(this);
    }

    /**
     *
     * @param dr
     * @param dc
     */
    public void move(final int dr, final int dc) {
        m_start += dr * m_row_inc + dc * m_col_inc;
    }

    /**
     * The following methods can be used to create fast iterations. They avoid
     * the creation of unnecessary objects
     *
     * example:
     *
     * (Sub)Matrix data=... SubMatrix cur=data.topLeft(); while (...){
     * cur.next(r,c); }
     */
    /**
     * Takes the bottom-right of the current submatrix as the new starting
     * position and updates the number of rows/columns
     *
     * @param nrows The number of rows in the submatrix
     * @param ncols The number of columns in the submatrix
     */
    public void next(int nrows, int ncols) {
        m_start += m_nrows * m_row_inc + m_ncols * m_col_inc;
        m_nrows = nrows;
        m_ncols = ncols;
    }

    /**
     * Takes the bottom-right of the current submatrix as the new starting
     * position
     */
    public void next() {
        m_start += m_nrows * m_row_inc + m_ncols * m_col_inc;
    }

    /**
     * Takes the top-right of the current submatrix as the new starting position
     * and updates the number of columns
     *
     * @param ncols The number of columns in the submatrix
     */
    public void hnext(int ncols) {
        m_start += m_ncols * m_col_inc;
        m_ncols = ncols;
    }

    /**
     * Takes the top-right of the current submatrix as the new starting position
     */
    public void hnext() {
        m_start += m_ncols * m_col_inc;
    }

    /**
     * Takes the bottom-left of the current submatrix as the new starting
     * position and updates the number of rows
     *
     * @param nrows The number of rows in the submatrix
     */
    public void vnext(int nrows) {
        m_start += m_nrows * m_row_inc;
        m_nrows = nrows;
    }

    /**
     * Takes the bottom-left of the current submatrix as the new starting
     * position
     */
    public void vnext() {
        m_start += m_nrows * m_row_inc;
    }

    /**
     * Takes the top-left of the current submatrix as the new ending position
     * and updates the number of rows/columns
     *
     * @param nrows The number of rows in the submatrix
     * @param ncols The number of columns in the submatrix
     */
    public void previous(int nrows, int ncols) {
        m_start -= nrows * m_row_inc + ncols * m_col_inc;
        m_nrows = nrows;
        m_ncols = ncols;
    }

    /**
     * Takes the top-left of the current submatrix as the new ending position
     */
    public void previous() {
        m_start -= m_nrows * m_row_inc + m_ncols * m_col_inc;
    }

    /**
     * Takes the bottom-left of the current submatrix as the new ending position
     * and updates the number of columns
     *
     * @param ncols The number of columns in the submatrix
     */
    public void hprevious(int ncols) {
        m_start -= ncols * m_col_inc;
        m_ncols = ncols;
    }

    /**
     * Takes the bottom-left of the current submatrix as the new ending position
     */
    public void hprevious() {
        m_start -= m_ncols * m_col_inc;
    }

    /**
     * Takes the top-right of the current submatrix as the new ending position
     * and updates the number of rows
     *
     * @param nrows The number of rows in the submatrix
     */
    public void vprevious(int nrows) {
        m_start -= nrows * m_row_inc;
        m_nrows = nrows;
    }

    /**
     * Takes the top-right of the current submatrix as the new ending position
     */
    public void vprevious() {
        m_start -= m_nrows * m_row_inc;
    }

    /**
     * Top-left empty sub-matrix. To be used with next(a,b)
     *
     * @return An empty sub-matrix
     */
    public SubMatrix topLeft() {
        return new SubMatrix(m_data, m_start, 0, 0, m_row_inc, m_col_inc);
    }

    /**
     * Top-left sub-matrix
     *
     * @param nr Number of rows. Could be 0.
     * @param nc Number of columns. Could be 0.
     * @return A nr src nc sub-matrix
     */
    public SubMatrix topLeft(int nr, int nc) {
        return new SubMatrix(m_data, m_start, nr, nc, m_row_inc, m_col_inc);
    }

    /**
     * Top-left sub-matrix
     *
     * @param nr Number of rows. Could be 0.
     * @return A nr src nc sub-matrix
     */
    public SubMatrix top(int nr) {
        return new SubMatrix(m_data, m_start, nr, m_ncols, m_row_inc, m_col_inc);
    }

    /**
     * Top-left sub-matrix
     *
     * @param nc Number of columns. Could be 0.
     * @return A nr src nc sub-matrix
     */
    public SubMatrix left(int nc) {
        return new SubMatrix(m_data, m_start, m_nrows, nc, m_row_inc, m_col_inc);
    }

    /**
     * bottom-right sub-matrix.
     *
     * @return An empty sub-matrix
     */
    public SubMatrix bottomRight() {
        int start = m_nrows * m_row_inc + m_ncols * m_col_inc;
        return new SubMatrix(m_data, start, 0, 0, m_row_inc, m_col_inc);
    }

    /**
     * Bottom-right sub-matrix
     *
     * @param nr Number of rows. Could be 0.
     * @param nc Number of columns. Could be 0.
     * @return A nr src nc sub-matrix
     */
    public SubMatrix bottomRight(int nr, int nc) {
        int start = (m_nrows - nr) * m_row_inc + (m_ncols - nc) * m_col_inc;
        return new SubMatrix(m_data, start, nr, nc, m_row_inc, m_col_inc);
    }

    /**
     * Bottom sub-matrix
     *
     * @param nr Number of rows. Could be 0.
     * @return The last n rows
     */
    public SubMatrix bottom(int nr) {
        return new SubMatrix(m_data, m_start + m_nrows - nr, nr, m_ncols, m_row_inc, m_col_inc);
    }

    /**
     * right sub-matrix
     *
     * @param nc Number of columns. Could be 0.
     * @return The nc right columns
     */
    public SubMatrix right(int nc) {
        return new SubMatrix(m_data, m_start + (m_ncols - nc) * m_col_inc, m_nrows, nc, m_row_inc, m_col_inc);
    }
//</editor-fold>

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DataBlock row : rows()) {
            builder.append(row.toString());
        }
        return builder.toString();
    }
}
