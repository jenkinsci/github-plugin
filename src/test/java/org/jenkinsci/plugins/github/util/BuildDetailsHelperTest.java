package org.jenkinsci.plugins.github.util;

import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildDetails;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.mockito.Mock;

/**
 * @author Baptiste Gaillard <baptiste.gaillard@gmail.com>
 */
@RunWith(Enclosed.class)
public class BuildDetailsHelperTest {

    public static class WhenBuildingRegularJobs {

        private static final String GITHUB_USERNAME = "user1";

        @Mock
        private Build build;

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProject() throws Exception {
            BuildDetails projectBuildDetails = new BuildDetails(
                this.build,
                "git",
                new ArrayList<UserRemoteConfig>()
            );

            projectBuildDetails.addRemoteUrl(
                    "https://github.com/" + GITHUB_USERNAME + "/project.git");

            List<BuildDetails> buildDetailsList = new ArrayList<>();
            buildDetailsList.add(projectBuildDetails);

            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                    "master", "project/master", buildDetailsList);

            assertThat("should fetch project build data", buildDetails, is(projectBuildDetails));
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithTwoBuildDetails() throws Exception {
            BuildDetails sharedLibBuildDetails = new BuildDetails(
                this.build,
               "git",
                new ArrayList<UserRemoteConfig>()
            );
            sharedLibBuildDetails.addRemoteUrl(
                    "https://github.com/" + GITHUB_USERNAME + "/sharedLibrary.git");

            BuildDetails realProjectBuildDetails = new BuildDetails(
                this.build,
                "git",
                new ArrayList<UserRemoteConfig>()
            );
            realProjectBuildDetails.addRemoteUrl(
                    "https://github.com/" + GITHUB_USERNAME + "/project.git");

            List<BuildDetails> buildDetailsList = new ArrayList<>();

            Collections.addAll(buildDetailsList, sharedLibBuildDetails, realProjectBuildDetails);

            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                    "master", "project/master", buildDetailsList);

            assertThat("should not fetch shared library build data", buildDetails, not(sharedLibBuildDetails));
            assertThat("should fetch project build data", buildDetails, is(realProjectBuildDetails));
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithEmptyBuildDetails() throws Exception {
            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                "master", "project/master", new ArrayList<BuildDetails>()
            );

            assertThat("should be null", buildDetails, nullValue());
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithNullBuildDetails() throws Exception {
            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                "master", "project/master", null
            );

            assertThat("should be null", buildDetails, nullValue());
        }

    }

    public static class WhenBuildingOrganizationJobs {

        private static final String ORGANIZATION_NAME = "Organization";

        @Mock
        private Build build;

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProject() throws Exception {
            BuildDetails projectBuildDetails = new BuildDetails(
                this.build,
                "git",
                new ArrayList<UserRemoteConfig>()
            );
            projectBuildDetails.addRemoteUrl(
                    "https://github.com/" + ORGANIZATION_NAME + "/project.git");

            List<BuildDetails> buildDetailsList = new ArrayList<>();
            buildDetailsList.add(projectBuildDetails);

            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                "master", ORGANIZATION_NAME + "/project/master", buildDetailsList
            );

            assertThat("should fetch project build data", buildDetails, is(projectBuildDetails));
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithTwoBuildDetails() throws Exception {
            BuildDetails sharedLibBuildDetails = new BuildDetails(
                this.build,
                "git",
                new ArrayList<UserRemoteConfig>()
            );
            sharedLibBuildDetails.addRemoteUrl(
                    "https://github.com/" + ORGANIZATION_NAME + "/sharedLibrary.git");

            BuildDetails realProjectBuildDetails = new BuildDetails(
                this.build,
               "git",
                new ArrayList<UserRemoteConfig>()
            );
            realProjectBuildDetails.addRemoteUrl(
                    "https://github.com/" + ORGANIZATION_NAME + "/project.git");

            List<BuildDetails> buildDetailsList = new ArrayList<>();

            Collections.addAll(buildDetailsList, sharedLibBuildDetails, realProjectBuildDetails);

            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                "master", ORGANIZATION_NAME + "/project/master", buildDetailsList
            );

            assertThat("should not fetch shared library build data", buildDetails, not(sharedLibBuildDetails));
            assertThat("should fetch project build data", buildDetails, is(realProjectBuildDetails));
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithEmptyBuildDetails() throws Exception {
            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                "master", ORGANIZATION_NAME + "/project/master", new ArrayList<BuildDetails>()
            );

            assertThat("should be null", buildDetails, nullValue());
        }

        @Test
        @Issue("JENKINS-53149")
        public void shouldCalculateDataBuildFromProjectWithNullBuildDetails() throws Exception {
            BuildDetails buildDetails = BuildDetailsHelper.calculateBuildDetails(
                    "master", ORGANIZATION_NAME + "/project/master", null);

            assertThat("should be null", buildDetails, nullValue());
        }

    }

}
