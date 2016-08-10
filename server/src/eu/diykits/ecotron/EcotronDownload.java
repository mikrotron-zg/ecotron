package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

public class EcotronDownload extends EcotronServer {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger.logDebug("Request received: " + request.getRequestURL());
    response.setContentType("text/csv");

    String delimiter = request.getParameter("delimiter");
    if ( delimiter != null ) delimiter = ";";

    PrintWriter out = response.getWriter();
    String stationId = request.getParameter("stationId");
    if ( stationId == null ) throw new ServletException( "Required parameter missing: stationId" );
    response.setHeader("Content-Disposition","attachment; filename="+stationId+".csv");
    response.setHeader("Access-Control-Allow-Origin","*");
    try {
      Object[] ret = db.getRange("GenericEntry", "stationId", stationId);
      if ( ret.length > 0 ) out.println(((DBObject)ret[0]).toCSVHeader(delimiter));
      for ( Object el: ret ) {
        out.println(((DBObject)el).toCSV(delimiter));
      }
    } catch ( Exception e ) {
      Logger.logError(e);
      out.println("ERROR: "+e.getMessage());
    } finally {
      out.flush();
      out.close();
    }
    Logger.logDebug("Sent response.");
  }
}