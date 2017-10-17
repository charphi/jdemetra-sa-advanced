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
package be.nbb.demetra.stl;

import data.Data;
import ec.demetra.timeseries.simplets.TsData;
import ec.tstoolkit.data.DataBlock;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jean Palate
 */
public class SeasonalLoessFilterTest {
    
    public SeasonalLoessFilterTest() {
    }

    @Test
    public void testSomeMethod() {
        TsData s=Data.X;
        double[] d = s.data();
        LoessSpecification spec = LoessSpecification.of(7, 0);
        SeasonalLoessFilter filter=new SeasonalLoessFilter(spec, 12);
        double[] sd=new double[d.length+24];
        filter.filter(IDataGetter.of(d), null, IDataSelector.of(sd, -12));
//        System.out.println(new DataBlock(d));
//        System.out.println(new DataBlock(sd));
    }
    
}
