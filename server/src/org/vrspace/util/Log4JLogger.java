package org.vrspace.util;

import java.lang.reflect.*;

public class Log4JLogger extends Logger {
  static Method warn;
  static Method info;
  static Method debug;
  static Method error;
  static Method errort;
  static Method push;
  static Method pop;
  protected static Object log4j;
  String name;
  public Log4JLogger( String name ) {
    this.name = name;
    if ( logger == null || ! (logger instanceof Log4JLogger) ) {
      // we do it like this couse we don't require log4j to compile vrspace
      try {
        Class cl = Class.forName("org.apache.log4j.Logger");
        Method method = cl.getMethod( "getLogger", new Class[] { String.class } );
        log4j = method.invoke( null, new Object[] { name } );
        if ( log4j != null ) {
          System.out.println( "Log4J found!" );
          debug = cl.getMethod( "debug", new Class[] { Object.class } );
          info = cl.getMethod( "info", new Class[] { Object.class } );
          warn = cl.getMethod( "warn", new Class[] { Object.class } );
          error = cl.getMethod( "error", new Class[] { Object.class } );
          errort = cl.getMethod( "error", new Class[] { Object.class, Throwable.class } );
          Class ndc = Class.forName("org.apache.log4j.NDC");
          push = ndc.getMethod( "push", new Class[] { String.class } );
          pop = ndc.getMethod( "pop", new Class[] {} );
          logger = this;
          if ( log4j != null ) {
            log( INFO, "Log4JLogger initialized!" );
          }
          active = false; // stop flush thread - we don't need it any longer
        } else {
          System.out.println("OOPS - null log4J?!");
        }
      } catch ( Throwable t ) {
        System.err.println("Log4j not available, using dumb logger - "+t);
        if ( logger == null ) {
          logger = new Logger();
        }
      }
    }
  }
  /**
  Log msg with specified severity, if severity is less or equal to current log level
  */
  protected void logString( int severity, String msg ) {
    if ( severity <= logLevel ) {
      try {
        pushNDC();
        if ( log4j == null ) {
          if ( logger != null ) {
            logger.log( severity, msg );
          } else {
            System.err.println( msg );
          }
        } else if ( severity == DEBUG ) {
          debug.invoke( log4j, new Object[] { msg } );
        } else if ( severity == INFO ) {
          info.invoke( log4j, new Object[] { msg } );
        } else if ( severity == WARNING ) {
          warn.invoke( log4j, new Object[] { msg } );
        } else if ( severity == ERROR ) {
          error.invoke( log4j, new Object[] { msg } );
        } else {
          // ?
          System.err.println( msg );
        }
        popNDC();
      } catch ( Throwable t ) {
        System.err.println( "FATAL ERROR IN LOGGER - SHUTDOWN PROCEEDING..." );
        t.printStackTrace( System.err );
        System.exit( 1 );
      }
    }
  }
  protected void logThrowable( Throwable t ) {
    try {
      pushNDC();
      if ( log4j == null ) {
        if ( logger != null ) {
          logger.log( t );
        } else {
          t.printStackTrace( System.err );
        }
      } else  {
        error.invoke( log4j, new Object[] { t } );
      }
      popNDC();
    } catch ( Throwable fatal ) {
      System.err.println( "FATAL ERROR IN LOGGER - SHUTDOWN PROCEEDING..." );
      fatal.printStackTrace( System.err );
      System.err.println( "Coused by: " );
      t.printStackTrace( System.err );
      System.exit( 1 );
    }
  }
  protected void logThrowable( String msg, Throwable t ) {
    try {
      pushNDC();
      if ( log4j == null ) {
        if ( logger != null ) {
          logger.log( t );
        } else {
          t.printStackTrace( System.err );
        }
      } else  {
        errort.invoke( log4j, new Object[] { msg, t } );
      }
      popNDC();
    } catch ( Throwable fatal ) {
      System.err.println( "FATAL ERROR IN LOGGER - SHUTDOWN PROCEEDING..." );
      fatal.printStackTrace( System.err );
      System.err.println( "Coused by: " );
      t.printStackTrace( System.err );
      System.exit( 1 );
    }
  }
  protected void pushNDC() throws Exception {
    push.invoke( null, new Object[] { Thread.currentThread().getName() } );
  }
  protected void popNDC() throws Exception {
    pop.invoke( null, (Object[])null );
  }
}
