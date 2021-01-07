package org.mosip.dataprovider;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.mosip.dataprovider.util.DataProviderConstants;
import org.mosip.dataprovider.util.Gender;


public class PhotoProvider {
	static String Photo_File_Format = "/photo_%02d.jpg";
	
	static String getPhoto(int idx, String gender) {
		String encodedImage="";
		try {
			String photoFile = String.format(Photo_File_Format, idx);
			BufferedImage img = ImageIO.read(new File(DataProviderConstants.RESOURCE+"Photos/" + gender.toLowerCase() + photoFile));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", baos);
			baos.flush();
		
			encodedImage = Base64.getEncoder().encodeToString(baos.toByteArray());
			baos.close();
		//	encodedImage = java.net.URLEncoder.encode(encodedImage, "ISO-8859-1");
		} catch (IOException e) {
			
			e.printStackTrace();
		}             
		return encodedImage;
	}
	static void splitImages() {
		///125 x129
		try {
			final BufferedImage source = ImageIO.read(new File(DataProviderConstants.RESOURCE+"Photos/female/celebrities.jpg"));
			int idx =0;
			for (int y = 0; y < source.getHeight()-129; y += 129) {
				for (int x = 0; x < source.getWidth()-125; x += 125) {
					ImageIO.write(source.getSubimage(x, y, 125, 129), "jpg", new File(DataProviderConstants.RESOURCE+"Photos/female/photo_" + idx++ + ".jpg"));
				}
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String [] args) {
		splitImages();
		String strImg = getPhoto(35,Gender.Male.name());
		System.out.println(strImg);
		
	}
}
