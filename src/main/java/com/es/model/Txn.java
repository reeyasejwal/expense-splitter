package com.es.model;

public class Txn {
    public final String from;
    public final String to;
    public final double amount;

    public Txn(String from, String to, double amount) {
        this.from   = from;
        this.to     = to;
        this.amount = amount;
    }
}
