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

import ec.demetra.eco.ILikelihood;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.maths.matrices.Matrix;

/**
 *
 * @author Jean Palate
 */
public class ProfileLikelihood implements ILikelihood {

    /**
     * Respectively: diffuse log-likelihood sum of the squared residuals log
     * determinant of the cov matrix diffuse correction
     */
    private double ll, ssqerr, ldet;
    private int n;
    private DataBlock b;
    private Matrix varB;

    /**
     *
     */
    public ProfileLikelihood() {
    }

    /**
     *
     */
    public void clear() {
        ll = 0;
        ssqerr = 0;
        ldet = 0;
        n = 0;
        b = null;
        varB = null;
    }

    @Override
    public double getFactor() {
        return Math.exp((ldet) / n);
    }

    @Override
    public double getLogLikelihood() {
        return ll;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public IReadDataBlock getResiduals() {
        return DataBlock.EMPTY;
    }

    @Override
    public double getLogDeterminant() {
        return ldet;
    }

    /**
     *
     * @return
     */
    public double getSer() {
        return Math.sqrt(ssqerr / n);
    }

    @Override
    public double getSigma() {
        return ssqerr / n;
    }

    @Override
    public double getSsqErr() {
        return ssqerr;
    }

    /**
     * Adjust the likelihood if the data have been pre-multiplied by a given
     * scaling factor
     *
     * @param factor The scaling factor
     */
    public void rescale(final double factor) {
        if (factor == 1) {
            return;
        }
        ssqerr /= factor * factor;
        ll += n * Math.log(factor);
    }

    /**
     * Initialize the profile likelihood. We consider below the GLS problem
     * corresponding to a given state space: y = a * X + e, where X is derived
     * from the initial conditions and e ~ N(0, V)
     *
     * The profile likelihood is then:
     *
     * -0.5*(n*log(2*pi)+n*log(ssqerr/n)+n+log|V|)
     *
     *
     * @param ssqerr The sum of the squared residuals
     * @param ldet The log of the determinant of V
     * @param b
     * @param varB
     * @param n The number of observations
     * @return
     */
    public boolean set(final double ssqerr, final double ldet, final DataBlock b, final Matrix varB,
            final int n) {
        this.ssqerr = ssqerr;
        this.ldet = ldet;
        this.b = b;
        this.varB = varB;
        this.n = n;
        calcll();
        return true;
    }

    private void calcll() {
        ll = -.5
                * (n * Math.log(2 * Math.PI) + n
                * (1 + Math.log(ssqerr / n)) + ldet);

    }

    public void add(ILikelihood ll) {
        n += ll.getN();
        ssqerr += ll.getSsqErr();
        ldet += ll.getLogDeterminant();
        calcll();
    }

    public IReadDataBlock getDiffuseEffects() {
        return b;
    }

    public Matrix getVarianceOfDiffuseEffects() {
        return varB;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ll=").append(this.getLogLikelihood()).append("\r\n");
        builder.append("n=").append(this.getN()).append("\r\n");
        builder.append("ssq=").append(this.getSsqErr()).append("\r\n");
        builder.append("ldet=").append(this.getLogDeterminant()).append("\r\n");
        return builder.toString();
    }

}
