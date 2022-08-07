package org.jenkinsci.plugins.github.util;

import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import org.eclipse.jgit.lib.ObjectId;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Stores common methods for {@link BuildData} handling.
 *
 * @author <a href="mailto:o.v.nenashev@gmail.com">Oleg Nenashev</a>
 * @since 1.10
 */
public final class BuildDataHelper {
    private BuildDataHelper() {
    }

    /**
     * Calculate build data from downstream builds, that could be a shared library
     * which is loaded first in a pipeline. For that reason, this method compares
     * all remote URLs for each build data, with the real project name, to determine
     * the proper build data. This way, the SHA returned in the build data will
     * relate to the project
     *
     * @param parentName name of the parent build
     * @param parentFullName full name of the parent build
     * @param buildDataList the list of build datas from a build run
     * @return the build data related to the project, null if not found
     */
    public static BuildData calculateBuildData(
        String parentName, String parentFullName, List<BuildData> buildDataList
    ) {

        if (buildDataList == null) {
            return null;
        }

        if (buildDataList.size() == 1) {
            return buildDataList.get(0);
        }

        String projectName = parentFullName.replace(parentName, "");

        if (projectName.endsWith("/")) {
            projectName = projectName.substring(0, projectName.lastIndexOf('/'));
        }

        for (BuildData buildData : buildDataList) {
            Set<String> remoteUrls = buildData.getRemoteUrls();

            for (String remoteUrl : remoteUrls) {
                if (remoteUrl.contains(projectName)) {
                    return buildData;
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
     * @return SHA1 of the las
     * @throws IOException Cannot get the info about commit ID
     */
    @NonNull
    public static ObjectId getCommitSHA1(@NonNull Run<?, ?> build) throws IOException {
        List<BuildData> buildDataList = build.getActions(BuildData.class);

        Job<?, ?> parent = build.getParent();

        BuildData buildData = calculateBuildData(
            parent.getName(), parent.getFullName(), buildDataList
        );

        if (buildData == null) {
            throw new IOException(Messages.BuildDataHelper_NoBuildDataError());
        }

        // buildData?.lastBuild?.marked and fall back to .revision with null check everywhere to be defensive
        Build b = buildData.lastBuild;
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
        throw new IOException(Messages.BuildDataHelper_NoLastRevisionError());
    }
}
