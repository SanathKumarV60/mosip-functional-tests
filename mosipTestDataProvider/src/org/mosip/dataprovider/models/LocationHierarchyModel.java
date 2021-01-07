package org.mosip.dataprovider.models;

import java.io.Serializable;

public class LocationHierarchyModel   implements Serializable{
	
	 private static final long serialVersionUID = 1L;
	 int hierarchyLevel;


	public int getHierarchyLevel() {
		return hierarchyLevel;
	}
	public void setHierarchyLevel(int hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}
	public String getHierarchyLevelName() {
		return hierarchyLevelName;
	}
	public void setHierarchyLevelName(String hierarchyLevelName) {
		this.hierarchyLevelName = hierarchyLevelName;
	}
	public String getLangCode() {
		return langCode;
	}
	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}
	public Boolean getIsActive() {
		return isActive;
	}
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}
	String hierarchyLevelName;
	String langCode;
	Boolean isActive;
}
