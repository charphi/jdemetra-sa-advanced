/*
 * Copyright 2016 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or â€“ as soon they will be approved 
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
package be.nbb.demetra.highfreq;

import be.nbb.demetra.modelling.arima.outliers.OutliersDetectionModule;
import be.nbb.demetra.modelling.outliers.AdditiveOutlier;
import be.nbb.demetra.modelling.outliers.IOutlierVariable;
import be.nbb.demetra.modelling.outliers.SwitchOutlier;
import ec.demetra.ssf.dk.DkToolkit;
import ec.demetra.ssf.implementations.arima.SsfUcarima;
import ec.demetra.ssf.univariate.DefaultSmoothingResults;
import ec.demetra.ssf.univariate.SsfData;
import ec.demetra.timeseries.calendars.IHoliday;
import ec.demetra.ucarima.TrendCycleDecomposer;
import ec.tstoolkit.arima.ArimaModel;
import ec.tstoolkit.arima.estimation.GlsArimaMonitor;
import ec.tstoolkit.arima.estimation.RegArimaEstimation;
import ec.tstoolkit.arima.estimation.RegArimaModel;
import ec.tstoolkit.data.AutoRegressiveSpectrum;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DataBlockStorage;
import ec.tstoolkit.data.ReadDataBlock;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.tstoolkit.maths.realfunctions.IParametricMapping;
import ec.tstoolkit.modelling.arima.x13.UscbForecasts;
import ec.tstoolkit.ucarima.ModelDecomposer;
import ec.tstoolkit.ucarima.SeasonalSelector;
import ec.tstoolkit.ucarima.TrendCycleSelector;
import ec.tstoolkit.ucarima.UcarimaModel;
import ec.tstoolkit.utilities.Arrays2;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jean Palate
 */
public class App {

    private static final double[] periods = {52, 365.25 / 7, 365, 365.25};

    private static Matrix data, regressors;
    private static int iperiod = 1;
    private static double tlength;
    private static String output;
    private static ArimaModel arima;
    private static UcarimaModel ucm;
    private static UcarimaModel tc_ucm;
    private static boolean silent = false, verbose = false;
    private static boolean ami = false, log;
    private static LocalDate start;
    private static Holidays holidays;
    private static boolean hol1;
    private static int hol_nb, hol_nf;
    private static int nb, nf;
    private static IOutlierVariable[] outliers;
    private static IParametricMapping<ArimaModel> mapping;
    private static RegArimaEstimation<ArimaModel> estimation;
    private static Matrix components;

    private static boolean isDaily() {
        return iperiod > 1;
    }

    private static boolean isWeekly() {
        return iperiod <= 1;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        if (!silent) {
            System.out.println("Reading data");
        }
        if (!decodeArgs(args)) {
            return;
        }
        if (!createMapping()) {
            return;
        }
        for (int col = 0; col < data.getColumnsCount(); ++col) {
            try {
                if (!silent) {
                    System.out.println("Series " + (col + 1));
                }
                if (!createModel()) {
                    return;
                }
                if (!silent) {
                    System.out.println("Estimating the model");
                }
                if (!estimateModel(col)) {
                    return;
                }
                if (ami) {
                    if (!silent) {
                        System.out.println("Estimating outliers");
                    }
                    estimateOutliers();
                }

                arima = estimation.model.getArima();
                if (verbose) {
                    System.out.println(arima);
                }
                if (!silent) {
                    System.out.println("Decomposing the model");
                }
                if (!decomposeModel()) {
                    return;
                }
                if (verbose) {
                    System.out.println(ucm);
                }
                if (!silent) {
                    System.out.println("Computing the components");
                }
                computeComponents(col);
                if (tlength > 0) {
                    if (!silent) {
                        System.out.println("Computing trend/cycle");
                    }
                    computeTC();
                }
                if (verbose) {
                    System.out.println(components);
                }
                if (!silent) {
                    System.out.println("Generating output");
                }
                generateOutput(col);

            } catch (Exception err) {
                System.out.println(err.getMessage());
            }
        }
    }

