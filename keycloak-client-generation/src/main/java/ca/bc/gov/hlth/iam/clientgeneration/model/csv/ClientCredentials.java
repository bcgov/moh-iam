package ca.bc.gov.hlth.iam.clientgeneration.model.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class ClientCredentials {

	@CsvBindByPosition(position = 0)
	@CsvBindByName(column = "Client Id", required = true)
	private String clientId;
	
	@CsvBindByPosition(position = 1)
	@CsvBindByName(column = "Cert Filename", required = true)
	private String certFilename;
	
	@CsvBindByPosition(position = 2)
	@CsvBindByName(column = "Cert Alias", required = true)
	private String certAlias;
	
	@CsvBindByPosition(position = 3)
	@CsvBindByName(column = "Key Password", required = true)
	private String keyPassword;
	
	@CsvBindByPosition(position = 4)
	@CsvBindByName(column = "Store Password", required = true)
	private String storePassword;
	
	@CsvBindByPosition(position = 5)
	@CsvBindByName(column = "Valid From Date", required = true)
	private String validFromDate;

	@CsvBindByPosition(position = 6)
	@CsvBindByName(column = "Expirty Date", required = true)
	private String expirtyDate;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getCertFilename() {
		return certFilename;
	}

	public void setCertFilename(String certFilename) {
		this.certFilename = certFilename;
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
	
	public String getValidFromDate() {
		return validFromDate;
	}

	public void setValidFromDate(String validFromDate) {
		this.validFromDate = validFromDate;
	}

	public String getExpirtyDate() {
		return expirtyDate;
	}

	public void setExpirtyDate(String expirtyDate) {
		this.expirtyDate = expirtyDate;
	}

	@Override
	public String toString() {
		return "ClientCredentials [clientId=" + clientId + ", certFilename=" + certFilename + ", certAlias=" + certAlias
				+ ", keyPassword=" + keyPassword + ", storePassword=" + storePassword + ", validFromDate="
				+ validFromDate + ", expirtyDate=" + expirtyDate + "]";
	}

}
