package org.mosip.dataprovider.test;

import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mosip.dataprovider.models.MosipIDSchema;
import org.mosip.dataprovider.models.MosipLocationModel;
import org.mosip.dataprovider.models.ResidentModel;
import org.mosip.dataprovider.preparation.MosipMasterData;
import org.mosip.dataprovider.util.CommonUtil;
import org.mosip.dataprovider.util.RestClient;
import org.mosip.dataprovider.util.Translator;

import variables.VariableManager;

public class CreatePersona {

	static 	Hashtable<Double,List<MosipIDSchema>> tbl;
	static {
		tbl = MosipMasterData.getIDSchemaLatestVersion();
	}
	
	public static JSONObject crateIdentity(ResidentModel resident) {

		//columns which are not arrays -IDSchemaVersion,Phone, email
		
		JSONObject identity = new JSONObject();
		JSONArray array = new JSONArray();
		JSONObject obj = null;
		Double schemaversion = tbl.keys().nextElement();
		List<MosipIDSchema> schemas = tbl.get(schemaversion);

		List<MosipLocationModel> locations =   resident.getLocation();
		List<MosipLocationModel> locations_seclang =   resident.getLocation_seclang();

		for(MosipIDSchema schema:schemas ) {
			
			if(schema.getRequired()) {
				obj = new JSONObject();
				if(schema.getId().equalsIgnoreCase("idschemaVersion")) {
					identity.put(schema.getId(),schemaversion );	
				}
				if(schema.getId().toLowerCase().equals("fullname")) {
					String name = resident.getName().getFirstName() +" " + resident.getName().getMidName()+ " "+ resident.getName().getSurName();
					
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", name);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
						name = resident.getName_seclang().getFirstName() +" "+ resident.getName_seclang().getMidName() + " "+ resident.getName_seclang().getSurName();
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", name);
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);

				}
				if(schema.getId().toLowerCase().equals("firstname")) {
					String name = resident.getName().getFirstName() ;
					
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", name);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
						name = resident.getName_seclang().getFirstName();
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", name);
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);

				}
				if(schema.getId().toLowerCase().equals("lastname")) {
					String name = resident.getName().getSurName() ;
					
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", name);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
						name = resident.getName_seclang().getSurName();
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", name);
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);

				}
				
