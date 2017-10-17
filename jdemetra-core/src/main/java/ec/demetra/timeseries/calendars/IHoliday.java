/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.demetra.timeseries.calendars;

import java.time.LocalDate;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
public interface IHoliday {
    Iterable<IHolidayInfo> getIterable(LocalDate start, LocalDate end);
    
}
