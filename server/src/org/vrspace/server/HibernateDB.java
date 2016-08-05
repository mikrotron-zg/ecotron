package org.vrspace.server;

import java.io.*;
import java.util.*;
import java.net.*;

import org.hibernate.*;
import org.hibernate.cfg.*;
import org.hibernate.type.*;
import org.hibernate.criterion.*;

import java.sql.Connection;
import java.sql.ResultSet;  //CHECKME: i don't wanna sql dependencies here
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import org.vrspace.util.*;

import java.lang.reflect.*;

/**
Database class.<br>

Used properties:<br>
vrspace.db.packages - packages containing persistence classes, default none<br>
Following properties are only used if no hibernate.cfg.xml is found:<br>
vrspace.db.driver - JDBC driver to use, default org.postgresql.Driver<br>
vrspace.db.url - database url, default //localhost/test<br>
vrspace.db.username - database username, default postgres<br>
vrspace.db.password - database password, default empty string<br>
vrspace.db.dialect - database dialect to use, default org.hibernate.dialect.PostgreSQLDialect<br>
vrspace.db.verbose - if true, adds hibernate show_sql statement, default true<br>
vrspace.db.create - if true, will create database, default true<br>
vrspace.db.sessioncontext - hibernate session context: thread, managed, jta - default thread<br>
vrspace.db.sessiontimeout - hibernate session timeout, default 300 (5 min)<br>
vrspace.db.useconfig - use hibernate configuration and mapping files, if exist<br>
vrspace.db.writeconfig - save hibernate configuration and mapping files, if do not exist<br>
<br>
Instead of vrspace.db.*, other properties may be used by setting propertyBase prior to setProperties() call.
<br>
This class tries to implement nested transactions by tracking threads and their open transactions.
<br>
NOTE - not synchronized.
*/
public class HibernateDB {
  private String db;
  private boolean commit = false;

  private myConfiguration conf;
  private boolean initialized = false;
  private SessionFactory sessionFactory;

  private String url;

  protected HashMap orMap = new HashMap();
  protected HashMap labels = new HashMap();
  protected HashMap names = new HashMap();

  protected static HibernateDB firstInstance;

  private Properties properties;

  /** Used during initialization, specifies database connection property names, default: vrspace.db */
  public String propertyBase = "vrspace.db";
  /**
  Used during initialization: should existing configuration be used? Default: true
  If false, or config file does not exist, configuration is built from properties.
  This applies to both hibernate configuration, and to hibernate mapping files.
  */
  public boolean useConfig = true;
  /**
  Used during initialization: should configuration be written to disk? Default:true
  Hiberante configuration and mappings are written to disk if:<br>
  1) this flag is set, AND <br>
  2) configuration/mapping file does not exist OR was not used (useConfig==false)
  */
  public boolean writeConfig = true;
  /**
  Path to write configuration files to. Default: null.
  If not specified, path is ../etc/ relative to location of this class/jar file.
  */
  public String configPath;

  public boolean useCache = false;
  public boolean autoFlush = true;

  public HibernateDB() {
    if ( firstInstance == null ) {
      firstInstance = this;
    }
  }

  /**
  create a new database if does not exist, not implemented.
  @param name Database name
  @return String to use as parameter to connect()
  @see #connect
  */
  public String create( String name ) {
    return "";
  }

  public void setProperties( Properties p ) {
    //Logger.logDebug("DB properties set");
    properties = p;
    String useCfg = p.getProperty(propertyBase+".useconfig");
    if ( useCfg != null ) {
      if ( "true".equals( useCfg ) ) useConfig = true;
      else if ( "false".equals( useCfg )) useConfig = false;
      //else error
    }
    String writeCfg = p.getProperty(propertyBase+".writeconfig");
    if ( writeCfg != null ) {
      if ( "true".equals( writeCfg ) ) writeConfig = true;
      else if ( "false".equals( writeCfg )) writeConfig = false;
      //else error
    }
  }

  /** connect to the database */
  public void connect( String url ) throws Exception {
    this.url = url;
    init();
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //org.hibernate.Session session = sessionFactory.openSession();
  }

