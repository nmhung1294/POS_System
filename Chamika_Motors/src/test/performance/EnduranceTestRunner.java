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
 * Runner for Endurance Tests (Test Lần 4).
 * 
 * NOTE: This runs shorter endurance tests (1 min, 5 min) suitable for CI/CD.
 * For full 24-hour endurance testing, run in production-like environment.
 */
public class EnduranceTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("TEST LẦN 4: ENDURANCE TESTING - LONG-TERM STABILITY");
        System.out.println("=".repeat(100));
        System.out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Note: Running shorter tests (1 min, 5 min) suitable for CI/CD");
        System.out.println("      Full 24h endurance test should be run in production environment");
        System.out.println("=".repeat(100));
        System.out.println();
        
        // Discover and run EnduranceTest only
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(EnduranceTest.class))
            .build();
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        // Get summary
        TestExecutionSummary summary = listener.getSummary();
        
        // Print results
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("=".repeat(100));
        System.out.println("Tests found:    " + summary.getTestsFoundCount());
        System.out.println("Tests started:  " + summary.getTestsStartedCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount() + " ");
        System.out.println("Tests failed:    " + summary.getTestsFailedCount() + " X");
        System.out.println("Tests skipped:   " + summary.getTestsSkippedCount() + " O");
        long totalTime = summary.getTimeFinished() - summary.getTimeStarted();
        System.out.println("Total time:      " + totalTime + " ms (" + (totalTime / 1000 / 60) + " minutes)");
        System.out.println("=".repeat(100));
        System.out.println();
        
        if (summary.getTestsFailedCount() > 0) {
            System.out.println("X SOME TESTS FAILED!");
            summary.getFailures().forEach(failure -> {
                System.out.println("Failed: " + failure.getTestIdentifier().getDisplayName());
                System.out.println("Reason: " + failure.getException().getMessage());
            });
            System.exit(1);
        } else {
            System.out.println(" ALL ENDURANCE TESTS PASSED!");
            System.out.println();
            System.out.println("Key Achievements:");
            System.out.println("-  System stable over extended time periods");
            System.out.println("-  No memory leaks detected");
            System.out.println("-  Connection pool resilient under continuous load");
            System.out.println("-  Performance consistent with no degradation");
            System.out.println();
            System.out.println("Next Steps:");
            System.out.println("- For production deployment, run 24-hour endurance test");
            System.out.println("- Monitor: memory usage, connection pool, error rates");
            System.out.println("- Verify: no performance degradation over full day");
            System.exit(0);
        }
    }
}
