package net.sourceforge.mayfly.datastore;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.MayflyInternalException;
import net.sourceforge.mayfly.datastore.constraint.Constraint;
import net.sourceforge.mayfly.datastore.constraint.Constraints;
import net.sourceforge.mayfly.evaluation.Checker;
import net.sourceforge.mayfly.evaluation.Value;
import net.sourceforge.mayfly.evaluation.ValueList;
import net.sourceforge.mayfly.evaluation.command.SetClause;
import net.sourceforge.mayfly.evaluation.command.UpdateTable;
import net.sourceforge.mayfly.evaluation.condition.Condition;
import net.sourceforge.mayfly.parser.Location;
import net.sourceforge.mayfly.util.ImmutableList;
import net.sourceforge.mayfly.util.L;
import net.sourceforge.mayfly.util.M;
import net.sourceforge.mayfly.util.StringBuilder;

import java.util.Iterator;
import java.util.List;

public class TableData {

    private final Columns columns;
    private final Rows rows;
    public final Constraints constraints;
    public final Indexes indexes;

    public TableData(Columns columns, Constraints constraints,
        ImmutableList indexes) {
        this(columns, constraints, new Rows(), new Indexes(indexes));
    }
    
    TableData(Columns columns, Constraints constraints, Rows rows,
        Indexes indexes) {
        this.constraints = constraints;
        if (constraints == null) {
            throw new NullPointerException("constraints is required");
        }
        columns.checkForDuplicates();
        this.columns = columns;
        this.rows = rows;
        this.indexes = indexes;
    }

    public TableData addRow(Checker checker, List columnNames, ValueList values) {
        Columns specified = findColumns(columnNames);
        specified.checkForDuplicates();
        
        return addRow(checker, specified, values);
    }

    public TableData addRow(Checker checker, ValueList values) {
        return addRow(checker, columns, values);
    }

    private TableData addRow(Checker checker,
        Columns columnsToInsert, ValueList values) {
        if (columnsToInsert.columnCount() != values.size()) {
            if (values.size() > columnsToInsert.columnCount()) {
                throw makeException("Too many values.\n", columnsToInsert, values);
            } else {
                throw makeException("Too few values.\n", columnsToInsert, values);
            }
        }
        
        M specifiedColumnToValue = columnsToInsert.zipper(values);
        Columns newColumns = columns;
        
        TupleBuilder tuple = new TupleBuilder();
        Iterator iter = columns.iterator();
        while (iter.hasNext()) {
            Column column = (Column) iter.next();
            newColumns = setColumn(
                specifiedColumnToValue, newColumns, tuple, column, 
                values.location, checker);
        }
        Row newRow = new Row(tuple);
        constraints.check(rows, newRow, values.location);
        checker.checkInsert(constraints, newRow);

        return new TableData(
            newColumns, constraints, rows.with(newRow), indexes);
    }

    private MayflyException makeException(String message, Columns columnsToInsert, ValueList values) {
        return new MayflyException(
            message + describeNamesAndValues(columnsToInsert, values.asCells()),
            values.location);
    }

    private Columns setColumn(M specifiedColumnToValue, Columns newColumns, 
        TupleBuilder tuple, Column column, Location location, Checker checker) {
        boolean isDefault;
        Cell cell;
        if (specifiedColumnToValue.containsKey(column)) {
            Value value = (Value) specifiedColumnToValue.get(column);
            isDefault = value.value == null;
            cell = column.coerce(
                isDefault ? column.defaultValue() : value.value, 
                value.location);
        } else {
            isDefault = true;
            cell = column.coerce(column.defaultValue(), location);
        }
        
        tuple.append(new TupleElement(column, cell));

        Column newColumn = column.afterAutoIncrement(checker, cell, isDefault);
        if (newColumn != null) {
            newColumns = newColumns.replace(newColumn);
        }

        return newColumns;
    }

