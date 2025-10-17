package com.glanz.idempotent.sceneEnum;

public enum LockEnum {
    REDIS("REDIS", "redis"),
    MYSQL("MYSQL", "mysql"),
    TOKEN("TOKEN", "token");

    private String key;
    private String value;


    LockEnum(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
