import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.os.Bundle;

public class WSComponentParser {
	private Bundle bundle;
	private String xml;
	
	public String compname;
	public String base;
	public String[] paths;
	public String[] keys;
	public String[] values;
	public String format;
	
	public String[] name;
	public String[] type;
	public String[] query;
	public String[] index;
	public String[] filter;
	
	public void setBundle(Bundle tbundle){
		bundle = tbundle;
	}
	
	public void setXML(String txml){
		xml = txml;
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			StringReader reader = new StringReader( xml );
			InputSource inputSource = new InputSource( reader );
			Document doc = docBuilder.parse(inputSource);
			
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();
			
			Node tempnode;
			NodeList tempnodes;
			tempnode = (Node) xpath.evaluate("/component/name", doc, XPathConstants.NODE);
			compname = tempnode.getChildNodes().item(0).getNodeValue();
			System.out.println("compname:"+compname);
			
			tempnode = (Node) xpath.evaluate("/component/webservice/base", doc, XPathConstants.NODE);
			base = tempnode.getChildNodes().item(0).getNodeValue();
			System.out.println("base:"+base);
			
			tempnode = (Node) xpath.evaluate("/component/webservice/paths", doc, XPathConstants.NODE);
			tempnodes = tempnode.getChildNodes();
			paths = new String[tempnodes.getLength()/2];
			for (int i = 0; i < tempnodes.getLength(); i++) {
				if(i%2==1){
					String value = tempnodes.item(i).getChildNodes().item(0).getNodeValue();
					if(!value.contains(".")){
						paths[(i-1)/2] = value;
						System.out.println("paths["+(i-1)/2+"]:"+paths[(i-1)/2]);
					}else{
						paths[(i-1)/2] = resolveString(value);
						System.out.println("paths["+(i-1)/2+"]:"+value+"-->"+paths[(i-1)/2]);
					}
				}
			}
			
			tempnode = (Node) xpath.evaluate("/component/webservice/keys", doc, XPathConstants.NODE);
			tempnodes = tempnode.getChildNodes();
			keys = new String[tempnodes.getLength()/2];
			for (int i = 0; i < tempnodes.getLength(); i++) {
				if(i%2==1){
					String value = tempnodes.item(i).getChildNodes().item(0).getNodeValue();
					if(!value.contains(".")){
						keys[(i-1)/2] = value;
						System.out.println("keys["+(i-1)/2+"]:"+keys[(i-1)/2]);
					}else{
						keys[(i-1)/2] = resolveString(value);
						System.out.println("keys["+(i-1)/2+"]:"+value+"-->"+keys[(i-1)/2]);
					}
				}
			}
			
			tempnode = (Node) xpath.evaluate("/component/webservice/values", doc, XPathConstants.NODE);
			tempnodes = tempnode.getChildNodes();
			values = new String[tempnodes.getLength()/2];
			for (int i = 0; i < tempnodes.getLength(); i++) {
				if(i%2==1){
					String value = tempnodes.item(i).getChildNodes().item(0).getNodeValue();
					if(!value.contains(".")){
						values[(i-1)/2] = value;
						System.out.println("values["+(i-1)/2+"]:"+values[(i-1)/2]);
					}else{
						values[(i-1)/2] = resolveString(value);
						System.out.println("values["+(i-1)/2+"]:"+value+"-->"+values[(i-1)/2]);
					}
				}
			}
			
			tempnode = (Node) xpath.evaluate("/component/webservice/format", doc, XPathConstants.NODE);
			format = tempnode.getChildNodes().item(0).getNodeValue();
			System.out.println("format:"+format);
			//System.out.println("base:"+tempnode.getChildNodes().item(0).getNodeValue());
			//base = tempnode.getChildNodes().item(0).getNodeValue();
			
			tempnode = (Node) xpath.evaluate("/component/webservice/results", doc, XPathConstants.NODE);
			tempnodes = tempnode.getChildNodes();
			name = new String[tempnodes.getLength()/2];
			type = new String[tempnodes.getLength()/2];
			query = new String[tempnodes.getLength()/2];
			index = new String[tempnodes.getLength()/2];
			filter = new String[tempnodes.getLength()/2];
			for (int i = 0; i < tempnodes.getLength(); i++) {
				if(i%2==1){
					tempnode = tempnodes.item(i);
					int loopj = tempnode.getChildNodes().getLength();
					for (int j = 0; j < loopj; j++) {
						if((j%2==1)){
							String nname = tempnode.getChildNodes().item(j).getNodeName();
							String value = tempnode.getChildNodes().item(j).getChildNodes().item(0).getNodeValue();
							//System.out.println(nname);
							if(nname.equals("result-name")){
								name[(i-1)/2] = value;
								System.out.println("name["+(i-1)/2+"]:"+name[(i-1)/2]);
							}else if(nname.equals("type")){
								type[(i-1)/2] = value;
								System.out.println("type["+(i-1)/2+"]:"+type[(i-1)/2]);
							}else if(nname.equals("query")){
								query[(i-1)/2] = value;
								System.out.println("query["+(i-1)/2+"]:"+query[(i-1)/2]);
							}else if(nname.equals("index")){
								index[(i-1)/2] = value;
								System.out.println("index["+(i-1)/2+"]:"+index[(i-1)/2]);
							}else if(nname.equals("filter")){
								filter[(i-1)/2] = value;
								System.out.println("filter["+(i-1)/2+"]:"+filter[(i-1)/2]);
							}
						}
					}
				}
			}
			
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String resolveString(String name){
		if(!name.contains("."))
			return name;
		else{
			return bundle.getString(getName(name));
		}
	}
	
	private String getName(String longname){
		String[] names = longname.split("\\.");
		//System.out.println("getname:"+names[names.length-1]);
		return names[names.length-1];
		
	}
}
