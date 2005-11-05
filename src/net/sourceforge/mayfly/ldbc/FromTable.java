package net.sourceforge.mayfly.ldbc;

import org.ldbc.antlr.collections.*;

import java.sql.*;
import java.util.*;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.what.*;
import net.sourceforge.mayfly.util.*;

public class FromTable extends ValueObject implements FromElement {
    private String tableName;
    private String alias;

    public FromTable(String tableName) {
        this(tableName, null);
    }

    public FromTable(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
    }


    public static FromElement fromSeletedTableTree(Tree table) {
        AST firstIdentifier = table.getFirstChild();
        String tableName = firstIdentifier.getText();

        AST secondIdentifier = firstIdentifier.getNextSibling();

        FromElement fromElement;
        if (secondIdentifier==null) {
            fromElement = new FromTable(tableName);
        } else {
            String alias = secondIdentifier.getText();
            fromElement = new FromTable(tableName, alias);
        }
        return fromElement;
    }

    public Rows tableContents(DataStore store) throws SQLException {
        return applyAlias(store.table(tableName).rows());
    }

    public Rows dummyRows(DataStore store) throws SQLException {
        return applyAlias(store.table(tableName).dummyRows());
    }

    private Rows applyAlias(Rows storedRows) {
        if (alias == null) {
            return storedRows;
        } else {
            return applyAlias(alias, storedRows);
        }
    }

    private Rows applyAlias(String alias, Rows storedRows) {
        L rows = new L();
        for (Iterator iter = storedRows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            rows.add(applyAlias(alias, row));
        }
        return new Rows(rows.asImmutable());
    }

    private Row applyAlias(String alias, Row row) {
        M columnToCell = new M();
        for (Iterator iter = row.iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            Column column = (Column) entry.getKey();
            Cell cell = (Cell) entry.getValue();

            Column newColumn = new Column(alias, column.columnName());
            columnToCell.put(newColumn, cell);
        }
        return new Row(columnToCell.asImmutable());
    }

}