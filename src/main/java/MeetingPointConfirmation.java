import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

@SuppressWarnings("serial")
public class MeetingPointConfirmation extends HttpServlet{
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	private Thread hthread;
	private static final int OUTPUT = 0xFFF;
	
	private String input_lat;
	private String input_lng;
	private String input_name;
	
	private Thread threadMapApplication;
	private static final int MA_MapApplication_SUCCEED = 0x101;
    private static final int MA_MapApplication_FAILED  = 0x10F;
	
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
		input_name = request.getParameter("name");
		
		debug("lat="+input_lat);
		debug("lng="+input_lng);
		debug("name="+input_name);
		
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
		                case MA_MapApplication_SUCCEED:
		                	stopThread(threadMapApplication);
		                	mHandler.sendEmptyMessage(OUTPUT);
		                	break;
		                case MA_MapApplication_FAILED:
		                	stopThread(threadMapApplication);
		                	
		                	job.setErrorXML(getErrorXML());
		                   	Result = job.getErrorJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){tm.notify();}

		            		break;
		                case OUTPUT:
		            		job.setXML(getOutputXML());
		                	Result = job.getJSON();
		                	
		                	stopThread(hthread);
		            		synchronized (tm){debug("tm.notified();");tm.notify();}
		                	break;
			        	}
			        }
				};
				prepareMapApplication();
				threadMapApplication.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
    	synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
	}
	
	public void prepareMapApplication(){
		threadMapApplication = new Thread(new Runnable() {
			public void run() {
				String uri = "geo:0,0?q="+input_lat+","+input_lng;
				if(input_name!=null)
					uri +=(" (" + input_name + ")");
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				androidContext.startActivity(intent);
				
				mHandler.sendEmptyMessage(MA_MapApplication_SUCCEED);
			}
		});
	}
	
	
	public void debug(String msg){
		Log.d("MeetingPointConfirmation",msg);
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
}
