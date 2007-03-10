package net.sourceforge.mayfly.datastore;

import net.sourceforge.mayfly.util.ImmutableList;

import java.util.Iterator;

public class Rows {
    private final ImmutableList rows;

    public Rows(ImmutableList rows) {
        this.rows = rows;
    }

    public Rows() {
        this(new ImmutableList());
    }

    public Rows(Row row) {
        this(ImmutableList.singleton(row));
    }

    public Iterator iterator() {
        return rows.iterator();
    }
    
    public Row row(int index) {
        return (Row) rows.get(index);
    }

    public int rowCount() {
        return rows.size();
    }
    
    public Rows subList(int fromIndex, int toIndex) {
        return new Rows((ImmutableList) rows.subList(fromIndex, toIndex));
    }

    public Rows with(Row newRow) {
        return new Rows(rows.with(newRow));
    }

    public Rows addColumn(Column newColumn) {
        return addColumn(newColumn, Position.LAST);
    }
    
    public Rows addColumn(Column newColumn, Position position) {
        Rows result = new Rows();
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            result = result.with(row.addColumn(newColumn, position));
        }
        return result;
    }

    public Rows dropColumn(String column) {
        Rows result = new Rows();
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            result = result.with(row.dropColumn(column));
        }
        return result;
    }

}
