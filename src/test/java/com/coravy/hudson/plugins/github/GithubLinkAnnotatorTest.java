package com.coravy.hudson.plugins.github;

import static org.junit.Assert.assertEquals;
import hudson.MarkupText;
import hudson.plugins.git.GitChangeSet;
import java.util.ArrayList;
import org.junit.Before;

import org.junit.Test;

public class GithubLinkAnnotatorTest {

    private final static String GITHUB_URL = "http://github.com/juretta/iphone-project-tools/";
    private final static String SHA1 = "deadbeef36cd854f4dd6fa40bf94c0c657681dd5";
    private GitChangeSet changeSet;

    @Before
    public void createChangeSet() throws Exception {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("commit " + SHA1);
        lines.add("tree 66236cf9a1ac0c589172b450ed01f019a5697c49");
        lines.add("parent e74a24e995305bd67a180f0ebc57927e2b8783ce");
        lines.add("author Author Name <author.name@nospam.com> 1363879004 +0100");
        lines.add("committer Committer Name <committer.name@nospam.com> 1364199539 -0400");
        lines.add("");
        lines.add("    Committer and author are different in this commit.");
        lines.add("");
        changeSet = new GitChangeSet(lines, true);
    }

    @Test
    public final void testAnnotateStringMarkupText() {
        assertAnnotatedTextEquals("An issue Closes #1 link",
                "An issue <a href='" + GITHUB_URL
                        + "issues/1/find'>Closes #1</a> link");
        assertAnnotatedTextEquals("An issue Close #1 link",
                "An issue <a href='" + GITHUB_URL
                        + "issues/1/find'>Close #1</a> link");
        assertAnnotatedTextEquals("An issue closes #123 link",
                "An issue <a href='" + GITHUB_URL
                        + "issues/123/find'>closes #123</a> link");
        assertAnnotatedTextEquals("An issue close #9876 link",
                "An issue <a href='" + GITHUB_URL
                        + "issues/9876/find'>close #9876</a> link");
    }

    @Test
    public final void testChangeSetAnnotateStringMarkupText() {
        final String expectedChangeSetAnnotation
                = " (<a href='" + GITHUB_URL + "commit/" + SHA1 + "'>commit: deadbee</a>)";
        assertChangeSetAnnotatedTextEquals("An issue Closes #1 link",
                "An issue <a href='" + GITHUB_URL
                + "issues/1/find'>Closes #1</a> link"
                + expectedChangeSetAnnotation);
        assertChangeSetAnnotatedTextEquals("An issue Close #1 link",
                "An issue <a href='" + GITHUB_URL
                + "issues/1/find'>Close #1</a> link"
                + expectedChangeSetAnnotation);
        assertChangeSetAnnotatedTextEquals("An issue closes #123 link",
                "An issue <a href='" + GITHUB_URL
                + "issues/123/find'>closes #123</a> link"
                + expectedChangeSetAnnotation);
        assertChangeSetAnnotatedTextEquals("An issue close #9876 link",
                "An issue <a href='" + GITHUB_URL
                + "issues/9876/find'>close #9876</a> link"
                + expectedChangeSetAnnotation);
    }

    private void assertAnnotatedTextEquals(final String originalText,
            final String expectedAnnotatedText) {
        assertEquals(expectedAnnotatedText, annotate(originalText, null));
    }

    private void assertChangeSetAnnotatedTextEquals(final String originalText,
            final String expectedAnnotatedText) {
        assertEquals(expectedAnnotatedText, annotate(originalText, changeSet));
    }

    private String annotate(final String originalText, GitChangeSet changeSet) {
        MarkupText markupText = new MarkupText(originalText);

        GithubLinkAnnotator annotator = new GithubLinkAnnotator();
        annotator.annotate(new GithubUrl(GITHUB_URL), markupText, changeSet);

        return markupText.toString(true);
    }
}
