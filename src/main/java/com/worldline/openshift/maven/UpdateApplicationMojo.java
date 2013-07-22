package com.worldline.openshift.maven;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Update an existing application.
 */
@Mojo(name = "update-application")
public class UpdateApplicationMojo extends BaseApplicationMojo {
    @Parameter(defaultValue = "${project.packaging}")
    protected String packaging;

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

    private ScmFileSet fileSet;
    private ScmRepository repo;
    private GitExeScmProvider provider;

    @Override
    protected void doExecute(final IOpenShiftConnection connection, final IApplication application) throws MojoExecutionException {
        final String gitUrl = application.getGitUrl();

        provider = new GitExeScmProvider();
        fileSet = new ScmFileSet(workDir);
        try {
            repo = new ScmRepository("git", provider.makeProviderScmRepository(gitUrl, ':'));
        } catch (ScmRepositoryException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

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

        updateFiles();

        repo.getProviderRepository().setPushChanges(true);
        gitCommit(application.getName());
    }

    private void updateFiles() throws MojoExecutionException {
        fileSet = new ScmFileSet(workDir); // updating fileSet with new files

        // cleanup
        final File[] children = workDir.listFiles();
        if (children != null) {
            for (final File child : children) {
                final String name = child.getName();

                if (useWebapps) {
                    if ("webapps".equals(name)) {
                        try {
                            FileUtils.deleteDirectory(child);
                        } catch (final IOException e) {
                            throw new MojoExecutionException(e.getMessage(), e);
                        }
                    }
                } else if (!".git".equals(name)) {
                    try {
                        FileUtils.deleteDirectory(child);
                    } catch (final IOException e) {
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
            }
        }

        // ensure webapps exists
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

        // copy new binary
        final String extension = FileUtils.extension(binary.getName());
        if (!extension.isEmpty() // has extension
            && extension.endsWith("ar") // to avoid issues with versions in the name, we ensure that's an archive
            && !destinationName.endsWith(extension)) {
            destinationName = destinationName + '.' + extension;
        }

        try {
            final File added = new File(destinationDir, destinationName);
            if (binary.isDirectory()) {
                FileUtils.copyDirectory(binary, added);
            } else {
                FileUtils.copyFile(binary, added);
            }
            fileSet.getFileList().add(destinationDir);
            getLog().info("updated " + destinationName);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitCommit(final String app) throws MojoExecutionException {
        try {
            final CheckInScmResult push = provider.checkIn(repo, fileSet, "Updating " + app + " from openshift-maven-plugin");
            if (!push.isSuccess()) {
                getLog().info(push.getCommandOutput());
                getLog().warn("Can't push changes, [" + push.getProviderMessage() + "]");
            } else {
                if (verbose) {
                    getLog().info(push.getCommandOutput());
                }
                getLog().info("Pushed " + push.getCheckedInFiles().size() + " files");
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
                if (verbose) {
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
}
