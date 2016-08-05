package org.vrspace.util;
/**
Object ID.
Unique object identifier in the system is the class name and a number within
the class. ID encapsulates them.
*/
public class ID implements java.io.Serializable {
  public String className;
  public long id;
  public ID(){}
  /**
  Constructs new ID from "<b>class</b> <b>id</b>" string
  */
  public ID( String classId ) {
    //Logger.logDebug( "New ID("+classId+" )" );
    classId = classId.trim();
    int pos = classId.indexOf( ' ' );
    className = classId.substring(0,pos);
    id=new Long( classId.substring(pos+1) ).longValue();
  }
  /**
  Constructs new ID from <b>className</b> and <b>id</b>
  */
  public ID( String className, long id ) {
    this.className = className;
    this.id = id;
  }
  /**
  Creates new ID from supplied class name and id.
  */
  public ID( Class cls, long id ) {
    this.className = Util.getClassName( cls );
    this.id = id;
  }
  /**
  Returns className+" "+id
  */
  public String toString() {
    return className+" "+id;
  }
  /**
  ID's are equal if class name (without package!) and id match
  */
  public boolean equals( Object o ) {
    //return ( o != null && o.getClass().equals( this.getClass() ) && this.className.equals(((ID)o).className) && this.id == ((ID)o).id);
    return toString().equals( ""+o );
  }

  public int hashCode() {
    int hash = 7;
    //CHECKME: this hashcode may not work for long id!!!
    hash = 31 * hash + (int)id;
    hash = 31 * hash + (null == className ? 0 : className.hashCode());
    return hash;
  }

  /**
  Returns Hibernate mapping xml
  */
  public static String getHibernateMapping() {
    StringBuffer ret = new StringBuffer();

    ret.append( "<?xml version=\"1.0\"?><!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n" );
    ret.append( "<hibernate-mapping package=\"org.vrspace.util\">\n" );
    ret.append( "  <class name=\"ID\" table=\"vrs_ID\">\n" );
    ret.append( "    <composite-id>\n" );
    ret.append( "      <key-property name=\"id\" column=\"vrs_id\" access=\"field\" />\n" );
    ret.append( "      <key-property name=\"className\" column=\"vrs_className\" access=\"field\" />\n" );
    ret.append( "    </composite-id>\n" );
    ret.append("  </class>\n");
    ret.append("</hibernate-mapping>\n");

    return ret.toString();
  }


}
