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
package be.nbb.demetra.sts;

import ec.demetra.ssf.implementations.structural.SsfBsm;
import be.nbb.demetra.sts.BsmMapping.Transformation;
import ec.demetra.realfunctions.IFunction;
import ec.demetra.realfunctions.IFunctionMinimizer;
import ec.demetra.realfunctions.ProxyMinimizer;
import ec.demetra.realfunctions.TransformedFunction;
import ec.demetra.ssf.dk.DkConcentratedLikelihood;
import ec.demetra.ssf.dk.SsfFunction;
import ec.demetra.ssf.dk.SsfFunctionInstance;
import ec.demetra.ssf.implementations.structural.BasicStructuralModel;
import ec.demetra.ssf.implementations.structural.Component;
import ec.demetra.ssf.implementations.structural.ModelSpecification;
import ec.demetra.ssf.univariate.SsfData;
import ec.tstoolkit.data.AbsMeanNormalizer;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.ReadDataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.matrices.SubMatrix;
import ec.demetra.realfunctions.IFunctionPoint;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public class BsmMonitor {

    private SubMatrix m_x;

    private double[] m_y;

    // mapper definition
    private ModelSpecification m_spec = new ModelSpecification();

    private int m_freq = 1;

    private BsmMapping m_mapping;

    private BasicStructuralModel m_bsm;

    private double m_eps = 1e-9;

    private boolean m_bconverged = false, m_dregs;

    private IFunctionMinimizer m_min = null;// new
    // ec.tstoolkit.maths.functions.minpack.LMMinimizer();

    private double m_dsmall = 0.01;

    private DkConcentratedLikelihood m_ll;
    private SsfFunction<BasicStructuralModel, SsfBsm> fn_;
    private SsfFunctionInstance<BasicStructuralModel, SsfBsm> fnmax_;

    private double m_factor;

    /**
     *
     */
    public BsmMonitor() {
    }

    @SuppressWarnings("unchecked")
    private boolean _estimate() {
        m_bconverged = false;

        if (m_bsm == null) {
            m_bsm = initialize();
        }

        if (m_mapping.getDim() == 0) {
            return true;
        }
        fn_ = null;
        fnmax_ = null;

        IFunctionMinimizer fmin;
        if (m_min != null) {
            fmin = m_min.exemplar();
        } else {
            // ec.tstoolkit.maths.realfunctions.QRMarquardt qr=new
            // ec.tstoolkit.maths.realfunctions.QRMarquardt();
            // qr.setIncreaseStep(32);
            // fmin = new ProxyMinimizer(qr);
            fmin = new ProxyMinimizer(new ec.demetra.realfunctions.levmar.LevenbergMarquardtMinimzer());
            //fmin = new ec.tstoolkit.maths.realfunctions.riso.LbfgsMinimizer();
            //fmin = new ec.tstoolkit.maths.realfunctions.jbfgs.Bfgs();
            fmin.setConvergenceCriterion(m_eps);
        }

        fmin.setMaxIter(10);
        for (int i = 0; i < 3; ++i) {
            fn_ = buildFunction(null);
            IReadDataBlock parameters = m_mapping.map(m_bsm);
            fmin.minimize(fn_.evaluate(parameters));
            m_bconverged = fmin.getIterCount() < fmin.getMaxIter();
            fnmax_ = (SsfFunctionInstance<BasicStructuralModel, SsfBsm>) fmin.getResult();
            m_bsm = fnmax_.getCore();
            m_ll = (DkConcentratedLikelihood) fnmax_.getLikelihood();

            Component cmp = m_bsm.fixMaxVariance(1);
            if (cmp != m_mapping.getFixedComponent()) {
                m_mapping.setFixedComponent(cmp);
            } else {
                break;
            }
        }

        if (!m_bconverged) {
            fmin.setMaxIter(30);
            fn_ = buildFunction(null);
            IReadDataBlock parameters = m_mapping.map(m_bsm);
            fmin.minimize(fn_.evaluate(parameters));
            m_bconverged = fmin.getIterCount() < fmin.getMaxIter();
            fnmax_ = (SsfFunctionInstance<BasicStructuralModel, SsfBsm>) fmin.getResult();
            m_bsm = fnmax_.getCore();
            m_ll = (DkConcentratedLikelihood) fnmax_.getLikelihood();
            Component cmp = m_bsm.fixMaxVariance(1);
            if (cmp != m_mapping.getFixedComponent()) {
                m_mapping.setFixedComponent(cmp);
            }
        }

        boolean ok = m_bconverged;
        if (fixsmallvariance(m_bsm))// bsm.FixSmallVariances(1e-4))
        {
            updateSpec(m_bsm);
            // update the likelihood !
            fn_ = buildFunction(null);
            IReadDataBlock parameters = m_mapping.map(m_bsm);
            fnmax_ = (SsfFunctionInstance<BasicStructuralModel, SsfBsm>) fn_.evaluate(parameters);
            m_ll = (DkConcentratedLikelihood) fnmax_.getLikelihood();
            ok = false;
        }
        if (m_factor != 1) {
            m_ll.rescale(m_factor);
        }
        return ok;
    }

    private SsfFunction<BasicStructuralModel, SsfBsm> buildFunction(BsmMapping mapping) {
        SsfData data = new SsfData(m_y);
        SsfFunction<BasicStructuralModel, SsfBsm> fn = new SsfFunction<>(
                data, m_x, diffuseItems(), mapping == null ? m_mapping : mapping, (BasicStructuralModel bsm) -> SsfBsm.create(bsm));
        fn.setFast(true);
        return fn;
    }

    private int[] diffuseItems() {
        int[] idiffuse = null;
        if (m_x != null && m_dregs) {
            idiffuse = new int[m_x.getColumnsCount()];
            for (int i = 0; i < idiffuse.length; ++i) {
                idiffuse[i] = i;
            }
        }
        return idiffuse;
    }

    private boolean estimate() {
        for (int i = 0; i < 4; ++i) {
            if (_estimate()) {
                return true;
            }
        }
        return true;

    }

    @SuppressWarnings("unchecked")
    private boolean fixsmallvariance(BasicStructuralModel model) {
        // return false;
        double vmin = m_dsmall;
        int imin = -1;
        BsmMapping mapper = new BsmMapping(model.getSpecification(), m_freq,
                BsmMapping.Transformation.None);
        mapper.setFixedComponent(m_mapping.getFixedComponent());
        SsfFunction<BasicStructuralModel, SsfBsm> fn = buildFunction(mapper);
        IReadDataBlock p = mapper.map(model);
        SsfFunctionInstance instance = new SsfFunctionInstance(fn, p);
        double ll = instance.getLikelihood().getLogLikelihood();
        int nvar = mapper.getVarsCount();
        for (int i = 0; i < nvar; ++i) {
            if (p.get(i) < 1e-2) {
                DataBlock np = new DataBlock(p);
                np.set(i, 0);
                instance = new SsfFunctionInstance(fn, np);
                double llcur = instance.getLikelihood().getLogLikelihood();
                double v = 2 * (ll - llcur);
                if (v < vmin) {
                    vmin = v;
                    imin = i;
                }
            }
        }

        if (imin < 0) {
            return false;
        }
        Component cmp = mapper.getComponent(imin);
        model.setVariance(cmp, 0);
        m_spec.fixComponent(cmp);
        return true;
    }

    /**
     *
     * @return
     */
    public DkConcentratedLikelihood getLikelihood() {
        return m_ll;
    }

    /**
     *
     * @return
     */
    public double getLRatioForSmallVariance() {
        return m_dsmall;
    }

    /**
     *
     * @return
     */
    public double getPrecision() {
        return m_eps;
    }

    /**
     *
     * @return
     */
    public BasicStructuralModel getResult() {
        if (m_bsm == null && m_y != null) {
            estimate();
        }
        return m_bsm;
    }

    /**
     *
     * @return
     */
    public ModelSpecification getSpecification() {
        return m_spec;
    }

    /**
     *
     * @return
     */
    public boolean hasConverged() {
        return m_bconverged;
    }

    @SuppressWarnings("unchecked")
    private BasicStructuralModel initialize() {
        // Search for the highest Variance
        // m_mapper = new BsmMapper(m_spec, m_freq);
        // BasicStructuralModel start = new BasicStructuralModel(m_spec,
        // m_freq);
        // if (m_spec.hasNoise())
        // m_mapper.setFixedComponent(Component.Noise);
        // else if (m_spec.hasLevel())
        // m_mapper.setFixedComponent(Component.Level);
        // else if (m_mapper.getDim() >= 1)
        // m_mapper.setFixedComponent(m_mapper.getComponent(0));
        // return start;

        m_mapping = new BsmMapping(m_spec, m_freq);
        BasicStructuralModel start = new BasicStructuralModel(m_spec, m_freq);
        if (m_mapping.getDim() == 1) {
            m_mapping.setFixedComponent(m_mapping.getComponent(0));
            return start;
        }
        // m_mapper.setFixedComponent(Component.Noise);

        BsmMapping mapping = new BsmMapping(m_spec, m_freq,
                BsmMapping.Transformation.None);
        SsfFunction<BasicStructuralModel, SsfBsm> fn = buildFunction(mapping);

        SsfFunctionInstance instance = new SsfFunctionInstance(fn, mapping.getDefault());
        double lmax = instance.getLikelihood().getLogLikelihood();
        IReadDataBlock p = instance.getParameters();
        int imax = -1;
        int nvars = mapping.getVarsCount();
        for (int i = 0; i < nvars; ++i) {
            DataBlock np = new DataBlock(p);
            np.set(.5);
            np.set(i, 1);
            int ncur = nvars;
            if (mapping.hasCycleDumpingFactor()) {
                np.set(ncur++, .9);
            }
            if (mapping.hasCycleLength()) {
                np.set(ncur, 1);
            }
            instance = new SsfFunctionInstance(fn, np);
            double nll = instance.getLikelihood().getLogLikelihood();
            if (nll > lmax) {
                lmax = nll;
                imax = i;
            }
        }
        if (imax < 0) {
            if (m_spec.hasNoise()) {
                m_mapping.setFixedComponent(Component.Noise);
            } else if (m_spec.hasLevel()) {
                m_mapping.setFixedComponent(Component.Level);
            } else {
                m_mapping.setFixedComponent(m_mapping.getComponent(0));
            }
            return start;
        } else {
            Component cmp = mapping.getComponent(imax);
            m_mapping.setFixedComponent(cmp);
            DataBlock np = new DataBlock(p);
            np.set(.1);
            np.set(imax, 1);
            if (mapping.hasCycleDumpingFactor()) {
                np.set(nvars++, .9);
            }
            if (mapping.hasCycleLength()) {
                np.set(nvars, 1);
            }
            return mapping.map(np);
        }
    }

    /**
     *
     * @return
     */
    public boolean isUsingDiffuseRegressors() {
        return m_dregs;
    }

    /**
     *
     * @param y
     * @param freq
     * @return
     */
    public boolean process(IReadDataBlock y, int freq) {
        return process(y, null, freq);
    }

    /**
     *
     * @param y
     * @param x
     * @param freq
     * @return
     */
    public boolean process(IReadDataBlock y, SubMatrix x, int freq) {
        AbsMeanNormalizer normalizer = new AbsMeanNormalizer();
        normalizer.process(y);
        m_y = normalizer.getNormalizedData();
        m_factor = normalizer.getFactor();
        m_x = x;
        m_freq = freq;
        boolean rslt = estimate();
        return rslt;
    }

    /**
     *
     * @param value
     */
    public void setLRatioForSmallVariance(double value) {
        m_dsmall = value;
    }

    /**
     *
     * @param value
     */
    public void setPrecision(double value) {
        m_eps = value;
    }

    /**
     *
     * @param spec
     */
    public void setSpecification(BsmSpecification spec) {
        m_spec = spec.getModelSpecification().clone();
        m_eps = spec.getPrecision();
        m_dregs = spec.isDiffuseRegressors();
        switch (spec.getOptimizer()) {
            case LevenbergMarquardt:
                m_min = new ProxyMinimizer(new ec.demetra.realfunctions.levmar.LevenbergMarquardtMinimzer());
                break;
            case MinPack:
                m_min = new ProxyMinimizer(new ec.demetra.realfunctions.minpack.LevenbergMarquardtMinimizer());
                break;
            case LBFGS:
                m_min = new ec.demetra.realfunctions.bfgs.Bfgs();
                break;
            default:
                m_min = null;
        }
        m_bsm = null;
    }

    public void setSpecification(ModelSpecification spec) {
        m_spec = spec.clone();
        m_bsm = null;
    }

    private void updateSpec(BasicStructuralModel bsm) {
        m_spec = bsm.getSpecification();
        Component fixed = m_mapping.getFixedComponent();
        m_mapping = new BsmMapping(m_spec, m_freq, m_mapping.transformation);
        m_mapping.setFixedComponent(fixed);
    }

    /**
     *
     * @param value
     */
    public void useDiffuseRegressors(boolean value) {
        m_dregs = value;
    }

    public IFunction likelihoodFunction() {
        BsmMapping mapping = new BsmMapping(m_bsm.getSpecification(), m_bsm.getFrequency(), Transformation.None);
        SsfFunction<BasicStructuralModel, SsfBsm> fn = buildFunction(mapping);
        double a = (m_ll.getN() - m_ll.getD()) * Math.log(m_factor);
        return new TransformedFunction(fn, TransformedFunction.linearTransformation(-a, 1));
    }

    public IFunctionPoint maxLikelihoodFunction() {
        BsmMapping mapping = new BsmMapping(m_bsm.getSpecification(), m_bsm.getFrequency(), Transformation.None);
        IFunction ll = likelihoodFunction();
        return ll.evaluate(mapping.map(m_bsm));
    }

}
