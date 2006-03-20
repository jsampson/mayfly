package net.sourceforge.mayfly.ldbc.what;

import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.datastore.LongCell;
import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.evaluation.Expression;
import net.sourceforge.mayfly.ldbc.Rows;

public class CountAll extends Expression {

    private final String functionName;

    public CountAll(String functionName) {
        this.functionName = functionName;
    }

    public Cell evaluate(Row row) {
        /** This is just for checking; aggregation happens in {@link #aggregate(Rows)}. */
        return new LongCell(0);
    }

    public Cell findValue(int zeroBasedColumn, Row row) {
        return row.byPosition(zeroBasedColumn);
    }

    public String firstAggregate() {
        return displayName();
    }

    public String displayName() {
        return functionName + "(*)";
    }

    public Cell aggregate(Rows rows) {
        return new LongCell(rows.size());
    }
    
    public boolean sameExpression(Expression other) {
        return other instanceof CountAll;
    }

}
