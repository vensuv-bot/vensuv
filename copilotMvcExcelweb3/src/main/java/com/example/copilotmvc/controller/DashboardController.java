package com.example.copilotmvc.controller;

import com.example.copilotmvc.model.TokenRecord;
import com.example.copilotmvc.repository.ExcelTokenRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {

    private final ExcelTokenRepository repository;

    public DashboardController(ExcelTokenRepository repository) {
        this.repository = repository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, @RequestParam(required = false) String message) {
        List<TokenRecord> tokens = repository.findAll();
        model.addAttribute("tokens", tokens);
        model.addAttribute("message", message);
        return "dashboard";
    }

    @PostMapping("/dashboard/add")
    public String addToken(@RequestParam String name, @RequestParam String tokenValue, RedirectAttributes ra) {
        TokenRecord r = repository.add(name, tokenValue);
        if (r != null) ra.addFlashAttribute("message", "Added token for " + name);
        else ra.addFlashAttribute("message", "Failed to add token");
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/delete/{id}")
    public String deleteToken(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = repository.deleteById(id);
        ra.addFlashAttribute("message", ok ? "Deleted token id=" + id : "Token id not found: " + id);
        return "redirect:/dashboard";
    }

    // Returns JSON boolean when Accept: application/json, otherwise redirect with message
    @PostMapping(value = "/dashboard/detect", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> detectJson(@RequestParam String tokenValue) {
        boolean exists = repository.existsByTokenValue(tokenValue);
        return ResponseEntity.ok().body(java.util.Map.of("exists", exists));
    }

    @PostMapping(value = "/dashboard/detect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String detectForm(@RequestParam String tokenValue, RedirectAttributes ra) {
        boolean exists = repository.existsByTokenValue(tokenValue);
        ra.addFlashAttribute("message", exists ? "Token exists" : "Token not found");
        return "redirect:/dashboard";
    }
}
