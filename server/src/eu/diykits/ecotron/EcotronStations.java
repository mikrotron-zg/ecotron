package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

/** Servlet to list all known stations */
public class EcotronStations extends EcotronServer {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger.logDebug("Request received: " + request.getRequestURL());
    response.setContentType("text/plain");
    response.setHeader("Access-Control-Allow-Origin","*");

    PrintWriter out = response.getWriter();
    try {
      db.transaction();
      org.hibernate.Query query = db.session().createQuery("select distinct e.stationId from GenericEntry e where e.stationId is not null");
      List stations = query.list();
      for ( Object station: stations ) {
        out.println( station );
      }
      db.commit();
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