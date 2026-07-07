package io.d4y.api;

import io.d4y.api.dto.ActivityView;
import io.d4y.app.OperationsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only Audit-Log operativer Aktionen (ADR-0013). */
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final OperationsService ops;

    public ActivityController(OperationsService ops) {
        this.ops = ops;
    }

    @GetMapping
    public List<ActivityView> activity(@RequestParam(defaultValue = "100") int limit) {
        return ops.activity(limit).stream().map(ActivityView::of).toList();
    }
}
