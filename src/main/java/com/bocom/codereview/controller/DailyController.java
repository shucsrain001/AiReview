package com.bocom.codereview.controller;

import com.bocom.codereview.task.DailyReviewTask;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DailyController {

    @Resource
    private DailyReviewTask dailyReviewTask;

    @GetMapping("/daily")
    public void daily() {
        dailyReviewTask.runDailyReview();

    }
}
