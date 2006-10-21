package net.sourceforge.mayfly.evaluation;

import net.sourceforge.mayfly.evaluation.what.Selected;

/**
 * Not yet immutable, because of {@link GroupByKeys}
 */
public interface Aggregator {

    public abstract ResultRows group(ResultRows rows, Selected selected);

    public abstract void check(ResultRow dummyRow, Selected selected);

}
