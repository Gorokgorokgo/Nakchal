package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.EntityNotFoundException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.point.entity.PointLock;
import com.cherrypick.app.domain.point.enums.PointLockStatus;
import com.cherrypick.app.domain.point.repository.PointLockRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {
    
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final PointLockRepository pointLockRepository;
    
    /**
     * 입찰하기
     * 
     * 비즈니스 로직:
     * 1. 경매 유효성 검증 (진행중, 종료시간 등)
     * 2. 입찰자 정보 확인
     * 3. 입찰 금액 유효성 검증 (최소 증가폭, 현재가 등)
     * 4. 포인트 잔액 확인 및 예치(Lock)
     * 5. 기존 입찰자의 포인트 잠금 해제
     * 6. 새 입찰 등록
     * 7. 경매 현재가 업데이트
     * 
     * @param userId 입찰자 사용자 ID
     * @param request 입찰 요청 정보
     * @return 입찰 결과
     */
    @Transactional
    public BidResponse placeBid(Long userId, PlaceBidRequest request) {
        // 요청 데이터 유효성 검증
        request.validate();
        
        // 경매 정보 조회 및 유효성 검증
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(EntityNotFoundException::auction);
        
        validateAuctionForBidding(auction);
        
        // 입찰자 정보 확인
        User bidder = userRepository.findById(userId)
                .orElseThrow(EntityNotFoundException::user);
        
        // 자신의 경매에는 입찰할 수 없음
        if (auction.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.SELF_BID_NOT_ALLOWED);
        }
        
        // 입찰 금액 유효성 검증
        validateBidAmount(auction, request.getBidAmount());
        
        // 포인트 잔액 확인 (민감한 정보 노출 방지)
        if (bidder.getPointBalance() < request.getBidAmount().longValue()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        
        // 기존 최고가 입찰자의 포인트 잠금 해제
        releaseExistingBidLocks(auction);
        
        // 새 입찰자의 포인트 예치(Lock)
        lockBidAmount(bidder, auction, request.getBidAmount());
        
        // 입찰 생성
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(request.getBidAmount())
                .isAutoBid(request.getIsAutoBid())
                .maxAutoBidAmount(request.getMaxAutoBidAmount())
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        
        Bid savedBid = bidRepository.save(bid);
        
        // 경매 현재가 및 입찰수 업데이트
        auction.updateCurrentPrice(request.getBidAmount());
        auction.increaseBidCount();
        auctionRepository.save(auction);
        
        return BidResponse.from(savedBid, true);
    }
    
    /**
     * 경매별 입찰 내역 조회
     */
    public Page<BidResponse> getBidsByAuction(Long auctionId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId, pageable);
        
        return bids.map(bid -> {
            // 최고가 입찰인지 확인
            BigDecimal highestAmount = bidRepository.findHighestBidAmountByAuctionId(auctionId);
            boolean isHighestBid = bid.getBidAmount().equals(highestAmount);
            
            return BidResponse.from(bid, isHighestBid);
        });
    }
    
    /**
     * 내 입찰 내역 조회
     */
    public Page<BidResponse> getMyBids(Long userId, Pageable pageable) {
        Page<Bid> bids = bidRepository.findByBidderIdOrderByBidTimeDesc(userId, pageable);
        
        return bids.map(bid -> {
            // 최고가 입찰인지 확인
            BigDecimal highestAmount = bidRepository.findHighestBidAmountByAuctionId(bid.getAuction().getId());
            boolean isHighestBid = bid.getBidAmount().equals(highestAmount);
            
            return BidResponse.from(bid, isHighestBid);
        });
    }
    
    /**
     * 특정 경매의 최고가 입찰 조회
     */
    public BidResponse getHighestBid(Long auctionId) {
        Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_BID_EXISTS));
        
        return BidResponse.from(highestBid, true);
    }
    
    /**
     * 경매 입찰 유효성 검증
     */
    private void validateAuctionForBidding(Auction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_ACTIVE);
        }
        
        if (auction.getEndAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUCTION_ENDED);
        }
    }
    
    /**
     * 입찰 금액 유효성 검증 (민감한 가격 정보 노출 방지)
     */
    private void validateBidAmount(Auction auction, BigDecimal bidAmount) {
        BigDecimal currentPrice = auction.getCurrentPrice();
        BigDecimal minimumBid = currentPrice.add(BigDecimal.valueOf(1000)); // 최소 1000원 증가
        
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT);
        }
    }
    
    /**
     * 기존 입찰자들의 포인트 잠금 해제
     */
    private void releaseExistingBidLocks(Auction auction) {
        List<PointLock> activeLocks = pointLockRepository.findByAuctionIdAndStatus(
                auction.getId(), PointLockStatus.LOCKED);
        
        for (PointLock lock : activeLocks) {
            lock.setStatus(PointLockStatus.UNLOCKED);
            lock.setUnlockedAt(LocalDateTime.now());
        }
        
        pointLockRepository.saveAll(activeLocks);
    }
    
    /**
     * 새 입찰자의 포인트 예치(Lock)
     */
    private void lockBidAmount(User bidder, Auction auction, BigDecimal bidAmount) {
        // 가장 최근 입찰을 기준으로 PointLock 생성
        Bid latestBid = bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId()).orElse(null);
        
        PointLock pointLock = PointLock.builder()
                .user(bidder)
                .auction(auction)
                .bid(latestBid) // 아직 저장되지 않은 입찰이므로 이전 입찰 참조
                .lockedAmount(bidAmount)
                .status(PointLockStatus.LOCKED)
                .lockedAt(LocalDateTime.now())
                .build();
        
        pointLockRepository.save(pointLock);
    }
}