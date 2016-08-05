package org.vrspace.util;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

/**
First created Logger is accessible from static methods.
Other loggers are not tested.
*/
public class Logger implements Runnable {
  static boolean isLogHeader=true;
  static boolean isLogInfo=true;
  static boolean isLogDebug=true;
  static boolean isLogError=true;
  static boolean isLogWarning=true;
  static boolean isVerbose=true;
  static boolean stopOnError=false;
  protected static int logLevel = Integer.MAX_VALUE;
  //static PrintStream out;
  static PrintWriter out = new PrintWriter( System.out );
  //static StringBuffer buffer = new StringBuffer();
  protected boolean active = true;
  static Thread thread;
  protected static boolean stop = false;

  public static final int ERROR = 0;
  public static final int WARNING  = 1;
  public static final int INFO = 2;
  public static final int DEBUG = 3;
  static final String[] desc = { "ERROR", "WARNING", "INFO", "DEBUG" };
  static Calendar calendar = Calendar.getInstance();
  protected static Logger logger = new Logger();

  public Logger() {
    if (logger == null) {
      //buffer=new StringBuffer();
      logger=this;
      (thread = new Thread( this, "Logger" )).start();
    } else {
      (new Thread( this, "Logger" )).start();
    }
    Runtime.getRuntime().addShutdownHook(new Thread( new LoggerHook(), "LoggerHook" ));
  }
  /**
  Creates Logger writing to <b>out</b>
  */
  public Logger( PrintWriter out ) {
    if (logger == null) {
      //this.out=out;
      this.out = new PrintWriter( out );
      //buffer=new StringBuffer();
      logger=this;
      (thread = new Thread( this, "Logger" )).start();
    } else {
      (new Thread( this, "Logger" )).start();
    }
    Runtime.getRuntime().addShutdownHook(new Thread( new LoggerHook(), "LoggerHook" ));
  }
  /**
   * Stop the main logger.
   */
  public static void stopStaticLogger() {
    if ( logger != null ) logger.active = false;
    if ( thread != null && thread.isAlive() )
    try {
      thread.interrupt();
      thread.join();
    } catch ( InterruptedException ie ) {} // who cares
    thread = null;
    logger = null;
  }
  /**
   * Start the main logger if it hasn't been started yet.
   */
  public static void startStaticLogger() {
    if ( ( logger == null ) || ( ! thread.isAlive() ) ) {
      logger = null;
      new Logger();
    }
  }
  /**
  main loop
  */
  public void run() {
    try {
      while ( active ) {
        try {
          flush();
          if ( stop ) {
            System.exit(1);
          }
          Thread.sleep( 1000 );
        } catch ( InterruptedException e ) {
          active = false;
        }
      }
    } catch ( Throwable e ) {
      //Logger.logError( e );
      e.printStackTrace();
      active = false;
    }

    //logDebug( "Logger Finished" );
    flush();
  }

