package net.sourceforge.mayfly.evaluation.from;

import junit.framework.TestCase;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.Options;
import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.datastore.Schema;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;
import net.sourceforge.mayfly.evaluation.select.StoreEvaluator;
import net.sourceforge.mayfly.util.ImmutableList;

public class FromTableTest extends TestCase {
    
    public void testCaseSensitiveTableName() throws Exception {
        Options options = new Options(true);
        DataStore store = new DataStore(
            new Schema().createTable("foo", ImmutableList.singleton("x")));
        try {
            new FromTable("Foo", "f")
                .dummyRow(
                    new StoreEvaluator(store, 
                        DataStore.ANONYMOUS_SCHEMA_NAME, options));
            fail();
        }
        catch (MayflyException e) {
            assertEquals("no table Foo", e.getMessage());
        }
    }

    public void testOptionsPassedToRows() throws Exception {
        Options options = new Options(true);
        DataStore store = new DataStore(
            new Schema().createTable("foo", ImmutableList.singleton("x")));
        ResultRow dummyRow = new FromTable("foo", "f")
            .dummyRow(
                new StoreEvaluator(store, 
                    DataStore.ANONYMOUS_SCHEMA_NAME, options));
        assertEquals(1, dummyRow.size());
        SingleColumn expression = (SingleColumn) dummyRow.expression(0);
        assertEquals("f", expression.tableOrAlias());
        assertTrue(expression.options.tableNamesCaseSensitive());
    }

}
