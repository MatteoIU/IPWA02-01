package test;

import java.util.Objects;

public class Benutzer {
    private String name;
    private String passwort;

    public Benutzer(String name, String passwort) {
        this.name = name;
        this.passwort = passwort;
    }
    public String getName() { return name; }
    public String getPasswort() { return passwort; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Benutzer)) return false;
        Benutzer b = (Benutzer) o;
        return Objects.equals(name, b.name) && Objects.equals(passwort, b.passwort);
    }
    @Override public int hashCode() { return Objects.hash(name, passwort); }
}
