package io.mosip.test.packetcreator.mosippacketcreator.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mosip.dataprovider.PacketTemplateProvider;
import org.mosip.dataprovider.ResidentDataProvider;
import org.mosip.dataprovider.models.AppointmentModel;
import org.mosip.dataprovider.models.AppointmentTimeSlotModel;
import org.mosip.dataprovider.models.CenterDetailsModel;
import org.mosip.dataprovider.models.MosipDocument;

import org.mosip.dataprovider.models.ResidentModel;

import org.mosip.dataprovider.test.ResidentPreRegistration;
import org.mosip.dataprovider.test.prereg.PreRegistrationSteps;
import org.mosip.dataprovider.util.CommonUtil;
import org.mosip.dataprovider.util.Gender;
import org.mosip.dataprovider.util.ResidentAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.mosip.test.packetcreator.mosippacketcreator.dto.PersonaRequestDto;
import io.mosip.test.packetcreator.mosippacketcreator.dto.PersonaRequestType;
import io.mosip.test.packetcreator.mosippacketcreator.dto.ResidentRequestDto;
import variables.VariableManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
//import org.mosip.dataprovider.models.PairDeserializer;

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
    @Autowired
    private ContextUtils contextUtils;
    
    
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
   
    @Value("${mosip.test.persona.configpath}")
    private String personaConfigPath;
   

    @Value("${mosip.test.baseurl}")
    private String baseUrl;

    void loadServerContextProperties(String contextKey) {
    	
    	if(contextKey != null && !contextKey.equals("")) {
    		
    		Properties props = contextUtils.loadServerContext(contextKey);
    		props.forEach((k,v)->{
    			String key = k.toString().trim();
    			String ns = VariableManager.NS_DEFAULT;
    			
    			if(!key.startsWith("mosip.test")) {
    	
    				if(key.startsWith("prereg"))
    					ns = VariableManager.NS_PREREG;
    				else
    				if(key.startsWith("regclient"))
        				ns = VariableManager.NS_REGCLIENT;
        					
    				VariableManager.setVariableValue(ns,key, v);
    			}
    			
    		});
    	}
    }

  //this will generate the requested number of resident data
    // Save the data in configured path as JSON
    // return list of resident Ids
    public String generateResidentData(int count,PersonaRequestDto residentRequestDto, String contextKey) {
    	
    	loadServerContextProperties(contextKey);
    	
    	Properties props = residentRequestDto.getRequests().get(PersonaRequestType.PR_ResidentAttribute);
    	Gender enumGender = Gender.Any;
		ResidentDataProvider provider = new ResidentDataProvider();
		if(props.containsKey("Gender")) {
			enumGender = Gender.valueOf( props.get("Gender").toString()); //Gender.valueOf(residentRequestDto.getGender());
		}
		provider.addCondition(ResidentAttribute.RA_Count, count);
		
		if(props.containsKey("Age")) {
			
			provider.addCondition(ResidentAttribute.RA_Age, ResidentAttribute.valueOf(props.get("Age").toString()));
		}
		else
			provider.addCondition(ResidentAttribute.RA_Age, ResidentAttribute.RA_Adult);
		
		if(props.containsKey("SkipGaurdian")) {
			provider.addCondition(ResidentAttribute.RA_SKipGaurdian, props.get("SkipGaurdian"));
		}
		provider.addCondition(ResidentAttribute.RA_Gender, enumGender);
		
		String primaryLanguage = "eng";
		if(props.containsKey("PrimaryLanguage")) {
			primaryLanguage = props.get("PrimaryLanguage").toString();
		}

		provider.addCondition(ResidentAttribute.RA_PRIMARAY_LANG, primaryLanguage);

		if(props.containsKey("SecondaryLanguage")) {
			provider.addCondition(ResidentAttribute.RA_SECONDARY_LANG, props.get("SecondaryLanguage").toString());
		}
		if(props.containsKey("Finger")) {
			provider.addCondition(ResidentAttribute.RA_Finger, Boolean.parseBoolean(props.get("Finger").toString()));
		}
		if(props.containsKey("Iris")) {
			provider.addCondition(ResidentAttribute.RA_Iris, Boolean.parseBoolean(props.get("Iris").toString()));
		}
		
		logger.info("before Genrate");
		List<ResidentModel> lst = provider.generate();
		logger.info("After Genrate");
		
		//ObjectMapper Obj = new ObjectMapper();
		JSONArray outIds = new JSONArray();
		
		try {
			String tmpDir;
			
			tmpDir = Files.createTempDirectory("residents_").toFile().getAbsolutePath();
			
			for(ResidentModel r: lst) {
				String jsonStr = r.toJSONString();
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
    public JSONObject makePacketAndSync(String preregId, String templateLocation, String contextKey) throws Exception {
    	logger.info("makePacketAndSync for PRID : {}", preregId);

    	String location = preregSyncService.downloadPreregPacket( preregId, contextKey);
        logger.info("Downloaded the prereg packet in {} ", location);
        File targetDirectory = Path.of(preregSyncService.getWorkDirectory(), preregId).toFile();
        if(!targetDirectory.exists()  && !targetDirectory.mkdir())
            throw new Exception("Failed to create target directory ! PRID : " + preregId);

        if(!zipUtils.unzip(location, targetDirectory.getAbsolutePath()))
            throw new Exception("Failed to unzip pre-reg packet >> " + preregId);

        Path idJsonPath = Path.of(targetDirectory.getAbsolutePath(), "ID.json");

        logger.info("Unzipped the prereg packet {}, ID.json exists : {}", preregId, idJsonPath.toFile().exists());

        String packetPath = packetMakerService.createContainer(idJsonPath.toString(),templateLocation,null,null, contextKey);

        logger.info("Packet created : {}", packetPath);

        String response = packetSyncService.syncPacketRid(packetPath, "dummy", "APPROVED",
                "dummy", null, contextKey);

        logger.info("RID Sync response : {}", response);
    	JSONObject functionResponse = new JSONObject();
    	JSONObject nobj = new JSONObject();
    
        JSONArray packets =  new JSONArray(response);
        if(packets.length() > 0) {
        	JSONObject resp = (JSONObject) packets.get(0);
        	if(resp.getString("status").equals("SUCCESS")) {
        	//RID Sync response : [{"registrationId":"10010100241000120201214134111","status":"SUCCESS"}]
        		String rid = resp.getString("registrationId");
        		response =  packetSyncService.uploadPacket(packetPath, contextKey);
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
                                String supervisorStatus, String supervisorComment, String proc,String contextKey) throws Exception {
        
    	if(contextKey != null && !contextKey.equals("")) {
    		
    		Properties props = contextUtils.loadServerContext(contextKey);
    		props.forEach((k,v)->{
    			if(k.toString().equals("mosip.test.packet.syncapi")) {
    				syncapi = v.toString();
    			}
    			else
    			if(k.toString().equals("mosip.test.regclient.machineid")) {
    				machineId = v.toString();
    			}
    			else
    			if(k.toString().equals("mosip.test.primary.langcode")) {
        			primaryLangCode = v.toString();
        		}
    			else
    			if(k.toString().equals("mosip.test.regclient.centerid")) {
        			centerId = v.toString();
        		}
    			else
        		if(k.toString().equals("mosip.test.baseurl")) {
            		baseUrl = v.toString();
            	}	
    			
    		});
    	}
    	Path container = Path.of(containerFile);
        String rid = container.getName(container.getNameCount()-1).toString().replace(".zip", "");
        if(proc !=null && !proc.equals(""))
        	process = proc;
        logger.info("Syncing data for RID : {}", rid);
        logger.info("Syncing data: process:", process);

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
        wrapper.put("requesttime", APIRequestUtil.getUTCDateTime(LocalDateTime.now(ZoneOffset.UTC)));
        wrapper.put("version", "1.0");
        wrapper.put("request", list);

        String packetCreatedDateTime = rid.substring(rid.length() - 14);
        String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
                + packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);
        LocalDateTime timestamp = LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

        String requestBody = Base64.encodeBase64URLSafeString(
                cryptoUtil.encrypt(wrapper.toString().getBytes("UTF-8"),
                centerId + UNDERSCORE + machineId, timestamp, contextKey) );

        JSONArray response = apiRequestUtil.syncRid(baseUrl,baseUrl+syncapi, requestBody, APIRequestUtil.getUTCDateTime(timestamp));

        return response.toString();
    }

    public String uploadPacket(String path, String contextKey) throws Exception {
    	
    	if(contextKey != null && !contextKey.equals("")) {
    		
    		Properties props = contextUtils.loadServerContext(contextKey);
    		props.forEach((k,v)->{
    			if(k.toString().equals("mosip.test.packet.uploadapi")) {
    		
    				uploadapi = v.toString();
    				
    			}
    			else
            		if(k.toString().equals("mosip.test.baseurl")) {
                		baseUrl = v.toString();
                	}	
    		});
    	}
        JSONObject response = apiRequestUtil.uploadFile(baseUrl, baseUrl+uploadapi, path);
        return response.toString();
    }

    public String preRegisterResident(List<String> personaFilePath, String contextKey) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	
    	loadServerContextProperties(contextKey);
    	
    	for(String path: personaFilePath) {
    		ResidentModel resident = readPersona(path);
    		String response = PreRegistrationSteps.postApplication(resident);
    		//preregid
    		saveRegIDMap(response, path);
    		builder.append(response);
    	}
    	return builder.toString();
    }
    void saveRegIDMap(String preRegId, String personaFilePath) {
    	
    	Properties p=new Properties();
    	try {
    		FileReader reader=new FileReader(preRegMapFile);  
    		p.load(reader);
    	
    	}catch (IOException e) {
			// TODO: handle exception
    		logger.error("saveRegIDMap " + e.getMessage());
		}
    	p.put(preRegId,  personaFilePath);
    	try {
        	
    		p.store(new FileWriter(preRegMapFile),"PreRegID to persona mapping file");  
    	}catch (IOException e) {
    		logger.error("saveRegIDMap " + e.getMessage());
		}
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
    public String requestOtp(List<String> personaFilePath, String to, String contextKey) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	
    	loadServerContextProperties(contextKey);
    	
    	for(String path: personaFilePath) {
    		ResidentModel resident = readPersona(path);
    		ResidentPreRegistration preReg = new ResidentPreRegistration(resident);
    		preReg.sendOtpTo(to);
    		
    	}
    	return builder.toString();
    }
    public String verifyOtp(String personaFilePath, String to, String otp, String contextKey) throws IOException {
    
    	loadServerContextProperties(contextKey);
    	ResidentModel resident = readPersona(personaFilePath);
    	ResidentPreRegistration preReg = new ResidentPreRegistration(resident);
    	
    	preReg.fetchOtp();
    	return preReg.verifyOtp(to,otp);
    		
    }
    public String bookAppointment( String preRegID,int nthSlot, String contextKey) {
   	
    String retVal= "{\"Failed\"}";
    Boolean bBooked = false;
    
    loadServerContextProperties(contextKey);
    
    String base = VariableManager.getVariableValue("urlBase").toString().trim();
	String api = VariableManager.getVariableValue(VariableManager.NS_PREREG, "appointmentslots").toString().trim();
	String centerId = VariableManager.getVariableValue(VariableManager.NS_REGCLIENT, "centerId").toString().trim();
	logger.info("BookAppointment:" + base +","+ api + ","+centerId);
	
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
   
    public String uploadDocuments(String personaFilePath, String preregId, String contextKey) throws IOException {
    
    	String response = "";
    	
    	loadServerContextProperties(contextKey);
    	
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
    public String createPackets(List<String> personaFilePaths, String process, String outDir, String contextKey) throws IOException {


    	Path packetDir = null;
    	JSONArray packetPaths = new JSONArray();
    	
    	logger.info("createPacket->outDir:" + outDir);

    	
    	loadServerContextProperties(contextKey);
    	
    	if(outDir == null || outDir.trim().equals("")) {
    		packetDir = Files.createTempDirectory("packets_");
    	}
    	else
    	{
    		packetDir = Paths.get(outDir);
    	}
    	if(!packetDir.toFile().exists()) {
    		packetDir.toFile().createNewFile();
    	}
    	PacketTemplateProvider packetTemplateProvider = new PacketTemplateProvider();
    	
    	for(String path: personaFilePaths) {
    		ResidentModel resident = readPersona(path);
    		String packetPath = packetDir.toString()+File.separator + resident.getId();
    		
    		
    		packetTemplateProvider.generate("registration_client", process, resident, packetPath);
    		JSONObject obj = new JSONObject();
    		obj.put("id",resident.getId());
    		obj.put("path", packetPath);
    		logger.info("createPacket:" + packetPath);
    		packetPaths.put(obj);
    		
    		
    	}
    	JSONObject response = new JSONObject();
    	response.put("packets", packetPaths);
     	return response.toString();
    }
    public String preRegToRegister( String templatePath, String preRegId, String contextKey) throws Exception {
  
    	return makePacketAndSync(preRegId, templatePath, contextKey).toString();
  		
  	  
    }
    public String updateResidentData(Hashtable<PersonaRequestType, Properties> hashtable , String uin, String rid) throws IOException {
    	Properties list = hashtable.get(PersonaRequestType.PR_ResidentList);
    	
    	String filePathResident =null;
    	String filePathParent = null;
    	ResidentModel persona = null;
    	ResidentModel guardian = null;
    	
    	for(Object key: list.keySet()) {
    		String keyS = key.toString().toLowerCase();
    		if(keyS.startsWith("uin")) {
    			filePathResident = list.get(key).toString();
    			persona = readPersona(filePathResident);
        		persona.setUIN(uin);
    		}
    		else
    		if(keyS.toString().startsWith("rid")) {
    			filePathResident = list.get(key).toString();
    			persona = readPersona(filePathResident);
    			persona.setRID(rid);
    		}
    		else
        	if(keyS.toString().startsWith("child")) {
        		filePathResident = list.get(key).toString();
        		persona = readPersona(filePathResident);
        	}
    		else
    		if(keyS.startsWith("guardian")) {
    			filePathParent = list.get(key).toString();
    			guardian = readPersona(filePathParent);
    		}   		
    	}
    	if(guardian != null)
    		persona.setGuardian(guardian);
    	
    	Files.write (Paths.get(filePathResident), persona.toJSONString().getBytes());
    	return "{\"response\":\"SUCCESS\"}";
    }
    ResidentModel readPersona(String filePath) throws IOException {
    	
    	ObjectMapper mapper = new ObjectMapper();
    	//mapper.registerModule(new SimpleModule().addDeserializer(Pair.class,new PairDeserializer()));
    //	mapper.registerModule(new SimpleModule().addSerializer(Pair.class, new PairSerializer()));
    	byte[] bytes = Files.readAllBytes(Path.of(filePath));
		return mapper.readValue(bytes, ResidentModel.class);
		
    }

 
}
