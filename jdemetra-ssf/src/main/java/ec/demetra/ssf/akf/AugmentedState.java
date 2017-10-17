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

import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.demetra.ssf.ISsfDynamics;
import ec.demetra.ssf.State;

/**
 * Represents x* = x + A d, 
 * where x is a usual state vector and A is a matrix of constraints on some
 * unknown parameters (d).
 * This is similar to the ENRV (extended normal random vector) of Snyder/Forbes
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class AugmentedState extends State{
    
    public static AugmentedState of(ISsfDynamics dyn){
        AugmentedState state=new AugmentedState(dyn.getStateDim(), dyn.getNonStationaryDim());
        if (! dyn.a0(state.a()))
            return null;
        if (!dyn.Pf0(state.P().all()))
            return null;
        if (dyn.isDiffuse())
            dyn.diffuseConstraints(state.B.all());
        return state;
    }
    
    /**
     * B contains the states of the constraints. Its interpretation depends on the considered step
     */
    private final Matrix B;
    private int ndropped=0;

    /**
     *
     *
     * @param dim
     * @param ndiffuse
     */
    public AugmentedState(final int dim, final int ndiffuse) {
        super(dim);
        B=new Matrix(dim, ndiffuse);
    }

    public final SubMatrix B(){
        return B.subMatrix(0, -1, ndropped, -1);
    }
    
    public void restoreB(SubMatrix b){
        int n=b.getColumnsCount(), m=B.getColumnsCount();
        ndropped=m-n;
        B().copy(b);
    }
    
    public final void dropDiffuseConstraint(){
        ++ndropped;
    }
   
    public final void dropAllConstraints(){
        ndropped=B.getColumnsCount();
    }
    
    public final boolean isDiffuse(){
        return ndropped < B.getColumnsCount();
    }
    
    public int getDiffuseDim(){
        return B.getColumnsCount()-ndropped;
    }
}
