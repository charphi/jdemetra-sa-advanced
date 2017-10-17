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

import be.nbb.demetra.modelling.outliers.IOutlierVariable;
import ec.tstoolkit.arima.IArimaModel;
import ec.tstoolkit.arima.estimation.ConcentratedLikelihoodEstimation;
import ec.tstoolkit.arima.estimation.RegArimaModel;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.maths.linearfilters.BackFilter;
import ec.tstoolkit.maths.linearfilters.RationalBackFilter;
import ec.tstoolkit.maths.polynomials.Polynomial;
import ec.tstoolkit.modelling.IRobustStandardDeviationComputer;

/**
 *
 * @author Jean Palate
 * @param <T>
 */
public class FastOutlierDetector<T extends IArimaModel> extends
        AbstractSingleOutlierDetector<T> {

    private double[] m_el;
    private IArimaModel m_stmodel;
    private BackFilter m_ur;


    public FastOutlierDetector() {
        super(IRobustStandardDeviationComputer.mad());
    }
    /**
     *
     * @param computer
     */
    public FastOutlierDetector(IRobustStandardDeviationComputer computer) {
        super(computer);
    }

    /**
     *
     * @return
     */
    @Override
    protected boolean calc() {
        if (!initmodel()) {
            return false;
        }
        for (int i = 0; i < getOutlierFactoriesCount(); ++i) {
            processOutlier(i);
        }

        return true;
    }

    /**
     *
     * @param all
     */
    @Override
    protected void clear(boolean all) {
        super.clear(all);
        m_el = null;
    }

    private boolean initmodel() {
        RegArimaModel<T> model = getModel();
        m_stmodel = (IArimaModel) model.getArma();
        m_ur = getModel().getDifferencingFilter();
        ConcentratedLikelihoodEstimation estimation = new ConcentratedLikelihoodEstimation();
        if (!estimation.estimate(model)) {
            return false;
        }
        m_el = estimation.getResiduals();
        DataBlock EL = new DataBlock(m_el);
        getStandardDeviationComputer().compute(EL);
        return true;
    }

    private void processOutlier(int idx) {
        int nl = m_el.length;
        int d = m_ur.getDegree();
        int n = nl + d;
//        double[] o = new double[n];
//        DataBlock O = new DataBlock(o);
        IOutlierVariable outlier = getOutlierFactory(idx).create(getLbound());
        IOutlierVariable.FilterRepresentation representation = outlier.getFilterRepresentation();
        if (representation == null) {
            return;
        }
        IArimaModel model = getModel().getArima();
        RationalBackFilter pi = model.getPiWeights();
        double[] o = pi.times(representation.filter).getWeights(n);
        double corr = 0;
        if (d == 0 && representation.correction != 0) {
            Polynomial ar = model.getAR().getPolynomial();
            Polynomial ma = model.getMA().getPolynomial();
            corr = representation.correction * ar.evaluateAt(1) / ma.evaluateAt(1);
            for (int i = 0; i < n; ++i) {
                o[i] += corr;
            }
        }

        // o contains the filtered outlier
        // we start at the end
        //double maxval = 0;
        double sxx = 0;
        if (corr != 0) {
            sxx = corr * corr * nl;
        }

        int lb=getLBound(), ub=getUBound();
        for (int ix = 0; ix < n; ++ix) {
            double rmse = getStandardDeviationComputer().get(n - ix - 1 - d);
            sxx += o[ix] * o[ix];
            if (corr != 0) {
                sxx -= corr * corr;
            }
            int kmax = ix + 1;
            if (kmax > nl) {
                kmax = nl;
                sxx -= o[ix - nl] * o[ix - nl];
                if (corr != 0) {
                    sxx += corr * corr;
                }
            }
            double sxy = 0;
            for (int k = 0, ek = nl - 1; k < kmax; ++k, --ek) {
                sxy += m_el[ek] * o[ix - k];
            }
            if (corr != 0) {
                double cxy = 0;
                for (int k = 0; k < nl - kmax; ++k) {
                    cxy += m_el[k];
                }
                sxy += cxy * corr;
            }
            int pos = n - 1 - ix;
            if (pos >= lb && pos < ub) {
                double c = sxy / sxx;
                double val = c * Math.sqrt(sxx) / rmse;
                setCoefficient(pos, idx, c);
                setT(pos, idx, val);
            }
        }
    }
}
