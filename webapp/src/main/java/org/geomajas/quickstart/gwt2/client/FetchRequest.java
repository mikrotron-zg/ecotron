package org.geomajas.quickstart.gwt2.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

public class FetchRequest extends RequestBuilder implements RequestCallback {
	static String urlBase = "http://www.mikrotron.hr/ecotronserver/download?";
	DemoDialogBox dlg;
	public FetchRequest(String stationId, DemoDialogBox dlg) {
		super("GET", urlBase+"stationId="+stationId);
		this.dlg = dlg;
		setCallback(this);
	}
	@Override
	public void onResponseReceived(Request request, Response response) {
		if ( response.getStatusCode() == 200 ) {
			dlg.writeContent(response.getText());
		} else {
			dlg.writeContent("ERROR "+response.getStatusCode()+": "+response.getStatusText());
		}
	}
	@Override
	public void onError(Request request, Throwable exception) {
		dlg.writeContent("ERROR: "+exception);
	}
}
