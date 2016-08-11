void flushsim(){
  debugPrintln("flushing "+String(sim808.available())+" bytes");
  while(sim808.available())sim808.read();
  return;
}

String getString(int len, int timeout){
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
  if(source.indexOf(piece)>-1)return true;else return false;
}

boolean checkResp(String cmd, int len, int timeout, String okflag){
  String state;
  flushsim();
  sim808.println(cmd);
  state=getString(len,timeout);
  if(inString(state,okflag)){
    debugPrintln("OK");
    return true;
  }else{
    debugPrintln("ERR");
    return false;
  }
}

void streamString(String s){
  for(int i=0;i<s.length();i++)sim808.write(s[i]);
}

int simStatecode(){
  String state;
  flushsim();
  sim808.println("at+cpin?");
  state=getString(30,3000);
  if(inString(state,"READY")){
    debugPrintln("SIM RDY");
    return 0;
  }
  if(inString(state,"SIM PIN")){
    debugPrintln("PIN REQ");
    return 1;
  }
  debugPrintln("SIM ERR");
  return 2;
}
