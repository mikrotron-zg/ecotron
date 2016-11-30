void pinsetup(){
  PORTD |= 0x04; 
  DDRD &=~ 0x04;
  pinMode(PWR,OUTPUT);
  pinMode(RI,INPUT);
  pinMode(DTR,OUTPUT);
  int k=0||DEBUG;
  for(int i=k;i<5;i++){
    pinMode(marcos[i],OUTPUT);
    digitalWrite(marcos[i],LOW);
    pinMode(polos[i],INPUT);
  }
  pinMode(2,INPUT_PULLUP);
  pinMode(3,OUTPUT);
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
  while(!digitalRead(RI)){if(millis()-t>SIMTO){debugPrintln(F("sim timeout"));while(1);}}
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
  while(digitalRead(RI)){if(millis()-t>SIMTO){debugPrintln(F("sim timeout"));while(1);}}
  debugPrintln("OFF");
}

void sim808wu(){
  digitalWrite(DTR,LOW);
  delay(100);
  digitalWrite(DTR,HIGH);
  // TODO: AT+CFUN=1 (full functionality)
}

void sim808sleep(){
  digitalWrite(DTR,HIGH); // TODO: remove
  // TODO: AT+CFUN=0 (deep sleep)
  // TODO: AT+CSCLK=1
}

void ssOn(){
  digitalWrite(3,LOW);
  delay(100);
}

void ssOff(){
  digitalWrite(3,HIGH);
}
