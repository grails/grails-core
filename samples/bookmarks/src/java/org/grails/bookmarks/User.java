package org.grails.bookmarks;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity         
@Table(name="user_table")
public class User {
	public enum Role {	ROLE_ANONYMOUS,
						ROLE_GENERAL_USER,
						ROLE_ADMIN,
						ROLE_SUBSCRIBER,
						ROLE_THIRD_PART_INTEGRATION};
	
	private Long id;
	private String login;
	private String password;
	private String firstName;
	private String lastName;
	private String email;
	private String role = Role.ROLE_GENERAL_USER.toString();
	
	@Id	
	@Column(name="user_id")
	@GeneratedValue
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}	
	@Column(nullable=false,unique=true,length=10)
	public String getLogin() {
		return login;
	}	
	public void setLogin(String login) {
		this.login = login;
	}
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	@Column(name="u_first_name")
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name="u_last_name")
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	@Column(name="u_pwd")
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = Role.valueOf(role).toString();
	}
	
	
}
