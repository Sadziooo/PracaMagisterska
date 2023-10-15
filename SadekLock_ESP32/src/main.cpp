#include <SPI.h>
#include <PN532.h>
#include <PN532_SPI.h>
#include <NfcAdapter.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <FirebaseJson.h>
#include <esp32-hal-ledc.h>
#include <stdio.h>
#include <time.h>
#include <SD.h>
#include <ESP32Time.h>

#define WIFI_SSID "Sadziochny"
#define WIFI_PASSWORD "kanapeczki123"

#define API_KEY "AIzaSyDY9dE-4P_tzKjFnlpIAMsi9gXAt3tpAv4"
#define DATABASE_URL "sadeklock-default-rtdb.firebaseio.com"
#define FIREBASE_PROJECT_ID "sadeklock"
#define USER_EMAIL "adrisad473@student.polsl.pl"
#define USER_PASSWORD "Siemanko123!"

#define led_green 16
#define led_red 4 
#define RELAY_PIN 26
#define RELAY_ON_TIME 3000  // Czas włączenia przekaźnika (w milisekundach)
#define BUZZER_PIN 27
#define BUZZER_SHORT_DELAY 300  // Czas trwania krótkiego dźwięku (w milisekundach)

#define MAX_USERS 5

#define SD_CS 15
#define NFC_CS 5
SPIClass spiV(VSPI);
SPIClass spiH(HSPI);

ESP32Time rtc(7200); // offset w sekundach GMT+2
const char* ntpServer = "pool.ntp.org";
const long  gmtOffset_sec = 3600;
const int   daylightOffset_sec = 3600;

FirebaseData fbdo;            /* Define the Firebase Data object */
FirebaseAuth auth;            /* Define the FirebaseAuth data for authentication data */
FirebaseConfig config;        /* Define the FirebaseConfig data for config data */
FirebaseJson json;            /* Define the FirebaseJson structure */
FirebaseData stream;          /* Define the Firebase Data obj for stream */

String usersPath = "/SadekLockData/Users";
String userUID;
String history_file = "/history.txt";
String users_file = "/users.txt";

volatile bool dataChanged = false; 

unsigned long DataMillis = 0;
unsigned long getDataPrevMillis = 0;
unsigned long refreshTokenPrevMillis = 0;
int count = 0;

PN532_SPI interface(spiV, NFC_CS); // create a PN532 SPI interface with the SPI CS terminal located at digital pin 10
NfcAdapter nfc = NfcAdapter(interface); // create an NFC adapter object

unsigned long start_time; 
unsigned long timed_event;
unsigned long current_time; // millis() function returns unsigned long

bool isRELAYOn = false;        // Flaga określająca, czy przekaźnik jest włączona
unsigned long RELAYStartTime;  // Czas rozpoczęcia włączania przekaźnika
bool isWrongCard = false;     // Flaga określająca, czy stwierdzono brak dostępu
unsigned long NoPermissionStartTime; // Czas rozpoczęcia dźwięku

const int pwmChannel = 0;
const int pwmResolution = 8;  // Rozdzielczość PWM (liczba bitów), 8 oznacza zakres wartości od 0 do 255
const int pwmFrequency = 500;  // Częstotliwość PWM w Hz

struct NFCRecord {
  String tagId;
  String name;
  bool permission;
};

struct User {
  String name;
  String uid;
};

struct tm timeinfo;

User users[5];

NFCRecord record;

File myFile;

void getUsersFromFirebase() {
  Serial.println("Zbieranie użytkowników...");

  if (Firebase.RTDB.getJSON(&fbdo, usersPath)) {
      FirebaseJson *json = fbdo.jsonObjectPtr();
      size_t len = json->iteratorBegin();
      FirebaseJson::IteratorValue value;
      for (size_t i = 0; i < len; i++) {
        value = json->valueAt(i);
        Serial.printf((const char *)FPSTR("%d, Name: %s, Value: %s\n"), i, value.key.c_str(), value.value.c_str());
        if(i==1) {
          users[0].name = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==2) {
          users[0].uid = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==4) {
          users[1].name = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==5) {
          users[1].uid = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==7) {
          users[2].name = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==8) {
          users[2].uid = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==10) {
          users[3].name = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==11) {
          users[3].uid = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==13) {
          users[4].name = String(value.value.c_str()).substring(1, value.value.length() - 1);
        } else if(i==14) {
          users[4].uid = String(value.value.c_str()).substring(1, value.value.length() - 1);
        }
      }
      json->iteratorEnd();
      json->clear();
  }
  
  Serial.println("Koniec!");
}

