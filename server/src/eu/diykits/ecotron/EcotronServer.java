package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

public class EcotronServer extends HttpServlet {
  private HibernateDB db;

  @Override
  public void init() throws ServletException {
    new Log4JLogger("EcotronServer");
    Logger.logInfo("Ecotron server initializing");
	// database setup
    db = new HibernateDB();
    db.propertyBase="db";
    Properties props = new Properties();
    props.put("db.url", "postgresql://localhost/ecotron");
    props.put("db.driver", "org.postgresql.Driver");
    props.put( "db.packages", "eu.diykits.ecotron.db" );
    props.put( "db.username", "postgres" );
    props.put( "db.password", "" );
    props.put( "db.dialect", "org.hibernate.dialect.PostgreSQLDialect" );
    props.put( "db.create", "true" );
    db.setProperties( props );
    db.init();
    Logger.logInfo("Ecotron server initialized");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger.logDebug("Request received: " + request.getRequestURL());
    response.setContentType("text/html");

    PrintWriter out = response.getWriter();
    out.println("<html><h1>OK</h1></html>");
    out.flush();
    out.close();
    Logger.logDebug("Sent response.");
    try {
      db.put(new GenericEntry(request.getParameterMap()));
    } catch ( Exception e ) {
      Logger.logError(e);
    }
  }

  @Override
  public void destroy() {
    Logger.stopStaticLogger();
  	Logger.logInfo("AAAARGH!!!");
  }
}