package com.cg.frontend.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoySchedDto {
    private Integer roySchedId;
    private String titleId;
    private Integer lorange;
    private Integer hirange;
    private Integer royalty;

    public RoySchedDto() {}

    public Integer getRoySchedId() { return roySchedId; }
    public void setRoySchedId(Integer roySchedId) { this.roySchedId = roySchedId; }
    public String getTitleId() { return titleId; }
    public void setTitleId(String titleId) { this.titleId = titleId; }
    public Integer getLorange() { return lorange; }
    public void setLorange(Integer lorange) { this.lorange = lorange; }
    public Integer getHirange() { return hirange; }
    public void setHirange(Integer hirange) { this.hirange = hirange; }
    public Integer getRoyalty() { return royalty; }
    public void setRoyalty(Integer royalty) { this.royalty = royalty; }
}