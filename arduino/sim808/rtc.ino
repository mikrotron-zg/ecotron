boolean checkRTC(){
  // check if RTC responds via I2C
  if(!rtc.begin())return false;
  return true;
}

void setAlarmClock(){
  // calculate desired wakeup time to write to the RTC at bed time
  // read now
  dtbuff=rtc.now();
  wu_min[0]=dtbuff.minute();
  wu_hr[0]=dtbuff.hour();
  // wrap over 24h and 60min
  if(wu_min[2]){
    wu_min[0]+=wu_min[1];
    wu_hr[0]+=(wu_min[0])>59;
  }
  if(wu_hr[2])wu_hr[0]+=wu_hr[1];
  wu_min[0]%=60;
  wu_hr[0]%=24;
  debugPrintln("Current time "+String(dtbuff.hour())+":"+String(dtbuff.minute()));
  debugPrintln("Offset " + String(wu_hr[1])+":"+String(wu_min[1]));
  debugPrintln("Wake up time "+String(wu_hr[0])+":"+String(wu_min[0]));
}

//void debugRTC(){
//  // to be called during sleep cycle debugging - set custom wakeup interval here
//  dumpNow();
//  wu_hr[1]=0;
//  wu_min[1]=2;
//  wu_hr[2]=0;
//  wu_min[2]=1;
//  hibernate();
//}

void standardRTCSetup(){
  // to be called as sleep interval configuration during normal device operation
  wu_hr[1]=11;
  wu_hr[2]=1;
  wu_min[1]=58;
  wu_min[2]=1;
}

void hibernate(){
  // set the clock, shut stuff down
  setAlarmClock();
  sim808off();
  sim808sleep();
  ssOff();
  hcshut();
  dtbuff=rtc.now();
  // drop the interrupt flag
  hflag=false;
  // make sure RTC understands that we've dealt with its alert
  while(!digitalRead(2)){rtc.clearINTStatus();delay(100);}
  debugPrintln(F("INIT SLEEP"));
  // set RTC interrupt time
  rtc.enableInterrupts(wu_hr[0],wu_min[0],0);
  delay(100);
  // unlock sleep functionality
  sleep_enable();
  // make sure interrupt can happen
  attachInterrupt(0, wuCall, LOW);
  // prepare the MCU for sleep
  set_sleep_mode(SLEEP_MODE_PWR_DOWN);
  cli();
  sleep_bod_disable();
  sei();
  power_all_disable();
  // stop MCU
  sleep_cpu();
  // !--code pauses here until interrupt occurs--!
  
  // lock MCU sleep functionality
  sleep_disable();
  // get all the MCU parts powered again
  power_all_enable();
  delay(100);
  // restore pin configuration
  hcopen();
  debugPrint(F("EXIT SLEEP"));
}

void wuCall(){
  // ISR
  sleep_disable();
  detachInterrupt(0);
  hflag=true;
}
