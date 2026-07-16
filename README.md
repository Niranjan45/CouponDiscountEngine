# Coupon & Discount Engine

Enterprise-grade coupon/discount engine built with Spring Boot 3 (Java 17).

## Project Structure

```
coupon-discount-engine/
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/example/couponengine/
    │   │   ├── CouponEngineApplication.java     # main entrypoint
    │   │   ├── config/
    │   │   │   └── PersistenceConfig.java       # JPA repo config
    │   │   ├── controller/
    │   │   │   ├── CouponController.java        # POST /api/v1/coupons/apply
    │   │   │   ├── CouponAdminController.java    # coupon CRUD for setup
    │   │   │   └── OrderController.java          # order create/fetch
    │   │   ├── dto/
    │   │   │   ├── ApplyCouponRequest.java
    │   │   │   ├── ApplyCouponResponse.java
    │   │   │   └── ApiErrorResponse.java
    │   │   ├── entity/
    │   │   │   ├── Coupon.java
    │   │   │   ├── CouponType.java
    │   │   │   ├── UserCoupon.java
    │   │   │   ├── Order.java
    │   │   │   └── OrderDiscount.java
    │   │   ├── exception/
    │   │   │   ├── CouponExpiredException.java
    │   │   │   ├── CouponAlreadyUsedException.java
    │   │   │   ├── CouponUsageLimitExceededException.java
    │   │   │   ├── InvalidCouponException.java
    │   │   │   ├── InvalidCouponCombinationException.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── GlobalExceptionHandler.java   # @RestControllerAdvice
    │   │   ├── repository/
    │   │   │   ├── CouponRepository.java         # incl. pessimistic lock query
    │   │   │   ├── UserCouponRepository.java      # incl. pessimistic lock query
    │   │   │   ├── OrderRepository.java           # incl. @EntityGraph fetch join
    │   │   │   └── OrderDiscountRepository.java
    │   │   ├── service/
    │   │   │   ├── CouponService.java / CouponServiceImpl.java   # core engine
    │   │   │   └── OrderService.java / OrderServiceImpl.java
    │   │   └── strategy/                          # Strategy Pattern
    │   │       ├── DiscountStrategy.java           # interface
    │   │       ├── FlatDiscountStrategy.java
    │   │       ├── PercentageDiscountStrategy.java
    │   │       ├── ConditionalDiscountStrategy.java
    │   │       └── DiscountStrategyFactory.java
    │   └── resources/
    │       ├── application.yml    # default = H2, `prod` profile = MySQL
    │       └── data.sql           # sample coupons for local testing
    └── test
        └── java/com/example/couponengine/
            └── CouponServiceConcurrencyTest.java   # idempotency + concurrency proof
```

## Dependencies (see `pom.xml`)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controllers |
| `spring-boot-starter-data-jpa` | Spring Data JPA / Hibernate |
| `spring-boot-starter-validation` | Hibernate Validator (`@NotNull`, `@DecimalMin`, etc.) |
| `spring-boot-starter-actuator` | health/metrics endpoints |
| `h2` | in-memory DB for dev/test |
| `mysql-connector-j` | production DB driver (`prod` profile) |
| `lombok` | boilerplate reduction (getters/setters/builders) |
| `spring-boot-starter-test` | JUnit 5, Mockito, Spring Test |

## Running

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`. Sample coupons are pre-loaded from `data.sql`:
`WELCOME100` (flat ₹100 off), `SAVE10PCT` (10% off, capped ₹500), `ELEC15`
(conditional 15% off electronics orders ≥ ₹2000, stackable), `EXPIRED5` (already expired, for testing).

H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:coupondb`).

## Sample Requests

**Create an order:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "totalAmount": 3000, "category": "ELECTRONICS"}'
```

**Apply a coupon (idempotent - safe to retry):**
```bash
curl -X POST http://localhost:8080/api/v1/coupons/apply \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "orderId": 1, "couponCodes": ["WELCOME100"]}'
```

**Apply multiple (stackable) coupons in one call:**
```bash
curl -X POST http://localhost:8080/api/v1/coupons/apply \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "orderId": 1, "couponCodes": ["SAVE10PCT", "ELEC15"]}'
```

## Design Notes — mapping to the assignment's review questions

**How is the rule set extensible?**
`DiscountStrategy` (Strategy Pattern) + `DiscountStrategyFactory`. Every
`CouponType` maps to one `@Component` implementing `DiscountStrategy`. New
discount logic = new class, zero changes to existing code (Open/Closed).

**How is reuse prevented?**
Two layers: (1) `UserCoupon.usedFlag` with a DB-level unique constraint on
`(user_id, coupon_id)`, checked/set inside the transaction under a
pessimistic lock; (2) the global `Coupon.usageLimit` / `usageCount` counter,
also incremented under a pessimistic lock, so it can never exceed the cap
even with parallel requests.

**How is concurrency handled?**
`CouponServiceImpl.applyCoupons` runs in one `@Transactional` boundary and
takes `PESSIMISTIC_WRITE` locks, in a fixed deterministic order (Order →
Coupon [sorted by code] → UserCoupon), on every row it will mutate. This
serializes conflicting requests instead of allowing lost updates, and the
fixed lock order prevents cross-transaction deadlocks. `@Version` fields on
`Coupon`/`UserCoupon`/`Order` provide an optimistic-locking safety net on
top. A configured lock-acquisition timeout
(`jakarta.persistence.lock.timeout`) prevents indefinite blocking; timeouts
surface as HTTP 409 via `GlobalExceptionHandler` so callers know to retry.
Idempotency is enforced via a unique constraint on
`order_discounts(order_id, coupon_id)`, so a retried "apply" call is
detected and returned as an already-applied result rather than double
discounting (see `CouponServiceConcurrencyTest` for a proof under 8
concurrent threads racing a `usageLimit=1` coupon).

**How is N+1 avoided?**
`OrderRepository.findByIdWithDiscounts` uses `@EntityGraph` to eager-fetch
`discounts` and `discounts.coupon` in one query.
`UserCouponRepository.findByUserIdAndCouponCode` and
`OrderDiscountRepository.findAllByOrderIdWithCoupon` use explicit
`JOIN FETCH`. `CouponRepository.findAllByCodeIn` batches the multi-coupon
combination-validation lookup into a single query instead of N lookups.
Hibernate batch fetching (`default_batch_fetch_size: 25`) is also enabled
as a blanket safety net for any remaining lazy associations.

## Edge Cases Covered

- Coupon expired at time of checkout → `CouponExpiredException` (HTTP 410)
- Same coupon applied concurrently → pessimistic lock + unique constraints
  guarantee only one redemption succeeds where `usageLimit` requires it
- Invalid coupon combinations (non-stackable, or two of the same type) →
  `InvalidCouponCombinationException` (HTTP 400), validated *before* any
  coupon row is locked/mutated
- Partial failures across multiple coupons in one request → the whole
  `applyCoupons` call is one transaction, so if any coupon in the batch is
  invalid, the entire application rolls back (all-or-nothing); no order
  is left in a half-discounted state
