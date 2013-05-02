package grails.util;

public class Triple<A, B, C> {
    final A aValue;
    final B bValue;
    final C cValue;

    public Triple(A aValue, B bValue, C cValue) {
        this.aValue = aValue;
        this.bValue = bValue;
        this.cValue = cValue;
    }

    public A getaValue() {
        return aValue;
    }

    public B getbValue() {
        return bValue;
    }

    public C getcValue() {
        return cValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aValue == null) ? 0 : aValue.hashCode());
        result = prime * result + ((bValue == null) ? 0 : bValue.hashCode());
        result = prime * result + ((cValue == null) ? 0 : cValue.hashCode());
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
        Triple other = (Triple)obj;
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
        if (cValue == null) {
            if (other.cValue != null)
                return false;
        }
        else if (!cValue.equals(other.cValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Triple [aValue=" + aValue + ", bValue=" + bValue + ", cValue=" + cValue + "]";
    }
}
