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
package ec.demetra.timeseries;

import ec.tstoolkit.design.Development;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Represents a time domain, which is a collection of contiguous periods.
 * Implementations of a time domain should be immutable objects.
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public interface IDomain  {


    /**
     * Gets the idx-th period
     * @param idx 0-based position of the period in the domain
     * @return
     */
    LocalDateTime getTime(int idx);

    /**
     * Gets the number of periods in the domain.
     * @return The length of the domain. 0 if the domain is empty.
     */
    int getLength();

    /**
     * @param time
     * @return -1 if not found. 
     */
    int search(LocalDateTime time);

    Period getPeriod();
}
