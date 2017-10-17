/*
 * Copyright 2016 National Bank of Belgium
 *  
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *  
 * http://ec.europa.eu/idabc/eupl
 *  
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.demetra.ssf.dk;

import ec.demetra.ssf.univariate.ISsfMeasurement;
import ec.demetra.ssf.univariate.ISsf;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.ResultsRange;
import ec.tstoolkit.data.IDataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.maths.linearfilters.ILinearProcess;

/**
 *
 * @author Jean Palate
 */
public class DkFilter implements ILinearProcess {

    private final IBaseDiffuseFilteringResults frslts;
    private final ISsfMeasurement measurement;
    private final ISsfDynamics dynamics;
    private final int start, end, enddiffuse;
    private boolean normalized = true;

    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    public boolean isnormalized() {
        return normalized;
    }

    public boolean filter(SubMatrix x) {
        if (x.getColumnsCount() == 1) {
            return new FastDiffuseFilter1().filter(x.column(0), normalized);
        } else {
            return new FastDiffuseFilterN().filter(x, normalized);
        }
    }

    public boolean filter(DataBlock x) {
        return new FastDiffuseFilter1().filter(x, normalized);
    }

    public DkFilter(ISsf ssf, IBaseDiffuseFilteringResults frslts, ResultsRange range) {
        this.frslts = frslts;
        measurement = ssf.getMeasurement();
        dynamics = ssf.getDynamics();
        start = range.getStart();
        end = range.getEnd();
        enddiffuse = frslts.getEndDiffusePosition();
    }

    @Override
    public boolean transform(IReadDataBlock in, IDataBlock out) {
        return new FastDiffuseFilter1().transform(in, out);
    }

    @Override
    public int getOutputLength(int inputLength) {
        int n = 0;
        int imax = start + inputLength;
        if (imax > end) {
            return -1;
        }
        for (int i = start; i < enddiffuse; ++i) {
            double e = frslts.error(i), v = frslts.errorVariance(i);
            if (Double.isFinite(e) && v != 0 && frslts.diffuseNorm2(i) == 0) {
                ++n;
            }
        }
        for (int i = enddiffuse; i < imax; ++i) {
            double e = frslts.error(i), v = frslts.errorVariance(i);
            if (Double.isFinite(e) && v != 0) {
                ++n;
            }
        }
        return n;
    }

    class FastDiffuseFilterN {

        private SubMatrix states;
        // temporaries
        private DataBlock tmp, scol;
        private DataBlockIterator scols;

        boolean filter(SubMatrix x, boolean normalized) {
            if (x.getRowsCount() > end - start) {
                return false;
            }
            int dim = dynamics.getStateDim();
            states = new Matrix(dim, x.getColumnsCount()).all();
            prepareTmp();
            DataBlockIterator rows = x.rows();
            DataBlock row = rows.getData();
            int pos = start;
            do {
                iterate(pos++, row, normalized);
            } while (rows.next());
            return true;
        }

        private void prepareTmp() {
            int nvars = states.getColumnsCount();
            tmp = new DataBlock(nvars);
            scols = states.columns();
            scol = scols.getData();
        }

        private void iterate(int i, DataBlock row, boolean normalized) {
            boolean missing = !Double.isFinite(frslts.error(i));
            if (!missing) {
                double f = frslts.errorVariance(i);
                double w;
                DataBlock K;
                boolean diffuse = false;
                if (i < enddiffuse) {
                    double fi = frslts.diffuseNorm2(i);
                    if (fi != 0) {
                        w = fi;
                        K = frslts.Mi(i);
                        diffuse = true;
                    } else {
                        w = f;
                        K = frslts.M(i);
                    }
                } else {
                    w = f;
                    K = frslts.M(i);
                }

                measurement.ZM(i, states, tmp);
                row.sub(tmp);
                // update the states
                scols.begin();
                int j = 0;
                do {
                    scol.addAY(row.get(j++) / w, K);
                    dynamics.TX(i, scol);
                } while (scols.next());
                if (f == 0) {
                    row.set(0);
                } else if (normalized) {
                    if (diffuse) {
                        row.set(Double.NaN);
                    } else {
                        row.mul(1 / Math.sqrt(f));
                    }
                }
            } else {
                scols.begin();
                do {
                    dynamics.TX(i, scol);
                } while (scols.next());
                row.set(Double.NaN);
            }
            //  
        }

    }

    class FastDiffuseFilter1 {

        private DataBlock state;

        boolean filter(DataBlock x, boolean normalized) {
            if (x.getLength() > end - start) {
                return false;
            }
            int dim = dynamics.getStateDim(), n = x.getLength();
            state = new DataBlock(dim);
            int pos = start, xpos = 0;
            do {
                x.set(xpos, iterate(pos, x.get(xpos), normalized));
                pos++;
                xpos++;
            } while (xpos < n);
            return true;
        }

        private double iterate(int i, double y, boolean normalized) {
            boolean missing = !Double.isFinite(frslts.error(i));
            double e = Double.NaN;
            if (!missing) {
                double f = frslts.errorVariance(i);
                double w;
                DataBlock K;
                boolean diffuse = false;
                if (i < enddiffuse) {
                    double fi = frslts.diffuseNorm2(i);
                    if (fi != 0) {
                        w = fi;
                        K = frslts.Mi(i);
                        diffuse = true;
                    } else {
                        w = f;
                        K = frslts.M(i);
                    }
                } else {
                    w = f;
                    K = frslts.M(i);
                }

                e = y - measurement.ZX(i, state);
                // update the states
                state.addAY(e / w, K);
                if (f == 0) {
                    e = 0;
                }
                if (normalized) {
                    if (diffuse) {
                        e = Double.NaN;
                    } else {
                        e /= Math.sqrt(f);
                    }
                }
            }
            dynamics.TX(i, state);
            return e;
        }

        boolean transform(IReadDataBlock in, IDataBlock out) {
            if (in.getLength() > end - start) {
                return false;
            }
            int dim = dynamics.getStateDim(), n = in.getLength();
            state = new DataBlock(dim);
            int pos = start, ipos = 0, opos = 0;
            do {
                boolean missing = !Double.isFinite(frslts.error(pos));
                if (!missing) {
                    double f = frslts.errorVariance(pos);
                    double w;
                    DataBlock K;
                    boolean diffuse = false;
                    if (pos < enddiffuse) {
                        double fi = frslts.diffuseNorm2(pos);
                        if (fi != 0) {
                            w = fi;
                            K = frslts.Mi(pos);
                            diffuse = true;
                        } else {
                            w = f;
                            K = frslts.M(pos);
                        }
                    } else {
                        w = f;
                        K = frslts.M(pos);
                    }

                    double e = in.get(ipos) - measurement.ZX(pos, state);
                    // update the states
                    state.addAY(e / w, K);
                    if (!diffuse && f != 0) {
                        out.set(opos++, e / Math.sqrt(f));
                    }
                }
                dynamics.TX(pos++, state);
            } while (++ipos < n);
            return true;
        }

    }
}
