// Ecotron node firmware
// Tomislav Mamić for Mikrotron d.o.o 2016.

#include <SoftwareSerial.h>

// runtime options
#define DEBUG true  // run in debug mode
#define MANUAL false // manual control mode
#define HCTO 15000  // HC-SR40 distance sensor

// software serial to sim808
SoftwareSerial sim808(10,8);

// sim808 controls
#define RI 12    // status
#define PWR 13   // soft power switch
#define DTR 11   // sleep

// temperature sensor
#define TMP A0   // signal pin
#define RESISTOR 10000  // sensor internal resistor value
double Rval[]={5.301,4.48,3.62};
double Tval[]={-40,0,50};

// carrier data for GSM network
String PIN="4448";
String APN="gprs0.vip.hr";

// server data
String SERVER="http://www.mikrotron.hr/ecotronserver/upload?stationId=alfa";
String gpsdata="";
String gpsp="&gpsInfo=";

// HC-SR40 pinout data
int marcos[]={3,A2,A4,7,5};
int polos[]={2,A1,A3,6,4};
int states[]={-1,-1,-1,-1,-1};

boolean repflag=false;

void setup(){
  pinsetup();
  Serial.begin(9600);
  sim808.begin(9600);
  sim808su();
  sim808wu();
}

void loop(){
  if(!digitalRead(RI)){
    //ToDo: handle dead module
    debugPrintln(F("sim808 not on!"));
  }
  debugStream();
  if(!repflag&&digitalRead(RI)&&!MANUAL){
    Serial.println("test "+PIN);
    if(!simcheck())return;else Serial.println(F("sim started!"));
    if(!initgprs())return;else Serial.println(F("gprs started!"));
    if(!setupgprs())return;else Serial.println(F("gprs configured!"));
    repflag=true;
  }
  if(repflag){
    checkResp(F("at+cbc"),30,1500,F("OK"));
    delay(5000);
    report();
    delay(5000);
  }
  //Serial.println(String(getmeasure(0,10))+"cm");
  //delay(1000);
}
