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

import ec.demetra.ssf.State;
import ec.demetra.ssf.StateInfo;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class PartialSmoothingResults implements ISmoothingResults {

    private final int npos;
    private ISmoothingResults core;

    public PartialSmoothingResults(int npos, final ISmoothingResults core) {
        this.npos = npos;
        this.core = core;
    }

    @Override
    public void save(int pos, State state, StateInfo info) {
        if (pos % npos == 0) {
            core.save(pos / npos, state, info);
        }
    }

    @Override
    public DataBlock a(int pos) {
        return core.a(pos);
    }

    @Override
    public SubMatrix P(int pos) {
        return core.P(pos);
    }

    @Override
    public void rescaleVariances(double factor) {
        core.rescaleVariances(factor);
    }

    @Override
    public IReadDataBlock getComponent(int item) {
        return core.getComponent(item);
    }

    @Override
    public IReadDataBlock getComponentVariance(int item) {
        return core.getComponentVariance(item);
    }

    @Override
    public boolean hasVariances() {
        return core.hasVariances();
    }
}
