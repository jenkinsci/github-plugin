package org.jenkinsci.plugins.github.util;

import hudson.model.Run;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import jenkins.scm.api.SCMRevisionAction;

import org.eclipse.jgit.lib.ObjectId;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Stores common methods for {@link BuildData} handling.
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 * @since 1.10
 */
public final class BuildDataHelper {
    private BuildDataHelper() {
    }

    /**
     * Gets SHA1 from the build.
     *
     * @param build
     *
     * @return SHA1 of the las
     * @throws IOException Cannot get the info about commit ID
     */
    @Nonnull
    public static String getCommitSHA1(@Nonnull Run<?, ?> build) throws IOException {
    	SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);
        if (scmRevisionAction == null) {
            throw new IOException(Messages.BuildDataHelper_NoBuildDataError());
        }

        // buildData?.lastBuild?.marked and fall back to .revision with null check everywhere to be defensive
        String b = scmRevisionAction.getRevision().toString();
        if (scmRevisionAction != null && scmRevisionAction.getRevision() != null) {
        	return scmRevisionAction.getRevision().toString();
        }

        // Nowhere to report => fail the build
        throw new IOException(Messages.BuildDataHelper_NoLastRevisionError());
    }
}