void streamCallback(FirebaseStream data) {
  Serial.printf("ścieżka strumienia, %s\nścieżka zdarzenia, %s\ntyp danej, %s\ntyp zdarzenia, %s\n\n",
                data.streamPath().c_str(),
                data.dataPath().c_str(),
                data.dataType().c_str(),
                data.eventType().c_str());
  printResult(data); // see addons/RTDBHelper.h
  Serial.println();
  Serial.printf("Rozmiar ładunku odebranego strumienia: %d (Max. %d)\n\n", data.payloadLength(), data.maxPayloadLength());

  dataChanged = true;
}

void streamTimeoutCallback(bool timeout) {
  if (timeout)
    Serial.println("Przekroczono limit czasu transmisji, wznawianie...\n");

  if (!stream.httpConnected())
    Serial.printf("Kod błędu: %d, Powód: %s\n\n", stream.httpCode(), stream.errorReason().c_str());
}

void setupFirebase() {
  Serial.printf("Firebase Client v%s\n\n", FIREBASE_CLIENT_VERSION);

  config.api_key = API_KEY; /* Assign the api key (required) */
  config.database_url = DATABASE_URL; /* Assign the RTDB URL (required) */
  auth.user.email = USER_EMAIL; /* Assign the user sign in credentials */
  auth.user.password = USER_PASSWORD; /* Assign the user sign in credentials */

  //Firebase.reconnectWiFi(true); // biblioteki sie ze sobą kłócą i coś się buguje przy reconect
  fbdo.setResponseSize(4096);

  /* Assign the callback function for the long running token generation task */
  config.token_status_callback = tokenStatusCallback; // see addons/TokenHelper.h
  config.max_token_generation_retry = 5; // Assign the maximum retry of token generation
  Firebase.begin(&config, &auth); /* Initialize the library with the Firebase authen and config */

  if (Firebase.ready()) {
    Serial.println("Firebase połączony.");
    Serial.println("Zbieranie User UID");
    while ((auth.token.uid) == "") {
      Serial.print('.');
      delay(1000);
    }
    userUID = auth.token.uid.c_str();
    Serial.print("User UID: "); Serial.println(userUID);
  } else {
    Serial.println("Firebase niepołączony.");
  }
}

void printLocalTime() {
  if (WiFi.status() == WL_CONNECTED) {
    getLocalTime(&timeinfo);
    Serial.print("Zsynchronizowano czas: ");
    Serial.println(&timeinfo, "%F %H:%M:%S");
    if (getLocalTime(&timeinfo)) {
      rtc.setTimeStruct(timeinfo); 
    }
  } else {
    timeinfo = rtc.getTimeStruct();
    Serial.println("Błąd odczytu czasu!");
  }
}

void setupRTC() {
  // Init and get the time
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  printLocalTime();
}

void refreshToken() {
  if (Firebase.isTokenExpired()) {
    Firebase.refreshToken(&config);
    if (WiFi.status() == WL_CONNECTED) {
      printLocalTime();
    }
    Serial.println("Refresh token");
  }
}

void setupWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.println("Łączenie z Wi-Fi...");
  int count = 0;
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    count++;
    delay(300);
    if (count > 10){
      Serial.println();
      Serial.println("Nie można połączyć się z Wi-Fi.");
      break;
    }
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());
    Serial.println(); 
  }
}