  public synchronized void init() {
    if ( ! initialized ) {
      if ( properties == null ) {
        Logger.logWarning("HibernateDB: properties not set!"); //CHECKME: throw RuntimeException?
      } else {
        String packages = properties.getProperty(propertyBase+".packages");
        if ( packages != null ) DBObject.addPackages( packages ); // using addPackages rather than setPackages, so other classes may use static package initialization
      }
      List packages = DBObject.getPackages();
      if ( packages.size() == 0 ) Logger.logWarning("HibernateDB: No persistence packages defined!");
      configure();
      sessionFactory = addMappings();
      initialized = true;
      Logger.logInfo("HibernateDB initialized, statistics: "+sessionFactory.getStatistics().isStatisticsEnabled());
      // needs not be set to false since we changed create to update
      //properties.setProperty(propertyBase+".create","false");  //CHECKME
    }
  }
  public void logStatistics() {
    Logger.logDebug(getStatistics());
  }
  public String getStatistics() {
    org.hibernate.stat.Statistics s = sessionFactory.getStatistics();
    //s.logSummary();
    StringBuilder sb = new StringBuilder("Hibernate statistics\n");

    // connections / sessions
    sb.append( "\nConnections: "+s.getConnectCount() );
    sb.append( "\nOptimistic failure count: "+s.getOptimisticFailureCount() );
    sb.append( "\nSessions opened: "+s.getSessionOpenCount() );
    sb.append( "\nSessions closed: "+s.getSessionCloseCount() );
    sb.append( "\nSession flushes: "+s.getFlushCount() );
    sb.append( "\nTotal transactions: "+s.getTransactionCount() );
    sb.append( "\nSuccessful transactions: "+s.getSuccessfulTransactionCount() );

    // statements statistics
    sb.append( "\nStatements prepared: "+s.getPrepareStatementCount() );
    sb.append( "\nStatements closed: "+s.getCloseStatementCount() );

    // entities
    sb.append( "\nEntities deleted: "+s.getEntityDeleteCount() );
    sb.append( "\nEntities fetched: "+s.getEntityFetchCount() );
    sb.append( "\nEntities inserted: "+s.getEntityInsertCount() );
    sb.append( "\nEntities loaded: "+s.getEntityLoadCount() );
    sb.append( "\nEntities updated: "+s.getEntityUpdateCount() );
    String[] entities = s.getEntityNames(); // TODO: print them out with getEntityStatistics( String name );

    // collection statistics
    sb.append( "\nCollections fetched: "+s.getCollectionFetchCount());
    sb.append( "\nCollections loaded: "+s.getCollectionLoadCount());
    sb.append( "\nCollections recreated: "+s.getCollectionRecreateCount());
    sb.append( "\nCollections removed: "+s.getCollectionRemoveCount());
    sb.append( "\nCollections updated: "+s.getCollectionUpdateCount());
    String[] roles = s.getCollectionRoleNames(); // TODO: print them out with getCollectionStatistics( String role )

    // Natural ID cache statistics
    sb.append( "\nID cache hits: "+s.getNaturalIdCacheHitCount() );
    sb.append( "\nID cache miss: "+s.getNaturalIdCacheMissCount() );
    sb.append( "\nID cache puts: "+s.getNaturalIdCachePutCount() );
    sb.append( "\nID cache queries: "+s.getNaturalIdQueryExecutionCount() );
    sb.append( "\nID cache max time: "+s.getNaturalIdQueryExecutionMaxTime() );
    sb.append( "\nID cache slowest region: "+s.getNaturalIdQueryExecutionMaxTimeRegion() );
    // TODO: getNaturalIdCacheStatistics( String region );

    // query cache statistics
    sb.append( "\nQuery cache hits: "+s.getQueryCacheHitCount() );
    sb.append( "\nQuery cache miss: "+s.getQueryCacheMissCount() );
    sb.append( "\nQuery cache puts: "+s.getQueryCachePutCount() );
    sb.append( "\nQuery executions: "+s.getQueryExecutionCount() );
    sb.append( "\nSlowest query execution: "+s.getQueryExecutionMaxTime() );
    sb.append( "\nSlowest query: "+s.getQueryExecutionMaxTimeQueryString() );
    String[] queries = s.getQueries(); // TODO: getQueryStatistics( String query )

    // cache timestamps
    sb.append( "\nTimestamps hits: "+s.getUpdateTimestampsCacheHitCount() );
    sb.append( "\nTimestamps miss: "+s.getUpdateTimestampsCacheMissCount() );
    sb.append( "\nTimestamps puts: "+s.getUpdateTimestampsCachePutCount() );

    // 2nd level cache
    sb.append( "\nL2 cache hits: "+s.getSecondLevelCacheHitCount() );
    sb.append( "\nL2 cache miss: "+s.getSecondLevelCacheMissCount() );
    sb.append( "\nL2 cache puts: "+s.getSecondLevelCachePutCount() );
    String[] regions = s.getSecondLevelCacheRegionNames();
    // TODO: getSecondLevelCacheStatistics( String region );

    return sb.toString();
  }

  /** Disconnect from the database **/
  public void disconnect() {
    close();
  }

  /**
  Commit changes. Should NOT be synchronized.
  */
  //public void commit() {
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.flush();
  //}

  /**
  Stores obj into database
  Logic:
    - all the public fields are stored
    - database table: obj.getClass().getName()
    - create table if does not exist
  */
  public void put( Object obj ) throws Exception {
    //Logger.logDebug( "HibernateDB.put( "+obj+" )" );
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    /*
    automatic rollback of transaction which produced exception
    prevents 'nested transactions not supported' error
    */
    long id = 0;
    try {
      id = obj.getClass().getField("db_id").getLong( obj );
      if ( id == 0 ) {
        session().save( obj );
        //Logger.logDebug("HibernateDB.save: "+obj);
      } else {
        session().saveOrUpdate( obj );
        //session().merge( obj ); // attemt to resolve org.hibernate.HibernateException: Illegal attempt to associate a collection with two open sessions
        //Logger.logDebug("HibernateDB.saveOrUpdate: "+obj);
      }
      //session.getTransaction().commit();
      commit();
    } catch ( Exception e ) {
      Logger.logError( "Cannot store "+obj.getClass().getName()+" "+id, e );
      //session.getTransaction().rollback();
      rollback();
      throw e;
    }
  }

  /** Return database object from table <b>obj</b>.getClass().getName() having db_id = <b>obj</b>.db_id
  */
  public Object get( Object obj ) throws Exception {
    if ( obj == null ) throw new NullPointerException( "Cannot get null object" );
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    long id = obj.getClass().getField("db_id").getLong( obj );
    transaction();
    //session().load( obj, new Long(id) ); //this may produce org.hibernate.PersistentObjectException: attempted to load into an instance that was already associated with the session
    obj = session().get( obj.getClass(), new Long(id) ); //this never returns uninitialized instance
    commit();
    //return get( Util.getClassName(obj), obj.getClass().getField( "db_id" ).getLong(obj) );
    return obj;
  }

  /** Returns the object having id == <b>obj</b>.db_id
  */
  public Object get( String className, long id ) throws Exception {
    //Class cls = Class.forName( className );
    //Object ret = cls.newInstance();
    Object ret = DBObject.newInstance( className );
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    session().load( ret, new Long(id) ); // forcing instantiation to avoid org.hibernate.LazyInitializationException: could not initialize proxy - no Session
    //session.getTransaction().commit();
    commit();
    return ret;
  }

