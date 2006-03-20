package net.sourceforge.mayfly.evaluation.expression;

import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.ldbc.what.SingleColumn;

public class Sum extends AggregateExpression {

    public Sum(SingleColumn column, String functionName, boolean distinct) {
        super(column, functionName, distinct);
    }

    protected Cell pickOne(Cell min, Cell max, Cell count, Cell sum, Cell average) {
        return sum;
    }

}
