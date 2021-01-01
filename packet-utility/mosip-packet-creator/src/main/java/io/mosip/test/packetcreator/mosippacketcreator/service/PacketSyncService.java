package io.mosip.test.packetcreator.mosippacketcreator.service;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mosip.dataprovider.ResidentDataProvider;
import org.mosip.dataprovider.models.AppointmentModel;
import org.mosip.dataprovider.models.AppointmentTimeSlotModel;
import org.mosip.dataprovider.models.CenterDetailsModel;
import org.mosip.dataprovider.models.MosipDocument;
import org.mosip.dataprovider.models.ResidentModel;
import org.mosip.dataprovider.test.CreatePersona;
import org.mosip.dataprovider.test.ResidentPreRegistration;
import org.mosip.dataprovider.test.prereg.PreRegistrationSteps;
import org.mosip.dataprovider.util.Gender;
import org.mosip.dataprovider.util.ResidentAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.test.packetcreator.mosippacketcreator.dto.ResidentRequestDto;
import variables.VariableManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Service
public class PacketSyncService {

    private static final String UNDERSCORE = "_";
    private static final Logger logger = LoggerFactory.getLogger(PacketSyncService.class);

    @Autowired
    private APIRequestUtil apiRequestUtil;

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private PreregSyncService preregSyncService;
    
    @Autowired
    private ZipUtils zipUtils;
    @Autowired
    private PacketMakerService packetMakerService;
    
    @Autowired
    private PacketSyncService packetSyncService;
    
    @Value("${mosip.test.primary.langcode}")
    private String primaryLangCode;

    @Value("${mosip.test.packet.template.process:NEW}")
    private String process;

    @Value("${mosip.test.regclient.centerid}")
    private String centerId;

    @Value("${mosip.test.regclient.machineid}")
    private String machineId;

    @Value("${mosip.test.packet.syncapi}")
    private String syncapi;

    @Value("${mosip.test.packet.uploadapi}")
    private String uploadapi;

    @Value("${mosip.test.prereg.mapfile:Preregistration.properties}")
    private String preRegMapFile;

