package test.performance;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runner for Load and Stress Tests (Test Lần 3).
 */
public class LoadStressTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(100));
        System.out.println("TEST LẦN 3: LOAD & STRESS TESTING - 50 CONCURRENT USERS");
        System.out.println("=".repeat(100));
        System.out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("=".repeat(100));
        System.out.println();
        
        // Discover and run LoadStressTest only
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(LoadStressTest.class))
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
        System.out.println("Total time:      " + totalTime + " ms");
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
            System.out.println(" ALL LOAD & STRESS TESTS PASSED!");
            System.out.println();
            System.out.println("Key Achievements:");
            System.out.println("-  50 concurrent users simulated successfully");
            System.out.println("-  Connection pool handled full capacity");
            System.out.println("-  System stable under sustained load");
            System.out.println("-  Zero critical errors under stress");
            System.exit(0);
        }
    }
}
