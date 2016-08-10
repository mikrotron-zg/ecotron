package org.geomajas.quickstart.gwt2.client;

import java.util.Set;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class FetchRequest extends RequestBuilder implements RequestCallback {
	static String urlBase = "http://www.mikrotron.hr/ecotronserver/last?";
	DemoDialogBox dlg;
	public FetchRequest(String stationId, DemoDialogBox dlg) {
		super("GET", urlBase+"stationId="+stationId);
		this.dlg = dlg;
		setCallback(this);
	}
	@Override
	public void onResponseReceived(Request request, Response response) {
		if ( response.getStatusCode() == 200 ) {
			String json = response.getText();
			JSONValue val = JSONParser.parseLenient(json);
			JSONObject o = val.isObject();
			if ( o == null) {
				dlg.writeContent("ERROR: invalid response "+json);
			} else {
				Set<String> keys = o.keySet();
				for ( String key: keys) {
					val = o.get(key);
					dlg.write(key, val.toString());
				}
			}
		} else {
			dlg.writeContent("ERROR "+response.getStatusCode()+": "+response.getStatusText());
		}
	}
	@Override
	public void onError(Request request, Throwable exception) {
		dlg.writeContent("ERROR: "+exception);
	}
}
