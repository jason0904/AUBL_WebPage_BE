package com.aubl.webpage.api;

import com.aubl.webpage.api.dto.ImportResult;
import com.aubl.webpage.service.FirestoreImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import/firestore")
public class FirestoreImportController {

    private final FirestoreImportService firestoreImportService;

    public FirestoreImportController(FirestoreImportService firestoreImportService) {
        this.firestoreImportService = firestoreImportService;
    }

    @PostMapping("/matches")
    public ResponseEntity<ImportResult> importCompletedMatches() {
        return ResponseEntity.ok(firestoreImportService.importCompletedMatches());
    }

    @PostMapping("/matches/{matchId}")
    public ResponseEntity<ImportResult> importMatch(@org.springframework.web.bind.annotation.PathVariable String matchId) {
        return ResponseEntity.ok(firestoreImportService.importMatchById(matchId));
    }
}
