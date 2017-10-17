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
package ec.demetra.timeseries.simplets;

import ec.demetra.timeseries.IDateDomain;
import ec.demetra.timeseries.PeriodSelector;
import ec.demetra.timeseries.PeriodSelectorType;
import ec.demetra.timeseries.TsException;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.design.Immutable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
@Immutable
public final class TsDomain implements IDateDomain, Serializable, Iterable<TsPeriod> {

    private static final class TSPeriodIterator implements Iterator<TsPeriod> {

        private final TsDomain m_dom;
        private int m_cur = 0;

        TSPeriodIterator(final TsDomain domain) {
            m_dom = domain;
        }

        @Override
        public boolean hasNext() {
            return m_cur < m_dom.getLength();
        }

        @Override
        public TsPeriod next() {
            return m_dom.get(m_cur++);
        }
    }
    private static final long serialVersionUID = 3500593038737276467L;
    private final TsFrequency freq;
    private final int beg;
    private final int len;

    TsDomain(final TsFrequency freq, final int beg, final int count) {
        this.freq = freq;
        this.beg = beg;
        len = count;
    }

    /**
     * Creates a new time domain, identified by its frequency, the year and the
     * position of the first period and the length of the domain.
     *
     * @param freq The frequency.
     * @param firstyear Year of the first period
     * @param firstperiod (0-based) position in the year of the first period.
     * @param count Length of the domain (number of periods).
     */
    public TsDomain(final TsFrequency freq, final int firstyear,
            final int firstperiod, final int count) {
        this(freq, TsPeriod.calcId(freq.intValue(), firstyear, firstperiod),
                count);
    }

    /**
     * Creates a new time domain from its first period and its length.
     *
     * @param start First period. This Object is not used internally.
     * @param count Number of periods.
     */
    public TsDomain(final TsPeriod start, final int count) {
        this(start.getFrequency(), start.id(), count);
    }

    /**
     * Checks that a given domain is inside another one. Both domains must have
     * the same frequency.
     *
     * @param domain The other domain
     * @return true if the given domain is (not strictly) included in this
     * domain.
     */
    public boolean contains(TsDomain domain) {
        if (this.freq != domain.freq) {
            throw new TsException(TsException.INCOMPATIBLE_FREQ);
        }
        return this.beg <= domain.beg
                && this.beg + this.len >= domain.beg + domain.len;
    }

