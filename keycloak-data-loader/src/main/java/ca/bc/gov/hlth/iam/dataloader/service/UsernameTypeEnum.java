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
	BCPROVIDER("@bcp", false), // BC Provider: username@bcp
	BCSC("@bcsc", false), // BCSC: username@bcsc
	BCEID_BUSINESS("@bceid_business", false), // BCeID Business: username@bceid_business
	FNHA("@fnha", false), // FNHA: username@fnha
	IDIR("@idir", false),		// IDIR: username@idir
	FHA("sfhr\\", true),		// Fraser Health: sfhr\\username
	IHA("iha\\", true),			// Interior Health: iha\\username
	NHA("nirhb\\", true),		// Northern Health: nirhb\\username AKA NHA
	PHSA("phsabc\\", true),		// Provincial Health: phsabc\\username
	VCH("vch\\", true),			// Vancouver Coastal Health: vch\\username
	VRHB("vrhb\\", true),		// Vancouver Coastal Health: vrhb\\username
	PHC("infosys\\", true),		// Providence Health Care: infosys\\username
	VIHA("viha\\", true),		// Island Health: viha\\username
	
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
