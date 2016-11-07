package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecretIn;
import static org.jenkinsci.plugins.github.webhook.WebhookManager.forHookUrl;
import static org.junit.Assert.assertThat;
import static org.kohsuke.github.GHEvent.CREATE;
import static org.kohsuke.github.GHEvent.PULL_REQUEST;
import static org.kohsuke.github.GHEvent.PUSH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
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

    public static final GitSCM GIT_SCM = new GitSCM("ssh://git@github.com/dummy/dummy.git");
    public static final URL HOOK_ENDPOINT = endpoint("http://hook.endpoint/");
    public static final URL ANOTHER_HOOK_ENDPOINT = endpoint("http://another.url/");

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Spy
    private WebhookManager manager = forHookUrl(HOOK_ENDPOINT);

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
        doReturn(newArrayList(repo)).when(nonactive).resolve(any(Predicate.class));
        when(repo.hasAdminAccess()).thenReturn(true);

        manager.unregisterFor(nonactive, newArrayList(active));

        verify(manager, times(1)).serviceWebhookFor(HOOK_ENDPOINT);
        verify(manager, times(1)).webhookFor(HOOK_ENDPOINT);
        verify(manager, times(1)).fetchHooks();
    }

    @Test
    public void shouldSearchOnlyServiceHookOnActiveName() throws Exception {
        doReturn(newArrayList(repo)).when(active).resolve(any(Predicate.class));
        when(repo.hasAdminAccess()).thenReturn(true);

        manager.unregisterFor(active, newArrayList(active));

        verify(manager, times(1)).serviceWebhookFor(HOOK_ENDPOINT);
        verify(manager, never()).webhookFor(HOOK_ENDPOINT);
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

        GHHook hook = hook(HOOK_ENDPOINT, PUSH);

        assertThat("webhook has web name and url prop", manager.webhookFor(HOOK_ENDPOINT).apply(hook), is(true));
    }

    @Test
    @WithoutJenkins
    public void shouldNotMatchOtherUrlWebHook() {
        when(repo.hasAdminAccess()).thenReturn(false);

        GHHook hook = hook(ANOTHER_HOOK_ENDPOINT, PUSH);

        assertThat("webhook has web name and another url prop",
                manager.webhookFor(HOOK_ENDPOINT).apply(hook), is(false));
    }

    @Test
    public void shouldMergeEventsOnRegisterNewAndDeleteOldOnes() throws IOException {
        doReturn(newArrayList(repo)).when(nonactive).resolve(any(Predicate.class));
        when(repo.hasAdminAccess()).thenReturn(true);
        Predicate<GHHook> del = spy(Predicate.class);
        when(manager.deleteWebhook()).thenReturn(del);

        GHHook hook = hook(HOOK_ENDPOINT, CREATE);
        GHHook prhook = hook(HOOK_ENDPOINT, PULL_REQUEST);
        when(repo.getHooks()).thenReturn(newArrayList(hook, prhook));

        manager.createHookSubscribedTo(copyOf(newArrayList(PUSH))).apply(nonactive);
        verify(del, times(2)).apply(any(GHHook.class));
        verify(manager).createWebhook(HOOK_ENDPOINT, EnumSet.copyOf(newArrayList(CREATE, PULL_REQUEST, PUSH)));
    }

    @Test
    public void shouldNotReplaceAlreadyRegisteredHook() throws IOException {
        doReturn(newArrayList(repo)).when(nonactive).resolve(any(Predicate.class));
        when(repo.hasAdminAccess()).thenReturn(true);

        GHHook hook = hook(HOOK_ENDPOINT, PUSH);
        when(repo.getHooks()).thenReturn(newArrayList(hook));

        manager.createHookSubscribedTo(copyOf(newArrayList(PUSH))).apply(nonactive);
        verify(manager, never()).deleteWebhook();
        verify(manager, never()).createWebhook(any(URL.class), anySetOf(GHEvent.class));
    }

    @Test
    public void shouldNotAddPushEventByDefaultForProjectWithoutTrigger() throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(GIT_SCM);

        manager.registerFor((Item)project).run();
        verify(manager, never()).createHookSubscribedTo(anyListOf(GHEvent.class));
    }

    @Test
    public void shouldAddPushEventByDefault() throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addTrigger(new GitHubPushTrigger());
        project.setScm(GIT_SCM);

        manager.registerFor((Item)project).run();
        verify(manager).createHookSubscribedTo(newArrayList(PUSH));
    }

    @Test
    public void shouldReturnNullOnGettingEmptyEventsListToSubscribe() throws IOException {
        doReturn(newArrayList(repo)).when(active).resolve(any(Predicate.class));
        when(repo.hasAdminAccess()).thenReturn(true);

        assertThat("empty events list not allowed to be registered",
                forHookUrl(HOOK_ENDPOINT)
                        .createHookSubscribedTo(Collections.<GHEvent>emptyList()).apply(active), nullValue());
    }

    @Test
    public void shouldSelectOnlyHookManagedCreds() {
        GitHubServerConfig conf = new GitHubServerConfig("");
        conf.setManageHooks(false);
        GitHubPlugin.configuration().getConfigs().add(conf);

        assertThat(forHookUrl(HOOK_ENDPOINT).createHookSubscribedTo(Lists.newArrayList(PUSH))
                .apply(new GitHubRepositoryName("github.com", "name", "repo")), nullValue());
    }

    @Test
    public void shouldNotSelectCredsWithCustomHost() {
        GitHubServerConfig conf = new GitHubServerConfig("");
        conf.setApiUrl(ANOTHER_HOOK_ENDPOINT.toString());
        conf.setManageHooks(false);
        GitHubPlugin.configuration().getConfigs().add(conf);

        assertThat(forHookUrl(HOOK_ENDPOINT).createHookSubscribedTo(Lists.newArrayList(PUSH))
                .apply(new GitHubRepositoryName("github.com", "name", "repo")), nullValue());
    }

    @Test
    public void shouldSendSecretIfDefined() throws Exception {
        String secretText = "secret_text";

        storeSecretIn(GitHubPlugin.configuration(), secretText);

        manager.createWebhook(HOOK_ENDPOINT, ImmutableSet.of(PUSH)).apply(repo);

        verify(repo).createHook(
                anyString(),
                (Map<String, String>) argThat(hasEntry("secret", secretText)),
                anySetOf(GHEvent.class),
                anyBoolean()
        );

    }

    private GHHook hook(URL endpoint, GHEvent event, GHEvent... events) {
        GHHook hook = mock(GHHook.class);
        when(hook.getName()).thenReturn("web");
        when(hook.getConfig()).thenReturn(ImmutableMap.of("url", endpoint.toExternalForm()));
        when(hook.getEvents()).thenReturn(EnumSet.copyOf(asList(event, events)));
        return hook;
    }

    private static URL endpoint(String endpoint) {
        try {
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
