package ca.bc.gov.hlth.iam.clientgeneration.service;

public enum EnvironmentEnum {

	DEV("dev", "DATA_LOADER_USER_PASSWORD_DEV"),
	TEST("test", "DATA_LOADER_USER_PASSWORD_TEST"),
	PROD("prod", "DATA_LOADER_USER_PASSWORD_PROD");

	private String value;
	
	private String passwordKey;

	private EnvironmentEnum(String value, String passwordKey) {
		this.value = value;
		this.passwordKey = passwordKey;
	}

	public String getValue() {
		return value;
	}

	public String getPasswordKey() {
		return passwordKey;
	}

}
