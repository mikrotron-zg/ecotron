boolean simcheck(){
  if(!(ATcheck()&&echocheck()))return false;
  int rdy=simStatecode();
  if(!rdy)return true;
  if(rdy==2)return false;
  if(rdy==1)return unlocksim();
}

boolean ATcheck(){
  debugPrint("AT ");
  return checkResp("at",20,1000,"OK");
}

boolean echocheck(){
  debugPrint("echo ");
  return checkResp("ate0",20,1000,"OK");
}

boolean unlocksim(){
  debugPrint("unlock ");
  return checkResp("at+cpin="+PIN,60,5000,"READY");
}
