package test;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("AdminController")
@ViewScoped
public class AdminController implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("LikeHeroToZeroPersistenceUnit");

    // --- Auswahl ---
    private List<String> countries = new ArrayList<>();
    private String selectedCountry;

    // --- Felder des aktuell gewählten Datensatzes ---
    private String isoCode;
    private Integer year;       // editierbar
    private Double co2Mt;       // CO₂-Wert
    private boolean existingRecord;
    private Double proposedCo2Mt;

    // Freigabe-Status des aktuell geladenen Datensatzes.
    private Boolean approved;

    @Inject
    private LoginController login;

    // Pending-Liste für Editor
    public static class PendingRow implements Serializable {
        private final String isoCode;
        private final String country;
        private final int year;
        private final Double proposedCo2Mt;
        public PendingRow(String isoCode, String country, short year, Double proposedCo2Mt) {
            this.isoCode = isoCode;
            this.country = country;
            this.year = year;
            this.proposedCo2Mt = proposedCo2Mt;
        }
        public String getIsoCode() { return isoCode; }
        public String getCountry() { return country; }
        public int getYear() { return year; }
        public Double getProposedCo2Mt() { return proposedCo2Mt; }
    }
    private List<PendingRow> pending = List.of();

    @PostConstruct
    public void init() {
        try (EntityManager em = EMF.createEntityManager()) {
            countries = em.createQuery(
                "SELECT DISTINCT e.country FROM Emission e " +
                "WHERE e.isoCode IS NOT NULL AND e.isoCode <> '' " +
                "ORDER BY e.country",
                String.class
            ).getResultList();
        }
        // gleich mitladen (unschädlich für Nicht-Editoren, nur Editor sieht sie)
        reloadPending();
    }

    //lädt alle Zeilen mit Vorschlag (proposed_co2_mt) und nicht freigegeben. */
    public void reloadPending() {
        try (EntityManager em = EMF.createEntityManager()) {
            pending = em.createQuery(
                "SELECT new test.AdminController$PendingRow(e.isoCode, e.country, e.year, e.proposedCo2Mt) " +
                "FROM Emission e " +
                "WHERE e.proposedCo2Mt IS NOT NULL AND (e.approved = false OR e.approved IS NULL) " +
                "ORDER BY e.country, e.year",
                PendingRow.class
            ).getResultList();
        }
    }

    
    public void onCountryChange() {
        isoCode = null;
        year = null;
        co2Mt = null;
        existingRecord = false;
        approved = null;

        if (selectedCountry == null || selectedCountry.isBlank()) return;

        try (EntityManager em = EMF.createEntityManager()) {
            isoCode = em.createQuery(
                "SELECT e.isoCode FROM Emission e " +
                "WHERE e.country = :c AND e.isoCode IS NOT NULL AND e.isoCode <> '' " +
                "ORDER BY e.year DESC",
                String.class
            ).setParameter("c", selectedCountry)
             .setMaxResults(1)
             .getResultStream().findFirst().orElse(null);

            if (isoCode == null) return;

            Short y = em.createQuery(
                "SELECT MAX(e.year) FROM Emission e WHERE e.country = :c",
                Short.class
            ).setParameter("c", selectedCountry)
             .getSingleResult();

            if (y != null) {
                year = y.intValue();
                Emission e = em.find(Emission.class, new EmissionId(isoCode, y));
                if (e != null) {
                    co2Mt = e.getCo2Mt();
                    approved = e.isApproved();
                    existingRecord = true;
                    proposedCo2Mt = e.getProposedCo2Mt();
                } else {
                    co2Mt = null;
                    approved = null;
                    existingRecord = false;
                    proposedCo2Mt = null;
                }
            }
        }
    }

    public void onYearChange() {
        co2Mt = null;
        existingRecord = false;
        approved = null;

        if (selectedCountry == null || selectedCountry.isBlank() || isoCode == null || year == null) return;

        try (EntityManager em = EMF.createEntityManager()) {
            Short y = year.shortValue();
            Emission e = em.find(Emission.class, new EmissionId(isoCode, y));
            if (e != null) {
                co2Mt = e.getCo2Mt();
                proposedCo2Mt = e.getProposedCo2Mt();
                approved = e.isApproved();
                existingRecord = true;
            }else {
                proposedCo2Mt = null;
            }
        }
    }

    public void save() {
        if (login == null || !login.isLoggedIn()) return;
        if (selectedCountry == null || selectedCountry.isBlank()) return;
        if (isoCode == null || isoCode.isBlank()) return;
        if (year == null) return;

        EntityManager em = EMF.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            EmissionId id = new EmissionId(isoCode, year.shortValue());
            Emission e = em.find(Emission.class, id);

            if (e == null) {
                e = new Emission();
                e.setIsoCode(isoCode);
                e.setCountry(selectedCountry);
                e.setYear(year.shortValue());
                // abhängig von der Rolle speichern
                if (login.isApprover()) {
                    // Editor: direkt freigeben
                    e.setCo2Mt(co2Mt);
                    e.setProposedCo2Mt(null);
                    e.setApproved(true);
                } else {
                    // Benutzer: nur Vorschlag
                    e.setCo2Mt(null);                 // (oder ein Default; wichtig ist: nicht überschreiben)
                    e.setProposedCo2Mt(co2Mt);
                    e.setApproved(false);
                }
                em.persist(e);
                
                
                
                
            } else {
                if (login.isApprover()) {
                    // Editor überschreibt den freigegebenen Wert
                    e.setCo2Mt(co2Mt);
                    e.setProposedCo2Mt(null);
                    e.setApproved(true);
                } else {
                    // Benutzer: schreibt NUR in proposed_co2_mt
                    e.setProposedCo2Mt(co2Mt);
                    e.setApproved(false);
                    
                }
                em.merge(e);     
            }
            tx.commit();
            
            //Nach dem Commit Datensatz frisch laden
            e = em.find(Emission.class, id);
            existingRecord = true;
            
            if (login.isApprover()) {
            approved = login.isApprover(); // Anzeige rechts
            proposedCo2Mt = null;
            co2Mt = e.getCo2Mt();
            } else {
            	approved = false;
                proposedCo2Mt = e.getProposedCo2Mt();  // <-- das ist der gerade gespeicherte Vorschlag
                // der freigegebene Wert bleibt separat in co2Mt (kann null sein)
                co2Mt = e.getCo2Mt();
            }
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }

        // Sidebar ggf. neu laden
        reloadPending();
    }


    public void deleteSelected() {
        if (login == null || !login.isLoggedIn()) return;
        if (isoCode == null || isoCode.isBlank() || year == null) return;

        EntityManager em = EMF.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            EmissionId id = new EmissionId(isoCode, year.shortValue());
            Emission entity = em.find(Emission.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
        //Werte zurücksetzen
        existingRecord = false;
        co2Mt = null;
        year = null;
        approved = null;
        reloadPending();
        proposedCo2Mt = null;
    }

    public void approveSelected() {
        if (isoCode == null || year == null) return;

        EntityManager em = EMF.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Emission e = em.find(Emission.class, new EmissionId(isoCode, year.shortValue()));
            if (e != null && e.getProposedCo2Mt() != null) {
                e.setCo2Mt(e.getProposedCo2Mt());
                e.setProposedCo2Mt(null);
                e.setApproved(true);
                em.merge(e);
                co2Mt = e.getCo2Mt();         // Formular rechts aktualisieren
                approved = true;
            }
            tx.commit();
            proposedCo2Mt = null;
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
        reloadPending();
    }


    // Getter/Setter 
    public List<String> getCountries() { return countries; }
    public String getSelectedCountry() { return selectedCountry; }
    public void setSelectedCountry(String selectedCountry) { this.selectedCountry = selectedCountry; }

    public String getIsoCode() { return isoCode; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Double getCo2Mt() { return co2Mt; }
    public void setCo2Mt(Double co2Mt) { this.co2Mt = co2Mt; }
    public boolean isExistingRecord() { return existingRecord; }
    public Boolean getApproved() { return approved; }
    public Double getProposedCo2Mt() { return proposedCo2Mt; }
    public void setProposedCo2Mt(Double v) { this.proposedCo2Mt = v; }

    //für die Sidebar
    public List<PendingRow> getPending() { return pending; }
}









