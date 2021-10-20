package org.eclipse.dataspaceconnector.catalog.cache.management;

import org.eclipse.dataspaceconnector.catalog.spi.Crawler;
import org.eclipse.dataspaceconnector.catalog.spi.PartitionManager;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.catalog.spi.WorkItemQueue;
import org.eclipse.dataspaceconnector.catalog.spi.model.ExecutionPlan;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PartitionManagerImpl implements PartitionManager {
    private final Queue<ExecutionPlan> scheduledUpdates;
    private final Monitor monitor;
    private final Function<WorkItemQueue, Crawler> crawlerGenerator;
    private final List<Crawler> crawlers;
    private final WorkItemQueue workQueue;
    private final List<WorkItem> staticWorkLoad;
    private ExecutorService crawlerScheduler;

    /**
     * Instantiates a new PartitionManagerImpl.
     *
     * @param workQueue        An implementation of a blocking {@link WorkItemQueue}
     * @param monitor          A {@link Monitor}
     * @param crawlerGenerator A generator function that MUST create a new instance of a {@link Crawler}
     * @param numCrawlers      A number indicating how many {@code Crawler} instances should be generated.
     *                         Note that the PartitionManager may choose to generate more or less, e.g. because of constrained system resources.
     * @param staticWorkLoad   A fixed list of {@link WorkItem} instances that need to be processed on every execution run. This list is treated as immutable,
     *                         the PartitionManager will only read from it.
     */
    public PartitionManagerImpl(Monitor monitor, WorkItemQueue workQueue, Function<WorkItemQueue, Crawler> crawlerGenerator, int numCrawlers, List<WorkItem> staticWorkLoad) {
        this.monitor = monitor;
        this.staticWorkLoad = staticWorkLoad;
        this.workQueue = workQueue;
        this.crawlerGenerator = crawlerGenerator;
        scheduledUpdates = new ConcurrentLinkedQueue<>();

        // create "numCrawlers" crawlers using the generator function
        crawlers = createCrawlers(numCrawlers);

        // crawlers will start running as soon as the workQueue gets populated
        startCrawlers(crawlers);
    }

    @Override
    public @NotNull ExecutionPlan update(ExecutionPlan newPlan) {
        if (!scheduledUpdates.offer(newPlan)) {
            monitor.severe("PartitionManager Update was not scheduled!");
        }

        if (!waitForCrawlers()) {
            monitor.severe("Warning: not all crawlers finished in time!");
        }
        return collateUpdates(scheduledUpdates);
    }

    @Override
    public void schedule(ExecutionPlan executionPlan) {
        //todo: should we really discard updates?
        executionPlan.run(() -> {
            monitor.debug("partition manager: execute plan - waiting for queue lock");
            workQueue.lock();
            monitor.debug("partition manager: execute plan - adding workload " + staticWorkLoad.size());
            workQueue.addAll(staticWorkLoad);
            monitor.debug("partition manager: execute release queue lock");
            workQueue.unlock();
        });
    }

    @Override
    public void stop() {
        waitForCrawlers();
    }

    private List<Crawler> createCrawlers(int numCrawlers) {
        return IntStream.range(0, numCrawlers).mapToObj(i -> crawlerGenerator.apply(workQueue)).collect(Collectors.toList());
    }

    private @NotNull ExecutionPlan collateUpdates(Collection<ExecutionPlan> scheduledUpdates) {
        return scheduledUpdates.stream().reduce(ExecutionPlan::merge).orElseThrow();
    }

    private Boolean waitForCrawlers() {
        if (crawlers == null || crawlers.isEmpty()) {
            return true;
        }
        return crawlers.stream().allMatch(Crawler::join);
    }

    private void startCrawlers(List<Crawler> crawlers) {
        if (crawlerScheduler != null) crawlerScheduler.shutdownNow();

        crawlerScheduler = Executors.newScheduledThreadPool(crawlers.size());
        crawlers.forEach(crawlerScheduler::submit);
    }
}