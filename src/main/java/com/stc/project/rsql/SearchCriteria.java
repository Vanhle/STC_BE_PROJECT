package com.stc.project.rsql;

/**
 * Created by huyvv
 * Date: 4/6/2025
 * Time: 2:37 PM
 * for all issues, contact me: huyvv@vnpt-technology.vn
 **/
public class SearchCriteria {
    private String key;
    private String operation;
    private Object value;

    public SearchCriteria(String key, String operation, Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getOperation() {
        return operation;
    }

    public Object getValue() {
        return value;
    }
}
