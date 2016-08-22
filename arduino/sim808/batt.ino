String getBatStat(){
// get battery status as per AT command cbc <0-not charging | 1-charging | 2-full>,<% charged(0-100)>,<voltage in milivolts>
  int timeout=1000;
  debugPrint(F("batstat "));
  String state;
  flushsim();
  sim808.println(F("at+cbc"));
  state=getString(40,timeout);
  debugPrintln(state);
  if(inString(state,ok)){
    debugPrintln(ok);
    state=state.substring(state.indexOf(" ")+1);
    state=state.substring(0,state.indexOf("\r"));
    debugPrintln("got "+state);
    return state;
  }else{
    debugPrintln(F("bat ERR"));
    return "err";
  }
}
