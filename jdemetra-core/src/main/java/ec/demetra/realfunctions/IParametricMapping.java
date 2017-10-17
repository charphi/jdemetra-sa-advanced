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


package ec.demetra.realfunctions;

import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.design.Development;

/**
 * 
 * @param <T>
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public interface IParametricMapping<T> extends IParametersDomain {
    /**
     * set the values in p, using information provided by the object t
     * 
     * @param p
     * @return A new object, which has the same structural form as t, but with
     *         the parameters provided by p.
     */
    T map(IReadDataBlock p);

}
