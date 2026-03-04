package com.examsystem.controller;

import com.examsystem.entity.Stem;
import com.examsystem.service.StemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stems")
public class StemController {

    @Autowired
    private StemService stemService;

    @GetMapping("/{id}")
    public Stem getStem(@PathVariable Long id) {
        return stemService.getStemById(id);
    }

    @GetMapping("/batch")
    public Map<Long, Stem> getStems(@RequestParam(required = false) List<Long> ids) {
        return stemService.getStemsByIds(ids);
    }

    @PostMapping
    public Stem createStem(@RequestBody Stem stem) {
        stemService.createStem(stem);
        return stem;
    }

    @PutMapping("/{id}")
    public Stem updateStem(@PathVariable Long id, @RequestBody Stem stem) {
        stem.setId(id);
        stemService.updateStem(stem);
        return stem;
    }
}

