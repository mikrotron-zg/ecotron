boolean simcheck(){
  if(!(ATcheck()&&echocheck()))return false;
  int rdy=simStatecode();
  if(!rdy)return true;
  if(rdy==2)return false;
  if(rdy==1)return unlocksim();
}

boolean ATcheck(){
  if(DEBUG)Serial.print("AT ");
  return checkResp("at",20,1000,"OK");
}

boolean echocheck(){
  if(DEBUG)Serial.print("echo ");
  return checkResp("ate0",20,1000,"OK");
}

boolean unlocksim(){
  if(DEBUG)Serial.print("unlock ");
  return checkResp("at+cpin="+PIN,60,5000,"READY");
}
