/*
 * Copyright 2016-2017 National Bank of Belgium
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
package ec.demetra.ssf.univariate;

import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.SsfException;
import ec.demetra.ssf.State;
import ec.demetra.ssf.StateInfo;
import ec.demetra.ssf.StateStorage;
import ec.demetra.ssf.dk.sqrt.DiffuseSquareRootInitializer;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.MatrixStorage;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.matrices.SymmetricMatrix;

/**
 * /**
 * The fixed point smoother computes the expectations and the covariance
 * matrices of [M*a(fixpos) | fixpos + k]. The ordinary filter is used till
 * position fixpos, where E(a(fixpos)|fixpos), Cov((a(fixpos)|fixpos)) is
 * available. The moments of the augmented state vector a(fixpos), Ma(fixpos)
 * can then be easily derived. The augmented vector is then used to compute the
 * next expectations/cov. See for instance Anderson and Moore (optimal filtering
 * [1979]).
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class FixedPointSmoother {

    private final ISsf ssf;
    private final int fixpos;
    private final Matrix M;
    private StateStorage states;

    /**
     * Returns the expectations of the augmented part of the filter at position
     * pos, computed after the fixed point position. The size of the augmented
     * part is either the size of the underlying state space model or the number
     * of rows of the M matrix that defines the linear transformation of the
     * state vector considered in the smoothing algorithm.
     *
     * @param pos The position of the requested information (corresponds to
     * fixdpos+pos)
     * @return The expectations vector. Should not be modified
     */
    public StateStorage getResults() {
        return states;
    }

    /**
     * Defines the smoother. The complete state array will be considered in the
     * smoothing
     *
     * @param ssf The original state space form
     * @param fixpos The position of the fixed point
     */
    public FixedPointSmoother(final ISsf ssf, final int fixpos) {
        this.ssf = ssf;
        this.fixpos = fixpos;
        M = null;
    }

    /**
     * Defines the smoother. The state array transformed by M will be considered
     * in the smoothing
     *
     * @param ssf The original state space form
     * @param fixpos The position of the fixed point
     * @param M The transformation matrix. May be null; in that case, M is
     * considered to be I.
     */
    public FixedPointSmoother(final ISsf ssf, final int fixpos, final Matrix M) {

        if (M != null && ssf.getStateDim() != M.getColumnsCount()) {
            throw new SsfException("Invalid fixed point argument");
        }
        this.ssf = ssf;
        this.fixpos = fixpos;
        this.M = M;
    }

    public Matrix getTransformationMatrix() {
        return M;
    }

    public int getFixedPointPosition() {
        return fixpos;
    }

    public boolean process(ISsfData data) {
        // step 1: filtering till fixpos
        OrdinaryFilter filter = new OrdinaryFilter(new Initializer(ssf, fixpos, M));
        SsfDataWindow xdata = new SsfDataWindow(data, fixpos, data.getLength());
        int mdim = M == null ? ssf.getStateDim() : M.getRowsCount();
        Ssf xssf = new Ssf(new Dynamics(ssf.getDynamics(), mdim), new Measurement(ssf.getMeasurement(), mdim));
        states = StateStorage.full(StateInfo.Concurrent);
        states.prepare(mdim, fixpos, data.getLength());
        Results frslts = new Results(states, ssf.getStateDim(), mdim);
        return filter.process(xssf, xdata, frslts);
    }

    static class Initializer implements OrdinaryFilter.Initializer {

        private final int fixpos;
        private final Matrix M;
        private final ISsf core;

        Initializer(final ISsf core, final int fixpos, final Matrix M) {
            this.fixpos = fixpos;
            this.M = M;
            this.core = core;
        }

        @Override
        public int initialize(State state, ISsf ssf, ISsfData data) {
            DiffuseSquareRootInitializer init = new DiffuseSquareRootInitializer(null);
            OrdinaryFilter filter = new OrdinaryFilter(init);
            SsfDataWindow data0 = new SsfDataWindow(data, 0, fixpos);
            filter.process(core, data, null);
            if (init.getEndDiffusePos() > fixpos) {
                return -1;
            }
            int r = core.getStateDim();
            DataBlock a = filter.getFinalState().a();
            SubMatrix P = filter.getFinalState().P().all();
            state.a().range(0, r).copy(a);
            SubMatrix cur = state.P().topLeft(r, r);
            cur.copy(P);
            if (M == null) {
                state.a().range(r, 2 * r).copy(a);
                cur.vnext(r);
                cur.copy(P);
                cur.hnext(r);
                cur.copy(P);
                cur.vprevious(r);
                cur.copy(P);
            } else {
                int m = M.getRowsCount();
                state.a().range(r, r + m).product(M.rows(), a);
                cur.vnext(r);
                cur.product(M.all(), P);
                cur.hnext(r);
                SymmetricMatrix.quadraticFormT(P, M.all(), cur);
                cur.vprevious(r);
                cur.product(M.all(), P);
            }
            return fixpos;
        }
    }

    static class Measurement implements ISsfMeasurement {

        private final ISsfMeasurement core;
        private final int cdim, mdim;

        Measurement(ISsfMeasurement core, int mdim) {
            this.core = core;
            this.mdim = mdim;
            this.cdim = core.getStateDim();
        }

        @Override
        public void Z(int pos, DataBlock z) {
            core.Z(pos, z.range(0, cdim));
        }

        @Override
        public boolean hasErrors() {
            return core.hasErrors();
        }

        @Override
        public boolean hasError(int pos) {
            return core.hasError(pos);
        }

        @Override
        public double errorVariance(int pos) {
            return core.errorVariance(pos);
        }

        @Override
        public double ZX(int pos, DataBlock m) {
            return core.ZX(pos, m.range(0, cdim));
        }

        @Override
        public double ZVZ(int pos, SubMatrix V) {
            return core.ZVZ(pos, V.topLeft(cdim, cdim));
        }

        @Override
        public int getStateDim() {
            return cdim + mdim;
        }

        @Override
        public boolean isTimeInvariant() {
            return core.isTimeInvariant();
        }

        @Override
        public boolean isValid() {
            return core.isValid();
        }

        @Override
        public void VpZdZ(int pos, SubMatrix V, double d) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void XpZd(int pos, DataBlock x, double d) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class Dynamics implements ISsfDynamics {

        private final ISsfDynamics core;
        private final int cdim, mdim;

        Dynamics(ISsfDynamics core, int mdim) {
            this.core = core;
            this.mdim = mdim;
            this.cdim = core.getStateDim();
        }

        @Override
        public int getInnovationsDim() {
            return core.getInnovationsDim();
        }

        @Override
        public void V(int pos, SubMatrix qm) {
            core.V(pos, qm.topLeft(cdim, cdim));
        }

        @Override
        public void S(int pos, SubMatrix cm) {
            core.S(pos, cm.top(cdim));
        }

        @Override
        public boolean hasInnovations(int pos) {
            return core.hasInnovations(pos);
        }

        @Override
        public void T(int pos, SubMatrix tr) {
            core.T(pos, tr.topLeft(cdim, cdim));
            tr.bottomRight(mdim, mdim).diagonal().set(1);
        }

        @Override
        public boolean isDiffuse() {
            return false;
        }

        @Override
        public int getNonStationaryDim() {
            return 0;
        }

        @Override
        public void diffuseConstraints(SubMatrix b) {
        }

        @Override
        public boolean a0(DataBlock a0) {
            return false;
        }

        @Override
        public boolean Pf0(SubMatrix pf0) {
            return false;
        }

        @Override
        public void TX(int pos, DataBlock x) {
            core.TX(pos, x.range(0, cdim));
        }

        @Override
        public void addSU(int pos, DataBlock x, DataBlock u) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addV(int pos, SubMatrix p) {
            core.addV(pos, p.topLeft(cdim, cdim));
        }

        @Override
        public void XT(int pos, DataBlock x) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void XS(int pos, DataBlock x, DataBlock xs) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getStateDim() {
            return cdim + mdim;
        }

        @Override
        public boolean isTimeInvariant() {
            return core.isTimeInvariant();
        }

        @Override
        public boolean isValid() {
            return core.isValid();
        }
    }

    static class Results implements IFilteringResults {

        private StateStorage states;
        private final int start, n;

        Results(StateStorage states, final int start, final int n) {
            this.states = states;
            this.start = start;
            this.n = n;
        }

        @Override
        public void save(int t, UpdateInformation pe) {
        }

        @Override
        public void clear() {
            states.clear();
        }

        @Override
        public void save(int pos, State state, StateInfo info) {
            if (info == StateInfo.Forecast) {
                states.save(pos, state.a().extract(start, n), state.P().subMatrix(start, start + n, start, start + n));
            }
        }

    }
}