void SD_Init() {
  Serial.println("Inicjalizowanie karty SD...");
  if(!SD.begin(SD_CS, spiH, 40000000)) {
    Serial.println("Inicjalizacja nie powiodła się!");
  }
  Serial.println("Karta SD została zainicjalizowana");
}

void SD_write_history(String file, String Name, String UID, bool Permission) { 
  myFile = SD.open(file, FILE_APPEND);

  if (myFile) {
    Serial.println("Trwa zapis do " + file + "...");
    myFile.println("Name: " + Name);
    myFile.println("UID: " + UID);
    myFile.print("Permission: ");
    myFile.println(Permission ? "true" : "false");
    myFile.print("Timestamp: ");
    Serial.println(&timeinfo, "%F %H:%M:%S");
    myFile.println(&timeinfo, "%F %H:%M:%S");
    myFile.println();
    myFile.close();
    Serial.println("Koniec.");
  } else {
    Serial.println("Błąd otwarcia pliku " + file);
  }
  
}

void SD_write_users(String file) {
  Serial.println("Aktualizowanie pliku " + file);
  myFile = SD.open(file, FILE_WRITE);

  if (myFile) {
    // Nadpisanie pliku nowymi danymi
    for (int i = 0; i < MAX_USERS; i++) {
      myFile.println("User" + String(i + 1));
      myFile.print("Name: ");
      myFile.println(users[i].name);
      myFile.print("UID: ");
      myFile.println(users[i].uid);
    }

    myFile.close();
    Serial.println("Plik " + file + " został zaktualizowany.");
  } else {
    Serial.println("Błąd otwarcia pliku " + file + " do zapisu!");
  }
}

void SD_read(String file) {
  Serial.println("Odczyt pliku " + file);
  myFile = SD.open(file);
  if (myFile) {
    while (myFile.available()) {
      Serial.write(myFile.read());
    }
    myFile.close();
    Serial.println();
    Serial.println("Koniec pliku " + file);
  } else {
    Serial.println("Błąd otwarcia pliku " + file + " do zapisu!");
  }
}

void SD_getUsers(String file, User users[]) {
  Serial.println("Inicjalizacja użytkowników...");

  File myFile = SD.open(file, FILE_READ);
  if (!myFile) {
    Serial.println("Błąd otwarcia pliku " + file);
    return;
  }

  // Variables for parsing user data
  String line;
  int userIndex = -1;

  // Read the myFile line by line
  while (myFile.available()) {
    line = myFile.readStringUntil('\n');
    line.trim();

    // Check if a new user entry is found
    if (line.startsWith("User")) {
      userIndex++;

      // Check if the maximum number of users has been reached
      if (userIndex >= MAX_USERS) {
        Serial.println("Osiągnięto maksymalną ilość użytkowników.");
        break;
      }
    } else if (line.startsWith("Name:")) {
      // Extract the name from the line
      String name = line.substring(6);
      users[userIndex].name = name;
    } else if (line.startsWith("UID:")) {
      // Extract the UID from the line
      String UID = line.substring(5);
      users[userIndex].uid = UID;
    }
  }

  // Close the myFile
  myFile.close();

  Serial.println("Inicjalizacja użytkowników skończona!");
}

void RELAY_ONOFF() {
  if(record.permission == true){ 
    // Sprawdź, czy przekaźnik powinien być włączony i czy czas włączenia nie został przekroczony
    if (isRELAYOn && (millis() - RELAYStartTime < RELAY_ON_TIME)) {
      digitalWrite(RELAY_PIN, HIGH);  // Włącz przekaźnik
      digitalWrite(led_green, HIGH);
      ledcWrite(pwmChannel, 128); // Włącz buzzer
      Firebase.RTDB.setBool(&fbdo, "/SadekLockData/PadlockState", true);
    } else {
      digitalWrite(RELAY_PIN, LOW);   // Wyłącz przekaźnik
      ledcWrite(pwmChannel, 0); // Wyłącz buzzer
      digitalWrite(led_green, LOW);
      isRELAYOn = false;              // Wyzeruj flagę
      Firebase.RTDB.setBool(&fbdo, "/SadekLockData/PadlockState", false);
    }
  } else {
    if (isWrongCard && (millis() - NoPermissionStartTime < BUZZER_SHORT_DELAY)) {
      ledcWrite(pwmChannel, 128); // Włącz buzzer
      digitalWrite(led_red, HIGH);
      delay(100);
      ledcWrite(pwmChannel, 0); // Wyłącz buzzer
      delay(100);
      ledcWrite(pwmChannel, 128); // Wyłącz buzzer
    } else {
      ledcWrite(pwmChannel, 0); // Wyłącz buzzer
      digitalWrite(led_red, LOW);
      isWrongCard = false;        // Wyzeruj flagę
    }  
  }
}

