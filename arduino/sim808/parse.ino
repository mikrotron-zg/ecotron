void flushsim(){
// discard whatever's waiting in serial
  debugPrintln("flushing "+String(sim808.available())+" bytes");
  while(sim808.available())sim808.read();
  return;
}

String getString(int len, int timeout){
// read a string until timeout or specified length is reached
  char b[len+1];
  unsigned long int t=millis();
  while(millis()-t<timeout&&sim808.available()<len){};
  int got=sim808.available();
  for(int i=0;i<got;i++){b[i]=sim808.read();}
  b[got]='\0';
  String ret(b);
  debugPrintln(String(got)+","+ret);
  return ret;
}

boolean inString(String source, String piece){
// check if string contains another string
  if(source.indexOf(piece)>-1)return true;else return false;
}

boolean checkResp(String cmd, int len, int timeout, String okflag){
// wraps the process of sending a command to sim808 and checking if it produces desired behaviour
  String state;
  flushsim();
  sim808.println(cmd);
  state=getString(len,timeout);
  if(inString(state,okflag)){
    debugPrintln(F("OK"));
    return true;
  }else{
    debugPrintln(F("ERR"));
    return false;
  }
}

void streamString(String s){
// sends a string from memory to the sim808 char by char
  for(int i=0;i<s.length();i++)sim808.write(s[i]);
}

int simStatecode(){
// querries and parses sim state, returns custom codes for different states
  String state;
  flushsim();
  sim808.println(F("at+cpin?"));
  state=getString(30,3000);
  if(inString(state,F("READY"))){
    debugPrintln(F("SIM RDY"));
    return 0;
  }
  if(inString(state,F("SIM PIN"))){
    debugPrintln(F("PIN REQ"));
    return 1;
  }
  debugPrintln(F("SIM ERR"));
  return 2;
}
