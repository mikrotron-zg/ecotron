boolean initgprs(){
  if(!regcheck())return false;
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
  if(!contype())return false;
  if(!setapn())return false;
  if(!gprsON())return false;
  if(!checkgprs())return false;
  return true;
}

boolean contype(){
  debugPrint("contype ");
  return checkResp("at+sapbr=3,1,\"Contype\",\"GPRS\"",20,1000,"OK");
}

boolean setapn(){
  debugPrint("set apn ");
  return checkResp("at+sapbr=3,1,\"APN\",\""+APN+"\"",20,1000,"OK");
}

boolean gprsON(){
  debugPrint("gprs ON ");
  return checkResp("at+sapbr=1,1",20,1000,"OK");
}

boolean checkgprs(){
  debugPrint("settings ");
  return checkResp("at+sapbr=2,1",60,1000,"OK");
}

boolean regcheck(){
  debugPrint("net reg ");
  return checkResp("at+creg?",40,2000,"0,1");
}

boolean attcheck(){
  debugPrint("gprs attach ");
  return checkResp("at+cgatt?",40,2000,": 1");
}

boolean shutIP(){
  debugPrint("IP shut ");
  return checkResp("at+cipshut",20,1000,"SHUT OK");
}

boolean IPstack(){
  debugPrint("IP stat ");
  return checkResp("at+cipstatus",30,1000,"IP INITIAL");
}

boolean nomux(){
  debugPrint("mux off ");
  return checkResp("at+cipmux=0",20,1000,"OK");
}

boolean nettask(){
  debugPrint("network ");
  return checkResp("at+cstt=\""+APN+"\"",60,10000,"OK");
}

boolean linkgprs(){
  debugPrint("gprs link ");
  return checkResp("at+ciicr",60,10000,"OK");
}

boolean checkIP(){
  debugPrint("IP ");
  return !checkResp("at+cifsr",20,5000,"ERROR");
}
