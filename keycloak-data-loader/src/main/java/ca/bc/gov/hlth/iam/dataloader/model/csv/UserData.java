package model.csv;

import com.opencsv.bean.CsvBindByPosition;

public class UserData {

	@CsvBindByPosition(position = 0)
	private String username;
	
	@CsvBindByPosition(position = 1)
	private String roles;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "UserData [username=" + username + ", roles=" + roles + "]";
	}

}
