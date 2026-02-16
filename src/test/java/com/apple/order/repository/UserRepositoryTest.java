package com.apple.order.repository;

import com.apple.order.entity.Order;
import com.apple.order.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User activeUser;
    private User blockedUser;

    @BeforeEach
    void setUp() {

        activeUser = userRepository.save(
                User.builder()
                        .name("John")
                        .email("john@mail.com")
                        .password("pass")
                        .creditLimit(1000.0)
                        .country("USA")
                        .blocked(false)
                        .build()
        );

        blockedUser = userRepository.save(
                User.builder()
                        .name("Alice")
                        .email("alice@mail.com")
                        .password("pass")
                        .creditLimit(2000.0)
                        .country("USA")
                        .blocked(true)
                        .build()
        );

        // Add orders for active user (for JOIN FETCH test)
        Order order1 = Order.builder()
                .productName("Laptop")
                .amount(500.0)
                .status(Order.OrderStatus.CREATED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .paymentType(Order.PaymentType.CREDIT_CARD)
                .cardNumber("1234567890123456")
                .user(activeUser)
                .build();

        entityManager.persist(order1);
        entityManager.flush();
        entityManager.clear();
    }

    // ==========================================================
    // existsByEmail
    // ==========================================================

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_true() {
        boolean exists = userRepository.existsByEmail("john@mail.com");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_false() {
        boolean exists = userRepository.existsByEmail("notfound@mail.com");
        assertThat(exists).isFalse();
    }

    // ==========================================================
    // findByEmail
    // ==========================================================

    @Test
    void findByEmail_success() {
        Optional<User> user = userRepository.findByEmail("john@mail.com");

        assertThat(user).isPresent();
        assertThat(user.get().getName()).isEqualTo("John");
    }

    @Test
    void findByEmail_notFound() {
        Optional<User> user = userRepository.findByEmail("x@mail.com");

        assertThat(user).isEmpty();
    }

    // ==========================================================
    // existsByIdAndBlockedFalse
    // ==========================================================

    @Test
    void existsByIdAndBlockedFalse_true() {
        boolean exists = userRepository.existsByIdAndBlockedFalse(activeUser.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByIdAndBlockedFalse_false_whenBlocked() {
        boolean exists = userRepository.existsByIdAndBlockedFalse(blockedUser.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void existsByIdAndBlockedFalse_false_whenNotFound() {
        boolean exists = userRepository.existsByIdAndBlockedFalse(999L);

        assertThat(exists).isFalse();
    }

    // ==========================================================
    // findUserWithOrders (JOIN FETCH)
    // ==========================================================

    @Test
    void findUserWithOrders_success() {
        Optional<User> userOpt =
                userRepository.findUserWithOrders(activeUser.getId());

        assertThat(userOpt).isPresent();

        User user = userOpt.get();
        List<Order> orders = user.getOrders();

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void findUserWithOrders_notFound() {
        Optional<User> userOpt =
                userRepository.findUserWithOrders(999L);

        assertThat(userOpt).isEmpty();
    }

    // ==========================================================
    // findCreditLimitIfActive
    // ==========================================================

    @Test
    void findCreditLimitIfActive_success() {
        Optional<Double> credit =
                userRepository.findCreditLimitIfActive(activeUser.getId());

        assertThat(credit).isPresent();
        assertThat(credit.get()).isEqualTo(1000.0);
    }

    @Test
    void findCreditLimitIfActive_blockedUser() {
        Optional<Double> credit =
                userRepository.findCreditLimitIfActive(blockedUser.getId());

        assertThat(credit).isEmpty();
    }

    @Test
    void findCreditLimitIfActive_notFound() {
        Optional<Double> credit =
                userRepository.findCreditLimitIfActive(999L);

        assertThat(credit).isEmpty();
    }
}