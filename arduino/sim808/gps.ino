// number and timeout for GPS to fix (lock)
#define MAX_GPS_TRIES 30
#define GPS_RETRY_DELAY 1000

void getGPSinfo(){
  // full procedure for turning gps on, getting data and turning it off again
  debugPrint(F("GPS on "));
  if(!checkResp(F("at+cgnspwr=1"),20,1000,ok))gpsdata="err";
  debugPrint(F("GPS INFO "));
  String ret="0";
  // safety count prevents GPS getting into endless loop when unreliable
  int gpstries = 0;
  do{
    delay(GPS_RETRY_DELAY);
    gpsdata=parseinfo();
    debugPrintln("bot "+ret);
  }while(gpsdata[2]!='1' && ++gpstries < MAX_GPS_TRIES);
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
