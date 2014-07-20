

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONStringer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.os.Bundle;
import android.util.Log;

public class JsonOutputBuilder {
	private Bundle bundle;
	private String xml;
	private String errorxml;
	
	public void setBundle(Bundle tbundle){
		bundle = tbundle;
	}
	
	public void setXML(String txml){
		xml = txml;
	}
	
	public void setErrorXML(String exml){
		errorxml = exml;
	}
	
	public String getJSON(){
		JSONStringer stringer = new JSONStringer();
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			StringReader reader = new StringReader( xml );
			InputSource inputSource = new InputSource( reader );
			Document doc = docBuilder.parse(inputSource);
			reader.close();
			
	        toJSON(doc.getElementsByTagName("object"),0,stringer,false);
		}catch (Exception e) {
			Log.i("JsonBuilder",e.toString());
		}
		Log.i("JsonBuilder","return:"+stringer.toString());
		return stringer.toString();
	}
	
	public String getErrorJSON(){
		JSONStringer stringer = new JSONStringer();
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbf.newDocumentBuilder();
			StringReader reader = new StringReader( errorxml );
			InputSource inputSource = new InputSource( reader );
			Document doc = docBuilder.parse(inputSource);
			reader.close();
			
	        toJSON(doc.getElementsByTagName("error"),0,stringer,false);
		}catch (Exception e) {
			Log.i("JsonBuilder",e.toString());
		}
		Log.i("JsonBuilder","return:"+stringer.toString());
		return stringer.toString();
	}
	
	public void toJSON(NodeList nl, int level,JSONStringer stringer,boolean loop)throws JSONException{
		System.out.println("---");
		//System.out.println("\nlevel="+level);
		for (int i = 0; i < nl.getLength(); i++) {
			Node currentNode = nl.item(i);
			if(!currentNode.getNodeName().equals("#text")){	
				System.out.println("current-->"+currentNode.getNodeName());
				//for(int j=0;j<level;j++) System.out.print("  ");
				if(currentNode.getNodeName().equals("object")){
					if (!loop) {
						System.out.println(">>object start");
						//mode = "object";
						stringer.object();
						toJSON(currentNode.getChildNodes(), ++level, stringer, loop);
						stringer.endObject();
						System.out.println(">>object end");
						return;
					}else{
						//stringer.array();
						int arrayindex=getIndex(currentNode,"object");
						//System.out.println("debug>> "+currentNode.getNodeName());
						
						for (int j = 0; j < arrayindex; j++) {
							System.out.println(">>object start");
							stringer.object();
							//System.out.println("kindex="+(currentNode.getChildNodes().getLength()-1)/2);
							for (int k = 0; k < (currentNode.getChildNodes().getLength()-1)/2; k++) {
								//System.out.println("debug n>> "+currentNode.getChildNodes().item(2*k+1).getNodeName());
								//System.out.println("debug v>> "+currentNode.getChildNodes().item(2*k+1).getChildNodes().item(0).getNodeValue());
								if(k%2==0){
									System.out.print(">-->key added");
									System.out.println("("+currentNode.getChildNodes().item(2*k+1).getChildNodes().item(0).getNodeValue()+")");
									stringer.key(currentNode.getChildNodes().item(2*k+1).getChildNodes().item(0).getNodeValue());
								}else{
									String[] resolved = resolveStringArray(currentNode.getChildNodes().item(2*k+1).getChildNodes().item(0).getNodeValue());
									
									if(resolved.length!=1){
										System.out.print(">-->value added");
										System.out.println("("+resolved[j]+")");
										stringer.value(resolved[j]);
									}else{
										System.out.print(">-->value added");
										System.out.println("("+resolved[0]+")");
										stringer.value(resolved[0]);
									}
								}
							}
							stringer.endObject();
							System.out.println(">>object end");
						}
						//stringer.endArray();
						System.out.println("loop end 1");
						loop=false;
						//break;
						//System.out.println("loop obj idx="+getIndex(currentNode,"object"));
					}
				}else if(currentNode.getNodeName().equals("error")){
					System.out.println(">>error start");
					//mode = "object";
					stringer.object();
					toJSON(currentNode.getChildNodes(), ++level, stringer, loop);
					stringer.endObject();
					System.out.println(">>error end");
					return;
				}else if(currentNode.getNodeName().equals("name")){
					System.out.println(">>name start");
					//mode = "name";
					System.out.print(">-->key added");
					System.out.println("("+currentNode.getChildNodes().item(0).getNodeValue()+")");
					stringer.key(currentNode.getChildNodes().item(0).getNodeValue());
					System.out.println(">>name end");
				}else if(currentNode.getNodeName().equals("value")){
					System.out.println(">>value start");
					//mode = "value";
					
					if(!loop){
						System.out.print(">-->value added");
						System.out.println("("+currentNode.getChildNodes().item(0).getNodeValue()+")");
						stringer.value(resolveString(currentNode.getChildNodes().item(0).getNodeValue()));
					}else{
						//System.out.println("loop=true");
						System.out.println("debug>>"+getName(currentNode.getChildNodes().item(0).getNodeValue()));
						String[] values = bundle.getStringArray(getName(currentNode.getChildNodes().item(0).getNodeValue()));
						//int index=getIndex(currentNode,"value");
						for (int j = 0; j < values.length; j++) {
							stringer.value(values[j]);
						}
						System.out.println("loop end 2");
						loop=false;
					}
					System.out.println(">>value end");
					
				}else if(currentNode.getNodeName().equals("array")){
					System.out.println(">>array start");
					//mode = "array";
					stringer.array();
					toJSON(currentNode.getChildNodes(),++level,stringer,loop);
					stringer.endArray();
					System.out.println(">>array end");
					System.out.println("loop="+loop);
					
					
					//loop=false;
					//continue;
				}else if(currentNode.getNodeName().equals("loop")){
					System.out.println(">>loop start");
					//System.out.println("loop start 1");
					loop = true;
					toJSON(currentNode.getChildNodes(),++level,stringer,loop);
					System.out.println(">>loop end");
				}
				
				
			}
		}
		
	}
	
	private String resolveString(String name){
		if(!name.contains("."))
			return name;
		else{
			return bundle.getString(getName(name));
		}
	}
	
	private String[] resolveStringArray(String name){
		//System.out.println("resolving:"+name);
		if(!name.contains("."))
			return new String[]{name};
		else{
			return bundle.getStringArray(getName(name));
		}
	}
	
	private String getName(String longname){
		String[] names = longname.split("\\.");
		//System.out.println("getname:"+names[names.length-1]);
		return names[names.length-1];
		
	}

	private int getIndex(Node n,String mode){
		if(mode.equals("value")){
			return bundle.getStringArray(getName(n.getNodeValue())).length;
		}else if(mode.equals("object")){
			NodeList nl = n.getChildNodes();
			int max = 0;
			for (int i = 0; i < nl.getLength(); i++) {
				Node currentNode = nl.item(i);
				if(!currentNode.getNodeName().equals("#text")){	
					if(currentNode.getNodeName().equals("value")){	
						//System.out.println("here v:"+currentNode.getChildNodes().item(0).getNodeValue());
						if(currentNode.getChildNodes().item(0).getNodeValue().contains(".")){
							int temp = bundle.getStringArray(getName(currentNode.getChildNodes().item(0).getNodeValue())).length;
							max = max(max,temp);
						}
					}
				}
			}
			return max;
		}else 
			return 1;
		/*System.out.println("idx");
		for (int i = 0; i < nl.getLength(); i++) {
			Node currentNode = nl.item(i);
			if(!currentNode.getNodeName().equals("#text")){	
				System.out.println("index("+i+"):"+currentNode.getNodeName());
				//if(currentNode.getNodeName().equals("object")){
				//	
				//}
			}
		}*/
	}
	
	private int max(int a,int b){
		if(a>b)
			return a;
		else return b;
	}
}
