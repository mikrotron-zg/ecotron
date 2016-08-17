/**
 * 
 */
package org.geomajas.quickstart.gwt2.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author prexy
 *
 */
public class DemoDialogBox extends DialogBox implements ClickHandler {

    interface Binder extends UiBinder<Widget, DemoDialogBox>{}
	private static final Binder binder = GWT.create(Binder.class);

    @UiField LabelElement dialogContent;
    @UiField LabelElement stationId;
    @UiField LabelElement gpsLongitude;
    @UiField LabelElement gpsLatitude;
    @UiField LabelElement can1;
    @UiField LabelElement can2;
    @UiField LabelElement can3;
    @UiField LabelElement can4;
    @UiField LabelElement can5;
    @UiField LabelElement temp;
    @UiField LabelElement gpsTime;
    @UiField LabelElement time;
    @UiField Button button;
    
    public DemoDialogBox(String dialogBoxTitle) {
        setWidget(binder.createAndBindUi(this));
        setAutoHideEnabled(true);
        setGlassEnabled(true);
        setText(dialogBoxTitle);
        button.addClickHandler(this);
    } 

    public void writeContent(String content) {
    	dialogContent.setInnerText(content);
    }

    public void write(String field, String val) {
    	if ( "can1".equals(field)) {
    		can1.setInnerText(percentage(val));
    	} else if ( "can2".equals(field)) {
    		can2.setInnerText(percentage(val));
    	} else if ( "can3".equals(field)) {
    		can3.setInnerText(percentage(val));
    	} else if ( "can4".equals(field)) {
    		can4.setInnerText(percentage(val));
    	} else if ( "can5".equals(field)) {
    		can5.setInnerText(percentage(val));
    	} else if ( "temp".equals(field)) {
    		temp.setInnerText(val + " °C");
    	} else if ( "gpsLatitude".equals(field)) {
    		gpsLatitude.setInnerText(val + "°");
    	} else if ( "gpsLongitude".equals(field)) {
    		gpsLongitude.setInnerText(val + "°");
    	} else if ( "gpsTime".equals(field)) {
    		gpsTime.setInnerText(val);
    	} else if ( "time".equals(field)) {
    		time.setInnerText(val);
    	} else if ( "stationId".equals(field)) {
    		stationId.setInnerText(val);
    	}
    }
    
    private String percentage(String val){
    	
    	//FIXME exact lengths will be known after sensors are mounted
    	final double empty = 120;
    	final double full = 40;
    	double measured = Double.parseDouble(val);
        
    	if (measured <= (full+5)) return "100 %";
    	if (measured >= (empty-5)) return "0 %";
    	
    	double calc = (1 - ((measured-full)/(empty-full)) + 0.05)*10;
    	int rounded = ((int)calc*100)/10;
    	
    	return rounded + " %";
    }
    
	@Override
	public void onClick(ClickEvent event) {
		hide();
	}
}
