# Gabay Public Transit & Commuter Routing Engine

## 1. Overview & Google Maps Parity

In public navigation applications like Google Maps and Citymapper, commuters do not simply follow a single vehicular road path. Instead, a commuter journey is **multimodal**—combining walking, boarding scheduled or high-frequency transit lines (trains, buses, ferries, jeepneys), transferring between platforms or terminals, and walking to the final destination.

Gabay introduces a specialized **Philippine Multimodal Commute Routing Engine** (`dev.gabay.app.commute`) designed to provide Google Maps-parity public commute directions natively on-device.

```
[Origin]
   │
   ▼ (Walk ~4 mins / 280m)
[Boarding Station: Taft Avenue MRT-3]
   │
   ▼ (Ride 5 stops ~14 mins | Departs every 4 mins | ₱18)
   ├─ Magallanes
   ├─ Ayala
   ├─ Buendia
   ├─ Guadalupe
   └─ Boni
   │
   ▼ (Alight at Cubao Station)
[Transfer Walkway ~3 mins / 180m via Gateway Mall]
   │
   ▼ (Boarding Station: Araneta Center-Cubao LRT-2)
   │
   ▼ (Ride 3 stops ~8 mins | Departs every 6 mins | ₱16)
   ├─ Anonas
   ├─ Katipunan
   └─ Santolan
   │
   ▼ (Walk ~2 mins / 120m)
[Destination]
```

---

## 2. Supported Philippine Transit Networks

Gabay's transit database (`CommuteTransitDatabase.kt`) catalogs active transit corridors across Metro Manila and Central Luzon:

| Transit Line | Type | Stations / Terminals | Route Coverage | Official Hex Color | Headway | Base Fare |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MRT-3** | Train (Heavy Rail) | 13 Stations | North Ave $\leftrightarrow$ Taft Ave (EDSA Corridor) | `#0284C7` (Sky Blue) | 4 min | ₱13 |
| **LRT-1** | Train (Light Rail) | 25 Stations | Dr. Santos (Cavite Extension) $\leftrightarrow$ FPJ (Roosevelt) | `#EAB308` (Gold) | 4 min | ₱15 |
| **LRT-2** | Train (Heavy Rail) | 13 Stations | Recto (Manila) $\leftrightarrow$ Antipolo (Rizal) | `#A855F7` (Purple) | 6 min | ₱15 |
| **EDSA Busway** | Bus Rapid Transit | 18 Median Stops | Monumento $\leftrightarrow$ PITX Terminal (24/7) | `#10B981` (Emerald) | 3 min | ₱15 |
| **Pasig River Ferry** | Water Ferry | 13 Terminals | Escolta (Manila) $\leftrightarrow$ Pinagbuhatan (Pasig) | `#06B6D4` (Cyan) | 20 min | FREE (MMDA) |
| **Maharlika Bus** | Provincial Bus | Major Terminals | Cabanatuan $\leftrightarrow$ Gapan $\leftrightarrow$ Baliwag $\leftrightarrow$ Manila | `#F97316` (Orange) | 10 min | ₱20 |
| **Modern PUV** | Jeepney / E-Jeep | Local Corridors | Gapan $\leftrightarrow$ San Isidro $\leftrightarrow$ Cabiao $\leftrightarrow$ Arayat | `#F59E0B` (Amber) | 5 min | ₱15 |

---

## 3. Multimodal Journey Representation

A commute itinerary is decomposed into sequential `CommuteLeg` instances:

1. **`CommuteLeg.Walk`**:
   - Walking distance in meters and estimated walking duration at $1.33\text{ m/s}$ ($\sim 4.8\text{ km/h}$).
   - Destination station target name.
   - Dotted/dashed visual polyline on MapLibre.

2. **`CommuteLeg.TransitRide`**:
   - Line metadata (line name, headsign, official color code, vehicle icon).
   - Departure and arrival stations.
   - Intermediate stops list (expandable in the UI).
   - Frequency (headway) and fare estimation in Philippine Pesos (`₱`).

3. **`CommuteLeg.Transfer`**:
   - Station transfer instructions (e.g. *"Transfer from MRT-3 to LRT-2 at Cubao via Gateway walkway"*).
   - Transfer walking distance and estimated platform change time.

---

## 4. CDN Distribution & Pre-Flight Vault Integrity Check

### A. Is GitHub Releases Allowed for Offline Vault Files?
* **Current Status**: GitHub Releases allows attachments up to $2.0\text{ GB}$ per release asset, distributed over Fastly CDN. For initial releases, open-source distribution, and on-demand installation, GitHub Releases is completely free and allowed.
* **Scalability Threshold**: If the application grows to tens of thousands of active users concurrently downloading $455\text{ MB}$ packages, GitHub may trigger rate limits or bandwidth throttling under their Terms of Service.
* **Production CDN Alternatives**:
  1. **Cloudflare R2**: 10 GB free storage with **$0 egress fees** (completely unlimited free download bandwidth worldwide).
  2. **Hugging Face Hub**: Free hosting and unlimited high-speed global CDN for AI models, datasets, and binary archives.

### B. Pre-Flight Integrity Check & User Confirmation Dialog
To prevent runtime crashes and ensure users have full control over data usage:
1. On launch, `VaultIntegrityManager.checkVault()` verifies the presence and integrity of:
   * `ph.gabay` ($273.8\text{ MB}$)
   * `pois_fts.sqlite` ($106.1\text{ MB}$)
   * `philippines_vector.pmtiles` ($75.4\text{ MB}$)
2. If all files exist, Gabay launches straight into Cockpit mode with zero prompts.
3. If any file is missing (fresh install or accidental deletion):
   * Gabay presents the **Offline Vault Setup Dialog** (`VaultSetupDialog.kt`).
   * Clearly shows which files are missing, their sizes, and the total download size ($455\text{ MB}$).
   * Gives the user the choice to **"Download Offline Vault"** (with live progress bar) or **"Skip / Use Manual ADB Push"**.
