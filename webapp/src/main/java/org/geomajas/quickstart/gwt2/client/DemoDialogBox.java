/**
 * 
 */
package org.geomajas.quickstart.gwt2.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author prexy
 *
 */
public class DemoDialogBox extends DialogBox {

    interface Binder extends UiBinder<Widget, DemoDialogBox>{}
	private static final Binder binder = GWT.create(Binder.class);

    @UiField ParagraphElement dialogContent;
    
    public DemoDialogBox(String dialogBoxTitle) {
        setWidget(binder.createAndBindUi(this));
        setAutoHideEnabled(true);
        setGlassEnabled(true);
        setText(dialogBoxTitle);
    } 

    public void writeContent(String content) {
    	dialogContent.setInnerText(content);
    }
    
    //TODO add click handler for OK button
}
