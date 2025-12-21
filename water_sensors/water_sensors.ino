#include <Arduino.h>
#include <ArduinoJson.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// ---------- TEMP SENSOR ----------
#define ONE_WIRE_BUS 22
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

// ---------- ANALOG PINS ----------
#define pH_pin A2
#define TurbidityPin A1
#define TdsSensorPin A0

#define VREF 3.3
#define SCOUNT 30

int analogBuffer[SCOUNT];
int analogBufferTemp[SCOUNT];
int analogBufferIndex = 0;

float calibration_value = 21.34 - 0.7;
float temperature = 25;
float tdsValue = 0;
float ph_act = 0;

// ---------- MEDIAN FILTER ----------
int getMedianNum(int bArray[], int len) {
  int temp;
  for (int i = 0; i < len - 1; i++) {
    for (int j = i + 1; j < len; j++) {
      if (bArray[i] > bArray[j]) {
        temp = bArray[i];
        bArray[i] = bArray[j];
        bArray[j] = temp;
      }
    }
  }
  return (len % 2) ? bArray[len / 2]
                   : (bArray[len / 2] + bArray[len / 2 - 1]) / 2;
}

// ---------- SETUP ----------
void setup() {
  Serial.begin(115200);
  sensors.begin();
  Serial.println("✅ Pico Serial Ready for sending sensor data...");
}

// ---------- LOOP ----------
unsigned long lastSend = 0;
const unsigned long interval = 30000; // 30 sec

void loop() {
  if (millis() - lastSend < interval) return;
  lastSend = millis();

  // ---- TEMPERATURE ----
  sensors.requestTemperatures();
  temperature = sensors.getTempCByIndex(0);

  // ---- TDS ----
  analogBuffer[analogBufferIndex++] = analogRead(TdsSensorPin);
  if (analogBufferIndex == SCOUNT) analogBufferIndex = 0;

  memcpy(analogBufferTemp, analogBuffer, sizeof(analogBuffer));
  float avgVoltage = getMedianNum(analogBufferTemp, SCOUNT) * VREF / 1024.0;
  float compensation = 1.0 + 0.02 * (temperature - 25.0);
  float compVoltage = avgVoltage / compensation;
  tdsValue = (133.42 * pow(compVoltage, 3)
             -255.86 * pow(compVoltage, 2)
             +857.39 * compVoltage) * 0.5;

  // ---- TURBIDITY ----
  int turbidityRaw = analogRead(TurbidityPin);
  float turbidityVoltage = turbidityRaw * (5.0 / 1024.0);

  // ---- PH ----
  int buffer_arr[10];
  for (int i = 0; i < 10; i++) {
    buffer_arr[i] = analogRead(pH_pin);
    delay(20);
  }

  int avg = 0;
  for (int i = 2; i < 8; i++) avg += buffer_arr[i];
  float volt = avg * 5.0 / 1024.0 / 6.0;
  ph_act = -5.70 * volt + calibration_value;

  // ---- BUILD JSON ----
  StaticJsonDocument<256> doc;
  doc["temp_c"] = temperature;
  doc["tds"] = tdsValue;
  doc["turbidity_v"] = turbidityVoltage;
  doc["ph"] = ph_act;

  char jsonBuffer[256];
  serializeJson(doc, jsonBuffer);

  // ---- SEND OVER USB SERIAL ----
  Serial.println(jsonBuffer);

  Serial.println("✅ Sensor data sent over USB");
}
