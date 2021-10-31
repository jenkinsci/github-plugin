package com.coravy.hudson.plugins.github;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hudson.MarkupText;
import hudson.plugins.git.GitChangeSet;
import java.util.ArrayList;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(DataProviderRunner.class)
public class GithubLinkAnnotatorTest {

    private final static String GITHUB_URL = "http://github.com/juretta/iphone-project-tools";
    private final static String SHA1 = "badbeef136cd854f4dd6fa40bf94c0c657681dd5";
    private final static Random RANDOM = new Random();
    private final String expectedChangeSetAnnotation = " ("
            + "<a href='" + GITHUB_URL + "/commit/" + SHA1 + "'>"
            + "commit: " + SHA1.substring(0, 7)
            + "</a>)";
    private static GitChangeSet changeSet;

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

    private static Object[] genActualAndExpected(String keyword) {
        int issueNumber = RANDOM.nextInt(1000000);
        final String innerText = keyword + " #" + issueNumber;
        final String startHREF = "<a href='" + GITHUB_URL + "/issues/" + issueNumber + "'>";
        final String endHREF = "</a>";
        final String annotatedText = startHREF + innerText + endHREF;
        return new Object[]{
            // Input text to the annotate method
            format("An issue %s link", innerText),
            // Expected result from the annotate method
            format("An issue %s link", annotatedText)
        };
    }

    @DataProvider
    public static Object[][] annotations() {
        return new Object[][]{
            genActualAndExpected("Closes"),
            genActualAndExpected("Close"),
            genActualAndExpected("closes"),
            genActualAndExpected("close")
        };
    }

    @Test
    @UseDataProvider("annotations")
    public void inputIsExpected(String input, String expected) throws Exception {
        assertThat(format("For input '%s'", input),
                annotate(input, null),
                is(expected));
    }

    @Test
    @UseDataProvider("annotations")
    public void inputIsExpectedWithChangeSet(String input, String expected) throws Exception {
        assertThat(format("For changeset input '%s'", input),
                annotate(input, changeSet),
                is(expected + expectedChangeSetAnnotation));
    }

    private String annotate(final String originalText, GitChangeSet changeSet) {
        MarkupText markupText = new MarkupText(originalText);

        GithubLinkAnnotator annotator = new GithubLinkAnnotator();
        annotator.annotate(new GithubUrl(GITHUB_URL), markupText, changeSet);

        return markupText.toString(true);
    }
}
