package org.mosip.dataprovider.models;

import java.io.Serializable;

public class MosipLocationModel  implements Serializable{
	
	 private static final long serialVersionUID = 1L;
	String code;
	String langCode;
	String name;
	int hierarchyLevel;
	String hierarchyName;
	String parentLocCode;
	Boolean isActive;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getLangCode() {
		return langCode;
	}
	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getHierarchyLevel() {
		return hierarchyLevel;
	}
	public void setHierarchyLevel(int hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}
	public String getHierarchyName() {
		return hierarchyName;
	}
	public void setHierarchyName(String hierarchyName) {
		if(hierarchyName.equals("Postal Code"))
			hierarchyName ="postalCode";
		this.hierarchyName = hierarchyName;
	}
	public String getParentLocCode() {
		return parentLocCode;
	}
	public void setParentLocCode(String parentLocCode) {
		this.parentLocCode = parentLocCode;
	}
	public Boolean getIsActive() {
		return isActive;
	}
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
	
}
