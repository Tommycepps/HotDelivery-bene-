package com.deliverymap.models;

/**
 * Modello che rappresenta un utente (cuoco o cliente).
 */
public class User {
    public String id;
    public String nome;
    public String email;
    public String password_hash;
    public String ruolo; // "cuoco" o "cliente"
    public String created_at;

    public User() {}

    public User(String nome, String email, String passwordHash, String ruolo) {
        this.nome = nome;
        this.email = email;
        this.password_hash = passwordHash;
        this.ruolo = ruolo;
    }
}
