package com.darshan.payment_ledger.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// Generic wrapper for paginated API responses.
//
// WHY a custom wrapper instead of returning Spring's Page<T> directly?
// Spring's Page<T> serializes to JSON with implementation-specific fields (pageable.sort.empty,
// pageable.offset, etc.) that are noisy and expose internal details.
// A custom DTO gives us a clean, predictable contract that frontend devs can rely on.
//
// Example response:
// {
//   "content": [...],
//   "page": 0,
//   "size": 20,
//   "totalElements": 157,
//   "totalPages": 8,
//   "first": true,
//   "last": false
// }

@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;        // The actual items for this page
    private int page;               // Current page number (0-indexed)
    private int size;               // Requested page size
    private long totalElements;     // Total records in DB matching the query
    private int totalPages;         // Total number of pages
    private boolean first;          // Is this the first page?
    private boolean last;           // Is this the last page?

    // Static factory — converts Spring's Page<S> to PagedResponse<T>
    // using a mapper function to convert entity → DTO.
    //
    // Usage:
    //   Page<Transaction> page = repo.findAll(pageable);
    //   PagedResponse<TransactionResponse> response = PagedResponse.from(page, service::toResponse);
    //
    // WHY static factory instead of constructor?
    // Encapsulates the mapping + metadata extraction in one place.
    // Callers don't need to know about Spring's Page internals.
    public static <S, T> PagedResponse<T> from(Page<S> page, Function<S, T> mapper) {
        List<T> content = page.getContent()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());

        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
