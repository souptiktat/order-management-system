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
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;

    private Order createdOrder;
    private Order shippedOrder;

    @BeforeEach
    void setUp() {

        user1 = userRepository.save(
                User.builder()
                        .name("John")
                        .email("john@mail.com")
                        .password("pass")
                        .creditLimit(1000.0)
                        .country("USA")
                        .blocked(false)
                        .build()
        );

        user2 = userRepository.save(
                User.builder()
                        .name("Alice")
                        .email("alice@mail.com")
                        .password("pass")
                        .creditLimit(2000.0)
                        .country("USA")
                        .blocked(false)
                        .build()
        );

        createdOrder = orderRepository.save(
                Order.builder()
                        .productName("Laptop")
                        .amount(500.0)
                        .status(Order.OrderStatus.CREATED)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(7))   // ✅ REQUIRED
                        .paymentType(Order.PaymentType.CREDIT_CARD)
                        .cardNumber("1234567890123456")
                        .upiId(null)
                        .user(user1)
                        .build()
        );

        shippedOrder = orderRepository.save(
                Order.builder()
                        .productName("Phone")
                        .amount(200.0)
                        .status(Order.OrderStatus.SHIPPED)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(5))   // ✅ REQUIRED
                        .paymentType(Order.PaymentType.UPI)
                        .cardNumber(null)
                        .upiId("john@upi")
                        .user(user1)
                        .build()
        );
    }

    // ==========================================================
    // findByUserId
    // ==========================================================

    @Test
    @DisplayName("Should return orders for given user ID")
    void findByUserId_success() {
        List<Order> orders = orderRepository.findByUserId(user1.getId());

        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when user has no orders")
    void findByUserId_noOrders() {
        List<Order> orders = orderRepository.findByUserId(user2.getId());

        assertThat(orders).isEmpty();
    }

    // ==========================================================
    // existsByIdAndStatus
    // ==========================================================

    @Test
    void existsByIdAndStatus_true() {
        boolean exists = orderRepository.existsByIdAndStatus(
                createdOrder.getId(),
                Order.OrderStatus.CREATED
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByIdAndStatus_false() {
        boolean exists = orderRepository.existsByIdAndStatus(
                createdOrder.getId(),
                Order.OrderStatus.SHIPPED
        );

        assertThat(exists).isFalse();
    }

    // ==========================================================
    // findByStatus
    // ==========================================================

    @Test
    void findByStatus_created() {
        List<Order> orders = orderRepository.findByStatus(Order.OrderStatus.CREATED);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getProductName()).isEqualTo("Laptop");
    }

    @Test
    void findByStatus_noMatch() {
        List<Order> orders = orderRepository.findByStatus(Order.OrderStatus.CANCELLED);

        assertThat(orders).isEmpty();
    }

    // ==========================================================
    // findOrderWithUser (JOIN FETCH)
    // ==========================================================

    @Test
    void findOrderWithUser_success() {
        Optional<Order> optionalOrder =
                orderRepository.findOrderWithUser(createdOrder.getId());

        assertThat(optionalOrder).isPresent();

        Order order = optionalOrder.get();

        assertThat(order.getUser()).isNotNull();
        assertThat(order.getUser().getEmail()).isEqualTo("john@mail.com");
    }

    @Test
    void findOrderWithUser_notFound() {
        Optional<Order> optionalOrder =
                orderRepository.findOrderWithUser(999L);

        assertThat(optionalOrder).isEmpty();
    }

    // ==========================================================
    // existsActiveProductForUser
    // ==========================================================

    @Test
    void existsActiveProductForUser_true() {
        boolean exists = orderRepository.existsActiveProductForUser(
                user1.getId(),
                "Laptop"
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsActiveProductForUser_false_differentProduct() {
        boolean exists = orderRepository.existsActiveProductForUser(
                user1.getId(),
                "Tablet"
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsActiveProductForUser_false_differentStatus() {
        boolean exists = orderRepository.existsActiveProductForUser(
                user1.getId(),
                "Phone"   // This order is SHIPPED, not CREATED
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsActiveProductForUser_false_noOrders() {
        boolean exists = orderRepository.existsActiveProductForUser(
                user2.getId(),
                "Laptop"
        );

        assertThat(exists).isFalse();
    }
}