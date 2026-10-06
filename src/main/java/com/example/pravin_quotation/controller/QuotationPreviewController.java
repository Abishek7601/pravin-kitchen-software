package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.service.QuotationItemService;
import com.example.pravin_quotation.service.QuotationRoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/quotation-preview")
public class QuotationPreviewController {

    private final QuotationRepository quotationRepository;
    private final QuotationRoomService quotationRoomService;
    private final QuotationItemService quotationItemService;

    public QuotationPreviewController(
            QuotationRepository quotationRepository,
            QuotationRoomService quotationRoomService,
            QuotationItemService quotationItemService) {

        this.quotationRepository = quotationRepository;
        this.quotationRoomService = quotationRoomService;
        this.quotationItemService = quotationItemService;
    }

    @GetMapping("/{id}")
    public String previewQuotation(
            @PathVariable Long id,
            Model model) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required.");
        }

        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."));

        List<QuotationRoom> rooms =
                quotationRoomService.getByQuotationId(id);

        Map<Long, Object> roomItems = new LinkedHashMap<>();

        for (QuotationRoom room : rooms) {
            roomItems.put(
                    room.getId(),
                    quotationItemService.getByQuotationRoomId(
                            room.getId()
                    )
            );
        }

        model.addAttribute("quotation", quotation);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomItems", roomItems);

        return "admin/quotation-preview";
    }
}