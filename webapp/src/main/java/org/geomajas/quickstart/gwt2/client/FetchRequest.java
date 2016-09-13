package org.geomajas.quickstart.gwt2.client;

import java.util.Set;

import org.vaadin.gwtgraphics.client.VectorObject;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.DialogBox;

public class FetchRequest extends RequestBuilder implements RequestCallback {
	static String urlBase = "http://www.mikrotron.hr/ecotronserver/last?";
	DemoDialogBox dlg;
	VectorObject vObj;
	StationManager sm;
	
    Double longitude;
    Double latitude;
    Float can1;
    Float can2;
    Float can3;
    Float can4;
    Float can5;
    Float temp;
    String time;
    String gpsTime;
    String stationId;
    String batInfo;
    
	public FetchRequest(String stationId, VectorObject vObj, DemoDialogBox dlg) {
		super("GET", urlBase+"stationId="+stationId);
		this.dlg = dlg;
		this.vObj = vObj;
		setCallback(this);
		this.stationId = stationId;
	}
	public FetchRequest(String stationId, StationManager sm ) {
		super("GET", urlBase+"stationId="+stationId);
		setCallback(this);
		this.stationId = stationId;
		this.sm = sm;
	}
	@Override
	public void onResponseReceived(Request request, Response response) {
		if ( response.getStatusCode() == 200 ) {
			String json = response.getText();
			JSONValue val = JSONParser.parseLenient(json);
			JSONObject o = val.isObject();
			if ( o == null) {
				if ( dlg != null ) dlg.writeContent("ERROR: invalid response "+json);
			} else {
				Set<String> keys = o.keySet();
				for ( String key: keys) {
					val = o.get(key);
					if ( dlg != null ) dlg.write(key, val.toString());
					if ( "gpsLatitude".equals(key)) {
						latitude = new Double(val.toString());
					} else if ( "gpsLongitude".equals(key)) {
						longitude = new Double(val.toString());
					} else if ( "can1".equals(key)) {
						can1 = new Float(val.toString());
					} else if ( "can2".equals(key)) {
						can2 = new Float(val.toString());
					} else if ( "can3".equals(key)) {
						can3 = new Float(val.toString());
					} else if ( "can4".equals(key)) {
						can4 = new Float(val.toString());
					} else if ( "can5".equals(key)) {
						can5 = new Float(val.toString());
					} else if ( "temp".equals(key)) {
						temp = new Float(val.toString());
					} else if ( "gpsTime".equals(key)) {
						gpsTime = val.toString();
					} else if ( "time".equals(key)) {
						time = val.toString();
					} else if ( "batInfo".equals(key)) {
						batInfo = val.toString();
					}
				}
			}
			if ( vObj == null && sm != null ) {
				// create vector object from parsed data
				sm.addStation(stationId, longitude, latitude, DemoUtils.percentageToColor(maxCan()));
				ApplicationLayout.jsConsoleLog("Station "+stationId+" added at "+longitude+","+latitude);
			}
		} else {
			if ( dlg != null ) dlg.writeContent("ERROR "+response.getStatusCode()+": "+response.getStatusText());
		}
	}
	@Override
	public void onError(Request request, Throwable exception) {
		dlg.writeContent("ERROR: "+exception);
	}
	
	//find measurement for fullest can
	private String maxCan(){
		//full can has shortest measurement - we search for minimum length
		Float minLength=500f;
		minLength = (can1 < minLength) ? can1 : minLength;
		minLength = (can2 < minLength) ? can2 : minLength;
		minLength = (can3 < minLength) ? can3 : minLength;
		minLength = (can4 < minLength) ? can4 : minLength;
		minLength = (can5 < minLength) ? can5 : minLength;
		
		//if there is a problem, it should be the same as full can
		if (minLength<0) return "500.0"; else return minLength.toString();
	}
}
