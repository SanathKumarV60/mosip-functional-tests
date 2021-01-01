package io.mosip.test.packetcreator.mosippacketcreator.controller;

import java.io.IOException;
import java.util.Base64;

import io.mosip.test.packetcreator.mosippacketcreator.dto.PacketCreateDto;
import io.mosip.test.packetcreator.mosippacketcreator.dto.PreRegisterRequestDto;
import io.mosip.test.packetcreator.mosippacketcreator.dto.ResidentRequestDto;
import io.mosip.test.packetcreator.mosippacketcreator.dto.SyncRidDto;
import io.mosip.test.packetcreator.mosippacketcreator.service.*;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.mosip.dataprovider.util.DataProviderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestDataController {

    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);
    
    @Value("${mosip.test.welcome}")
    private String welcomeMessage;

    @Value("${mosip.test.persona.configpath}")
	private String personaConfigPath;
    
    @Autowired
    PacketMakerService pkm;

    @Autowired
    PreregSyncService pss;

    @Autowired
    APIRequestUtil apiUtil;

    @Autowired
    CryptoUtil cryptoUtil;

    @Autowired
    PacketSyncService packetSyncService;

    @Autowired
    JobScheduler jobScheduler;

    @Autowired
    PacketJobService packetJobService;

    @PostMapping(value = "/packetcreator")
    public @ResponseBody String getTestData(@RequestBody PacketCreateDto packetCreateDto) {
        try{
            return pkm.createContainer(packetCreateDto.getIdJsonPath(), packetCreateDto.getTemplatePath());
        } catch (Exception ex){
             logger.error("", ex);
        }
        return "Failed!";
    }

    @GetMapping(value = "/auth")
    public @ResponseBody String getAPITestData() {
        return String.valueOf(apiUtil.initToken());
    }

    @GetMapping(value = "/sync")
    public @ResponseBody String syncPreregData() {
        try {
            pss.syncPrereg();
            return "All Done!";
        } catch (Exception exception) {
            logger.error("", exception);
            return exception.getMessage();
        }
    }
        
    @GetMapping(value = "/sync/{preregId}")
    public @ResponseBody String getPreregData(@PathVariable("preregId") String preregId){
        try{
            return pss.downloadPreregPacket(preregId);
        } catch(Exception exception){
            logger.error("", exception);
            return "Failed";
        }
    }

    @GetMapping(value = "/encrypt")
    public @ResponseBody String encryptData() throws Exception {
        return Base64.getUrlEncoder().encodeToString(cryptoUtil.encrypt("test".getBytes(), "referenceId"));
    }

    @PostMapping(value = "/ridsync")
    public @ResponseBody String syncRid(@RequestBody SyncRidDto syncRidDto) throws Exception {
        return packetSyncService.syncPacketRid(syncRidDto.getContainerPath(), syncRidDto.getName(),
                syncRidDto.getSupervisorStatus(), syncRidDto.getSupervisorComment());
    }

    @GetMapping(value = "/packetsync")
    public @ResponseBody String packetsync(String path) throws Exception {
        return packetSyncService.uploadPacket(path);
    }

    @GetMapping(value = "/startjob")
    public @ResponseBody String startJob() {
        String response = jobScheduler.scheduleRecurrently(()->packetJobService.execute(),
                Cron.every5minutes());
        return response;
    }
    @GetMapping(value = "/makepacketandsync/{preregId}")
    public @ResponseBody String makePacketAndSync(@PathVariable("preregId") String preregId) {

    	try{    	
    	
    		return packetSyncService.makePacketAndSync(preregId).toString();
    	
    	} catch (Exception ex){
             logger.error("makePacketAndSync", ex);
    	}
    	return "{Failed}";
    }
    @PostMapping(value = "/generateResident/{count}")
    public @ResponseBody String generateResidentData(@RequestBody ResidentRequestDto residentRequestDto, @PathVariable("count") int count) {

    	try{
    		logger.info("Persona Config Path="+ personaConfigPath );
    		if(personaConfigPath !=null && !personaConfigPath.equals("")) {
    			DataProviderConstants.RESOURCE = personaConfigPath;
    		}
    		logger.info("Resource Path="+ DataProviderConstants.RESOURCE );
    		logger.info("DOC_Template Path="+ DataProviderConstants.RESOURCE+DataProviderConstants.DOC_TEMPLATE_PATH);
    		return packetSyncService.generateResidentData(count,residentRequestDto).toString();
    	
    	} catch (Exception ex){
             logger.error("generateResidentData", ex);
    	}
    	return "{Failed}";
    }    
    @PostMapping(value = "/preregister/")
    public @ResponseBody String preRegisterResident(@RequestBody PreRegisterRequestDto preRegisterRequestDto) {

    	try{    	
    	
    		return packetSyncService.preRegisterResident(preRegisterRequestDto.getPersonaFilePath());
    	
    	} catch (Exception ex){
             logger.error("registerResident", ex);
    	}
    	return "{Failed}";
    }
    /*
     * to : email | mobile
     */
    @PostMapping(value = "/requestotp/{to}") 
    public @ResponseBody String requestOtp(@RequestBody PreRegisterRequestDto preRegisterRequestDto, @PathVariable("to") String to) {

    	try{    	
    	
    		return packetSyncService.requestOtp(preRegisterRequestDto.getPersonaFilePath(), to);
    	
    	} catch (Exception ex){
             logger.error("requestOtp", ex);
    	}
    	return "{Failed}";
    }
    
    @PostMapping(value = "/verifyotp/{to}")
    public @ResponseBody String verifyOtp(@RequestBody PreRegisterRequestDto preRegisterRequestDto,@PathVariable("to") String to) {

    	try{    	
    	
    		return packetSyncService.verifyOtp(preRegisterRequestDto.getPersonaFilePath().get(0), to, null);
    	
    	} catch (Exception ex){
             logger.error("verifyOtp", ex);
    	}
    	return "{Failed}";
    }
    /*
     * Book first nn th available slot
     */
    @PostMapping(value = "/bookappointment/{preregid}/{nthSlot}")
    public @ResponseBody String bookAppointment(@PathVariable("preregid") String preregId,@PathVariable("nthSlot") int  nthSlot) {

    	try{    	
    	
    		return packetSyncService.bookAppointment(preregId,nthSlot);
    	
    	} catch (Exception ex){
             logger.error("bookAppointment", ex);
    	}
    	return "{\"Failed\"}";
    }
    
    @PostMapping(value = "/documents/{preregid}")
    public @ResponseBody String uploadDocuments(@RequestBody PreRegisterRequestDto preRegisterRequestDto,@PathVariable("preregid") String preregId) {

    	try{    	
    	
    		return packetSyncService.uploadDocuments(preRegisterRequestDto.getPersonaFilePath().get(0),preregId);
    	
    	} catch (Exception ex){
             logger.error("uploadDocuments", ex);
    	}
    	return "{\"Failed\"}";
    }
    
    	  
    
}
