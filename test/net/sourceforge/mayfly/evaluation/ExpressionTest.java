package net.sourceforge.mayfly.evaluation;

import junit.framework.TestCase;

import net.sourceforge.mayfly.datastore.NullCell;
import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.datastore.TupleBuilder;
import net.sourceforge.mayfly.parser.Parser;

public class ExpressionTest extends TestCase {

    public void testSameExpression() throws Exception {
        Expression one = (Expression) new Parser("x + y * z / 2 || 5").parseWhatElement();
        Expression two = (Expression) new Parser("x+((y*z)/2) ||   5").parseWhatElement();
        assertTrue(one.sameExpression(two));
        assertTrue(two.sameExpression(one));
        
        Expression three = (Expression) new Parser("(x + y) * z / 2 || 5").parseWhatElement();
        assertFalse(three.sameExpression(one));
    }
    
    public void testResolve() throws Exception {
        Expression one = new Parser("x + 5 * avg(x) - count(*)").parseExpression().asNonBoolean();
        Expression resolved = one.resolveAndReturn(
            new Row(new TupleBuilder().appendColumnCell("foo", "x", NullCell.INSTANCE)));

        String expectedString = "foo.x + 5 * avg( foo.x ) - count ( * )";
        Expression expected = new Parser(expectedString).parseExpression().asNonBoolean();
        assertTrue("expected " + expectedString + " but was:" + resolved.toString(), expected.sameExpression(resolved));
    }
    
}
