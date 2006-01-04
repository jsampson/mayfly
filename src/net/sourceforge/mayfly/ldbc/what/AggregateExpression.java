package net.sourceforge.mayfly.ldbc.what;

import java.util.*;

import net.sourceforge.mayfly.*;
import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.*;
import net.sourceforge.mayfly.util.*;

public abstract class AggregateExpression extends WhatElement implements Transformer {

    private final SingleColumn column;
    private final String functionName;

    protected AggregateExpression(SingleColumn column, String functionName) {
        this.column = column;
        this.functionName = functionName;
    }

    public Cell evaluate(Row row) {
        /** This is just for checking; aggregation happens in {@link #aggregate(Rows)}. */
        return column.evaluate(row);
    }

    public Tuple process(Tuple originalTuple, M aliasToTableName) {
        throw new UnimplementedException();
    }

    public String firstAggregate() {
        return functionName + "(" + column.displayName() + ")";
    }

    public Cell aggregate(Rows rows) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long count = 0;
        long sum = 0;
        for (Iterator iter = rows.iterator(); iter.hasNext();) {
            Row row = (Row) iter.next();
            Cell cell = evaluate(row);
            if (!(cell instanceof NullCell)) {
                long value = cell.asLong();
                min = Math.min(min, value);
                max = Math.max(max, value);
                count++;
                sum += value;
            }
        }
        
        if (count > 0) {
            return pickOne(new LongCell(min), new LongCell(max),
                new LongCell(count), new LongCell(sum), new LongCell(sum / count));
        } else {
            return pickOne(NullCell.INSTANCE, NullCell.INSTANCE, 
                new LongCell(0), 
                
                // Lame (0 would be more convenient), but standard.
                // Is it possible/desirable for Mayfly to help?
                // (giving an error and pointing out a better way, or whatever).
                NullCell.INSTANCE,
                
                NullCell.INSTANCE);
        }
    }

    abstract protected Cell pickOne(Cell min, Cell max, Cell count, Cell sum, Cell average);
    
    public Object transform(Object from) {
        throw new UnimplementedException();
    }

}
