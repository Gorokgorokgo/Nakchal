# 🛒 CherryPick 구매자 시나리오

**대상**: 가성비 중고물품을 경매로 구매하고자 하는 사용자

---

## 💳 **B1. 첫 포인트 충전 플로우**

**목표**: 경매 입찰을 위해 포인트를 충전하고 싶음  
**사용자**: 신규 구매자

### 사용자 여정
```
1. 앱 실행 → 로그인
   ↓
2. 관심 있는 경매 발견 → 입찰 시도
   ↓
3. 포인트 부족 안내
   "입찰하려면 100,000원이 필요해요. 현재 잔액: 0원"
   ↓
4. 포인트 충전 화면
   ├─ 충전 금액 선택 (10만원/20만원/50만원/직접입력)
   ├─ 계좌 연결 (최초 1회, 본인 명의 계좌만)
   └─ 충전 완료 → 자동으로 입찰 화면 복귀
```

### 핵심 요구사항
- 포인트 부족 시 자동 충전 안내
- 본인 명의 계좌만 연결 가능
- 충전 후 원래 화면 복귀

### API 연관성
- `GET /api/points/balance` - 포인트 잔액 조회
- `POST /api/points/charge` - 포인트 충전
- `POST /api/accounts/link` - 계좌 연결

---

## 🎯 **B2. 입찰 참여 플로우**

**목표**: 관심 있는 상품에 입찰하여 낙찰받고 싶음  
**사용자**: 경매 참여 구매자

### 사용자 여정
```
1. 경매 목록에서 관심 상품 클릭
   ↓
2. 경매 상세 화면
   ├─ 📷 상품 사진들 확인
   ├─ 📝 상품 설명 읽기
   ├─ 💰 현재가: 580,000원
   ├─ ⏰ 남은 시간: 14시간 32분
   ├─ 👥 입찰자 수: 5명
   └─ 📍 거래 지역: 서울 강남구
   ↓
3. 입찰하기
   ├─ 입찰가 입력 (현재가 + 5~10%)
   ├─ "600,000원에 입찰하기"
   ├─ 포인트 60만원 즉시 예치(lock)
   └─ 입찰 완료 알림
   ↓
4. 경쟁 상황 실시간 확인
   "이영수님이 620,000원에 입찰했어요!"
   ├─ 더 높은 가격으로 재입찰 가능
   └─ 자동 입찰 설정 가능 (최대 70만원까지)
```

### 핵심 요구사항
- 입찰 시 포인트 즉시 예치(lock) 시스템
- 현재가 대비 5-10% 증가 가이드라인
- 실시간 경쟁 상황 알림
- 자동 입찰 기능

### API 연관성
- `GET /api/auctions` - 경매 목록 조회
- `GET /api/auctions/{id}` - 경매 상세 조회
- `POST /api/auctions/{id}/bids` - 입찰하기
- `POST /api/bids/auto-bid` - 자동 입찰 설정

---

## 🎉 **B3. 낙찰 받은 후 플로우**

**목표**: 낙찰받은 상품을 판매자와 안전하게 거래하고 싶음  
**사용자**: 낙찰 성공 구매자

### 사용자 여정
```
1. 낙찰 알림 📱
   "축하합니다! 650,000원에 낙찰받으셨어요!"
   ↓
2. 판매자와 거래 협의
   ├─ 채팅으로 만날 장소/시간 협의
   ├─ 직거래 위치 확인 (안전한 공공장소 권장)
   └─ 거래 전 최종 확인
   ↓
3. 직거래 완료 후
   ├─ 상품 상태 확인
   ├─ "거래 완료" 버튼 클릭
   ├─ 판매자도 "거래 완료" 확인 필요
   └─ 양측 확인 완료시 → 포인트 정산
   ↓
4. 거래 완료
   ├─ 예치된 650,000원 사용 처리
   ├─ 구매 경험치 +160 EXP
   ├─ 자동 입찰 설정시 추가 보너스 EXP
   └─ 후기 작성 요청
```

### 핵심 요구사항
- 안전한 거래 장소 가이드라인
- 양측 거래 완료 확인 시스템
- 예치 포인트 자동 정산
- 경험치 보상 시스템

### API 연관성
- `POST /api/chat/rooms` - 채팅방 생성
- `POST /api/chat/send` - 메시지 전송
- `POST /api/transactions/{id}/complete` - 거래 완료 처리
- `GET /api/transactions/my` - 내 거래 내역

---

## 📊 **구매자 성공 지표**

### KPI 목표
- ✅ 회원가입 → 첫 입찰까지 **10분 이내**
- ✅ 입찰 후 낙찰 확률 **30% 이상**
- ✅ 낙찰 후 거래 완료율 **98% 이상**
- ✅ 자동 입찰 활용으로 편의성 증대
- ✅ 재구매 의향 **80% 이상**

### 성공 요인
1. **빠른 온보딩**: 간편한 회원가입과 포인트 충전
2. **스마트 입찰**: 자동 입찰 기능 활용
3. **신뢰할 수 있는 판매자**: 신뢰도 지표 확인
4. **안전한 거래**: 공공장소에서의 직거래
5. **만족스러운 경험**: 기대 가격 대비 좋은 상품

---

## 🔍 **구매자 의사결정 요소**

### 입찰 결정 요인
1. **상품 상태**: 사진과 설명의 상세함
2. **판매자 신뢰도**: 레벨, 신뢰도 점수, 후기
3. **현재 경쟁 상황**: 입찰자 수, 현재가
4. **거래 편의성**: 지역, 거래 방식
5. **가격 매력도**: 시중가 대비 할인율

### 입찰 전략
1. **관심 목록 활용**: 여러 경매 동시 모니터링
2. **자동 입찰 설정**: 최대 예산 내에서 자동 경쟁
3. **마지막 순간 입찰**: 경매 종료 직전 입찰
4. **적정 예산 설정**: 시중가의 70-80% 수준

---

## ⚠️ **예외 상황 대응**

### 😅 구매자 노쇼 예방
- 거래 약속 알림 시스템
- 위약금 차감 경고 (10%)
- 계정 제재 안내

### 💸 입찰 실패 시 대응
- 유사 상품 추천
- 알림 설정 제안
- 예산 조정 가이드

### 🔄 포인트 관리
- 입찰 실패 시 즉시 포인트 해제
- 잔액 부족 시 충전 안내
- 사용 내역 투명 공개