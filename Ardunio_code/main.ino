#include <SoftwareSerial.h>
#include <EEPROM.h>
#include <TinyGPS++.h>

// ---------------- Pin Definitions ----------------
#define BUTTON_PIN 2
#define VIBRATION_PIN 3
#define SIM800_TX 7   // Arduino RX (connect to SIM800 TX)
#define SIM800_RX 8   // Arduino TX (connect to SIM800 RX)
#define BT_TX 11      // Arduino RX (connect to HC-05 TX)
#define BT_RX 10      // Arduino TX (connect to HC-05 RX)
#define GPS_TX 5      // Arduino RX (connect to GPS TX)
#define GPS_RX 4      // Arduino TX (connect to GPS RX)

// ---------------- Serial Objects ----------------
SoftwareSerial sim800(SIM800_TX, SIM800_RX);
SoftwareSerial btSerial(BT_TX, BT_RX);
SoftwareSerial gpsSerial(GPS_TX, GPS_RX);
TinyGPSPlus gps;

// ---------------- Global Variables ----------------
String contacts[3];
bool sosActive = false;

// ---------------- EEPROM Functions ----------------
void saveContact(int index, String number) {
  int addr = index * 15;  
  for (int i = 0; i < 15; i++) {
    if (i < number.length()) {
      EEPROM.write(addr + i, number[i]);
    } else {
      EEPROM.write(addr + i, 0);
    }
  }
}

String readContact(int index) {
  if (index < 0 || index > 2) return "";
  int addr = index * 15;
  char num[16];
  for (int i = 0; i < 15; i++) {
    num[i] = EEPROM.read(addr + i);
  }
  num[15] = '\0';
  return String(num);
}

void loadContacts() {
  for (int i = 0; i < 3; i++) {
    contacts[i] = readContact(i);
    if (contacts[i].length() == 0) {
      contacts[i] = "7903237319"; // default number (no country code)
      saveContact(i, contacts[i]);
    }
  }
}

// ---------------- GSM SMS Function ----------------
void sendSMS(String number, String message) {
  Serial.print("📨 Sending SMS to ");
  Serial.println(number);

  sim800.println("AT+CMGF=1");  
  delay(500);

  sim800.print("AT+CMGS=\"");
  sim800.print(number);
  sim800.println("\"");
  delay(500);

  sim800.print(message);
  delay(200);

  sim800.write(26); // CTRL+Z
  delay(5000);
  Serial.println("✅ SMS Sent");
}

// ---------------- Location Fetch Function ----------------
String getLocation() {
  String location = "";

  // --- Step 1: Ask phone for GPS (10s wait) ---
  Serial.println("📡 Requesting GPS from phone...");
  btSerial.println("LOC_REQ");

  unsigned long startTime = millis();
  while (millis() - startTime < 10000) {
    if (btSerial.available()) {
      String response = btSerial.readStringUntil('\n');
      response.trim();
      if (response.startsWith("LOC:")) {
        location = response.substring(4);
        Serial.print("✅ Got Phone Location: ");
        Serial.println(location);
        return location;
      }
    }
  }

  // --- Step 2: GPS Module (5s wait) ---
  Serial.println("📡 Phone failed, checking GPS module...");
  startTime = millis();
  while (millis() - startTime < 5000) {
    while (gpsSerial.available() > 0) gps.encode(gpsSerial.read());
    if (gps.location.isUpdated()) {
      float lat = gps.location.lat();
      float lon = gps.location.lng();
      location = String(lat, 6) + "," + String(lon, 6);
      Serial.print("✅ Got GPS Module Location: ");
      Serial.println(location);
      return location;
    }
  }

  // --- Step 3: No location ---
  Serial.println("❌ No location found");
  return "No location found";
}

// ---------------- SOS Sequence ----------------
void triggerSOS() {
  if (sosActive) return;
  sosActive = true;

  Serial.println("🚨 SOS Triggered!");
  for (int i = 0; i < 3; i++) {
    digitalWrite(VIBRATION_PIN, HIGH);
    delay(3000);
    digitalWrite(VIBRATION_PIN, LOW);
    delay(1000);
  }

  String loc = getLocation();
  String msg = "🚨 SOS! Need Help! Location: ";
  if (loc != "No location found") msg += "https://maps.google.com/?q=" + loc;
  else msg += loc;

  for (int i = 0; i < 3; i++) {
    sendSMS(contacts[i], msg);
  }

  sosActive = false;
}

// ---------------- Setup ----------------
void setup() {
  Serial.begin(9600);
  sim800.begin(9600);
  btSerial.begin(9600);
  gpsSerial.begin(9600);

  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(VIBRATION_PIN, OUTPUT);

  loadContacts();

  Serial.println("✅ System Ready!");
}

// ---------------- Loop ----------------
void loop() {
  // --- Check panic button ---
  if (digitalRead(BUTTON_PIN) == LOW) {
    triggerSOS();
    delay(1000);
  }

  // --- Check Bluetooth commands ---
  if (btSerial.available()) {
    String cmd = btSerial.readStringUntil('\n');
    cmd.trim();

    if (cmd == "SOS") {
      triggerSOS();
    }
    else if (cmd.startsWith("SET_CONTACT")) {
      int idx = cmd.charAt(11) - '0';
      String newNum = cmd.substring(13);
      if (idx >= 0 && idx < 3) {
        saveContact(idx, newNum);
        contacts[idx] = newNum;
        btSerial.println("OK CONTACT UPDATED");
      }
    }
  }
}
