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
    		can1.setInnerText(val);
    	} else if ( "can2".equals(field)) {
    		can2.setInnerText(val);
    	} else if ( "can3".equals(field)) {
    		can3.setInnerText(val);
    	} else if ( "can4".equals(field)) {
    		can4.setInnerText(val);
    	} else if ( "can5".equals(field)) {
    		can5.setInnerText(val);
    	} else if ( "temp".equals(field)) {
    		temp.setInnerText(val);
    	} else if ( "gpsLatitude".equals(field)) {
    		gpsLatitude.setInnerText(val);
    	} else if ( "gpsLongitude".equals(field)) {
    		gpsLongitude.setInnerText(val);
    	} else if ( "gpsTime".equals(field)) {
    		gpsTime.setInnerText(val);
    	} else if ( "time".equals(field)) {
    		time.setInnerText(val);
    	} else if ( "stationId".equals(field)) {
    		stationId.setInnerText(val);
    	}
    }
	@Override
	public void onClick(ClickEvent event) {
		hide();
	}
}