  /**
  Returns the object having id == <b>obj</b>.db_id
  */
  public Object get( Class cls, long id ) throws Exception {
    transaction();
    Object ret = session().byId(cls).load( id );
    commit();
    return ret;
  }

  /**
  Returns the object of <b>className</b> class having <b>field</b> == <b>value</b><br>
  NOTE: using this method to check for object existence may be quite slow, depending on peristent members; use contains() instead
  */
  public Object get( String className, String field, Object value ) throws Exception {
    Object ret;
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    try {
      Query q = session().createQuery( "from "+className+" as "+className+" where "+field+" = ?" ).setCacheable( true );
      if ( value instanceof Boolean ) {
        ret = q.setBoolean(0,(Boolean)value).uniqueResult();
      } else if ( value instanceof String ) {
        ret = q.setString(0,(String)value).uniqueResult();
      } else {
        ret = q.setEntity(0,value).uniqueResult();
      }
      if ( ret != null ) {
        // reload the (cached) object to avoid org.hibernate.LazyInitializationException: could not initialize proxy - no Session
        // this is needed sometimes but sometimes is not - CHECKME
        //ret = session.get( ret.getClass(), ret.getClass().getField("db_id").getLong( ret ));
      }
      //Logger.logDebug( "HibernateDB.get( "+className+","+field+","+value+" ) -> "+ret );
      // catch, abort, rethrow
      //session.getTransaction().commit();
      commit();
    } catch ( Exception e ) {
      Logger.logError( "Cannot get "+className+"."+field+"='"+value+"'", e );
      //session.getTransaction().rollback();
      rollback();
      throw e;
    }
    return ret;
  }

  /**
  Returns the object of class <b>cls</b> class having <b>field</b> == <b>value</b><br>
  */
  public Object get( Class cls, String field, Object value ) throws Exception {
    Object ret;
    transaction();
    try {
      //ret = (T) session().createCriteria(cls).add( Restrictions.naturalId().set(field, value) ).setCacheable(true).uniqueResult();
      ret = session().bySimpleNaturalId(cls).load(value);
      commit();
    } catch ( Exception e ) {
      Logger.logError( "Cannot get "+cls+"."+field+"='"+value+"'", e );
      rollback();
      throw e;
    }
    return ret;
  }

  /**
  Check if given object exists. This method uses SQL query rather than HQL query for performance reasons.
  WARNING - mixing SQL and HQL within same transaction may fail with org.hibernate.exception.ConstraintViolationException: ERROR: duplicate key value violates unique constraint
  */
  public boolean contains( String className, String field, Object value ) throws Exception {
    boolean ret = false;
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    //String sql = "select db_id from "+className+" where "+field+" = ?";
    String sql = "select db_id from "+className.toLowerCase()+" where "+field.toLowerCase()+" = ?";
    try {
      if ( value instanceof Boolean ) {
        ret = (session().createSQLQuery( sql ).setBoolean(0,(Boolean)value).uniqueResult() != null );
      } else if ( value instanceof String ) {
        ret = (session().createSQLQuery( sql ).setString(0,(String)value).uniqueResult() != null);
      } else {
        ret = (session().createSQLQuery( sql ).setEntity(0,value).uniqueResult() != null);
      }
      //session.getTransaction().commit();
      commit();
    } catch ( Exception e ) {
      Logger.logError( "Cannot get "+className+"."+field+"="+value, e );
      //session.getTransaction().rollback();
      rollback();
      throw e;
    }
    return ret;
  }

  /**
  Same as contains(), but returns object's db_id, or 0 if not found.
  */
  public long getId( String className, String field, Object value ) throws Exception {
    Long ret;
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    //String sql = "select db_id from "+className+" where "+field+" = ?";
    String sql = "select db_id from "+className.toLowerCase()+" where "+field.toLowerCase()+" = ?";
    SQLQuery q = session().createSQLQuery( sql ).addScalar("db_id", LongType.INSTANCE);
    try {
      if ( value instanceof Boolean ) {
        ret = (Long) q.setBoolean(0,(Boolean)value).uniqueResult();
      } else if ( value instanceof String ) {
        ret = (Long) q.setString(0,(String)value).uniqueResult();
      } else {
        ret = (Long) q.setEntity(0,value).uniqueResult();
      }
      //session.getTransaction().commit();
      commit();
    } catch ( Exception e ) {
      Logger.logError( "Cannot get "+className+"."+field+"="+value, e );
      //session.getTransaction().rollback();
      rollback();
      throw e;
    }
    if ( ret == null ) ret = new Long(0);
    return ret;
  }

  /**
  Returns objects of <b>className</b> class having <b>field</b> == <b>value</b>.<br>
  BUG!<br>
  If value is an entity (instance of DBObject), hibernate seems to ignore property-ref,
  and always performs lookup on object's id (db_id).<br>
  Workaround:<br>
  Do not use getRange( class, field, entity ); use getRange( class, field, entity.field ) instead.
  */
  public Object[] getRange( String className, String field, Object value ) throws Exception {
    List ret;
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    Query query = session().createQuery( "from "+className+" as "+className+" where "+field+" = ?" ).setCacheable(true);
    if ( value instanceof Boolean ) {
      query.setBoolean(0,(Boolean)value);
    } else if ( value instanceof String ) {
      query.setString(0,(String)value);
    } else {
      query.setEntity(0,value);
    }
    ret = query.list();
    //session.getTransaction().commit();
    commit();
    //Logger.logDebug( "HibernateDB.getRange("+className+", "+field+"="+value+")->"+ret.size() );
    return ret.toArray();
    //return getRange( className, new String[]{ field }, new Object[]{ value } );
  }

