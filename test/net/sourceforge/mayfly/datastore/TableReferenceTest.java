package net.sourceforge.mayfly.datastore;

import junit.framework.TestCase;

import net.sourceforge.mayfly.evaluation.command.UnresolvedTableReference;

public class TableReferenceTest extends TestCase {
    
    public void testCanonicalizeTable() throws Exception {
        DataStore store = new DataStore(
            new Schema()
                .createTable("FOO", "a")
        );
        UnresolvedTableReference raw = new UnresolvedTableReference("Foo");
        assertEquals("Foo", raw.tableName());

        TableReference reference = raw.resolve(
            store, DataStore.ANONYMOUS_SCHEMA_NAME, "bar");
        assertEquals("FOO", reference.tableName());
        assertTrue(reference.matches(DataStore.ANONYMOUS_SCHEMA_NAME, "FOo"));
    }
    
    public void testAdditionalTable() throws Exception {
        DataStore store = new DataStore(new Schema());
        UnresolvedTableReference raw = new UnresolvedTableReference("Foo");
        assertEquals("Foo", raw.tableName());

        TableReference reference = raw.resolve(
            store, DataStore.ANONYMOUS_SCHEMA_NAME, "FOO");
        assertEquals("FOO", reference.tableName());
        assertTrue(reference.matches(DataStore.ANONYMOUS_SCHEMA_NAME, "FOo"));
    }

}
