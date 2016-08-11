void getmeasure(int index,int goods){
  unsigned long timeout=millis();
  int good=0;
  double dist=0;
  while(good<goods&&millis()-timeout<HCTO){
    digitalWrite(marcos[index],HIGH);
    delayMicroseconds(10);
    digitalWrite(marcos[index],LOW);
    unsigned long d=pulseIn(polos[index],HIGH);
    if(d<58.2*500&&d>0){
      dist+=(double)d/58.2;
      good++;
    }
  }
  if(good!=goods){states[index]=-1;return;}
  dist/=goods;
  states[index]=(int)dist;
}
