package net.sourceforge.mayfly.datastore;

import net.sourceforge.mayfly.*;
import net.sourceforge.mayfly.ldbc.*;
import net.sourceforge.mayfly.ldbc.what.*;
import net.sourceforge.mayfly.util.*;

import java.util.*;

public class TableData {

    private final Columns columns;
    private final Rows rows;

    public TableData(Columns columns) {
        this(columns, new Rows());
    }
    
    private TableData(Columns columns, Rows rows) {
        columns.checkForDuplicates();
        this.columns = columns;
        this.rows = rows;
    }

    public TableData addRow(List columnNames, List values) {
        Columns specified = findColumns(columnNames);
        specified.checkForDuplicates();
        
        return addRow(specified, values);
    }

    public TableData addRow(List values) {
        return addRow(columns, values);
    }

    private TableData addRow(Columns columnsToInsert, List values) {
        if (columnsToInsert.size() != values.size()) {
            if (values.size() > columnsToInsert.size()) {
                throw new MayflyException("Too many values.\n" + describeNamesAndValues(columnsToInsert, values));
            } else {
                throw new MayflyException("Too few values.\n" + describeNamesAndValues(columnsToInsert, values));
            }
        }
        
        M specifiedColumnToValue = columnsToInsert.zipper(new L(values));
        
        TupleBuilder tuple = new TupleBuilder();
        Iterator iter = columns.iterator();
        while (iter.hasNext()) {
            Column column = (Column) iter.next();
            if (specifiedColumnToValue.containsKey(column)) {
                Cell cell = Cell.fromContents(specifiedColumnToValue.get(column));
                tuple.append(new TupleElement(column, cell));
            } else {
                tuple.append(new TupleElement(column, NullCell.INSTANCE));
            }
        }
        Row newRow = new Row(tuple);

        return new TableData(columns, (Rows) rows.with(newRow));
    }

    private String describeNamesAndValues(Columns columns, List values) {
        StringBuilder result = new StringBuilder();
        
        result.append("Columns and values were:\n");

        Iterator nameIterator = columns.iterator();
        Iterator valueIterator = values.iterator();
        while (nameIterator.hasNext() || valueIterator.hasNext()) {
            if (nameIterator.hasNext()) {
                Column column = (Column) nameIterator.next();
                result.append(column.columnName());
            } else {
                result.append("(none)");
            }

            result.append(' ');

            if (valueIterator.hasNext()) {
                Object value = valueIterator.next();
                result.append(value.toString());
            } else {
                result.append("(none)");
            }
            
            result.append('\n');
        }
        return result.toString();
    }

    private Columns findColumns(List columnNames) {
        L columnList =
            new L(columnNames)
                .collect(
                    new Transformer() {
                        public Object transform(Object from) {
                            return findColumn((String) from);
                        }
                    }
                );

        Columns specified = new Columns(columnList.asImmutable());
        return specified;
    }
    
    public Rows dummyRows() {

        TupleBuilder tuple = new TupleBuilder();
        for (int i = 0; i < columns.size(); ++i) {
            tuple.append(
                new TupleElement(
                    columns.get(i),
                    NullCell.INSTANCE
                )
            );
        }

        return new Rows(new Row(tuple));
    }

    public Column findColumn(String columnName) {
        return columns.columnFromName(columnName);
    }

    public List columnNames() {
        return columns.asNames();
    }
    
    public Columns columns() {
        return columns;
    }
    
    public int rowCount() {
        return rows.size();
    }

    public Rows rows() {
        return rows;
    }

    public boolean hasColumn(String target) {
        for (Iterator iter = columns.iterator(); iter.hasNext();) {
            Column column = (Column) iter.next();
            if (column.matchesName(target)) {
                return true;
            }
        }
        return false;
    }

}