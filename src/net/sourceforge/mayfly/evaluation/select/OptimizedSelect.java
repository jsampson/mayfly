package net.sourceforge.mayfly.evaluation.select;

import net.sourceforge.mayfly.MayflyResultSet;
import net.sourceforge.mayfly.evaluation.Aggregator;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.ResultRows;
import net.sourceforge.mayfly.evaluation.condition.Condition;
import net.sourceforge.mayfly.evaluation.from.FromElement;
import net.sourceforge.mayfly.evaluation.what.Selected;
import net.sourceforge.mayfly.evaluation.what.What;

public class OptimizedSelect {

    private final Evaluator evaluator;
    public final Selected selected;
    private final ResultRow dummyRow;

    public final FromElement from;
    final Condition where;
    private final Aggregator groupBy;
    private final Distinct distinct;
    private final OrderBy orderBy;
    private final What what;
    private final Limit limit;

    public OptimizedSelect(
        Evaluator evaluator, Selected selected, ResultRow dummyRow,
        FromElement from, Condition where, Aggregator groupBy, Distinct distinct, 
        OrderBy orderBy, What what, Limit limit) {
        this.evaluator = evaluator;
        this.selected = selected;
        this.dummyRow = dummyRow;
        this.from = from;
        this.where = where;
        this.groupBy = groupBy;
        this.distinct = distinct;
        this.orderBy = orderBy;
        this.what = what;
        this.limit = limit;
    }

    ResultRows query() {
        ResultRows joinedRows = from.tableContents(evaluator);

        ResultRows afterWhere = joinedRows.select(where, evaluator);
        
        ResultRows afterGrouping = groupBy.group(afterWhere, evaluator, selected);

        ResultRows afterDistinct = distinct.distinct(selected, afterGrouping);

        ResultRows sorted = orderBy.sort(afterDistinct, what, evaluator);
        return limit.limit(sorted);
    }

    public MayflyResultSet asResultSet() {
        return new MayflyResultSet(selected, query());
    }
    
    public MayflyResultSet dummyResultSet() {
        return new MayflyResultSet(selected, new ResultRows(dummyRow));
    }

}
