package org.mosip.dataprovider.models;

import java.io.Serializable;

import java.util.List;

//import org.apache.commons.lang3.tuple.Pair;
import org.mosip.dataprovider.util.CommonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.Data;

@Data
public class ResidentModel  implements Serializable {
	
	 private static final long serialVersionUID = 1L;
	private String id;
	private String primaryLanguage;
	private String secondaryLanguage;	
	private String gender;
	private String gender_seclang;
	private String dob;
	private Boolean minor;
	private DynamicFieldValueModel bloodgroup;
	private List<MosipLocationModel> location;
	private List<MosipLocationModel> location_seclang;
	private Contact contact;
	private Name name;
	private Name name_seclang;
	private MosipIndividualTypeModel residentStatus;
	private MosipIndividualTypeModel residentStatus_seclang;

	
	//if minor set guardian
	private ResidentModel guardian;
	
	private BiometricDataModel biometric;
	
	private DynamicFieldValueModel maritalStatus;
	
	private List<DynamicFieldModel> dynaFields;
	private List<MosipDocument> documents;
	private String UIN;
	private String RID;
	
	public ResidentModel() {
	
		id = String.format("%04d", CommonUtil.generateRandomNumbers(1,99999, 1000)[0]);
	//ID must be atleast 12 characters
		id = id + id + id;
	}

	public String toJSONString() {
		
		ObjectMapper mapper = new ObjectMapper();
		//mapper.registerModule(new SimpleModule().addSerializer(Pair.class, new PairSerializer()));
		String jsonStr ="";
		try {
				jsonStr = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
				
				e.printStackTrace();
		}	
		return jsonStr;
	}
}
