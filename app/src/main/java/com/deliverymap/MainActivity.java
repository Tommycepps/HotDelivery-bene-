package com.deliverymap;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverymap.utils.SessionManager;

/**
 * Activity di lancio: reindirizza a AuthActivity o MapActivity
 * in base allo stato della sessione corrente.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Se l'utente è già loggato (sessione in memoria), vai direttamente alla mappa
        if (SessionManager.getInstance().isLoggedIn()) {
            startActivity(new Intent(this, MapActivity.class));
        } else {
            startActivity(new Intent(this, AuthActivity.class));
        }
        // Chiudi MainActivity così non rimane nello stack di navigazione
        finish();
    }
}
