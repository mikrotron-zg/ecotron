boolean initgprs(){
  if(!regcheck())return false;
  if(!attcheck())return false;
  if(!shutIP())return false;
  if(!IPstack())return false;
  if(!nomux())return false;
  return true;
}

boolean regcheck(){
  if(DEBUG)Serial.print("net reg ");
  return checkResp("at+creg?",40,2000,"0,1");
}

boolean attcheck(){
  if(DEBUG)Serial.print("gprs attach ");
  return checkResp("at+cgatt?",40,2000,": 1");
}

boolean shutIP(){
  if(DEBUG)Serial.print("IP shut ");
  return checkResp("at+cipshut",20,1000,"SHUT OK");
}

boolean IPstack(){
  if(DEBUG)Serial.print("IP stat ");
  return checkResp("at+cipstatus",30,1000,"IP INITIAL");
}

boolean nomux(){
  if(DEBUG)Serial.print("mux off ");
  return checkResp("at+cipmux=0",20,1000,"OK");
}
