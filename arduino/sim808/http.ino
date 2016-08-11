boolean report(){
  if(!initHTTP()){stopHTTP();return false;}
  if(!setCID())return false;
  getGPSinfo();
  boolean flag=true;
  if(!setURL())flag=false;
  if(!flag)return flag;
  if(!GETaction())return false;
  if(!stopHTTP())return false;
  return true;
}

boolean initHTTP(){
  debugPrint("HTTP start ");
  return checkResp("at+httpinit",20,1000,"OK");
}

boolean setCID(){
  debugPrint("CID ");
  return checkResp("at+httppara=\"CID\",1",20,1000,"OK");
}

boolean setURL(){
  String cmd="at+httppara=\"URL\",\"";
  streamString(cmd);
  streamString(SERVER);
  streamString(gpsp);
  streamString(gpsdata);
  return checkResp("\"",20,3000,"OK");
}

boolean GETaction(){
  debugPrint("GET ");
  return checkResp("at+httpaction=0",40,5000,"OK");
}

boolean stopHTTP(){
  debugPrint("HTTP stop ");
  return checkResp("at+httpterm",20,1000,"OK");
}
