void pinsetup(){
  pinMode(PWR,OUTPUT);
  pinMode(RI,INPUT);
  pinMode(DTR,OUTPUT);
  pinMode(GO,INPUT_PULLUP);
  for(int i=0;i<5;i++){
    pinMode(marcos[i],OUTPUT);
    digitalWrite(marcos[i],LOW);
    pinMode(polos[i],INPUT);
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
  delay(54);
}

void sim808sleep(){
  digitalWrite(DTR,HIGH);
}
