package com.deliverymap.utils;

/**
 * Gestione della sessione utente in memoria (singleton).
 * Salva userId, nome e ruolo dopo il login.
 */
public class SessionManager {

    private static SessionManager instance;

    private String userId;
    private String nome;
    private String ruolo; // "cuoco" o "cliente"
    private boolean loggedIn = false;

    private SessionManager() {}

    /** Restituisce l'istanza singleton. */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Salva i dati dell'utente dopo il login riuscito. */
    public void login(String userId, String nome, String ruolo) {
        this.userId = userId;
        this.nome = nome;
        this.ruolo = ruolo;
        this.loggedIn = true;
    }

    /** Cancella la sessione (logout). */
    public void logout() {
        this.userId = null;
        this.nome = null;
        this.ruolo = null;
        this.loggedIn = false;
    }

    public boolean isLoggedIn() { return loggedIn; }
    public String getUserId()   { return userId; }
    public String getNome()     { return nome; }
    public String getRuolo()    { return ruolo; }

    /** Restituisce true se l'utente ha ruolo cuoco. */
    public boolean isCuoco() { return "cuoco".equals(ruolo); }
}
