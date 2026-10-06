package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.service.QuotationRoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/quotation-rooms")
public class QuotationRoomController {

    private final QuotationRoomService quotationRoomService;
    private final QuotationRepository quotationRepository;

    public QuotationRoomController(
            QuotationRoomService quotationRoomService,
            QuotationRepository quotationRepository) {

        this.quotationRoomService = quotationRoomService;
        this.quotationRepository = quotationRepository;
    }


    // =========================================================
    // LIST ROOMS FOR A QUOTATION
    // =========================================================

    @GetMapping("/quotation/{quotationId}")
    public String listRooms(
            @PathVariable Long quotationId,
            Model model) {

        Quotation quotation = getQuotation(quotationId);

        model.addAttribute("quotation", quotation);
        model.addAttribute(
                "rooms",
                quotationRoomService.getActiveByQuotationId(quotationId)
        );

        return "admin/quotation-rooms";
    }


    // =========================================================
    // NEW ROOM FORM
    // =========================================================

    @GetMapping("/new/{quotationId}")
    public String newRoom(
            @PathVariable Long quotationId,
            Model model) {

        Quotation quotation = getQuotation(quotationId);

        QuotationRoom quotationRoom = new QuotationRoom();
        quotationRoom.setQuotation(quotation);

        model.addAttribute("quotation", quotation);
        model.addAttribute("quotationRoom", quotationRoom);

        return "admin/quotation-room-form";
    }


    // =========================================================
    // SAVE ROOM
    // =========================================================

    @PostMapping("/save")
    public String saveRoom(
            @RequestParam Long quotationId,
            @RequestParam String floor,
            @RequestParam String room,
            @RequestParam(required = false) String workDescription) {

        Quotation quotation = getQuotation(quotationId);

        quotationRoomService.create(
                quotation,
                floor,
                room,
                workDescription
        );

        return "redirect:/admin/quotation-rooms/quotation/"
                + quotationId;
    }


    // =========================================================
    // EDIT ROOM
    // =========================================================

    @GetMapping("/edit/{id}")
    public String editRoom(
            @PathVariable Long id,
            Model model) {

        QuotationRoom quotationRoom =
                quotationRoomService.getById(id);

        model.addAttribute(
                "quotationRoom",
                quotationRoom
        );

        model.addAttribute(
                "quotation",
                quotationRoom.getQuotation()
        );

        return "admin/quotation-room-form";
    }


    // =========================================================
    // UPDATE ROOM
    // =========================================================

    @PostMapping("/update")
    public String updateRoom(
            @RequestParam Long id,
            @RequestParam String floor,
            @RequestParam String room,
            @RequestParam(required = false) String workDescription) {

        QuotationRoom quotationRoom =
                quotationRoomService.update(
                        id,
                        floor,
                        room,
                        workDescription
                );

        return "redirect:/admin/quotation-rooms/quotation/"
                + quotationRoom.getQuotation().getId();
    }


    // =========================================================
    // ACTIVATE ROOM
    // =========================================================

    @PostMapping("/activate/{id}")
    public String activateRoom(
            @PathVariable Long id) {

        QuotationRoom quotationRoom =
                quotationRoomService.activate(id);

        return "redirect:/admin/quotation-rooms/quotation/"
                + quotationRoom.getQuotation().getId();
    }


    // =========================================================
    // DEACTIVATE ROOM
    // =========================================================

    @PostMapping("/deactivate/{id}")
    public String deactivateRoom(
            @PathVariable Long id) {

        QuotationRoom quotationRoom =
                quotationRoomService.deactivate(id);

        return "redirect:/admin/quotation-rooms/quotation/"
                + quotationRoom.getQuotation().getId();
    }


    // =========================================================
    // DELETE ROOM
    // =========================================================

    @PostMapping("/delete/{id}")
    public String deleteRoom(
            @PathVariable Long id) {

        QuotationRoom quotationRoom =
                quotationRoomService.getById(id);

        Long quotationId =
                quotationRoom.getQuotation().getId();

        quotationRoomService.delete(id);

        return "redirect:/admin/quotation-rooms/quotation/"
                + quotationId;
    }


    // =========================================================
    // GET QUOTATION
    // =========================================================

    private Quotation getQuotation(Long quotationId) {

        if (quotationId == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required."
            );
        }

        return quotationRepository.findById(quotationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."
                        ));
    }
}