  /** Returns objects of <b>className</b> class having each of <b>fields</b> == <b>value</b> */
  public Object[] getRange( String className, String[] fields, Object[] values ) {
    List ret;
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    StringBuffer sb = new StringBuffer("from "+className+" as "+className+" where ");
    for ( int i = 0; i < fields.length-1; i++ ) {
      sb.append( fields[i] );
      sb.append( " = ? and " );
    }
    sb.append( fields[fields.length-1] );
    sb.append( " = ?" );
    Query query = session().createQuery( sb.toString() ).setCacheable(true);
    for ( int i = 0; i < fields.length; i++ ) {
      if ( values[i] instanceof Boolean ) {
        query.setBoolean(i,(Boolean)values[i]);
      } else if ( values[i] instanceof String ) {
        query.setString(i,(String)values[i]);
      } else {
        query.setEntity(i,values[i]);
      }
    }
    ret = query.list();
    //session.getTransaction().commit();
    commit();
    return ret.toArray();
  }

  /**
  Returns Object[] between o1 and o2
  Class must have comparator() method to be searchable.
  NOT IMPLEMENTED
  */
  public Object[] getRange( Object o1, Object o2 ) throws Exception {
    return new Object[0];
  }

  /**
  Returns all members of the class
  */
  public Object[] getAll( String className ) throws Exception {
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    java.util.List ret = session().createQuery("from "+className).setCacheable(true).list();
    //session.getTransaction().commit();
    commit();
    return ret.toArray();
  }

  /**
  Returns all members of the class
  */
  public List list( String className ) throws Exception {
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    java.util.List ret = session().createQuery("from "+className).setCacheable(true).list();
    //session.getTransaction().commit();
    commit();
    return ret;
  }

  /**
  Returns all classes stored in the database. Calls DBObject.listClasses()
  */
  //public String[] getClasses() throws Exception {
  public String[] getClasses() {
    Vector cl = new Vector();

    /*
    StringTokenizer st = new StringTokenizer( clPreload );
    while (st.hasMoreTokens()) {
      String className = st.nextToken();
      cl.add( className );
    }
    */
    return DBObject.listClasses();

    //return (String[]) cl.toArray( new String[ cl.size() ] );
  }

  /**
  Returns all the tables in the database.
  */
  public String[] getTables() throws SQLException {
    Connection c = getConnection();
    DatabaseMetaData dbmd = c.getMetaData();

    Vector ret = new Vector();

    ResultSet rsTables = dbmd.getTables( c.getCatalog(), null, null, null );
    while ( rsTables.next() ) {
      String name = rsTables.getString( 3 );
      String type = rsTables.getString( 4 );
      //System.out.println( name+" "+type );
      if ( "TABLE".equals( type ) ) {
        ret.add( name );
      }
    }
    closeConnection();

    return (String[])ret.toArray( new String[ ret.size() ] );
  }

  /** From the table <b>obj</b>.getClass().getName() deletes the row having
   ** db_id == <b>obj</b>.db_id
  */
  public void delete( Object obj ) throws Exception {
    //org.hibernate.Session session = sessionFactory.getCurrentSession();
    //session.beginTransaction();
    transaction();
    session().delete( obj );
    //session.getTransaction().commit();
    commit();
  }

  /**
  A request is a change to <b>one</b> field. This method allows
  optimal database update.
  NOT IMPLEMENTED
  */
  //public void update( Request r ) throws Exception {
  //}

  /**
  Returns the path of hibernate configuration file, hibernate.cfg.xml.
  */
  protected String getConfigPath() {
    //String configPath = properties.getProperty("vrspace.cfg");
    if ( configPath == null ) buildConfigPath();
    return configPath + "../etc/hibernate.cfg.xml";
  }
  protected void buildConfigPath() {
    configPath = Util.getDir(Util.getLocation(this).getPath());
    Logger.logDebug("Current config path: "+configPath+" separator: "+File.pathSeparator+" location: "+Util.getLocation(this));
    //if ( !configPath.endsWith(File.pathSeparator) ) configPath += File.pathSeparator;
  }

  /**
  Process hibernate.cfg.xml (returned by getConfigPath()) and configure hibernate engine.
  */
  private myConfiguration configure() {

    String cfgPath = getConfigPath();
    File configFile = new File( cfgPath );

    StringBuffer sb = new StringBuffer();
    boolean useCfg = useConfig && configFile.exists() && configFile.canRead();
    boolean writeCfg = writeConfig && (!configFile.exists() || ! useCfg);

    if ( useCfg ) {
      try {
        Logger.logInfo( "Configuring Hibernate using existing configuration file "+configPath );
        BufferedReader in = new BufferedReader( new FileReader( configFile ));
        String line = null;
        while ( (line = in.readLine()) != null ) {
          /*
          if ( line.indexOf( "hbm2ddl.auto" ) >= 0 ) {
            Logger.logDebug( "VRSpace config takes precedence, ignoring line "+line.trim() );
            addAutoCreateStatement( sb );
            continue;
          }
          if ( line.indexOf( "show_sql" ) >= 0 ) {
            Logger.logDebug( "VRSpace config takes precedence, ignoring line "+line.trim() );
            addShowSqlStatement( sb );
            continue;
          }
          */
          sb.append(line);
          sb.append("\n");
        }
        in.close();
      } catch ( IOException ioe ) {
        Logger.logError( "Cannot read "+cfgPath+" - using default configuration", ioe );
        sb = new StringBuffer();
        defaultConfig( sb );
      }
    } else {
      defaultConfig( sb );
    }

    String cfg = sb.toString();

    //Logger.logDebug("Hibernate configuration:\n"+ cfg);

    myConfiguration ret = new myConfiguration();

    try {
      ret.configure( cfg );
    } catch ( Exception e ) {
      if ( useCfg ) {
        Logger.logError( "Cannot configure Hibernate using "+cfgPath+", falling back to default configuration", e );
        sb = new StringBuffer();
        defaultConfig( sb );
        ret = new myConfiguration();
        cfg = sb.toString();
        ret.configure( cfg );
      } else {
        Logger.logError( "FATAL: Cannot configure Hibernate with these parameters, exiting", e );
        System.exit(1);
      }
    } finally {
      if ( writeCfg ) {
        try {
          Logger.logInfo("Writing hibernate configuration to "+cfgPath );
          FileWriter fw = new FileWriter( configFile );
          fw.write( cfg, 0, cfg.length() );
          fw.close();
        } catch ( IOException ioe ) {
          Logger.logError( "Cannot write Hibernate configuration file "+cfgPath, ioe );
        }
      }
    }

    conf = ret;

    Logger.logInfo("Hibernate configured.");
    return ret;
  }

