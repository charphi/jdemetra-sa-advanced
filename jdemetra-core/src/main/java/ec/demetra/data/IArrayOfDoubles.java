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
package ec.demetra.data;

import ec.tstoolkit.design.Development;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;

/**
 * Represents a read/write block of doubles
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public interface IArrayOfDoubles extends IArrayOfDoublesReader {

    /**
     * Copies data from a given buffer. The buffer must contain enough data for
     * filling this object (it can be larger).
     *
     * @param buffer The buffer containing the data
     * @param start The position in the buffer of the first data being copied
     */
    default void copyFrom(double[] buffer, int start) {
        set(start == 0 ? i -> buffer[i] : i -> buffer[start + i]);
    }

    /**
     * Extracts a new data block from an existing data block
     *
     * @param start The position of the first extracted data
     * @param length The number of data being extracted
     * @return A new data block is returned, except when the extract is
     * equivalent to the current object. In that case, the existing data block
     * can be returned.
     */
    @Override
    IArrayOfDoubles extract(int start, int length);

   
    /**
     * Sets an element of the data block to a given value
     *
     * @param idx The position of the element being modified.
     * @param value The new value.
     */
    void set(int idx, double value);

    default void apply(DoubleUnaryOperator fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.applyAsDouble(get(i)));
        }
    }

    default void applyIf(DoublePredicate pred, DoubleUnaryOperator fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            double cur = get(i);
            if (pred.test(cur)) {
                set(i, fn.applyAsDouble(cur));
            }
        }
    }

    default void applyRecursively(final double initial, DoubleBinaryOperator fn) {
        double cur = initial;
        int n = getLength();
        for (int i = 0; i < n; i++) {
            cur = fn.applyAsDouble(cur, get(i));
            set(i, cur);
        }
    }

    default void apply(IArrayOfDoublesReader x, DoubleBinaryOperator fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.applyAsDouble(get(i), x.get(i)));
        }
    }

    default void copy(IArrayOfDoublesReader x) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, x.get(i));
        }
    }

    default void set(DoubleSupplier fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.getAsDouble());
        }
    }

    default void setIf(DoublePredicate pred, DoubleSupplier fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            if (pred.test(get(i))) {
                set(i, fn.getAsDouble());
            }
        }
    }

    default void set(IntToDoubleFunction fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.applyAsDouble(i));
        }
    }

    default void set(IArrayOfDoublesReader x, DoubleUnaryOperator fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.applyAsDouble(x.get(i)));
        }
    }

    default void set(IArrayOfDoublesReader x, IArrayOfDoublesReader y, DoubleBinaryOperator fn) {
        int n = getLength();
        for (int i = 0; i < n; i++) {
            set(i, fn.applyAsDouble(x.get(i), y.get(i)));
        }
    }

    // some default shortcuts
    default void setAY(final double a, IArrayOfDoubles y) {
        if (a == 0) {
            set(()->0);
        } else if (a == 1) {
            copy(y);
        } else if (a == -1) {
            set(y, u -> -u);
        } else {
            set(y, u -> a*u);
        }
    }

    // some default shortcuts
    default void addAY(double a, IArrayOfDoubles y) {
        if (a == 0) {
            return;
        } else if (a == 1) {
            apply(y, (u, v) -> u + v);
        } else if (a == -1) {
            apply(y, (u, v) -> u - v);
        } else {
            apply(y, (u, v) -> u + a * v);
        }
    }

    default void add(double a) {
        if (a != 0) {
            apply(x -> x + a);
        }
    }

    default void add(IArrayOfDoublesReader y) {
        apply(y, (a, b) -> a + b);
    }

    default void sub(double a) {
        if (a != 0) {
            apply(x -> x + a);
        }
    }

    default void sub(IArrayOfDoublesReader y) {
        apply(y, (a, b) -> a - b);
    }

    default void mul(double a) {
        if (a == 0) {
            set(() -> 0);
        } else if (a != 1) {
            apply(x -> x * a);
        }
    }

    default void mul(IArrayOfDoublesReader y) {
        apply(y, (a, b) -> a * b);
    }

    default void div(double a) {
        if (a == 0) {
            set(() -> Double.NaN);
        } else if (a != 1) {
            apply(x -> x * a);
        }
    }

    default void div(IArrayOfDoublesReader y) {
        apply(y, (a, b) -> a / b);
    }

}
