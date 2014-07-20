import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

@SuppressWarnings("serial")
public class MeetingPointRegistration extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
	
	private String input_lat;
	private String input_lng;
	
	private Thread threadGPSLocator;
	private Parcel replyGPSLocator;
	private String GPSLocator_Latitude;
	private String GPSLocator_Longitude;
	private static final int MS_GPSLocator_SUCCEED = 0x101;
    private static final int MS_GPSLocator_FAILED  = 0x10F;
	
	private Thread threadMiddleLat;
	private String MiddleLat_Result;
	private static final int AR_MiddleLat_SUCCEED  = 0x201;
    private static final int AR_MiddleLat_FAILED   = 0x20F;
	
	private Thread threadMiddleLng;
	private String MiddleLng_Result;
	private static final int AR_MiddleLng_SUCCEED  = 0x301;
    private static final int AR_MiddleLng_FAILED   = 0x30F;
	
	private Thread threadGourNavi;
	private Parcel replyGourNavi;
	private WSComponentParser GourNaviWSCParser;
	private String[] GourNavi_Name;
	private String[] GourNavi_Category;
	private String[] GourNavi_Latitude;
	private String[] GourNavi_Longitude;
	private static final int WS_GourNavi_SUCCEED   = 0x401;
    private static final int WS_GourNavi_FAILED    = 0x40F;
	
	private Thread threadDistanceCalculator;
	private String[] DistanceCalculator_Distance;
	private static final int AR_DistanceCalculator_SUCCEED   = 0x501;
    private static final int AR_DistanceCalcularor_FAILED    = 0x50F;

    private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_MiddleLat = false;
    private boolean Synchronize_1_MiddleLng = false;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		if(!stm.isFree) waitserver();
		
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		out = response.getWriter();
		
		input_lat = request.getParameter("lat");
		input_lng = request.getParameter("lng");
		
		hthread = new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
			        @Override 
			        public void handleMessage(Message msg){
			        	ThreadMonitor tm = ThreadMonitor.getInstance(); 
			        	JsonOutputBuilder job = new JsonOutputBuilder();
			        	Bundle tempBundle;
			        	String[] tempArray;
			        	switch (msg.what) {
		                case MS_GPSLocator_SUCCEED:
		                	stopThread(threadGPSLocator);
		                	
		                	tempBundle = replyGPSLocator.readBundle();
		                	GPSLocator_Latitude = tempBundle.getString("LATITUDE");
		                	GPSLocator_Longitude = tempBundle.getString("LONGITUDE");
		                	
		                	prepareMiddleLat();
		                	threadMiddleLat.run();
		                	prepareMiddleLng();
		                	threadMiddleLng.run();
		                	
		                	break;
			        	case MS_GPSLocator_FAILED:
		                	stopThread(threadGPSLocator);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}

		            		break;
			        	case AR_MiddleLat_SUCCEED:
			        		stopThread(threadMiddleLat);
			        		
			        		Synchronize_1_MiddleLat = true;
			        		
			        		if(Synchronize_1_MiddleLat&&Synchronize_1_MiddleLng)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("MiddleLat finished, other=not yet");
							break;
			        	case AR_MiddleLat_FAILED:
		                	stopThread(threadMiddleLat);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}
		            		
		            		break;
			        	case AR_MiddleLng_SUCCEED:
			        		stopThread(threadMiddleLng);
			        		
			        		Synchronize_1_MiddleLng = true;
			        		
			        		if(Synchronize_1_MiddleLat&&Synchronize_1_MiddleLng)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("MiddleLng finished, other=not yet");
			        		break;
			        	case AR_MiddleLng_FAILED:
		                	stopThread(threadMiddleLng);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}

		            		break;
			        	case SYNCHRONIZE_1:
			        		Synchronize_1_MiddleLat = false;
			        		Synchronize_1_MiddleLng = false;
			        		
		                   	tempBundle = new Bundle();
		                   	tempBundle.putString("MiddleLatResult", MiddleLat_Result);
		                   	tempBundle.putString("MiddleLngResult", MiddleLng_Result);
		                   	GourNaviWSCParser = new WSComponentParser();
		                   	GourNaviWSCParser.setBundle(tempBundle);
		                   	GourNaviWSCParser.setXML(getGourNaviXML());
		                   	
							prepareGourNavi();
							threadGourNavi.run();
		                	
		                   	break;
			        	case WS_GourNavi_SUCCEED:
			        		stopThread(threadGourNavi);
							
		                	tempBundle = replyGourNavi.readBundle();
		                	tempArray = tempBundle.getStringArray("Name");
		                	GourNavi_Name = tempArray;
		                	tempArray = tempBundle.getStringArray("Category");
		                	GourNavi_Category = tempArray;
		                	tempArray = tempBundle.getStringArray("Latitude");
		                	GourNavi_Latitude = tempArray;
		                	tempArray = tempBundle.getStringArray("Longitude");
		                	GourNavi_Longitude = tempArray;
		                	
		                	prepareDistanceCalculator();
		                	threadDistanceCalculator.run();
		            
		                	break;
			        	case WS_GourNavi_FAILED:
		                	stopThread(threadGourNavi);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}

		            		break;
			        	case AR_DistanceCalculator_SUCCEED:
			        		stopThread(threadDistanceCalculator);
			        		
			        		mHandler.sendEmptyMessage(OUTPUT);
		
			        		break;
			        	case AR_DistanceCalcularor_FAILED:
		                	stopThread(threadDistanceCalculator);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}

		            		break;
			        	case OUTPUT:
			        		tempBundle = new Bundle();
			        		tempBundle.putStringArray("Name", GourNavi_Name);
			        		tempBundle.putStringArray("Category", GourNavi_Category);
			        		tempBundle.putStringArray("Latitude", GourNavi_Latitude);
			        		tempBundle.putStringArray("Longitude", GourNavi_Longitude);
			        		tempBundle.putStringArray("Distance",DistanceCalculator_Distance);
			        		
			        		job.setBundle(tempBundle);
		                	job.setXML(getOutputXML());
		                	Result = job.getJSON();
		                	
		            		stopThread(hthread);
		            		synchronized (tm){debug("tm.notified();");tm.notify();}
			        		break;
			        	default:
			        		super.handleMessage(msg);
			        	}
			        }
			    };
			    prepareGPSLocator();
				threadGPSLocator.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		 
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
	}
	
	public void prepareGPSLocator(){
		threadGPSLocator = new Thread(new Runnable() {
			public void run() {
				replyGPSLocator = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.GPSService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x67707301;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("GPSLocator Service connected: "+ name.flattenToShortString());
						
						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String type = "null";
						bundle.putString("TYPE",type);						
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyGPSLocator, 0);
						} catch (RemoteException ex) {
							debug("GPSLocator Service Remote exception when calling service:"+ex.toString());
							res = false;
						}
						
						if (res)
							mHandler.sendEmptyMessage(MS_GPSLocator_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_GPSLocator_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("GPSLocator Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);
				
				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_GPSLocator_FAILED);
				}
			}
		});
	}
	
	public void prepareMiddleLat(){
		threadMiddleLat = new Thread(new Runnable() {
			public void run() {
				MiddleLat_Result = ARFunction.average(GPSLocator_Latitude,input_lat);
				mHandler.sendEmptyMessage(AR_MiddleLat_SUCCEED);
			}
		});
	}
	
	public void prepareMiddleLng(){
		threadMiddleLng = new Thread(new Runnable() {
			public void run() {
				MiddleLng_Result = ARFunction.average(GPSLocator_Longitude,input_lng);
				mHandler.sendEmptyMessage(AR_MiddleLng_SUCCEED);
			}
		});
	}
	
	public void prepareGourNavi(){
		threadGourNavi = new Thread(new Runnable() {
			public void run() {
				replyGourNavi = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.WSCService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x101;
					public void onServiceConnected(ComponentName cname,IBinder service) {
						debug("ExchangeRate WS Service connected: "+ cname.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						bundle.putString("BASE", GourNaviWSCParser.base);
						bundle.putStringArray("PATHS",GourNaviWSCParser.paths);
						bundle.putStringArray("KEYS", GourNaviWSCParser.keys);
						bundle.putStringArray("VALUES",GourNaviWSCParser.values);
						bundle.putString("FORMAT", GourNaviWSCParser.format);
						
						bundle.putStringArray("NAME", GourNaviWSCParser.name);
						bundle.putStringArray("TYPE", GourNaviWSCParser.type);
						bundle.putStringArray("QUERY", GourNaviWSCParser.query);
						bundle.putStringArray("INDEX", GourNaviWSCParser.index);
						
						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyGourNavi, 0);
						} catch (RemoteException ex) {
							Log.e("onServiceConnected",
									"Remote exception when calling service",ex);
							res = false;
						}
						if (res)
							mHandler.sendEmptyMessage(WS_GourNavi_SUCCEED);
						else
							mHandler.sendEmptyMessage(WS_GourNavi_FAILED);
					}

					public void onServiceDisconnected(ComponentName name) {
						debug("ExchangeRate WS Service disconnected: "+ name.flattenToShortString());

					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("ExchangeRate WS Service could not be connected ");
					mHandler.sendEmptyMessage(WS_GourNavi_FAILED);
				}
			}
		});
	}
	
	public void prepareDistanceCalculator(){
		threadDistanceCalculator = new Thread(new Runnable() {
			public void run() {
				DistanceCalculator_Distance = ARFunction.gpsdistance(input_lat,input_lng,GourNavi_Latitude,GourNavi_Longitude);
				mHandler.sendEmptyMessage(AR_DistanceCalculator_SUCCEED);
			}
		});
	}
	
	public void debug(String msg){
		Log.d("MeetingPointRegistration",msg);
	}
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
	}
	
	public void waitfinal(){
		ThreadMonitor tm = ThreadMonitor.getInstance();
		synchronized (tm) {
			try {
				tm.wait();
			} catch (InterruptedException e) {
				debug("waitfinal()->error="+e.toString());
			}
		}
	}
	
	public void waitserver(){
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		synchronized (stm) {
			try {
				stm.wait();
			} catch (InterruptedException e) {
				debug("waitserver()->error="+e.toString());
			}
		}
	}
    
    public String getOutputXML(){
       	return 
    	"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>Succeed</value>\n"+
		"	<name>name</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>GourNavi.results.Name</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>category</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>GourNavi.results.Category</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>latitude</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>GourNavi.results.Latitude</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>longitude</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>GourNavi.results.Longitude</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"	<name>distance</name>\n"+
		"	<array>\n"+
		"		<loop>\n"+
		"			<value>DistanceCalculator.results.Distance</value>\n"+
		"		</loop>\n"+
		"	</array>\n"+
		"</object>";
    }
    
    public String getErrorXML(){
       	return 
    	"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
    	"<error>\n"+
    	"	<name>ResultStatus</name>\n"+
    	"	<value>failed</value>\n"+
    	"</error>";
    }
    
    public String getGourNaviXML(){
    	return
    	"<component>\n"+
		"	<name>GourNavi</name>\n"+
		"	<role>\n"+
		"		<medium>\n"+
		"			<subscriber-id>002</subscriber-id>\n"+
		"			<subscriber-id>003</subscriber-id>\n"+
		"			<publisher-id>004</publisher-id>\n"+
		"		</medium>\n"+
		"	</role>\n"+
		"	<execution>single</execution>\n"+
		"	<webservice>\n"+
		"		<base>http://api.gnavi.co.jp/</base>\n"+
		"		<paths>\n"+
		"			<path>ver1</path>\n"+
		"			<path>RestSearchAPI</path>\n"+
		"			<path>null</path>\n"+
		"		</paths>\n"+
		"		<keys>\n"+
		"			<key>keyid</key>\n"+
		"			<key>input_coordinates_mode</key>\n"+
		"			<key>coordinates_mode</key>\n"+
		"			<key>latitude</key>\n"+
		"			<key>longitude</key>\n"+
		"			<key>hit_per_page</key>\n"+
		"			<key>range</key>\n"+
		"		</keys>\n"+
		"		<values>\n"+
		"			<value>10d9098dba2f680c748de5b03b28940d</value>\n"+
		"			<value>2</value>\n"+
		"			<value>2</value>\n"+
		"			<value>publisher[002].results.MiddleLatResult</value>\n"+
		"			<value>publisher[003].results.MiddleLngResult</value>\n"+
		"			<value>8</value>\n"+
		"			<value>2</value>\n"+
		"		</values>\n"+
		"		<format>XML</format>\n"+
		"		<results>\n"+
		"			<result>\n"+
		"				<result-name>Name</result-name>\n"+
		"				<type>multiple</type>\n"+
		"				<query>//name</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"			<result>\n"+
		"				<result-name>Category</result-name>\n"+
		"				<type>multiple</type>\n"+
		"				<query>//category</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"			<result>\n"+
		"				<result-name>Latitude</result-name>\n"+
		"				<type>multiple</type>\n"+
		"				<query>//latitude</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"			<result>\n"+
		"				<result-name>Longitude</result-name>\n"+
		"				<type>multiple</type>\n"+
		"				<query>//longitude</query>\n"+
		"				<index>null</index>\n"+
		"				<filter>null</filter>\n"+
		"			</result>\n"+
		"		</results>\n"+
		"	</webservice>\n"+
		"</component>";
    }
}