  //this will generate the requested number of resident data
    // Save the data in configured path as JSON
    // return list of resident Ids
    public String generateResidentData(int count,ResidentRequestDto residentRequestDto) {
    	
    	  logger.info("gen Res Data " + residentRequestDto.getAge() + " " 
    	  + residentRequestDto.getGender() 
    	  + " "+ residentRequestDto.getPrimaryLanguage());
    	
		ResidentDataProvider provider = new ResidentDataProvider();
		Gender enumGender = residentRequestDto.getGender(); //Gender.valueOf(residentRequestDto.getGender());
		provider.addCondition(ResidentAttribute.RA_Count, count);
		if(residentRequestDto.getAge() != null && !residentRequestDto.getAge().equals(""))
			provider.addCondition(ResidentAttribute.RA_Age, ResidentAttribute.valueOf(residentRequestDto.getAge()));
		else
			provider.addCondition(ResidentAttribute.RA_Age, ResidentAttribute.RA_Adult);
		provider.addCondition(ResidentAttribute.RA_Gender, enumGender);
		if(residentRequestDto.getPrimaryLanguage() == null || residentRequestDto.getPrimaryLanguage().equals(""))
			provider.addCondition(ResidentAttribute.RA_PRIMARAY_LANG, "eng");
		else
			provider.addCondition(ResidentAttribute.RA_PRIMARAY_LANG, residentRequestDto.getPrimaryLanguage());

		if(! (residentRequestDto.getSecondaryLanguage() == null || residentRequestDto.getSecondaryLanguage().equals("")))
			provider.addCondition(ResidentAttribute.RA_SECONDARY_LANG, residentRequestDto.getSecondaryLanguage());
		
		logger.info("before Genrate");
		List<ResidentModel> lst = provider.generate();
		logger.info("After Genrate");
		
		ObjectMapper Obj = new ObjectMapper();
		JSONArray outIds = new JSONArray();
		
		try {
			String tmpDir;
			
			tmpDir = Files.createTempDirectory("residents_").toFile().getAbsolutePath();
			
			for(ResidentModel r: lst) {
				String jsonStr = Obj.writeValueAsString(r);
				Path tempPath = Path.of(tmpDir, r.getId() +".json");
				Files.write(tempPath, jsonStr.getBytes());
				
				JSONObject id  = new JSONObject();
				id.put("id", r.getId());
				id.put("path", tempPath.toFile().getAbsolutePath());
				outIds.put(id);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject response = new JSONObject();
		response.put("status", "SUCCESS");
		response.put("response",outIds);
    	return response.toString();
    			//"{\"status\":\"SUCCESS\"}";
    }
    public JSONObject makePacketAndSync(String preregId) throws Exception {
    	logger.info("makePacketAndSync for PRID : {}", preregId);

    	String location = preregSyncService.downloadPreregPacket( preregId);
        logger.info("Downloaded the prereg packet in {} ", location);
        File targetDirectory = Path.of(preregSyncService.getWorkDirectory(), preregId).toFile();
        if(!targetDirectory.exists()  && !targetDirectory.mkdir())
            throw new Exception("Failed to create target directory ! PRID : " + preregId);

        if(!zipUtils.unzip(location, targetDirectory.getAbsolutePath()))
            throw new Exception("Failed to unzip pre-reg packet >> " + preregId);

        Path idJsonPath = Path.of(targetDirectory.getAbsolutePath(), "ID.json");

        logger.info("Unzipped the prereg packet {}, ID.json exists : {}", preregId, idJsonPath.toFile().exists());

        String packetPath = packetMakerService.createContainer(idJsonPath.toString(),null);

        logger.info("Packet created : {}", packetPath);

        String response = packetSyncService.syncPacketRid(packetPath, "dummy", "APPROVED",
                "dummy");

        logger.info("RID Sync response : {}", response);
    	JSONObject functionResponse = new JSONObject();
    	JSONObject nobj = new JSONObject();
    
        JSONArray packets =  new JSONArray(response);
        if(packets.length() > 0) {
        	JSONObject resp = (JSONObject) packets.get(0);
        	if(resp.getString("status").equals("SUCCESS")) {
        	//RID Sync response : [{"registrationId":"10010100241000120201214134111","status":"SUCCESS"}]
        		String rid = resp.getString("registrationId");
        		response =  packetSyncService.uploadPacket(packetPath);
        		logger.info("Packet Sync response : {}", response);
        		JSONObject obj =  new JSONObject(response);
        		if(obj.getString("status").equals("Packet has reached Packet Receiver")) {
        	        		 
        		//{"status":"Packet has reached Packet Receiver"}
        			
        			functionResponse.put("response", nobj );
        			nobj.put("status", "SUCCESS");
        			nobj.put("registrationId", rid);
        			return functionResponse;
        		}
        	}
        }
    	functionResponse.put("response", nobj );
		nobj.put("status", "Failed");
		
        //{"status": "Failed"} or {"status": "Passed"}  instead of "Failed"
        return functionResponse;
    	
    }
    public String syncPacketRid(String containerFile, String name,
                                String supervisorStatus, String supervisorComment) throws Exception {
        Path container = Path.of(containerFile);
        String rid = container.getName(container.getNameCount()-1).toString().replace(".zip", "");
        logger.info("Syncing data for RID : {}", rid);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("registrationId", rid);
        jsonObject.put("langCode", primaryLangCode);
        jsonObject.put("name", name);
        jsonObject.put("email", "");
        jsonObject.put("phone", "");
        jsonObject.put("registrationType", process);

        byte[] fileBytes = Files.readAllBytes(container);

        jsonObject.put("packetHashValue", cryptoUtil.getHexEncodedHash(fileBytes));
        jsonObject.put("packetSize", fileBytes.length);
        jsonObject.put("supervisorStatus", supervisorStatus);
        jsonObject.put("supervisorComment", supervisorComment);
        JSONArray list = new JSONArray();
        list.put(jsonObject);

        JSONObject wrapper = new JSONObject();
        wrapper.put("id", "mosip.registration.sync");
        wrapper.put("requesttime", apiRequestUtil.getUTCDateTime(LocalDateTime.now(ZoneOffset.UTC)));
        wrapper.put("version", "1.0");
        wrapper.put("request", list);

        String packetCreatedDateTime = rid.substring(rid.length() - 14);
        String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
                + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
        LocalDateTime timestamp = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

        String requestBody = Base64.encodeBase64URLSafeString(
                cryptoUtil.encrypt(wrapper.toString().getBytes("UTF-8"),
                centerId + UNDERSCORE + machineId, timestamp));

        JSONArray response = apiRequestUtil.syncRid(syncapi, requestBody, apiRequestUtil.getUTCDateTime(timestamp));

        return response.toString();
    }

    public String uploadPacket(String path) throws Exception {
        JSONObject response = apiRequestUtil.uploadFile(uploadapi, path);
        return response.toString();
    }

    public String preRegisterResident(List<String> personaFilePath) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	
    	for(String path: personaFilePath) {
    		ResidentModel resident = readPersona(path);
    		String response = PreRegistrationSteps.postApplication(resident);
    		//preregid
    		saveRegIDMap(response, path);
    		builder.append(response);
    	}
    	return builder.toString();
    }
    void saveRegIDMap(String preRegId, String personaFilePath) throws IOException{
    	 
    	FileReader reader=new FileReader(preRegMapFile);  
    	Properties p=new Properties();  
    	p.load(reader);
    	p.put(preRegId,  personaFilePath);
    	p.store(new FileWriter(preRegMapFile),"PreRegID to persona mapping file");  
    	
    }
    String getPersona(String preRegId) {
    	try {
    		FileReader reader=new FileReader(preRegMapFile);  
    		Properties p=new Properties();  
    		p.load(reader);
    		return p.getProperty(preRegId);
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    public String requestOtp(List<String> personaFilePath, String to) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	
    	
    	for(String path: personaFilePath) {
    		ResidentModel resident = readPersona(path);
    		ResidentPreRegistration preReg = new ResidentPreRegistration(resident);
    		preReg.sendOtpTo(to);
    		
    	}
    	return builder.toString();
    }
    public String verifyOtp(String personaFilePath, String to, String otp) throws IOException {
    
    	ResidentModel resident = readPersona(personaFilePath);
    	ResidentPreRegistration preReg = new ResidentPreRegistration(resident);
    	
    	preReg.fetchOtp();
    	return preReg.verifyOtp(to,otp);
    		
    }
    public String bookAppointment( String preRegID,int nthSlot) {
   	
    String retVal= "{\"Failed\"}";
    Boolean bBooked = false;
	
   	 AppointmentModel res = PreRegistrationSteps.getAppointments();
		
		for( CenterDetailsModel a: res.getAvailableDates()) {
			if(!a.getHoliday()) {
				for(AppointmentTimeSlotModel ts: a.getTimeslots()) {
					if(ts.getAvailability() > 0) {
						nthSlot--;
						if(nthSlot ==0) {
							retVal =PreRegistrationSteps.bookAppointment(preRegID,a.getDate(),res.getRegCenterId(),ts);
							bBooked = true;
							
							break;
						}
					}
				}
			}
			if(bBooked) break;
		}
		return retVal;
    }
   
    public String uploadDocuments(String personaFilePath, String preregId) throws IOException {
    
    	String response = "";
    	ResidentModel resident = readPersona(personaFilePath);
    	 
    	//System.out.println("uploadProof " + docCategory);
   	 
    	for(MosipDocument a: resident.getDocuments()) {
    		JSONObject respObject = PreRegistrationSteps.UploadDocument(a.getDcoCategoryName(),
				 a.getType().get(0).getCode(),
				 a.getDocCategoryLang(), a.getDocs().get(0) ,preregId);
    		if(respObject != null)
    			response = response + respObject.toString();
    	}
		    
    	return response;
    }
    
    ResidentModel readPersona(String filePath) throws IOException {
    	
    	ObjectMapper obj = new ObjectMapper();
    	byte[] bytes = Files.readAllBytes(Path.of(filePath));
		return obj.readValue(bytes, ResidentModel.class);
		
    }
}