  protected void defaultConfig( StringBuffer sb ) {
    String dbdriver = properties.getProperty(propertyBase+".driver"); //"org.postgresql.Driver"
    String url = properties.getProperty(propertyBase+".url"); //"//localhost/test";
    String username = properties.getProperty(propertyBase+".username"); //"postgres";
    String password = properties.getProperty(propertyBase+".password"); //"";
    String dialect = properties.getProperty(propertyBase+".dialect"); //"org.hibernate.dialect.PostgreSQLDialect";
    String sessionContext = properties.getProperty(propertyBase+".sessioncontext"); //thread
    String sessionTimeout = properties.getProperty(propertyBase+".sessiontimeout");
    if (sessionContext == null) sessionContext = "thread";
    if (sessionTimeout == null) sessionTimeout = "300";

    Logger.logInfo( "Configuring Hibernate: driver="+dbdriver+" url=jdbc:"+url+" user="+username+" dialect="+dialect );
    sb.append("<?xml version='1.0' encoding='utf-8'?>\n");
    sb.append("<!DOCTYPE hibernate-configuration PUBLIC\n");
    sb.append("\"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n");
    sb.append("\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\n");

    sb.append("<hibernate-configuration>\n");

    sb.append("  <session-factory>\n");

    //<!-- Database connection settings -->
    sb.append("    <property name=\"connection.driver_class\">"+dbdriver+"</property>\n");
    sb.append("    <property name=\"connection.url\">jdbc:"+url+"</property>\n");
    sb.append("    <property name=\"connection.username\">"+username+"</property>\n");
    sb.append("    <property name=\"connection.password\">"+password+"</property>\n");

    //<!-- JDBC connection pool (use the built-in) -->
    //sb.append("    <property name=\"connection.pool_size\">1</property>\n");
    // use c3p0 pool:
    sb.append("    <property name=\"hibernate.c3p0.min_size\">1</property>\n");
    sb.append("    <property name=\"hibernate.c3p0.max_size\">20</property>\n");
    sb.append("    <property name=\"hibernate.c3p0.max_statements\">50</property>\n");
    sb.append("    <property name=\"hibernate.c3p0.timeout\">"+sessionTimeout+"</property>\n");


    //<!-- SQL dialect -->
    sb.append("    <property name=\"dialect\">"+dialect+"</property>\n");

    //<!-- Enable Hibernate's automatic session context management -->
    sb.append("    <property name=\"current_session_context_class\">"+sessionContext+"</property>\n");
    //sb.append("    <property name=\"current_session_context_class\">jta</property>\n");
    //sb.append("    <property name=\"current_session_context_class\">managed</property>\n");

    //<!-- Enable the second-level cache  -->
    /*
    ENABLE_SELECTIVE (Default and recommended value): entities are not cached unless explicitly marked as cacheable.
    DISABLE_SELECTIVE: entities are cached unless explicitly marked as not cacheable.
    ALL: all entities are always cached even if marked as non cacheable.
    NONE: no entity are cached even if marked as cacheable. This option can make sense to disable second-level cache altogether.
    */
    if ( useCache ) {
      sb.append("    <property name=\"hibernate.cache.use_second_level_cache\">true</property>\n");
      sb.append("    <property name=\"hibernate.cache.region.factory_class\">org.hibernate.cache.ehcache.EhCacheRegionFactory</property>\n");
      sb.append("    <property name=\"javax.persistence.sharedCache.mode\">DISABLE_SELECTIVE</property>\n"); // CHECKME: doesn't change anything
      sb.append("    <property name=\"hibernate.cache.default_cache_concurrency_strategy\">read-write</property>\n"); //read-only, read-write, nonstrict-read-write, transactional
      sb.append("    <property name=\"hibernate.cache.use_query_cache\">true</property>\n");
    }

    //<!-- Echo all executed SQL to stdout -->
    addShowSqlStatement( sb );

    //<!-- Drop and re-create the database schema on startup -->
    addAutoCreateStatement( sb );

    // this is to avoid
    // org.hibernate.HibernateException: Unable to build the default ValidatorFactory
    // which happens when used with GWT
    sb.append("    <property name=\"javax.persistence.validation.mode\">none</property>");

    sb.append("  </session-factory>\n");

    sb.append("</hibernate-configuration>\n");
  }

  protected void addShowSqlStatement( StringBuffer sb ) {
    String sverbose = properties.getProperty(propertyBase+".verbose");
    boolean showsql = "true".equalsIgnoreCase(sverbose);
    sb.append("    <property name=\"show_sql\">"+showsql+"</property>\n");
  }

  protected void addAutoCreateStatement( StringBuffer sb ) {
    String screate = properties.getProperty(propertyBase+".create");
    boolean create = "true".equalsIgnoreCase(screate);
    /*
    DANGER!!!
    This may result in database being recreated next time we start the server.
    if ( create ) sb.append("    <property name=\"hbm2ddl.auto\">create</property>\n");
    */
    if ( create ) sb.append("    <property name=\"hbm2ddl.auto\">update</property>\n");
  }

