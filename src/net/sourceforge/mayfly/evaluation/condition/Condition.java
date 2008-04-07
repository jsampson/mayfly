package net.sourceforge.mayfly.evaluation.condition;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.select.Evaluator;
import net.sourceforge.mayfly.parser.Location;

/**
 * @internal
 * The intention is that a condition should be immutable. Right now
 * the condition subclasses themselves don't always enforce this but
 * mutating the condition, after its initial construction, is not
 * allowed as it is in the data store. (The subclass in question is
 * {@link SubselectedIn}).
 */
public abstract class Condition {

    public static final Condition TRUE = new True();

    final public boolean evaluate(Row row, String table) {
        return evaluate(new ResultRow(row, table));
    }
    
    final public boolean evaluate(ResultRow row) {
        return evaluate(row, Evaluator.NO_SUBSELECT_NEEDED);
    }

    abstract public boolean evaluate(ResultRow row, Evaluator evaluator);
    
    abstract public Condition resolve(ResultRow row, Evaluator evaluator);

    abstract public String firstAggregate();

    public String firstAggregate(Condition left, Condition right) {
        String firstInLeft = left.firstAggregate();
        if (firstInLeft != null) {
            return firstInLeft;
        }
        return right.firstAggregate();
    }

    abstract public void check(ResultRow row);

    public void rejectAggregates(String context) {
        rejectAggregates(firstAggregate(), context);
    }

    public static void rejectAggregates(String firstAggregate, String context) {
        if (firstAggregate != null) {
            throw new MayflyException(
                "aggregate " + firstAggregate + " not valid in " + context);
        }
    }
    
    public final boolean isAggregate() {
        return firstAggregate() != null;
    }
    
    public Location location() {
        return Location.UNKNOWN;
    }

}
