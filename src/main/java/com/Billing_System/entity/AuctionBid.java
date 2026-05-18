package com.Billing_System.entity;

import com.Billing_System.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auction_bids")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ReverseAuction auction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "bid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bidAmount;

    @CreationTimestamp
    @Column(name = "bid_time", updatable = false, nullable = false)
    private LocalDateTime bidTime;

    @Column(name = "is_winning", nullable = false)
    private boolean isWinning = false;
}
