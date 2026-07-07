package io.d4y.api;

import io.d4y.api.dto.HoldView;
import io.d4y.app.OperationsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only Übersicht der aktiven Holds (ADR-0013). */
@RestController
@RequestMapping("/api/holds")
public class HoldController {

    private final OperationsService ops;

    public HoldController(OperationsService ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<HoldView> holds() {
        return ops.activeHolds().stream()
                .map(h -> HoldView.of(h, ops.remainingSeconds(h)))
                .toList();
    }
}
