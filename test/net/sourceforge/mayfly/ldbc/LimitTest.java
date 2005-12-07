package net.sourceforge.mayfly.ldbc;

import junit.framework.*;

public class LimitTest extends TestCase {
    
    public void testIsSpecified() throws Exception {
        assertTrue(new Limit(0, 5).isSpecified());
        assertTrue(new Limit(50, 0).isSpecified());
        assertTrue(new Limit(Integer.MAX_VALUE, 5).isSpecified());
        assertFalse(Limit.NONE.isSpecified());
    }

}