#include <SoftwareSerial.h>

SoftwareSerial sim808(10,3);

#define DEBUG true

String PIN ="4448";

void setup() {
  // put your setup code here, to run once:
  pinMode(13,OUTPUT);
  pinMode(12,INPUT);
  pinMode(11,OUTPUT);
  Serial.begin(57600);
  digitalWrite(11,LOW);
  delay(54);
  sim808.begin(9600);
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
  if(!digitalRead(9)){
    Serial.println("toggled");
    digitalWrite(13,HIGH);
    delay(2050);
    digitalWrite(13,LOW);
  }
  if(!digitalRead(8)){
    Serial.println("test "+PIN);
    if(!simcheck())return;else Serial.println("sim started!");
    if(!initgprs())return;else Serial.println("gprs started!");
  }
}