    public UpdateTable update(Checker checker, List setClauses, 
        Condition where, String tableName) {
        checker.evaluate(where, dummyRow(), tableName);

        Rows newRows = new Rows();
        int rowsAffected = 0;
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            
            if (checker.evaluate(where, row, tableName)) {
                Row newRow = newRow(setClauses, row, tableName);
                constraints.check(newRows, newRow, Location.UNKNOWN);
                checker.checkInsert(constraints, newRow);
                checker.checkDelete(row, newRow);

                newRows = newRows.with(newRow);
                ++rowsAffected;
            }
            else {
                constraints.check(newRows, row, Location.UNKNOWN);
                newRows = newRows.with(row);
            }

        }
        TableData newTable = new TableData(
            columns, constraints, newRows, indexes);
        return new UpdateTable(newTable, rowsAffected);
    }

    private Row newRow(List setClauses, Row row, String table) {
        TupleMapper mapper = new TupleMapper(row);
        for (Iterator iterator = setClauses.iterator(); iterator.hasNext();) {
            SetClause setClause = (SetClause) iterator.next();
            Column column = setClause.column(columns);
            mapper.put(column, setClause.value(row, table, column));
        }
        setOnUpdateColumns(mapper);
        Row newRow = mapper.asRow();
        return newRow;
    }

    private void setOnUpdateColumns(TupleMapper mapper) {
        for (Iterator iter = columns.iterator(); iter.hasNext();) {
            Column column = (Column) iter.next();
            if (column.hasOnUpdateValue()) {
                mapper.put(column, column.getOnUpdateValue());
            }
        }
    }

    public UpdateTable delete(Condition where, Checker checker, String tableName) {
        checker.evaluate(where, dummyRow(), tableName);
        
        Rows newRows = new Rows();
        int rowsAffected = 0;
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            
            if (checker.evaluate(where, row, tableName)) {
                ++rowsAffected;
                checker.checkDelete(row, null);
            }
            else {
                newRows = newRows.with(row);
            }

        }
        TableData newTable = new TableData(
            columns, constraints, newRows, indexes);
        return new UpdateTable(newTable, rowsAffected);
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

    public Columns findColumns(List columnNames) {
        L columnList = new L();
        for (Iterator iter = columnNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            columnList.add(findColumn(name));
        }

        Columns specified = new Columns(columnList.asImmutable());
        return specified;
    }
    
    public Row dummyRow() {

        TupleBuilder tuple = new TupleBuilder();
        for (int i = 0; i < columns.columnCount(); ++i) {
            tuple.append(
                new TupleElement(
                    columns.column(i),
                    NullCell.INSTANCE
                )
            );
        }

        return new Row(tuple);
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
        return rows.rowCount();
    }

    public Row row(int index) {
        return rows.row(index);
    }

    public Rows rows() {
        return rows;
    }

    public boolean hasValue(String column, Cell value) {
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            if (row.cell(column).sqlEquals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNull(Column column) {
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            if (row.cell(column.columnName()) instanceof NullCell) {
                return true;
            }
        }
        return false;
    }

    public DataStore checkDelete(
        DataStore store,
        String schema, String table, Row rowToDelete, Row replacementRow) {
        return constraints.checkDelete(store, schema, table, 
            rowToDelete, replacementRow);
    }

    public void checkDropTable(DataStore store, String schema, String table) {
        constraints.checkDropTable(store, schema, table);
    }

    public TableData addColumn(Column newColumn, Position position) {
        if (columns.hasColumn(newColumn.columnName())) {
            throw new MayflyException(
                "column " + newColumn.columnName() + " already exists");
        }
        return new TableData(
            columns.with(newColumn, position), 
            constraints,
            rows.addColumn(newColumn),
            indexes
        );
    }

    public TableData dropColumn(TableReference table, String column) {
        // TODO: what if column is mentioned in indexes?
        return new TableData(
            columns.without(column),
            constraints.dropColumn(table, column),
            rows.dropColumn(column),
            indexes
        );
    }

    /** 
     * @internal
     * Check for whether this table has any foreign keys which
     * reference the other column specified by the parameters.
     * If so, throw an exception.
     */
    public void checkDropColumn(TableReference table, String column) {
        constraints.checkDropColumn(table, column);
    }

    public TableData modifyColumn(Column newColumn) {
        Column oldColumn = columns.columnFromName(newColumn.columnName());
        if (!oldColumn.isNotNull && newColumn.isNotNull) {
            // Check the constraint that we're adding
            if (hasNull(oldColumn)) {
                throw new MayflyException(
                    "cannot make column " + newColumn.columnName() + 
                    " NOT NULL because it contains null values");
            }
        }
        
        if (!oldColumn.isAutoIncrement() && newColumn.isAutoIncrement()) {
            if (rows.rowCount() > 0) {
                newColumn = 
                    newColumn.withIncrementedDefault(
                        highest(newColumn.columnName()));
            }
        }

        return new TableData(
            columns.replace(newColumn),
            constraints,
            rows,
            indexes
        );
    }

    public TableData renameColumn(String oldName, String newName) {
        Column oldColumn = columns.columnFromName(oldName);
        Column newColumn = oldColumn.withName(newName);
        return new TableData(
            columns.replace(oldName, newColumn),
            constraints.renameColumn(oldName, newName),
            rows.renameColumn(oldName, newName),
            indexes.renameColumn(oldName, newName)
        );
    }

    Cell highest(String column) {
        Iterator iter = rows.iterator(); 
        if (!iter.hasNext()) {
            throw new MayflyInternalException("no rows");
        }
        Row firstRow = (Row) iter.next();
        Cell best = firstRow.cell(column);
        while (iter.hasNext()) {
            Row row = (Row) iter.next();
            Cell value = row.cell(column);
            if (value.compareTo(best) > 0) {
                best = value;
            }
        }
        return best;
    }

    public TableData dropForeignKey(String constraintName) {
        return new TableData(
            columns,
            constraints.dropForeignKey(constraintName),
            rows,
            indexes
        );
    }

    public TableData addConstraint(Constraint key) {
        return new TableData(
            columns,
            constraints.addConstraint(key),
            rows,
            indexes
        );
    }

    public TableData addIndex(Index index) {
        return new TableData(
            columns,
            constraints,
            rows,
            indexes.with(index));
    }

    public boolean canBeTargetOfForeignKey(String targetColumn) {
        return constraints.canBeTargetOfForeignKey(targetColumn);
    }

    public boolean hasPrimaryKey() {
        return constraints.hasPrimaryKey();
    }

}
