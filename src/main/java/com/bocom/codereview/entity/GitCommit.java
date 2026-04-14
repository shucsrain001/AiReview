package com.bocom.codereview.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class GitCommit {
    private String commitId;
    private String author;
    private Date commitTime;
    private String message;
    private List<ChangedFile> changedFiles;
}