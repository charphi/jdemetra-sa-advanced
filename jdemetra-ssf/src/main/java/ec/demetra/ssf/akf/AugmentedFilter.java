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
package ec.demetra.ssf.akf;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;
import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.State;
import ec.demetra.ssf.StateInfo;
import ec.demetra.ssf.univariate.ISsf;
import ec.demetra.ssf.univariate.ISsfData;
import ec.demetra.ssf.univariate.ISsfMeasurement;

/**
 *
 * @author Jean Palate
 */
public class AugmentedFilter {

    private AugmentedState state;
    private AugmentedUpdateInformation pe;
    private ISsfMeasurement measurement;
    private ISsfDynamics dynamics;
    private ISsfData data;
    private boolean missing;
    private final boolean collapsing;
    private int collapsingPos = -1;

    /**
     *
     */
    public AugmentedFilter() {
        collapsing = false;
    }

    public AugmentedFilter(final boolean collapsing) {
        this.collapsing = collapsing;
    }

    /**
     * Computes a(t+1|t), P(t+1|t) from a(t|t), P(t|t)
     */
    private void pred(int t) {
        SubMatrix P = state.P().all();
        DataBlock a = state.a();
        dynamics.TX(t, a);
        dynamics.TM(t, state.B());
        dynamics.TVT(t, P);
        dynamics.addV(t, P);
    }

    protected boolean error(int t) {
         missing = data.isMissing(t);
        if (missing) {
            pe.E().set(0);
            pe.M().set(0);
            // pe_ = null;
            return false;
        } else {
            // pe_ = new PredictionError(ssf_.getStateDim(), 1);
            // K = PZ'/f
            // computes (ZP)' in K'. Missing values are set to 0 
            // Z~v x r, P~r x r, K~r x v
            DataBlock C = pe.M();
            // computes ZPZ'; results in pe_.L
            //measurement.ZVZ(pos_, state_.P.subMatrix(), F);
            measurement.ZM(t, state.P().all(), C);
            double v = measurement.ZX(t, C);
            if (measurement.hasErrors()) {
                v += measurement.errorVariance(t);
            }
            if (v < State.ZERO) {
                v = 0;
            }
            pe.setVariance(v);
            // We put in K  PZ'*(ZPZ'+H)^-1 = PZ'* F^-1 = PZ'*(LL')^-1/2 = PZ'(L')^-1
            // K L' = PZ' or L K' = ZP

            double y = data.get(t);
            pe.set(y - measurement.ZX(t, state.a()));
            measurement.ZM(t, state.B(), pe.E());
            pe.E().chs();
            return true;
        }
    }

    protected void update() {
        double v = pe.getVariance(), e = pe.get();
        // P = P - (M)* F^-1 *(M)' --> Symmetric
        // PZ'(LL')^-1 ZP' =PZ'L'^-1*L^-1*ZP'
        // a = a + (M)* F^-1 * v
        state.a().addAY(e / v, pe.M());
        DataBlockIterator acols = state.B().columns();
        DataBlock acol = acols.getData();
        do {
            acol.addAY(pe.E().get(acols.getPosition()) / v, pe.M());
        } while (acols.next());
        update(state.P(), v, pe.M());
    }

    /**
     *
     * @return
     */
    public AugmentedState getState() {
        return state;
    }

    public int getCollapsingPosition() {
        return collapsingPos;
    }

    private boolean initState() {
        state = AugmentedState.of(dynamics);
        if (state == null) {
            return false;
        }
        pe = new AugmentedUpdateInformation(dynamics.getStateDim(), dynamics.getNonStationaryDim());
        return true;
    }

    /**
     *
     * @param ssf
     * @param data
     * @param rslts
     * @return
     */
    public boolean process(final ISsf ssf, final ISsfData data, final IAugmentedFilteringResults rslts) {
        measurement = ssf.getMeasurement();
        dynamics = ssf.getDynamics();
        this.data = data;
        if (!initState()) {
            return false;
        }
        int t=0, end = data.getLength();
        while (t < end) {
            if (collapse(t, rslts)) {
                break;
            }
            if (rslts != null) {
                rslts.save(t, state, StateInfo.Forecast);
            }
            if (error(t)) {
                if (rslts != null) {
                    rslts.save(t, pe);
                }
                update();
            }  
            if (rslts != null) {
                rslts.save(t, state, StateInfo.Concurrent);
            }
            pred(t++);
        }
        return true;
    }

    // P -= c*r
    private void update(Matrix P, double v, DataBlock C) {//, DataBlock r) {
        SymmetricMatrix.addXaXt(P, -1 / v, C);
    }

    protected boolean collapse(int t, IAugmentedFilteringResults decomp) {
        if (!collapsing) {
            return false;
        }
        // update the state vector
        if (!decomp.collapse(t, state)) {
            return false;
        }
        collapsingPos = t;
        return true;
    }

}