				if(schema.getId().toLowerCase().equals("middlename")) {
					String name = resident.getName().getMidName() ;
					
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", name);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
						name = resident.getName_seclang().getMidName();
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", name);
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);

				}
				if(schema.getId().toLowerCase().equals("dateofbirth") ||schema.getId().toLowerCase().equals("dob") || schema.getId().toLowerCase().equals("birthdate") ) {
					
					//should be informat yyyy/mm/dd
					//SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");  
					String strDate= resident.getDob();
					String gender ="FML";
					identity.put(schema.getId(), strDate);
					if(resident.getGender().equals("Male"))
						gender = "MLE";
					
					array = new JSONArray();
					obj = new JSONObject();
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", gender);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
					
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", gender);
						array.put(1, obj);
					}
					identity.put("gender", array);
				}
				if(schema.getId().toLowerCase().contains("address")) {
						
					String addr = "#111, 127th Main, " + schema.getId();
					array = new JSONArray();
					obj = new JSONObject();
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", addr);
					array.put(0, obj);
					
					if(resident.getSecondaryLanguage() != null) {	
						
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", Translator.translate(resident.getSecondaryLanguage(),addr));
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);
				}
				if(schema.getId().toLowerCase().contains("residen") /*|| schema.getId().toLowerCase().contains("individual")*/ ) {
					String name = resident.getResidentStatus().getCode() ;
					
					obj.put("language", resident.getPrimaryLanguage());
					obj.put("value", name);
					array.put(0, obj);
					if(resident.getSecondaryLanguage() != null) {	
						name = resident.getResidentStatus_seclang().getCode();
						obj = new JSONObject();
						obj.put("language", resident.getSecondaryLanguage());
						obj.put("value", name);
						array.put(1, obj);
					}
					identity.put(schema.getId(), array);

				}
				
				// construct as per location hierarchy
				array = new JSONArray();
				Boolean bFound= false;
				for(MosipLocationModel locModel: locations) {
					
					System.out.println("Schema.id="+ schema.getId() + "== locModel[" + locModel.getHierarchyLevel() + "]=" +locModel.getHierarchyName());
				
					if(schema.getId().equalsIgnoreCase(locModel.getHierarchyName())  ) {
				
						obj = new JSONObject();
						obj.put("language", locModel.getLangCode());
						obj.put("value", locModel.getName());
						array.put(0, obj);
						bFound= true;
						break;
					}
				}
				if(locations_seclang != null)			
				for(MosipLocationModel locModel: locations_seclang) {
					
					if(schema.getId().equalsIgnoreCase(locModel.getHierarchyName())  ) {
						obj = new JSONObject();
						obj.put("language", locModel.getLangCode());
						obj.put("value", locModel.getName());
						array.put(1, obj);
						break;
					}
				}
				if(bFound) {
					if(schema.getId().equalsIgnoreCase("postalCode")) {
						JSONObject objPostal= array.getJSONObject(0);
						identity.put(schema.getId(), objPostal.get("value"));
					}
					else
						identity.put(schema.getId(), array);
				}
			
				if(schema.getId().toLowerCase().contains("phone") || schema.getId().toLowerCase().contains("mobile") ) {
					identity.put(schema.getId(),  resident.getContact().getMobileNumber());
				}
				if(schema.getId().toLowerCase().contains("email") || schema.getId().toLowerCase().contains("mail") ) {
					identity.put(schema.getId(),  resident.getContact().getEmailId());
				}
				if(schema.getId().toLowerCase().contains("referenceIdentity") ) {
					identity.put(schema.getId(),  resident.getId());	
				}
			}
		}
		return identity;
	}
	/*
	public static JSONObject crateIdentityOld(ResidentModel resident) throws JSONException {
		//columns which are not arrays -IDSchemaVersion,Phone, email
		
		JSONObject identity = new JSONObject();
		JSONArray array = new JSONArray();
		JSONObject obj = new JSONObject();
	
		Double schemaversion = tbl.keys().nextElement();
		List<MosipIDSchema> schemas = tbl.get(schemaversion);
		
		identity.put("IDSchemaVersion", schemaversion);

		List<MosipLocationModel> locations =   resident.getLocation();
		List<MosipLocationModel> locations_seclang =   resident.getLocation_seclang();
		
		for(MosipIDSchema schema:schemas ) {

			if(schema.getId().equals("fullName") && schema.getRequired()) {
				String name = resident.getName().getFirstName() +" " + resident.getName().getMidName()+ " "+ resident.getName().getSurName();
		
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", name);
				array.put(0, obj);
	
				if(resident.getSecondaryLanguage() != null) {	
					name = resident.getName_seclang().getFirstName() +" "+ resident.getName_seclang().getMidName() + " "+ resident.getName_seclang().getSurName();
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", name);
					array.put(1, obj);
				}
				identity.put("fullName", array);
			}
			if(schema.getId().equals("dateOfBirth") && schema.getRequired()) {
				
				//should be informat yyyy/mm/dd
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");  
				String strDate= formatter.format(resident.getDob());
				String gender ="FML";
				identity.put("dateOfBirth", strDate);
				if(resident.getGender().equals("Male"))
					gender = "MLE";
				
				array = new JSONArray();
				obj = new JSONObject();
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", gender);
				array.put(0, obj);
				if(resident.getSecondaryLanguage() != null) {	
				
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", gender);
					array.put(1, obj);
				}
				identity.put("gender", array);
			}
			if(schema.getId().equals("addressLine1") && schema.getRequired()) {
					
				String addr = "#11, 127th Main, Golden Heights";
				array = new JSONArray();
				obj = new JSONObject();
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", addr);
				array.put(0, obj);
				
				if(resident.getSecondaryLanguage() != null) {	
					
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", Translator.translate(resident.getSecondaryLanguage(),addr));
					array.put(1, obj);
				}
				identity.put("addressLine1", array);
			}
			if(schema.getId().equals("addressLine2") && schema.getRequired()) {
				
				String addr = "abcd area";
				array = new JSONArray();
				obj = new JSONObject();
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", addr);
				array.put(0, obj);
				
				if(resident.getSecondaryLanguage() != null) {	
					
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", Translator.translate(resident.getSecondaryLanguage(),addr));
					array.put(1, obj);
				}
				identity.put("addressLine2", array);
			}
			if(schema.getId().equals("addressLine3") && schema.getRequired()) {
				
				String addr = "xyz county";
				array = new JSONArray();
				obj = new JSONObject();
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", addr);
				array.put(0, obj);
				
				if(resident.getSecondaryLanguage() != null) {	
					
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", Translator.translate(resident.getSecondaryLanguage(),addr));
					array.put(1, obj);
				}
				identity.put("addressLine3", array);
			}
			if(schema.getId().equals("residenceStatus") && schema.getRequired()) {
						
				//Resident status
				array = new JSONArray();
				obj = new JSONObject();
				obj.put("language", resident.getPrimaryLanguage());
				obj.put("value", resident.getResidentStatus());
				array.put(0, obj);
					
				if(resident.getSecondaryLanguage() != null) {	
					
					obj = new JSONObject();
					obj.put("language", resident.getSecondaryLanguage());
					obj.put("value", resident.getResidentStatus_seclang());
					array.put(1, obj);
				}
				identity.put("residenceStatus", array);
			}
			// construct as per location hierarchy
			array = new JSONArray();
			Boolean bFound= false;
			for(MosipLocationModel locModel: locations) {
				
				System.out.println("Schema.id="+ schema.getId() + "== locModel[" + locModel.getHierarchyLevel() + "]=" +locModel.getHierarchyName());
			
				if(schema.getId().equalsIgnoreCase(locModel.getHierarchyName()) && schema.getRequired() ) {
	
			
					obj = new JSONObject();
					obj.put("language", locModel.getLangCode());
					obj.put("value", locModel.getName());
					array.put(0, obj);
					bFound= true;
					break;
				}
			}
			if(locations_seclang != null)			
			for(MosipLocationModel locModel: locations_seclang) {
				
				if(schema.getId().equalsIgnoreCase(locModel.getHierarchyName()) && schema.getRequired() ) {
					obj = new JSONObject();
					obj.put("language", locModel.getLangCode());
					obj.put("value", locModel.getName());
					array.put(1, obj);
					break;
				}
			}
			if(bFound) {
				if(schema.getId().equals("postalCode")) {
					JSONObject objPostal= array.getJSONObject(0);
					identity.put(schema.getId(), objPostal.get("value"));
				}
				else
					identity.put(schema.getId(), array);
			}
		
		}
		identity.put("phone",  resident.getContact().getMobileNumber());
		identity.put("email",  resident.getContact().getEmailId());
		identity.put("referenceIdentityNumber",  resident.getId());	
		return identity;
		
	}
	*/
	public static JSONObject createRequestBody(JSONObject requestObject) throws JSONException {
		
		
		JSONObject obj = new JSONObject();
		obj.put("id", "mosip.pre-registration.demographic.create");
		obj.put("version", "1.0");
		obj.put("request", requestObject);
		obj.put("requesttime", CommonUtil.getUTCDateTime(LocalDateTime.now()));
		
		 return obj;
	}
	/*
	 * {"id":"mosip.pre-registration.login.sendotp",
	 *    "request":{"userId":"9845024662"},
	 *    "version":"1.0","requesttime":"2020-12-05T10:01:50.763Z"
	 *    }
	 */
	public static String sendOtpTo(String to) throws JSONException {
		//urlBase
		//https://dev.mosip.net//preregistration/v1/login/sendOtp
		String response ="";
		JSONObject obj = new JSONObject();
		obj.put("id", "mosip.pre-registration.login.sendotp");
		obj.put("version", "1.0");
		obj.put("requesttime", CommonUtil.getUTCDateTime(LocalDateTime.now()));
		JSONObject req = new JSONObject();
		req.put("userId", to);	
		obj.put("request", req);
		//RestClient client = annotation.getRestClient();
		String url = VariableManager.getVariableValue("urlBase") +"/preregistration/v1/login/sendOtp";
		try {
			JSONObject resp = RestClient.post(url, obj);
			response = resp.toString();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return response;
	}
	
	public static String sendOtpToPhone(String mobile) throws JSONException {
		return sendOtpTo(mobile);
	}
	/*
	 * {"id":"mosip.pre-registration.login.useridotp",
	 *  "request":{
	 *        "otp":"111111","userId":"abc.efg@gmail.com"
	 *        },
	 *        "version":"1.0",
	 *        "requesttime":"2020-12-05T10:32:52.541Z"
	 *   }
	 */
	public static String validateOTP(String otp, String mobileOrEmailId) throws JSONException {
		String url = VariableManager.getVariableValue("urlBase") +"/preregistration/v1/login/validateOtp";
		String response ="";
		JSONObject obj = new JSONObject();
		obj.put("id", "mosip.pre-registration.login.useridotp");
		obj.put("version", "1.0");
		obj.put("requesttime", CommonUtil.getUTCDateTime(LocalDateTime.now()));
		JSONObject req = new JSONObject();
		req.put("otp",otp);
		req.put("userId",mobileOrEmailId);
		
		obj.put("request", req);
		
		//RestClient client = annotation.getRestClient();

		
		try {
			JSONObject resp = RestClient.post(url, obj);
			response = resp.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return response;
		
	}
		
}
