package eu.diykits.ecotron.db;

import java.util.*;
import org.vrspace.server.*;
import org.vrspace.util.*;

public class GenericEntry extends DBObject {
  static java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  public static final String[] _index = { "stationId", "time", "gpsTime", "can1", "can2", "can3", "can4", "can5" };
	public String stationId;
	public Date time = new Date();
	public float can1;
	public float can2;
	public float can3;
	public float can4;
	public float can5;
	public float temp;
	/** SIM808 AT+CGNSINF return parameters

	+CGNSINF: <GNSS run status>,<Fix status>,
	<UTC date & Time>,<Latitude>,<Longitude>,
	<MSL Altitude>,<Speed Over Ground>,
	<Course Over Ground>,
	<Fix Mode>,<Reserved1>,<HDOP>,<PDOP>,
	<VDOP>,<Reserved2>,<GNSS Satellites in View>,
	<GNSS Satellites Used>,<GLONASS Satellites
	Used>,<Reserved3>,<C/N0 max>,<HPA>,<VPA>
	*/
	public String gpsInfo;
	/*
	Index Parameter Unit Range
	1 GPS run status -- 0-1 1
	2 Fix status -- 0-1 1
	3 UTC date & Time yyyyMMddhh
	mmss.sss yyyy: [1980,2039]
	MM : [1,12]
	dd: [1,31]
	hh: [0,23]
	mm: [0,59]
	ss.sss:[0.000,60.999] 18
	4 Latitude ±dd.dddddd [-90.000000,90.000000] 10
	5 Longitude ±ddd.dddddd [-180.000000,180.000000] 11
	6 MSL Altitude meters 7 Speed Over Ground Km/hour [0,999.99] 6
	8 Course Over Ground degrees [0,360.00] 6
	9 Fix Mode -- 0,1,2 [1] 1
	10 Reserved1 11 HDOP -- [0,99.9] 4
	12 PDOP -- [0,99.9] 4
	13 VDOP -- [0,99.9] 4
	14 Reserved2 15 GPS Satellites in View -- [0,99] 2
	16 GNSS Satellites Used -- [0,99] 2
	17 GLONASS Satellites in View -- [0,99] 2
	18 Reserved3
	19 C/N0 max dBHz [0,55] 2
	20 HPA meters [0,9999.9] 6
	21 VPA meters [0,9999.9] 6
	*/
	public Date gpsTime;
	public float gpsLatitude;
	public float gpsLongitude;
	public float gpsAltitude;
	public float gpsSpeed;
	public float gpsCourse;
	public float gpsHdop;
	public float gpsPdop;
	public float gpsVdop;
	/** battery level info */
	public String batInfo;

	public GenericEntry() {}
	public GenericEntry(Map<String,String[]> params) {
    for ( String key: params.keySet() ) {
      String[] values = params.get(key);
      if ( values != null ) {
        for ( String val: values ) {
          Logger.logDebug(key+" = "+val);
          try {
            setField(key,val);
            if ( "gpsInfo".equals( key )) {
              String[] gps = val.split(",");
              Logger.logDebug("Got gpsInfo with "+gps.length+" parameters");
              String timeString = gps[2]; //TODO:parse
              String year = timeString.substring(0,4);
              String month = timeString.substring(4,6);
              String day = timeString.substring(6,8);
              String hour = timeString.substring(8,10);
              String minute = timeString.substring(10,12);
              String second = timeString.substring(12);
              Logger.logDebug("GPS time: "+year+"-"+month+"-"+day+" "+hour+":"+minute+":"+second);
              gpsTime = dateFormat.parse(year+"-"+month+"-"+day+" "+hour+":"+minute+":"+second);
              String latString = gps[3];
              gpsLatitude = new Float(latString);
              String longString = gps[4];
              gpsLongitude = new Float(longString);
              String altString = gps[5];
              gpsAltitude = new Float(altString);
              String speedString = gps[6];
              gpsSpeed = new Float( speedString );
              String courseString = gps[7];
              gpsCourse = new Float( courseString );
              String hdopString = gps[10];
              gpsHdop = new Float( hdopString );
              String vdopString = gps[11];
              gpsVdop = new Float( vdopString );
              String pdopString = gps[12];
              gpsPdop = new Float( pdopString );
            }
          } catch ( Exception e ) {
            Logger.logError("Cannot set field "+key+" to "+val+" - ",e);
          }
        }
      }
    }

	}
}