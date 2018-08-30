package org.jenkinsci.plugins.github.util;

import hudson.plugins.git.util.BuildData;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Manuel de la Peña
 */
@RunWith(Enclosed.class)
public class BuildDataHelperTest {

	public static class WhenBuildingRegularJobs {

		private static final String GITHUB_USERNAME = "user1";

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithTwoBuildDatas() throws Exception {
			BuildData sharedLibBuildData = new BuildData();
			sharedLibBuildData.remoteUrls = new HashSet<>();

			sharedLibBuildData.addRemoteUrl(
				"https://github.com/" + GITHUB_USERNAME + "/sharedLibrary.git");

			BuildData realProjectBuildData = new BuildData();
			realProjectBuildData.remoteUrls = new HashSet<>();

			realProjectBuildData.addRemoteUrl(
				"https://github.com/" + GITHUB_USERNAME + "/project.git");

			List<BuildData> buildDataList = new ArrayList<>();

			Collections.addAll(buildDataList, sharedLibBuildData, realProjectBuildData);

			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", "project/master", buildDataList);

			assertThat("should not fetch shared library build data", buildData, not(sharedLibBuildData));
			assertThat("should fetch project build data", buildData, is(realProjectBuildData));
		}

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithEmptyBuildDatas() throws Exception {
			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", "project/master", Collections.EMPTY_LIST);

			assertThat("should be null", buildData, nullValue());
		}

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithNullBuildDatas() throws Exception {
			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", "project/master", null);

			assertThat("should be null", buildData, nullValue());
		}

	}

	public static class WhenBuildingOrganizationJobs {

		private static final String ORGANIZATION_NAME = "Organization";

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithTwoBuildDatas() throws Exception {
			BuildData sharedLibBuildData = new BuildData();
			sharedLibBuildData.remoteUrls = new HashSet<>();

			sharedLibBuildData.addRemoteUrl(
				"https://github.com/" + ORGANIZATION_NAME + "/sharedLibrary.git");

			BuildData realProjectBuildData = new BuildData();
			realProjectBuildData.remoteUrls = new HashSet<>();

			realProjectBuildData.addRemoteUrl(
				"https://github.com/" + ORGANIZATION_NAME + "/project.git");

			List<BuildData> buildDataList = new ArrayList<>();

			Collections.addAll(buildDataList, sharedLibBuildData, realProjectBuildData);

			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", ORGANIZATION_NAME + "/project/master", buildDataList);

			assertThat("should not fetch shared library build data", buildData, not(sharedLibBuildData));
			assertThat("should fetch project build data", buildData, is(realProjectBuildData));
		}

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithEmptyBuildDatas() throws Exception {
			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", ORGANIZATION_NAME + "/project/master", Collections.EMPTY_LIST);

			assertThat("should be null", buildData, nullValue());
		}

		@Test
		@Issue("JENKINS-53149")
		public void shouldCalculateDataBuildFromProjectWithNullBuildDatas() throws Exception {
			BuildData buildData = BuildDataHelper.calculateBuildData(
				"master", ORGANIZATION_NAME + "/project/master", null);

			assertThat("should be null", buildData, nullValue());
		}

	}

}