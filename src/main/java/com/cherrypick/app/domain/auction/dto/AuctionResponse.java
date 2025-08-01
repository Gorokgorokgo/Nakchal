package com.cherrypick.app.domain.auction.dto;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionImage;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AuctionResponse {
    
    private Long id;
    private String title;
    private String description;
    private Category category;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal hopePrice;
    private BigDecimal depositAmount;
    private Integer auctionTimeHours;
    private RegionScope regionScope;
    private String regionCode;
    private String regionName;
    private AuctionStatus status;
    private Integer viewCount;
    private Integer bidCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    
    // 판매자 정보
    private Long sellerId;
    private String sellerNickname;
    
    // 이미지 목록
    private List<String> imageUrls;
    
    // 시간 관련 계산 필드
    private Long remainingTimeMs; // 남은 시간 (밀리초)
    private boolean isExpired;
    
    public static AuctionResponse from(Auction auction, List<AuctionImage> images) {
        AuctionResponse response = new AuctionResponse();
        
        response.setId(auction.getId());
        response.setTitle(auction.getTitle());
        response.setDescription(auction.getDescription());
        response.setCategory(auction.getCategory());
        response.setStartPrice(auction.getStartPrice());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setHopePrice(auction.getHopePrice());
        response.setDepositAmount(auction.getDepositAmount());
        response.setAuctionTimeHours(auction.getAuctionTimeHours());
        response.setRegionScope(auction.getRegionScope());
        response.setRegionCode(auction.getRegionCode());
        response.setRegionName(auction.getRegionName());
        response.setStatus(auction.getStatus());
        response.setViewCount(auction.getViewCount());
        response.setBidCount(auction.getBidCount());
        response.setStartAt(auction.getStartAt());
        response.setEndAt(auction.getEndAt());
        response.setCreatedAt(auction.getCreatedAt());
        
        // 판매자 정보
        response.setSellerId(auction.getSeller().getId());
        response.setSellerNickname(auction.getSeller().getNickname());
        
        // 이미지 URL 목록
        response.setImageUrls(images.stream()
                .map(AuctionImage::getImageUrl)
                .toList());
        
        // 시간 계산
        LocalDateTime now = LocalDateTime.now();
        if (auction.getEndAt().isAfter(now)) {
            response.setRemainingTimeMs(java.time.Duration.between(now, auction.getEndAt()).toMillis());
            response.setExpired(false);
        } else {
            response.setRemainingTimeMs(0L);
            response.setExpired(true);
        }
        
        return response;
    }
}