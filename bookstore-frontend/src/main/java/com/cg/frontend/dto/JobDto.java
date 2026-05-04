package com.cg.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDto {

    private Short jobId;
    private String jobDesc;
    private Integer minLvl;
    private Integer maxLvl;

    public JobDto() {}

    public Short getJobId() { return jobId; }
    public void setJobId(Short jobId) { this.jobId = jobId; }

    public String getJobDesc() { return jobDesc; }
    public void setJobDesc(String jobDesc) { this.jobDesc = jobDesc; }

    public Integer getMinLvl() { return minLvl; }
    public void setMinLvl(Integer minLvl) { this.minLvl = minLvl; }

    public Integer getMaxLvl() { return maxLvl; }
    public void setMaxLvl(Integer maxLvl) { this.maxLvl = maxLvl; }
}
