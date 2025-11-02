package test;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("LoginController")
@SessionScoped
public class LoginController implements Serializable {
    private static final long serialVersionUID = 1L;

    //HIER den einzigen Freigabe-Account festlegen
    private static final String APPROVER_USERNAME = "editor";

    // Erlaubte Logins 
    private final List<Benutzer> erlaubte = List.of(
        new Benutzer("benutzer", "passwort"),  // darf NUR ändern/speichern
        new Benutzer("editor",   "review")     // darf zusätzlich FREIGEBEN
    );

    private String benutzername;
    private String passwort;
    private boolean loggedIn;

    public String login() {
        if (erlaubte.contains(new Benutzer(benutzername, passwort))) {
            loggedIn = true;
            return "backend?faces-redirect=true";
        }
        return null;
    }

    public String logout() {
        loggedIn = false;
        benutzername = null;
        passwort = null;
        return "index?faces-redirect=true";
    }

    // ---- Rollenabfrage: nur EIN Username ist Freigebende:r
    public boolean isApprover() {
        return APPROVER_USERNAME.equals(benutzername);
    }

    // ---- Getter/Setter
    public boolean isLoggedIn() { return loggedIn; }
    public String getBenutzername() { return benutzername; }
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public String getPasswort() { return passwort; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
}

