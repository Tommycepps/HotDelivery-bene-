package com.deliverymap.utils;

/**
 * Costanti di configurazione del progetto.
 * IMPORTANTE: sostituisci SUPABASE_URL e SUPABASE_ANON_KEY con i tuoi valori!
 * Vedi SETUP.md per le istruzioni dettagliate.
 */
public class Constants {

    // ─── Supabase ───────────────────────────────────────────────────────────
    public static final String SUPABASE_URL      = "https://<PROJECT_ID>.supabase.co";
    public static final String SUPABASE_ANON_KEY = "<ANON_KEY>";

    // ─── Polling ────────────────────────────────────────────────────────────
    /** Intervallo di aggiornamento posizione in millisecondi (30 secondi). */
    public static final long POLLING_INTERVAL_MS = 30_000L;

    // ─── Mappa ──────────────────────────────────────────────────────────────
    public static final double DEFAULT_LAT  = 44.4949; // Bologna
    public static final double DEFAULT_LNG  = 11.3426;
    public static final double DEFAULT_ZOOM = 13.0;

    /** Stile MapLibre gratuito basato su OpenStreetMap. */
    public static final String MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json";
}
