package com.flowboard.comment.exception;
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String msg) { super(msg); }
}