package net.sourceforge.mayfly.ldbc;

import java.util.*;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.where.*;
import net.sourceforge.mayfly.util.*;

public class LeftJoin extends Join implements FromElement {

    protected LeftJoin(FromElement left, FromElement right, Where condition) {
        super(left, right, condition);
    }

    public Rows tableContents(DataStore store) {
        Rows leftRows = left.tableContents(store);
        Rows rightRows = right.tableContents(store);

        final L joinResult = new L();

        Iterator leftIter = leftRows.iterator();
        while (leftIter.hasNext()) {
            Row leftRow = (Row) leftIter.next();
            boolean haveJoinedThisLeftRow = false;
            
            Iterator rightIter = rightRows.iterator();
            while (rightIter.hasNext()) {
                Row rightRow = (Row) rightIter.next();

                Row combined = (Row) leftRow.plus(rightRow);
                
                if (condition.evaluate(combined)) {
                    joinResult.append(combined);
                    haveJoinedThisLeftRow = true;
                }
            }
            
            if (!haveJoinedThisLeftRow) {
                Row nullRightRow = (Row) right.dummyRows(store).element(0);
                Row withNulls = (Row) leftRow.plus(nullRightRow);
                joinResult.append(withNulls);
            }
        }
        return new Rows(joinResult.asImmutable());
    }

}
