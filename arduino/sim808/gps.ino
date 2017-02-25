void getGPSinfo(){
  // full procedure for turning gps on, getting data and turning it off again
  debugPrint(F("GPS on "));
  if(!checkResp(F("at+cgnspwr=1"),20,1000,ok))gpsdata="err";
  debugPrint(F("GPS INFO "));
  String ret="0";
  // TODO: introduce safety timeout here, or GPS may get into endless loop
  do{
    gpsdata=parseinfo();
    debugPrintln("bot "+ret);
    delay(500);
  }while(gpsdata[2]!='1');
  debugPrint(F("GPS off "));
  if(!checkResp(F("at+cgnspwr=1"),20,1000,ok))debugPrintln(F("GPS PWR ERR"));
}

String parseinfo(){
// parsing function for gps data - deals with limited string length, returns only a data string
  int timeout=4000;
  debugPrint(F("GPS parse "));
  String state;
  flushsim();
  sim808.println(F("at+cgnsinf"));
  String s1=getString(60,timeout);
  String s2=getString(60,timeout);
  state=s1+s2;
  debugPrintln(state);
  if(inString(state,ok)){
    debugPrintln(ok);
    state=state.substring(state.indexOf(" ")+1);
    state=state.substring(0,state.indexOf("\r"));
    debugPrintln("got "+state);
    return state;
  }else{
    debugPrintln(F("parse ERR"));
    return "err";
  }
}
