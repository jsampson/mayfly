package net.sourceforge.mayfly.dump;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.MayflyInternalException;
import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.datastore.Column;
import net.sourceforge.mayfly.datastore.Columns;
import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.datastore.Index;
import net.sourceforge.mayfly.datastore.Indexes;
import net.sourceforge.mayfly.datastore.LongCell;
import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.datastore.TableData;
import net.sourceforge.mayfly.datastore.constraint.Constraint;
import net.sourceforge.mayfly.datastore.constraint.Constraints;
import net.sourceforge.mayfly.evaluation.select.Evaluator;
import net.sourceforge.mayfly.evaluation.select.StoreEvaluator;
import net.sourceforge.mayfly.graph.CycleDetectedException;
import net.sourceforge.mayfly.graph.Graph;
import net.sourceforge.mayfly.parser.Lexer;
import net.sourceforge.mayfly.parser.TokenType;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The SQL dumper is able to dump a database as an SQL script which can 
 * be run to create the database.  The script will order the statements
 * so that foreign key constraints, for example, will be satisfied in
 * reloading.  However, it is not yet able to handle circular foreign
 * keys (in which neither row or table can be inserted first, but
 * something fancier with UPDATE or ALTER TABLE is needed).
 */
public class SqlDumper {
    
    private final boolean dumpSequenceValues;

    /**
     * Construct a dumper with default settings.  In particular, the
     * dumper will be designed to produce dumps which re-create the
     * state of the database as of the dump, rather than just part of it.
     */
    public SqlDumper() {
        this(true);
    }

    /**
     * Construct a dumper specifying the settings.
     * @param dumpSequenceValues
     *      Should the dumper dump the next value for auto-increment
     *      columns and SQL 2003 identity columns?  The default is
     *      true, which means that the dump will restore the full
     *      state of the database if reloaded. Set this to false
     *      if you want to compare two dumps and consider
     *      a difference in sequence values unimportant.
     */
    public SqlDumper(boolean dumpSequenceValues) {
        this.dumpSequenceValues = dumpSequenceValues;
    }

