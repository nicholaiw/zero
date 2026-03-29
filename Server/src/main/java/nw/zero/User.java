package nw.zero;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String googleId;

	@Column(unique = true, nullable = false)
	private String username;

	public Long getId()           { return id; }
	public String getGoogleId()   { return googleId; }
	public String getUsername()   { return username; }

	public void setGoogleId(String googleId) { this.googleId = googleId; }
	public void setUsername(String username) { this.username = username; }
}