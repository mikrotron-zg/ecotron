package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

public class EcotronUpload extends EcotronServer {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger.logDebug("Request received: " + request.getRequestURL());
    response.setContentType("text/html");

    PrintWriter out = response.getWriter();
    try {
      db.put(new GenericEntry(request.getParameterMap()));
      out.println("<html><h1>OK</h1></html>");
    } catch ( Exception e ) {
      Logger.logError(e);
      out.println("<html><h1>ERROR: "+e.getMessage()+"</h1></html>");
    } finally {
      out.flush();
      out.close();
    }
    Logger.logDebug("Sent response.");
  }
}