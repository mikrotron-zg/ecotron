void debugPrintln(String msg){
  if(DEBUG)Serial.println(msg);
}

void debugPrint(String msg){
  if(DEBUG)Serial.print(msg);
}

void debugStream(){
// pass serial communication to and from sim808 for direct AT command entry
  if(!DEBUG)return;
  if(sim808.available())Serial.write(sim808.read());
  if(Serial.available()){ 
    while(Serial.available()){
      sim808.write(Serial.read());
    }
    sim808.println();
  }
}

void debugHCSR04(){
// measure and report HCSR04 readings
  int k=0||DEBUG;
  ssOn();
  for(int i=k;i<5;i++){
    getmeasure(i,5);
    Serial.println(String(i)+": "+String(states[i]));
  }
  ssOff();
}

void dumpNow(){
// read and report RTC date-time
  dtbuff = rtc.now();
  Serial.print(dtbuff.year(), DEC);
  Serial.print('/');
  Serial.print(dtbuff.month(), DEC);
  Serial.print('/');
  Serial.print(dtbuff.date(), DEC);
  Serial.print(' ');
  Serial.print(dtbuff.hour(), DEC);
  Serial.print(':');
  Serial.print(dtbuff.minute(), DEC);
  Serial.print(':');
  Serial.print(dtbuff.second(), DEC);
  Serial.println();
}
