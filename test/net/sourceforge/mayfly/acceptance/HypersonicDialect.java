package net.sourceforge.mayfly.acceptance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class HypersonicDialect extends Dialect {

    public Connection openConnection() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        return openAdditionalConnection();
    }

    public Connection openAdditionalConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:hsqldb:mem:SqlTestCase");
    }

    public void shutdown(Connection connection) throws Exception {
        SqlTestCase.execute("SHUTDOWN", connection); // So next test gets a new database.
    }
    
    public boolean detectsAmbiguousColumns() {
        return false;
    }
    
    public boolean crossJoinRequiresOn() {
        // In hypersonic, CROSS JOIN seems to be a synonym for INNER JOIN
        return true;
    }
    
    public boolean crossJoinCanHaveOn() {
        return true;
    }
    
    public boolean onIsRestrictedToJoinsTables() {
        return false;
    }
    
    public boolean allowDuplicateTableInQuery() {
        return true;
    }
    
    public boolean rightHandArgumentToJoinCanBeJoin(boolean withParentheses) {
        return false;
    }
    
    public boolean authorizationRequiredInCreateSchema() {
        return true;
    }

    public boolean requiresAllParameters() {
        return false;
    }
    
    public boolean trailingSpacesConsultedInComparisons() {
        return true;
    }

    public boolean orderByCountsAsWhat() {
        return true;
    }
    
    public boolean canOrderByExpression() {
        return true;
    }
    
    public boolean aggregateAsteriskIsForCountOnly() {
        // Hypersonic has a variety of behaviors, depending on whether there
        // are any rows, and which function.  None of them seem very useful.
        return false;
    }
    
    public boolean allowCountDistinctStar() {
        // What count(distinct *) means I don't really know.
        return true;
    }
    
    public boolean canSumStrings(boolean rowsPresent) {
        if (rowsPresent) {
            return super.canSumStrings(rowsPresent);
        }
        else {
            return true;
        }
    }
    
    public boolean errorIfNotAggregateOrGroupedWhenGroupByExpression() {
        return false;
    }
    
    public boolean errorIfUpdateToAggregate(boolean rowsPresent) {
        return rowsPresent;
    }
    
    public boolean errorIfAggregateInWhere() {
        return false;
    }

    public boolean disallowColumnAndAggregateInExpression() {
        return false;
    }
    
    public boolean canHaveHavingWithoutGroupBy() {
        return true;
    }
    
    public boolean notRequiresBoolean() {
        return false;
    }
    
    public boolean canInsertNoValues() {
        return false;
    }

    public boolean disallowNullsInExpressions() {
        return false;
    }
    
    public boolean allowDateInTimestampColumn() {
        return true;
    }
    
    public boolean allowTimestampInDateColumn() {
        return true;
    }

    public boolean quotedIdentifiersAreCaseSensitive() {
        return true;
    }
    
    public boolean isReservedWord(String word) {
        return word.equalsIgnoreCase("if");
    }

    protected boolean constraintCanHaveForwardReference() {
        return false;
    }
    
    public boolean allowUniqueAsPartOfColumnDeclaration() {
        return false;
    }
    
    public boolean allowMultipleNullsInUniqueColumn() {
        return false;
    }
    
    public boolean haveUpdateDefault() {
        return false;
    }
    
    public boolean willReadUncommitted() {
        return true;
    }
    
    public boolean canProvideRepeatableRead() {
        return false;
    }
    
    public boolean autoCommitMustBeOffToCallRollback() {
        return false;
    }
    
    public boolean haveForUpdate() {
        return false;
    }
    
    public boolean foreignKeyCanReferToAnotherSchema() {
        return false;
    }
    
    public boolean haveTextType() {
        // VARCHAR or LONGVARCHAR (which are the same as
        // each other, I think) are the hypersonic equivalent.
        return false;
    }
    
    public String binaryTypeName() {
        return "BINARY";
    }
    
    public boolean allowHexForBinary() {
        return false;
    }
    
    public boolean canMixStringAndInteger() {
        return true;
    }

    public boolean decimalScaleIsFromType() {
        return false;
    }
    
    public boolean canUpdateToDefault() {
        return false;
    }

    public boolean canDropLastColumn() {
        return true;
    }
    
    public boolean haveDropForeignKey() {
        return false;
    }
    
    public boolean haveModifyColumn() {
        return false;
    }

    public boolean haveIdentity() {
        /* Sometimes this seems to start with 0.  Haven't fully tried to
           figure out what is going on there.  */
        return true;
    }
    
    public String identityType() {
        return "INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1)";
    }
    
    public boolean haveSql200xAutoIncrement() {
        return true;
    }
    
    public boolean autoIncrementIsRelativeToLastValue() {
        return true;
    }
    
    public boolean allowOrderByOnDelete() {
        /* The message is 
             Integrity constraint violation SYS_FK_46 table: FOO
           which makes me wonder whether the ORDER BY is simply
           being ignored.
           */
        return false;
    }

}
