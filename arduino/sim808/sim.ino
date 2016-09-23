void checkModule(){
  if(!digitalRead(RI)){
// if low RI is detected in debug mode, check if it's low because of incoming call signal (120ms down) - if not assume it's off
    delay(200);
    if(digitalRead(RI))return;
// if assumed off, attempt to reboot MAXERRS times - if this fails, brick thyself and wait for a technician to unbrick thine pitiful arse
    debugPrintln(F("sim808 not on!"));
    int fails=0;
    while(fails<MAXERRS){
      sim808su();
      sim808wu();
      if(ATcheck())return;else fails++;
    }
    debugPrintln(F("sim808 dead?"));
    while(1);
  }
}

boolean simcheck(){
// full process of checking if the module is good to go and unlocking sim card
  if(!(ATcheck()&&echocheck()))return false;
  int rdy=simStatecode();
  if(!rdy)return true;
  if(rdy==2)return false;
  if(rdy==1)return unlocksim();
}

boolean connectSIM(){
  debugPrintln(F("init..."));
  if(!simcheck())return false;else debugPrintln(F("sim started!"));
  if(!initgprs())return false;else debugPrintln(F("gprs started!"));
  if(!setupgprs())return false;else debugPrintln(F("gprs configured!"));
  return true;
}

boolean ATcheck(){
  debugPrint(F("AT "));
  return checkResp(F("at"),20,1000,ok);
}

boolean echocheck(){
  debugPrint(F("echo "));
  return checkResp(F("ate0"),20,1000,ok);
}

boolean unlocksim(){
  debugPrint(F("unlock "));
  return checkResp("at+cpin="+PIN,60,5000,F("READY"));
}
