// Add to voice_assistant.ino: receive phone notifications over Bluetooth Classic (SPP) and relay them to the PC bridge.
#include "BluetoothSerial.h"
#include <HTTPClient.h>
BluetoothSerial SerialBT;
void btSetup() { SerialBT.begin("Kate"); }          // call in setup(); print SerialBT.getBtAddress() once, write it to the NFC tag
void btLoop() {                                      // call in loop()
  if (!SerialBT.available()) return;
  String l = SerialBT.readStringUntil('\n');         // N|app|title|text
  if (!l.startsWith("N|")) return;
  int a = l.indexOf('|', 2), b = l.indexOf('|', a + 1);
  String json = "{\"app\":\"" + l.substring(2, a) + "\",\"title\":\"" + l.substring(a + 1, b) + "\",\"text\":\"" + l.substring(b + 1) + "\"}";
  HTTPClient h; h.begin("http://PC_IP:8000/notify"); h.addHeader("Content-Type", "application/json"); h.POST(json); h.end();
}
