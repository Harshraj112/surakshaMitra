
# 🚨 Suraksha Mitra - SOS Safety App

[Web_Page](https://surakshamitra112.netlify.app/)

Suraksha Mitra is a **personal safety and emergency response app** designed to help users quickly alert their trusted contacts and emergency services during critical situations.  

The app integrates **voice detection, panic button, GPS location, GSM module, and hardware triggering** to ensure help is just one tap (or word) away.

---

## 📌 Features

- **🔑 Login/Registration**  
  Secure access with user accounts.

- **🏠 Home Screen**  
  Central hub with emergency actions.

- **🚨 Panic Button**  
  Instantly trigger SOS alerts.

- **🎙️ Keyword Voice Detection**  
  Speak a predefined keyword (e.g., *Help*) to activate SOS.

- **📞 One-Tap Emergency**  
  Directly contact police, ambulance, or emergency services.

- **👨‍👩‍👧 Friends/Family Contacts**  
  Save trusted contacts for immediate notifications.

- **📍 Location Tracking (GPS/GSM)**  
  Share live location with contacts when SOS is triggered.

- **🔔 Alert Notifications**  
  Send instant SMS/Call notifications to connected devices.

- **⚡ Hardware Triggering**  
  Integration with wearable SOS band via Bluetooth/GSM module.

---

## 📝 App Flow

```mermaid
flowchart TD
    A[Start] --> B[Login/Registration]
    B --> C{Logged In?}
    C -- No --> B
    C -- Yes --> D[Home Screen]
    D --> E[Panic Button]
    D --> F[Keyword Voice Detection]
    D --> G[One-Tap Emergency]
    D --> H[Friends/Family Contacts]
    E --> I[SOS Triggered]
    F --> I
    G --> I
    H --> I
    I --> J[Send SMS/Call to Contacts]
    J --> K[Include Location via GSM/GPS]
    K --> L[Alert Notifications to Connected Devices]
