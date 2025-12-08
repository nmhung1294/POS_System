package test.performance;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Phase 4: Endurance Tests
 * - EnduranceTest (4 tests: 1min, 5min, 15min, Memory Leak)
 */
public class Phase4Runner {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("PHASE 4: ENDURANCE TESTS");
        System.out.println("=".repeat(100));
        System.out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Tests: Long-running tests (1min, 5min, 15min) + Memory Leak Detection");
        System.out.println("WARNING: This will take approximately 21+ minutes!");
        System.out.println("=".repeat(100));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        // Run tests
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(EnduranceTest.class))
            .build();
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        long endTime = System.currentTimeMillis();
        long totalMinutes = (endTime - startTime) / 1000 / 60;
        
        // Print summary
        TestExecutionSummary summary = listener.getSummary();
        printSummary(summary, totalMinutes);
        
        int exitCode = summary.getTotalFailureCount() > 0 ? 1 : 0;
        System.exit(exitCode);
    }
    
    private static void printSummary(TestExecutionSummary summary, long totalMinutes) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("PHASE 4 RESULTS");
        System.out.println("=".repeat(100));
        System.out.println(String.format("Tests found:     %d", summary.getTestsFoundCount()));
        System.out.println(String.format("Tests started:   %d", summary.getTestsStartedCount()));
        System.out.println(String.format("Tests succeeded: %d ", summary.getTestsSucceededCount()));
        System.out.println(String.format("Tests failed:    %d X", summary.getTestsFailedCount()));
        System.out.println(String.format("Tests skipped:   %d O", summary.getTestsSkippedCount()));
        System.out.println(String.format("Total time:      %d ms (~%d minutes)", 
            summary.getTimeFinished() - summary.getTimeStarted(), totalMinutes));
        System.out.println("=".repeat(100));
        
        if (summary.getTotalFailureCount() > 0) {
            System.out.println("\nFAILURES:");
            summary.getFailures().forEach(failure -> {
                System.out.println("  X " + failure.getTestIdentifier().getDisplayName());
                System.out.println("     " + failure.getException().getMessage());
            });
        } else {
            System.out.println("\n ALL PHASE 4 TESTS PASSED!");
            System.out.println(" No memory leaks detected!");
        }
        System.out.println("=".repeat(100));
    }
}
