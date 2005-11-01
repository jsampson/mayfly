package net.sourceforge.mayfly.ldbc;

import net.sourceforge.mayfly.*;
import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.what.*;
import net.sourceforge.mayfly.ldbc.where.*;
import net.sourceforge.mayfly.util.*;

import org.apache.commons.collections.*;
import org.ldbc.parser.*;

import java.sql.*;
import java.util.*;

public class Select extends ValueObject {

    public static Select fromTree(Tree selectTree) {

        int[] typesToIgnore = new int[]{SQLTokenTypes.COMMA};

        L converted =
            selectTree.children().convertUsing(TreeConverters.forSelectTree(), typesToIgnore);

        Where where =
            converted.selectObjectsThatAre(Where.class).size() > 0 ?
                    (Where) converted.selectObjectThatIs(Where.class) :
                    Where.EMPTY;

        return
            new Select(
                new What(converted.selectObjectsThatAre(WhatElement.class)),
                new From(converted.selectObjectsThatAre(FromElement.class)),
                where
            );
    }


    private What what;
    private From from;
    private Where where;

    public Select(What what, From from, Where where) {
        this.what = what;
        this.from = from;
        this.where = where;
    }

    public From from() {
        return from;
    }

    public ResultSet select(final DataStore store) throws SQLException {
        try {
            Columns columns = what.selectedColumns();
            checkColumns(store, columns);
            return new MyResultSet(columns, executeOn(store));
        } catch (MayflyException e) {
            // This wrapping isn't really desirable.  It can be avoided by
            // having a MayflyException create an SQLException inside it.
            throw (SQLException)new SQLException(e.getMessage()).initCause(e);
        }
    }

    private void checkColumns(final DataStore store, Columns columns) throws SQLException {
        L tableNames = from.tableNames();

        Iterator iterator = tableNames.iterator();
        String firstTable = (String) iterator.next();
        Rows joinedRows = new Rows(store.table(firstTable).dummyRow());
        while (iterator.hasNext()) {
            String tableName = (String) iterator.next();
            joinedRows = (Rows)joinedRows.cartesianJoin(new Rows(store.table(tableName).dummyRow()));
        }

        if (joinedRows.size() != 1) {
            throw new RuntimeException("internal error: got " + joinedRows.size());
        }
        Row row = (Row) joinedRows.element(0);
        for (Iterator iter = columns.iterator(); iter.hasNext();) {
            Column column = (Column) iter.next();
            row.cell(column);
        }

        //new Rows(row).select(where);
    }

    public Rows executeOn(DataStore store) throws SQLException {
        L tableNames = from.tableNames();

        Iterator iterator = tableNames.iterator();
        String firstTable = (String) iterator.next();
        Rows joinedRows = store.table(firstTable).rows();
        while (iterator.hasNext()) {
            String tableName = (String) iterator.next();
            joinedRows = (Rows)joinedRows.cartesianJoin(store.table(tableName).rows());
        }

        return (Rows) joinedRows.select(where);
    }
}
