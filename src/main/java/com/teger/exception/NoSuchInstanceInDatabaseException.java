package com.teger.exception;

public class NoSuchInstanceInDatabaseException extends Exception{
    public NoSuchInstanceInDatabaseException(String message){
        super(message);
    }
}
