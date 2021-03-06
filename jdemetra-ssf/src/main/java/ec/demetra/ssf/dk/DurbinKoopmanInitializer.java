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

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.SsfException;
import ec.demetra.ssf.State;
import ec.demetra.ssf.StateInfo;
import ec.demetra.ssf.univariate.ISsf;
import ec.demetra.ssf.univariate.ISsfData;
import ec.demetra.ssf.univariate.ISsfMeasurement;
import ec.demetra.ssf.univariate.OrdinaryFilter;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class DurbinKoopmanInitializer implements OrdinaryFilter.Initializer {

    private final IDiffuseFilteringResults results;
    private DiffuseState state;
    private DiffuseUpdateInformation pe;
    private ISsfMeasurement measurement;
    private ISsfDynamics dynamics;
    private ISsfData data;
    private double norm = 0;

    public DurbinKoopmanInitializer() {
        this.results = null;
    }

    /**
     *
     * @param results
     */
    public DurbinKoopmanInitializer(IDiffuseFilteringResults results) {
        this.results = results;
    }

    /**
     * Computes a(t+1|t), P(t+1|t) from a(t|t), P(t|t) a(t+1|t) = T(t)a(t|t)
     * P(t+1|t) = T(t)P(t|t)T'(t) + V(t)
     */
    protected void pred(int t) {
        SubMatrix P = state.P().all();
        DataBlock a = state.a();
        dynamics.TX(t, a);
        dynamics.TVT(t, P);
        dynamics.addV(t, P);
        dynamics.TVT(t, state.Pi().all());
    }

    /**
     * Computes: e(t)=y(t) - Z(t)a(t|t-1)) F(t)=Z(t)P(t|t-1)Z'(t)+H(t) F(t) =
     * L(t)L'(t) E(t) = e(t)L'(t)^-1 K(t)= P(t|t-1)Z'(t)L'(t)^-1
     *
     * Not computed for missing values
     *
     * @param t
     * @return false if it has not been computed (missing value), true otherwise
     */
    protected boolean error(int t) {
        // computes the gain of the filter and the prediction error 
        // calc f and fi
        // fi = Z Pi Z' , f = Z P Z' + H
        double fi = measurement.ZVZ(t, state.Pi().all());
        if (Math.abs(fi) < State.ZERO) {
            fi = 0;
        }
        pe.setDiffuseNorm2(fi);
        double f = measurement.ZVZ(t, state.P().all());
        if (measurement.hasErrors()) {
            f += measurement.errorVariance(t);
        }
        if (Math.abs(f) / norm < State.ZERO) {
            f = 0;
        }
        pe.setVariance(f);
        if (data.hasData()) {
            double y = data.get(t);
            if (Double.isNaN(y)) {
                pe.setMissing();
                return false;
            } else {
                pe.set(y - measurement.ZX(t, state.a()));
            }
        }
        measurement.ZM(t, state.P().all(), pe.M());
        if (pe.isDiffuse()) {
            measurement.ZM(t, state.Pi().all(), pe.Mi());
        }
        return true;
    }

    /**
     *
     * @param fstate
     * @param ssf
     * @param data
     * @return
     */
    @Override
    public int initialize(final State fstate, final ISsf ssf, final ISsfData data) {
        measurement = ssf.getMeasurement();
        dynamics = ssf.getDynamics();
        this.data = data;
        if (!initState()) {
            return -1;
        }
        int t = 0, end = data.getLength();
        while (t < end) {
            if (isZero(this.state.Pi())) {
                break;
            }
            if (results != null) {
                results.save(t, state, StateInfo.Forecast);
            }
            if (error(t)) {
                if (results != null) {
                    results.save(t, pe);
                }
                update();
            } else if (results != null) {
                results.save(t, pe);
            }
            if (results != null) {
                results.save(t, state, StateInfo.Concurrent);
            }
            pred(t++);
        }
        if (results != null) {
            results.close(t);
        }
        fstate.P().copy(state.P());
        fstate.a().copy(state.a());
        return t;
    }

    private boolean initState() {
        state = DiffuseState.of(dynamics);
        if (state == null) {
            return false;
        }
        norm = state.Pi().nrm2();
        pe = new DiffuseUpdateInformation(dynamics.getStateDim());
        return true;
    }

    private boolean isZero(final Matrix P) {
        return P.isZero(1e-9 * norm);
    }

    private void update() {
        if (pe.isDiffuse()) {
            update1();
        } else {
            update0();
        }
    }

    private void update0() {
        // variance

        double f = pe.getVariance(), e = pe.get();
        DataBlock C = pe.M();
        SymmetricMatrix.addXaXt(state.P(), -1 / f, C);

        // state
        // a0 = Ta0 + f1*TMi*v0. Reuse Mf as temporary buffer
        if (data.hasData()) {
            // prod(n, m_T, m_a0, m_tmp);
            double c = e / f;
//            for (int i = 0; i < m_r; ++i)
//                state.A.set(i, state.A.get(i) + state.C.get(i) * c);
            state.a().addAY(c, C);
        }
    }

    private void update1() {
        // calc f0, f1, f2
//        double f1 = 1 / pe.fi;
//        double f2 = -pe.f * f1 * f1;
        double f = pe.getVariance(), e = pe.get(), fi = pe.getDiffuseNorm2();
        DataBlock C = pe.M(), Ci = pe.Mi();

        // Pi = Pi - f1* (Ci)(Ci)'
        SymmetricMatrix.addXaXt(state.Pi(), -1 / fi, Ci);

        // P = P - f2*(Ci)(Ci)'-f1(Ci*Cf' + Cf*Ci')
        // = P + f/(fi*fi)(Ci)(Ci)' - 1/fi(Ci*Cf' + Cf*Ci')
        // = P - 1/f (Cf)(Cf') + f/(fi*fi)(Ci)(Ci)'- 1/fi(Ci*Cf' + Cf*Ci')+ 1/f (Cf)(Cf')
        // = P  - 1/f (Cf)(Cf') + (1/f)(Cf - (f/fi)Ci)(Cf - (f/fi)Ci)'
        SymmetricMatrix.addXaXt(state.P(), -1 / f, C);
        DataBlock tmp = C.deepClone();
        tmp.addAY(-f / fi, Ci);
        SymmetricMatrix.addXaXt(state.P(), 1 / f, tmp);

        if (data.hasData()) {
            // a0 = Ta0 + f1*TMi*v0. 
            state.a().addAY(e / fi, Ci);
        }
    }

}
