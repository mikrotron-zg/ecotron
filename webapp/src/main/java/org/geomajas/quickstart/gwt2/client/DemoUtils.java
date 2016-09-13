package org.geomajas.quickstart.gwt2.client;

public class DemoUtils {


	public static String percentage(String val){
    	
    	//FIXME exact lengths will be known after sensors are mounted
    	final double empty = 120;
    	final double full = 40;
    	double measured;
    	
    	try{
    		measured = Double.parseDouble(val);
    	} catch (NumberFormatException e){
    		return "Nepoznati oblik podatka";
    	}
    	if (measured == -1) return "Nema podatka";
    	if (measured <= (full+5)) return "100 %";
    	if (measured >= (empty-5)) return "0 %";
    	
    	double calc = (1 - ((measured-full)/(empty-full)) + 0.05)*10;
    	int rounded = ((int)calc*100)/10;
    	
    	return rounded + " %";
    }
	
	public static int percentageAmount(String val){
		String p = percentage(val);
		return Integer.parseInt(p.substring(0, p.length()-2));
	}
	
	public static String percentageToColor(String val){
		int p = percentageAmount(val);
		if (p<50) return "#036600"; //dark green
		if (p<90) return "#EAC300"; //dark yellow
		return "#C00000"; //red
	}
}
