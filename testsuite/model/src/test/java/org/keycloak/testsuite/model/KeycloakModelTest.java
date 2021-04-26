/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.model;

import org.keycloak.Config.Scope;
import org.keycloak.authorization.AuthorizationSpi;
import org.keycloak.authorization.DefaultAuthorizationProviderFactory;
import org.keycloak.authorization.store.StoreFactorySpi;
import org.keycloak.cluster.ClusterSpi;
import org.keycloak.events.EventStoreSpi;
import org.keycloak.executors.DefaultExecutorsProviderFactory;
import org.keycloak.executors.ExecutorsSpi;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.ClientScopeSpi;
import org.keycloak.models.ClientSpi;
import org.keycloak.models.GroupSpi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmSpi;
import org.keycloak.models.RoleSpi;
import org.keycloak.models.ServerInfoProviderFactory;
import org.keycloak.models.ServerInfoSpi;
import org.keycloak.models.UserLoginFailureSpi;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.UserSpi;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.timer.TimerSpi;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Base of testcases that operate on session level. The tests derived from this class
 * will have access to a shared {@link KeycloakSessionFactory} in the {@link #LOCAL_FACTORY}
 * field that can be used to obtain a session and e.g. start / stop transaction.
 * <p>
 * This class expects {@code keycloak.model.parameters} system property to contain
 * comma-separated class names that implement {@link KeycloakModelParameters} interface
 * to provide list of factories and SPIs that are visible to the {@link KeycloakSessionFactory}
 * that is offered to the tests.
 * <p>
 * If no parameters are set via this property, the tests derived from this class are skipped.
 * @author hmlnarik
 */
public abstract class KeycloakModelTest {

    private static final Logger LOG = Logger.getLogger(KeycloakModelParameters.class);
    private static final AtomicInteger FACTORY_COUNT = new AtomicInteger();
    protected final Logger log = Logger.getLogger(getClass());
    private static final List<String> MAIN_THREAD_NAMES = Arrays.asList("main", "Time-limited test");

    @ClassRule
    public static final TestRule GUARANTEE_REQUIRED_FACTORY = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            Class<?> testClass = description.getTestClass();
            Stream<RequireProvider> st = Stream.empty();
            while (testClass != Object.class) {
                st = Stream.concat(Stream.of(testClass.getAnnotationsByType(RequireProvider.class)), st);
                testClass = testClass.getSuperclass();
            }
            List<Class<? extends Provider>> notFound = st
              .filter(rp -> rp.only().length == 0 
                ? getFactory().getProviderFactory(rp.value()) == null
                : Stream.of(rp.only()).allMatch(provider -> getFactory().getProviderFactory(rp.value(), provider) == null))
              .map(RequireProvider::value)
              .collect(Collectors.toList());
            Assume.assumeThat("Some required providers not found", notFound, Matchers.empty());

            Statement res = base;
            for (KeycloakModelParameters kmp : KeycloakModelTest.MODEL_PARAMETERS) {
                res = kmp.classRule(res, description);
            }
            return res;
        }
    };

    @Rule
    public final TestRule guaranteeRequiredFactoryOnMethod = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            Stream<RequireProvider> st = Optional.ofNullable(description.getAnnotation(RequireProviders.class))
              .map(RequireProviders::value)
              .map(Stream::of)
              .orElseGet(Stream::empty);

            RequireProvider rp = description.getAnnotation(RequireProvider.class);
            if (rp != null) {
                st = Stream.concat(st, Stream.of(rp));
            }

            for (Iterator<RequireProvider> iterator = st.iterator(); iterator.hasNext();) {
                RequireProvider rpInner = iterator.next();
                Class<? extends Provider> providerClass = rpInner.value();
                String[] only = rpInner.only();

                if (only.length == 0) {
                    if (getFactory().getProviderFactory(providerClass) == null) {
                        return new Statement() {
                            @Override
                            public void evaluate() throws Throwable {
                                throw new AssumptionViolatedException("Provider must exist: " + providerClass);
                            }
                        };
                    }
                } else {
                    boolean notFoundAny = Stream.of(only).allMatch(provider -> getFactory().getProviderFactory(providerClass, provider) == null);
                    if (notFoundAny) {
                        return new Statement() {
                            @Override
                            public void evaluate() throws Throwable {
                                throw new AssumptionViolatedException("Provider must exist: " + providerClass + " one of [" + String.join(",", only) + "]");
                            }
                        };
                    }
                }
            }

            Statement res = base;
            for (KeycloakModelParameters kmp : KeycloakModelTest.MODEL_PARAMETERS) {
                res = kmp.instanceRule(res, description);
            }
            return res;
        }
    };

    private static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(AuthorizationSpi.class)
      .add(ClientScopeSpi.class)
      .add(ClientSpi.class)
      .add(ClusterSpi.class)
      .add(EventStoreSpi.class)
      .add(ExecutorsSpi.class)
      .add(GroupSpi.class)
      .add(RealmSpi.class)
      .add(RoleSpi.class)
      .add(ServerInfoSpi.class)
      .add(StoreFactorySpi.class)
      .add(TimerSpi.class)
      .add(UserLoginFailureSpi.class)
      .add(UserSessionSpi.class)
      .add(UserSpi.class)
      .build();

    private static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(DefaultAuthorizationProviderFactory.class)
      .add(DefaultExecutorsProviderFactory.class)
      .add(ServerInfoProviderFactory.class)
      .build();

    protected static final List<KeycloakModelParameters> MODEL_PARAMETERS;
    protected static final Config CONFIG = new Config();
    private static volatile KeycloakSessionFactory DEFAULT_FACTORY;
    private static final ThreadLocal<KeycloakSessionFactory> LOCAL_FACTORY = new ThreadLocal<>();
    protected static boolean USE_DEFAULT_FACTORY = false;

    static {
        org.keycloak.Config.init(CONFIG);

        KeycloakModelParameters basicParameters = new KeycloakModelParameters(ALLOWED_SPIS, ALLOWED_FACTORIES);
        MODEL_PARAMETERS = Stream.concat(
          Stream.of(basicParameters),
          Stream.of(System.getProperty("keycloak.model.parameters", "").split("\\s*,\\s*"))
            .filter(s -> s != null && ! s.trim().isEmpty())
            .map(cn -> { try { return Class.forName(cn.indexOf('.') >= 0 ? cn : ("org.keycloak.testsuite.model.parameters." + cn)); } catch (Exception e) { LOG.error("Cannot find " + cn); return null; }})
            .filter(Objects::nonNull)
            .map(c -> { try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { LOG.error("Cannot instantiate " + c); return null; }} )
            .filter(KeycloakModelParameters.class::isInstance)
            .map(KeycloakModelParameters.class::cast)
          )
          .collect(Collectors.toList());

        reinitializeKeycloakSessionFactory();
        DEFAULT_FACTORY = getFactory();
    }

    /**
     * Creates a fresh initialized {@link KeycloakSessionFactory}. The returned factory uses configuration
     * local to the thread that calls this method, allowing for per-thread customization. This in turn allows
     * testing of several parallel session factories which can be used to simulate several servers
     * running in parallel.
     * @return
     */
    public static KeycloakSessionFactory createKeycloakSessionFactory() {
        int factoryIndex = FACTORY_COUNT.incrementAndGet();
        String threadName = Thread.currentThread().getName();
        CONFIG.reset();
        MODEL_PARAMETERS.forEach(m -> m.updateConfig(CONFIG));

        LOG.debugf("Creating factory %d in %s using the following configuration:\n    %s", factoryIndex, threadName, CONFIG);

        DefaultKeycloakSessionFactory res = new DefaultKeycloakSessionFactory() {
            @Override
            protected boolean isEnabled(ProviderFactory factory, Scope scope) {
                return super.isEnabled(factory, scope) && isFactoryAllowed(factory);
            }

            @Override
            protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> loadFactories(ProviderManager pm) {
                spis.removeIf(s -> ! isSpiAllowed(s));
                return super.loadFactories(pm);
            }

            private boolean isSpiAllowed(Spi s) {
                return MODEL_PARAMETERS.stream().anyMatch(p -> p.isSpiAllowed(s));
            }

            private boolean isFactoryAllowed(ProviderFactory factory) {
                return MODEL_PARAMETERS.stream().anyMatch(p -> p.isFactoryAllowed(factory));
            }

            @Override
            public String toString() {
                return "KeycloakSessionFactory " + factoryIndex + " (from " + threadName + " thread)";
            }
        };
        res.init();
        res.publish(new PostMigrationEvent());
        return res;
    }

    /**
     * Closes and initializes new {@link #LOCAL_FACTORY}. This has the same effect as server restart in full-blown server scenario.
     */
    public static synchronized void reinitializeKeycloakSessionFactory() {
        closeKeycloakSessionFactory();
        setFactory(createKeycloakSessionFactory());
    }

    public static synchronized void closeKeycloakSessionFactory() {
        KeycloakSessionFactory f = getFactory();
        setFactory(null);
        if (f != null) {
            LOG.debugf("Closing %s", f);
            f.close();
        }
    }

    /**
     * Runs the given {@code task} in {@code numThreads} parallel threads, each thread operating
     * in the context of a fresh {@link KeycloakSessionFactory} independent of each other thread.
     *
     * @see #inIndependentFactory
     *
     * @param numThreads
     * @param timeoutSeconds
     * @param task
     * @throws InterruptedException
     */
    public static void inIndependentFactories(int numThreads, int timeoutSeconds, Runnable task) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(numThreads);
        try {
            Callable<?> independentTask = () -> inIndependentFactory(() -> { task.run(); return null; });
            es.invokeAll(
              IntStream.range(0, numThreads)
                .mapToObj(i -> independentTask)
                .collect(Collectors.toList()),
              timeoutSeconds, TimeUnit.SECONDS
            );
        } finally {
            es.shutdownNow();
        }
    }

    /**
     * Runs the given {@code task} in a context of a fresh {@link KeycloakSessionFactory} which is created before
     * running the task and destroyed afterwards.
     * @return
     */
    public static <T> T inIndependentFactory(Callable<T> task) {
        if (USE_DEFAULT_FACTORY) {
            throw new IllegalStateException("USE_DEFAULT_FACTORY must be false to use an independent factory");
        }
        KeycloakSessionFactory original = getFactory();
        KeycloakSessionFactory factory = createKeycloakSessionFactory();
        try {
            setFactory(factory);
            return task.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            closeKeycloakSessionFactory();
            setFactory(original);
        }
    }

    private static boolean isOnMainThread() {
        return MAIN_THREAD_NAMES.contains(Thread.currentThread().getName());
    }

    protected static KeycloakSessionFactory getFactory() {
        return (USE_DEFAULT_FACTORY || isOnMainThread()) ? DEFAULT_FACTORY : LOCAL_FACTORY.get();
    }

    private static void setFactory(KeycloakSessionFactory factory) {
        if (USE_DEFAULT_FACTORY || isOnMainThread()) {
            DEFAULT_FACTORY = factory;
        } else {
            LOCAL_FACTORY.set(factory);
        }
    }

    @BeforeClass
    public static void checkValidParameters() {
        Assume.assumeTrue("keycloak.model.parameters property must be set", MODEL_PARAMETERS.size() > 1);   // Additional parameters have to be set
    }

    protected void createEnvironment(KeycloakSession s) {
    }

    protected void cleanEnvironment(KeycloakSession s) {
    }

    @Before
    public void createEnvironment() {
        USE_DEFAULT_FACTORY = isUseSameKeycloakSessionFactoryForAllThreads();
        KeycloakModelUtils.runJobInTransaction(getFactory(), this::createEnvironment);
    }

    @After
    public void cleanEnvironment() {
        if (getFactory() == null) {
            reinitializeKeycloakSessionFactory();
        }
        KeycloakModelUtils.runJobInTransaction(getFactory(), this::cleanEnvironment);
    }

    protected <T> Stream<T> getParameters(Class<T> clazz) {
        return MODEL_PARAMETERS.stream().flatMap(mp -> mp.getParameters(clazz)).filter(Objects::nonNull);
    }

    protected <T> void withEach(Class<T> parameterClazz, Consumer<T> what) {
        getParameters(parameterClazz).forEach(what);
    }

    protected <T> void inRolledBackTransaction(T parameter, BiConsumer<KeycloakSession, T> what) {
        KeycloakSession session = getFactory().create();
        session.getTransactionManager().begin();

        what.accept(session, parameter);

        session.getTransactionManager().rollback();
    }

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what) {
        return inComittedTransaction(parameter, what, null, null);
    }

    protected void inComittedTransaction(Consumer<KeycloakSession> what) {
        inComittedTransaction(a -> { what.accept(a); return null; });
    }

    protected <R> R inComittedTransaction(Function<KeycloakSession, R> what) {
        return inComittedTransaction(1, (a,b) -> what.apply(a), null, null);
    }

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what, BiConsumer<KeycloakSession, T> onCommit, BiConsumer<KeycloakSession, T> onRollback) {
        AtomicReference<R> res = new AtomicReference<>();
        KeycloakModelUtils.runJobInTransaction(getFactory(), session -> {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    if (onCommit != null) { onCommit.accept(session, parameter); }
                }

                @Override
                protected void rollbackImpl() {
                    if (onRollback != null) { onRollback.accept(session, parameter); }
                }
            });
            res.set(what.apply(session, parameter));
        });
        return res.get();
    }

    /**
     * Convenience method for {@link #inComittedTransaction(java.util.function.Consumer)} that
     * obtains realm model from the session and puts it into session context before
     * running the {@code what} task.
     * @param <R>
     * @param realmId
     * @param what
     * @return
     */
    protected <R> R withRealm(String realmId, BiFunction<KeycloakSession, RealmModel, R> what) {
        return inComittedTransaction(session -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            return what.apply(session, realm);
        });
    }

    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return false;
    }
}
