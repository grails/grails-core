package grails.util;

public class Pair<A, B> {
    final A aValue;
    final B bValue;

    public Pair(A aValue, B bValue) {
        this.aValue = aValue;
        this.bValue = bValue;
    }

    public A getaValue() {
        return aValue;
    }

    public B getbValue() {
        return bValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aValue == null) ? 0 : aValue.hashCode());
        result = prime * result + ((bValue == null) ? 0 : bValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pair other = (Pair)obj;
        if (aValue == null) {
            if (other.aValue != null)
                return false;
        }
        else if (!aValue.equals(other.aValue))
            return false;
        if (bValue == null) {
            if (other.bValue != null)
                return false;
        }
        else if (!bValue.equals(other.bValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TupleKey [aValue=" + aValue + ", bValue=" + bValue + "]";
    }
}
