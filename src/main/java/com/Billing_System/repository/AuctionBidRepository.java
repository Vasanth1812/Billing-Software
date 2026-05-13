package com.Billing_System.repository;

import com.Billing_System.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, UUID> {
    List<AuctionBid> findByAuctionIdOrderByBidAmountAsc(UUID auctionId);
    
    // Find the current lowest bid for a given auction
    @Query(value = "SELECT * FROM auction_bids WHERE auction_id = ?1 ORDER BY bid_amount ASC LIMIT 1", nativeQuery = true)
    Optional<AuctionBid> findLowestBidForAuction(UUID auctionId);
}
