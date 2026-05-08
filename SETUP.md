# Setup DeliveryMap — Guida configurazione

## 1. Crea il progetto su Supabase

1. Vai su **[supabase.com](https://supabase.com)** → crea account gratuito
2. Clicca **"New Project"** → scegli un nome (es. `deliverymap`) → imposta password DB → scegli regione Europa
3. Attendi ~2 minuti mentre il progetto viene creato

---

## 2. Crea le tabelle nel database

1. Nella sidebar sinistra clicca **"SQL Editor"**
2. Clicca **"New query"** e incolla il seguente SQL:

```sql
-- Tabella utenti
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nome TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  ruolo TEXT NOT NULL CHECK (ruolo IN ('cuoco', 'cliente')),
  created_at TIMESTAMP DEFAULT now()
);

-- Tabella delivery
CREATE TABLE deliveries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cuoco_id UUID REFERENCES users(id),
  nome TEXT NOT NULL,
  descrizione TEXT,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  attivo BOOLEAN DEFAULT TRUE,
  updated_at TIMESTAMP DEFAULT now()
);

-- Disabilita RLS per semplicità (solo per sviluppo)
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE deliveries DISABLE ROW LEVEL SECURITY;
```

3. Clicca **"Run"** — dovresti vedere "Success"

---

## 3. Ottieni le chiavi API

1. Nella sidebar sinistra vai su **"Project Settings"** → **"API"**
2. Copia:
   - **Project URL** → es. `https://abcdefgh.supabase.co`
   - **anon/public key** → stringa lunga che inizia con `eyJ...`

---

## 4. Configura il progetto Android

Apri il file:
```
app/src/main/java/com/deliverymap/utils/Constants.java
```

Sostituisci i placeholder:
```java
public static final String SUPABASE_URL      = "https://TUO_PROJECT_ID.supabase.co";
public static final String SUPABASE_ANON_KEY = "TUA_ANON_KEY";
```

---

## 5. Sincronizza e avvia

1. In Android Studio: **File → Sync Project with Gradle Files**
2. Collega un dispositivo Android (API 24+) o usa un emulatore
3. Clicca **Run ▶**

---

## Struttura del progetto

```
app/src/main/java/com/deliverymap/
├── MainActivity.java       ← launcher
├── AuthActivity.java       ← login / registrazione
├── MapActivity.java        ← mappa + delivery
├── api/
│   └── SupabaseClient.java ← tutte le chiamate REST
├── models/
│   ├── User.java
│   └── Delivery.java
└── utils/
    ├── Constants.java      ← ← CONFIGURA QUI ← ←
    ├── HashUtils.java      ← SHA-256
    ├── LocationUtils.java  ← haversine
    └── SessionManager.java ← sessione in memoria
```

---

## Note importanti

- ⚠️ La password è hashata con SHA-256 lato client (sufficiente per uso scolastico)
- 📡 Il polling aggiorna la posizione ogni 30 secondi (modificabile in `Constants.POLLING_INTERVAL_MS`)
- 🗺️ La mappa usa tile gratuiti di [MapLibre Demo Tiles](https://demotiles.maplibre.org)
