package com.teger.exception;

import java.sql.SQLException;

public class CloseDatabaseException extends Exception {
    public CloseDatabaseException(String message){
        super(message);
    }
}
