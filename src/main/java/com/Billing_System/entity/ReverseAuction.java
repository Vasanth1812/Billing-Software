package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reverse Auction Entity.
 * Enterprise tells vendors what they need, and vendors bid the price DOWN.
 */
@Entity
@Table(name = "reverse_auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReverseAuction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "auction_number", nullable = false, unique = true, length = 20)
    private String auctionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "ceiling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal ceilingPrice; // Maximum we are willing to pay per unit

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // SCHEDULED, ACTIVE, CLOSED, CANCELLED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "winning_bid_id")
    private UUID winningBidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
