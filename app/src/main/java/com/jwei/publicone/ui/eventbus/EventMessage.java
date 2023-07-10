package com.jwei.publicone.ui.eventbus;

public class EventMessage {
    private String action;
    private Object obj;
    private int arg = -1;
    private Class cls;

    public EventMessage(String action, Object obj, int arg) {
        this.action = action;
        this.obj = obj;
        this.arg = arg;
    }

    public EventMessage(String action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    public EventMessage(String action, Object obj) {
        this.action = action;
        this.obj = obj;
    }

    public EventMessage(String action, Class cls) {
        this.action = action;
        this.cls = cls;
    }

    public EventMessage(String action) {
        this.action = action;
    }

    public EventMessage() {
    }

    public Class getCls() {
        return cls;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public int getArg() {
        return arg;
    }

    public void setArg(int arg) {
        this.arg = arg;
    }
}
