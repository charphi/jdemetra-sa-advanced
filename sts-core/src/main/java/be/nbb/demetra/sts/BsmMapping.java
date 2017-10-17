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

import ec.demetra.realfunctions.IParametricMapping;
import ec.demetra.realfunctions.ParamValidation;
import ec.demetra.ssf.implementations.structural.BasicStructuralModel;
import ec.demetra.ssf.implementations.structural.Component;
import ec.demetra.ssf.implementations.structural.ComponentUse;
import ec.demetra.ssf.implementations.structural.ModelSpecification;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.data.IDataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.ReadDataBlock;
import ec.tstoolkit.design.Development;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public class BsmMapping implements IParametricMapping<BasicStructuralModel> {

    static final double STEP = 1e-6, STEP2 = 1e-4;
    private static double RMIN = 0, RMAX = 0.999, RDEF = 0.5;
    private static double PMIN = .25, PMAX = 2.5, PDEF = 1;

    /**
     *
     */
    public enum Transformation {

        /**
         *
         */
        None,
        /**
         *
         */
        Exp,
        /**
         *
         */
        Square
    }

    private Component cFixed = Component.Undefined;

    /**
     *
     */
    public final Transformation transformation;

    /**
     *
     */
    public final ModelSpecification spec;

    /**
     *
     */
    public final int freq;

    /**
     *
     * @param spec
     * @param freq
     */
    public BsmMapping(ModelSpecification spec, int freq) {
        transformation = Transformation.Square;
        this.spec = spec;
        this.freq = freq;
    }

    /**
     *
     * @param spec
     * @param freq
     * @param tr
     */
    public BsmMapping(ModelSpecification spec, int freq, Transformation tr) {
        this.transformation = tr;
        this.spec = spec;
        this.freq = freq;
    }

    public int getVarsCount() {
        int n = 0;
        if (_hasLevel()) {
            ++n;
        }
        if (_hasSlope()) {
            ++n;
        }
        if (_hasSeas()) {
            ++n;
        }
        if (_hasCycle()) {
            ++n;
        }
        if (_hasNoise()) {
            ++n;
        }
        return n;
    }

    public boolean hasCycleDumpingFactor() {
        return spec.hasCycle() && (spec.getCyclicalDumpingFactor() == null
                || !spec.getCyclicalDumpingFactor().isFixed());
    }

    public boolean hasCycleLength() {
        return spec.hasCycle() && (spec.getCyclicalPeriod() == null
                || !spec.getCyclicalPeriod().isFixed());
    }

    boolean _hasLevel() {
        return spec.getLevelUse() == ComponentUse.Free && cFixed != Component.Level;
    }

    int _pCycle() {
        if (spec.getCycleUse()== ComponentUse.Unused) {
            return 0;
        }
        int n = 0;
        Parameter p = spec.getCyclicalDumpingFactor();
        if (p == null || !p.isFixed()) {
            ++n;
        }
        p = spec.getCyclicalPeriod();
        if (p == null || !p.isFixed()) {
            ++n;
        }
        return n;
    }

    boolean _hasCycle() {
        return spec.getCycleUse() == ComponentUse.Free && cFixed != Component.Cycle;
    }

    boolean _hasNoise() {
        return spec.getNoiseUse() == ComponentUse.Free && cFixed != Component.Noise;
    }

    boolean _hasSeas() {
        return spec.getSeasUse() == ComponentUse.Free
                && cFixed != Component.Seasonal;
    }

    boolean _hasSlope() {
        return spec.getSlopeUse() == ComponentUse.Free && cFixed != Component.Slope;
    }

    @Override
    public boolean checkBoundaries(IReadDataBlock p) {
        int pc = _pCycle();
        int nvar = p.getLength() - pc;
        if (transformation == Transformation.None) {
            for (int i = 0; i < nvar; ++i) {
                if (p.get(i) <= 0) {
                    return false;
                }
            }
        } else if (transformation == Transformation.Square) {
            for (int i = 0; i < nvar; ++i) {
                if (p.get(i) < -.1 || p.get(i) > 10) {
                    return false;
                }
            }
        }
        if (pc > 0) {
            // rho
            if (hasCycleDumpingFactor()) {
                double rho = p.get(nvar++);
                if (rho < RMIN || rho > RMAX) {
                    return false;
                }
            }
            if (hasCycleLength()) {
                double period = p.get(nvar);
                if (period < PMIN || period > PMAX) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public double epsilon(IReadDataBlock p, int idx) {
        int pc = _pCycle();
        int nvar = p.getLength() - pc;
        if (idx < nvar) {
            double x = p.get(idx);
            if (x < .5) {
                return STEP;
            } else {
                return -STEP;
            }
        } else if (idx == nvar && hasCycleDumpingFactor()) {
            double x = p.get(idx);
            if (x < .5) {
                return STEP;
            } else {
                return -STEP;
            }
        } else {
            double x = p.get(idx);
            if (x < 1) {
                return STEP2;
            } else {
                return -STEP2;
            }
        }
    }

    /**
     *
     * @param idx
     * @return
     */
    public Component getComponent(final int idx) {
        int cur = idx;
        if (_hasLevel()) {
            if (cur == 0) {
                return Component.Level;
            }
            --cur;
        }
        if (_hasSlope()) {
            if (cur == 0) {
                return Component.Slope;
            }
            --cur;
        }
        if (_hasSeas()) {
            if (cur == 0) {
                return Component.Seasonal;
            }
            --cur;
        }
        if (_hasNoise()) {
            if (cur == 0) {
                return Component.Noise;
            }
            --cur;
        }
        if (_hasCycle()) {
            if (cur == 0) {
                return Component.Cycle;
            }
        }
        return Component.Undefined;
    }

    @Override
    public int getDim() {
        int n = 0;
        if (_hasLevel()) {
            ++n;
        }
        if (_hasSlope()) {
            ++n;
        }
        if (_hasSeas()) {
            ++n;
        }
        if (_hasNoise()) {
            ++n;
        }
        if (_hasCycle()) {
            ++n;
        }
        return n + _pCycle();
    }

    /**
     *
     * @return
     */
    public Component getFixedComponent() {
        return cFixed;
    }

    private double inparam(double d) {
        switch (transformation) {
            case None:
                return d;
            case Square:
                return d * d;
            default:
                return Math.exp(2 * d);
        }
    }

    @Override
    public double lbound(int idx) {
        return transformation == Transformation.None ? 0
                : Double.NEGATIVE_INFINITY;
    }

    @Override
    public BasicStructuralModel map(IReadDataBlock p) {
        BasicStructuralModel t = new BasicStructuralModel(spec, freq);
        int idx = 0;
        if (_hasLevel()) {
            t.setVariance(Component.Level, inparam(p.get(idx++)));
        }
        if (_hasSlope()) {
            t.setVariance(Component.Slope, inparam(p.get(idx++)));
        }
        if (_hasSeas()) {
            t.setVariance(Component.Seasonal,  inparam(p.get(idx++)));
        }
        if (_hasNoise()) {
            t.setVariance(Component.Noise, inparam(p.get(idx++)));
        }
        if (_hasCycle()) {
            t.setVariance(Component.Cycle, inparam(p.get(idx++)));
        }
        if (spec.getCycleUse() != ComponentUse.Unused) {
            double cdump, clen;
            Parameter pm = spec.getCyclicalDumpingFactor();
            if (pm == null || !pm.isFixed()) {
                cdump = p.get(idx++);
            } else {
                cdump = pm.getValue();
            }
            pm = spec.getCyclicalPeriod();
            if (pm == null || !pm.isFixed()) {
                clen = 6 * freq * p.get(idx);
            } else {
                clen = freq * pm.getValue();
            }
            t.setCycle(cdump, clen);
        }
        if (cFixed != Component.Undefined) {
            t.setVariance(cFixed, 1);
        }
        return t;
    }

    private double outparam(double d) {
        switch (transformation) {
            case None:
                return d;
            case Square:
                return d <= 0 ? 0 : Math.sqrt(d);
            default:
                return .5 * Math.log(d);
        }
    }

    /**
     *
     * @param value
     */
    public void setFixedComponent(Component value) {
        cFixed = value;
    }

    @Override
    public double ubound(int idx) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public ParamValidation validate(IDataBlock ioparams) {
        ParamValidation status = ParamValidation.Valid;
        if (ioparams.getLength() == 0) {
            return ParamValidation.Valid;
        }
        int pc = _pCycle();
        int nvar = ioparams.getLength() - pc;
        if (transformation == Transformation.Square) {
            for (int i = 0; i < nvar; ++i) {
                if (ioparams.get(i) > 10) {
                    ioparams.set(i, 10);
                    status = ParamValidation.Changed;
                } else if (ioparams.get(i) < -0.1) {
                    ioparams.set(i, Math.min(10, -ioparams.get(i)));
                    status = ParamValidation.Changed;
                }
            }
        } else if (transformation == Transformation.None) {
            for (int i = 0; i < nvar; ++i) {
                if (ioparams.get(i) < 1e-9) {
                    ioparams.set(i, 1e-9);
                    status = ParamValidation.Changed;
                }
            }
        }
        if (pc > 0) {
            if (hasCycleDumpingFactor()) {
                double rho = ioparams.get(nvar);
                if (rho < RMIN) {
                    ioparams.set(nvar, 0.1);
                    status = ParamValidation.Changed;
                }
                if (rho > RMAX) {
                    ioparams.set(nvar, .9);
                    status = ParamValidation.Changed;
                }
                ++nvar;
            }
            if (hasCycleLength()) {
                double p = ioparams.get(nvar);
                if (p < PMIN) {
                    ioparams.set(nvar , PMIN);
                    status = ParamValidation.Changed;
                }
                if (p > PMAX) {
                    ioparams.set(nvar, PMAX);
                    status = ParamValidation.Changed;
                }
            }
        }
        return status;
    }

    @Override
    public String getDescription(final int idx) {
        int n = getVarsCount();
        if (idx < n) {
            return getComponent(idx).name() + " var.";
        }
        if (idx == n && this.hasCycleDumpingFactor()) {
            return "Cycle dumping factor";
        } else {
            return "Cycle length";
        }
    }

    @Override
    public IReadDataBlock getDefault() {
        // creates the parameters array corresponding to the given model.
        BasicStructuralModel model=new BasicStructuralModel(spec, freq);
        return map(model);
    }
    
    public IReadDataBlock map(BasicStructuralModel t) {
        double[] p = new double[getDim()];
        int idx = 0;
        if (_hasLevel()) {
            p[idx++] = outparam(t.getVariance(Component.Level));
        }
        if (_hasSlope()) {
            p[idx++] = outparam(t.getVariance(Component.Slope));
        }
        if (_hasSeas()) {
            p[idx++] = outparam(t.getVariance(Component.Seasonal));
        }
        if (_hasNoise()) {
            p[idx++] = outparam(t.getVariance(Component.Noise));
        }
        if (_hasCycle()) {
            p[idx++] = outparam(t.getVariance(Component.Cycle));
        }
        if (spec.getCycleUse() != ComponentUse.Unused) {
            double cdump, clen;
            Parameter pm = spec.getCyclicalDumpingFactor();
            if (pm == null || !pm.isFixed()) {
                p[idx++] = t.getCyclicalDumpingFactor();
            }
            pm = spec.getCyclicalPeriod();
            if (pm == null || !pm.isFixed()) {
                p[idx++] = t.getCyclicalPeriod() / (6 * freq);
            }
        }
        return new ReadDataBlock(p);
    }
}
