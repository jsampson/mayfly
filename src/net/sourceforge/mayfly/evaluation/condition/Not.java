package net.sourceforge.mayfly.evaluation.condition;

import net.sourceforge.mayfly.evaluation.ResultRow;


public class Not extends BooleanExpression {

    public final BooleanExpression operand;

    public Not(BooleanExpression operand) {
        this.operand = operand;
    }

    public boolean evaluate(ResultRow row) {
        return !operand.evaluate(row);
    }

    public String firstAggregate() {
        return operand.firstAggregate();
    }

}