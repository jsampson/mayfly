package net.sourceforge.mayfly.evaluation;

import junit.framework.TestCase;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.LongCell;
import net.sourceforge.mayfly.datastore.StringCell;
import net.sourceforge.mayfly.datastore.TupleBuilder;
import net.sourceforge.mayfly.evaluation.expression.CountAll;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;

public class ResultRowTest extends TestCase {
    
    public void testFindColumn() throws Exception {
        ResultRow row = 
            new ResultRow()
                .withColumn("foo", "y", new StringCell("hi"))
                .withColumn("foo", "x", new LongCell(5))
                .withColumn("foo", "z", new LongCell(5))
                .with(new CountAll("count"), new LongCell(15))
                ;
        SingleColumn column = row.findColumn("x");
        assertEquals("foo", column.tableOrAlias());
        assertEquals("x", column.columnName());
    }

    public void testFindColumnAmbiguous() throws Exception {
        ResultRow row = 
            new ResultRow(
                new TupleBuilder()
                    .appendColumnCell("foo", "x", new LongCell(5))
                    .appendColumnCell("bar", "x", new LongCell(5))
                    .asRow()
            );
        try {
            row.findColumn("x");
            fail();
        }
        catch (MayflyException e) {
            assertEquals("ambiguous column x", e.getMessage());
        }
    }

    public void testFindColumnWithPeriod() throws Exception {
        ResultRow row = 
            new ResultRow(
                new TupleBuilder()
                    .appendColumnCell("foo", "x", new LongCell(5))
                    .appendColumnCell("bar", "x", new LongCell(5))
                    .asRow()
            );
        try {
            row.findColumn("foo.x");
            fail();
        }
        catch (MayflyException e) {
            assertEquals("column name foo.x should not contain a period", e.getMessage());
        }
    }

    public void testFindColumnNotFound() throws Exception {
        ResultRow row = 
            new ResultRow(
                new TupleBuilder()
                    .appendColumnCell("foo", "x", new LongCell(5))
                    .asRow()
            );
        try {
            row.findColumn("y");
            fail();
        }
        catch (MayflyException e) {
            assertEquals("no column y", e.getMessage());
        }
    }
    
    public void testFindValue() throws Exception {
        ResultRow row = 
            new ResultRow(
                new TupleBuilder()
                    .appendColumnCell("foo", "x", new LongCell(5))
                    .appendColumnCell("bar", "x", new LongCell(6))
                    .appendExpression(new CountAll("count"), new LongCell(2))
                    .asRow()
            );
        LongCell barX = (LongCell) row.findValue(new SingleColumn("bar", "x"));
        assertEquals(6L, barX.asLong());

        LongCell count = (LongCell) row.findValue(new CountAll("Count"));
        assertEquals(2L, count.asLong());
    }
    
    public void testAdd() throws Exception {
        ResultRow row = new ResultRow();
        assertEquals(0, row.size());
        
        ResultRow biggerRow = row.with(new CountAll("count"), new LongCell(2));
        assertEquals(1, biggerRow.size());
        LongCell value = (LongCell) biggerRow.findValue(new CountAll("count"));
        assertEquals(2L, value.asLong());
    }

}
