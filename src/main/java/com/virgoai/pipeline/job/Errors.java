package com.virgoai.pipeline.job;

final class Errors {

    private Errors() {
    }

    static Throwable sqlCauseOrSelf(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.sql.SQLException) {
                return t;
            }
        }
        return e;
    }

    static String describe(Throwable e) {
        Throwable cause = sqlCauseOrSelf(e);
        String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        message = message.replaceAll("\\s+", " ");
        return message.length() > 900 ? message.substring(0, 900) : message;
    }
}
