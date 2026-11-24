package com.example.pravin_quotation.config;

import com.example.pravin_quotation.util.DimensionFormatter;
import com.example.pravin_quotation.util.IndianCurrencyFormatter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final IndianCurrencyFormatter inr;
    private final DimensionFormatter dimFormatter;

    public GlobalModelAdvice(IndianCurrencyFormatter inr, DimensionFormatter dimFormatter) {
        this.inr = inr;
        this.dimFormatter = dimFormatter;
    }

    @ModelAttribute("inr")
    public IndianCurrencyFormatter inr() {
        return this.inr;
    }

    @ModelAttribute("dimFormatter")
    public DimensionFormatter dimFormatter() {
        return this.dimFormatter;
    }

    @ModelAttribute("dimensionFormatter")
    public DimensionFormatter dimensionFormatter() {
        return this.dimFormatter;
    }
}

