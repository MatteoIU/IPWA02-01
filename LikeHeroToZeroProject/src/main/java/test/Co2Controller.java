package test;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

@Named("Co2Controller")
@ApplicationScoped
public class Co2Controller {

    private static final EntityManagerFactory EMF =
        Persistence.createEntityManagerFactory("LikeHeroToZeroPersistenceUnit");

    private List<String> countries;   // Dropdown
    private String selectedCountry;   // Auswahl
    
    private List<Integer> years = new ArrayList<>();
    private Integer selectedYear;   // aktuell im Dropdown gewähltes Jahr

    private Integer latestYear;       // neuestes (freigegebenes) Jahr
    private Double latestValueMt;     // CO₂ im neuesten (freigegebenen) Jahr

    @PostConstruct
    public void init() {
        try (EntityManager em = EMF.createEntityManager()) {
            countries = em.createQuery(
                "SELECT DISTINCT e.country FROM Emission e WHERE e.isoCode IS NOT NULL AND e.isoCode <> '' ORDER BY e.country",
                String.class
            ).getResultList();
        }
    }

    /** Nach Länderauswahl: ermittele (Year, Value) des neuesten FREIGEGEBENEN Datensatzes. */
    public void onCountryChange() {
        latestYear = null;
        latestValueMt = null;
        years = new ArrayList<>();
        selectedYear = null;

        if (selectedCountry == null || selectedCountry.isBlank()) return;

        try (EntityManager em = EMF.createEntityManager()) {
            // verfügbare Jahre für das Land (absteigend sortiert)
            List<Short> yShorts = em.createQuery(
                "SELECT DISTINCT e.year FROM Emission e WHERE e.country = :c ORDER BY e.year DESC",
                Short.class
            ).setParameter("c", selectedCountry).getResultList();

            for (Short s : yShorts) years.add(s.intValue());

            if (!years.isEmpty()) {
                selectedYear = years.get(0);     // neuester Jahrgang vorauswählen
                latestYear  = selectedYear;

                // ISO bestimmen
                String iso = em.createQuery(
                    "SELECT e.isoCode FROM Emission e WHERE e.country = :c AND e.isoCode IS NOT NULL AND e.isoCode <> '' ORDER BY e.year DESC",
                    String.class
                ).setParameter("c", selectedCountry).setMaxResults(1).getResultStream().findFirst().orElse(null);

                if (iso != null) {
                    Emission e = em.find(Emission.class, new EmissionId(iso, selectedYear.shortValue()));
                    latestValueMt = (e != null) ? e.getCo2Mt() : null;
                }
            }
        }
    }
    
    
    public void onYearChange() {
        latestValueMt = null;
        latestYear = selectedYear;

        if (selectedCountry == null || selectedCountry.isBlank() || selectedYear == null) return;

        try (EntityManager em = EMF.createEntityManager()) {
            String iso = em.createQuery(
                "SELECT e.isoCode FROM Emission e WHERE e.country = :c AND e.isoCode IS NOT NULL AND e.isoCode <> '' ORDER BY e.year DESC",
                String.class
            ).setParameter("c", selectedCountry).setMaxResults(1)
            .getResultStream().findFirst().orElse(null);

            if (iso == null) return;

            Emission e = em.find(Emission.class, new EmissionId(iso, selectedYear.shortValue()));
            latestValueMt = (e != null) ? e.getCo2Mt() : null;
        }
    }



    // --- Getter/Setter ---
    public List<String> getCountries() { return countries; }
    public String getSelectedCountry() { return selectedCountry; }
    public void setSelectedCountry(String selectedCountry) { this.selectedCountry = selectedCountry; }
    public Integer getLatestYear() { return latestYear; }
    public Double getLatestValueMt() { return latestValueMt; }
    
    public List<Integer> getYears() { return years; }
    public Integer getSelectedYear() { return selectedYear; }
    public void setSelectedYear(Integer selectedYear) { this.selectedYear = selectedYear; }
}










