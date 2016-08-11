// Ecotron node firmware
// Tomislav Mamić for Mikrotron d.o.o 2016.

#include <SoftwareSerial.h>

// runtime options
#define DEBUG true  // run in debug mode
#define HCTO 15000  // HC-SR40 distance sensor

// software serial to sim808
SoftwareSerial sim808(10,3);

// sim808 controls
#define RI 12    // status
#define PWR 13   // soft power switch
#define DTR 11   // sleep

// carrier data for GSM network
String PIN="4448";
String APN="gprs0.vip.hr";

// server data
String SERVER="http://www.mikrotron.hr/ecotronserver/upload?stationId=alfa";
String gpsdata="";
String gpsp="&gpsInfo=";

// HC-SR40 pinout data
int marcos[]={A0,A2,A4,7,5};
int polos[]={A1,A3,A5,6,4};

boolean repflag=false;

void setup() {
  pinMode(13,OUTPUT);
  pinMode(12,INPUT);
  pinMode(11,OUTPUT);
  pinMode(marcos[0],OUTPUT);
  pinMode(polos[0],INPUT);
  Serial.begin(9600);
  digitalWrite(11,LOW);
  digitalWrite(marcos[0],LOW);
  delay(54);
  sim808.begin(9600);
  if(digitalRead(RI)){
    debugPrint("restarting sim808...");
    digitalWrite(PWR,HIGH);
    delay(2050);
    digitalWrite(PWR,LOW);
    delay(1000);
    digitalWrite(PWR,HIGH);
    delay(2050);
    digitalWrite(PWR,LOW);
  }else{
    debugPrint("starting sim808...");
    digitalWrite(PWR,HIGH);
    delay(2050);
    digitalWrite(PWR,LOW);
  }
  delay(2000);
}

bool sync(){
  String msg=sim808.readStringUntil('K');
  return msg=="O";
}

void loop() {
  // put your main code here, to run repeatedly:
  if(!digitalRead(12)){
    Serial.println("sim808 not on!");
    delay(2000);
  }
  if(sim808.available())Serial.write(sim808.read());
  if(Serial.available()){ 
    while(Serial.available()){
      sim808.write(Serial.read());
    }
    sim808.println();
  }
  if(!repflag&&digitalRead(RI)){
    Serial.println("test "+PIN);
    if(!simcheck())return;else Serial.println("sim started!");
    if(!initgprs())return;else Serial.println("gprs started!");
    if(!setupgprs())return;else Serial.println("gprs configured!");
    repflag=true;
  }
  if(repflag){
    delay(5000);
    report();
    delay(5000);
  }
  //Serial.println(String(getmeasure(10))+"cm");
  //delay(1000);
}
