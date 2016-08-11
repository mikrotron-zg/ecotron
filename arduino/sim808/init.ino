void pinsetup(){
  pinMode(PWR,OUTPUT);
  pinMode(RI,INPUT);
  pinMode(DTR,OUTPUT);
  for(int i=0;i<5;i++){
    pinMode(marcos[i],OUTPUT);
    digitalWrite(marcos[i],LOW);
    pinMode(polos[i],INPUT);
  }
}

void sim808su(){
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

void sim808wu(){
  digitalWrite(DTR,LOW);
  delay(54);
}

void sim808sleep(){
  digitalWrite(DTR,HIGH);
}
