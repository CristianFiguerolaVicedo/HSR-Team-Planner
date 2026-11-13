package com.crifigvic.hsrplanner.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final UidImportService imports;

    /**
     * Characters (and Eidolons) shown in a player's in-game profile. Only the showcase is public,
     * so this never returns the whole account. Nothing is saved: the frontend merges the result
     * into "My characters".
     */
    @GetMapping("/{uid}")
    public ImportResult importUid(@PathVariable String uid) {
        return imports.importUid(uid);
    }
}
