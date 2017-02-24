void getmeasure(int index,int goods){
// measuring the distance as seen by a HC-SR04 sensor - deals with faulty measurements by demanding an amount of believeable ones, returns average
// a believeable measurement is set to anywhere between 0cm and 5m - default operation range of the sensor
  debugPrintln("can "+String(index+1));
  unsigned long timeout=millis();
  int good=0;
  double dist=0;
  while(good<goods&&millis()-timeout<HCTO){
    digitalWrite(triggerPins[index],HIGH);
    delayMicroseconds(10);
    digitalWrite(triggerPins[index],LOW);
    unsigned long d=pulseIn(echoPins[index],HIGH);
    if(d<58.2*500&&d>0){
      dist+=(double)d/58.2;
      debugPrint(String((double)d/58.2)+", ");
      good++;
    }
    delay(2000);
  }
  if(good!=goods){states[index]=-1;return;}
  dist/=goods;
  states[index]=(int)(0.5+dist);
}

void updateStates(){
  // read and update trash can states
  // NOTE: sensor 0 interferes with serial debugger
  int k=0; //||DEBUG;
  for(int i=k;i<5;i++){
    getmeasure(i,5);
    debugPrintln(String(i+1)+": "+String(states[i]));
  }
}
