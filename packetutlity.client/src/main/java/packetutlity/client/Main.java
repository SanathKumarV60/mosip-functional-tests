package packetutlity.client;

import static io.restassured.RestAssured.given;


import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class Main {

	static String urlBase = "http://localhost:8080/";
	
	public static List<String> generateResidents(int n, Boolean bAdult,Boolean bSkipGuardian, String gender){
		List<String> res = new ArrayList<String>();
		
		String url = urlBase + "resident/" + n;

		JSONObject jsonwrapper = new JSONObject();
		JSONObject jsonReq = new JSONObject();
		JSONObject residentAttrib = new JSONObject();
		
		if(bAdult) {
			residentAttrib.put("Age", "RA_Adult");
		}
		else {
			residentAttrib.put("Age", "RA_Minor");
			residentAttrib.put("SkipGaurdian", bSkipGuardian);
		}
		residentAttrib.put("Gender", gender);
		residentAttrib.put("PrimaryLanguage", "eng");
		jsonReq.put("PR_ResidentAttribute", residentAttrib);
				
		jsonwrapper.put("requests", jsonReq);
		//jsonReq.put("secondaryLanguage", "eng");
		
		//Response response = given().contentType(ContentType.JSON).get(url,jsonReq );
	    Response response = given().contentType(ContentType.JSON).body(jsonwrapper.toString()).post(url);

	    System.out.println(response.getBody().asString());
	    
	    JSONArray resp = new JSONObject(response.getBody().asString()).getJSONArray("response");
	    
	    for(int i=0;i < resp.length(); i++) {
	    	JSONObject obj = resp.getJSONObject(i);
	    	String resFilePath = obj.get("path").toString();
	    	res.add(resFilePath);
	    }
	    return res;
	    
	}
	public static String updateResidentGuardian(String personaFilePath, String guardianFilePath) {
		String url = urlBase + "updateresident/" ;

		JSONObject jsonwrapper = new JSONObject();
		JSONObject jsonReq = new JSONObject();
		JSONObject residentAttrib = new JSONObject();
	
		residentAttrib.put("guardian", guardianFilePath);
		residentAttrib.put("child", personaFilePath);
		
		jsonReq.put("PR_ResidentList", residentAttrib);
		
		jsonwrapper.put("requests", jsonReq);
		Response response = given().contentType(ContentType.JSON).body(jsonwrapper.toString()).post(url);

		String ret = response.getBody().asString();
		System.out.println(ret);
		return ret;
		
	}
	public static List<String> generatePacketTemplates(List<String> residents, String process){
		List<String> res = new ArrayList<String>();
		
		String url = urlBase + "packet/" +process +"/ /";
		JSONObject jsonReq = new JSONObject();
		JSONArray arr = new JSONArray();
		for(String path : residents) {
			arr.put(path);	
		}
		jsonReq.put("personaFilePath",arr);
	    Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
	    System.out.println(response.getBody().asString());
	    JSONArray resp = new JSONObject(response.getBody().asString()).getJSONArray("packets");
	    
	    for(int i=0;i < resp.length(); i++) {
	    	JSONObject obj = resp.getJSONObject(i);
	    	String resFilePath = obj.get("path").toString();
	    	res.add(resFilePath);
	    }
	    return res;
	     
		
	}
	public static Boolean requestOtp(String resFilePath){
		String url = urlBase + "requestotp/email";
		JSONObject jsonReq = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(resFilePath);
		jsonReq.put("personaFilePath", jsonArray);

		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());
		return true;

	}
	public static Boolean verifyOtp(String resFilePath){
		String url = urlBase + "verifyotp/email";
		JSONObject jsonReq = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(resFilePath);
		jsonReq.put("personaFilePath", jsonArray);

		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());
		return true;

	}
	public static String preRegister(String resFilePath){
		String url = urlBase + "preregister/";
		JSONObject jsonReq = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(resFilePath);
		jsonReq.put("personaFilePath", jsonArray);

		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());
		String prid = response.getBody().asString();
		
		return prid;

	}

	public static String bookAppointment(String prid, int nthSlot){
		String url = urlBase + "bookappointment/"+ prid + "/" + nthSlot;
		JSONObject jsonReq = new JSONObject();

		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());

		return prid;

	}

	public static String uploadDocuments(String resFilePath, String prid){
		String url = urlBase + "documents/"+ prid;
		JSONObject jsonReq = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(resFilePath);
		jsonReq.put("personaFilePath", jsonArray);

		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());
	
		return prid;

	}
	public static String register(String prid){
		String url = urlBase + "makepacketandsync/"+ prid ;
		//JSONObject jsonReq = new JSONObject();

		Response response = given().contentType(ContentType.JSON).get(url);
		System.out.println(response.getBody().asString());
		JSONObject jsonResp = new JSONObject(response.getBody().asString());

		String rid = jsonResp.getJSONObject("response").getString("registrationId");
		
		//{"response":{"registrationId":"10002100741000120201227152843","status":"SUCCESS"}}
		return rid;

	}
	public static String registerWithPacket(String prid, String packetPath){
		String url = urlBase + "packet/sync/"+ prid ;

		JSONObject jsonReq = new JSONObject();
		JSONArray arr = new JSONArray();
		arr.put(packetPath);	
		jsonReq.put("personaFilePath",arr);
		Response response = given().contentType(ContentType.JSON).body(jsonReq.toString()).post(url);
		System.out.println(response.getBody().asString());
		JSONObject jsonResp = new JSONObject(response.getBody().asString());

		String rid = jsonResp.getJSONObject("response").getString("registrationId");
		
		//{"response":{"registrationId":"10002100741000120201227152843","status":"SUCCESS"}}
		return rid;

	}

	public static Date generateDob(int minAge, int maxAge) {
		
		
		Random rand = new Random();
		int offset =  rand.nextInt(maxAge - minAge) + minAge;
		Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.YEAR, -offset);
	    return calendar.getTime();

	}
	public static void main(String[] args) throws IOException {

	
		//Step 1 : generate resident
		//generate minor
		List<String> resFiles = generateResidents(1,false,true,"Any");
		//genrate adult
		List<String> adultResidentFiles = generateResidents(1,true,false,"Male");
		
		updateResidentGuardian(resFiles.get(0),adultResidentFiles.get(0));
		
		//Generate template for each of the resident
		List<String> templateFiles = generatePacketTemplates(resFiles, "NEW");
		int n=0;
		for(String resFilePath: resFiles) {
	    	System.out.println(resFilePath);
	    
	    	//step 2: Request Otp
	    	requestOtp(resFilePath);
	    	//Step 3: verify otp
	    	verifyOtp(resFilePath);
	    	//Step 4: send application
	     	String prid = preRegister(resFilePath); 	
	    	//step 5: book appointment
	     	bookAppointment(prid, 1);
	    	//step 6: upload documents
	     	uploadDocuments(resFilePath, prid);
	    	//step 7: generate and upload packet
	     	//String rid  = register(prid);
	     	//test
	     //	String templatePath = "C:\\temp\\10002300012";
	     //	templatePath = "C:\\Mosip.io\\gitrepos\\sanathrepo\\mosip-functional-tests\\packet-utility\\packetcreator\\template\\packettemplate";
	     	
	     	String rid = registerWithPacket(prid, 
	     	//		templatePath);
	     		templateFiles.get(n));
	     	n++;
	     	System.out.println("RID["+ n + "]=" + rid);
	     	
	    	//step 8: verify status
	     
	     	//update RID to resident record
	     	
		}
	 }
