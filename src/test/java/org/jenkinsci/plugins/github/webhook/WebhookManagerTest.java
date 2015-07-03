package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.webhook.WebhookManager.forHookUrl;
import static org.junit.Assert.assertThat;
import static org.kohsuke.github.GHEvent.CREATE;
import static org.kohsuke.github.GHEvent.PULL_REQUEST;
import static org.kohsuke.github.GHEvent.PUSH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class WebhookManagerTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public static final String HOOK_ENDPOINT = "http://hook.endpoint/";

    @Spy
    private WebhookManager manager = forHookUrl(endpoint());

    @Spy
    private GitHubRepositoryName nonactive = new GitHubRepositoryName("github.com", "dummy", "dummy");

    @Spy
    private GitHubRepositoryName active = new GitHubRepositoryName("github.com", "dummy", "active");

    @Mock
    private GHRepository repo;


    @Test
    public void shouldDoNothingOnNoAdminRights() throws Exception {
        manager.unregisterFor(nonactive, newArrayList(active));
        verify(manager, times(1)).withAdminAccess();
        verify(manager, never()).fetchHooks();
    }

    @Test
    public void shouldSearchBothWebAndServiceHookOnNonActiveName() throws Exception {
        when(nonactive.resolve()).thenReturn(newArrayList(repo));
        when(repo.hasAdminAccess()).thenReturn(true);

        manager.unregisterFor(nonactive, newArrayList(active));

        verify(manager, times(1)).serviceWebhookFor(endpoint());
        verify(manager, times(1)).webhookFor(endpoint());
        verify(manager, times(1)).fetchHooks();
    }

    @Test
    public void shouldSearchOnlyServiceHookOnActiveName() throws Exception {
        when(active.resolve()).thenReturn(newArrayList(repo));
        when(repo.hasAdminAccess()).thenReturn(true);

        manager.unregisterFor(active, newArrayList(active));

        verify(manager, times(1)).serviceWebhookFor(endpoint());
        verify(manager, never()).webhookFor(endpoint());
        verify(manager, times(1)).fetchHooks();
    }

    @Test
    @WithoutJenkins
    public void shouldMatchAdminAccessWhenTrue() throws Exception {
        when(repo.hasAdminAccess()).thenReturn(true);

        assertThat("has admin access", manager.withAdminAccess().apply(repo), is(true));
    }

    @Test
    @WithoutJenkins
    public void shouldMatchAdminAccessWhenFalse() throws Exception {
        when(repo.hasAdminAccess()).thenReturn(false);

        assertThat("has no admin access", manager.withAdminAccess().apply(repo), is(false));
    }

    @Test
    @WithoutJenkins
    public void shouldMatchWebHook() {
        when(repo.hasAdminAccess()).thenReturn(false);

        GHHook hook = hook(PUSH);

        assertThat("webhook has web name and url prop", manager.webhookFor(endpoint()).apply(hook), is(true));
    }

    @Test
    public void shouldMergeEventsOnRegisterNewAndDeleteOldOnes() throws IOException {
        when(nonactive.resolve()).thenReturn(newArrayList(repo));
        when(repo.hasAdminAccess()).thenReturn(true);
        Predicate<GHHook> del = spy(Predicate.class);
        when(manager.deleteWebhook()).thenReturn(del);

        GHHook hook = hook(CREATE);
        GHHook prhook = hook(PULL_REQUEST);
        when(repo.getHooks()).thenReturn(newArrayList(hook, prhook));

        manager.createHookSubscribedTo(copyOf(newArrayList(PUSH))).apply(nonactive);
        verify(del, times(2)).apply(any(GHHook.class));
        verify(manager).createWebhook(endpoint(), EnumSet.copyOf(newArrayList(CREATE, PULL_REQUEST, PUSH)));
    }

    private URL endpoint() {
        try {
            return new URL(HOOK_ENDPOINT);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private GHHook hook(GHEvent event, GHEvent... events) {
        GHHook hook = mock(GHHook.class);
        when(hook.getName()).thenReturn("web");
        when(hook.getConfig()).thenReturn(ImmutableMap.of("url", endpoint().toExternalForm()));
        when(hook.getEvents()).thenReturn(EnumSet.copyOf(asList(event, events)));
        return hook;
    }
}
