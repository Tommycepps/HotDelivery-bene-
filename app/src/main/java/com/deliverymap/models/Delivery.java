package com.deliverymap.models;

/**
 * Modello che rappresenta un delivery attivo sulla mappa.
 */
public class Delivery {
    public String id;
    public String cuoco_id;
    public String nome;
    public String descrizione;
    public double lat;
    public double lng;
    public boolean attivo;
    public String updated_at;

    /** Distanza calcolata localmente con Haversine, non salvata nel DB */
    public transient double distanzaKm;

    public Delivery() {}

    public Delivery(String cuocoId, String nome, String descrizione, double lat, double lng) {
        this.cuoco_id = cuocoId;
        this.nome = nome;
        this.descrizione = descrizione;
        this.lat = lat;
        this.lng = lng;
        this.attivo = true;
    }
}