    /**
     * Convenience method, which returns a string rather than writing
     * to a writer.  Otherwise the same as {@link #dump(DataStore, Writer)}.
     */
    public String dump(DataStore store) {
        StringWriter out = new StringWriter();
        try {
            dump(store, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    /**
     * Dump both the schema and the data of store to out.
     */
    public void dump(DataStore store, Writer out) throws IOException {
        List sortedTables = sortTables(store);

        definition(store, sortedTables, out);

        data(store, sortedTables, out);
    }

    /**
     * Dump only the data (not the schema) of store to out.
     */
    public void data(DataStore store, Writer out) 
    throws IOException {
        List sortedTables = sortTables(store);
        data(store, sortedTables, out);
    }

    /**
     * Dump only the schema (not the data) of store to out.
     */
    public void definition(DataStore store, Writer out) 
    throws IOException {
        List sortedTables = sortTables(store);
        definition(store, sortedTables, out);
    }

    private void definition(DataStore store, List sortedTables, Writer out) 
    throws IOException {
        for (Iterator iter = sortedTables.iterator(); iter.hasNext();) {
            String tableName = (String) iter.next();
            createTable(tableName, store.table(tableName), out);
        }
    }

    private List sortTables(final DataStore store) {
        Set tableNames = store.anonymousSchema().tables();
        List tableNodes = namesToNodes(tableNames);
        Graph graph = new Graph();
        graph.addNodes(tableNodes);
        addEdgesForForeignKeys(graph, tableNodes, store);
        List sortedNodes = topologicalSortOnTables(graph);
        return nodesToNames(sortedNodes);
    }

    private List topologicalSortOnTables(Graph graph) {
        try {
            return graph.topologicalSort();
        }
        catch (CycleDetectedException e) {
            throw new MayflyException(
                "cannot dump: circular foreign key references between tables");
        }
    }

    private void addEdgesForForeignKeys(Graph graph, List tableNodes,
        DataStore store) {
        Evaluator evaluator = new StoreEvaluator(
            store, DataStore.ANONYMOUS_SCHEMA_NAME);
        for (Iterator iter = tableNodes.iterator(); iter.hasNext();) {
            TableNode referringTable = (TableNode) iter.next();
            List referenced = store.table(referringTable.name).constraints
                .referencedTables(evaluator);
            for (Iterator iterator = referenced.iterator(); iterator.hasNext();) {
                String referencedTable = (String) iterator.next();
                TableNode referencedNode = 
                    findInList(tableNodes, referencedTable);
                graph.addEdge(referencedNode, referringTable);
            }
        }
    }
    
    TableNode findInList(List nodes, String name) {
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            TableNode candidate = (TableNode) iter.next();
            if (candidate.name.equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        throw new MayflyInternalException("should have added " + name);
    }

    private List namesToNodes(Collection tableNames) {
        List result = new ArrayList();
        for (Iterator iter = tableNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            result.add(new TableNode(name));
        }
        return result;
    }

    private List nodesToNames(Collection tableNodes) {
        List result = new ArrayList();
        for (Iterator iter = tableNodes.iterator(); iter.hasNext();) {
            TableNode node = (TableNode) iter.next();
            result.add(node.name);
        }
        return result;
    }

    private void createTable(String tableName, TableData table, Writer out) 
    throws IOException {
        out.write("CREATE TABLE ");
        identifier(tableName, out);
        out.write("(\n");
        columns(table, out);
        constraints(table.constraints, out);
        out.write(");\n");
        indexes(tableName, table.indexes, out);
        out.write("\n");
    }

    public static void identifier(String text, Writer out) throws IOException {
        if (TokenType.lookupKeyword(text) != null) {
            out.write("\"");
            out.write(text);
            out.write("\"");
        }
        else if (looksLikeIdentifier(text)) {
            out.write(text);
        }
        else {
            out.write("\"");
            out.write(text);
            out.write("\"");
        }
    }

    private static boolean looksLikeIdentifier(String text) {
        if (text.indexOf('\"') != -1) {
            throw new MayflyException(
                "don't know how to dump identifier containing a double quote"
            );
        }
        if (text.length() == 0) {
            throw new MayflyInternalException(
                "shouldn't have empty string as identifier");
        }
        if (!Lexer.isIdentifierStart(text.charAt(0))) {
            return false;
        }
        for (int i = 1; i < text.length(); ++i) {
            if (!Lexer.isIdentifierCharacter(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void columns(TableData data, Writer out) throws IOException {
        for (Iterator iter = data.columns().iterator(); iter.hasNext();) {
            Column column = (Column) iter.next();
            column(column, out);
            if (iter.hasNext() || data.constraints.constraintCount() > 0) {
                out.write(",");
            }
            out.write("\n");
        }
    }

    private void column(Column column, Writer out) throws IOException {
        out.write("  ");
        identifier(column.columnName(), out);
        out.write(" ");
        out.write(column.type.dumpName());
        if (column.hasDefault() && !column.isSequence() && dumpSequenceValues) {
            out.write(" DEFAULT ");
            out.write(column.defaultValueAsSql());
        }
        
        if (column.hasOnUpdateValue()) {
            out.write(" ON UPDATE ");
            out.write(column.onUpdateValueAsSql());
        }
        
        if (column.isAutoIncrement()) {
            out.write(" AUTO_INCREMENT");
        }
        else if (column.isSequence()) {
            out.write(" GENERATED BY DEFAULT AS IDENTITY");
            Cell startWith = column.defaultValue();
            if (!startWith.sqlEquals(new LongCell(1)) && dumpSequenceValues) {
                out.write("(START WITH ");
                out.write(column.defaultValueAsSql());
                out.write(")");
            }
        }

        if (column.isNotNull) {
            out.write(" NOT NULL");
        }
    }

    private void constraints(Constraints constraints, Writer out) 
    throws IOException {
        for (int i = 0; i < constraints.constraintCount(); ++i) {
            Constraint constraint = constraints.constraint(i);
            out.write("  ");
            if (constraint.constraintName != null) {
                out.write("CONSTRAINT ");
                out.write(constraint.constraintName);
                out.write(" ");
            }
            constraint.dump(out);
            if (i < constraints.constraintCount() - 1) {
                out.write(",");
            }
            out.write("\n");
        }
    }

    private void indexes(String tableName, Indexes indexes, Writer out) 
    throws IOException {
        for (Index index : indexes) {
            out.write("CREATE ");
            if (index.unique) {
                out.write("UNIQUE ");
            }
            out.write("INDEX ");

            if (index.hasName()) {
                identifier(index.name(), out);
            }
            else {
                out.write("an_index");
            }
            out.write(" ON ");
            identifier(tableName, out);
            out.write("(");
            index.columns.dump(out);
            out.write(");\n");
        }
    }

    public void data(DataStore store, List sortedTables, Writer out) 
    throws IOException {
        for (Iterator iter = sortedTables.iterator(); iter.hasNext();) {
            String tableName = (String) iter.next();
            rows(tableName, store.table(tableName), out);
        }
    }

    private void rows(String tableName, TableData table, Writer out) 
    throws IOException {
        Collection rows = sortRows(table, tableName);
        Columns columns = table.columns();
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            row(tableName, columns, row, out);
        }
        
        if (rows.size() > 0) {
            out.write("\n");
        }
    }

    private Collection sortRows(final TableData table, String tableName) {
        List rowNodes = rowNodes(table, tableName);
        Graph graph = new Graph();
        graph.addNodes(rowNodes);
        addEdgesForRows(graph, rowNodes, table);
        List sortedNodes = topologicalSortOnRows(graph, tableName);
        return nodesToRows(sortedNodes);
    }

    private void addEdgesForRows(Graph graph, List rowNodes, TableData table) {
        for (Iterator i = rowNodes.iterator(); i.hasNext();) {
            RowNode left = (RowNode) i.next();
            for (Iterator j = rowNodes.iterator(); j.hasNext();) {
                RowNode right = (RowNode) j.next();
                if (table.constraints.mustInsertBefore(left.row, right.row)) {
                    graph.addEdge(left, right);
                }
            }
        }
    }

    private List rowNodes(final TableData table, String tableName) {
        Columns columns = table.columns();

        List result = new ArrayList();
        for (int i = 0; i < table.rowCount(); ++i) {
            Row row = table.row(i);
            result.add(new RowNode(row, tableName, columns));
        }
        return result;
    }
    
    private List topologicalSortOnRows(Graph graph, String tableName) {
        try {
            return graph.topologicalSort();
        }
        catch (CycleDetectedException e) {
            throw new MayflyException(
                "cannot dump: circular reference between rows in table " + 
                tableName);
        }
    }

    private List nodesToRows(Collection rowNodes) {
        List result = new ArrayList();
        for (Iterator iter = rowNodes.iterator(); iter.hasNext();) {
            RowNode node = (RowNode) iter.next();
            result.add(node.row);
        }
        return result;
    }

    private void row(String tableName, Columns columns, Row row, Writer out) throws IOException {
        out.write("INSERT INTO ");
        out.write(tableName);
        out.write("(");
        for (int i = 0; i < columns.columnCount(); ++i) {
            out.write(columns.columnName(i));
            if (i < columns.columnCount() - 1) {
                out.write(", ");
            }
        }
        out.write(") VALUES(");
        for (int i = 0; i < columns.columnCount(); ++i) {
            out.write(row.cell(columns.columnName(i)).asSql());
            if (i < columns.columnCount() - 1) {
                out.write(", ");
            }
        }
        out.write(");\n");
    }

}
