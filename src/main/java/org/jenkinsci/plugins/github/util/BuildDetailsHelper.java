package org.jenkinsci.plugins.github.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.eclipse.jgit.lib.ObjectId;

import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Revision;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.BuildDetails;

/**
 * Stores common methods for {@link BuildDetails} handling.
 *
 * @author Baptiste Gaillard <baptiste.gaillard@gmail.com>
 * @since 1.30.0
 */
public final class BuildDetailsHelper {

    private BuildDetailsHelper() {
    }

    /**
     * Calculate build data from downstream builds, that could be a shared library which is loaded first in a pipeline.
     * For that reason, this method compares all remote URLs for each build data, with the real project name, to
     * determine the proper build data. This way, the SHA returned in the build data will relate to the project.
     *
     * @param parentName name of the parent build.
     * @param parentFullName full name of the parent build.
     * @param buildDetailsList the list of build details from a build run.
     *
     * @return the build data related to the project, null if not found.
     */
    public static BuildDetails calculateBuildDetails(
        String parentName, String parentFullName, List<BuildDetails> buildDetailsList
    ) {
        if (buildDetailsList == null) {
            return null;
        }

        if (buildDetailsList.size() == 1) {
            return buildDetailsList.get(0);
        }

        String projectName = parentFullName.replace(parentName, "");

        if (projectName.endsWith("/")) {
            projectName = projectName.substring(0, projectName.lastIndexOf('/'));
        }

        for (BuildDetails buildDetails : buildDetailsList) {
            Set<String> remoteUrls = buildDetails.getRemoteUrls();

            for (String remoteUrl : remoteUrls) {
                if (remoteUrl.contains(projectName)) {
                    return buildDetails;
                }
            }
        }

        return null;
    }

    /**
     * Gets SHA1 from the build.
     *
     * @param build
     *
     * @return SHA1 of the last build.
     *
     * @throws IOException Cannot get the info about commit ID.
     */
    @Nonnull
    public static ObjectId getCommitSHA1(@Nonnull Run<?, ?> build) throws IOException {

        Job<?, ?> parent = build.getParent();

        List<BuildDetails> buildDetailsList = build.getActions(BuildDetails.class);

        // If we cannot get build data from a 'BuildDetails' action then we try to get it from a 'BuildData' action.
        // This should not be possible in the future as the 'BuildData' class will be deprecated.
        if (buildDetailsList.isEmpty()) {
            buildDetailsList = new ArrayList<>();

            for (BuildData buildData : build.getActions(BuildData.class)) {
                Collection<UserRemoteConfig> remoteConfigs = new ArrayList<>();

                for (String url : buildData.getRemoteUrls()) {
                    remoteConfigs.add(new UserRemoteConfig(url, null, null, null));
                }

                buildDetailsList.add(new BuildDetails(buildData.lastBuild, buildData.scmName, remoteConfigs));
            }
        }

        BuildDetails buildDetails = calculateBuildDetails(parent.getName(), parent.getFullName(), buildDetailsList);

        if (buildDetails == null) {
            throw new IOException(Messages.BuildDetailsHelper_NoBuildDataError());
        }

        // buildData?.lastBuild?.marked and fall back to .revision with null check everywhere to be defensive
        Build b = buildDetails.getBuild();
        if (b != null) {
            Revision r = b.marked;
            if (r == null) {
                r = b.revision;
            }
            if (r != null) {
                return r.getSha1();
            }
        }

        // Nowhere to report => fail the build
        throw new IOException(Messages.BuildDetailsHelper_NoLastRevisionError());
    }
}
