package com.marketplace.service;

import com.marketplace.dto.request.CreateProductRequest;
import com.marketplace.dto.request.RejectProductRequest;
import com.marketplace.dto.request.UpdateProductRequest;
import com.marketplace.dto.response.ProductResponse;
import com.marketplace.entity.Category;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.ProductStatus;
import com.marketplace.exception.BadRequestException;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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
                .status(ProductStatus.DRAFT)
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getApprovedProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.APPROVED, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getApprovedProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByStatusAndCategoryId(ProductStatus.APPROVED, categoryId, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProductResponse getApprovedProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));

        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new ResourceNotFoundException("Product", slug);
        }

        return ProductResponse.fromEntity(product);
    }

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

    // Vendor: moves a DRAFT product into the admin approval queue.
    @Transactional
    public ProductResponse submitForReview(Long productId, Long vendorId) {
        Product product = getOwnedProduct(productId, vendorId);

        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new BadRequestException(
                    "Only DRAFT or REJECTED products can be submitted for review (current status: " + product.getStatus() + ")"
            );
        }

        product.setStatus(ProductStatus.PENDING_REVIEW);
        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
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

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPendingProducts(Pageable pageable) {
        return productRepository.findByStatusOrderByCreatedAtAsc(ProductStatus.PENDING_REVIEW, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional
    public ProductResponse approveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new BadRequestException(
                    "Only products in PENDING_REVIEW can be approved (current status: " + product.getStatus() + ")"
            );
        }

        product.setStatus(ProductStatus.APPROVED);
        product.setRejectionReason(null);

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public ProductResponse rejectProduct(Long productId, RejectProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new BadRequestException(
                    "Only products in PENDING_REVIEW can be rejected (current status: " + product.getStatus() + ")"
            );
        }

        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(request.reason());

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    // Vendor: uploads/replaces the source-code zip for a product. Only allowed
    // while the product is still DRAFT — once submitted for review, the file
    // is what the admin is reviewing, so it can't be swapped underneath them.
    @Transactional
    public ProductResponse uploadProductFile(Long productId, Long vendorId, MultipartFile file) {
        Product product = getOwnedProduct(productId, vendorId);

        if (product.getStatus() != ProductStatus.DRAFT) {
            throw new BadRequestException(
                    "Files can only be uploaded while the product is in DRAFT status (current status: " + product.getStatus() + ")"
            );
        }

        FileStorageService.UploadResult result = fileStorageService.uploadProductFile(file, vendorId, productId);

        if (productRepository.existsByFileSha256(result.fileSha256())) {
            fileStorageService.deleteFile(result.fileKey());
            throw new BadRequestException("This exact file has already been uploaded to the platform");
        }

        if (product.getFileKey() != null) {
            fileStorageService.deleteFile(product.getFileKey());
        }

        product.setFileKey(result.fileKey());
        product.setFileSha256(result.fileSha256());

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }
}