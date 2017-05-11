void pinsetup(){
  // full pin configuration
  PORTD |= 0x04; 
  DDRD &=~ 0x04;
  pinMode(PWR,OUTPUT);
  pinMode(RI,INPUT);
  pinMode(DTR,OUTPUT);
  hcopen();
  pinMode(2,INPUT_PULLUP);
  pinMode(9,OUTPUT);
}

void hcshut(){
  // make data pins behave while sleeping
  int k=0; //||DEBUG;
  for(int i=k;i<5;i++){
    pinMode(triggerPins[i],OUTPUT);
    digitalWrite(triggerPins[i],LOW);
    pinMode(echoPins[i],OUTPUT);
    digitalWrite(triggerPins[i],LOW);
  }
}

void hcopen(){
  // make data pins misbehave again
  Serial.println("Entered hcopen()");
  Serial.println("hflag=" + String(hflag));
  int k=0; //||DEBUG;
  for(int i=k;i<5;i++){
    pinMode(triggerPins[i],OUTPUT);
    digitalWrite(triggerPins[i],LOW);
    pinMode(echoPins[i],INPUT);
  }
}

void sim808su(){
// get sim808 to start up
  if(digitalRead(RI)){
    debugPrint(F("restarting sim808..."));
    sim808off();
    delay(2000);
    sim808on();
  }else{
    debugPrint(F("starting sim808..."));
    sim808on();
  }
  delay(2000);
}

void sim808on(){
// do the power toggle, lock until sim says it's on or timeout
  unsigned long int t=millis();
  digitalWrite(PWR,LOW);
  delay(500);
  digitalWrite(PWR,HIGH);
  delay(2050);
  digitalWrite(PWR,LOW);
  while(!digitalRead(RI)){if(millis()-t>SIMTO){debugPrintln(F("sim ON timeout"));while(1);}}
  debugPrintln("ON");
}

void sim808off(){
// do the power toggle, lock until sim says it's off or timeout
  unsigned long int t=millis();
  digitalWrite(PWR,LOW);
  delay(500);
  digitalWrite(PWR,HIGH);
  delay(2050);
  digitalWrite(PWR,LOW);
  while(digitalRead(RI)){if(millis()-t>SIMTO){debugPrintln(F("sim OFF timeout"));while(1);}}
  debugPrintln("OFF");
}

void sim808wu(){
  digitalWrite(DTR,LOW);
  delay(100);
}

void sim808sleep(){
  delay(100);
  digitalWrite(DTR,HIGH);
}

void ssOn(){
  digitalWrite(9,HIGH);
  delay(100);
}

void ssOff(){
  digitalWrite(9,LOW);
}
