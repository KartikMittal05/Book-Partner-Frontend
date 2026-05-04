package com.cg.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublisherDtoForEmployee {

    private String pubId;
    private String pubName;
    private String city;
    private String state;
    private String country;

    public PublisherDtoForEmployee() {}

    public String getPubId()   { return pubId; }
    public void   setPubId(String pubId) { this.pubId = pubId; }

    public String getPubName()   { return pubName; }
    public void   setPubName(String pubName) { this.pubName = pubName; }

    public String getCity()   { return city; }
    public void   setCity(String city) { this.city = city; }

    public String getState()   { return state; }
    public void   setState(String state) { this.state = state; }

    public String getCountry()   { return country; }
    public void   setCountry(String country) { this.country = country; }

    /** e.g. "Boston, MA, USA" — only non-null parts joined */
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        if (city    != null && !city.isBlank())    sb.append(city);
        if (state   != null && !state.isBlank())   { if (sb.length() > 0) sb.append(", "); sb.append(state); }
        if (country != null && !country.isBlank()) { if (sb.length() > 0) sb.append(", "); sb.append(country); }
        return sb.length() > 0 ? sb.toString() : "—";
    }
}