package com.rbdip.bookstore.purchase;

/** Публичный контракт для модулей, которым нужно проверить факт покупки. */
public interface PurchaseHistory {
    boolean hasPurchased(Long productId, String customerName);
}
