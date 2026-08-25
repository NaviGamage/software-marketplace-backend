package com.marketplace.service;

import com.marketplace.dto.request.CreateProductRequest;
import com.marketplace.dto.request.UpdateProductRequest;
import com.marketplace.dto.response.ProductResponse;
import com.marketplace.entity.Category;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.ProductStatus;
import com.marketplace.exception.DuplicateResourceException;
import com.marketplace.exception.ForbiddenException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CategoryRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductResponse createProduct(Long vendorId, CreateProductRequest request) {

        if (productRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("A product with this slug already exists");
        }

        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", vendorId));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        Product product = Product.builder()
                .vendor(vendor)
                .category(category)
                .title(request.title())
                .slug(request.slug())
                .description(request.description())
                .demoUrl(request.demoUrl())
                .priceRegular(request.priceRegular())
                .priceExtended(request.priceExtended())
                .status(ProductStatus.DRAFT) // Every product starts as DRAFT; file upload + submission move it forward.
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    // Public: only APPROVED products, paginated.
    @Transactional(readOnly = true)
    public Page<ProductResponse> getApprovedProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.APPROVED, pageable)
                .map(ProductResponse::fromEntity);
    }

    // Public: APPROVED products within one category.
    @Transactional(readOnly = true)
    public Page<ProductResponse> getApprovedProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByStatusAndCategoryId(ProductStatus.APPROVED, categoryId, pageable)
                .map(ProductResponse::fromEntity);
    }

    // Public product detail page — but a non-APPROVED product should only be
    // visible to its owning vendor or an admin, not to the public. Since this
    // method has no caller identity, callers needing that check use
    // getProductForVendor / the admin queue instead; this is for the public page only.
    @Transactional(readOnly = true)
    public ProductResponse getApprovedProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));

        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new ResourceNotFoundException("Product", slug);
        }

        return ProductResponse.fromEntity(product);
    }

    // Vendor dashboard: "my products" — every status, only this vendor's own.
    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(Long vendorId, Pageable pageable) {
        return productRepository.findByVendorId(vendorId, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, Long vendorId, UpdateProductRequest request) {
        Product product = getOwnedProduct(productId, vendorId);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setDemoUrl(request.demoUrl());
        product.setPriceRegular(request.priceRegular());
        product.setPriceExtended(request.priceExtended());
        product.setCategory(category);

        // Editing an APPROVED product sends it back for re-review rather than
        // silently changing a live listing — same logic a marketplace like
        // Envato applies to material edits.
        if (product.getStatus() == ProductStatus.APPROVED) {
            product.setStatus(ProductStatus.PENDING_REVIEW);
        }

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProduct(Long productId, Long vendorId) {
        Product product = getOwnedProduct(productId, vendorId);
        productRepository.delete(product);
    }

    // Shared ownership check: loads the product and verifies it belongs to
    // this vendor. Used by every vendor-facing write operation so the
    // "is this actually your product?" logic lives in exactly one place.
    private Product getOwnedProduct(Long productId, Long vendorId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.getVendor().getId().equals(vendorId)) {
            throw new ForbiddenException("You do not have permission to modify this product");
        }

        return product;
    }
}