package com.cg.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TitleDto {

    private String titleId;
    private String title;
    private String type;
    private BigDecimal price;
    private BigDecimal advance;
    private Integer royalty;
    private Integer ytdSales;
    private String notes;
    private String pubdate;

    // Populated from form submission
    private String pubId;

    // Populated by TitleService.parseTitle() — kept for potential future use
    private PublisherDto publisher;

    public String getTitleId()                   { return titleId; }
    public void   setTitleId(String v)           { this.titleId = v; }
    public String getTitle()                     { return title; }
    public void   setTitle(String v)             { this.title = v; }
    public String getType()                      { return type; }
    public void   setType(String v)              { this.type = v; }
    public BigDecimal getPrice()                 { return price; }
    public void       setPrice(BigDecimal v)     { this.price = v; }
    public BigDecimal getAdvance()               { return advance; }
    public void       setAdvance(BigDecimal v)   { this.advance = v; }
    public Integer getRoyalty()                  { return royalty; }
    public void    setRoyalty(Integer v)         { this.royalty = v; }
    public Integer getYtdSales()                 { return ytdSales; }
    public void    setYtdSales(Integer v)        { this.ytdSales = v; }
    public String getNotes()                     { return notes; }
    public void   setNotes(String v)             { this.notes = v; }
    public String getPubdate()                   { return pubdate; }
    public void   setPubdate(String v)           { this.pubdate = v; }
    public String getPubId()                     { return pubId; }
    public void   setPubId(String v)             { this.pubId = v; }
    public PublisherDto getPublisher()           { return publisher; }
    public void         setPublisher(PublisherDto v) { this.publisher = v; }

    /**
     * Checks pubId first (form submission), then falls back to the
     * publisher object (parsed from response). This order matters —
     * pubId from form is always the most direct source.
     */
    public String getResolvedPubId() {
        if (pubId != null && !pubId.isBlank()) return pubId;
        if (publisher != null && publisher.getPubId() != null) return publisher.getPubId();
        return null;
    }
}