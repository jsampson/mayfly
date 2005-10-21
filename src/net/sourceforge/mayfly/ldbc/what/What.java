package net.sourceforge.mayfly.ldbc.what;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.util.*;

import java.sql.*;
import java.util.*;

public class What extends Aggregate {


    private List masks = new ArrayList();

    public What() {
    }

    public What(List masks) {
        this.masks = masks;
    }


    protected Object createNew(Iterable items) {
        return new What(asList(items));
    }

    public Iterator iterator() {
        return masks.iterator();
    }

    public What add(WhatElement maskElement) {
        masks.add(maskElement);
        return this;
    }

    public List selectedColumns(TableData tableData) throws SQLException {
        List result = new ArrayList();
        for (Iterator iter = masks.iterator(); iter.hasNext();) {
            WhatElement element = (WhatElement) iter.next();
            String canonicalizedColumnName = tableData.findColumn(element.columnName());
            result.add(canonicalizedColumnName);
        }
        return result;
    }


}