  protected String getMappingPath() {
    //String configPath = properties.getProperty("vrspace.cfg");
    //configPath = Util.getDir( configPath ) + "vrspace.hbm.xml";
    if ( configPath == null ) buildConfigPath();
    return configPath + "../etc/ormapping.hbm.xml";
  }

  /**
  Adds O/R mapping XML to existing Configuration.
  Creates new Configuration as required.
  */
  protected SessionFactory addMappings() {
    SessionFactory ret = null;

    if ( conf == null ) {
      conf = configure();
    }

    DBObject.L2Caching( useCache );

    String mappingPath = getMappingPath();
    File mapFile = new File( mappingPath );
    boolean useCfg = useConfig && mapFile.exists() && mapFile.canRead();
    boolean writeCfg = writeConfig && (!mapFile.exists() || !useCfg);

    StringBuilder xml = new StringBuilder();

    if ( useCfg ) {
      try {
        Logger.logInfo( "Configuring Hibernate using existing mapping file "+mappingPath );
        BufferedReader in = new BufferedReader( new FileReader( mapFile ));
        String line = null;
        int pos=0;
        String table = null;
        while ( (line = in.readLine()) != null ) {
          if ( (pos = line.indexOf( "table" )) > 0 ) {
            int start = line.indexOf( "\"", pos+"table".length()+1 );
            int end = line.indexOf( "\"", start+1 );
            table = line.substring( start+1, end ).toLowerCase();
          } else if ( (pos = line.indexOf( "label" )) > 0 ) {
            // process and drop
            int start = line.indexOf( "\"", pos+"label".length()+1 );
            int end = line.indexOf( "\"", start+1 );
            String label = line.substring( start+1, end );
            line = line.substring( 0, pos ) + line.substring( end+1 );
            //Logger.logDebug( "Label = "+label+", line = "+line );
            if ( (pos = line.indexOf( "name=" )) > 0 ) {
              start = line.indexOf( "\"", pos+"name=".length() );
              end = line.indexOf( "\"", start+1 );
              String column = line.substring( start+1, end ).toLowerCase();
              //Logger.logDebug( "Property "+column+" label "+label );
              // CHECKME: lowercase?
              labels.put( table.toLowerCase()+"."+column.toLowerCase(), label );
              names.put( table.toLowerCase()+"."+label.toLowerCase(), column );
            } else {
              Logger.logWarning( "Invalid label "+label+" - no name" );
            }
          }
          xml.append( line );
          xml.append( "\n" );
        }
        in.close();
        buildORMap();
      } catch ( IOException ioe ) {
        Logger.logError( "Cannot read "+mappingPath+" - using default configuration", ioe );
        xml = new StringBuilder();
        defaultMapping( xml );
      }
    } else {
      defaultMapping( xml );
    }

    try {
      conf.addXML( xml.toString() );

      conf.buildMappings();
      ret = conf.buildSessionFactory();
    } catch ( Exception e ) {
      Logger.logError( "FATAL: Cannot build Hibernate mappings, exiting", e );
      //System.exit(1);
    }

    if ( writeCfg ) {
      try {
        Logger.logInfo("Writing hibernate mapping to "+mappingPath );
        FileWriter fw = new FileWriter( mapFile );
        fw.write( xml.toString(), 0, xml.length() );
        fw.close();
      } catch ( IOException ioe ) {
        Logger.logError( "Cannot write Hibernate configuration file "+mappingPath, ioe );
      }
    }

    return ret;
  }

  /**
  Used when hbm.xml file does not exist, or is invalid
  */
  protected void defaultMapping( StringBuilder xml ) {
    DBObject.addHibernateHeader( xml );
    // this way we don't have polymorphysm, but table per class structure
    //StringTokenizer st = new StringTokenizer( clPreload );
    //while (st.hasMoreTokens()) {
      //String className = st.nextToken();
      //}
    for ( String className: getClasses()) {
      try {
        DBObject obj = DBObject.newInstance( className );
        Logger.logDebug( "Processing mapping for "+obj );
        obj.addHibernateClass( xml );
        orMap.put( (obj.fieldPrefix+obj.getClassName()).toLowerCase(), obj.getClass() );
      } catch ( Exception e ) {
        Logger.logWarning( "Cannot map "+className+" - "+e );
      }
    }
    DBObject.addHibernateFooter( xml );
  }
  /**
  Used when hbm.xml file exists
  */
  protected void buildORMap() {
    //StringTokenizer st = new StringTokenizer( clPreload );
    //while (st.hasMoreTokens()) {
      //String className = st.nextToken();
    for ( String className: getClasses()) {
      try {
        DBObject obj = DBObject.newInstance( className );
        if ( obj != null ) {
          orMap.put( (obj.fieldPrefix+obj.getClassName()).toLowerCase(), obj.getClass() );
          //Logger.logDebug("Class "+obj.getClassName()+" mapped to table "+obj.fieldPrefix+obj.getClassName().toLowerCase());
        } else {
          Logger.logWarning( "Can't instantiate "+className+" - instance is null!!!" );
        }
      } catch ( Exception e ) {
        Logger.logWarning( "Cannot map "+className+" - "+e );
      }
    }
  }

  /**
  This extends org.hibernate.cfg.Configuration in order to make it read cfg from string
  */
  public class myConfiguration extends org.hibernate.cfg.Configuration {
    /**
    Reads configuration from XML string
    @param resourceString XML config string
    */
    protected InputStream getConfigurationInputStream( String resourceString ) {
      return new Util.StringInputStream( resourceString );
    }
  }

