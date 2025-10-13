package com.glanz.idempotent.exception;

/**
 * @author zz
 * 幂等异常
 */
public class IdempotentException extends RuntimeException {
    public IdempotentException(String msg) { super(msg); }
}
