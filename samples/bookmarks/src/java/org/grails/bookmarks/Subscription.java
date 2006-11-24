package org.grails.bookmarks;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Subscription {

	private Long id;
	private User user;
	private Tag tag;
	@Id
    @GeneratedValue			
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@ManyToOne
	public Tag getTag() {
		return tag;
	}
	public void setTag(Tag tag) {
		this.tag = tag;
	}
	@ManyToOne
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	   
	public String toString() {
		if(tag != null) {
			return tag.getName();
		}                   
		return super.toString();
	}
	
}
