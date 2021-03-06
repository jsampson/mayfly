package net.sourceforge.mayfly.datastore;

import junit.framework.TestCase;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.types.FakeDataType;
import net.sourceforge.mayfly.evaluation.expression.DefaultValue;
import net.sourceforge.mayfly.util.ImmutableList;
import net.sourceforge.mayfly.util.L;

import java.util.Arrays;

public class ColumnsTest extends TestCase {

    public void testFromColumnNames() throws Exception {
        Columns columns = Columns.fromColumnNames(
                new L()
                    .append("a")
                    .append("b")
            );
        assertEquals(2, columns.columnCount());
        assertEquals("a", columns.column(0).columnName());
        assertEquals("b", columns.column(1).columnName());
    }

    public void testLookup() throws Exception {
        Columns columns = new Columns(new ImmutableList(Arrays.asList(
            new Column[] {
                new Column("a"),
                new Column("b"),
                new Column("d")
            })));
        
        assertEquals("b", columns.columnFromName("b").columnName());
        assertEquals("b", columns.columnFromName("B").columnName() );

        try {
            columns.columnFromName("c");
            fail();
        } catch (MayflyException e) {
            assertEquals("no column c", e.getMessage());
        }

        assertEquals("d", columns.columnFromName("d").columnName());
    }
    
    public void testReplace() throws Exception {
        Columns columns = new Columns(new ImmutableList(Arrays.asList(
            new Column[] {
                new Column("a"),
                new Column("b"),
                new Column("c")
            })));
        Columns newColumns = columns.replace(
            new Column("b", DefaultValue.NOT_SPECIFIED, null, true, false,
                new FakeDataType(), false)
        );
        
        assertFalse(columns.columnFromName("b").isSequenceOrAutoIncrement());
        assertTrue(newColumns.columnFromName("b").isSequenceOrAutoIncrement());
        
        assertEquals(Arrays.asList(new String[] { "a", "b", "c" }), 
            newColumns.asNames());
    }
    
    public void testAddLast() throws Exception {
        Columns columns = Columns.fromColumnNames(ImmutableList.singleton("a"));
        Columns newColumns = columns.with(new Column("b"));
        assertEquals(Arrays.asList(new String[] { "a", "b" }),
            newColumns.asNames());
    }

    public void testAddFirst() throws Exception {
        Columns columns = Columns.fromColumnNames(ImmutableList.singleton("b"));
        Columns newColumns = columns.with(new Column("a"), Position.FIRST);
        assertEquals(Arrays.asList(new String[] { "a", "b" }),
            newColumns.asNames());
    }
    
    public void testAddAfter() throws Exception {
        Columns columns = new Columns(new ImmutableList(Arrays.asList(
            new Column[] {
                new Column("a"),
                new Column("c"),
            })));
        Columns newColumns = columns.with(new Column("b"), Position.after("a"));
        assertEquals(Arrays.asList(new String[] { "a", "b", "c" }),
            newColumns.asNames());
    }

    public void testAddAfterNotFound() throws Exception {
        Columns columns = new Columns(new ImmutableList(Arrays.asList(
            new Column[] {
                new Column("a"),
                new Column("c"),
            })));
        try {
            columns.with(new Column("b"), Position.after("b"));
            fail();
        }
        catch (MayflyException e) {
            assertEquals("no column b", e.getMessage());
        }
    }

    public void testAddDuplicate() throws Exception {
        Columns columns = new Columns(new ImmutableList(Arrays.asList(
            new Column[] {
                new Column("a"),
                new Column("b"),
                new Column("c")
            })));
        
        try {
            columns.with(new Column("b"));
            fail();
        }
        catch (MayflyException e) {
            assertEquals("duplicate column b", e.getMessage());
        }
    }
    
}
