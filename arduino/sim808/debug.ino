void debugPrintln(String msg){
  if(DEBUG)Serial.println(msg);
}

void debugPrint(String msg){
  if(DEBUG)Serial.print(msg);
}

void debugStream(){
  if(!DEBUG)return;
  if(sim808.available())Serial.write(sim808.read());
  if(Serial.available()){ 
    while(Serial.available()){
      sim808.write(Serial.read());
    }
    sim808.println();
  }
}
