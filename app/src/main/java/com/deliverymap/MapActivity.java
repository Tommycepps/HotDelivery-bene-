package com.deliverymap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.deliverymap.api.SupabaseClient;
import com.deliverymap.models.Delivery;
import com.deliverymap.utils.Constants;
import com.deliverymap.utils.LocationUtils;
import com.deliverymap.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.plugins.annotation.Symbol;
import org.maplibre.android.plugins.annotation.SymbolManager;
import org.maplibre.android.plugins.annotation.SymbolOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Schermata principale con la mappa MapLibre.
 * Vista differenziata per Cuoco (condivide posizione) e Cliente (vede i delivery).
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int    REQUEST_LOCATION = 100;
    private static final String ICON_SCOOTER     = "icon-scooter";
    private static final String ICON_CHEF        = "icon-chef";

    // Mappa e posizione
    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private SymbolManager symbolManager;
    private FusedLocationProviderClient locationClient;
    private Location miaPosizioneAttuale;

    // UI
    private Button btnDisponibile, btnPausa, btnAggiorna;
    private TextView tvNomeUtente, tvEmptyState;
    private FloatingActionButton fabCentraMappa;

    // Stato cuoco
    private boolean cuocoAttivo = false;

    // Handler per il polling automatico ogni 30 secondi
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;

    // Lista dei marker attualmente sulla mappa
    private final List<Symbol> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MapLibre DEVE essere inizializzato prima di setContentView
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map);

        inizializzaUI();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        richiedPermessiPosizione();
    }

    // ─── Inizializzazione UI ───────────────────────────────────────────────────────

    private void inizializzaUI() {
        mapView        = findViewById(R.id.map_view);
        btnDisponibile = findViewById(R.id.btn_disponibile);
        btnPausa       = findViewById(R.id.btn_pausa);
        btnAggiorna    = findViewById(R.id.btn_aggiorna);
        tvNomeUtente   = findViewById(R.id.tv_nome_utente);
        tvEmptyState   = findViewById(R.id.tv_empty_state);
        fabCentraMappa = findViewById(R.id.fab_centra_mappa);

        SessionManager session = SessionManager.getInstance();
        tvNomeUtente.setText(session.getNome() + " · " + session.getRuolo());

        // Mostra bottoni in base al ruolo
        if (session.isCuoco()) {
            btnDisponibile.setVisibility(View.VISIBLE);
            btnPausa.setVisibility(View.GONE);
            btnAggiorna.setVisibility(View.GONE);
        } else {
            btnDisponibile.setVisibility(View.GONE);
            btnPausa.setVisibility(View.GONE);
            btnAggiorna.setVisibility(View.VISIBLE);
        }

        btnDisponibile.setOnClickListener(v -> attivaDisponibilita());
        btnPausa.setOnClickListener(v -> mettereInPausa());
        btnAggiorna.setOnClickListener(v -> aggiornaDelivery());
        fabCentraMappa.setOnClickListener(v -> centraSullaMiaPosizione());
        findViewById(R.id.btn_logout).setOnClickListener(v -> eseguiLogout());

        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    // ─── MapLibre: mappa pronta ──────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        this.mapLibreMap = map;
        map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(Constants.DEFAULT_LAT, Constants.DEFAULT_LNG))
                .zoom(Constants.DEFAULT_ZOOM)
                .build());

        // Carica stile OpenStreetMap gratuito
        map.setStyle(new Style.Builder().fromUri(Constants.MAP_STYLE_URL), style -> {
            // Aggiunge le icone personalizzate alla mappa
            style.addImage(ICON_SCOOTER,
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_scooter));
            style.addImage(ICON_CHEF,
                    BitmapFactory.decodeResource(getResources(), R.drawable.ic_chef));

            symbolManager = new SymbolManager(mapView, mapLibreMap, style);
            symbolManager.setIconAllowOverlap(true);

            // Click su un marker: mostra BottomSheet con i dettagli
            symbolManager.addClickListener(symbol -> {
                mostraBottomSheetDelivery(symbol);
                return true;
            });

            caricaDelivery();
        });
    }

    // ─── Posizione GPS ───────────────────────────────────────────────────────────

    private void richiedPermessiPosizione() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            ottieniPosizioneAttuale();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ottieniPosizioneAttuale();
        } else {
            mostraSnackbar("Permesso posizione negato. Funzionalità limitate.");
        }
    }

    /** Recupera l'ultima posizione nota del dispositivo. */
    private void ottieniPosizioneAttuale() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                miaPosizioneAttuale = location;
                if (mapLibreMap != null) {
                    mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()),
                            Constants.DEFAULT_ZOOM));
                }
            }
        });
    }

    /** Centra la mappa sulla posizione attuale dell'utente. */
    private void centraSullaMiaPosizione() {
        if (miaPosizioneAttuale != null && mapLibreMap != null) {
            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(miaPosizioneAttuale.getLatitude(),
                            miaPosizioneAttuale.getLongitude()),
                    Constants.DEFAULT_ZOOM));
        } else {
            mostraSnackbar("Posizione non disponibile");
        }
    }

    // ─── Carica e mostra delivery sulla mappa ─────────────────────────────────────

    /** Recupera i delivery attivi da Supabase e aggiorna i marker. */
    private void caricaDelivery() {
        new Thread(() -> {
            try {
                List<Delivery> deliveries = SupabaseClient.getInstance().getDeliveryAttivi();

                // Calcola distanza Haversine per ogni delivery
                if (miaPosizioneAttuale != null) {
                    for (Delivery d : deliveries) {
                        d.distanzaKm = LocationUtils.haversine(
                                miaPosizioneAttuale.getLatitude(),
                                miaPosizioneAttuale.getLongitude(),
                                d.lat, d.lng);
                    }
                    // Ordina per distanza crescente (più vicino prima)
                    deliveries.sort((a, b) -> Double.compare(a.distanzaKm, b.distanzaKm));
                }

                final List<Delivery> finalList = deliveries;
                runOnUiThread(() -> aggiornaMarkerSullaMappa(finalList));

            } catch (Exception e) {
                runOnUiThread(() -> mostraSnackbar("Offline – riprovo tra poco"));
            }
        }).start();
    }

    /** Rimuove i vecchi marker e inserisce quelli aggiornati. */
    private void aggiornaMarkerSullaMappa(List<Delivery> deliveries) {
        if (symbolManager == null) return;

        symbolManager.delete(markers);
        markers.clear();

        if (deliveries.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyState.setVisibility(View.GONE);

        String mioId = SessionManager.getInstance().getUserId();
        boolean ioSonoCuoco = SessionManager.getInstance().isCuoco();

        for (Delivery d : deliveries) {
            boolean isMio = ioSonoCuoco && d.cuoco_id != null && d.cuoco_id.equals(mioId);
            String icona = isMio ? ICON_CHEF : ICON_SCOOTER;

            Symbol symbol = symbolManager.create(new SymbolOptions()
                    .withLatLng(new LatLng(d.lat, d.lng))
                    .withIconImage(icona)
                    .withIconSize(1.2f)
                    .withTextField(d.nome)
                    .withTextOffset(new Float[]{0f, 1.8f})
                    .withTextSize(11f)
                    // Salva l'id del delivery come dato del simbolo per il click
                    .withData(new com.google.gson.JsonPrimitive(d.id))
            );
            markers.add(symbol);
        }
    }

    // ─── Vista CUOCO ────────────────────────────────────────────────────────────────

    /** Attiva la disponibilità: crea o aggiorna il delivery con la posizione attuale. */
    private void attivaDisponibilita() {
        if (miaPosizioneAttuale == null) {
            mostraSnackbar("Posizione non ancora disponibile, riprova tra qualche secondo");
            return;
        }
        String cuocoId = SessionManager.getInstance().getUserId();
        double lat = miaPosizioneAttuale.getLatitude();
        double lng = miaPosizioneAttuale.getLongitude();

        new Thread(() -> {
            try {
                Delivery esistente = SupabaseClient.getInstance().getDeliveryByCuoco(cuocoId);
                if (esistente == null) {
                    // Prima volta: crea un nuovo record
                    Delivery nuovo = new Delivery(cuocoId,
                            SessionManager.getInstance().getNome(),
                            "Delivery disponibile", lat, lng);
                    SupabaseClient.getInstance().createDelivery(nuovo);
                } else {
                    // Già esistente: aggiorna posizione e attivo = true
                    SupabaseClient.getInstance().updateDeliveryPosizione(cuocoId, lat, lng, true);
                }
                runOnUiThread(() -> {
                    cuocoAttivo = true;
                    btnDisponibile.setVisibility(View.GONE);
                    btnPausa.setVisibility(View.VISIBLE);
                    mostraSnackbar("Sei ora visibile ai clienti!");
                    avviaPolling();
                    caricaDelivery();
                });
            } catch (Exception e) {
                runOnUiThread(() -> mostraSnackbar("Errore: " + e.getMessage()));
            }
        }).start();
    }

    /** Mette il cuoco in pausa: imposta attivo = false. */
    private void mettereInPausa() {
        new Thread(() -> {
            try {
                SupabaseClient.getInstance()
                        .setDeliveryInattivo(SessionManager.getInstance().getUserId());
                runOnUiThread(() -> {
                    cuocoAttivo = false;
                    btnPausa.setVisibility(View.GONE);
                    btnDisponibile.setVisibility(View.VISIBLE);
                    mostraSnackbar("Sei ora in pausa");
                    fermaPolling();
                    caricaDelivery();
                });
            } catch (Exception e) {
                runOnUiThread(() -> mostraSnackbar("Errore: " + e.getMessage()));
            }
        }).start();
    }

    // ─── Vista CLIENTE ───────────────────────────────────────────────────────────────

    private void aggiornaDelivery() {
        mostraSnackbar("Aggiornamento in corso...");
        caricaDelivery();
    }

    // ─── Polling automatico (solo cuoco attivo) ──────────────────────────────────

    /** Avvia il polling: aggiorna la posizione del cuoco ogni 30 secondi. */
    private void avviaPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (cuocoAttivo && miaPosizioneAttuale != null) {
                    String cuocoId = SessionManager.getInstance().getUserId();
                    double lat = miaPosizioneAttuale.getLatitude();
                    double lng = miaPosizioneAttuale.getLongitude();
                    new Thread(() -> {
                        try {
                            SupabaseClient.getInstance()
                                    .updateDeliveryPosizione(cuocoId, lat, lng, true);
                            runOnUiThread(() -> mostraSnackbar("Posizione aggiornata"));
                        } catch (Exception ignored) {}
                    }).start();
                }
                pollingHandler.postDelayed(this, Constants.POLLING_INTERVAL_MS);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, Constants.POLLING_INTERVAL_MS);
    }

    /** Ferma il polling quando il cuoco è in pausa o esce dall'app. */
    private void fermaPolling() {
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    // ─── BottomSheet dettagli delivery ──────────────────────────────────────────

    /** Mostra le informazioni del delivery selezionato in un BottomSheet Material. */
    private void mostraBottomSheetDelivery(Symbol symbol) {
        String deliveryId = (symbol.getData() != null)
                ? symbol.getData().getAsString() : "";

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_delivery, null);

        TextView tvNome        = view.findViewById(R.id.tv_delivery_nome);
        TextView tvDescrizione = view.findViewById(R.id.tv_delivery_descrizione);
        TextView tvDistanza    = view.findViewById(R.id.tv_delivery_distanza);
        TextView tvAggiornato  = view.findViewById(R.id.tv_delivery_aggiornato);

        new Thread(() -> {
            try {
                List<Delivery> lista = SupabaseClient.getInstance().getDeliveryAttivi();
                Delivery trovato = null;
                for (Delivery d : lista) {
                    if (deliveryId.equals(d.id)) { trovato = d; break; }
                }
                final Delivery delivery = trovato;
                runOnUiThread(() -> {
                    if (delivery != null) {
                        tvNome.setText(delivery.nome);
                        tvDescrizione.setText(delivery.descrizione != null
                                ? delivery.descrizione : "Nessuna descrizione");
                        if (miaPosizioneAttuale != null) {
                            double dist = LocationUtils.haversine(
                                    miaPosizioneAttuale.getLatitude(),
                                    miaPosizioneAttuale.getLongitude(),
                                    delivery.lat, delivery.lng);
                            tvDistanza.setText("Distanza: " + LocationUtils.formatDistanza(dist));
                        } else {
                            tvDistanza.setText("Distanza: N/D");
                        }
                        tvAggiornato.setText("Aggiornato: " + delivery.updated_at);
                    }
                });
            } catch (Exception ignored) {}
        }).start();

        dialog.setContentView(view);
        dialog.show();
    }

    // ─── Logout ─────────────────────────────────────────────────────────────────────

    private void eseguiLogout() {
        fermaPolling();
        if (cuocoAttivo) {
            new Thread(() -> {
                try { SupabaseClient.getInstance()
                        .setDeliveryInattivo(SessionManager.getInstance().getUserId()); }
                catch (Exception ignored) {}
            }).start();
        }
        SessionManager.getInstance().logout();
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    // ─── Lifecycle MapLibre (obbligatorio) ──────────────────────────────────────

    @Override protected void onStart()   { super.onStart();   mapView.onStart(); }
    @Override protected void onResume()  { super.onResume();  mapView.onResume(); caricaDelivery(); }
    @Override protected void onPause()   { super.onPause();   mapView.onPause(); }
    @Override protected void onStop()    { super.onStop();    mapView.onStop(); fermaPolling(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    private void mostraSnackbar(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }
}
