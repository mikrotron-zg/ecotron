double getR(int pin){
  double adc=(double)analogRead(pin);
  return adc*RESISTOR/(1024-adc);
}

int getT(){
  double R=getR(A0);
  R=log10(R);
  if(R<Rval[1]){
    return (int)(Tval[0]+(Tval[1]-Tval[0])*((Rval[0]-R)/(Rval[0]-Rval[1])));
    }else{
      return (int)(Tval[1]+(Tval[2]-Tval[1])*((Rval[1]-R)/(Rval[1]-Rval[2])));
    }
}
