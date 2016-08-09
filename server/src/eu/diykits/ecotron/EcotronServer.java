package eu.diykits.ecotron;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.vrspace.util.*;
import org.vrspace.server.*;
import eu.diykits.ecotron.db.*;

public abstract class EcotronServer extends HttpServlet {
  protected HibernateDB db;

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
  public abstract void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

  @Override
  public void destroy() {
    Logger.stopStaticLogger();
  	Logger.logInfo("AAAARGH!!!");
  }
}