  /**
  Loads file content into the db. Default protocol for <b>url</b> is 'file'
  */
  public void load( String url ) throws Exception {
    URL u;
    try {
      u = new URL( url );
    } catch (MalformedURLException e) {
      url = "file:"+url;
      try {
        u = new URL( url );
      } catch ( MalformedURLException e1 ) {
        throw e;
      }
    }
    InputStream in = u.openStream();
    load( new InputStreamReader(in) );
  }
  /**
  Loads stream content into the database
  */
  public void load( Reader in ) throws Exception {
    BufferedReader reader = new BufferedReader( in );
    String line;
    StringBuffer text = new StringBuffer();
    long time = System.currentTimeMillis();
    while ( ( line = reader.readLine() ) != null ) {
      text.append(line + "\n");
    }
    Object[] objects = DBObject.fromText( text.toString() );
    time = System.currentTimeMillis()-time;
    Logger.logDebug( "DB: "+objects.length+" objects loaded in "+time/100+" seconds" );
    /*
    for ( int i = 0; i < objects.length; i++ ) {
      //Logger.logDebug( objects[i].toString() );
      ((DBObject)objects[i]).isNew = true;
      put( objects[ i ] );
      ((DBObject)objects[i]).isNew = false;
    }
    */
  }
  /**
  Loads file content into the database
  */
  public void load( File file ) throws Exception {
    load( new FileReader( file ));
  }

  /**
  Dumps the database to dump.db file
  */
  public void unload() {
    unload( new File( "dump.db" ));
  }
  public void unload( File file ) {
    if ( ! commit ) {
      commit = true;
      try {
        FileWriter out = new FileWriter( file );
        Vector objects = new Vector();
        //String[] tables = getClasses();
        // TODO: sort these tables in proper order
        // IOW first dump tables that contain no foreign keys
        String[] tables = getSortedClassNames();
        for ( int i = 0; i < tables.length; i++ ) {
          //org.hibernate.Session session = sessionFactory.getCurrentSession();
          //session.beginTransaction();
          transaction();
          java.util.List list = session().createQuery("from "+tables[i]).list();
          ListIterator li = list.listIterator();
          while ( li.hasNext() ) {
            DBObject obj = (DBObject)li.next();
            out.write( obj.toText() );
            // dump in toString() format:
            //out.write( obj.toString() );
            //out.write( ";\n" );
          }
          //session.getTransaction().commit();
          commit();
        }
        out.close();
        //String s = DBObject.toText( objects.toArray() );
      } catch ( Exception e ) {
        Logger.logError( "Unable to commit database", e );
      }
      commit=false;
    }
  }

  public Connection getConnection() {
    //sessionFactory.getCurrentSession().beginTransaction();
    transaction();
    //CHECKME:
    //return sessionFactory.getCurrentSession().connection();
    //connection() method no longer present in the interface; may be the same in implementation
    org.hibernate.internal.SessionImpl session = (org.hibernate.internal.SessionImpl)session();
    return session.connection();
  }
  public void closeConnection() {
    //FIXME!!!
    //sometimes we get
    //org.hibernate.HibernateException: connnection proxy not usable after transaction completion
    // if this is not commented out
    // check TVUtil, DBUtil
    //sessionFactory.getCurrentSession().getTransaction().commit();
  }
  public String getURL() {
    return url;
  }
  HashMap trans = new HashMap();
  public synchronized void transaction(){
    Transaction tran = session().getTransaction();
    Integer t = 0;
    if ( tran.isActive() ) {
      t = (Integer) trans.get( tran ) + 1; // NPE possible
      trans.put( tran, t );
    } else {
      tran = session().beginTransaction();
      trans.put( tran, 0 );
    }
  }
  public synchronized void commit(){
    // avoiding org.hibernate.exception.ConstraintViolationException: ERROR: duplicate key value violates unique constraint "something unique"
    //session().flush(); //CHECKME: same as flushmode.always?

    Transaction tran = session().getTransaction();
    Integer t = (Integer) trans.get(tran);
    if ( t == null ) throw new NullPointerException( "Cannot commit - no active transaction for current session!" );
    if ( t == 0 ) {
      //if ( tran.isActive() ) session().flush(); // same as flushmode.transaction
      tran.commit();
      trans.remove(tran); // needs to be before commit, in case commit produces exception - nope, then rollback won't work
    } else {
      trans.put( tran, t-1 );
    }
  }
  public synchronized void rollback(){
    //session().getTransaction().rollback();
    Transaction tran = session().getTransaction();
    if ( tran != null ) {
      Integer t = (Integer) trans.get(tran);
      if ( t == null ) throw new NullPointerException( "Cannot rollback - no active transaction for current session!" );
      if ( t == 0 ) {
        trans.remove(tran);
        tran.rollback();
      } else {
        trans.put( tran, t-1 );
      }
    } else {
      throw new NullPointerException("Trying to rollback without active transaction");
    }
  }
  public org.hibernate.Session session() {
    return sessionFactory.getCurrentSession();
  }
  public SessionFactory sessionFactory() {
    return sessionFactory;
  }
  public Session startSession() {
    Session ret = openSession();
    //ret.setFlushMode(FlushMode.ALWAYS);
    if ( autoFlush ) ret.setFlushMode(FlushMode.AUTO); // AUTO often results in bogus error reporting, i.e. value too long for value not currently being stored
    else ret.setFlushMode(FlushMode.COMMIT); // COMMIT produces constraint violation exceptions - during get! Possibly due to concurrency issues
    //ret.setFlushMode(FlushMode.MANUAL);
    bind( ret );
    return ret;
  }
  public void endSession(Session s) {
    unbind();
    s.close();
  }
  public void endSession() {
    Session s = unbind();
    if ( s != null ) s.close();
    //s.close(); // may throw NPE - testing
  }
  public Session openSession() {
    return sessionFactory().openSession();
  }
  public void bind( Session session ) {
    org.hibernate.context.internal.ManagedSessionContext.bind( session );
  }
  public Session unbind() {
    return org.hibernate.context.internal.ManagedSessionContext.unbind( sessionFactory() );
  }
  public void close() {
    sessionFactory().close();
    initialized = false;
  }
  public Class getClass( String table ) {
    return (Class) orMap.get(table.toLowerCase());
  }
  public String getLabel( String table, String column ) {
    return (String) labels.get( table.toLowerCase()+"."+column.toLowerCase() );
  }
  public String getName( String table, String label ) {
    return (String) names.get( table.toLowerCase()+"."+label.toLowerCase() );
  }

