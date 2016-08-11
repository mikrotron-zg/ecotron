package org.geomajas.quickstart.gwt2.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geomajas.geometry.Coordinate;
import org.geomajas.geometry.service.WktException;
import org.geomajas.geometry.service.WktService;
import org.geomajas.gwt2.client.GeomajasImpl;
import org.geomajas.quickstart.gwt2.client.resource.ApplicationResource;
import org.vaadin.gwtgraphics.client.Group;
import org.vaadin.gwtgraphics.client.VectorObject;
import org.vaadin.gwtgraphics.client.shape.Circle;
import org.vaadin.gwtgraphics.client.shape.Text;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;

public class StationManager {
	//TODO: hold stationId,VectorObject map of all objects
	private VectorObject mikrotron;
	private VectorObject veta;
	private ApplicationLayout layout;
	private HashMap stations = new HashMap();
	
	public StationManager( ApplicationLayout layout ) {
		this.layout = layout;
	}
	public void init() {
		//add objects to Map
		//Mikrotron
		//mikrotron = getVectorObject(new Coordinate(15.928245, 45.789566), "alfa", "Mikrotron d.o.o.");
		//layout.addObject(mikrotron);
		
		//VETA
		veta = getVectorObject(new Coordinate(15.537470, 45.460168), "KarlovacTest", "VETA d.o.o.");
		layout.addObject(veta);
		
		//TODO: fetch station list here
		ListRequest list = new ListRequest(this);
		try {
			list.send();
		} catch (RequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private VectorObject getVectorObject(Coordinate coordinate, final String stationId, String title){
		
		try {
			final VectorObject vObject = GeomajasImpl.getInstance().getGfxUtil().toShape(
					WktService.toGeometry(coordinatePointToString(coordinate)));
			GeomajasImpl.getInstance().getGfxUtil().applyStroke(vObject, "#C00000", 1, 10, null);
			coordinate = toProjection(coordinate);
			final VectorObject text = new Text(coordinate.getX(), coordinate.getY(), title);
			text.setTranslation(20, 0);
			GeomajasImpl.getInstance().getGfxUtil().applyStroke(text, "#000000", 1, 1, null);
			final Group group = new Group();
			group.add(vObject);
			group.add(text);
			vObject.setTitle(title);
			vObject.addDomHandler(new MouseOverHandler() {
	            @Override
	            public void onMouseOver(MouseOverEvent e) {
	            	ApplicationService.getInstance().getToolTip().clearContent();
					List<Label> content = new ArrayList<Label>();
					final Label label = new Label(((Circle)e.getSource()).getElement().getAttribute("title"));
					label.addStyleName(ApplicationResource.INSTANCE.css().toolTipLine());
					content.add(label);
					ApplicationService.getInstance().getToolTip().addContentAndShow(
							content, e.getClientX() + 5, e.getClientY() + 5);
	            }
	        }, MouseOverEvent.getType());
			
			vObject.addDomHandler(new MouseOutHandler() {
	            @Override
	            public void onMouseOut(MouseOutEvent event) {
	            	ApplicationService.getInstance().getToolTip().hide();
	            }
	        }, MouseOutEvent.getType());
			
			vObject.addDomHandler(new ClickHandler(){
				@Override
				public void onClick(ClickEvent e) {
					String name = ((Circle)e.getSource()).getElement().getAttribute("title");
					makePopup(stationId, vObject, e);
				 	ApplicationService.getInstance().getToolTip().hide();
				}
			}, ClickEvent.getType());
			
			//return vObject;
			return group;
		} catch (Exception e) {
			return null;
		}
		
	}

	private void makePopup( String stationId, VectorObject vObject, ClickEvent e ) {
		DemoDialogBox popup = new DemoDialogBox("Stanica: " + stationId);
		popup.writeContent("Zadnje očitanje:");
		if ( e == null ) {
			popup.center();
		} else {
			popup.setPopupPosition(e.getClientX(), e.getClientY());
		}
	 	popup.show();
	 	FetchRequest fetch = new FetchRequest(stationId, vObject, popup);
	 	try {
			fetch.send();
		} catch (RequestException re) {
			popup.writeContent("Error:  " + re);
			re.printStackTrace();
		}
	}
	
	private String coordinatePointToString(Coordinate point){
		point = toProjection(point);
		return "POINT (" + point.getX() + " " + point.getY() + ")";
	}
	
	private Coordinate toProjection(Coordinate point){

//	    CHECKME Can't use this code on client side
//		try {
//			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
//			CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
//			CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");
//			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
//			Point temp = (Point) JTS.transform(geometryFactory.createPoint(new com.vividsolutions.jts.geom.Coordinate(
//					point.getX(), point.getY())), transform);
//			return new Coordinate(temp.getX(), temp.getY());
//		} catch (NoSuchAuthorityCodeException e) {
//			//quiet quit
//		} catch (FactoryException e) {
//			//quiet quit
//		} catch (TransformException e) {
//			//quiet quit
//		}

		double x = point.getX() * 20037508.34 / 180;
		double y = Math.log(Math.tan((90 + point.getY()) * Math.PI / 360)) / (Math.PI / 180);
		y *= 20037508.34 / 180;
		return new Coordinate(x, y);
	}
	
	public class ListRequest extends RequestBuilder implements RequestCallback {
		static final String urlBase = "http://www.mikrotron.hr/ecotronserver/stations";
	    final StationManager sm;
	    
		public ListRequest(StationManager sm) {
			super("GET", urlBase);
			this.sm = sm;
			setCallback(this);
		}
		@Override
		public void onResponseReceived(Request request, Response response) {
			if ( response.getStatusCode() == 200 ) {
				String[] list = response.getText().split("\n");
				for ( String stationId: list ) {
					stationId = stationId.trim();
					stations.put(stationId, null);
					FetchRequest req = new FetchRequest(stationId, sm);
					try {
						req.send();
						ApplicationLayout.jsConsoleLog("Station "+stationId+" loading...");
					} catch (RequestException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			// TODO: else?
		}
		@Override
		public void onError(Request request, Throwable exception) {
			// TODO Auto-generated method stub
		}
	}
	public void addStation(String stationId, Double longitude, Double latitude ) {
		VectorObject station = getVectorObject(new Coordinate(longitude, latitude), stationId, stationId);
		stations.put(stationId, station);
		layout.addObject(station);
	}
}
