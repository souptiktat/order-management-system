package com.apple.order.controller;

import com.apple.order.dto.OrderRequest;
import com.apple.order.entity.Order;
import com.apple.order.exception.ErrorResponse;
import com.apple.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Operations related to Order Management")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Create a new Order",
            description = "Creates a new order for a user after validating business rules, payment conditions, and credit limits."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User is blocked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderRequest request) {

        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Operation(summary = "Get Order by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @Operation(summary = "Update entire Order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Cannot update shipped order"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Order> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {

        return ResponseEntity.ok(orderService.update(id, request));
    }

    @Operation(summary = "Partially update Order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order partially updated"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Order> partialUpdate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        return ResponseEntity.ok(orderService.patch(id, updates));
    }

    @Operation(summary = "Delete Order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete shipped order")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}