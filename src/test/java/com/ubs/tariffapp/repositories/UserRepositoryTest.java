// package com.ubs.tariffapp.repositories;

// import java.time.LocalDateTime;

// import static org.assertj.core.api.Assertions.assertThat;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

// import com.ubs.tariffapp.models.User;

// // Use "mvn test" or the Test Runner extension to run test
// // No output should show if the test passes
// // Data also doesn't persist btw

// // TODO: Add more robust tests

// @DataJpaTest
// public class UserRepositoryTest {
//     private static final String USER_ID = "some-user-id-123456789";
//     private static final String NAME = "Alice";
//     private static final String EMAIL = "alice@example.smu.sg";

//     @Autowired
//     private UserRepository userRepository;

//     @Test
//     void testSaveAndFindUser() {
//     User user = new User(USER_ID, "user", NAME, EMAIL, LocalDateTime.now());
//     userRepository.save(user);

//     User found = userRepository.findById(USER_ID).orElse(null);
//     assertThat(found).isNotNull();
//     assertThat(found.getName()).isEqualTo(NAME);
//     }
// }