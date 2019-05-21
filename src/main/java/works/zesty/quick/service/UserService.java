package works.zesty.quick.service;

import java.util.List;

public interface UserService {

	User create(User user);
	
	User select(Long id);
	
	User findByEmail(String email);
	
	List<User> findByName(String firstName);
	
	class User {
		Long id;
		String firstName;
		String emailAddr;
	}
}
