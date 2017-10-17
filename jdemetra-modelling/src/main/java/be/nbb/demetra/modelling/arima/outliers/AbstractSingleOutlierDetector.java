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
package be.nbb.demetra.modelling.arima.outliers;

import be.nbb.demetra.modelling.outliers.IOutlierFactory;
import be.nbb.demetra.modelling.outliers.IOutlierVariable;
import ec.tstoolkit.arima.IArimaModel;
import ec.tstoolkit.arima.estimation.RegArimaModel;
import ec.tstoolkit.data.TableOfBoolean;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.modelling.IRobustStandardDeviationComputer;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.utilities.DoubleList;
import java.util.ArrayList;

public abstract class AbstractSingleOutlierDetector<T extends IArimaModel> {

    protected final IRobustStandardDeviationComputer sdevComputer;
    protected final ArrayList<IOutlierFactory> factories = new ArrayList<>();
    protected final DoubleList weights = new DoubleList();
    private RegArimaModel<T> model;
    protected int lbound;
    protected int ubound;
    protected Matrix T;
    protected Matrix coef;

    private TableOfBoolean allowedTable;

    private int posMax = -1, oMax = -1;

    /**
     *
     * @param sdevComputer
     */
    protected AbstractSingleOutlierDetector(IRobustStandardDeviationComputer sdevComputer) {
        this.sdevComputer = sdevComputer;
    }

    /**
     *
     * @param o
     */
    public void addOutlierFactory(IOutlierFactory o) {
        factories.add(o);
        weights.add(1);
        clear(true);
    }

    /**
     *
     * @param o
     * @param weight
     */
    public void addOutlierFactory(IOutlierFactory o, double weight) {
        factories.add(o);
        weights.add(weight);
        clear(true);
    }

    /**
     * @return the sdevComputer
     */
    public IRobustStandardDeviationComputer getStandardDeviationComputer() {
        return sdevComputer;
    }

    /**
     *
     * @return
     */
    protected abstract boolean calc();

    /**
     *
     * @param all
     */
    protected void clear(boolean all) {
        sdevComputer.reset();
        model = null;
        oMax = -1;
        posMax = -1;
        if (all) {
            T = null;
            coef = null;
            allowedTable = null;
        } else if (T != null) {
            T.clear();
            coef.clear();
        }
    }

    /**
     *
     */
    public void clearOutlierFactories() {
        factories.clear();
        weights.clear();
        clear(true);
    }

    /**
     *
     * @param pos
     * @param outlier
     * @return
     */
    public double coeff(int pos, int outlier) {
        return coef.get(pos, outlier);
    }

    /**
     *
     * @param pos
     * @param ioutlier
     */
    public void exclude(int pos, int ioutlier) {
        // avoid outliers outside the current range
        if (pos >= 0 && pos < allowedTable.getRowsCount()) {
            allowedTable.set(pos, ioutlier, false);
            T.set(pos, ioutlier, 0);
        }
    }

    /**
     *
     * @param pos
     * @param ioutlier
     */
    public void allow(int pos, int ioutlier) {
        // avoid outliers outside the current range
        if (pos >= 0 && pos < allowedTable.getRowsCount()) {
            allowedTable.set(pos, ioutlier, true);
            T.set(pos, ioutlier, 0);
        }
    }

    /**
     *
     * @param pos
     */
    public void exclude(int[] pos) {
        if (pos == null) {
            return;
        }
        for (int i = 0; i < pos.length; ++i) {
            for (int j = 0; j < factories.size(); ++j) {
                exclude(pos[i], j);
            }
        }
    }

    /**
     *
     * @param pos
     */
    public void allow(int[] pos) {
        if (pos == null) {
            return;
        }
        for (int i = 0; i < pos.length; ++i) {
            for (int j = 0; j < factories.size(); ++j) {
                allow(pos[i], j);
            }
        }
    }

    /**
     *
     * @param pos
     */
    public void exclude(int pos) {
        for (int j = 0; j < factories.size(); ++j) {
            exclude(pos, j);
        }
    }

    /**
     *
     * @return
     */
    public int getLBound() {
        return lbound;
    }

    /**
     *
     * @return
     */
    public double getMAD() {
        return sdevComputer.get();
    }

    /**
     *
     * @return
     */
    public IOutlierVariable getMaxOutlier() {
        if (posMax == -1) {
            searchMax();
        }
        if (oMax == -1) {
            return null;
        }
        return factories.get(oMax).create(posMax);
    }

