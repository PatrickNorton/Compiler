package main.java.converter;

public final class ErrorCounter {
    private int warnings;
    private int errors;

    public ErrorCounter() {
        this.warnings = 0;
        this.errors = 0;
    }

    public int getWarnings() {
        return warnings;
    }

    public int getErrors() {
        return errors;
    }

    public void addWarning() {
        warnings++;
    }

    public void addError() {
        errors++;
    }
}
