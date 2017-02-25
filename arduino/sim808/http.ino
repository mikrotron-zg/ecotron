boolean report(){
// reports state to server - full procedure
  if(!initHTTP()){stopHTTP();return false;}
  if(!setCID())return false;
  if(!setREDIR())return false;
  getGPSinfo();
  boolean flag=true;
  if(!setURL())flag=false;
  if(!flag)return flag;
  if(!GETaction())return false;
  if(!stopHTTP())return false;
  return true;
}

boolean initHTTP(){
  debugPrint(F("HTTP start "));
  return checkResp(F("at+httpinit"),20,1000,ok);
}

boolean setCID(){
  debugPrint(F("CID "));
  return checkResp(F("at+httppara=\"CID\",1"),20,1000,ok);
}

boolean setREDIR(){
  debugPrint(F("REDIR "));
  return checkResp(F("at+httppara=\"REDIR\",0"),20,1000,ok);
}

boolean setURL(){
// streams a very long url which contains all the report data
// url can't be held in memory entirely so it is streamed to the sim808 in pieces
  String cmd="at+httppara=\"URL\",\"";
  String bat=getBatStat();
  streamString(cmd);
  streamString(SERVER);
  streamString(gpsp);
  streamString(gpsdata);
  streamString(batp);
  streamString(bat);
  //for(int i=DEBUG;i<5;i++){
  for(int i=0;i<5;i++){
    streamString("&can"+String(i+1)+"="+String(states[i]));
  }
  streamString("&temp="+String(getT()));
  return checkResp("\"",20,3000,ok);
}

boolean GETaction(){
  debugPrint(F("GET "));
  return checkResp(F("at+httpaction=0"),40,20000,ok);
}

boolean stopHTTP(){
  debugPrint(F("HTTP stop "));
  return checkResp(F("at+httpterm"),20,1000,ok);
}
