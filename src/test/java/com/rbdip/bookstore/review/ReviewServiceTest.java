package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.purchase.PurchaseHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviews;
    @Mock
    private PurchaseHistory purchases;
    @InjectMocks
    private ReviewService service;

    @Test
    void unverifiedBuyerCanStillLeaveAnonymousReview() {
        when(reviews.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Review result = service.addReview(1L, null, 5, null);
        verify(purchases).hasPurchased(1L, null);
        assertThat(result.getAuthorName()).isEqualTo("anonymous");
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isNull();
    }

    @Test
    void verifiedPurchasePreservesReviewContents() {
        when(purchases.hasPurchased(2L, "Anna")).thenReturn(true);
        when(reviews.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.addReview(2L, "Anna", 4, "Useful book");
        var saved = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(saved.capture());
        assertThat(saved.getValue().getAuthorName()).isEqualTo("Anna");
        assertThat(saved.getValue().getComment()).isEqualTo("Useful book");
        assertThat(saved.getValue().getRating()).isEqualTo(4);
    }
}
