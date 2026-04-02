package nw.zero;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String githubID;

	@Column(unique = true)
	private String username;

	@Column
	private int balance = 100;

	public Long getId() { return id; }
	public String getGithubID() { return githubID; }
	public String getUsername() { return username; }
	public int getBalance() { return balance; }

	public void setGithubID(String githubID) { this.githubID = githubID; }
	public void setUsername(String username) { this.username = username; }
	public void setBalance(int balance) { this.balance = balance; }
}