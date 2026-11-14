package com.sip.wbx.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.sip.wbx.entities.Categorie;
import com.sip.wbx.services.CategorieService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:4200")
public class CategorieController {

    private final CategorieService service;

    public CategorieController(CategorieService service) {
        this.service = service;
    }

    @GetMapping
    public List<Categorie> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Categorie get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categorie add(@Valid @RequestBody Categorie c) {
        return service.save(c);
    }

    @PutMapping("/{id}")
    public Categorie update(@PathVariable Long id, @Valid @RequestBody Categorie c) {
        return service.update(id, c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}