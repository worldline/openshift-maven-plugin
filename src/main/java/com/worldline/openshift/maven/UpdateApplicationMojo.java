package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Update an existing application.
 */
@Mojo(name = "update-application")
public class UpdateApplicationMojo extends BaseApplicationMojo {
    private static final String MESSAGE_PREFIX = "[openshift-maven-plugin] ";

    /**
     * Source of the files to update. Can be a .war or an exploded war.
     */
    @Parameter(property = PREFIX + "binary", defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
    protected File binary;

    /**
     * destination name in the git repository of the binary
     */
    @Parameter(property = PREFIX + "destination", defaultValue = "ROOT")
    protected String destinationName;

    /**
     * workdirectory for git commands
     */
    @Parameter(property = PREFIX + "work", defaultValue = "${project.build.directory}/openshift")
    protected File workDir;

    /**
     * are git command logged
     */
    @Parameter(property = PREFIX + "verbose", defaultValue = "false")
    protected boolean verbose;

    /**
     * are git command logged
     */
    @Parameter(property = PREFIX + "webapps", defaultValue = "true")
    protected boolean useWebapps;

    /**
     * Source of the files to update. Can be a .war or an exploded war.
     */
    @Parameter(property = PREFIX + "binary", defaultValue = "src/main/openshift")
    protected File openshift;

    private ScmFileSet fileSet;
    private ScmRepository repo;
    private GitExeScmProvider provider;

    @Override
    protected void doExecute(final IOpenShiftConnection connection, final IApplication application) throws MojoExecutionException {
        initGitObjects(application.getGitUrl());
        createWorkingCopy();
        updateFiles();

        getLog().info("Application redeployed, you can access it on " + application.getApplicationUrl());
    }

    private void initGitObjects(final String gitUrl) throws MojoExecutionException {
        provider = new GitExeScmProvider();
        fileSet = new ScmFileSet(workDir);
        try {
            repo = new ScmRepository("git", provider.makeProviderScmRepository(gitUrl, ':'));
        } catch (ScmRepositoryException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void createWorkingCopy() throws MojoExecutionException {
        if (!workDir.exists()) {
            try {
                FileUtils.forceMkdir(workDir);
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            gitClone();
        } else {
            if (!gitPull()) {
                gitClone();
            }
        }
    }

    private void updateFiles() throws MojoExecutionException {
        cleanWorkingCopy();

        // now we'll add files from openshift (src/main/openshift) and binary (war)
        fileSet = new ScmFileSet(workDir);

        // copy user openshift files
        fileSet.getFileList().addAll(copyDirectory(openshift, workDir));

        // copy new binary/binaries
        try {
            addNewFiles();

            fileSet.getFileList().clear(); // force a git commit -a
            gitCommit(application);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void addNewFiles() throws MojoExecutionException, IOException {
        final File destinationDir = createDestination();

        final File added;
        if (destinationName != null && !destinationName.isEmpty() && useWebapps) {
            // fix extension if needed
            final String extension = FileUtils.extension(binary.getName());
            if (!extension.isEmpty() // has extension
                && extension.endsWith("ar") // to avoid issues with versions in the name, we ensure that's an archive
                && !destinationName.endsWith(extension)) {
                destinationName = destinationName + '.' + extension;
            }

            added = new File(destinationDir, destinationName);
        } else {
            added = destinationDir;
        }

        if (binary.isDirectory()) {
            fileSet.getFileList().addAll(copyDirectory(binary, added));
        } else {
            FileUtils.copyFile(binary, added);
            fileSet.getFileList().add(added);
        }
        gitAdd();
    }

    private File createDestination() throws MojoExecutionException {
        final File destinationDir;
        if (useWebapps) {
            destinationDir = new File(workDir, "webapps");
            if (!destinationDir.exists()) {
                try {
                    FileUtils.forceMkdir(destinationDir);
                } catch (final Exception e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        } else {
            destinationDir = workDir;
        }
        return destinationDir;
    }

    private void cleanWorkingCopy() throws MojoExecutionException {
        fileSet = new ScmFileSet(workDir); // updating fileSet with new files
        final File[] children = workDir.listFiles();
        if (children != null) {
            for (final File child : children) {
                final String name = child.getName();

                if (useWebapps) {
                    if ("webapps".equals(name)) {
                        expandFiles(child);
                        delete(child);
                    }
                } else if (!".git".equals(name)) {
                    expandFiles(child);
                    delete(child);
                }
            }
        }
        gitRemove();
    }

    private void expandFiles(final File file) throws MojoExecutionException {
        // simulate git remove -r
        if (file.isDirectory()) {
            try {
                fileSet.getFileList().addAll(FileUtils.getFiles(file, null, null, true));
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            fileSet.getFileList().add(file);
        }
    }

    private void gitAdd() throws MojoExecutionException {
        if (fileSet.getFileList().isEmpty()) {
            return;
        }

        try {
            final AddScmResult add = provider.add(repo, fileSet, MESSAGE_PREFIX + " Adding new files to the repository before pushing a new version");
            if (!add.isSuccess()) {
                getLog().info(add.getCommandOutput());
                getLog().warn("Can't add changes, [" + add.getProviderMessage() + "]");
            } else {
                if (verbose && add.getCommandOutput() != null) {
                    getLog().info(add.getCommandOutput());
                }
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitRemove() throws MojoExecutionException {
        if (fileSet.getFileList().isEmpty()) {
            return;
        }

        // doesn't support directory (git remove -r), we expanded file before so just remove dirs here
        final Iterator<File> it = fileSet.getFileList().iterator();
        while (it.hasNext()) {
            if (it.next().isDirectory()) {
                it.remove();
            }
        }

        try {
            final RemoveScmResult remove = provider.remove(repo, fileSet, MESSAGE_PREFIX + " Cleaning the repository before pushing a new version");
            if (!remove.isSuccess()) {
                getLog().info(remove.getCommandOutput());
                getLog().warn("Can't remove changes, [" + remove.getProviderMessage() + "]");
            } else {
                if (verbose && remove.getCommandOutput() != null) {
                    getLog().info(remove.getCommandOutput());
                }
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitCommit(final String app) throws MojoExecutionException {
        repo.getProviderRepository().setPushChanges(true);

        try {
            final CheckInScmResult push = provider.checkIn(repo, fileSet, MESSAGE_PREFIX + "Redeploying " + app);
            if (!push.isSuccess()) {
                getLog().info(push.getCommandOutput());
                getLog().warn("Can't push changes, [" + push.getProviderMessage() + "]");
            } else {
                final int checkedInFilesSize = push.getCheckedInFiles().size();
                if (verbose) {
                    if (push.getCommandOutput() != null) {
                        getLog().info(push.getCommandOutput());
                    }
                    if (checkedInFilesSize > 0) {
                        getLog().info("Changes:");
                        Collections.sort(push.getCheckedInFiles(), new Comparator<ScmFile>() {
                            @Override
                            public int compare(ScmFile o1, ScmFile o2) {
                                if (o1.getStatus().equals(o2.getStatus())) {
                                    return o1.getPath().compareTo(o2.getPath());
                                }
                                return o1.getStatus().toString().compareTo(o2.getStatus().toString());
                            }
                        });

                        for (final ScmFile file : push.getCheckedInFiles()) {
                            getLog().info(SPACE + file.getStatus().toString() + " " + file.getPath());
                        }
                    }
                }
                getLog().info("Pushed " + checkedInFilesSize + " files");
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean gitPull() throws MojoExecutionException {
        try {
            final UpdateScmResult pull = provider.update(repo, fileSet);
            if (!pull.isSuccess()) {
                getLog().warn("Can't update existing repo, redoing the clone [" + pull.getProviderMessage() + "]");
                return false;
            } else {
                if (verbose) {
                    getLog().info(pull.getCommandOutput());
                }
                getLog().info("Updated " + pull.getUpdatedFiles().size() + " files");
                return true;
            }
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitClone() throws MojoExecutionException {
        try {
            final CheckOutScmResult clone = provider.checkOut(repo, fileSet);
            if (!clone.isSuccess()) {
                getLog().info(clone.getCommandOutput());
                throw new MojoExecutionException("Can't clone " + repo.toString());
            } else {
                if (verbose && clone.getCommandOutput() != null) {
                    getLog().info(clone.getCommandOutput());
                }
                getLog().info("Cloned " + clone.getCheckedOutFiles().size() + " files");
            }
        } catch (final MojoExecutionException mee) {
            throw mee;
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static void delete(final File child) throws MojoExecutionException {
        try {
            if (child.isDirectory()) {
                FileUtils.deleteDirectory(child);
            } else {
                FileUtils.forceDelete(child);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static Collection<File> copyDirectory(final File src, final File dest) throws MojoExecutionException {
        if (src == null || !src.exists()) {
            return Collections.emptyList();
        }

        final Collection<File> copied = new ArrayList<File>();
        if (src.isDirectory()) {
            if (!dest.exists() && !dest.mkdirs()) {
                throw new MojoExecutionException("Can't create " + dest.getAbsolutePath());
            }

            final String[] children = src.list();
            if (children != null) {
                for (final String file : children) {
                    final File target = new File(dest, file);
                    copied.add(target);
                    copyDirectory(new File(src, file), target);
                }
            }
        } else {
            copied.add(dest);
            try {
                FileUtils.copyFile(src, dest);
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            // keep scripts them executable
            if (dest.getName().endsWith(".sh")
                || dest.getParentFile().getName().equals("action_hooks")) {
                dest.setExecutable(true, false);
                dest.setReadable(true, false);
            }
        }
        return copied;
    }
}