/*
	public static JSONObject merge(JSONObject mainNode, JSONObject updateNode) {

	    Iterator<String> fieldNames = updateNode.keys();

	    while (fieldNames.hasNext()) {
	        String updatedFieldName = fieldNames.next();
	        Object valueToBeUpdatedO = mainNode.get(updatedFieldName);
	        Object updatedValueO = updateNode.get(updatedFieldName);

	        // If the node is an @ArrayNode
	        if (valueToBeUpdatedO != null && valueToBeUpdatedO instanceof JSONArray && 
	            updatedValueO instanceof JSONArray) {
	        	JSONArray valueToBeUpdated = (JSONArray)valueToBeUpdatedO;
	        	JSONArray updatedValue = (JSONArray) updatedValueO;
	        	
	            // running a loop for all elements of the updated ArrayNode
	        	
	            for (int i = 0; i < updatedValue.length(); i++) {
	                JSONObject updatedChildNode = updatedValue.getJSONObject(i);
	                // Create a new Node in the node that should be updated, if there was no corresponding node in it
	                // Use-case - where the updateNode will have a new element in its Array
	                if (valueToBeUpdated.length() <= i) {
	                    valueToBeUpdated.put(updatedChildNode);
	                }
	                // getting reference for the node to be updated
	                JSONObject childNodeToBeUpdated = valueToBeUpdated.getJSONObject(i);
	                merge(childNodeToBeUpdated, updatedChildNode);
	            }
	        // if the Node is an @ObjectNode
	        } else if (valueToBeUpdatedO != null && valueToBeUpdatedO instanceof JSONObject) {
	        	
	            merge((JSONObject)valueToBeUpdatedO,(JSONObject) updatedValueO);
	        } else {
	            if (mainNode instanceof JSONObject) {
	                mainNode.put(updatedFieldName,updatedValueO);
	            }
	        }
	    }
	    return mainNode;
	}
	*/
}