    /**
     * Shortens this domain.
     *
     * @param nfirst Number of periods to drop at the beginning of the domain.
     * If nfirst &lt 0, -nfirst periods are added.
     * @param nlast Number of periods to drop at the end of the domain. If nlast
     * &lt 0, -nlast periods are added.
     * @return The returned domain may be Empty.
     */
    public TsDomain drop(int nfirst, int nlast) {
        return extend(-nfirst, -nlast);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof TsDomain && equals((TsDomain) obj));
    }

    public boolean equals(TsDomain other) {
        return (freq == other.freq) && (beg == other.beg)
                && (len == other.len);
    }

    /**
     * Extends this domain.
     *
     * @param nbefore Number of periods to add at the beginning of the domain.
     * If nbefore &lt 0, -nbefore periods are dropped.
     * @param nafter Number of periods to add at the end of the domain. If
     * nafter &lt 0, -nafter periods are dropped.
     * @return The returned domain may be Empty.
     */
    public TsDomain extend(final int nbefore, final int nafter) {
        int c = Math.max(0, len + nbefore + nafter);
        return new TsDomain(freq, beg - nbefore, c);
    }

    int firstid() {
        return beg;
    }

    @Override
    public TsPeriod get(final int idx) {
        return new TsPeriod(freq, beg + idx);
    }

    @Override
    public Period getPeriod(){
        switch (freq){
            case Yearly:return Period.ofYears(1);
            case Monthly:return Period.ofMonths(1);
            default:return Period.ofMonths(12/freq.intValue());
        }
     }
    /**
     * Return the first period at the end of the domain.
     *
     * @return The end of the domain. That period doesn't belong to the domain!
     */
    public TsPeriod getEnd() {
        return new TsPeriod(freq, beg + len);
    }

    /**
     * Returns the frequency of each periods.
     *
     * @return The frequency of the domain. Even an empty domain must have a
     * frequency.
     */
    public TsFrequency getFrequency() {
        return freq;
    }

    /**
     * Counts the number of years (complete or not).
     *
     * @return The number of years.
     */
    public int getYearsCount() {
        return getLast().getYear() - getStart().getYear() + 1;
    }

    /**
     * Counts the number of full years.
     *
     * @return The number of full years.
     */
    public int getFullYearsCount() {
        int ifreq = freq.intValue();
        int start = beg;
        int pos = start % ifreq;
        if (pos > 0) {
            start += ifreq - pos;
        }
        int end = beg + len;
        end -= end % ifreq;
        return (end - start) / ifreq;
    }

    /**
     * Returns the last period of the domain (which is just before getEnd().
     *
     * @return A new period is returned. Should not be used on empty domain,
     */
    public TsPeriod getLast() {
        return new TsPeriod(freq, beg + len - 1);
    }

    @Override
    public int getLength() {
        return len;
    }

    /**
     * Returns the first period of the domain.
     *
     * @return A new period is returned, even for empty domain,
     */
    public TsPeriod getStart() {
        return new TsPeriod(freq, beg);
    }
    
    /**
     * Id of the starting period
     * @return 
     */
    public int startId(){
        return beg;
    }

    @Override
    public int hashCode() {
        return freq.hashCode() + beg + len;
    }

    public static TsDomain and(TsDomain l, TsDomain r) {
        if (l == null) {
            return r;
        }
        if (r == null) {
            return l;
        }
        return l.intersection(r);
    }

    public static TsDomain or(TsDomain l, TsDomain r) {
        if (l == null) {
            return l;
        }
        if (r == null) {
            return l;
        }
        return l.union(r);
    }

    /**
     * Returns the intersection between this domain and another domain.
     *
     * @param d The other domain. Should have the same frequency.
     * @return <I>null</I> if the frequencies are not the same. May be Empty.
     */
    public TsDomain intersection(final TsDomain d) {
        if (d == this) {
            return this;
        }
        if (d.freq != freq) {
            throw new TsException(TsException.INCOMPATIBLE_FREQ);
        }

        int ln = len, rn = d.len;
        int lbeg = beg, rbeg = d.beg;

        int lend = lbeg + ln, rend = rbeg + rn;
        int beg = lbeg <= rbeg ? rbeg : lbeg;
        int end = lend >= rend ? rend : lend;

        return new TsDomain(freq, beg, Math.max(0, end - beg));
    }

    /**
     * Checks if a domain is empty.
     *
     * @return true if the domain is empty. false otherwise.
     */
    public boolean isEmpty() {
        return this.len == 0;
    }

    /**
     * Returns an iterator on the periods of the domain
     *
     * @return A new iterator. The first call to the iterator should be
     * "has+Next()".
     */
    @Override
    public Iterator<TsPeriod> iterator() {
        return new TSPeriodIterator(this);
    }

    public Stream<TsPeriod> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Moves forward or backward this domain.
     *
     * @param nperiods Any integer values. Move forward if nperiods is &gt> 0,
     * backward otherwise.
     * @return The start of the returned TsDomain is changed of nperiods
     * position. The current object is not modified.
     */
    public TsDomain move(final int nperiods) {
        return new TsDomain(freq, beg + nperiods, len);
    }

    @Override
    public int search(final LocalDate day) {
        TsPeriod p = new TsPeriod(freq, day);
        return search(p);
    }

    /**
     * Searches the place of a given period.
     *
     * @param p The period searched in the domain.
     * @return The index of the period is returned if it is found (exact match),
     * -1 otherwise.
     */
    public int search(final TsPeriod p) {

        if (p.getFrequency() != freq) {
            return -1;
        }
        int id = p.id();
        id -= beg;
        if ((id < 0) || (id >= len)) {
            return -1;
        } else {
            return id;
        }
    }

    /**
     * Makes a new domain from this domain and a period selector.
     *
     * @param ps The selector.
     * @return The corresponding domain. May be Empty.
     */
    public TsDomain select(final PeriodSelector ps) {
        if (len == 0) {
            return this;
        }
        // throw new ArgumentNullException("ps");

        int nf = 0, nl = 0;
        PeriodSelectorType type = ps.getType();
        if (type == PeriodSelectorType.None) {
            nf = len;
        } else if (type == PeriodSelectorType.First) {
            int nobs = ps.getN0();
            nl = len - nobs;
        } else if (type == PeriodSelectorType.Last) {
            int nobs = ps.getN1();
            nf = len - nobs;
        } else if (type == PeriodSelectorType.Excluding) {
            nf = ps.getN0();
            nl = ps.getN1();
            if (nf < 0) {
                nf = -nf * freq.intValue();
            }
            if (nl < 0) {
                nl = -nl * freq.intValue();
            }

        } else {
            if ((type == PeriodSelectorType.From)
                    || (type == PeriodSelectorType.Between)) {
                LocalDate d = ps.getD0();
                TsPeriod cur = new TsPeriod(freq, d);
                int c = cur.id() - beg;
                if (c >= len) {
                    nf = len; // on ne garde rien
                } else if (c >= 0) {
                    if (cur.firstDay().isBefore(d)) {
                        nf = c + 1;
                    } else {
                        nf = c;
                    }
                }
            }
            if ((type == PeriodSelectorType.To)
                    || (type == PeriodSelectorType.Between)) {
                LocalDate d = ps.getD1();
                TsPeriod cur = new TsPeriod(freq,d);

                int c = cur.id() - beg;
                if (c < 0) {
                    nl = len; // on ne garde rien
                } else if (c < len) {
                    if (cur.lastDay().isAfter(d)) {
                        nl = len - c;
                    } else {
                        nl = len - c - 1;
                    }
                }
            }
        }
        if (nf < 0) {
            nf = 0;
        }
        if (nl < 0) {
            nl = 0;
        }
        return new TsDomain(freq, beg + nf, len - nf - nl);
    }

    /**
     * Returns the union between this domain and another one.
     *
     * @param d Another domain. Should have the same frequency.
     * @return <I>null</I> if the frequencies are not the same. If the actual
     * union contains a hole, it is removed in the returned domain.
     *
     */
    public TsDomain union(final TsDomain d) {
        if (d == this) {
            return this;
        }
        if (d.freq != freq) {
            return null;
        }

        int ln = len, rn = d.len;
        int lbeg = beg, rbeg = d.beg;
        int lend = lbeg + ln, rend = rbeg + rn;
        int beg = lbeg <= rbeg ? lbeg : rbeg;
        int end = lend >= rend ? lend : rend;

        return new TsDomain(freq, beg, end - beg);
    }

    public TsDomain changeFrequency(final TsFrequency newfreq, final boolean complete) {
        int freq = this.freq.intValue(), nfreq = newfreq.intValue();
        if (freq == nfreq) {
            return this;
        }

        if (freq > nfreq) {
            if (freq % nfreq != 0) {
                return null;
            }

            int nconv = freq / nfreq;

            int z0 = 0;

            // beginning and end
            int nbeg = beg / nconv;
            int n0 = nconv, n1 = nconv;
            if (beg % nconv != 0) {
                if (complete) {
                if (beg > 0) {
                    ++nbeg;
                    z0 = nconv - beg % nconv;
                } else {
                    z0 = - beg % nconv;
                }
                } else {
                    if (beg < 0) {
                        --nbeg;
                    }
                    n0 = (nbeg + 1) * nconv - beg;
                }
            }

            int end = beg + len; // excluded
            int nend = end / nconv;

            if (end % nconv != 0) {
                if (complete) {
                    if (end < 0) {
                        --nend;
                    }
                } else {
                    if (end > 0) {
                        ++nend;
                    }
                    n1 = end - (nend - 1) * nconv;
                }
            }
            int n = nend - nbeg;
            return new TsDomain(newfreq, nbeg, n);
        } else { // The new frequency is higher than the current one
            if (nfreq % freq != 0) {
                return null;
            }
            TsPeriod start=getStart().firstPeriod(newfreq);
            return new TsDomain(start, len*nfreq/freq);
        }
    }
    
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(getStart()).append((" - ")).append(getLast());
        return builder.toString();
    }
}
