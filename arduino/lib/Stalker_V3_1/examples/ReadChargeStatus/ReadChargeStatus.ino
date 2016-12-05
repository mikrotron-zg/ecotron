// Seeeduino Stalker V3.1 - ReadChargeStatus 
// Loovee@2016-7-26

#include "SeeeduinoStalker.h"

Stalker stalker;

void setup()
{
    Serial.begin(9600);
    Serial.println("Seeeduino Stalker V3.1 - Charge Status test");
}

void loop()
{
    int status = stalker.readChrgStatus();
    
    switch(status)
    {
        case 0:
            Serial.println("no battery insert");
            break;
            
        case 1:
            Serial.println("charging");
            break;
            
        case 2:
        
            Serial.println("charge done");
            break;
            
        default:;   
    }
    
    delay(1000);
}

// END FILE