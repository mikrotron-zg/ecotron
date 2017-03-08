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
// 0  - P1 - far right 180 ---! overlaps with hardware serial - disconnect can0 sensor before debugging !---
// 1  - M1
// 4  - P5 - far left 180
// 5  - M5
// 6  - P4 - middle left 150
// 7  - M4
// A1 - P2 - close right 60
// A2 - M2
// A3 - P3 - close left 60
// 3  - M3

// - Assorted
// A0 - temp. sensor
// A5 - I2C SDA
// A4 - I2C SCL
//  9 - sensor power control

#include <avr/sleep.h>
#include <avr/power.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include "DS1337.h"
#include "SeeeduinoStalker.h"

// runtime options
#define DEBUG false     // run in debug mode
#define MANUAL false    // manual control mode
#define CHECKHC false   // sensor checking loop
#define CHECKRTC false  // rtc checking loop
#define HCTO 15000      // HC-SR40 distance sensor timeout
#define SIMTO 60000     // sim808 general timeout
#define MAXERRS 5       // consecutive error tolerance

// software serial to sim808
SoftwareSerial sim808(10,8);

// sim808 controls
#define RI 12    // status
#define PWR 13   // soft power switch
#define DTR 11   // sleep

// stalker & RTC control
// brown out detection disabling function - copy pasta
#define sleep_bod_disable() \
{ \
  uint8_t tempreg; \
  __asm__ __volatile__("in %[tempreg], %[mcucr]" "\n\t" \
                       "ori %[tempreg], %[bods_bodse]" "\n\t" \
                       "out %[mcucr], %[tempreg]" "\n\t" \
                       "andi %[tempreg], %[not_bodse]" "\n\t" \
                       "out %[mcucr], %[tempreg]" \
                       : [tempreg] "=&d" (tempreg) \
                       : [mcucr] "I" _SFR_IO_ADDR(MCUCR), \
                         [bods_bodse] "i" (_BV(BODS) | _BV(BODSE)), \
                         [not_bodse] "i" (~_BV(BODSE))); \
}
DS1337 rtc;
DateTime dtbuff;
Stalker stalker;
boolean hflag=false;
// wakeup timing [when to wake up, interval, interval on/off flag*] *flag = 0 --> does not add to the interval
int wu_hr[3];
int wu_min[3];

// temperature sensor
#define TMP A0   // signal pin
#define RESISTOR 10000  // sensor internal resistor value
double Rval[]={5.301,4.48,3.62};
double Tval[]={-40,0,50};

// carrier data for GSM network
//String PIN="8215";
//String PIN="7312";
String PIN="2147";
String APN="internet.ht.hr";
//String APN="mobileinternet.tele2.hr";
//String APN="web.htgprs";

// server data
String SERVER="http://www.mikrotron.hr/ecotronserver/upload?stationId=delta";
String gpsdata="";
String gpsp="&gpsInfo=";
String batp="&batInfo=";

// generic strings for memory saving
String ok="OK";

// HC-SR40 trigger/echo pins and states
//int marcos[]={1,A2,3,7,5};
//int polos[]={0,A1,A3,6,4};
int triggerPins[]={0,A1,3,6,4};
int echoPins[]={1,A2,A3,7,5};
int states[]={-1,-1,-1,-1,-1};

void setup(){
  // configure atmega pins
  pinsetup();
  // set initial send flag (so that device sends when first turned on, for debug purposes)
  hflag=true;
  // shut down sensor shield
  ssOff();
  // start serial in debug mode
  if(DEBUG)Serial.begin(9600);
  delay(500);
  // loop sensor check on demand
  if(CHECKHC){while(1)debugHCSR04();}
  // make sure RTC works if device isn't started in manual mode
  if(!checkRTC()&&!MANUAL){debugPrintln(F("RTC DOWN"));while(1);}else{standardRTCSetup();}
  // loop RTC check on demand
  if(CHECKRTC){while(1)debugRTC();}
  // open serial to sim808
  sim808.begin(9600);
  // power sensor shield up
  ssOn();
  // start up sim808
  sim808su();
  sim808wu();
  //if (MANUAL) verboseErrors();
}

void loop(){
  // handle interrupt
  if(hflag&&!MANUAL){
    // turn stuff on
    ssOn();
    sim808su();
    sim808wu();
    // check if sim808 works
    checkModule();
    // ping trash cans
    updateStates();
    // ignore SIM connection error and go back to sleep
    if(!connectSIM())return;
    // report state if connection is successful
    report();
  }
  if(!MANUAL)hibernate();else debugStream();
  //ToDo: various recovery methods from unexpected events
  // -- uncalled for restarts
  // -- network issues
  // -- dead sensors
}
