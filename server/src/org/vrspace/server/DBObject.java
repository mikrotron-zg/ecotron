package org.vrspace.server;

import org.vrspace.util.*;
import java.lang.reflect.*;
import java.util.*;

// class search stuff:
import java.io.*;
import java.util.zip.*;
import java.net.*;

//import javax.persistence.*;
//import java.lang.annotation.*;

public class DBObject implements Cloneable {
  /** date format - CHECKME */
  public static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  /** This array specifies indexed fields */
  public static final String[] _index = new String[0];
  /** This array specifies fields with unique indexes */
  public static final String[] _indexUnique = new String[0];
  /** Object ID; must be unique with class */
  public long db_id;
  /** Primitive types that can be processed: key is primitive, value is object*/
  public static final HashMap primitives = new HashMap();
  /** Primitive types that can be processed: key is object, value is primitive*/
  public static final HashMap primitiveMap = new HashMap();
  //public static final String fieldPrefix = "mikrotron_";
  /** Database field names will be prefixed with this prefix */
  public static final String fieldPrefix = "";

  public static final int FKEY_INDEX = 0;
  public static final int FKEY_ID = 1;
  static int fkeyPolicy = FKEY_ID;

  public static final int ID_INCREMENT = 0;
  public static final int ID_NATIVE = 1;
  static int idType = ID_NATIVE;

  static boolean cacheCollections = false;
  static boolean L2Caching = false;
  public static boolean _cache = false;

