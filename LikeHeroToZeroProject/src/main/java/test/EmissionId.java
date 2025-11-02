package test;

import java.io.Serializable;
import java.util.Objects;

public class EmissionId implements Serializable {
    private String isoCode;
    private short year;

    public EmissionId() {}
    public EmissionId(String isoCode, short year) {
        this.isoCode = isoCode;
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmissionId)) return false;
        EmissionId other = (EmissionId) o;
        return year == other.year && Objects.equals(isoCode, other.isoCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoCode, year);
    }
}