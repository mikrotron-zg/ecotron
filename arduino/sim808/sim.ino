boolean simcheck(){
// full process of checking if the module is good to go and unlocking sim card
  if(!(ATcheck()&&echocheck()))return false;
  int rdy=simStatecode();
  if(!rdy)return true;
  if(rdy==2)return false;
  if(rdy==1)return unlocksim();
}

boolean ATcheck(){
  debugPrint(F("AT "));
  return checkResp(F("at"),20,1000,ok);
}

boolean echocheck(){
  debugPrint(F("echo "));
  return checkResp(F("ate0"),20,1000,ok);
}

boolean unlocksim(){
  debugPrint(F("unlock "));
  return checkResp("at+cpin="+PIN,60,5000,F("READY"));
}
