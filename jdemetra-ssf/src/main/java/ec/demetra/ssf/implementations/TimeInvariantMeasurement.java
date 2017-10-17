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

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockIterator;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.demetra.ssf.univariate.ISsfMeasurement;
import java.text.DecimalFormat;

/**
 *
 * @author Jean Palate
 */
public class TimeInvariantMeasurement implements ISsfMeasurement {

    private final DataBlock Z;
    private final double var;

    public static TimeInvariantMeasurement of(int dim, ISsfMeasurement measurement) {
        if (!measurement.isTimeInvariant()) {
            return null;
        }
        DataBlock Z = new DataBlock(dim);
        measurement.Z(0, Z);
        if (!measurement.hasErrors()) {
            return new TimeInvariantMeasurement(Z, 0);
        } else {
            return new TimeInvariantMeasurement(Z, measurement.errorVariance(0));
        }

    }

    public TimeInvariantMeasurement(DataBlock Z, double var) {
        this.Z = Z;
        this.var = var;
    }

    @Override
    public boolean isTimeInvariant() {
        return true;
    }

    @Override
    public void Z(int pos, DataBlock z) {
        z.copy(Z);
    }

    @Override
    public boolean hasErrors() {
        return var != 0;
    }

    @Override
    public boolean hasError(int pos) {
        return var != 0;
    }

    @Override
    public double errorVariance(int pos) {
        return var;
    }

    @Override
    public double ZX(int pos, DataBlock m) {
        return Z.dot(m);
    }

    @Override
    public double ZVZ(int pos, SubMatrix V) {
        DataBlock zv = new DataBlock(V.getColumnsCount());
        zv.product(Z, V.columns());
        return zv.dot(Z);
    }

    @Override
    public void VpZdZ(int pos, SubMatrix V, double d) {

        DataBlockIterator cols = V.columns();
        DataBlock col = cols.getData();
        DataBlock zi = Z, zj = Z;
        int i = 0;
        do {
            col.addAY(d * zj.get(i++), zi);
        } while (cols.next());
    }

    @Override
    public void XpZd(int pos, DataBlock x, double d) {
        x.addAY(d, Z);
    }

    @Override
    public int getStateDim() {
        return Z.getLength();
    }

    @Override
    public boolean isValid() {
        return Z != null;
    }
    
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append("Z:\r\n").append(Z.toString(FMT)).append("\r\n");
        builder.append("H:\r\n").append(new DecimalFormat(FMT).format(var)).append("\r\n\r\n");
        return builder.toString();
    }
    private static final String FMT="0.#####";

}
