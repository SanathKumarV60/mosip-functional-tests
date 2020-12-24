package io.mosip.test.packetcreator.mosippacketcreator.service;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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


}