void readNFC() {
  static unsigned long previousTime = 0;
  unsigned long currentTime = millis();

  if (currentTime - previousTime >= 2000) {
    previousTime = currentTime;

    if (nfc.tagPresent()) {
      getUsersFromFirebase();
      NfcTag tag = nfc.read();
      tag.print();

      printLocalTime();
      
      
      record.tagId = tag.getUidString();

      bool isTagIdMatched = false;
      record.permission = false;

      for (int i = 0; i <= 4; i++) {
        if (record.tagId == users[i].uid) {
          record.name = users[i].name;
          isRELAYOn = true;           // Ustaw flagę, że przekaźnik powinien być włączony
          RELAYStartTime = millis();  // Zapisz aktualny czas
          isTagIdMatched = true;
          record.permission = true;
          break;
        }
      }

      if (!isTagIdMatched) {
        record.name = "Unknown";
        isWrongCard = true;
        NoPermissionStartTime = millis();
      }

      RELAY_ONOFF();

      json.clear().add("Name", record.name);
      json.add("UID", record.tagId);
      json.add("Permission", record.permission);

      SD_write_history(history_file, record.name, record.tagId, record.permission); 

      nfc.begin();

      if(Firebase.ready() && Firebase.RTDB.pushJSON(&fbdo, "/SadekLockData/History", &json)){
        Serial.println("Push poprawny");
      } else {
        Serial.println("Push nie powiódł się.");
        Serial.printf("Powód: "); Serial.println(fbdo.errorReason());
      }
    }

    RELAY_ONOFF();
  }
}

void PrintUsers() {
  for (int i = 0; i < 5; i++) {
      Serial.println("User" + String(i + 1));
      Serial.print("Name: ");
      Serial.println(users[i].name);
      Serial.print("UID: ");
      Serial.println(users[i].uid);
    }
}

void setup(void) {
  pinMode(4, OUTPUT);
  pinMode(16, OUTPUT);
  pinMode(26, OUTPUT);
  pinMode(27, OUTPUT);
  pinMode(32, OUTPUT);
  pinMode(33, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  digitalWrite(led_green, LOW);
  digitalWrite(led_red, LOW);

  spiV.begin();
  spiH.begin();

  ledcSetup(pwmChannel, pwmFrequency, pwmResolution);
  ledcAttachPin(BUZZER_PIN, pwmChannel);

  Serial.begin(115200);

  setupWiFi();

  setupFirebase();
  
  setupRTC();

  SD_Init();

  if (!Firebase.RTDB.beginStream(&stream, "/SadekLockData/Users"))
    Serial.printf("Błąd rozpoczęcia strumienia, %s\n\n", stream.errorReason().c_str());

  Firebase.RTDB.setStreamCallback(&stream, streamCallback, streamTimeoutCallback);

  SD_getUsers(users_file, users);

  PrintUsers();

  nfc.begin();
}

void loop() {
  readNFC();
  
  if (Firebase.ready() && (millis() - getDataPrevMillis > 10000 || getDataPrevMillis == 0)) {
    getDataPrevMillis = millis();
    if (dataChanged) {
      dataChanged = false;
      getUsersFromFirebase();
      SD_write_users(users_file);
      SD_read(users_file);
    }
  }

  if (millis() - refreshTokenPrevMillis > 3000) { 
    refreshTokenPrevMillis = millis();
    refreshToken();
  }
}