  public static HibernateDB getInstance() {
    return firstInstance;
  }

  public String[] getSortedClassNames() throws Exception {
    TreeSet classes = processClasses();
    Iterator it = classes.iterator();
    String[] ret = new String[ classes.size() ];
    int i = 0;
    while (it.hasNext()) {
      ClassMetaData cmd = (ClassMetaData) it.next();
      ret[i++] = cmd.toString();
    }
    return ret;
  }
  /**
  Processes all available classes and sorts them by depth.
  Depth relates to number of classes that are member of each class.
  I.e. class A has member B, B has members C and D, C has member E, F has members E and D.
  Depths are A:3 B:2 C:1 D:0 E:0 F:1.
  @return classes (ClassMetaData) sorted by depth.
  */
  TreeSet processClasses() throws Exception {
    String[] classes = getClasses();
    HashMap classMap = new HashMap();
    TreeSet ret = new TreeSet( new ClassComparator() );
    // step 1: prepare class metadata
    for ( int i = 0; i < classes.length; i++ ) {
      ClassMetaData cmd = new ClassMetaData( classes[i] );
      classMap.put( cmd.name, cmd );
    }
    // step 2: update parents for each member - this isn't required for depth calculation
    Iterator ci = classMap.keySet().iterator();
    while ( ci.hasNext() ) {
      ClassMetaData cmd = (ClassMetaData)classMap.get( ci.next() );
      Iterator it = cmd.children.iterator();
      cmd.depth = 0; // 0 = no children
      while ( it.hasNext() ) {
        Class cls = (Class) it.next();
        ClassMetaData child = (ClassMetaData)classMap.get(cls.getName());
        child.parents.add( cmd.cls );
      }
    }
    // step 3: calculate depths
    ci = classMap.keySet().iterator();
    while ( ci.hasNext() ) {
      ClassMetaData cmd = (ClassMetaData)classMap.get( ci.next() );
      //Logger.logDebug( "Calculating depth for "+cmd.name );
      cmd.depth = calcDepth( cmd, classMap );
      Logger.logDebug( "Class "+cmd.name+" depth="+cmd.depth );
      ret.add( cmd );
    }
    // and now ret contains classes sorted by depth
    return ret;
  }

  int calcDepth( ClassMetaData cmd, HashMap classMap ) {
    if ( cmd.processing ) {
      if ( ! cmd.circular ) {
        Logger.logWarning( "Circular dependency for class "+cmd.name );
        cmd.circular = true;
      }
      return 1; // ???
    }
    cmd.processing = true;
    int ret = 0; // 0 = no children
    Iterator it = cmd.children.iterator();
    while ( it.hasNext() ) {
      //ClassMetaData child = (ClassMetaData) it.next();
      Class cls = (Class) it.next();
      ClassMetaData child = (ClassMetaData)classMap.get(cls.getName());
      int depth = 1+calcDepth( child, classMap );
      ret = Math.max( ret, depth );
    }
    cmd.processing = false;
    return ret;
  }

  public class ClassComparator implements Comparator {
    public int compare( Object o1, Object o2 ) {
      ClassMetaData cmd1 = (ClassMetaData)o1;
      ClassMetaData cmd2 = (ClassMetaData)o2;
      int ret = cmd1.depth - cmd2.depth;
      if (ret == 0) {
        // classes of same depth
        // sub-order them by alphabet
        ret = cmd1.name.compareTo( cmd2.name );
      }
      return ret;
    }
  }

  public class ClassMetaData {
    public int depth;
    public String name;
    public Class cls;
    public HashSet parents = new HashSet();
    public HashSet children = new HashSet();
    boolean processing = false;
    boolean circular = false;
    public ClassMetaData( String name ) throws ClassNotFoundException {
      //this( Class.forName(name) );
      this( DBObject.newInstance(name).getClass());
    }
    public ClassMetaData( Class cls ) {
      this.cls = cls;
      this.name = cls.getName();
      Field[] fields = getPublicFields(cls);
      for ( int i = 0; i < fields.length; i++ ) {
        if ( DBObject.class.isAssignableFrom( fields[i].getType() ) ) {
          // DBObject member = foreign key
          children.add( fields[i].getType() );
        }
      }
    }
    public boolean hasChildren() {
      return children != null && children.size() > 0;
    }
    public Field[] getPublicFields( Class cls) {
      Vector ret = new Vector();
      Field [] fields = cls.getFields();
      for ( int i = 0; i < fields.length; i++ ) {
        try {
          int mods = fields[i].getModifiers();
          if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) ) {
            ret.add(fields[i]);
          }
        } catch (Exception e) {
          // this cannot happen
          Logger.logError(e);
        }
      }
      return (Field[]) ret.toArray( new Field[ret.size()] );
    }
    public String toString() {
      return name;
    }
  }
  /*
  public void update( Request req ) throws Exception {
    if ( req != null && req.object != null ) {
      put( req.object );
    }
  }
  */
  public static <T> T materialize(T entity) {
    if (entity == null) {
      throw new NullPointerException("Entity passed for initialization is null");
    }

    Hibernate.initialize(entity);
    if (entity instanceof org.hibernate.proxy.HibernateProxy) {
        entity = (T) ((org.hibernate.proxy.HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
    }
    return entity;
  }

}