package com.bocom.codereview.service;


import com.bocom.codereview.entity.ChangedFile;
import com.bocom.codereview.entity.GitCommit;
import com.bocom.codereview.util.ContextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitService {

    private final ContextExtractor contextExtractor;

    public List<GitCommit> getDailyCommits(String repoDir, Date targetDate) {
        List<GitCommit> commitList = new ArrayList<>();
        try (Git git = Git.open(new File(repoDir))) {
            Repository repo = git.getRepository();
            Iterable<RevCommit> commits = git.log().all().call();

            for (RevCommit revCommit : commits) {
                Date commitTime = revCommit.getAuthorIdent().getWhen();
                if (!isSameDay(commitTime, targetDate)) {
                    continue;
                }

                GitCommit gitCommit = new GitCommit();
                gitCommit.setCommitId(revCommit.getName());
                gitCommit.setAuthor(revCommit.getAuthorIdent().getName());
                gitCommit.setCommitTime(commitTime);
                gitCommit.setMessage(revCommit.getFullMessage());
                gitCommit.setChangedFiles(getChangedFiles(git, repo, revCommit, repoDir));
                commitList.add(gitCommit);
            }
        } catch (Exception e) {
            log.error("获取 Git Commit 失败", e);
        }
        return commitList;
    }

    private List<ChangedFile> getChangedFiles(Git git, Repository repo, RevCommit commit, String repoDir) throws GitAPIException, IOException {
        List<ChangedFile> changedFiles = new ArrayList<>();
        RevWalk revWalk = new RevWalk(repo);

        RevTree currentTree = revWalk.parseTree(commit.getTree().getId());
        if (commit.getParentCount() == 0) {
            return changedFiles;
        }
        RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
        RevTree parentTree = revWalk.parseTree(parentCommit.getTree().getId());

        CanonicalTreeParser parentIter = new CanonicalTreeParser();
        parentIter.reset(repo.newObjectReader(), parentTree.getId());
        CanonicalTreeParser currentIter = new CanonicalTreeParser();
        currentIter.reset(repo.newObjectReader(), currentTree.getId());

        List<DiffEntry> diffEntries = git.diff()
                .setOldTree(parentIter)
                .setNewTree(currentIter)
                .call();

        for (DiffEntry entry : diffEntries) {
            ChangedFile changedFile = new ChangedFile();
            changedFile.setFilePath(entry.getNewPath());
            changedFile.setChangeType(entry.getChangeType().name());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            git.diff()
                    .setOldTree(parentIter)
                    .setNewTree(currentIter)
                    .setPathFilter(PathFilter.create(entry.getNewPath()))
                    .setOutputStream(out)
                    .call();
            changedFile.setRawDiff(out.toString());

            changedFile.setChangedLines(new ArrayList<>());

            String fullFilePath = repoDir + File.separator + entry.getNewPath();
            changedFile.setContext(contextExtractor.extractContext(fullFilePath, changedFile.getChangedLines()));

            changedFiles.add(changedFile);
        }
        return changedFiles;
    }

    private boolean isSameDay(Date date1, Date date2) {
        return date1.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .equals(date2.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }
}
