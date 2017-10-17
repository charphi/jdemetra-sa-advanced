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
import java.util.Random;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Jean Palate
 */
public class StlTest {

    public StlTest() {
    }

    @Test
    public void testDefault() {
        StlSpecification spec = StlSpecification.defaultSpec(12, 7, false);
        Stl stl = new Stl(spec);
        spec.setNo(5);
        spec.setMultiplicative(true);
        stl.process(Data.X);
//        System.out.println(new DataBlock(stl.trend));
//        System.out.println(new DataBlock(stl.season));
//        System.out.println(new DataBlock(stl.irr));
    }

    @Test
    public void testMissing() {
        StlSpecification spec = StlSpecification.defaultSpec(12, 7, false);
        Stl stl = new Stl(spec);
        spec.setNo(5);
        spec.setMultiplicative(true);
        TsData s = Data.X.clone();
        Random rnd=new Random();
        for (int i=0; i<10; ++i){
            s.set(rnd.nextInt(s.getLength()), Double.NaN);
        }
        stl.process(s);
//        System.out.println(new DataBlock(stl.trend));
//        System.out.println(new DataBlock(stl.season));
//        System.out.println(new DataBlock(stl.irr));
    }

    @Test
//    @Ignore
    public void testLargeFilter() {

        StlSpecification spec = StlSpecification.defaultSpec(12, 21, false);
        Stl stl = new Stl(spec);
        stl.process(Data.X);
//        System.out.println(new DataBlock(stl.trend));
//        System.out.println(new DataBlock(stl.season));
//        System.out.println(new DataBlock(stl.irr));
    }

    @Test
    public void testMul() {
        StlSpecification spec = StlSpecification.defaultSpec(12, 7, false);
        spec.setMultiplicative(true);
        Stl stl = new Stl(spec);
        spec.setNo(5);
        stl.process(Data.X);
//        System.out.println(new DataBlock(stl.trend));
//        System.out.println(new DataBlock(stl.season));
//        System.out.println(new DataBlock(stl.irr));
    }

    @Test
    @Ignore
    public void stressTest() {
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            StlSpecification spec = StlSpecification.defaultSpec(12, 7, false);
            spec.setNo(5);
            Stl stl = new Stl(spec);
            stl.process(Data.X);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
    }

    @Test
//    @Ignore
    public void testInner() {
        StlSpecification spec = StlSpecification.defaultSpec(12, 9, true);
        Stl stl = new Stl(spec);
        stl.process(Data.X);
//        System.out.println(new DataBlock(stl.trend));
//        System.out.println(new DataBlock(stl.season));
//        System.out.println(new DataBlock(stl.irr));
    }

}
