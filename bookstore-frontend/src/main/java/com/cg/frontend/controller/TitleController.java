package com.cg.frontend.controller;

import com.cg.frontend.dto.*;
import com.cg.frontend.service.TitleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/titles")
public class TitleController {

    private final TitleService titleService;

    public TitleController(TitleService titleService) {
        this.titleService = titleService;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    public String listTitles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String search,
            Model model) {

        List<TitleDto> titles;
        PageMetaDto pageMeta;

        if (search != null && !search.isBlank()) {
            titles = titleService.searchTitles(search);
            pageMeta = new PageMetaDto();
            pageMeta.setTotalElements(titles.size());
            pageMeta.setTotalPages(1);
            pageMeta.setNumber(0);
            pageMeta.setSize(titles.size());
        } else {
            titles = titleService.getAllTitles(page, size);
            pageMeta = titleService.getPageMeta(page, size);
        }

        model.addAttribute("titles", titles);
        model.addAttribute("pageMeta", pageMeta);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("publishers", titleService.getAllPublishers());
        model.addAttribute("authors", titleService.getAllAuthors());
        model.addAttribute("pageTitle", "Titles — Tanmay's Module");
        return "titles/list";
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    @GetMapping("/{titleId}")
    public String viewTitle(@PathVariable String titleId, Model model) {
        TitleDto title = titleService.getTitleById(titleId);
        if (title == null) return "redirect:/titles";

        List<TitleAuthorDto>      titleAuthors = titleService.getAuthorsByTitle(titleId);
        List<RoySchedDto>         royScheds    = titleService.getRoySchedByTitle(titleId);
        List<Map<String, String>> allAuthors   = titleService.getAllAuthors();

        Map<String, String> authorNames = new LinkedHashMap<>();
        for (Map<String, String> a : allAuthors) {
            authorNames.put(a.get("auId"), a.get("auFname") + " " + a.get("auLname"));
        }

        Map<String, String> publisherNames = new LinkedHashMap<>();
        for (PublisherDto pub : titleService.getAllPublishers()) {
            publisherNames.put(pub.getPubId(), pub.getPubName());
        }

        model.addAttribute("title", title);
        model.addAttribute("titleAuthors", titleAuthors);
        model.addAttribute("royScheds", royScheds);
        model.addAttribute("authorNames", authorNames);
        model.addAttribute("publisherNames", publisherNames);
        model.addAttribute("publishers", titleService.getAllPublishers());
        model.addAttribute("pageTitle", "Title Detail — " + title.getTitle());
        return "titles/detail";
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping("/create")
    public String createTitle(
            @ModelAttribute TitleDto titleDto,
            @RequestParam(value = "authorIds",       required = false) List<String>  authorIds,
            @RequestParam(value = "authorRoyalties", required = false) List<Integer> authorRoyalties,
            @RequestParam(value = "rs_lorange",      required = false) List<Integer> rsLoranges,
            @RequestParam(value = "rs_hirange",      required = false) List<Integer> rsHiranges,
            @RequestParam(value = "rs_royalty",      required = false) List<Integer> rsRoyalties,
            RedirectAttributes ra) {

        if (authorIds == null || authorIds.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "At least one author is required.");
            return "redirect:/titles?error=create";
        }

        String error = titleService.createTitle(titleDto);
        if (error != null) {
            ra.addFlashAttribute("errorMsg", error);
            return "redirect:/titles?error=create";
        }

        titleService.linkAuthorsToTitle(titleDto.getTitleId(), authorIds, authorRoyalties);

        if (rsLoranges != null && !rsLoranges.isEmpty()) {
            for (int i = 0; i < rsLoranges.size(); i++) {
                RoySchedDto rs = new RoySchedDto();
                rs.setTitleId(titleDto.getTitleId());
                rs.setLorange(rsLoranges.get(i));
                rs.setHirange(rsHiranges.get(i));
                rs.setRoyalty(rsRoyalties.get(i));
                titleService.createRoySched(rs);
            }
        }

        return "redirect:/titles?success=create";
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PostMapping("/update/{titleId}")
    public String updateTitle(
            @PathVariable String titleId,
            @ModelAttribute TitleDto titleDto,
            RedirectAttributes ra) {

        titleDto.setTitleId(titleId);
        String error = titleService.updateTitle(titleId, titleDto);
        if (error != null) {
            ra.addFlashAttribute("errorMsg", error);
            return "redirect:/titles/" + titleId + "?error=update";
        }

        return "redirect:/titles/" + titleId + "?success=update";
    }

    // ── Royalty Schedule ──────────────────────────────────────────────────────

    @GetMapping("/{titleId}/royscheds")
    @ResponseBody
    public List<RoySchedDto> getRoyScheds(@PathVariable String titleId) {
        return titleService.getRoySchedByTitle(titleId);
    }

    @PostMapping("/{titleId}/royscheds")
    public String createRoyScheds(
            @PathVariable String titleId,
            @RequestParam("rs_lorange") List<Integer> loranges,
            @RequestParam("rs_hirange") List<Integer> hiranges,
            @RequestParam("rs_royalty") List<Integer> royalties,
            RedirectAttributes ra) {

        for (int i = 0; i < loranges.size(); i++) {
            RoySchedDto rs = new RoySchedDto();
            rs.setTitleId(titleId);
            rs.setLorange(loranges.get(i));
            rs.setHirange(hiranges.get(i));
            rs.setRoyalty(royalties.get(i));
            titleService.createRoySched(rs);
        }
        return "redirect:/titles/" + titleId + "?success=update";
    }

    @PostMapping("/{titleId}/royscheds/update")
    public String updateRoyScheds(
            @PathVariable String titleId,
            @RequestParam(value = "rs_id",          required = false) List<Integer> ids,
            @RequestParam(value = "rs_lorange",     required = false) List<Integer> loranges,
            @RequestParam(value = "rs_hirange",     required = false) List<Integer> hiranges,
            @RequestParam(value = "rs_royalty",     required = false) List<Integer> royalties,
            @RequestParam(value = "rs_new_lorange", required = false) List<Integer> newLoranges,
            @RequestParam(value = "rs_new_hirange", required = false) List<Integer> newHiranges,
            @RequestParam(value = "rs_new_royalty", required = false) List<Integer> newRoyalties,
            RedirectAttributes ra) {

        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                RoySchedDto rs = new RoySchedDto();
                rs.setTitleId(titleId);
                rs.setLorange(loranges.get(i));
                rs.setHirange(hiranges.get(i));
                rs.setRoyalty(royalties.get(i));
                titleService.updateRoySched(ids.get(i), rs);
            }
        }

        if (newLoranges != null) {
            for (int i = 0; i < newLoranges.size(); i++) {
                RoySchedDto rs = new RoySchedDto();
                rs.setTitleId(titleId);
                rs.setLorange(newLoranges.get(i));
                rs.setHirange(newHiranges.get(i));
                rs.setRoyalty(newRoyalties.get(i));
                titleService.createRoySched(rs);
            }
        }

        return "redirect:/titles/" + titleId + "?success=update";
    }
}