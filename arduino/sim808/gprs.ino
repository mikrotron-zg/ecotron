boolean initgprs(){
// whole procedure for starting up gprs
  int errcount=0;
  while(!regcheck()&&errcount<MAXERRS)errcount++;
  if(errcount==MAXERRS)return false;
  if(!attcheck())return false;
  if(!shutIP())return false;
  if(!IPstack())return false;
  if(!nomux())return false;
  if(!nettask())return false;
  if(!linkgprs())return false;
  if(!checkIP())return false;
  return true;
}

boolean setupgprs(){
// whole procedure for setting gprs up, to be called after initgprs
  if(!contype())return false;
  if(!setapn())return false;
  if(!gprsON())return false;
  if(!checkgprs())return false;
  return true;
}

boolean contype(){
  debugPrint(F("contype "));
  return checkResp(F("at+sapbr=3,1,\"Contype\",\"GPRS\""),20,1000,ok);
}

boolean setapn(){
  debugPrint(F("set apn "));
  return checkResp("at+sapbr=3,1,\"APN\",\""+APN+"\"",20,1000,ok);
}

boolean gprsON(){
  debugPrint(F("gprs ON "));
  return checkResp(F("at+sapbr=1,1"),20,1000,ok);
}

boolean checkgprs(){
  debugPrint(F("settings "));
  return checkResp(F("at+sapbr=2,1"),60,1000,ok);
}

boolean regcheck(){
  debugPrint(F("net reg "));
  return checkResp(F("at+creg?"),40,4000,F("0,1"));
}

boolean attcheck(){
  debugPrint(F("gprs attach "));
  return checkResp(F("at+cgatt?"),40,2000,F(": 1"));
}

boolean shutIP(){
  debugPrint(F("IP shut "));
  return checkResp(F("at+cipshut"),20,1000,F("SHUT OK"));
}

boolean IPstack(){
  debugPrint(F("IP stat "));
  return checkResp(F("at+cipstatus"),30,1000,F("IP INITIAL"));
}

boolean nomux(){
  debugPrint(F("mux off "));
  return checkResp(F("at+cipmux=0"),20,1000,ok);
}

boolean nettask(){
  debugPrint(F("network "));
  return checkResp("at+cstt=\""+APN+"\"",60,10000,ok);
}

boolean linkgprs(){
  debugPrint(F("gprs link "));
  // CHECKME: this only works if at+cipstatus returns IP START
  return checkResp(F("at+ciicr"),60,10000,ok);
}

boolean checkIP(){
  debugPrint(F("IP "));
  return !checkResp(F("at+cifsr"),20,5000,F("ERROR"));
}
