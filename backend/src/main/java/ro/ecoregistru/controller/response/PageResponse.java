package ro.ecoregistru.controller.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of rows, plus what the table under it needs to draw itself.
 *
 * <p>Deliberately <b>not</b> Spring's own {@code Page}: that one serialises twenty fields, half of
 * them about the {@code Pageable} it came from, and its JSON shape is explicitly unstable across
 * Spring versions (Boot 3 logs a warning about serialising it). The screen needs four numbers, and
 * four numbers is what the contract says.
 *
 * @param content       the rows of this page, already mapped to responses
 * @param page          zero-based index of this page, echoed back so the client can trust it
 * @param size          how many rows a full page holds
 * @param totalElements how many rows match the filters <em>and</em> the search, across all pages —
 *                      this is the number the toolbar shows next to the search box
 * @param totalPages    how many pages that makes, at least 1 so an empty table still has a page
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Maps a page of entities into a page of responses, keeping the counts as the database gave them. */
    public static <E, R> PageResponse<R> of(Page<E> source, Function<E, R> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                Math.max(1, source.getTotalPages()));
    }
}
