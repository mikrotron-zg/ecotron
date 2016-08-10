package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

public class EcotronLast extends EcotronServer {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger.logDebug("Request received: " + request.getRequestURL());
    response.setContentType("application/json");

    PrintWriter out = response.getWriter();
    String stationId = request.getParameter("stationId");
    if ( stationId == null ) throw new ServletException( "Required parameter missing: stationId" );

    response.setHeader("Access-Control-Allow-Origin","*");
    try {
      // FIXME: iteration is bad! Use order by desc limit 1
      Object[] ret = db.getRange("GenericEntry", "stationId", stationId);
      GenericEntry last = null;
      if ( ret.length > 0 ) {
        last = (GenericEntry) ret[0];
        for ( Object el: ret ) {
          if ( ((GenericEntry)el).db_id > last.db_id ) last = (GenericEntry) el;
        }
      }
      out.println(last.toJSON());
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