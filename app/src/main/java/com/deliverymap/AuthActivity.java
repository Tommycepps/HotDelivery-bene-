package com.deliverymap;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverymap.api.SupabaseClient;
import com.deliverymap.models.User;
import com.deliverymap.utils.HashUtils;
import com.deliverymap.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;

/**
 * Schermata di login e registrazione.
 * Gestisce l'autenticazione tramite Supabase REST API.
 */
public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnAccedi, btnRegistrati;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        etEmail       = findViewById(R.id.et_email);
        etPassword    = findViewById(R.id.et_password);
        btnAccedi     = findViewById(R.id.btn_accedi);
        btnRegistrati = findViewById(R.id.btn_registrati);
        progressBar   = findViewById(R.id.progress_bar);

        btnAccedi.setOnClickListener(v -> tentaLogin());
        btnRegistrati.setOnClickListener(v -> mostraDialogRegistrazione());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────────

    /** Verifica le credenziali su Supabase e avvia la sessione. */
    private void tentaLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            mostraSnackbar("Inserisci email e password");
            return;
        }

        String passwordHash = HashUtils.sha256(password);
        mostraLoading(true);

        // Chiamata di rete in background — mai bloccare il thread UI
        new Thread(() -> {
            try {
                User user = SupabaseClient.getInstance().getUserByEmail(email);
                runOnUiThread(() -> {
                    mostraLoading(false);
                    if (user == null) {
                        mostraSnackbar("Email non trovata");
                        return;
                    }
                    if (!user.password_hash.equals(passwordHash)) {
                        mostraSnackbar("Password errata");
                        return;
                    }
                    // Login riuscito: salva la sessione e vai alla mappa
                    SessionManager.getInstance().login(user.id, user.nome, user.ruolo);
                    startActivity(new Intent(AuthActivity.this, MapActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    mostraLoading(false);
                    mostraSnackbar("Errore di rete: " + e.getMessage());
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // REGISTRAZIONE
    // ─────────────────────────────────────────────────────────────────────────────

    /** Mostra un dialog per creare un nuovo account con scelta del ruolo. */
    private void mostraDialogRegistrazione() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_registrazione, null);
        EditText etNome    = dialogView.findViewById(R.id.et_nome);
        EditText etEmailR  = dialogView.findViewById(R.id.et_email_reg);
        EditText etPassR   = dialogView.findViewById(R.id.et_password_reg);
        RadioGroup rgRuolo = dialogView.findViewById(R.id.rg_ruolo);

        new AlertDialog.Builder(this)
                .setTitle("Crea account")
                .setView(dialogView)
                .setPositiveButton("Registrati", (dialog, which) -> {
                    String nome  = etNome.getText().toString().trim();
                    String email = etEmailR.getText().toString().trim();
                    String pass  = etPassR.getText().toString().trim();
                    int checkedId = rgRuolo.getCheckedRadioButtonId();
                    String ruolo = (checkedId == R.id.rb_cuoco) ? "cuoco" : "cliente";

                    if (nome.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        mostraSnackbar("Compila tutti i campi");
                        return;
                    }
                    registraUtente(nome, email, pass, ruolo);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    /** Invia i dati del nuovo utente a Supabase. */
    private void registraUtente(String nome, String email, String password, String ruolo) {
        String passwordHash = HashUtils.sha256(password);
        User nuovoUtente = new User(nome, email, passwordHash, ruolo);
        mostraLoading(true);

        new Thread(() -> {
            try {
                User creato = SupabaseClient.getInstance().createUser(nuovoUtente);
                runOnUiThread(() -> {
                    mostraLoading(false);
                    if (creato == null) {
                        mostraSnackbar("Registrazione fallita (email già in uso?)");
                        return;
                    }
                    SessionManager.getInstance().login(creato.id, creato.nome, creato.ruolo);
                    startActivity(new Intent(AuthActivity.this, MapActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    mostraLoading(false);
                    mostraSnackbar("Errore: " + e.getMessage());
                });
            }
        }).start();
    }

    // Helper UI
    private void mostraLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAccedi.setEnabled(!show);
        btnRegistrati.setEnabled(!show);
    }

    private void mostraSnackbar(String messaggio) {
        Snackbar.make(findViewById(android.R.id.content), messaggio, Snackbar.LENGTH_LONG).show();
    }
}
