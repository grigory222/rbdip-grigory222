package com.rbdip.bookstore.review;

import com.rbdip.bookstore.purchase.PurchaseHistory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewService.class);
    private final ReviewRepository reviewRepository;
    private final PurchaseHistory purchases;

    public ReviewService(ReviewRepository reviewRepository, PurchaseHistory purchases) {
        this.reviewRepository = reviewRepository;
        this.purchases = purchases;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        boolean verifiedPurchase = purchases.hasPurchased(productId, authorName);
        // Как и прежде, отзывы разрешены всем; факт покупки пока нужен только для диагностики.
        LOG.debug("Review for product {}: verified purchase = {}", productId, verifiedPurchase);
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
