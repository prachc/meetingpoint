import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestJetty extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String proofOfLife = null;
    
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config) throws ServletException
    {
    	super.init(config);
    	//to demonstrate it is possible
        Object o = config.getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
        @SuppressWarnings("unused")
		android.content.ContentResolver resolver = (android.content.ContentResolver)o;
        android.content.Context androidContext = (android.content.Context)config.getServletContext().getAttribute("org.mortbay.ijetty.context");
        proofOfLife = androidContext.getApplicationInfo().packageName;
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	StringBuffer sb = new StringBuffer();
		boolean first = true;
        
		response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();
        out.println("TestJetty<br>");
        
        Enumeration paramNames = request.getParameterNames();
	    while(paramNames.hasMoreElements()) {
	    	if(first){
	    		sb.append("?");
	    		first=false;
	    	}else
	    		sb.append("&");
	    	String param = (String)paramNames.nextElement();
	    	sb.append(param);
	    	sb.append("=");
	    	sb.append(request.getParameter(param));
	    }
	    out.println(sb.toString());
		out.flush();
    }
}
