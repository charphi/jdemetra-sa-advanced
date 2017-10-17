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
package ec.demetra.ssf.implementations;

import ec.demetra.ssf.multivariate.IMultivariateSsf;
import ec.demetra.ssf.univariate.ISsf;
import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.multivariate.ISsfMeasurements;
import ec.demetra.ssf.multivariate.MultivariateSsf;

/**
 *
 * @author Jean Palate
 */
public class MultivariateTimeInvariantSsf extends MultivariateSsf{
    public static IMultivariateSsf of(IMultivariateSsf ssf){
        TimeInvariantDynamics td=TimeInvariantDynamics.of(ssf.getDynamics());
        if (td == null)
            return null;
        TimeInvariantMeasurements tm=TimeInvariantMeasurements.of(ssf.getStateDim(), ssf.getMeasurements());
        return new MultivariateTimeInvariantSsf(td, tm);
    }
    
    public static IMultivariateSsf of(ISsf ssf){
        TimeInvariantDynamics td=TimeInvariantDynamics.of(ssf.getDynamics());
        if (td == null)
            return null;
        TimeInvariantMeasurements tm=TimeInvariantMeasurements.of(ssf.getStateDim(), ssf.getMeasurement());
        return new MultivariateTimeInvariantSsf(td, tm);
    }

    private MultivariateTimeInvariantSsf(final ISsfDynamics dynamics, ISsfMeasurements measurement) {
        super(dynamics, measurement);
    }
}
