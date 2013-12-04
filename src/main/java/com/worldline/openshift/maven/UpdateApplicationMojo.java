package com.worldline.openshift.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.SshSessionFactory;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;

/**
 * Update an existing application.
 */
@Mojo(name = "update-application")
public class UpdateApplicationMojo extends BaseApplicationMojo {

	static {
		setSshHttpProxy();
	}

	/**
	 * Set http proxy for the ssh session if "sshHttpProxyHost" is set
	 */
	private static void setSshHttpProxy() {
		String sshProxyHTTPHost= System.getProperty("sshHttpProxyHost");
		if (sshProxyHTTPHost == null) {
			return;
		}
		String sshProxyHTTPPortStr = System.getProperty("sshHttpProxyPort");
		int sshProxyHTTPPort = 3128;
		try {
			sshProxyHTTPPort = Integer.valueOf(sshProxyHTTPPortStr);
		} catch (NumberFormatException nfe) {
			sshProxyHTTPPort = 3128;
		}
		SSHoverHttpSessionFactory sessionFactory = new SSHoverHttpSessionFactory(sshProxyHTTPHost, sshProxyHTTPPort);
		sessionFactory.Initialize();
		SshSessionFactory.setInstance(sessionFactory);				
	}
	
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

    private Repository jgitRepo;
    private Git jgit;
    private String gitUrl;

    @Override
    protected void doExecute(final IOpenShiftConnection connection, final IApplication application) throws MojoExecutionException {
    	getLog().info("Deploying on Openshift server:"+connection.getServer()+" app="+application.getName());
        
    	gitUrl = application.getGitUrl();
        
        cloneOrPullWorkingCopy();
        
        deployFiles();

        getLog().info("Application redeployed, you can access it on " + application.getApplicationUrl());
    }

    private void initJGitObjects() throws MojoExecutionException {
		try {
	    	FileRepositoryBuilder builder = new FileRepositoryBuilder();
			jgitRepo = builder.setGitDir(new File(workDir,Constants.DOT_GIT)).readEnvironment().findGitDir().build();
		} catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
		}
		jgit = new Git(jgitRepo);
    }

    private void cloneOrPullWorkingCopy() throws MojoExecutionException {
    	getLog().info("Creating working copy in "+workDir.getAbsolutePath());
        if (!workDir.exists()) {
            try {
                FileUtils.forceMkdir(workDir);
                initJGitObjects();
            } catch (final IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            gitClone();
        } else {
        	initJGitObjects();
        	
            if (!gitPull()) {
                gitClone();
            }
        }
    }

    private void deployFiles() throws MojoExecutionException {
    	getLog().info("Updating files");
    	
    	//Remove webapps directory or every files
        cleanWorkingCopy();

        
        
        //Copy file from openshift config dir to repo
        copyDirectory(openshift, workDir);

        try {
        	gitAddCommitNewFiles();
            gitPush(application);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitAddCommitNewFiles() throws MojoExecutionException, IOException {
    	getLog().info("Adding files to git");
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
        getLog().info("Added "+added.getAbsolutePath()+ " to the update");

        if (binary.isDirectory()) {
        	copyDirectory(binary, added);
        } else {
            FileUtils.copyFile(binary, added);
        }
        gitAddCommit();
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
    	getLog().info("Clearing work directory");
        final File[] children = workDir.listFiles();
        if (children != null) {
            for (final File child : children) {
                final String name = child.getName();
                if (useWebapps) {
                    if ("webapps".equals(name)) {
                        delete(child, getLog());
                    }
                } else if (!".git".equals(name)) {
                    delete(child, getLog());
                }
            }
        }
        gitRm();
    }

    private void gitAddCommit() throws MojoExecutionException {
        try {
        	AddCommand addcommand = jgit.add();
			addcommand.addFilepattern(".").call();
        	
			CommitCommand commit = jgit.commit();
			commit.setMessage(MESSAGE_PREFIX + " Adding new files to the repository before pushing a new version").call();
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitRm() throws MojoExecutionException {
        try {
        	//git status
        	StatusCommand statusCmd = jgit.status();
        	Status status = statusCmd.call();
        	//Status.getMissing() contains files marked as locally deleted (not yet deleted in index)
        	//So we need to make "git rm" on each file 
        	if(!status.getMissing().isEmpty()){
            	RmCommand rmCommand = jgit.rm();
	        	for (String unstagedFile : status.getMissing()) {
					getLog().info("JGIT : Removing "+unstagedFile);
	        		rmCommand.addFilepattern(unstagedFile);
				}
	        	//git rm <each deleted file>
	        	rmCommand.call();
	        	
	        	//git commit
	        	RevCommit rmCommit = jgit.commit().setMessage( MESSAGE_PREFIX + " Cleaning the repository before pushing a new version").call();
	        	getLog().info("Just commited remove file : "+rmCommit.getName());
	        	
	        	//Show 5 last logs on output
	        	LogCommand log = jgit.log().setMaxCount(5);
	        	for (RevCommit commit : log.call()) {
	        		getLog().info("log : "+commit.name()+" - "+commit.getShortMessage());
				}
        	}
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitPush(final String app) throws MojoExecutionException {
    	PushCommand pushCmd = jgit.push()
    			.setForce(true)
    			.setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
    			.setPushAll()
    			.setOutputStream(System.out);
    	try {
			Iterable<PushResult> callResults = pushCmd.call();
			for (PushResult pushResult : callResults) {
				getLog().info(pushResult.getMessages());
			}
		} catch (InvalidRemoteException e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (TransportException e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (GitAPIException e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
    }

    private boolean gitPull() throws MojoExecutionException {
    	getLog().info("Pull changes from "+jgit.getRepository().toString());
        try {
        	PullCommand command = jgit.pull().setRebase(true);
        	PullResult pullResult = command.call();
        	if(!pullResult.isSuccessful()){
                getLog().warn("Can't update existing repo, redoing the clone [" + pullResult.toString() + "]");
                return false;
        	} else {
        		if (verbose) {
                    getLog().info(command.toString());
                }
                getLog().info("Updated files from "+ pullResult.getFetchedFrom());
                return true;
        	}
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void gitClone() throws MojoExecutionException {
    	getLog().info("Cloning master branch from "+gitUrl);
    	getLog().info("   into "+workDir.getAbsolutePath());
    	
        try {
        	Git.cloneRepository().setURI(gitUrl).setDirectory(workDir).setBranch(Constants.MASTER).call();
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static void delete(final File child, Log log) throws MojoExecutionException {
        try {
        	log.info("Deleting tree "+child.getAbsolutePath());
            if (child.isDirectory()) {
            	log.info("Deleting directory "+child.getAbsolutePath());
                FileUtils.deleteDirectory(child);
            } else {
            	log.info("Deleting file "+child.getAbsolutePath());
                FileUtils.forceDelete(child);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static List<File> copyDirectory(final File src, final File dest) throws MojoExecutionException {
        if (src == null || !src.exists()) {
            return Collections.emptyList();
        }

        final List<File> copied = new ArrayList<File>();
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
