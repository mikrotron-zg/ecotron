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

public class FetchRequest extends RequestBuilder implements RequestCallback {
	static String urlBase = "http://www.mikrotron.hr/ecotronserver/last?";
	DemoDialogBox dlg;
	VectorObject vObj;
    Double longitude;
    Double latitude;
    
	public FetchRequest(String stationId, VectorObject vObj, DemoDialogBox dlg) {
		super("GET", urlBase+"stationId="+stationId);
		this.dlg = dlg;
		this.vObj = vObj;
		setCallback(this);
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
					}
				}
			}
			updateCoordinates();
		} else {
			if ( dlg != null ) dlg.writeContent("ERROR "+response.getStatusCode()+": "+response.getStatusText());
		}
	}
    /** called from http callback, after all parameters have been written */
	public void updateCoordinates() {
		if ( longitude != null && latitude != null ) {
			// TODO: verify and recalculate as necessary! 
			Double deltaX = new Double(longitude);
			Double deltaY = new Double(latitude);
			//if ( vObj != null ) vObj.setTranslation(deltaX,deltaY);
		}
	}
	@Override
	public void onError(Request request, Throwable exception) {
		dlg.writeContent("ERROR: "+exception);
	}
}
