package com.coravy.hudson.plugins.github;

import static org.junit.Assert.assertEquals;
import hudson.MarkupText;

import org.junit.Test;

public class GithubLinkAnnotatorTest {

    private final static String GITHUB_URL = "http://github.com/juretta/iphone-project-tools/";

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

    private void assertAnnotatedTextEquals(final String originalText,
            final String expectedAnnotatedText) {
        MarkupText markupText = new MarkupText(originalText);

        GithubLinkAnnotator annotator = new GithubLinkAnnotator();
        annotator.annotate(new GithubUrl(GITHUB_URL), markupText, null);

        assertEquals(expectedAnnotatedText, markupText.toString());
    }
}