  private String myClassName;
  private int pos;
  static List<String> packages = new LinkedList<String>();
  static {
    primitives.put( "boolean", new Boolean( false ) );
    primitives.put( "char", new Character( ' ' ) );
    primitives.put( "byte", new Byte( "0" ) );
    primitives.put( "short", new Short( "0" ) );
    primitives.put( "int", new Integer( 0 ) );
    primitives.put( "long", new Long( 0 ) );
    primitives.put( "float", new Float( 0 ) );
    primitives.put( "double", new Double( 0 ) );
    //primitives.put( "void", new Void() );

    primitiveMap.put( Boolean.class, "boolean" );
    primitiveMap.put( Character.class, "char" );
    primitiveMap.put( Byte.class, "byte" );
    primitiveMap.put( Short.class, "short" );
    primitiveMap.put( Integer.class, "int" );
    primitiveMap.put( Long.class, "long" );
    primitiveMap.put( Float.class, "float" );
    primitiveMap.put( Double.class, "double" );
  }
  static Hashtable knownClasses = new Hashtable();
  /**
  Adds Hibernate mapping xml header to passed StringBuffer.
  */
  public static void addHibernateHeader( StringBuilder ret ) {
    ret.append( "<?xml version=\"1.0\"?><!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">\n" );
    ret.append( "<hibernate-mapping default-access=\"field\">\n" );
  }
  /**
  Adds hibernate mapping footer (closing hibernate-mapping tag) to passed StringBuffer
  */
  public static void addHibernateFooter( StringBuilder ret ) {
    ret.append("</hibernate-mapping>\n"); //CHECKME: required?
  }

  /**
  Adds Hibernate class tag to passed StringBuffer.
  */
  public void addHibernateClass( StringBuilder ret ) {
    ret.append( "  <class name=\"" );
    //ret.append( getClassName() );
    ret.append( getClass().getName() );
    // CHECKME: table naming strategy may allow multiple servers store in one db
    ret.append( "\" table=\"" );
    ret.append( fieldPrefix );
    //ret.append( getClassName() );
    ret.append( getTableName() );
    ret.append( "\">\n" );

    // CHECKME: cache usage
    processCache(ret);

    ret.append( "    <id name=\"db_id\" column=\"" );
    ret.append( fieldPrefix );
    ret.append( "db_id\" access=\"field\">\n" );
    if ( idType == ID_NATIVE ) {
      //ret.append( "      <generator class=\"native\"/>\n" ); // native generator uses a single sequence to generate id
      ret.append( "      <generator class=\"native\">\n" ); // so let's specify sequence name as generator parameter
      //ret.append( "        <param name=\"sequence\">"+getTableName()+"_idseq</param>\n" );
      ret.append( "        <param name=\"sequence\">"+getTableName()+"_db_id_seq</param>\n" ); // default naming for postgres
      ret.append( "      </generator>\n" );
    } else if ( idType == ID_INCREMENT ) {
      ret.append( "      <generator class=\"increment\"/>\n" ); // increment may cause troubles if multiple processes fill the database
    }
    ret.append( "    </id>\n" );

    //now property tags
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
        boolean naturalId = false;
        StringBuilder sb = new StringBuilder();
        if ( DBObject.class.isAssignableFrom( fields[i].getType() ) ) {
          /*
          <many-to-one name="field" column="field" class="org.vrspace.server.db.Something" property-ref="referringTable" access="field"/>
          */
          try {
            sb.append("    <many-to-one name=\"");
            sb.append( fields[i].getName() );
            sb.append( "\" column=\"" );
            sb.append( fieldPrefix );
            //sb.append( fields[i].getName() ); //mixed case
            sb.append( fields[i].getName().toLowerCase() );
            sb.append( "\" class=\"" );
            sb.append( fields[i].getType().getName());
            sb.append( "\"");
            //sb.append( " fetch=\"join\""); //using join to avoid too many select statements - CHECKME: this actually may slow things down

            DBObject tmp = (DBObject)fields[i].getType().newInstance();

            /*
            // self-reference check - hibernate may fail on this, i.e.
            public class Folder extends DBObject {
              public String name;
              public Folder parent; // Hibernate fail: NPE in Entity.getId() for root folder has null parent
            }
            */
            if ( tmp.getClass().equals( getClass() ) ) {
              Logger.logWarning( "Self-referring entity "+getTableName()+"."+fields[i].getName() );
            }

            //String[] tmpFields = tmp.getPublicFields();
            //sb.append( tmpFields[0] );
            String keyField = "db_id";
            if ( fkeyPolicy == FKEY_INDEX ) {
              keyField = tmp.getKeyFieldName();
              if ( keyField == null ) {
                Logger.logWarning("Class "+tmp.getClass().getName()+" does not have primary key field, but is member of "+getClass().getName()+" - using db_id instead");
                keyField = "db_id";
              } else {
                // property-ref should be used ONLY if using properties other than primary key
                // for primary key it produces NPE in getID()
                sb.append(" property-ref=\"" );
                sb.append( keyField );
                sb.append("\"");
              }
            }
            // process annotated indexes
            //processIndex( fields[i], this, sb );
            // CHECKME: this may fail due to lacking quotes
            processIndex( fields[i], "_index", this, sb );
            processIndex( fields[i], "_indexUnique", this, sb );
            //sb.append("\"");

            //sb.append(" entity-name=\""+getClassName()+tmp.getClassName()+keyField+"\" "); //what's this entity-name for?
            sb.append(" access=\"field\"");

            sb.append("/>\n" );
          } catch ( Exception e ) {
            Logger.logError( "Error generating mapping for "+fields[i].getName(), e );
          }
        } else if ( fields[i].getType().isArray() ) {
          // Transform contains array of ID objects
          // workaround: requires manual setters/getters
          Class compType = fields[i].getType().getComponentType();
          //Logger.logWarning(getTableName()+"."+fields[i].getName()+" - array of "+compType.getName());
          //if ( compType.isAssignableFrom(getClass()) ) {
          if ( DBObject.class.isAssignableFrom(compType)) {
            try {
              // arrays of vrspace objects
              //Logger.logWarning("DBObject array member:"+getTableName()+"."+fields[i].getName()); // no warning necessary, seems to work now
              /*
              // FIXME: check for getters/setters
              if ( ! checkMethods( fields[i].getName() )) {
                Logger.logWarning( "No getter/setter for "+getTableName()+"."+fields[i].getName());
              }
              */
              /*
              // VRSpace specifics
              // we can store these as Sets. Doesn't make much sense as long as we use one child VRObject per Transform
              sb.append("    <property name=\"");
              sb.append( fields[i].getName() );
              sb.append( "\" column=\"" );
              sb.append( fieldPrefix );
              sb.append( fields[i].getName() );
              sb.append("\" access=\"property\" type=\"string\"/>\n" );
              */

              // Mapping arrays as many-to-many
              DBObject tmp = (DBObject)compType.newInstance();
              mapCollection( sb, tmp, fields[i] );
            } catch ( Exception e ) {
              Logger.logError( "Error generating mapping for "+fields[i].getName(), e );
            }
          } else {
            //Hibernate knows how to store arrays
            sb.append("    <property name=\"");
            sb.append( fields[i].getName() );
            sb.append( "\" column=\"" );
            sb.append( fieldPrefix );
            sb.append( fields[i].getName() );
            if ( byte.class.isAssignableFrom(compType)) {
              // maps to blob
              sb.append("\" type=\"binary\" length=\"");
              // see if we have length specified in this instance
              try {
                byte[] tmp = (byte[]) fields[i].get( this );
                if ( tmp == null || tmp.length == 0 ) {
                  sb.append( 1042*1024*32 ); // 32M should map to 4-byte blobs
                } else {
                  sb.append( tmp.length );
                }
              } catch ( IllegalAccessException iae ) {
                Logger.logError(iae);
              }
            }
            //sb.append("\" access=\"field\"/>\n" ); //default
            sb.append("\"/>\n" ); //default
          }
        } else if ( Collection.class.isAssignableFrom(fields[i].getType()) ) {
          Type compType = fields[i].getGenericType();
          if ( compType instanceof ParameterizedType ) {
            // i.e. List<DBObject>
            Type[] types = ((ParameterizedType)compType).getActualTypeArguments();
            if ( types.length > 1 ) Logger.logWarning( types.length+" generics in "+this.getClassName()+"."+fields[i].getName());
            for ( Type argType: types ) {
              // this probably contains only one element
              Class cls = (Class) argType;
              if ( DBObject.class.isAssignableFrom( cls )) {
                try {
                  // yep, List<DBObject>
                  Logger.logDebug("Mapping collection "+this.getClassName()+"."+fields[i].getName()+" of type "+cls.getName());
                  DBObject tmp = (DBObject)cls.newInstance();
                  mapCollection( sb, tmp, fields[i] );
                } catch ( Exception e ) {
                  Logger.logError( "Error generating mapping for "+fields[i].getName(), e );
                }
              }
            }
          } else {
            // CHECKME
            Logger.logError( "Don't know how to map "+this.getClassName()+"."+fields[i].getName());
          }
        } else if ( Map.class.isAssignableFrom(fields[i].getType()) ) {
          /* i.e.
          <map name="clientProperties">
            <key column="appuser_id"/>
            <index column="name" type="string"/>
            <element column="value" type="string"/>
          </map>
          */
          Type compType = fields[i].getGenericType();
          String[] mapParams = {"index", "element"};
          String[] mapNames = {"name", "value"};
          if ( compType instanceof ParameterizedType ) {
            sb.append( "    <map name=\"" );
            sb.append( fields[i].getName() );
            sb.append( "\">\n" );
            sb.append( "      <key column=\"" );
            sb.append( getTableName() );
            sb.append( "_id\"/>\n" );
            //sb.append( "      <cache usage=\"read-write\"\n/>"); //Can't cache maps
            // i.e. Map<String,String>
            Type[] types = ((ParameterizedType)compType).getActualTypeArguments();
            for ( int t = 0; t<types.length; t++ ) {
              sb.append( "      <" );
              sb.append( mapParams[t] );
              sb.append( " column=\"" );
              //sb.append( fields[i].getName() );
              //sb.append( "_" );
              sb.append( mapNames[t] );
              sb.append( "\"" );
              Type argType = types[t];
              // this probably contains only one element
              Class cls = (Class) argType;
              if ( DBObject.class.isAssignableFrom( cls )) {
                Logger.logError( "Don't know how to map "+this.getClassName()+"."+fields[i].getName()+" - map can't contain DBObjects" );
              } else if ( String.class.isAssignableFrom( cls )) {
                sb.append( " type=\"string\"" );
              } else {
                Logger.logError( "Don't know how to map "+this.getClassName()+"."+fields[i].getName()+" - only strings supported" );
              }
              sb.append( "/>\n" );
            }
            sb.append( "    </map>\n" );
          } else {
            Logger.logError( "Don't know how to map "+this.getClassName()+"."+fields[i].getName()+" - map must have type parameters" );
          }
        } else {
          // First we check for string setters and getters,
          // they still may be used for types that hibernate can't handle.
          boolean isMethod = checkMethods( fields[i].getName() );

          sb.append("    <property name=\"");
          sb.append( fields[i].getName() );
          sb.append( "\" column=\"" );
          sb.append( fieldPrefix );
          sb.append( fields[i].getName() );
          sb.append( "\"" );
          //processIndex( fields[i], this, sb );
          processIndex( fields[i], "_index", this, sb );
          //naturalId = processIndex( fields[i], "_indexUnique", this, sb );
          naturalId = processIndex( fields[i], "_indexUnique", this, sb );
          //sb.append("\" access=\"field" );
          if ( isMethod ) sb.append( "\" access=\"property" );
          sb.append("/>\n" ); //default
        }
        // TODO: natural-id
        // rules: only one natural id, must be place right after id
        //if ( naturalId ) {
          //ret.append( "    <natural-id>\n" );
          //ret.append( "  " );
          //ret.append(sb);
          //ret.append( "    </natural-id>\n" );
        //} else {
          ret.append(sb);
        //}
      }
    }
    ret.append("  </class>\n");
  }
  /**
  Process an array or collection and produce either one-to-many or many-to-many mapping.
  FIXME: creating one-to-many based only on member unique index is wrong heuristics, i.e. we should be able to create unique index to ensure consistency
  @param ret StringBuffer containing hibernate mapping
  @param obj DBObject component (array or list member)
  */
  void mapCollection(StringBuilder ret, DBObject member, Field field) {
    Class cls = field.getType();
    String what = null;
    if ( cls.isArray() ) {
      what = "array";
    } else if ( List.class.isAssignableFrom( cls )) {
      what = "list";
    } else if ( SortedSet.class.isAssignableFrom( cls )) {
      what = "list"; //CHECKME - this may be wrong mapping
    } else if ( Set.class.isAssignableFrom( cls )) {
      what = "set";
    } else {
      Logger.logError( "Don't know how to map "+cls.getName() );
    }
    ret.append( "    <");
    ret.append( what );
    ret.append( " name=\"" );
    ret.append( field.getName() ); // field name, must be case-sensitive
    ret.append( "\" access=\"field\"");
    ret.append( " table=\"" );
    ret.append( getTableName() );
    ret.append( "_" );
    ret.append( field.getName().toLowerCase() ); //for case-sensitive databases
    ret.append( "\"" );

    //cascade = all is sort of mandatory for one-to-many
    Logger.logDebug("Member "+field.getName()+" unique index: "+member.getKeyFieldName() );
    //if ( member.getKeyFieldName() == null ) {
    boolean oneToMany = oneToMany( member );
    if ( oneToMany ) {
      // one-to-many - cascade operations
      ret.append( " cascade=\"all\"" );
    }
    //ret.append( " fetch=\"join\""); //using join to avoid too many select statements - CHECKME: this actually may slow things down
    ret.append( ">\n" );

    if ( cacheCollections ) ret.append( "      <cache usage=\"read-only\"/>\n"); //CHECKME - cache

    // key column: classname_member_id
    ret.append( "      <key column=\"" );
    //ret.append( getClassName() ); // sort of parent-id
    ret.append( getTableName() ); // should be lowercase
    ret.append( "_id\"/>\n" );
    //ret.append( "_id\" not-null=\"true\"/>\n" ); // CHECKME: mostly produces errors during insert

    // for arrays, sorted sets and lists, we need to include index column
    if ( cls.isArray() || List.class.isAssignableFrom( cls ) || SortedSet.class.isAssignableFrom(cls) ) { //CHECKME: sortedset
      ret.append( "      <list-index column=\"" );
      //ret.append( member.getClassName() );
      ret.append( member.getTableName() );//should be lowercase
      ret.append( "_no\"/>\n" );
    }

    if ( oneToMany ) {
      // array member has no natural primary key -> one to many
      ret.append( "      <one-to-many class=\"" );
    } else {
      // array member with natural primary key -> many to many
      ret.append( "      <many-to-many class=\"" );
    }
    ret.append( member.getClass().getName() );
    ret.append( "\" ");
    //if ( member.getKeyFieldName() != null ) {
    if ( ! oneToMany ) {
      // one to many: must not have column attribute!
      // hibernate reports that opposite: one-to-many must have column attribute
      ret.append( "column=\"");
      //ret.append( member.getClassName() ); // for current parent-id
      ret.append( member.getTableName() ); // for current parent-id
      ret.append( "_id\"" );
    }
    ret.append("/>\n" );
    ret.append( "    </" );
    ret.append( what );
    ret.append( ">\n" );
  }

  /**
  Tries to guess whether this class has one-to-many or many-to-many relationship with given collection member.
  If member has natural primary key, we assume it's something we can use for search, then it's many-to-many.
  */
  protected boolean oneToMany( DBObject member ) {
    // member has no natural primary key -> one to many
    boolean ret = (member.getKeyFieldName() == null);
    // Without nautral key, we look up deeper in member, looking up it's members.
    // If member contains any (only one?) DBObject members with natural primary key (many-to-one), it's still many-to-many.
    // this may produce undesirable many-to-may relations, thus - protected
    // TODO: collection of DBObjects should imply many- or one-to-many?
    if ( ret ) {
      Field [] fields = getClass().getFields();
      for ( int i = 0; i < fields.length; i++ ) {
        int mods = fields[i].getModifiers();
        if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
          if ( DBObject.class.isAssignableFrom( fields[i].getType() )) {
            try {
              DBObject instance = (DBObject) fields[i].getType().newInstance();
              // when looking up for any key:
              //ret &= ( instance.getKeyFieldName() == null );
              //break;
              // when looking up for only one key:
              if ( instance.getKeyFieldName() == null ) {
                ret = true;
                break;
              } else ret = false;
            } catch ( Exception e ) {
              // should never happen
              Logger.logError(e);
              break;
            }
          }
        }
      }
    }
    return ret;
  }

  /**
  Check if string getter and setter exist for given property
  */
  public boolean checkMethods( String property ) {
    boolean isMethod = false;
    String name = property.substring(0,1).toUpperCase()+property.substring(1);
    try {
      Class[] classes = { String.class };
      Method setter = getClass().getMethod( "set" + name, classes );
      Method getter = getClass().getMethod( "get" + name, new Class[]{} );
      // so we have a public var, and string setter/getter
      isMethod = true;
    } catch (NoSuchMethodException e3) {}
    return isMethod;
  }
  protected void processCache(StringBuilder sb) {
    if ( L2Caching ) {
      try {
        Field cacheField = getClass().getField("_cache");
        boolean cache = (Boolean) cacheField.get( this );
        if ( cache ) {
          sb.append( "    <cache usage=\"read-only\"/>\n");
        }
      } catch ( Exception e ) {
        // this should never happen
        Logger.logError(e);
      }
    }
  }
  /**
  Internal: appends appropriate indexing expression to the StringBuffer. Uses _index and _indexUnique static arrays of field names.
  @return true for existing natural id
  */
  protected boolean processIndex( Field field, String fieldName, DBObject obj, StringBuilder sb ) {
    boolean ret = false;
    try {
      Field indexField = getClass().getField(fieldName);
      String[] indexes = (String[])indexField.get( obj );
      boolean unique = fieldName.equals( "_indexUnique" );
      for ( int j = 0; j < indexes.length; j++ ) {
        if ( field.getName().equals( indexes[j] )) {
          Logger.logDebug("Indexed field: "+getClassName()+"."+field.getName()+" unique="+unique);
          appendIndex( indexes[j], unique, sb );
          ret = unique;
          break;
        }
      }
    } catch ( Exception e ) {
      // this should never happen
      Logger.logError(e);
    }
    return ret;
  }
  protected void appendIndex( String index, boolean unique, StringBuilder ret ) {
    // indexed field
    ret.append( " index=\"" );
    //ret.append( getClassName() );
    ret.append( getTableName() );
    ret.append( "_" );
    ret.append( index.toLowerCase() );
    ret.append( "\"" );
    if ( unique ) ret.append( " unique=\"true\"" );
  }

  /**
  Add package to package search path, in order to make newInstance() work.
  */
  public static void addPackage( String pkgName ) {
    if ( packages == null ) packages = new LinkedList();
    if ( ! (packages.contains(pkgName))) packages.add( pkgName );
  }
  /**
  Get all known persistence packages
  */
  public static List getPackages() {
    return packages;
  }
  /**
  Get string containing known persistence packages
  */
  public static String packageString() {
    StringBuilder ret = new StringBuilder();
    for ( String pkg: packages ) {
      ret.append(pkg);
      ret.append(" ");
    }
    return ret.substring(0,ret.length()-1);
  }
  /**
  Set known packages, package list is reinitalized.
  @param pkgs package names separated by space
  */
  public static void setPackages(String pkgs) {
    packages = new LinkedList();
    parseAndAdd( pkgs );
  }
  /**
  Add known packages to package list, list created if needed.
  @param pkgs package names separated by space
  */
  public static void addPackages(String pkgs) {
    parseAndAdd( pkgs );
  }
  private static void parseAndAdd( String pkgs ) {
    StringTokenizer st = new StringTokenizer( pkgs );
    while ( st.hasMoreTokens() ) {
      addPackage( st.nextToken() );
    }
    Logger.logDebug("Packages set:"+pkgs+", "+packages.size());
  }

  /**
  Creates new instance of <b>className</b>. May return null if instantiation fails, logs error if so.
  WARNING: this method is not synchronized - make sure to add all packages earlier.
  CHECKME: all classes are cached to a Hashtable, this may be incompatible with class unloading performed by garbage collector - use weak/soft references?
  @param className Class to instantiate. May be full class name with package name, or without package name.
  If package is not specified, packages are consulted in given order.
  @see #addPackage
  */
  public static DBObject newInstance( String className ) {
    Object obj=null;
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    Throwable err = null;

    if ( className.indexOf(".") > 0 ) {
      // full class name containing package
      try {
        obj = Class.forName( className ).newInstance();
      } catch ( Throwable t ) {
        err = t;
      }
    } else {
      // guess package name
      if ( packages.size() == 0 ) {
        // try instantiate without package name, maybe we do have such a class
        try {
          obj = Class.forName( className ).newInstance();
        } catch ( Throwable t ) {
          throw new RuntimeException( "Undefined search packages", t );
        }
      }
      // CHECKME: remember known and unknown classes?
      Object cls = knownClasses.get( className );
      if ( cls != null ) {
        // we instantiated this class earlier
        if ( cls instanceof Throwable ) {
          // we tried but failed to instantiate this class
          Logger.logWarning( "Trying to instantiate uknown class "+className+" - "+cls );
        } else {
          try {
            obj = ((Class)cls).newInstance();
          } catch ( Throwable t ) {
            err = t;
          }
        }
      } else {
        for ( Object pkg: packages ) {
          try {
            obj = Class.forName( pkg+"."+className ).newInstance();
            DBObject tmp = (DBObject) obj; // to force classcastexception
            err = null;
            knownClasses.put( className, obj.getClass() );
            break; // first class found is valid one
          } catch ( Throwable t ) {
            Logger.logWarning( "Class "+pkg+"."+className+" not found");
            err = t;
          }
        }
      }
    }

    if ( err != null ) {
      knownClasses.put( className, err );
      Logger.logError( "Can't instantiate "+className, err );
    }
    return (DBObject) obj;
  }

  /**
  Returns class name without package name.
  */
  public String getClassName() {
    if ( myClassName == null ) {
      /*
      myClassName = getClass().getName();
      Package pkg = getClass().getPackage();
      if ( pkg != null ) {
        myClassName = myClassName.substring( pkg.getName().length()+1 );
    }
      */
      myClassName = Util.getClassName( this );
    }
    return myClassName;
  }

  /**
  Returns database table name; this implementation returns getClassName(),
  override to persist in different tables.
  */
  public String getTableName() {
    return getClassName().toLowerCase(); // some databases are case-sensitive
    //return getClassName();
  }

  /**
  Returns the package name to which this class belongs.
  */
  public String getPackageName() {
    return Util.getPackageName( this );
  }

  /** Returns string representation of objects, used by fromText() */
  public static String toText( Object[] obj ) throws IllegalAccessException {
    StringBuffer ret = new StringBuffer();
    for ( int i = 0; i < obj.length; i++ ) {
      ret.append(((DBObject) obj[i]).toText());
    }
    return ret.toString();
  }
  /** Returns string representation of objects, used by fromText() */
  public String toText() throws IllegalAccessException {
    StringBuffer ret = new StringBuffer();
    // Store all public fields?
    //Field [] fields = obj.getClass().getDeclaredFields();
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
        ret.append( fieldToText( fields[i] ) );
      }
    }
    return ret.toString();
  }

  public String toCSVHeader( String separator ) throws IllegalAccessException {
    if ( separator == null ) separator = ",";
    StringBuilder ret = new StringBuilder();
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
        ret.append("\"");
        ret.append( fields[i].getName() );
        ret.append("\"");
        ret.append( separator );
      }
    }
    if ( ret.length() > 0 ) ret.deleteCharAt(ret.length()-1);
    return ret.toString();
  }

  public String toCSV( String separator ) throws IllegalAccessException {
    if ( separator == null ) separator = ",";
    StringBuilder ret = new StringBuilder();
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
        Object val = fields[i].get( this );
        ret.append("\"");
        ret.append( val );
        ret.append("\"");
        ret.append( separator );
      }
    }
    if ( ret.length() > 0 ) ret.deleteCharAt(ret.length()-1);
    return ret.toString();
  }

  /**
  Returns a member fields value represented as string.
  @param field Name of the member field.
  */
  public String fieldToText( Field field ) throws IllegalAccessException {
    String ret = "";
    long id = getId();
    String c = getClassName();
    if ( field.getType().isArray() ) {
      Object val = field.get( this );
      // we ignore empty arrays - CHECKME
      if ( val != null && Array.getLength( val ) > 0 ) {
        ret = c + " " + id + " " + field.getName() + " " + arrayToString(val) + ";\n";
      }
    } else if ( DBObject.class.isAssignableFrom( field.getType() ) ) {
      // CHECKME - DBObjects: show only class and id?
      DBObject val = (DBObject) field.get( this );
      if ( val != null ) {
        // obsolete: this dumps all member object's properties
        //ret = c + " " + id + " " + field.getName() + " " + val + ";\n";
        ret = c + " " + id + " " + field.getName() + " ( " + val.getClassName() + " " + val.db_id + " );\n";
      }
    } else if ( field.getType().equals( Vector.class )) {
      // Vector is much like an array
      Object[] val = ((Vector) field.get( this )).toArray();
      if ( val != null ) {
        ret = c + " " + id + " " + field.getName() + " " + arrayToString(val) + ";\n";
      }
    } else if ( Set.class.isAssignableFrom( field.getType() ) ) {
      // Set is much like a Vector
      Object[] val = ((Set) field.get( this )).toArray();
      if ( val != null ) {
        ret = c + " " + id + " " + field.getName() + " " + arrayToString(val) + ";\n";
      }
    } else if ( Date.class.isAssignableFrom( field.getType() )) {
      if ( field.get(this) != null ) {
        ret = c + " " + id + " " + field.getName() + " " + dateFormat.format( (Date) field.get(this) ) + ";\n";
      }
    } else if ( Map.class.isAssignableFrom( field.getType() ) ) {
      Map map = (Map) field.get( this );
      if ( map != null && map.size() > 0 ) {
        Object[] entries = map.entrySet().toArray();
        String[] val = new String[ entries.length * 2 ];

        for ( int i = 0; i < entries.length; i++ ) {
          val[ 2 * i ] = ( (Map.Entry) entries[ i ] ).getKey().toString();
          val[ 2 * i + 1 ] = ( (Map.Entry) entries[ i ] ).getValue().toString();
        }

        if ( val != null ) {
          ret = c + " " + id + " " + field.getName() + " " + arrayToString( val ) + ";\n";
        }
      } else {
        ret = "";
      }
    } else {
      Object val = field.get( this );
      if ( val != null && ! ( field.getType().equals( String.class ) && ((String)val).length() == 0 )) {
        ret = c + " " + id + " " + field.getName() + " " + val + ";\n";
      }
    }
    return ret;
  }

  /**
  As toText(), but does not return fields beginning with <b>filter</b>.
  Used for sending events over the network.
  */
  public String toText( String filter ) throws IllegalAccessException {
    StringBuffer ret = new StringBuffer();
    String fieldName;

    // URL is special.  It must be added last for the protocol.
    Field url = null;

    // Store all public fields?
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      fieldName = fields[i].getName();
      if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) &&
              ! fieldName.equals( "db_id" ) &&
              ! fieldName.substring( 0, filter.length() ).equals( filter )
         ) {
        // url is added last
        if ( ! fieldName.equals( "url" ) ) {
          ret.append( fieldToText( fields[i] ) );
        } else {
          url = fields[ i ];
        }
      }
    }

    // Now add url if it exists
    if ( url != null ) {
      ret.append( fieldToText( url ) );
    }

    return ret.toString();
  }

  /** Returns array of objects defined by string in toText() format */
  public static DBObject[] fromText( String definition )
  throws NoSuchMethodException,
        NoSuchFieldException,
        IllegalAccessException,
        java.lang.InstantiationException,
        ClassNotFoundException,
        InvocationTargetException,
    Exception {
    StringTokenizer lines = new StringTokenizer ( definition, ";"+System.getProperty( "line.separator" ) );
    HashMap ret = new HashMap();
    //TreeMap ret = new TreeMap();
    DBObject obj = null;
    int lineNumber = 0;
    while ( lines.hasMoreTokens() ) {
      String line = lines.nextToken();
      //Logger.logDebug( line );
      lineNumber++;
      StringTokenizer st = new StringTokenizer( line );

      String c = null;
      String cid = null;
      try {
        c = st.nextToken();
        cid = st.nextToken();
        long id = new Long(cid).longValue();
        String var = st.nextToken();
        String val = line.substring( c.length()+cid.length()+var.length()+3 );
        //Logger.logDebug( "VRObject.fromText(): "+c+"["+id+"]."+var+"="+val );
        if ( ret.containsKey( c+" "+new Long(id) ) ) {
          if ( obj.db_id != id ) {
            Logger.logError("Wrong id: db "+obj.db_id+" "+id);
          }
          obj = (DBObject) ret.get( c+" "+new Long(id) ); // CHECKME!!!!!
        } else {
          obj = (DBObject) newInstance( c );
          obj.db_id = id;
          ret.put( c + " " + new Long(id), obj );
          //Logger.logDebug("New "+c+" "+id);
        }
        try {
          obj.setField( var, val );
          // check for DBObject members and update references if needed
          Field field = obj.getClass().getField(var);
          if ( DBObject.class.isAssignableFrom( field.getType() ) ) {
            // member is DBObject instance
            DBObject member = (DBObject) field.get( obj );
            //Logger.logDebug( c+" "+id+" "+var+" = "+member );
            if ( member != null ) {
              if ( ret.containsKey( member.getClassName()+" "+member.db_id ) ) {
                // member object already processed; update reference
                member = (DBObject) ret.get(member.getClassName()+" "+member.db_id);
                //Logger.logDebug( "Member: "+member );
                field.set( obj, member );
              } else {
                // member not processed yet; store new object to map
                ret.put( member.getClassName() + " " + new Long(member.db_id), member );
              }
            }
          }
        } catch ( Throwable t ) {
          // ignore field errors
          Logger.logError("Error in setField "+c+"["+id+"]."+var+"="+val, t);
        }
      } catch ( Exception e ) {
        Logger.logError( e.toString() + " error occurred while parsing DBObject of class " + ( c != null ? c : "unknown" ) +
                         " id " + ( cid != null ? cid : "unknown" ) + " line number " + lineNumber + " :\n" + line );
        throw e;
      }
    }
    return (DBObject[]) ret.values().toArray(new DBObject[0]);
  }

  /** setFields() on all variables specified in <names>
   ** parse args and set each
   */
  public void setFields( String[] names, String args )
  throws Exception {
    StringTokenizer st = new StringTokenizer( args, " ", true);
    pos = 0;
    String[] values = new String [ names.length ];
    try {
      for ( int i = 0; i < names.length-1; i++ ) {
        setField( names[i], nextToken(st) );
      }
      setField( names[ names.length ], args.substring( pos ) );
    } catch ( Exception e ) {
      Logger.logError(getID()+": error in setField", e);
      throw new Exception( e.getMessage() );
    }
  }

  /**
  Takes field values from passed object, by field name. Non-exisisting
  fields ignored.
  */
  public void setFields( DBObject o ) {
    Field[] fields = o.getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      try {
        int mods = fields[i].getModifiers();
        // Important not to set final or static fields as they contain no information
        // particular to the object.  They will be created automatically when the
        // class is instantiated either way.  Also certain static fields are complex
        // objects not yet serializable by the server.
        if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
          setField( fields[i].getName(), o.getField(fields[i].getName()).toString() );
        }
      } catch ( Exception e ) {}
    }
  }


  /**
  Sets the property <b>name</b> to <b>value</b>.
  @param name Property name.
  @param value Property value.
  */
  public void setField( String name, String value ) throws Exception {
    //setField( name, value, null );
    //}

    /**
    Equals to <b>obj.name = value</b>.
    If this raises exception, tries
    <b>obj</b>.set_<b>name</b>( Request <b>r</b>, String <b>value</b> )
    */
    //public void setField( String name, String value, Request r ) throws Exception {
    // System.out.println( "VRObject.setField(): "+obj.getClass().getName()+" "+name+" "+value );
    Field field = null;
    DBObject obj = this;
    boolean isMethod = false;

    // First attempt to find a method
    // The request itself may be null, for example, when loading object
    // from db.
    /*
    if ( r != null ) {
      try {
        Class[] classes = { Request.class, String.class };
        Method method = obj.getClass().getMethod( "set_" + name, classes );
        Object[] o = { r, value };
        method.invoke( obj, o );
        isMethod = true;
      } catch (NoSuchMethodException e3) {
      } catch (InvocationTargetException e) {
        Logger.logError( "Unable to invoke method " + name + " in class " + obj.getClass().getName(), e.getTargetException() );
        throw new Exception( "Unable to invoke method " + name + " in class " + obj.getClass().getName() );
      } catch (IllegalAccessException e2) {
        Logger.logError( "Unable to access method " + name + " in class " + obj.getClass().getName(), e2 );
        throw new Exception( "Unable to access field " + name + " in class " + obj.getClass().getName() );
      }
  }
    */

    // get public field
    if ( ! isMethod ) {
      try {
        field = obj.getClass().getField( name );
      } catch (NoSuchFieldException e2) {
        throw new Exception( "No field " + name + " in class " + obj.getClass().getName() + " " + e2.toString() );
      }
      catch (Exception e) {
        Logger.logError( getID() + ": error attempting to access field.", e );
      }
    }

    if ( field != null ) {
      int modifiers = field.getModifiers();
      Class type = field.getType();
      try {
        // If the sent value is null, then it is a request for the
        // fields value.
        if ( value == null ) {
        }
        else if ( type.isPrimitive() ) {
          // Primitive
          String typeName = field.getType().getName();
          if ( typeName.equals( "long" ) ) {
            field.setLong( obj, Long.valueOf( value ).longValue() );
          } else if ( typeName.equals( "int" ) ) {
            field.setInt( obj, Integer.valueOf( value ).intValue() );
          } else if ( typeName.equals( "float" ) ) {
            field.setFloat( obj, Float.valueOf( value ).floatValue() );
          } else if ( typeName.equals( "double" ) ) {
            field.setDouble( obj, Double.valueOf( value ).doubleValue() );
          } else if ( typeName.equals( "short" ) ) {
            field.setShort( obj, Short.valueOf( value ).shortValue() );
          } else if ( typeName.equals( "char" ) ) {
            field.setChar( obj, value.charAt(0) );
          } else if ( typeName.equals( "byte" ) ) {
            field.setByte( obj, new Integer(value).byteValue() );
          } else if ( typeName.equals( "boolean" ) ) {
            field.setBoolean( obj, new Boolean(value).booleanValue() );
          } else {
            Logger.logError( "Field has uknown typeName:"+typeName+" "+name+" = "+value );
          }
        } else if ( value.equals( "null" ) ) {
          field.set( obj, null );
          //} else if ( type.isInstance( this )) {
        }
        else if ( DBObject.class.isAssignableFrom( type ) ) {
          // VRObject in toString() format
          DBObject arg = (DBObject) type.newInstance();
          // TODO: Should this be only class/id or fromString()?
          arg.fromString( value );
          field.set( obj, arg );
        } else if ( type.isArray() ) {
          // How to store arrays?
          try {
            field.set( obj, stringToArray( type, value ) );
          } catch ( Exception e ) {
            Logger.logError( "Field "+type.getName()+" "+name+" = "+value, e );
          }
        } else if ( type.equals( Vector.class ) ) {
          // Vector is a wrapped-around array
          Vector tmp = new Vector();
          Object a = stringToArray( Object.class, value ); //?
          for ( int i = 0; i < Array.getLength(a); i++ ) {
            tmp.add( Array.get(a, i) );
          }
          field.set( obj, tmp );
        } else if ( type.equals( Set.class )) {
          // Set can be represented by an array
          Set tmp = (Set) type.newInstance();
          Object a = stringToArray( Object.class, value ); // wrong! change class
          for ( int i = 0; i < Array.getLength(a); i++ ) {
            tmp.add( Array.get(a, i) );
          }
          field.set( obj, tmp );
        } else if ( Map.class.isAssignableFrom( type ) ) {
          // Set can be represented by an array
          Map tmp = (Map) type.newInstance();
          Object a = stringToArray( Object.class, value ); // wrong! change class
          for ( int i = 0; i < Array.getLength( a ) / 2; i++ ) {
            tmp.put( Array.get( a, 2 * i ), Array.get( a, 2 * i + 1 ) );
          }
          field.set( obj, tmp );
        } else if ( Date.class.isAssignableFrom( type ) ) {
          Date tmp = dateFormat.parse( value );
          field.set( obj, tmp );
        } else {
          try {
            // try to get a constructor with String argument
            Class[] params = { String.class };
            Constructor constructor=type.getConstructor( params );
            Object[] args = { value };
            field.set( obj, constructor.newInstance( args ));
          } catch ( NoSuchMethodException nomE ) {
            // last try
            try {
              field.set( obj, value );
            } catch ( Exception e ) {
              Logger.logError( "Field "+field.getName()+" ("+type.getName()+") = "+value+" ("+value.getClass()+")", e );
            }
          }
        }
      } catch (Throwable e) {
        Logger.logError( "Field "+type.getName()+" "+name+" = "+value, e );
      }
    }
  }

  /**
  Returns names of all public member varaibles. Use getField( String ) to retreive value.
  @see #getField
  */
  public String[] getFields() {
    Vector ret = new Vector();
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      try {
        ret.add(fields[i].getName());
      } catch (Exception e) {
        // this cannot happen
      }
    }
    return (String[]) ret.toArray( new String[ret.size()] );
  }

  /**
  Returns true if this object contains public field with this name
  */
  public boolean hasField(String name) {
    boolean ret = false;
    Field[] fields = getClass().getFields();
    for ( int i = 0; !ret && i < fields.length; i++ ) {
      ret = name.equals( fields[i].getName() );
    }
    return ret;
  }
  /**
  Returns true if this object contains public method with this name
  */
  public boolean hasMethod( String name ) {
    boolean ret = false;
    Method[] methods = getClass().getMethods();
    for ( int i = 0; !ret && i < methods.length; i++ ) {
      ret = name.equals( methods[i].getName() );
    }
    return ret;
  }
  /**
  Returns true if there's either field or set_ method for given property.
  */
  public boolean canWrite( String name ) {
    return hasField( name ) || hasMethod( "set_"+name );
  }
  /**
  Returns true if there's either field or get_ method for given property.
  @param name Property name.
  */
  public boolean canRead( String name ) {
    return hasField( name ) || hasMethod( "get_"+name );
  }
  /**
  Returns a public field's class.
  */
  public Class getFieldClass( String name ) throws NoSuchFieldException {
    Field field = getClass().getField(name);
    return field.getType();
  }

  /**
  Returns a property value:
  returns a field value; if there's no member variable with given name, tries to execute "get_"+name method.
  @param name Property name.
  @return Property value
  @throws Exception if neither field nor method were found
  */
  public Object getField( String name ) throws Exception {
    Field field = null;
    Object ret = null;
    try {
      // get public field
      field = getClass().getField(name);
      ret = field.get( this );
    } catch ( NoSuchFieldException e ) {
      // not a field, try the method
      try {
        //Class[] classes = { r.getClass(), new String().getClass() };
        Class[] classes = {};
        Method method = getClass().getMethod("get_" + name, classes );
        Object[] o = { null };
        ret = method.invoke( this, o );
      } catch (InvocationTargetException e3) {
        Logger.logError( getID()+": error executing method get_"+name, e3.getTargetException() );
        throw new Exception( "Unable to access field "+name+" in class "+getClass().getName()+" - "+e3.getTargetException().toString());
      }
      catch (Exception e2) {
        throw new Exception( "No field "+name+" in class "+getClass().getName()+" "+e2.toString());
      }
    }
    catch ( Throwable t ) {
      throw new Exception( "No field "+name+" in class "+getClass().getName()+" "+t.toString());
    }
    return ret;
  }
  /**
  DBObject.equals( DBObject ) if class and db_id are the same.
  */
  public boolean equals( DBObject obj ) {
    if ( obj == null ) {
      return false;
    } else {
      return (this.db_id == obj.db_id && this.getClass().getName().equals(obj.getClass().getName()));
    }
  }
  public int hashCode() {
    int hash = 7;
    //CHECKME: this hashcode may not work for long id!!!
    hash = 31 * hash + (int)db_id;
    hash = 31 * hash + getClassName().hashCode();
    return hash;
  }

  /** Returns objects unique id */
  public long getId() {
    return db_id;
  }
  /** Returns objects unique id */
  public ID getID() {
    return new ID( getClass(), db_id );
  }

  /**
  Converts array <b>val</b> to String.
  */
  //public static String arrayToString( Object[] val ) {
  public static String arrayToString( Object array ) {
    if ( ! array.getClass().isArray() ) {
      throw new IllegalArgumentException( "Argument is not an array!" );
    }
    StringBuffer ret=new StringBuffer();
    String name = array.getClass().getName();
    int len = Array.getLength( array );
    // we ignore empty arrays - CHECKME
    if ( len > 0 ) {

      // array without primitives
      if ( name.length() > 2 ) {
        name = name.substring(2,name.length()-1);

        // array with primitives
      } else {
        Object obj = Array.get( array, 0 );
        name = (String) primitiveMap.get( obj.getClass() );
      }

      try {
        ret.append( "[" );
        ret.append( name );

        for (int i=0; i<len; i++ ) {
          ret.append( "(" + Array.get(array, i).toString() + ")" );
        }

        ret.append( "]" );
      } catch ( NullPointerException e ) {
        ret.append( "[()]" );
      }
    } else {
      ret.append( "[]" );
    }

    return ret.toString();
  }

  /**
  Converts String in arrayToString() format back to array.
  */
  public static Object stringToArray( Class cl, String val ) {
    Object ret=null;
    StringTokenizer st=new StringTokenizer(val,"[()]" );
    String token;

    try {
      Vector vec = new Vector();

      // If this is an empty array then returns an empty array.
      if ( ! st.hasMoreTokens() ) {
        //return new Object[ 0 ];
        return Array.newInstance( cl, 0 );
      }

      String className=st.nextToken();
      Class c = null;
      try {
        Object primitive = (Object) primitives.get( className );

        // array with non-primitive values
        if ( primitive == null ) {
          c = Class.forName( className );
        } else {
          c = primitive.getClass();
        }

        Class[] params = { String.class };
        Constructor constructor=c.getConstructor( params );
        while (st.hasMoreElements()) {
          token = st.nextToken();
          Object[] args = { token };
          vec.add(constructor.newInstance( args ));
        }

        if ( primitive != null ) {
          ret=Array.newInstance( (Class) primitive.getClass().getField( "TYPE" ).get( null ), vec.size() );
        } else {
          ret=Array.newInstance( c, vec.size() );
        }

        //generates exception for null arrays:
        for ( int i = 0; i < vec.size(); i++ ) {
          Array.set( ret, i, vec.elementAt( i ) );
        }
      } catch (Exception e) {
        Logger.logError("Error constructing array", e);
      }
    } catch ( NoSuchElementException e ) {
      // no class name?
      Logger.logError( "Error processing " + val, e );
    }

    return ret;
  }

  private String nextToken( StringTokenizer st ) {
    String ret = null;
    String token = st.nextToken();
    StringBuffer sb = new StringBuffer();
    while ( token.equals( " " ) ) {
      token = st.nextToken();
      pos++;
    }
    pos += token.length();
    if ( token.charAt(0) == '"' ) {
      while ( true ) {
        sb.append( token );
        if ( sb.length() > 1
                && sb.charAt( sb.length()-1 ) == '"'
                && sb.charAt( sb.length() - 2 ) != '\\'
           ) {
          break;
        }
        token = st.nextToken();
        pos+=token.length();
      }
      ret = sb.substring( 1, sb.length()-1 );
    } else {
      sb.append( token );
      ret = sb.toString();
    }
    //Logger.logDebug( "Token: '"+ret+"' pos = "+pos );
    return ret;
  }

  /**
  Uses toString() formated String to set this object's properties.
  */
  public int fromString( String s ) throws Exception {
    //Logger.logDebug( "Parsing '"+s+"'" );
    // CHECKME: new StringTokenizer is created for each recursive call
    // TODO: avoid creating new stringTokenizer in recursive calls
    StringTokenizer st = new StringTokenizer( s, " ", true );
    pos = 0;
    String tmp;
    if ( ! nextToken(st).equals( "(" ) ) {
      // wrong class
      throw new Exception( "Invalid syntax: '"+s+"'" );
    }
    tmp = nextToken(st);
    if ( ! tmp.equals( getClass().getName() ) && ! tmp.equals( getClassName())) {
      // wrong class
      throw new Exception( "Invalid class: '"+tmp+"'");
    }
    db_id = new Long( nextToken(st) ).longValue();

    while ( st.hasMoreTokens() ) {
      String var = nextToken(st);
      if ( var.equals( ")" ) ) {
        //Logger.logDebug( "Parsed "+tmp+" "+db_id+" pos="+pos );
        break;
      }
      //Logger.logDebug("var="+var+" pos="+pos);
      Field f = getClass().getField( var );
      String val = nextToken( st );
      if ( val.equals( "(" )) {
        int start = pos;
        String className = nextToken( st );
        //Logger.logDebug("new "+className+" pos="+pos);
        DBObject obj = newInstance( className );
        start += obj.fromString( "( " + s.substring( start ));
        start --;
        f.set( this, obj );

        while( pos < start - 2 && st.hasMoreTokens()) {
          tmp = nextToken( st );
          //Logger.logDebug( "Skip "+tmp+" pos="+pos+" start="+start );
        }
      } else {
        //Logger.logDebug("val="+val+" pos="+pos);
        if ( val.equals( "null" ) ) {
          val = null;
        }
        //f.set( this, val );
        setField( var, val );
      }
    }
    return pos;
  }

  /**
  Returns the string representation of this object.
  Original object can be retrieved from this string using fromString() method.
  */
  public String toString() {
    String ret = "( " + getClass().getName() + " " + db_id;
    Field[] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      int mods = fields[i].getModifiers();
      if ( Modifier.isFinal( mods ) || Modifier.isStatic( mods ) || fields[i].getName().equals( "db_id" ) ) {
        continue;
      }
      ret += " " + fields[i].getName();
      try {
        if ( fields[i].getType().equals( String.class ) ) {
          String val = (String) fields[i].get(this);
          if ( val == null ) {
            ret += " null";
          } else {
            ret += " \"" + fields[i].get(this) + "\"";
          }
        } else if ( fields[i].getType().equals( Vector.class )) {
          // Vector is much like an array
          Vector vec = (Vector) fields[i].get( this );
          if ( vec != null && vec.size() > 0 ) {
            Class c = vec.elementAt(0).getClass();
            Object tmp=Array.newInstance( c, vec.size() );
            tmp = vec.toArray( (Object[]) tmp );
            ret += " " + arrayToString((Object[]) tmp);
          } else {
            ret += " null ";
          }
        } else if ( Set.class.isAssignableFrom( fields[i].getType() ) ) {
          Set set = (Set) fields[i].get( this );
          if ( set != null ) {
            Object[] val = set.toArray();
            if ( val != null ) {
              ret += " " + arrayToString(val);
            }
          } else {
            ret += " null ";
          }
        } else if ( Map.class.isAssignableFrom( fields[ i ].getType() ) ) {
          Map map = (Map) fields[i].get( this );
          if ( map != null ) {
            Object[] val = map.entrySet().toArray();
            if ( val != null ) {
              ret += " " + arrayToString( val );
            }
          } else {
            ret += " null ";
          }
        } else if (DBObject.class.isAssignableFrom( fields[i].getType() )) {
          // Need to address DBObject members in a different manner in order to prevent recursion
          // CHECKME
          if ( this.equals( fields[i].get(this) )) {
            Logger.logWarning("Object has itself for member! "+getID()+"."+fields[i].getName());
            //} else {
            //Logger.logError("Class "+getClassName()+" member: "+fields[i].getType().getName(), (new Exception("Something is wrong!")).fillInStackTrace());
          }
          if ( fields[i].get(this) == null ) {
            //ret += " ( null )";
            ret += " null";
          } else {
            ret += " ( " + ((DBObject)fields[i].get(this)).getID()+" )";
          }
        } else if ( Date.class.isAssignableFrom( fields[i].getType() )) {
          // TODO: null date!
          if ( fields[i].get(this) != null ) {
            ret += " "+dateFormat.format( (Date) fields[i].get(this) );
          } else {
            ret += " null";
          }
        } else {
          ret += " " + fields[i].get( this );
        }
      } catch ( IllegalAccessException e ) {
        Logger.logError(e);
      }
    }
    ret += " )";
    return ret;
  }

  /**
  This returns only public members
  */
  public String[] getPublicFields() {
    Vector ret = new Vector();
    Field [] fields = getClass().getFields();
    for ( int i = 0; i < fields.length; i++ ) {
      try {
        int mods = fields[i].getModifiers();
        //if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) && ! fields[i].getName().equals( "db_id" ) ) {
        if ( ! Modifier.isFinal( mods ) && ! Modifier.isStatic( mods ) ) {
          ret.add(fields[i].getName());
        }
      } catch (Exception e) {
        // this cannot happen
        Logger.logError(e);
      }
    }
    return (String[]) ret.toArray( new String[ret.size()] );
  }

  /**
  Returns the database field name for given field, that is, fieldPrefix+name;
  @param name Member field name.
  */
  public String getHibernateName( String name ) {
    return fieldPrefix+name;
  }
  private HashMap fieldMap;
  private synchronized void checkFieldMap() {
    if ( fieldMap == null ) {
      fieldMap = new HashMap();
      String[] fields = getPublicFields();
      for ( int i = 0; i < fields.length; i++ ) {
        String key = getHibernateName( fields[i] ).toLowerCase();
        fieldMap.put( key, fields[i] );
        //Logger.logDebug( key+" -> "+fields[i] );
      }
    }
  }
  /**
  Returns the value of a database field.
  @param hibernateName Name of field in the database.
  */
  public Object getHibernateFieldValue( String hibernateName ) {
    Field field = getHibernateField( hibernateName );
    try {
      return field.get( this );
    } catch ( Exception e ) {
      Logger.logError( "Can't get field value: "+hibernateName, e );
    }
    return null;
  }
  /**
  Returns the Field for a given database field name.
  @param hibernateName Name of field in the database.
  */
  public Field getHibernateField( String hibernateName ) {
    Field ret = null;
    checkFieldMap();
    String name = (String) fieldMap.get( hibernateName.toLowerCase() );
    try {
      ret = getClass().getField( name );
    } catch ( Exception e ) {
      Logger.logError( "Can't get field "+hibernateName, e );
    }
    return ret;
  }
  /**
  Sets the value of a member field by its database name.
  @param hibernateName Field's name in the database
  @param value The value.
  */
  public void setHibernateField( String hibernateName, Object value ) {
    boolean ok = false;
    checkFieldMap();
    String name = (String) fieldMap.get( hibernateName.toLowerCase() );
    Field field = null;
    //FIXME: this should throw an exception!!!
    try {
      //setField( name, ""+value );  // FIXME!!!
      field = getClass().getField(name);
      if ( DBObject.class.isAssignableFrom( field.getType() ) ) {
        Logger.logDebug("Instance of DBObject!!! Checking referential "+field.getType().getName() );
        DBObject obj = (DBObject) field.getType().newInstance();
        //String[] fields = obj.getPublicFields();
        String[] fields = (String[]) obj.getField( "_indexUnique" );
        for ( int i = 0; !ok && i < fields.length; i++ ) {
          if( obj.getClass().getField( fields[i] ).getType().equals( value.getClass() ) ) {
            Logger.logDebug( "Search "+obj.getClassName()+"."+fields[i]+" == "+value );
            DBObject ref = (DBObject) HibernateDB.getInstance().get( obj.getClassName(), fields[i], value );
            if ( ref != null ) {
              field.set( this, ref );
              ok = true;
            }
          }
        }
      } else if ( value != null && value.getClass() == String.class && field.getType() != String.class ) {
        setField( name, (String) value );
        ok = true;
      } else {
        field.set( this, value );
        ok = true;
      }
    } catch ( Exception e ) {
      if ( field != null && value != null ) {
        Logger.logError( "Can't set field "+hibernateName+" ("+field.getType().getName()+")"+" = "+value+" ("+value.getClass().getName()+")", e );
      } else {
        Logger.logError( "Can't set field "+hibernateName+" = "+value, e );
      }
    }
    if ( !ok ) throw new ReferentialIntegrityException( "Invalid referential value for "+hibernateName+"='"+value+"'" );
  }
  /**
  This method is supposed to return primary/unique key (other than db_id) that
  may be used to display and identify instances of specific DBObject subclass,
  or null or no such field exists.
  This implementation returns value of first public field specified in
  public static final String[] _uniqueIndex array.
  */
  public Object getKey() {
    Object ret = null;
    try {
      String fieldName = getKeyFieldName();
      if ( fieldName != null ) ret = getClass().getField(fieldName).get( this );
    } catch ( Exception e ) {
      Logger.logError( e );
    }
    return ret;
  }
  /**
  This method is supposed to return primary/unique key (other than db_id) that
  may be used to display and identify instances of specific DBObject subclass,
  or null or no such field exists.
  This implementation returns name of first public field specified in
  public static final String[] _uniqueIndex array.
  */
  public String getKeyFieldName() {
    String ret = null;
    try {
      Field field = getClass().getField("_indexUnique");
      String[] fields = (String[]) field.get( this );
      if ( fields.length > 0 ) ret = fields[0];
    } catch ( Exception e ) {
      Logger.logError( "Error fetching unique index for "+getClassName(), e );
    }
    return ret;
  }

  /**
  Returns key (indexed) fields names, specifiedn in _index array.
  */
  public String[] getKeyNames() {
    Vector ret = new Vector();
    try {
      Field field = getClass().getField("_index");
      String[] fields = (String[]) field.get( this );
      for ( int i = 0; i < fields.length; i++ ) {
        ret.add( fields[i] );
      }
    } catch ( Exception e ) {
      Logger.logError( "Error fetching unique index for "+getClassName(), e );
    }
    return (String[]) ret.toArray( new String[ret.size()] );
  }
  /**
  Clones the object: returns a brand new instance filled with same properties as this one.
  */
  public DBObject clone() {
    try {
      return (DBObject) super.clone();
    } catch ( CloneNotSupportedException e ) {
      Logger.logError(e);
      return null;
    }
  }

  /**
  Returns all known (registered with addPackage()) class names.
  */
  public static String[] listClasses() {
    LinkedList classes = new LinkedList();
    for ( String pkg: (List<String>) getPackages() ) {
      try {
        List cl = getClasses( pkg );
        for ( Class cls: (List<Class>) cl ) {
          if ( ! Modifier.isAbstract(cls.getModifiers()) ) classes.add( Util.getClassName( cls ));
        }
      } catch ( IOException ioe ) {
        // something must be very wrong to fail here
        Logger.logError( ioe );
      }
    }
    return (String[]) classes.toArray( new String[classes.size()] );
  }

  /**
  Scans all classes accessible from the context class loader which belong to the given package and subpackages.
  Based on http://snippets.dzone.com/posts/show/4831
  Used by listClasses()
  @see #listClasses
  @param packageName The base package
  @return The classes
  @throws ClassNotFoundException
  @throws IOException
  */
  public static List getClasses(String packageName) throws IOException {
    //try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      assert classLoader != null;
      String path = packageName.replace('.', '/');
      Enumeration<URL> resources = classLoader.getResources(path);
      List<String> dirs = new ArrayList<String>();
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        dirs.add(resource.getFile());
      }
      TreeSet<String> classes = new TreeSet<String>();
      for (String directory : dirs) {
        classes.addAll(findClasses(directory, packageName));
      }
      ArrayList<Class> classList = new ArrayList<Class>();
      for (String clazz : classes) {
        try {
          classList.add(Class.forName(clazz));
        } catch ( ClassNotFoundException cnfe ) {
          Logger.logError( "Unable to access class "+clazz, cnfe );
        }
      }
      //return classList.toArray(new Class[classes.size()]);
      return classList;
    //} catch (Exception e) {
      //e.printStackTrace();
      //return null;
    //}
  }

  /**
  Recursive method used to find all classes in a given directory and subdirs.
  Adapted from http://snippets.dzone.com/posts/show/4831 and extended to support use of JAR files
  Fixed to return only classes in given package
  @param directory   The base directory
  @param packageName The package name for classes found inside the base directory
  @return The classes
  @throws ClassNotFoundException
   */
  private static TreeSet<String> findClasses(String directory, String packageName) throws IOException {
    TreeSet<String> classes = new TreeSet<String>();
    if (directory.startsWith("file:") && directory.contains("!")) {
      String pkg = packageName.replace('.', '/'); //fix
      String [] split = directory.split("!");
      URL jar = new URL(split[0]);
      ZipInputStream zip = new ZipInputStream(jar.openStream());
      ZipEntry entry = null;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.getName().endsWith(".class") && entry.getName().startsWith(pkg)) { //fix
          String className = entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
          classes.add(className);
        }
      }
    }
    File dir = new File(directory);
    if (!dir.exists()) {
      return classes;
    }
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(findClasses(file.getAbsolutePath(), packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        classes.add(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
      }
    }
    return classes;
  }

  public static void setFKeyPolicy( int p ) {
    fkeyPolicy = p;
  }
  public static int getFKeyPolicy() {
    return fkeyPolicy;
  }
  public static void setIdGenerator( int type ) {
    idType = type;
  }
  public static int getIdGenerator() {
    return idType;
  }
  public static void cacheCollections(boolean arg) {
    cacheCollections = arg;
  }
  public static void L2Caching( boolean arg ) {
    L2Caching = arg;
  }
}
