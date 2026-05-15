package com.deliverymap.utils;

/**
 * Costanti di configurazione del progetto.
 */
public class Constants {

    // ─── Supabase ───────────────────────────────────────────────────────────
    public static final String SUPABASE_URL      = "https://qjeoppuibcwflmawgfvk.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFqZW9wcHVpYmN3ZmxtYXdnZnZrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg2MzczMTQsImV4cCI6MjA5NDIxMzMxNH0.AGIgN3-KhpvtWfBDCIzY-p7R1N6zNXebOzIzXil41xs";

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
