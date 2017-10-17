/*
 * Copyright 2013-2014 National Bank of Belgium
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
package ec.demetra.ssf.implementations.var;

import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;

/**
 *
 * @author Jean Palate
 */
public class VarDescriptor {

    public static final double AR_DEF = .6;

    /**
     * Number of lags
     */
    private final int nlags, nvars;
    /**
     * Parameters of the VAR equations The row i contains the coefficients
     * c(i,k) of fi(t): fi(t)= c(i,0)f0(t-1)+...+c(i,n)fn(t-1)+...
     * +c(i,(n-1)*l)f0(t-l)...+c(i,n*l-1)fn(t-l))
     */
    private final Matrix varMatrix;
    /**
     * Covariance matrix of the innovations
     */
    private final Matrix covar;
    /**
     * Creates a new descriptor of the transition equation (VAR).
     *
     * @param nvars Number of variables (equations) (
     * @param nlags Number of lags in the VAR model
     */
    public VarDescriptor(int nvars, int nlags) {
        varMatrix = new Matrix(nvars, nvars * nlags);
        covar = new Matrix(nvars, nvars);
        this.nlags = nlags;
        this.nvars=nvars;
        setDefault();
    }

    /**
     * Initialize the matrices
     */
    public final void setDefault() {
        covar.set(0);
        covar.diagonal().set(1);
        varMatrix.set(0);
        varMatrix.diagonal().set(AR_DEF);
    }
    
    public int getLagsCount(){
        return nlags;
    }
    
    public int getVariablesCount(){
        return nvars;
    }
    
    public Matrix getVarMatrix(){
        return varMatrix;
    }
    
    public Matrix getInnovationsVariance(){
        return covar;
    }

    /**
     * Gets the matrix of the var parameters corresponding to a given lag
     *
     * @param lag The lag in the var equation. Should belong to [1, nlags]
     * @return The corresponding square sub-matrix is returned. That sub-matrix is a view of the underlying 
     * parameters
     */
    public SubMatrix getA(int lag) {
        int c0=(lag-1)*nvars, c1=c0+nvars;
        return varMatrix.subMatrix(0, nvars, c0, c1);
    }

}