    private static boolean decodeArgs(String[] args) {
        //
        int cur = 0;
        while (cur < args.length) {
            String cmd = args[cur++];
            if (cmd.length() == 0) {
                return false;
            }
            cmd = cmd.toLowerCase();

            switch (cmd) {
                case "-y": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        data = MatrixSerializer.read(new File(str));
                    } catch (IOException ex) {
                        System.out.println("Invalid data");
                        return false;
                    }
                    break;
                }
                case "-x": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        regressors = MatrixSerializer.read(new File(str));
                    } catch (IOException ex) {
                        System.out.println("Invalid regressors");
                        return false;
                    }
                    break;
                }
                case "-hol": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    int ns = str.length() - 1;
                    if (str.charAt(ns) == '1') {
                        hol1 = true;
                        str = str.substring(0, ns);
                    } else {
                        hol1 = false;
                    }
                    switch (str) {
                        case "france":
                            holidays = Holidays.france();
                            break;
                        case "belgium":
                            holidays = Holidays.belgium();
                            break;
                    }
                    break;
                }
                case "-hol_nb": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        hol_nb = Integer.parseInt(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-hol_nf": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        hol_nf = Integer.parseInt(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-nb": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        nb = Integer.parseInt(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-nf": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        nf = Integer.parseInt(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-p": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    switch (str) {
                        case "w":
                            iperiod = 0;
                            break;
                        case "W":
                            iperiod = 1;
                            break;
                        case "d":
                            iperiod = 2;
                            break;
                        case "D":
                            iperiod = 3;
                            break;
                        default:
                            System.out.println("Invalid period. Should be w, W, d or D");
                    }
                    break;
                }
                case "-t": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        tlength = Double.parseDouble(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-start": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    try {
                        start = LocalDate.parse(str);
                    } catch (Exception e) {
                        return false;
                    }
                    break;
                }
                case "-o":
                case "-output": {
                    if (cur == args.length) {
                        return false;
                    }
                    String str = args[cur++];
                    if (str.length() == 0 || str.charAt(0) == '-') {
                        return false;
                    }
                    output = str;
                    break;
                }
                case "-log": {
                    log = true;
                    break;
                }
                case "-s": {
                    silent = true;
                    break;
                }
                case "-v": {
                    verbose = true;
                    break;
                }
                case "-ami": {
                    ami = true;
                    break;
                }
                default:
                    System.out.println(cmd + " is not supported");
                    return false;
            }
        }
        return true;
    }
    
    private static DataBlock y(int col){
        if (! log)
            return data.column(col);
        else{
            DataBlock y = data.column(col).deepClone();
            y.apply(x->Math.log(x));
            return y;
        }
    }

    private static boolean createModel() {
        int np = isWeekly() ? 2 : 3;
        double[] p0 = new double[np];
        for (int i = 0; i < np; ++i) {
            p0[i] = .9;
        }
        arima = mapping.map(new DataBlock(p0));
        return true;
    }

    private static boolean createMapping() {
        switch (iperiod) {
            case 0: {
                WeeklyMapping wmapping = new WeeklyMapping();
                wmapping.setAdjust(false);
                mapping = wmapping;
                return true;
            }
            case 1: {
                WeeklyMapping wmapping = new WeeklyMapping();
                wmapping.setAdjust(true);
                mapping = wmapping;
                return true;
            }
            case 2: {
                DailyMapping dmapping = new DailyMapping();
                dmapping.setAdjust(false);
                mapping = dmapping;
                return true;
            }
            case 3: {
                DailyMapping dmapping = new DailyMapping();
                dmapping.setAdjust(true);
                mapping = dmapping;
                return true;
            }
            default:
                return false;
        }
    }

    private static RegArimaModel<ArimaModel> generateRegArima(int col) {
        RegArimaModel<ArimaModel> regarima = new RegArimaModel<>(arima, y(col));
        if (regressors != null) {
            for (int i = 0; i < regressors.getColumnsCount(); ++i) {
                regarima.addX(regressors.column(i).drop(nb, nf));
            }
        }
        if (holidays != null && start != null) {
            int nhol = hol1 ? 1 : holidays.getHolidays().size();
            Matrix[] td = new Matrix[1 + hol_nb + hol_nf];
            int cur = 0;
            for (int i = hol_nb; i > 0; --i) {
                Matrix m = new Matrix(data.getRowsCount(), nhol);
                holidays.fillPreviousWorkingDays(m.all(), start, data.getRowsCount(), i);
                td[cur++] = m;
            }
            Matrix m0 = new Matrix(data.getRowsCount(), nhol);
            holidays.fillDays(m0.all(), start, data.getRowsCount());
            td[cur++] = m0;
            for (int i = 1; i <= hol_nf; ++i) {
                Matrix m = new Matrix(data.getRowsCount(), nhol);
                holidays.fillNextWorkingDays(m.all(), start, data.getRowsCount(), i);
                td[cur++] = m;
            }
            for (int i = 0; i < nhol; ++i) {
                for (int j = 0; j < td.length; ++j) {
                    regarima.addX(td[j].column(i));
                }
            }
        }
        return regarima;
    }

    private static void fillX(SubMatrix all, int start, LocalDate dstart) {
        int cur = 0;
        int len = all.getRowsCount();
        if (regressors != null) {
            for (int i = 0; i < regressors.getColumnsCount(); ++i) {
                all.column(cur++).copy(regressors.column(i).extract(start, len));
            }
        }
        if (holidays != null && dstart != null) {
            int nhol = hol1 ? 1 : holidays.getHolidays().size();
            Matrix[] td = new Matrix[1 + hol_nb + hol_nf];
            int dcur = 0;
            for (int i = hol_nb; i > 0; --i) {
                Matrix m = new Matrix(len, nhol);
                holidays.fillPreviousWorkingDays(m.all(), dstart, len, i);
                td[dcur++] = m;
            }
            Matrix m0 = new Matrix(len, nhol);
            holidays.fillDays(m0.all(), dstart, len);
            td[dcur++] = m0;
            for (int i = 1; i <= hol_nf; ++i) {
                Matrix m = new Matrix(len, nhol);
                holidays.fillNextWorkingDays(m.all(), dstart, len, i);
                td[dcur++] = m;
            }
            for (int i = 0; i < nhol; ++i) {
                for (int j = 0; j < td.length; ++j) {
                    all.column(cur++).copy(td[j].column(i));
                }
            }
        }
        for (IOutlierVariable out : outliers) {
            out.data(start - nb, all.column(cur++));
        }
    }

    private static boolean estimateModel(int col) {
        GlsArimaMonitor monitor = new GlsArimaMonitor();
        monitor.setMultiThread(true);
        monitor.setMapping(mapping);
        estimation = monitor.process(generateRegArima(col));
        return estimation != null;
    }

    private static boolean decomposeModel() {
        ModelDecomposer decomposer = new ModelDecomposer();
        TrendCycleSelector tsel = new TrendCycleSelector();
        if (isWeekly()) {
            AllSelector ssel = new AllSelector();
            decomposer.add(tsel);
            decomposer.add(ssel);
        } else {
            SeasonalSelector ssel = new SeasonalSelector(7);
            decomposer.add(tsel);
            decomposer.add(ssel);
            decomposer.add(new AllSelector());
        }
        ucm = decomposer.decompose(arima);
        ucm.setVarianceMax(-1);
        ucm.compact(ucm.getComponentsCount() - 2, 2);
        return ucm.isValid();
    }

    private static void computeComponents(int col) {
        DataBlock y = y(col);
        DataBlock mlin=y;
        if (estimation.likelihood.getB() != null) {
            mlin = estimation.model.calcRes(new ReadDataBlock(estimation.likelihood.getB()));
        }
        SsfUcarima ssf = SsfUcarima.create(ucm);
        DataBlockStorage sr = DkToolkit.fastSmooth(ssf, new SsfData(mlin));//, (pos, a, e)->a.add(0,e));
        int ncmps = (tlength > 0) ? 4 : 3;
        components = new Matrix(mlin.getLength(), ncmps + ucm.getComponentsCount());
        components.column(0).copy(y);
        components.column(1).copy(mlin);
        for (int i = 1; i < ucm.getComponentsCount(); ++i) {
            components.column(ncmps + i).copy(sr.item(ssf.getComponentPosition(i)));
        }
        // computes sa, t
        components.column(2).copy(mlin);
        // sa=y-s
        components.column(2).sub(components.column(ncmps + 1));
        if (isDaily()) {
            components.column(2).sub(components.column(ncmps + 2));
        }
        components.column(3).copy(components.column(2));
        components.column(3).sub(components.column(components.getColumnsCount() - 1));

    }

    private static void computeTC() {
        TrendCycleDecomposer tcdecomposer = new TrendCycleDecomposer();
        tcdecomposer.setTau(tlength);
        if (isWeekly()) {
            tcdecomposer.setDifferencing(2);
        } else {
            tcdecomposer.setDifferencing(3);
        }
        tcdecomposer.decompose(ucm.getComponent(0));
        tc_ucm = new UcarimaModel(ucm.getComponent(0), new ArimaModel[]{tcdecomposer.getTrend(), tcdecomposer.getCycle()});
        SsfUcarima ssf = SsfUcarima.create(tc_ucm);
        DefaultSmoothingResults sr = DkToolkit.smooth(ssf, new SsfData(components.column(3)), false);
        components.column(3).copy(sr.getComponent(ssf.getComponentPosition(0)));
        components.column(4).copy(sr.getComponent(ssf.getComponentPosition(1)));
    }

    private static File generateFile(String name, int col) {
        File path = new File(output == null ? "." : output);
        if (!path.exists()) {
            path.mkdirs();
        }
        return new File(path, name + ("-") + (col + 1) + ".txt");
    }

    private static void generateRegression(File file) throws IOException {
        int nx = estimation.model.getXCount();
        String[] items = new String[nx];
        int cur = 0;
        if (regressors != null) {
            for (int i = 1; i <= regressors.getColumnsCount(); ++i) {
                items[cur++] = "reg-" + i;
            }
        }
        if (holidays != null && start != null) {
            if (hol1) {
                for (int i = hol_nb; i > 0; --i) {
                    items[cur++] = "td(-" + i + ")";
                }
                items[cur++] = "td";
                for (int i = 1; i <= hol_nf; ++i) {
                    items[cur++] = "td(+" + i + ")";
                }
            } else {
                List<IHoliday> lh = holidays.getHolidays();
                for (IHoliday h : lh) {
                    String td = h.toString();
                    for (int i = hol_nb; i > 0; --i) {
                        items[cur++] = td + "(-" + i + ")";
                    }
                    items[cur++] = td;
                    for (int i = 1; i <= hol_nf; ++i) {
                        items[cur++] = td + "(+" + i + ")";
                    }
                }
            }
        }
        if (outliers != null) {
            for (int i = 0; i < outliers.length; ++i) {
                String o = outliers[i].getCode();
                if (start != null) {
                    o += start.plusDays(outliers[i].getPosition());
                } else {
                    o += outliers[i].getPosition();
                }
                items[cur++] = o;
            }
        }
        OutputFormatter.write(file, items, new DataBlock(estimation.likelihood.getB()), new DataBlock(estimation.likelihood.getTStats()));
    }

    private static void generateARSpectrum(int col) throws IOException {
        // AR spectrum of the linearized, of the SA, of C, of I
        int nar = (int) (isWeekly() ? 52 * 2.5 : 365 * 2.5);
        int n = 3;
        DataBlock ylin = components.column(1).deepClone();
        DataBlock sa = components.column(2).deepClone();
        DataBlock irr = components.column(components.getColumnsCount() - 1);
        sa.difference();
        sa = sa.drop(1, 0);
        ylin.difference();
        ylin = ylin.drop(1, 0);
        irr = irr.drop(1, 0);
        DataBlock c = null;
        if (tlength != 0) {
            ++n;
            c = components.column(4).deepClone();
            c.difference();
            c = c.drop(1, 0);
        }

        AutoRegressiveSpectrum arylin = new AutoRegressiveSpectrum(AutoRegressiveSpectrum.Method.Durbin);
        AutoRegressiveSpectrum arsa = new AutoRegressiveSpectrum(AutoRegressiveSpectrum.Method.Durbin);
        AutoRegressiveSpectrum arc = new AutoRegressiveSpectrum(AutoRegressiveSpectrum.Method.Durbin);
        AutoRegressiveSpectrum ari = new AutoRegressiveSpectrum(AutoRegressiveSpectrum.Method.Durbin);
        arylin.process(ylin, nar);
        arsa.process(sa, nar);
        if (c != null) {
            arc.process(c, nar);
        }
        ari.process(irr, nar);
        int nf = 4 * (isWeekly() ? 52 : 365);
        double rd = Math.PI / (1 + nf);
        Matrix rslt = new Matrix(nf, n + 1);
        double cur = rd;
        for (int i = 1; i <= nf; ++i) {
            int j = 0;
            rslt.set(i - 1, j++, cur);
            rslt.set(i - 1, j++, arylin.value(cur));
            rslt.set(i - 1, j++, arsa.value(cur));
            if (c != null) {
                rslt.set(i - 1, j++, arc.value(cur));
            }
            rslt.set(i - 1, j, ari.value(cur));
            cur += rd;
        }
        File cmp = generateFile("arspectrum", col);
        MatrixSerializer.write(rslt, cmp);
    }

    private static void generateOutput(int col) {
        try {
            File est = generateFile("estimation", col);
            OutputFormatter.writeEstimation(est, estimation, mapping, log);
            // components
            File cmp = generateFile("components", col);
            MatrixSerializer.write(components, cmp);
            // regression 
            if (estimation.model.getXCount() > 0) {
                File reg = generateFile("regression", col);
                generateRegression(reg);
            }
//            File farima = generateFile("arima", col);
//            OutputFormatter.writeArima(farima, arima);
//            File fucm = generateFile("ucm", col);
//            OutputFormatter.writeUcm(fucm, ucm);
            File fducm = generateFile("ucm", col);
            OutputFormatter.writeUcmPolynomials(fducm, ucm);
            if (tc_ucm != null) {
                File ftcucm = generateFile("ucm-tc", col);
                OutputFormatter.writeUcmPolynomials(ftcucm, tc_ucm);
            }
            generateARSpectrum(col);
            generateLjungBox(col);
            if (nb > 0 || nf > 0) {
                generatefcasts(col);
            }

        } catch (IOException ex) {
        }
    }

    private static void estimateOutliers() {
        OutliersDetectionModule outliersDetector = new OutliersDetectionModule();
        if (!silent) {
            Consumer<IOutlierVariable> hook = o -> System.out.println("add outlier:" + o.getCode()
                    + ((start == null) ? (o.getPosition() + 1) : start.plusDays(o.getPosition())));
            outliersDetector.setAddHook(hook);
        }
        GlsArimaMonitor monitor = new GlsArimaMonitor();
        monitor.setMultiThread(true);
        monitor.setMapping(mapping);
        outliersDetector.setMonitor(monitor);
        outliersDetector.addOutlierFactory(new SwitchOutlier.Factory());
        outliersDetector.addOutlierFactory(new AdditiveOutlier.Factory());

        outliersDetector.process(estimation.model);
        RegArimaModel<ArimaModel> regarima = outliersDetector.getRegarima();
        estimation = monitor.optimize(regarima);
        arima = regarima.getArima();
        outliers = outliersDetector.getOutliers();
    }

    private static void generateLjungBox(int col) throws IOException {
        double period = isWeekly() ? 365.25 / 7 : 365.25;
        DataBlock ylin = components.column(1).deepClone();
        DataBlock sa = components.column(2).deepClone();
        DataBlock irr = components.column(components.getColumnsCount() - 1);
        int nlag=isWeekly() ? 1 : 7;
        sa.difference(1.0, nlag);
        sa = sa.drop(nlag, 0);
        ylin.difference(1.0, nlag);
        ylin = ylin.drop(nlag, 0);
        irr = irr.drop(nlag, 0);
        DataBlock c = null;
        if (tlength != 0) {
            c = components.column(4).deepClone();
            c.difference(1.0, nlag);
            c = c.drop(nlag, 0);
        }

        File lbs = generateFile("lbs", col);
        // compute seasonal Ljung-Box
        int[] lags = new int[4];
        for (int i = 0; i < lags.length; ++i) {
            lags[i] = (int) (period * (i + 1));
            
        }
        LjungBoxTest2 stest = new LjungBoxTest2();
        stest.setLags(lags);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(lbs))) {
            stest.test(ylin);
            OutputFormatter.writeLb(writer, "ylin", stest);
            stest.test(sa);
            OutputFormatter.writeLb(writer, "sa", stest);
            stest.test(irr);
            OutputFormatter.writeLb(writer, "irr", stest);
            if (c != null) {
                stest.test(c);
                OutputFormatter.writeLb(writer, "cycle", stest);
            }
        }
    }

    private static void generatefcasts(int col) throws IOException {
        UscbForecasts fcast = new UscbForecasts(estimation.model.getArima());
        if (nb > 0) {
            Matrix m = new Matrix(nb, 3);
            double[] bcasts = fcast.forecasts(components.column(1).reverse(), nb);
            Arrays2.reverse(bcasts);
            m.column(0).copyFrom(bcasts, 0);
            int nx = estimation.likelihood.getNx();
            if (nx > 0) {
                // gets the regression variables
                Matrix x = new Matrix(nb, nx);
                fillX(x.all(), 0, start.minusDays(nb));
                m.column(1).product(x.rows(), new DataBlock(estimation.likelihood.getB()));
                m.column(2).sum(m.column(0), m.column(1));
            }
            File b = generateFile("bcasts", col);
            MatrixSerializer.write(m, b);
        }
        if (nf > 0) {
            Matrix m = new Matrix(nf, 3);
            double[] forecasts = fcast.forecasts(components.column(1), nf);
            m.column(0).copyFrom(forecasts, 0);
            int nx = estimation.likelihood.getNx();
            if (nx > 0) {
                // gets the regression variables
                Matrix x = new Matrix(nf, nx);
                fillX(x.all(), nf + data.getRowsCount(), start.plusDays(data.getRowsCount()));
                m.column(1).product(x.rows(), new DataBlock(estimation.likelihood.getB()));
                m.column(2).sum(m.column(0), m.column(1));
            }
            File f = generateFile("fcasts", col);
            MatrixSerializer.write(m, f);
        }

    }

}
