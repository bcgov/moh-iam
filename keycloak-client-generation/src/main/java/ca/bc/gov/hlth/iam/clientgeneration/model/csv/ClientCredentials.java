package ca.bc.gov.hlth.iam.clientgeneration.model.csv;

import com.opencsv.bean.CsvBindByPosition;

public class ClientCredentials {

	@CsvBindByPosition(position = 0)
	private String clientId;
	
	@CsvBindByPosition(position = 1)
	private String certFileName;
	
	@CsvBindByPosition(position = 2)
	private String certAlias;
	
	@CsvBindByPosition(position = 3)
	private String keyPassword;
	
	@CsvBindByPosition(position = 4)
	private String storePassword;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getCertFileName() {
		return certFileName;
	}

	public void setCertFileName(String certFileName) {
		this.certFileName = certFileName;
	}

	public String getCertAlias() {
		return certAlias;
	}

	public void setCertAlias(String certAlias) {
		this.certAlias = certAlias;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public String getStorePassword() {
		return storePassword;
	}

	public void setStorePassword(String storePassword) {
		this.storePassword = storePassword;
	}

	@Override
	public String toString() {
		return "ClientCredentials [clientId=" + clientId + ", certFileName=" + certFileName + ", certAlias=" + certAlias
				+ ", keyPassword=" + keyPassword + ", storePassword=" + storePassword + "]";
	}
	
}
