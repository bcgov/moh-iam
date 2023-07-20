package ca.bc.gov.hlth.iam.dataloader.service;

/**
 * List of allowed types of usernames handled by the script. Can be added to if a new valid username type is required. 
 *
 *		IDIR: username@idir
 *		Fraser Health: sfhr\\username
 *		Interior Health: iha\\username
 *		Northern Health: nirhb\\username
 *		Provincial Health: phsabc\\username
 *		Vancouver Coastal Health: vch\\username or vrhb\\username
 *
 */
public enum UsernameTypeEnum {

	IDIR("@idir", false),		// IDIR: username@idir
	FHR("sfhr\\", true),		// Fraser Health: sfhr\\username
	IHA("iha\\", true),			// Interior Health: iha\\username
	NIRHB("nirhb\\", true),		// Northern Health: nirhb\\username
	PHSA("phsabc\\", true),		// Provincial Health: phsabc\\username
	VCH("vch\\", true),			// Vancouver Coastal Health: vch\\username
	VRHB("vrhb\\", true),		// Vancouver Coastal Health: vrhb\\username
	NONE("", false);			// No prefix/suffix required, usernames are complete.
	
	private String value;
	
	private boolean isPrefix;

	private UsernameTypeEnum(String value, boolean isPrefix) {
		this.value = value;
		this.isPrefix = isPrefix;
	}

	public String getValue() {
		return value;
	}

	public boolean isPrefix() {
		return isPrefix;
	}
	
}
