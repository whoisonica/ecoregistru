package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.service.StockService;

import java.time.LocalDate;
import java.util.UUID;

/** F3 — stocul depozitelor. Îl citește oricine vede depozitul (D2.4); nu scrie nimic. */
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockController {

    StockService service;

    @GetMapping
    public StockResponse stock(@RequestParam(required = false) UUID workPointId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.stock(workPointId, date);
    }
}
