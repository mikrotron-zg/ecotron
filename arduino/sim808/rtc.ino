boolean checkRTC(){
  if(!rtc.begin())return false;
  return true;
}

void setAlarmClock(){
  dtbuff=rtc.now();
  wu_min[0]=dtbuff.minute();
  wu_hr[0]=dtbuff.hour();
  if(wu_min[2]){
    wu_min[0]+=wu_min[1];
    wu_hr[0]+=(wu_min[0])>59;
  }
  if(wu_hr[2])wu_hr[0]+=wu_hr[1];
  wu_min[0]%=60;
  wu_hr[0]%=24;
  debugPrintln(String(wu_hr[0])+":"+String(wu_min[0]));
}

void debugRTC(){
  dumpNow();
  standardRTCSetup();
//  wu_hr[1]=0;
//  wu_min[1]=2;
//  wu_hr[2]=0;
//  wu_min[2]=1;
  hibernate();
}

void standardRTCSetup(){
  wu_hr[1]=3;
  wu_hr[2]=1;
  wu_min[1]=0;
  wu_min[2]=0;
}

void hibernate(){
  setAlarmClock();
  //sim808off(); // this may put all SIM808 pins to LOW, while ATMEGA pins remain HIGH -> power drain!
  ssOff();
  dtbuff=rtc.now();
  hflag=false;
  while(!digitalRead(2)){rtc.clearINTStatus();delay(100);}
  debugPrintln(F("INIT SLEEP"));
  rtc.enableInterrupts(wu_hr[0],wu_min[0],0);
  sim808sleep();
  int minexact=dtbuff.minute()+2;
  delay(100);
  sleep_enable();
  attachInterrupt(0, wuCall, LOW);
  set_sleep_mode(SLEEP_MODE_PWR_DOWN);
  cli();
  sleep_bod_disable();
  sei();
  power_all_disable();
  sleep_cpu();
  // code pauses here until interrupt occurs
  sleep_disable();
  power_all_enable();
  delay(100);
  debugPrint(F("EXIT SLEEP"));
}

void wuCall(){
  sleep_disable();
  detachInterrupt(0);
  hflag=true;
}
