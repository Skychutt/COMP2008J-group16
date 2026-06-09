import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple test runner that uses reflection to discover and run
 * methods annotated with org.junit.jupiter.api.Test.
 */
public class SimpleTestRunner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java SimpleTestRunner [ClassName1] [ClassName2] ...");
            System.exit(1);
        }

        int total = 0;
        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (String className : args) {
            Class<?> testClass = Class.forName(className);
            java.lang.reflect.Constructor<?> ctor = testClass.getDeclaredConstructor();
            ctor.setAccessible(true);

            List<Method> beforeEach = new ArrayList<>();
            List<Method> afterEach = new ArrayList<>();
            List<Method> tests = new ArrayList<>();
            for (Method method : testClass.getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class) || method.isAnnotationPresent(org.junit.Before.class)) {
                    beforeEach.add(method);
                } else if (method.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class) || method.isAnnotationPresent(org.junit.After.class)) {
                    afterEach.add(method);
                } else if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class) || method.isAnnotationPresent(org.junit.Test.class)) {
                    tests.add(method);
                }
            }

            System.out.println("Running: " + className);
            System.out.println("--------------------------------------------------");
            for (Method method : tests) {
                total++;
                String testName = className + "." + method.getName();
                System.out.print("  " + method.getName() + ": ");
                Object instance = ctor.newInstance();
                try {
                    for (Method bm : beforeEach) bm.invoke(instance);
                    method.invoke(instance);
                    for (Method am : afterEach) am.invoke(instance);
                    System.out.println("PASS");
                    passed++;
                } catch (Throwable t) {
                    System.out.println("FAIL");
                    String cause = t.getCause() != null ? t.getCause().toString() : t.toString();
                    System.out.println("    Reason: " + cause);
                    failed++;
                    failures.add(testName + " -> " + cause);
                }
            }
            System.out.println();
        }

        System.out.println("==================================================");
        System.out.println("Results: " + passed + " passed, " + failed + " failed out of " + total + " tests");
        if (!failures.isEmpty()) {
            System.out.println("Failed tests:");
            for (String f : failures) {
                System.out.println("  - " + f);
            }
        }
        System.exit(failed > 0 ? 1 : 0);
    }
}
