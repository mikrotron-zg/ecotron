// Ecotron node firmware
// Tomislav Mamić for Mikrotron d.o.o 2016.

// --- pin map ---

// - sim808
// 8  - TX
// 10 - RX
// 11 - DTR
// 12 - RI
// 13 - PWR

// - HC-SR04
// 2  - P1 - far right 180
// 3  - M1
// 4  - P5 - far left 180
// 5  - M5
// 6  - P4 - middle left 150
// 7  - M4
// A1 - P2 - close right 60
// A2 - M2
// A3 - P3 - close left 60
// A4 - M3

// - Assorted
// A0 - temp. sensor
//  9 - manual trigger

// - Unused
// 0  - hardware serial reserved
// 1  - hardware serial reserved
// A5 - free

#include <SoftwareSerial.h>

// runtime options
#define DEBUG true  // run in debug mode
#define MANUAL false // manual control mode
#define MANUAL_TRIGGER true // trigger uptade via button
#define TIMED true  // internally timed update - NOT a good idea, used for testing only
#define INTERVAL 1000*3600 // 1h interval
#define HCTO 15000  // HC-SR40 distance sensor
#define SIMTO 60000 // sim808 general timeout
#define MAXERRS 5   // consecutive error tolerance

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

// manual state trigger
#define GO 9  // trigger pin

// carrier data for GSM network
String PIN="4448";
String APN="gprs0.vip.hr";

// server data
String SERVER="http://www.mikrotron.hr/ecotronserver/upload?stationId=alfa";
String gpsdata="";
String gpsp="&gpsInfo=";
String batp="&batInfo=";

// generic strings for lazy people
String ok="OK";

// HC-SR40 pinout data
int marcos[]={3,A2,A4,7,5};
int polos[]={2,A1,A3,6,4};
int states[]={-1,-1,-1,-1,-1};

boolean repflag=false;
unsigned long int lastCall=0;

void setup(){
  pinsetup();
  Serial.begin(9600);
  sim808.begin(9600);
  sim808su();            // start up
  sim808wu();            // wake up
}

void loop(){
  if(!digitalRead(RI)){
    //ToDo: handle dead module - not in a hurry because the device can't recover from that on its own
    debugPrintln(F("sim808 not on!"));
  }
  debugStream();
  if(!repflag&&digitalRead(RI)&&!MANUAL){
    //ToDo: wrap this
    Serial.println("test "+PIN);
    if(!simcheck())return;else Serial.println(F("sim started!"));
    if(!initgprs())return;else Serial.println(F("gprs started!"));
    if(!setupgprs())return;else Serial.println(F("gprs configured!"));
    repflag=true;
  }
  if(repflag&&TIMED&&(millis()-lastCall>INTERVAL||lastCall==0)){
    //ToDo: wrap this
    checkResp(F("at+cbc"),30,1500,ok);
    delay(5000);
    report();
    delay(5000);
    lastCall=millis();
  }
  if(repflag&&MANUAL_TRIGGER){
    //ToDo: wrap this
    if(digitalRead(GO))return;
    checkResp(F("at+cbc"),30,1500,ok);
    delay(5000);
    report();
    delay(5000);
  }
  //ToDo: various recovery methods from unexpected events
  // -- uncalled for restarts
  // -- network issues
  // -- dead sensors
}
