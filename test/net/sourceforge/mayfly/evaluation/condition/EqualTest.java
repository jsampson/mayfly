package net.sourceforge.mayfly.evaluation.condition;

import junit.framework.TestCase;

import net.sourceforge.mayfly.datastore.LongCell;
import net.sourceforge.mayfly.datastore.NullCell;
import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.datastore.TupleBuilder;
import net.sourceforge.mayfly.evaluation.condition.Equal;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;
import net.sourceforge.mayfly.evaluation.expression.literal.QuotedString;

public class EqualTest extends TestCase {

    public void testColumnAndQuotedString() throws Exception {
        Row row = new Row(
            new TupleBuilder()
                .appendColumnCellContents("colA", "1")
        );

        assertTrue(new Equal(new SingleColumn("colA"), new QuotedString("'1'")).evaluate(row));
        assertFalse(new Equal(new SingleColumn("colA"), new QuotedString("'2'")).evaluate(row));
    }
    
    public void testEvaluate() throws Exception {
        Equal equal = new Equal(new SingleColumn("a"), new SingleColumn("b"));

        /** Although Mayfly tries to mitigate some of the confusing
            aspects of "null = null" being false, when push comes
            to shove - like
            {@link net.sourceforge.mayfly.acceptance.JoinTest#testJoinOnNull()}
            - we stick with the usual SQL semantics.
         */
        assertFalse(equal.evaluate(
            new Row(new TupleBuilder()
                .appendColumnCell("a", NullCell.INSTANCE)
                .appendColumnCell("b", NullCell.INSTANCE)
        )));

        assertTrue(equal.evaluate(
            new Row(new TupleBuilder()
                .appendColumnCell("a", new LongCell(5))
                .appendColumnCell("b", new LongCell(5))
        )));
    }

}