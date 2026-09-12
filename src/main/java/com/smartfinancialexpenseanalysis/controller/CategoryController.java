package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.CategoryResponse;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving expense categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retrieves all available expense categories.
     *
     * @return List of CategoryResponse DTOs sorted by ID
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll(Sort.by("id"))
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }
}
