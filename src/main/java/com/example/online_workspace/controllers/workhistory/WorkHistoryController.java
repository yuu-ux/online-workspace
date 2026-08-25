package com.example.online_workspace.controllers.workhistory;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.WorkHistory.WorkSessionPage;
import com.example.online_workspace.models.WorkHistory.WorkSessionSummary;
import com.example.online_workspace.services.WorkHistoryService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/work-sessions")
public class WorkHistoryController {

	private final WorkHistoryService workHistoryService;

	public WorkHistoryController(WorkHistoryService workHistoryService) {
		this.workHistoryService = workHistoryService;
	}

	@GetMapping
	public WorkSessionPage listWorkSessions(
		Principal principal,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(required = false) Long categoryId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return workHistoryService.findSessions(principal.getName(), from, to, categoryId, page, size);
	}

	@GetMapping("/summary")
	public WorkSessionSummary getWorkSessionSummary(
		Principal principal,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return workHistoryService.summarize(principal.getName(), from, to);
	}
}
