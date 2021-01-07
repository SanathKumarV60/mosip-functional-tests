package org.mosip.dataprovider;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mosip.dataprovider.models.MosipDocCategoryModel;
import org.mosip.dataprovider.models.MosipDocTypeModel;
import org.mosip.dataprovider.models.MosipDocument;
import org.mosip.dataprovider.models.MosipLocationModel;
import org.mosip.dataprovider.models.ResidentModel;
import org.mosip.dataprovider.preparation.MosipMasterData;
import org.mosip.dataprovider.util.DataProviderConstants;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

public class DocumentProvider {

	//public static String parseThymeleafTemplate(String photo,  String name,String date, String address) {
	  
	 static String parseThymeleafTemplateDriverLicense(String photo,  String name,String date, String address) {
		  
		FileTemplateResolver templateResolver = new FileTemplateResolver();//ClassLoaderTemplateResolver();
		templateResolver.setPrefix(DataProviderConstants.RESOURCE+DataProviderConstants.DOC_TEMPLATE_PATH);
	    templateResolver.setSuffix(".html");
	    templateResolver.setTemplateMode(TemplateMode.HTML);

	    TemplateEngine templateEngine = new TemplateEngine();
	    templateEngine.setTemplateResolver(templateResolver);

	    Context context = new Context();
	    context.setVariable("myphoto", photo);
	    context.setVariable("date", date);
	    context.setVariable("name", name);
	    
	    context.setVariable("address", address);
	    
	    return templateEngine.process("driverlicense", context);
	}
	 static String parseThymeleafTemplatePassport(String photo,  String name,String date, String address) {
		  
		FileTemplateResolver templateResolver = new FileTemplateResolver();//ClassLoaderTemplateResolver();
		templateResolver.setPrefix(DataProviderConstants.RESOURCE+DataProviderConstants.DOC_TEMPLATE_PATH);
	    templateResolver.setSuffix(".html");
	    templateResolver.setTemplateMode(TemplateMode.HTML);

	    TemplateEngine templateEngine = new TemplateEngine();
	    templateEngine.setTemplateResolver(templateResolver);

	    Context context = new Context();
	    context.setVariable("myphoto", photo);
	    context.setVariable("date", date);
	    context.setVariable("name", name);
	    
	    context.setVariable("address", address);
	    
	    return templateEngine.process("passport", context);
	}

	 static void generatePdfFromHtml(String html, File outFile) throws DocumentException, IOException {

	    OutputStream outputStream = new FileOutputStream(outFile);

	    ITextRenderer renderer = new ITextRenderer();
	    renderer.setDocumentFromString(html);
	    renderer.layout();
	    renderer.createPDF(outputStream);

	    outputStream.close();
	}
	public static List<MosipDocument>  generateDocuments(ResidentModel res) throws DocumentException, IOException, ParseException{
	

		List<String> docs = new ArrayList<String>();
		
		String locAddr = "";
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
		Date dob = df.parse(res.getDob());
		
		int age = new Date().getYear() - dob.getYear();
		List<MosipLocationModel> locs = res.getLocation();

		for( MosipLocationModel loc :locs){
			locAddr = locAddr +" "+ loc.getName();
		}
		Date datelic = dob;
		datelic.setYear( datelic.getYear() + age + 5);
		String html = parseThymeleafTemplatePassport(
				res.getBiometric().getEncodedPhoto(),
				res.getName().getFirstName() + " " +res.getName().getSurName(),
				datelic.toLocaleString(),
				locAddr
		);
	
		File docFile = File.createTempFile("Passport_", ".pdf");
		docs.add(docFile.getAbsolutePath());
		
		generatePdfFromHtml(html,docFile);
		
		docFile = File.createTempFile("DrivingLic_", ".pdf");
		docs.add(docFile.getAbsolutePath());
		
		html = parseThymeleafTemplateDriverLicense(
				res.getBiometric().getEncodedPhoto(),
				res.getName().getFirstName() + " " +res.getName().getSurName(),
				datelic.toLocaleString(),
				locAddr
		);
		generatePdfFromHtml(html,docFile);
		
		/*
		 * docs as per template
		 */
		List<MosipDocument> lstDocs = new ArrayList<MosipDocument>();
		
		List<MosipDocCategoryModel> docCats =MosipMasterData.getDocumentCategories();
		for(MosipDocCategoryModel cat: docCats) {
			if(cat.getIsActive()) {
	//			System.out.println(cat.toJSONString());
				MosipDocument doc = new MosipDocument();
				doc.setDcoCategoryName(cat.getName());
				doc.setDocCategoryCode(cat.getCode());
				doc.setDocCategoryLang(cat.getLangCode());
				List<MosipDocTypeModel> docTypes = new ArrayList<MosipDocTypeModel>();
				List<String> catDocs = new ArrayList<String> ();
				doc.setType(docTypes);
				doc.setDocs(catDocs);
				List<MosipDocTypeModel> allDocTypes= MosipMasterData.getDocumentTypes(cat.getCode(),cat.getLangCode());
				int i=0;
				if(allDocTypes != null) {
					for(MosipDocTypeModel dt: allDocTypes) {
					//System.out.println(dt.toJSONString());
						if(dt.getIsActive()) {
							docTypes.add(dt);
							catDocs.add( docs.get( i % docs.size()));
							i++;
						}
					}
					lstDocs.add(doc);
				}
			}
			
		}
		 
	
		return lstDocs;
	}
	
	public static void main(String[] args) {
		
		
		String photo = PhotoProvider.getPhoto(1, "female");
		String html = parseThymeleafTemplatePassport(photo,"Angel","01/12/2025", "Some where on this planet , on earth");
		try {
			generatePdfFromHtml(html,new File("out.pdf"));
		} catch (DocumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
