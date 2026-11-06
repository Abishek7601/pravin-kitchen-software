package com.example.pravin_quotation.config;

import com.example.pravin_quotation.util.IndianCurrencyFormatter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final IndianCurrencyFormatter inr;

    public GlobalModelAdvice(IndianCurrencyFormatter inr) {
        this.inr = inr;
    }

    @ModelAttribute("inr")
    public IndianCurrencyFormatter inr() {
        return this.inr;
    }
}

