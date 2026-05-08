# 🚵 DeliveryMap

App Android nativa per visualizzare delivery attivi su una mappa in tempo reale.
**Nessun servizio Google** (no Firebase, no Google Maps) — solo MapLibre + Supabase.

## Funzionalità

- **Login / Registrazione** con ruolo Cuoco o Cliente
- **Vista Cuoco**: condivide la sua posizione GPS sulla mappa, aggiornata ogni 30 secondi
- **Vista Cliente**: vede tutti i delivery attivi come marker sulla mappa, ordinati per distanza
- **BottomSheet** al click su un marker: nome, descrizione, distanza calcolata con Haversine
- **FAB** per centrare la mappa sulla propria posizione

## Stack tecnologico

| Componente | Tecnologia |
|---|---|
| Mappa | MapLibre Android SDK 10.2 |
| Tile | OpenStreetMap (gratuito) |
| Database | Supabase (REST API) |
| HTTP | OkHttp 4 + Gson |
| Posizione | FusedLocationProviderClient |
| UI | Material Components 3 |
| Linguaggio | Java |

## Setup rapido

Vedi **[SETUP.md](SETUP.md)** per le istruzioni complete.

In breve:
1. Crea progetto su [supabase.com](https://supabase.com) (gratuito)
2. Esegui le query SQL di `SETUP.md` per creare le tabelle
3. Incolla URL e ANON_KEY in `Constants.java`
4. Sync Gradle → Run

## Struttura

```
app/src/main/java/com/deliverymap/
├── MainActivity.java       ← launcher
├── AuthActivity.java       ← login/registrazione
├── MapActivity.java        ← mappa + logica delivery
├── api/SupabaseClient.java  ← chiamate REST Supabase
├── models/{User,Delivery}  ← modelli dati
└── utils/{Constants,HashUtils,LocationUtils,SessionManager}
```
