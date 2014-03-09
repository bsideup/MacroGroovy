package ru.trylogic.groovy.macro;

public class SubstitutionKey {

    private int startLine;
    private int startColumn;
    private int endLine;
    private int endColumn;

    public SubstitutionKey(int startLine, int startColumn, int endLine, int endColumn) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubstitutionKey that = (SubstitutionKey) o;

        if (endColumn != that.endColumn) return false;
        if (endLine != that.endLine) return false;
        if (startColumn != that.startColumn) return false;
        if (startLine != that.startLine) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startLine;
        result = 31 * result + startColumn;
        result = 31 * result + endLine;
        result = 31 * result + endColumn;
        return result;
    }

    @Override
    public String toString() {
        return "SubstitutionKey{" +
                "startLine=" + startLine +
                ", startColumn=" + startColumn +
                ", endLine=" + endLine +
                ", endColumn=" + endColumn +
                '}';
    }
}
