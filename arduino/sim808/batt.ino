String getBatStat(){
// get battery status as per stalker spec <0-not charging | 1-charging | 2-full>,<% charged(0-100)>,<voltage in milivolts>
  int timeout=1000;
  debugPrint(F("batstat "));
  String state=String(stalker.readChrgStatus())+",";
  float v=1000*stalker.readBattery();
  int pct=100*(v-3000)/1200;
  state+=String(pct)+","+String((int)v);
  return state;
}
