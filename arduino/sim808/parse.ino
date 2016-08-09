void flushsim(){
  if(DEBUG)Serial.println("flushing "+String(sim808.available())+" bytes");
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
  if(DEBUG)Serial.println(String(got)+","+ret);
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
    if(DEBUG)Serial.println("OK");
    return true;
  }else{
    if(DEBUG)Serial.println("ERR");
    return false;
  }
}

int simStatecode(){
  String state;
  flushsim();
  sim808.println("at+cpin?");
  state=getString(30,3000);
  if(inString(state,"READY")){
    if(DEBUG)Serial.println("SIM RDY");
    return 0;
  }
  if(inString(state,"SIM PIN")){
    if(DEBUG)Serial.println("PIN REQ");
    return 1;
  }
  if(DEBUG)Serial.println("SIM ERR");
  return 2;
}
