package com.flowboard.board.exception;
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String msg) { super(msg); }
}