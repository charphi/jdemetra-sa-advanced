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
package ec.demetra.ssf.ckms;

import ec.demetra.ssf.dk.DiffusePredictionErrorDecomposition;
import ec.demetra.ssf.dk.DkLikelihood;
import ec.demetra.ssf.dk.DkConcentratedLikelihood;
import ec.demetra.ssf.dk.sqrt.DiffuseSquareRootInitializer;
import ec.demetra.ssf.univariate.IConcentratedLikelihoodComputer;
import ec.demetra.ssf.univariate.ILikelihoodComputer;
import ec.demetra.ssf.univariate.ISsf;
import ec.demetra.ssf.univariate.ISsfData;
import ec.demetra.ssf.univariate.SsfRegressionModel;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public class CkmsToolkit {

    public static ILikelihoodComputer<DkLikelihood> likelihoodComputer() {
        return (ISsf ssf, ISsfData data) -> {
            DiffusePredictionErrorDecomposition decomp = new DiffusePredictionErrorDecomposition(false);
            CkmsDiffuseInitializer ff = new CkmsDiffuseInitializer(new DiffuseSquareRootInitializer(decomp));
            CkmsFilter ffilter = new CkmsFilter(ff);
            ffilter.process(ssf, data, decomp);
            return decomp.likelihood();
        };
    }
}
