package com.deliverymap.api;

import android.util.Log;

import com.deliverymap.models.Delivery;
import com.deliverymap.models.User;
import com.deliverymap.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client singleton per le chiamate REST a Supabase.
 * Tutte le chiamate sono sincrone — usale sempre su un thread in background.
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    private final OkHttpClient http;
    private final Gson gson;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private SupabaseClient() {
        this.http = new OkHttpClient();
        this.gson = new Gson();
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper: costruisce una Request con gli header Supabase obbligatori
    // ─────────────────────────────────────────────────────────────────────────────

    private Request.Builder baseRequest(String endpoint) {
        return new Request.Builder()
                .url(Constants.SUPABASE_URL + "/rest/v1/" + endpoint)
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation");
    }

    /**
     * Encoda una stringa per l'uso sicuro in un URL query parameter.
     * Converte '@' -> '%40', '+' -> '%20', ecc.
     */
    private String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 è sempre supportato su Android — non accade mai
            return value;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Cerca un utente per email.
     * IMPORTANTE: la email DEVE essere URL-encoded prima di inserirla nella query.
     * Senza encoding, la '@' spezza l'URL e Supabase restituisce un errore 400.
     *
     * @return User se trovato, null altrimenti
     * @throws IOException in caso di errore di rete o risposta HTTP non 2xx
     */
    public User getUserByEmail(String email) throws IOException {
        String emailEncoded = encodeParam(email);
        Request request = baseRequest("users?email=eq." + emailEncoded + "&select=*")
                .get().build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "(nessun body)";
                Log.e(TAG, "getUserByEmail HTTP " + response.code() + ": " + errorBody);
                throw new IOException("Errore server (" + response.code() + "): " + errorBody);
            }
            if (response.body() == null) return null;
            String body = response.body().string();
            Type listType = new TypeToken<List<User>>() {}.getType();
            List<User> users = gson.fromJson(body, listType);
            return (users != null && !users.isEmpty()) ? users.get(0) : null;
        }
    }

    /**
     * Registra un nuovo utente. Restituisce l'utente con id assegnato da Supabase.
     *
     * @throws IOException se la richiesta fallisce o l'email è già in uso
     */
    public User createUser(User user) throws IOException {
        String json = gson.toJson(user);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = baseRequest("users").post(body).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "(nessun body)";
                Log.e(TAG, "createUser HTTP " + response.code() + ": " + errorBody);
                // 409 Conflict = email già registrata
                if (response.code() == 409) {
                    throw new IOException("Email già registrata");
                }
                throw new IOException("Errore registrazione (" + response.code() + ")");
            }
            if (response.body() == null) return null;
            String responseBody = response.body().string();
            Type listType = new TypeToken<List<User>>() {}.getType();
            List<User> users = gson.fromJson(responseBody, listType);
            return (users != null && !users.isEmpty()) ? users.get(0) : null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELIVERIES
    // ─────────────────────────────────────────────────────────────────────────────

    public List<Delivery> getDeliveryAttivi() throws IOException {
        Request request = baseRequest("deliveries?attivo=eq.true&select=*")
                .get().build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return new ArrayList<>();
            String body = response.body().string();
            Type listType = new TypeToken<List<Delivery>>() {}.getType();
            List<Delivery> list = gson.fromJson(body, listType);
            return list != null ? list : new ArrayList<>();
        }
    }

    public Delivery getDeliveryByCuoco(String cuocoId) throws IOException {
        Request request = baseRequest("deliveries?cuoco_id=eq." + cuocoId + "&select=*")
                .get().build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String body = response.body().string();
            Type listType = new TypeToken<List<Delivery>>() {}.getType();
            List<Delivery> list = gson.fromJson(body, listType);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
    }

    public Delivery createDelivery(Delivery delivery) throws IOException {
        String json = gson.toJson(delivery);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = baseRequest("deliveries").post(body).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String responseBody = response.body().string();
            Type listType = new TypeToken<List<Delivery>>() {}.getType();
            List<Delivery> list = gson.fromJson(responseBody, listType);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
    }

    public boolean updateDeliveryPosizione(String cuocoId, double lat, double lng, boolean attivo)
            throws IOException {
        String json = String.format(
                "{\"lat\": %f, \"lng\": %f, \"attivo\": %b}",
                lat, lng, attivo
        );
        RequestBody body = RequestBody.create(json, JSON);
        Request request = baseRequest("deliveries?cuoco_id=eq." + cuocoId)
                .patch(body).build();
        try (Response response = http.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean setDeliveryInattivo(String cuocoId) throws IOException {
        String json = "{\"attivo\": false}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = baseRequest("deliveries?cuoco_id=eq." + cuocoId)
                .patch(body).build();
        try (Response response = http.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }
}
