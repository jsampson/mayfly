package net.sourceforge.mayfly.ldbc.what;

import junit.framework.*;
import net.sourceforge.mayfly.datastore.*;

public class WhatTest extends TestCase {
    public void testApplyWhat_Simple() throws Exception {
        Row original = new Row(
            new TupleBuilder()
                .append(new TupleElement(new Column("colA"), new StringCell("1")))
                .append(new TupleElement(new Column("colB"), new StringCell("2")))
        );

        Row expected = new Row(
            new TupleBuilder()
                .append(new TupleElement(new Column("colB"), new StringCell("2")))
        );

        assertEquals(expected,
                     new What()
                        .add(new SingleColumn("colB"))
                        .applyTo(original)
        );
    }
}