  void flush() {
    /*
    if ( buffer.length() > 0 ) {
      if ( isVerbose ) {
        //Logger.logDebug(buffer.toString());
        System.out.print(buffer.toString());
      }
      //if ( out instanceof PrintStream ) {
      if ( out instanceof PrintWriter ) {
        //out.println(buffer.toString());
        out.print(buffer.toString());
      }
      buffer = new StringBuffer();
    }
    */
    if ( out != null ) out.flush();
  }
  /*
  static void log( String type, String line ) {
    Calendar c=Calendar.getInstance();
    buffer.append(type+" "+c.get(c.YEAR)+"/"+c.get(c.MONTH)+"/"+c.get(c.DATE)+" "+c.get(c.HOUR_OF_DAY)+":"+c.get(c.MINUTE)+":"+c.get(c.SECOND)+" "+line+"\n");
  }
  */
  private static void logHeader( int severity ) {
    calendar.setTime(new Date());
    out.print( "[" );
    out.print( calendar.get(Calendar.YEAR) );
    out.print( "/" );
    int tmp = calendar.get(Calendar.MONTH)+1;
    if (tmp < 10) out.print("0");
    out.print( tmp );
    out.print( "/" );
    tmp = calendar.get(Calendar.DATE);
    if (tmp < 10) out.print("0");
    out.print( tmp );
    out.print( " " );
    tmp = calendar.get(Calendar.HOUR_OF_DAY);
    if (tmp < 10) out.print("0");
    out.print( tmp );
    out.print( ":" );
    tmp = calendar.get(Calendar.MINUTE);
    if (tmp < 10) out.print("0");
    out.print( tmp );
    out.print( ":" );
    tmp = calendar.get(Calendar.SECOND);
    if (tmp < 10) out.print("0");
    out.print( tmp );
    out.print( "." );
    out.print( calendar.get(Calendar.MILLISECOND) );
    out.print( "] " );
    if ( severity < desc.length ) {
      out.print( desc[severity] );
      out.print( " " );
    } else {
      out.print( "CUSTOM(" );
      out.print( severity );
      out.print( ") " );
    }
    out.print( Thread.currentThread().getName() );
    out.print( " " );
    out.print( ": " );
  }
  /**
  Log msg with specified severity, if severity is less or equal to current log level
  */
  public static void log( int severity, String msg ) {
    if ( logger != null ) {
      logger.logString( severity, msg );
    } else {
      out.println( msg );
    }
  }
  /**
  Log msg with specified severity, if severity is less or equal to current log level
  */
  public static void log( String msg, Throwable t ) {
    if ( logger != null ) {
      logger.logThrowable( msg, t );
    } else {
      out.println( msg );
    }
  }
  /**
  Log error
  */
  public static void log( Throwable t ) {
    if ( logger != null ) {
      logger.logThrowable( t.getMessage(), t );
    } else {
      t.printStackTrace( out );
    }
  }
  protected void logString( int severity, String msg ) {
    if ( severity <= logLevel ) {
      synchronized( out ) {
        if ( isLogHeader )
          logHeader( severity );
        out.println( msg );
      }
    }
  }
  protected void logThrowable( Throwable t ) {
    synchronized ( out ) {
      logHeader(ERROR);
      t.printStackTrace( out );
    }
  }
  protected void logThrowable( String msg, Throwable t ) {
    synchronized ( out ) {
      logHeader(ERROR);
      out.println( msg );
      logHeader(ERROR);
      t.printStackTrace( out );
    }
  }
  /**
   * Include the log header?
   */
  public static void logHeader( boolean b ) {
    isLogHeader = b;
  }
  /**
  Log errors?
  */
  public static void logError( boolean b ) {
    isLogError = b;
  }
  /**
  Log info?
  */
  public static void logInfo( boolean b ) {
    isLogInfo=b;
  }
  /**
  Log debugging info?
  */
  public static void logDebug( boolean b ) {
    logInfo( "LogDebug = "+b );
    isLogDebug=b;
  }
  /**
  Log warnings?
  */
  public static void logWarning( boolean b ) {
    isLogWarning=b;
  }
  /**
  Set log level
  */
  public static void setLogLevel( int level ) {
    logLevel = level;
  }
  /**
  returns current log level
  */
  public static int getLogLevel() {
    return logLevel;
  }
  /**
  Exit jvm on error?
  */
  public static void stopOnError( boolean stop ) {
    stopOnError=stop;
  }
  /**
  Log <b>line</b> to info stream
  */
  public static void logInfo( String line ) {
    if (isLogInfo) {
      log( INFO, line );
    }
  }
  /**
  Log <b>line</b> to debug stream
  */
  public static void logDebug( String line ) {
    if (isLogDebug) {
      log( DEBUG, line );
    }
  }
  /**
  Log <b>line</b> to warning stream
  */
  public static void logWarning( String line ) {
    if (isLogWarning) {
      log( WARNING, line );
    }
  }
  /**
  Log <b>line</b> to error stream
  */
  public static void logError( String line ) {
    //thread.interrupt();
    if (isLogError) {
      log( ERROR, line );
    }
    stop=stopOnError;
  }
  /**
  Log <b>e</b> to error stream
  */
  public static void logError( Throwable e ) {
    if (isLogError && e != null) {
      //thread.interrupt();
      log( e );
      if ( isVerbose ) {
        e.printStackTrace(System.out);
      }
      /**
      if ( out instanceof PrintWriter ) {
        //e.printStackTrace( out );
        log( e );
      }
      */
    }
    stop=stopOnError;
  }
  public static void logError( String msg, Throwable e ) {
    log( msg, e );
  }
  public class LoggerHook implements Runnable {
    public void run() {
      flush();
      active = false;
    }
  }
}
