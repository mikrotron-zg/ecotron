void getGPSinfo(){
  debugPrint("GPS on ");
  if(!checkResp("at+cgnspwr=1",20,1000,"OK"))gpsdata="err";
  debugPrint("GPS INFO ");
  String ret="0";
  do{
    gpsdata=parseinfo();
    debugPrintln("bot "+ret);
    delay(500);
  }while(gpsdata[0]=='0');
  debugPrint("GPS off ");
  if(!checkResp("at+cgnspwr=1",20,1000,"OK"))Serial.println("GPS PWR ERR");
}

String parseinfo(){
  int timeout=4000;
  debugPrint("GPS info ");
  String state;
  flushsim();
  sim808.println("at+cgnsinf");
  String s1=getString(60,timeout);
  String s2=getString(60,timeout);
  state=s1+s2;
  debugPrintln(state);
  if(inString(state,"OK")){
    debugPrintln("OK");
    state=state.substring(state.indexOf(" ")+1);
    state=state.substring(0,state.indexOf("\r"));
    debugPrintln("got "+state);
    return state;
  }else{
    debugPrintln("ERR");
    return "err";
  }
}
