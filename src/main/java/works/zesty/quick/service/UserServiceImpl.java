package works.zesty.quick.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {

	private Map<Long, User> repo = new ConcurrentHashMap<>();
	private final AtomicLong idGen = new AtomicLong(0);

	@Override
	public User create(User user) {
		user.id = nextId();
		return repo.put(user.id, user);
	}

	@Override
	public User select(Long id) {
		return repo.get(id);
	}

	@Override
	public User findByEmail(String email) {
		return repo.values().stream().filter( user -> user.emailAddr.equals(email)).findFirst().orElse(null);
	}

	@Override
	public List<User> findByName(String firstName) {
		return repo.values().stream().filter( user -> user.firstName.equals(firstName)).collect(Collectors.toList());
	}
	
	private Long nextId() {
		return idGen.incrementAndGet();
	}
}
