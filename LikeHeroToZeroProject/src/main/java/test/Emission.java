package test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@IdClass(EmissionId.class)
@Table(name = "emission")
public class Emission implements Serializable {

    @Id
    @Column(name = "iso_code", length = 10, nullable = false)
    private String isoCode;

    @Id
    @Column(name = "year", nullable = false)
    private short year;

    @Column(name = "country", length = 191, nullable = false)
    private String country;

    // Spaltenname co2_mt, nicht co2
    @Column(name = "co2_mt")
    private Double co2Mt;

    // Falls du die Vorschlags-Logik verwendest, diese Spalte behalten
    @Column(name = "proposed_co2_mt")
    private Double proposedCo2Mt;

    // Freigabe-Flag (0/1 in MySQL)
    @Column(name = "approved", nullable = false)
    private boolean approved;

    public Emission() {}

    public Emission(String isoCode, short year) {
        this.isoCode = isoCode;
        this.year = year;
    }

    // Getter/Setter

    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }

    public short getYear() { return year; }
    public void setYear(short year) { this.year = year; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getCo2Mt() { return co2Mt; }
    public void setCo2Mt(Double co2Mt) { this.co2Mt = co2Mt; }

    public Double getProposedCo2Mt() { return proposedCo2Mt; }
    public void setProposedCo2Mt(Double proposedCo2Mt) { this.proposedCo2Mt = proposedCo2Mt; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Emission)) return false;
        Emission that = (Emission) o;
        return year == that.year &&
               Objects.equals(isoCode, that.isoCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoCode, year);
    }
}

