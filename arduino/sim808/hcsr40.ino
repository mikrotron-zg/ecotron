int getmeasure(int goods){
  unsigned long timeout=millis();
  int good=0;
  double dist=0;
  while(good<goods&&millis()-timeout<HCTO){
    digitalWrite(marcos[0],HIGH);
    delayMicroseconds(10);
    digitalWrite(marcos[0],LOW);
    unsigned long d=pulseIn(polos[0],HIGH);
    if(d<58.2*500&&d>0){
      dist+=(double)d/58.2;
      good++;
    }
  }
  if(good!=goods)return -1;
  dist/=goods;
  return (int)dist;
}
