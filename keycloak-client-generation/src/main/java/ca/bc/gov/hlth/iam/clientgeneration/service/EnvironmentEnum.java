package ca.bc.gov.hlth.iam.clientgeneration.service;

public enum EnvironmentEnum {

	DEV("dev", "CLIENT_GENERATION_CLIENT_SECRET_DEV"),
	TEST("test", "CLIENT_GENERATION_CLIENT_SECRET_TEST"),
	PROD("prod", "CLIENT_GENERATION_CLIENT_SECRET_PROD");

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
