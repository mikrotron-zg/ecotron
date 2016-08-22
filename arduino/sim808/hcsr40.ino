void getmeasure(int index,int goods){
// measuring the distance as seen by a HC-SR04 sensor - deals with faulty measurements by demanding an amount of believeable ones, returns average
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
