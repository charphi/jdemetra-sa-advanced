/*
* Copyright 2013 National Bank of Belgium
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


package be.nbb.demetra.mairline;

import ec.tstoolkit.design.Development;
import ec.tstoolkit.sarima.SarimaModel;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Exploratory)
public class MaSpecification implements Cloneable {

    public static enum EstimationMethod{
        Iterative,
        ErrorVariance,
        LikelihoodGradient
    }

    public SarimaModel airline;
    // search method
    public int[] noisyPeriods; // pre-specified periods
    public boolean allPeriods;
    public double step=.1;
    
    public EstimationMethod method=EstimationMethod.LikelihoodGradient;

    @Override
    public MaSpecification clone(){
        try {
            MaSpecification spec = (MaSpecification) super.clone();
            if (airline != null) {
                spec.airline = airline.clone();
            }
            if (noisyPeriods != null) {
                spec.noisyPeriods = noisyPeriods.clone();
            }
            return spec;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof MaSpecification && equals((MaSpecification) obj));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Arrays.hashCode(this.noisyPeriods);
        hash = 67 * hash + (this.allPeriods ? 1 : 0);
        hash = 67 * hash + Objects.hashCode(this.method);
        return hash;
    }

    private boolean equals(MaSpecification other) {
        return allPeriods == other.allPeriods && method == other.method && step == other.step
                && Arrays.equals(noisyPeriods, other.noisyPeriods);
    }

}
