package net.sourceforge.mayfly.evaluation.what;

import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.evaluation.Expression;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.ResultRows;
import net.sourceforge.mayfly.evaluation.select.Evaluator;

/**
 * @internal
 * expression AS alias
 * 
 * (as in, for example, SELECT a + b AS total FROM foo).
 */
public class AliasedExpression extends Expression {

    private final String alias;
    private final Expression expression;

    public AliasedExpression(String aliasedColumn, Expression expression) {
        this.alias = aliasedColumn;
        this.expression = expression;
    }

    public String displayName() {
        return expression.displayName() + " AS " + alias;
    }
    
    public String firstAggregate() {
        return expression.firstAggregate();
    }
    
    public String firstColumn() {
        return expression.firstColumn();
    }

    public Cell aggregate(ResultRows rows) {
        return expression.aggregate(rows);
    }

    public Cell evaluate(ResultRow row, Evaluator evaluator) {
        return expression.evaluate(row, evaluator);
    }

    public boolean sameExpression(Expression other) {
        if (!(other instanceof AliasedExpression)) {
            return false;
        }

        AliasedExpression aliasedOther = (AliasedExpression) other;
        if (!alias.equalsIgnoreCase(aliasedOther.alias)) {
            return false;
        }
        if (!expression.sameExpression(aliasedOther.expression)) {
            return false;
        }
        return true;
    }
    
    public boolean matches(String columnName) {
        return alias.equalsIgnoreCase(columnName);
    }
    
    @Override
    public Expression lookupAlias(String name) {
        return matches(name) ? expression : null;
    }

}
