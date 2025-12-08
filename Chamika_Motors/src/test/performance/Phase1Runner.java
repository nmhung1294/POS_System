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
 * Phase 1: Basic Performance Tests
 * - DatabasePerformanceTest (8 tests)
 * - TransactionPerformanceTest (4 tests)
 */
public class Phase1Runner {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("PHASE 1: BASIC PERFORMANCE TESTS");
        System.out.println("=".repeat(100));
        System.out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Tests: Database Performance + Transaction Performance");
        System.out.println("=".repeat(100));
        System.out.println();
        
        // Run tests
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(
                selectClass(DatabasePerformanceTest.class),
                selectClass(TransactionPerformanceTest.class)
            )
            .build();
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        // Print summary
        TestExecutionSummary summary = listener.getSummary();
        printSummary(summary);
        
        int exitCode = summary.getTotalFailureCount() > 0 ? 1 : 0;
        System.exit(exitCode);
    }
    
    private static void printSummary(TestExecutionSummary summary) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("PHASE 1 RESULTS");
        System.out.println("=".repeat(100));
        System.out.println(String.format("Tests found:     %d", summary.getTestsFoundCount()));
        System.out.println(String.format("Tests started:   %d", summary.getTestsStartedCount()));
        System.out.println(String.format("Tests succeeded: %d ", summary.getTestsSucceededCount()));
        System.out.println(String.format("Tests failed:    %d X", summary.getTestsFailedCount()));
        System.out.println(String.format("Tests skipped:   %d O", summary.getTestsSkippedCount()));
        System.out.println(String.format("Total time:      %d ms (%.1f seconds)", 
            summary.getTimeFinished() - summary.getTimeStarted(),
            (summary.getTimeFinished() - summary.getTimeStarted()) / 1000.0));
        System.out.println("=".repeat(100));
        
        if (summary.getTotalFailureCount() > 0) {
            System.out.println("\nFAILURES:");
            summary.getFailures().forEach(failure -> {
                System.out.println("  X " + failure.getTestIdentifier().getDisplayName());
                System.out.println("     " + failure.getException().getMessage());
            });
        } else {
            System.out.println("\n ALL PHASE 1 TESTS PASSED!");
        }
        System.out.println("=".repeat(100));
    }
}
