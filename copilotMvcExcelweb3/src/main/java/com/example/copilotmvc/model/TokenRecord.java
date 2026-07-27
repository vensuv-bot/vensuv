package com.example.copilotmvc.model;

public class TokenRecord {
    private long id;
    private String name;
    private String tokenValue;

    public TokenRecord() {}

    public TokenRecord(long id, String name, String tokenValue) {
        this.id = id;
        this.name = name;
        this.tokenValue = tokenValue;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
}
