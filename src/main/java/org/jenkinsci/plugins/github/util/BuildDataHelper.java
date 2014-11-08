package org.jenkinsci.plugins.github.util;

import hudson.model.AbstractBuild;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Stores common methods for {@link BuildData} handling.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 * @since 1.10
 */
public class BuildDataHelper {
    
    /**
     * Gets SHA1 from the build.
     * @param build
     * @return SHA1 of the las
     * @throws IOException Cannot get the info about commit ID
     */
    public static @Nonnull ObjectId getCommitSHA1(@Nonnull AbstractBuild<?, ?> build) throws IOException {
        BuildData buildData = build.getAction(BuildData.class);
        if (buildData == null) {
            throw new IOException(Messages.BuildDataHelper_NoBuildDataError());
        }
        final Revision lastBuildRevision = buildData.getLastBuiltRevision();
        final ObjectId sha1 = lastBuildRevision != null ? lastBuildRevision.getSha1() : null;
        if (sha1 == null) { // Nowhere to report => fail the build
            throw new IOException(Messages.BuildDataHelper_NoLastRevisionError());
        }
        return sha1;
    }
}
