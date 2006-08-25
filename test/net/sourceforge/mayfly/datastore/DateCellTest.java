package net.sourceforge.mayfly.datastore;

import junit.framework.TestCase;

import org.joda.time.DateTimeZone;

public class DateCellTest extends TestCase {
    
    public void testAsBriefString() throws Exception {
        assertEquals("1776-07-02", new DateCell(1776, 7, 2).asBriefString());
        assertEquals("0532-12-31", new DateCell(532, 12, 31).asBriefString());
    }
    
    public void testAsDate() throws Exception {
        long march1 = 636249600000L;
        assertEquals(march1,
            new DateCell(1990, 3, 1).asDate(DateTimeZone.UTC).getTime());
    }

}
