// Seeeduino Stalker V3.1 - ReadBattery 
// Loovee@2016-7-26

#include "SeeeduinoStalker.h"

Stalker stalker;

void setup()
{
    Serial.begin(9600);
    Serial.println("Seeeduino Stalker V3.1 - ReadBattery test");
}

void loop()
{
    float vol = stalker.readBattery();
    Serial.println(vol, 2);
    delay(100);
}

// END FILE