    /**
     *
     * @return
     */
    public int getMaxOutlierType() {
        if (oMax == -1) {
            searchMax();
        }
        return oMax;
    }

    /**
     *
     * @return
     */
    public int getMaxPosition() {
        if (posMax == -1) {
            searchMax();
        }
        return posMax;
    }

    /**
     *
     * @return
     */
    public double getMaxTStat() {
        if (oMax == -1) {
            searchMax();
        }
        double tmax = T(posMax, oMax);
        return tmax;
    }

    /**
     *
     * @return
     */
    public RegArimaModel<T> getModel() {
        return model;
    }

    /**
     *
     * @return
     */
    public int getOutlierFactoriesCount() {
        return factories.size();
    }

    /**
     *
     * @param i
     * @return
     */
    public IOutlierFactory getOutlierFactory(int i) {
        return factories.get(i);
    }

    /**
     *
     * @return
     */
    public int getUBound() {
        return ubound;
    }

    /**
     *
     * @param pos
     * @param outlier
     * @return
     */
    public boolean isDefined(int pos, int outlier) {
        return allowedTable.get(pos, outlier);
    }

    public void setBounds(int lbound, int ubound) {
        this.lbound = lbound;
        this.ubound = ubound;
    }

    /**
     *
     * @param n
     */
    protected void prepare(int n) {
        T = new Matrix(n, factories.size());
        coef = new Matrix(n, factories.size());
        allowedTable = new TableOfBoolean(n, factories.size());
        for (int i = 0; i < factories.size(); ++i) {
            IOutlierFactory fac = getOutlierFactory(i);
            int jstart = Math.max(lbound, fac.excludingZoneAtStart());
            int jend = Math.min(ubound, n - fac.excludingZoneAtEnd());
            for (int j = jstart; j < jend; ++j) {
                allowedTable.set(j, i, true);
            }
        }
    }

    /**
     *
     * @param model
     * @return
     */
    public boolean process(RegArimaModel<T> model) {
        clear(false);
        this.model = model.clone();
        return calc();
    }

    private void searchMax() {
        if (T == null) {
            return;
        }
        double max = 0;
        int imax = -1;
        double[] table = T.internalStorage();
        for (int i = 0, c = 0; c < T.getColumnsCount(); ++c) {
            double w = weights.get(c);

            for (int r = 0; r < T.getRowsCount(); ++r, ++i) {
                double cur = Math.abs(table[i]) * w;
                if (cur > max) {
                    imax = i;
                    max = cur;
                }
            }
            if (imax == -1) {
                return;
            }
        }
        posMax = imax % T.getRowsCount();
        oMax = imax / T.getRowsCount();
    }

    /**
     *
     * @param pos
     * @param outlier
     * @param val
     */
    protected void setT(int pos, int outlier, double val) {
        T.set(pos, outlier, val);
    }

    /**
     *
     * @param pos
     * @param outlier
     * @param val
     */
    protected void setCoefficient(int pos, int outlier, double val) {
        coef.set(pos, outlier, val);
    }

    /**
     *
     * @param pos
     * @param outlier
     * @return
     */
    public double T(int pos, int outlier) {
        return T.get(pos, outlier);
    }

    /**
     * @return the sdevComputer
     */
    public IRobustStandardDeviationComputer getSdevComputer() {
        return sdevComputer;
    }

    /**
     * @return the factories
     */
    public ArrayList<IOutlierFactory> getFactories() {
        return factories;
    }

    /**
     * @return the weights
     */
    public DoubleList getWeights() {
        return weights;
    }

    /**
     * @return the lbound
     */
    public int getLbound() {
        return lbound;
    }

    /**
     * @return the ubound
     */
    public int getUbound() {
        return ubound;
    }

    /**
     * @return the T
     */
    public Matrix getT() {
        return T;
    }

    /**
     * @return the coef
     */
    public Matrix getCoef() {
        return coef;
    }

    void allow(IOutlierVariable o) {
        for (int i = 0; i < factories.size(); ++i) {
            IOutlierFactory exemplar = factories.get(i);
            if (exemplar.getOutlierCode().equals(o.getCode())) {
                allow(o.getPosition() - lbound, i);
                return;
            }
        }
    }

    void exclude(IOutlierVariable o) {
        for (int i = 0; i < factories.size(); ++i) {
            IOutlierFactory exemplar = factories.get(i);
            if (exemplar.getOutlierCode().equals(o.getCode())) {
                exclude(o.getPosition() - lbound, i);
                return;
            }
        }
    }
}
