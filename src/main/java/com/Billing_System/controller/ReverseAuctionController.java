package com.Billing_System.controller;

import com.Billing_System.dto.AuctionBidDTO;
import com.Billing_System.entity.AuctionBid;
import com.Billing_System.entity.Product;
import com.Billing_System.entity.ReverseAuction;
import com.Billing_System.entity.User;
import com.Billing_System.repository.AuctionBidRepository;
import com.Billing_System.repository.ProductRepository;
import com.Billing_System.repository.ReverseAuctionRepository;
import com.Billing_System.repository.UserRepository;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
public class ReverseAuctionController {

    private final ReverseAuctionRepository auctionRepository;
    private final AuctionBidRepository bidRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- REST API: Create an Auction ---

    @PostMapping
    public ResponseEntity<ReverseAuction> createAuction(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID productId = UUID.fromString(payload.get("productId").toString());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        BigDecimal quantity = new BigDecimal(payload.get("quantity").toString());
        BigDecimal ceilingPrice = new BigDecimal(payload.get("ceilingPrice").toString());

        int durationMinutes = 120; // Default to 2 hours (120 minutes)
        if (payload.containsKey("durationMinutes") && payload.get("durationMinutes") != null) {
            try {
                durationMinutes = Integer.parseInt(payload.get("durationMinutes").toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid durationMinutes format: {}, falling back to 120", payload.get("durationMinutes"));
            }
        }

        ReverseAuction auction = ReverseAuction.builder()
                .auctionNumber("AUC-" + System.currentTimeMillis())
                .product(product)
                .quantity(quantity)
                .ceilingPrice(ceilingPrice)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusMinutes(durationMinutes))
                .status("ACTIVE")
                .createdBy(creator)
                .build();

        auction = auctionRepository.save(auction);
        log.info("Created Reverse Auction: {}", auction.getAuctionNumber());
        return ResponseEntity.ok(auction);
    }

    @GetMapping
    public ResponseEntity<List<ReverseAuction>> getActiveAuctions() {
        return ResponseEntity.ok(auctionRepository.findByStatus("ACTIVE"));
    }

    // --- WEBSOCKET API: Place a Bid ---

    /**
     * Vendors send bids to /app/auction/bid via WebSockets.
     * The server processes the bid, checks if it's the lowest, and broadcasts it to everyone.
     */
    @MessageMapping("/auction/bid")
    public void placeBid(@Payload AuctionBidDTO bidRequest) {
        log.info("Received Bid via WebSocket: {}", bidRequest);

        ReverseAuction auction = auctionRepository.findById(bidRequest.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (!"ACTIVE".equals(auction.getStatus())) {
            throw new IllegalArgumentException("Auction is closed");
        }

        Vendor vendor = vendorRepository.findById(bidRequest.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        BigDecimal bidAmount = bidRequest.getBidAmount();

        // 1. Must be lower than Ceiling Price
        if (bidAmount.compareTo(auction.getCeilingPrice()) > 0) {
            throw new IllegalArgumentException("Bid must be lower than the ceiling price");
        }

        // 2. Must be lower than the current lowest bid
        Optional<AuctionBid> currentLowest = bidRepository.findLowestBidForAuction(auction.getId());
        if (currentLowest.isPresent() && bidAmount.compareTo(currentLowest.get().getBidAmount()) >= 0) {
            throw new IllegalArgumentException("Bid must be lower than the current lowest bid");
        }

        // 3. Save the new winning bid
        AuctionBid newBid = AuctionBid.builder()
                .auction(auction)
                .vendor(vendor)
                .bidAmount(bidAmount)
                .isWinning(true)
                .build();

        newBid = bidRepository.save(newBid);
        
        // 4. Update the auction
        auction.setWinningBidId(newBid.getId());
        auctionRepository.save(auction);

        // 5. Broadcast the new lowest price to ALL connected vendors in real-time!
        Map<String, Object> broadcastPayload = Map.of(
                "auctionId", auction.getId(),
                "newLowestPrice", bidAmount,
                "vendorName", vendor.getLegalName(), // Optional: Anonymize this in real life
                "timestamp", newBid.getBidTime()
        );

        messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(), broadcastPayload);
        log.info("Broadcasted new lowest bid {} to /topic/auctions/{}", bidAmount, auction.getId());
    }

    // --- REST API: Fetch Single Auction ---
    @GetMapping("/{id}")
    public ResponseEntity<ReverseAuction> getAuctionById(@PathVariable UUID id) {
        ReverseAuction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        return ResponseEntity.ok(auction);
    }

    // --- REST API: Fetch Chronological Bids ---
    @GetMapping("/{id}/bids")
    public ResponseEntity<List<AuctionBid>> getBidsForAuction(@PathVariable UUID id) {
        return ResponseEntity.ok(bidRepository.findByAuctionIdOrderByBidAmountAsc(id));
    }

    // --- REST API: Update Auction Status (Pause, Close, Award) ---
    @PutMapping("/{id}/status")
    public ResponseEntity<ReverseAuction> updateAuctionStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        ReverseAuction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));
        
        auction.setStatus(status.toUpperCase());

        // Handle winner calculation if status is CLOSED or AWARDED
        if ("CLOSED".equalsIgnoreCase(status) || "AWARDED".equalsIgnoreCase(status)) {
            Optional<AuctionBid> lowestBid = bidRepository.findLowestBidForAuction(id);
            if (lowestBid.isPresent()) {
                auction.setWinningBidId(lowestBid.get().getId());
            }
        }

        ReverseAuction savedAuction = auctionRepository.save(auction);
        log.info("Updated Auction {} status to {}", savedAuction.getAuctionNumber(), status);
        return ResponseEntity.ok(savedAuction);
    }
}
