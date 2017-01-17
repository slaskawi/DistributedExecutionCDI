import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.SerializableFunction;
import org.jboss.weld.environment.se.events.ContainerInitialized;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Created by Seto on 2017/1/17.
 */
public class Test {
    @Inject
    EmbeddedCacheManager ecm;

    @Inject
    @TestCache
    Cache<String, Object> testCache;

    public static void main(String[] args) {
        CdiContainer cdiContainer = CdiContainerLoader.getCdiContainer();
        cdiContainer.boot();

        // Starting the application-context enables use of @ApplicationScoped beans
        ContextControl contextControl = cdiContainer.getContextControl();
        contextControl.startContext(ApplicationScoped.class);

        // You can use CDI here
        //cdiContainer.shutdown();
    }

    public void onContainerInitialized(@Observes ContainerInitialized event) {
        System.out.println(testCache);

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // it seems that only Callable is working with CDI
        DefaultExecutorService des = new DefaultExecutorService(testCache);
        des.submit(new TestRunnable());
        des.submit(new TestCallable());

        scanner.nextLine();

        // Runnable won't work with CDI, I have to use EmbeddedCacheManager to get the cache myself
        testCache.getCacheManager().executor().submit(new TestRunnable());
        testCache.getCacheManager().executor().submitConsumer(new TestFunction(), null);

        scanner.nextLine();

        // the same as above
        ecm.executor().submit(new TestRunnable());
        ecm.executor().submitConsumer(new TestFunction(), null);

        scanner.nextLine();

        // thread issue open two to test, the node who submitEverywhere will run in the same thread one by one
        // the node accept will run in multiple threads concurrently
        for (int i = 0; i < 5; i++) {
            des.submitEverywhere(new TestCallableThread());
        }

        scanner.nextLine();

        // cluster executor all node will run in multiple threads concurrently
        for (int i = 0; i < 5; i++) {
            testCache.getCacheManager().executor().submit(new TestRunnableThread());
        }

        scanner.nextLine();

        // cluster executor all node will run in multiple threads concurrently
        for (int i = 0; i < 5; i++) {
            testCache.getCacheManager().executor().submitConsumer(new TestFunctionThread(), null);
        }

        System.out.println("container initialized");
    }

    private static class TestRunnable implements Runnable, Serializable {
        @Inject
        @TestCache
        Cache<String, Object> testCache;

        @Override
        public void run() {
            System.out.println("TestRunnable");
            System.out.println(testCache);
        }
    }

    private static class TestCallable implements Callable<Void>, Serializable {
        @Inject
        @TestCache
        Cache<String, Object> testCache;

        @Override
        public Void call() {
            System.out.println("TestCallable");
            System.out.println(testCache);
            return null;
        }
    }

    private static class TestFunction implements Function<EmbeddedCacheManager, Void>, Serializable {
        @Override
        public Void apply(EmbeddedCacheManager embeddedCacheManager) {
            System.out.println("TestFunction0");
            Cache<String, Object> testCache = embeddedCacheManager.getCache("test-cache");
            System.out.println(testCache);
            return null;
        }
    }

    private static class TestRunnableThread implements Runnable, Serializable {
        @Override
        public void run() {
            System.out.println("TestRunnableThread begin" + Thread.currentThread());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("TestRunnableThread end" + Thread.currentThread());
        }
    }

    private static class TestCallableThread implements Callable<Void>, Serializable {
        @Override
        public Void call() {
            System.out.println("TestCallableThread begin" + Thread.currentThread());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("TestCallableThread end" + Thread.currentThread());
            return null;
        }
    }

    private static class TestFunctionThread implements Function<EmbeddedCacheManager, Void>, Serializable {
        @Override
        public Void apply(EmbeddedCacheManager embeddedCacheManager) {
            System.out.println("TestFunctionThread begin" + Thread.currentThread());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("TestFunctionThread end" + Thread.currentThread());
            return null;
        }
    }

}
