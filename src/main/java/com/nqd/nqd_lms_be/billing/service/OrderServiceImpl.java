package com.nqd.nqd_lms_be.billing.service;

import com.nqd.nqd_lms_be.billing.dto.CreateOrderRequest;
import com.nqd.nqd_lms_be.billing.dto.OrderResponse;
import com.nqd.nqd_lms_be.common.dto.PageResponse;
import com.nqd.nqd_lms_be.common.exception.ForbiddenOperationException;
import com.nqd.nqd_lms_be.common.exception.ResourceNotFoundException;
import com.nqd.nqd_lms_be.entity.*;
import com.nqd.nqd_lms_be.entity.enums.FeatureKey;
import com.nqd.nqd_lms_be.entity.enums.OrderStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductStatus;
import com.nqd.nqd_lms_be.entity.enums.ProductType;
import com.nqd.nqd_lms_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final CouponRepository couponRepository;
    private final com.nqd.nqd_lms_be.membership.service.MembershipEntitlementService membershipEntitlementService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public Order createCourseOrder(UUID userId, UUID courseId, String couponCode, String idempotencyKey) {
        User user = getUserOrThrow(userId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Idempotent order request hit for key: {}", idempotencyKey);
                return existingOrder.get();
            }
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + courseId));

        // Find or provision Product for this course
        Product product = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(courseId, ProductType.COURSE)
                .orElseGet(() -> {
                    Product newProduct = Product.builder()
                            .code("PRD-CRS-" + course.getCode())
                            .title(course.getName())
                            .description(course.getDescription())
                            .productType(ProductType.COURSE)
                            .targetEntityId(courseId)
                            .basePrice(course.getEffectivePrice() != null ? course.getEffectivePrice() : new BigDecimal("499000.00"))
                            .currency("VND")
                            .status(ProductStatus.PUBLISHED)
                            .thumbnailUrl(course.getThumbnailUrl())
                            .build();
                    return productRepository.save(newProduct);
                });

        boolean isPro = membershipEntitlementService.hasFeature(userId, FeatureKey.DISCOUNT_ON_PURCHASES);
        String orderCode = generateOrderCode("COURSE", course.getCode());
        BigDecimal totalAmount = product.getBasePrice();
        BigDecimal proDiscount = isPro ? totalAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal couponDiscount = calculateCouponDiscount(couponCode, totalAmount);
        BigDecimal discountAmount = couponDiscount.add(proDiscount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency(product.getCurrency())
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .couponCode(couponCode)
                .placedAt(LocalDateTime.now())
                .notes("Đăng ký mua khóa học: " + course.getName() + (isPro ? " (Ưu đãi PRO -20%)" : ""))
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .productTitle(product.getTitle())
                .productType(ProductType.COURSE)
                .unitPrice(product.getBasePrice())
                .quantity(1)
                .subtotal(finalAmount)
                .currency(product.getCurrency())
                .build();

        orderItemRepository.save(orderItem);
        savedOrder.getItems().add(orderItem);

        log.info("Created course purchase order #{} for user {}", savedOrder.getOrderCode(), user.getEmail());
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createMembershipOrder(UUID userId, UUID membershipPlanId, String couponCode, String idempotencyKey) {
        User user = getUserOrThrow(userId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Idempotent membership order request hit for key: {}", idempotencyKey);
                return existingOrder.get();
            }
        }

        MembershipPlan plan = membershipPlanRepository.findById(membershipPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói hội viên với ID: " + membershipPlanId));

        Product product = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(membershipPlanId, ProductType.MEMBERSHIP)
                .orElseGet(() -> {
                    Product newProduct = Product.builder()
                            .code("PRD-MEM-" + plan.getPlanCode())
                            .title(plan.getName())
                            .description(plan.getDescription())
                            .productType(ProductType.MEMBERSHIP)
                            .targetEntityId(membershipPlanId)
                            .basePrice(plan.getPrice())
                            .currency(plan.getCurrency())
                            .status(ProductStatus.PUBLISHED)
                            .build();
                    return productRepository.save(newProduct);
                });

        String orderCode = generateOrderCode("MEM", plan.getPlanCode());
        BigDecimal totalAmount = plan.getPrice();
        BigDecimal discountAmount = calculateCouponDiscount(couponCode, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency(plan.getCurrency())
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .couponCode(couponCode)
                .placedAt(LocalDateTime.now())
                .notes("Đăng ký gói hội viên: " + plan.getName())
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .productTitle(plan.getName())
                .productType(ProductType.MEMBERSHIP)
                .unitPrice(plan.getPrice())
                .quantity(1)
                .subtotal(plan.getPrice())
                .currency(plan.getCurrency())
                .build();

        orderItemRepository.save(orderItem);
        savedOrder.getItems().add(orderItem);

        log.info("Created membership order #{} for user {}", savedOrder.getOrderCode(), user.getEmail());
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createChapterOrder(UUID userId, UUID chapterId, String couponCode, String idempotencyKey) {
        User user = getUserOrThrow(userId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương học với ID: " + chapterId));

        BigDecimal basePrice = chapter.getPrice() != null && chapter.getPrice().compareTo(BigDecimal.ZERO) > 0
                ? chapter.getPrice()
                : new BigDecimal("99000.00");

        Product product = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(chapterId, ProductType.CHAPTER_PURCHASE)
                .orElseGet(() -> {
                    Product newProduct = Product.builder()
                            .code("PRD-CHP-" + chapter.getId().toString().substring(0, 8))
                            .title("Chương: " + chapter.getTitle())
                            .description(chapter.getDescription())
                            .productType(ProductType.CHAPTER_PURCHASE)
                            .targetEntityId(chapterId)
                            .basePrice(basePrice)
                            .currency("VND")
                            .status(ProductStatus.PUBLISHED)
                            .build();
                    return productRepository.save(newProduct);
                });

        boolean isPro = membershipEntitlementService.hasFeature(userId, FeatureKey.DISCOUNT_ON_PURCHASES);
        BigDecimal totalAmount = product.getBasePrice();
        BigDecimal proDiscount = isPro ? totalAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal couponDiscount = calculateCouponDiscount(couponCode, totalAmount);
        BigDecimal discountAmount = couponDiscount.add(proDiscount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        String orderCode = generateOrderCode("CHP", chapter.getId().toString().substring(0, 8));
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency(product.getCurrency())
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .couponCode(couponCode)
                .placedAt(LocalDateTime.now())
                .notes("Đăng ký mua chương học: " + chapter.getTitle() + (isPro ? " (Ưu đãi PRO -20%)" : ""))
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .productTitle(product.getTitle())
                .productType(ProductType.CHAPTER_PURCHASE)
                .unitPrice(product.getBasePrice())
                .quantity(1)
                .subtotal(finalAmount)
                .currency(product.getCurrency())
                .build();

        orderItemRepository.save(orderItem);
        savedOrder.getItems().add(orderItem);
        log.info("Created chapter purchase order #{} for user {}", savedOrder.getOrderCode(), user.getEmail());
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createLessonOrder(UUID userId, UUID lessonId, String couponCode, String idempotencyKey) {
        User user = getUserOrThrow(userId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với ID: " + lessonId));

        BigDecimal basePrice = lesson.getPrice() != null && lesson.getPrice().compareTo(BigDecimal.ZERO) > 0
                ? lesson.getPrice()
                : new BigDecimal("29000.00");

        Product product = productRepository.findByTargetEntityIdAndProductTypeAndIsDeletedFalse(lessonId, ProductType.LESSON_PURCHASE)
                .orElseGet(() -> {
                    Product newProduct = Product.builder()
                            .code("PRD-LES-" + lesson.getId().toString().substring(0, 8))
                            .title("Bài học: " + lesson.getTitle())
                            .description(lesson.getSummary())
                            .productType(ProductType.LESSON_PURCHASE)
                            .targetEntityId(lessonId)
                            .basePrice(basePrice)
                            .currency("VND")
                            .status(ProductStatus.PUBLISHED)
                            .build();
                    return productRepository.save(newProduct);
                });

        boolean isPro = membershipEntitlementService.hasFeature(userId, FeatureKey.DISCOUNT_ON_PURCHASES);
        BigDecimal totalAmount = product.getBasePrice();
        BigDecimal proDiscount = isPro ? totalAmount.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal couponDiscount = calculateCouponDiscount(couponCode, totalAmount);
        BigDecimal discountAmount = couponDiscount.add(proDiscount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        String orderCode = generateOrderCode("LES", lesson.getId().toString().substring(0, 8));
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency(product.getCurrency())
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .couponCode(couponCode)
                .placedAt(LocalDateTime.now())
                .notes("Đăng ký mua bài học: " + lesson.getTitle() + (isPro ? " (Ưu đãi PRO -20%)" : ""))
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .productTitle(product.getTitle())
                .productType(ProductType.LESSON_PURCHASE)
                .unitPrice(product.getBasePrice())
                .quantity(1)
                .subtotal(finalAmount)
                .currency(product.getCurrency())
                .build();

        orderItemRepository.save(orderItem);
        savedOrder.getItems().add(orderItem);
        log.info("Created lesson purchase order #{} for user {}", savedOrder.getOrderCode(), user.getEmail());
        return savedOrder;
    }

    @Override
    @Transactional
    public Order createOrder(UUID userId, CreateOrderRequest request) {
        if (request.getCourseId() != null) {
            return createCourseOrder(userId, request.getCourseId(), request.getCouponCode(), request.getIdempotencyKey());
        }
        if (request.getChapterId() != null) {
            return createChapterOrder(userId, request.getChapterId(), request.getCouponCode(), request.getIdempotencyKey());
        }
        if (request.getLessonId() != null) {
            return createLessonOrder(userId, request.getLessonId(), request.getCouponCode(), request.getIdempotencyKey());
        }
        if (request.getMembershipPlanId() != null) {
            return createMembershipOrder(userId, request.getMembershipPlanId(), request.getCouponCode(), request.getIdempotencyKey());
        }

        User user = getUserOrThrow(userId);
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất một sản phẩm.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + itemReq.getProductId()));

            int quantity = itemReq.getQuantity() != null && itemReq.getQuantity() > 0 ? itemReq.getQuantity() : 1;
            BigDecimal subtotal = product.getBasePrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productTitle(product.getTitle())
                    .productType(product.getProductType())
                    .unitPrice(product.getBasePrice())
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .currency(product.getCurrency())
                    .build();

            orderItems.add(orderItem);
        }

        BigDecimal discountAmount = calculateCouponDiscount(request.getCouponCode(), totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String orderCode = generateOrderCode("GEN", "ITEM");

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .currency("VND")
                .status(OrderStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .couponCode(request.getCouponCode())
                .placedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }
        savedOrder.setItems(orderItems);

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn không có quyền xem đơn hàng của người dùng khác.");
        }

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCodeWithDetails(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        return getMyOrders(userId, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserIdAndStatusAndIsDeletedFalseOrderByPlacedAtDesc(userId, status, pageable);
        return PageResponse.fromPage(orderPage, OrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAdminOrders(OrderStatus status, String search, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted"))));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("orderCode")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("user").get("email")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                predicates.add(cb.or(codeMatch, emailMatch, nameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> page = orderRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page, OrderResponse::fromEntity);
    }

    @Override
    @Transactional
    public Order cancelOrder(UUID orderId, UUID userId, String reason) {
        Order order = getOrderById(orderId, userId);
        if (order.getStatus().isPaid()) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã thanh toán.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setNotes(order.getNotes() != null ? order.getNotes() + " | Lý do hủy: " + reason : "Lý do hủy: " + reason);
        return orderRepository.save(order);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
    }

    private BigDecimal calculateCouponDiscount(String couponCode, BigDecimal totalAmount) {
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeAndIsActiveTrueAndIsDeletedFalse(couponCode.trim().toUpperCase());
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            if (coupon.isApplicable(totalAmount)) {
                return coupon.calculateDiscount(totalAmount);
            }
        }
        return BigDecimal.ZERO;
    }

    private String generateOrderCode(String type, String subcode) {
        String cleanSubcode = subcode != null ? subcode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "DEF";
        if (cleanSubcode.length() > 6) {
            cleanSubcode = cleanSubcode.substring(0, 6);
        }
        String randomSuffix = String.format("%06d", RANDOM.nextInt(1000000));
        return "NQD" + type.toUpperCase() + cleanSubcode + randomSuffix;
    }
}
