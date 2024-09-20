package ca.bc.gov.hlth.iam.dataloader.service;

public enum EnvironmentEnum {

	DEV("dev", "BULK-USER-UPLOAD-CLIENT-SECRET-DEV"),
	TEST("test", "BULK-USER-UPLOAD-CLIENT-SECRET-TEST"),
	PROD("prod", "BULK-USER-UPLOAD-CLIENT-SECRET-PROD");

	private String value;
	
	private String clientSecret;

	private EnvironmentEnum(String value, String clientSecret) {
		this.value = value;
		this.clientSecret = clientSecret;
	}

	public String getValue() {
		return value;
	}

	public String getClientSecret() {
		return clientSecret;
	}

}
