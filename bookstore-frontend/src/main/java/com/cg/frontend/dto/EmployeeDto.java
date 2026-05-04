package com.cg.frontend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeDto {

    private String empId;
    private String fname;
    private String minit;
    private String lname;
    private Short jobId;
    private Integer jobLvl;
    private String pubId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime hireDate;

    public EmployeeDto() {}

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    public String getMinit() { return minit; }
    public void setMinit(String minit) { this.minit = minit; }

    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }

    public Short getJobId() { return jobId; }
    public void setJobId(Short jobId) { this.jobId = jobId; }

    public Integer getJobLvl() { return jobLvl; }
    public void setJobLvl(Integer jobLvl) { this.jobLvl = jobLvl; }

    public String getPubId() { return pubId; }
    public void setPubId(String pubId) { this.pubId = pubId; }

    public LocalDateTime getHireDate() { return hireDate; }
    public void setHireDate(LocalDateTime hireDate) { this.hireDate = hireDate; }

    @JsonIgnore
    public String getFullName() {
        String full = (fname != null ? fname : "");
        if (minit != null && !minit.isBlank()) full += " " + minit + ".";
        full += " " + (lname != null ? lname : "");
        return full.trim();
    }

    @JsonIgnore
    public String getHireDateFormatted() {
        if (hireDate == null) return "—";
        return hireDate.toLocalDate().toString();
    }

    @JsonIgnore
    public String getHireDateTimeForInput() {
        if (hireDate == null) return "";
        // Format for datetime-local input: yyyy-MM-ddTHH:mm
        return hireDate.toString().substring(0, 16);
    }
}
