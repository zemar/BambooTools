/**
 * PlanSpec.java
 * 
 * Bamboo spec class that allows Bamboo plan to be defined (via the Plan type) and published.
 * The publish() operation will create the plan new OR overwrite any existing plan.
 * 
 * @author michael.howard
 * 
 */
package specs;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.AllOtherPluginsConfiguration;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.requirement.Requirement;
import com.atlassian.bamboo.specs.builders.task.AntTask;
import com.atlassian.bamboo.specs.builders.task.CommandTask;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;

/**
 * PlanSpec class definition.  The plan configuration for Bamboo.
 * Provides methods to configure a Bamboo build plan, plan permissions and a Bamboo server.
 * The final step is to publis to this server to make the plan active in Bamboo.
 * Learn more on: <a href="https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs</a>
 */
@BambooSpec
public class PlanSpec {

    public static String baseUrl = "https://bamboo.trustvesta.com/bamboo";
    public static String user = "michael.howard";
    public static String project = "DVOPS";
    public static String shortPlanName = "BambooSpecs-Test";
    public static String shortPlanKey = "BSTEST";
    
    /**
     * Run main to publish plan on Bamboo
     */
    public static void main(final String[] args) throws Exception {
        //By default credentials are read from the '.credentials' file.
        BambooServer bambooServer = new BambooServer(baseUrl);
        
        // Define and publish the Bamboo Spec
        Plan plan = new PlanSpec().createPlan();
        bambooServer.publish(plan);
        PlanPermissions planPermission = new PlanSpec().createPlanPermission(plan.getIdentifier());
        bambooServer.publish(planPermission);
    }
    
    /**
     * createPlanPermission()
     * 
     * Set all permissions for the build plan.
     * 
     * @param planIdentifier
     * @return PlanPermission object representing the plan permission scheme.
     * 
     */
    PlanPermissions createPlanPermission(PlanIdentifier planIdentifier) {
        Permissions permission = new Permissions()
                .userPermissions(user, PermissionType.ADMIN, PermissionType.CLONE, PermissionType.EDIT)
                .groupPermissions("bamboo-admin", PermissionType.ADMIN)
                .loggedInUserPermissions(PermissionType.VIEW)
                .anonymousUserPermissionView();
        return new PlanPermissions(planIdentifier.getProjectKey(), planIdentifier.getPlanKey()).permissions(permission);
    }

    /**
     * project()
     * 
     * Define and create the Project object to be used in creating the plan.
     * 
     * @return Project object representing which Bamboo project to create the plan in.
     * 
     */
    Project project() {
        return new Project()
                .name("Project Name")
                .key(project);
    }

    /**
     * createPlan()
     * 
     * Create a new Plan object containing all details (project, stages, jobs, tasks, etc...) for the 
     * Bamboo build plan.
     * 
     * @return Plan object representing the Bamboo build plan.
     * 
     */
    Plan createPlan() {
        return new Plan(project(), shortPlanName, shortPlanKey)
                .description("Build plan for " + shortPlanName)
                .pluginConfigurations(new ConcurrentBuilds().useSystemWideDefault(false))
                .stages(new Stage("Default Stage")
                        .jobs(new Job("Integration Tests", new BambooKey("ITJOB"))
                                .description("Deploy component and all dependencies using OCD and execute GRT.")
                                .pluginConfigurations(new AllOtherPluginsConfiguration()
                                        .configuration(new MapBuilder()
                                                .put("repositoryDefiningWorkingDirectory", -1)
                                                .put("custom", new MapBuilder()
                                                    .put("auto", new MapBuilder()
                                                        .put("regex", "")
                                                        .put("label", "")
                                                        .build())
                                                    .put("buildHangingConfig.enabled", "false")
                                                    .put("ncover.path", "")
                                                    .put("clover", new MapBuilder()
                                                        .put("path", "target/site/clover/clover.xml")
                                                        .put("license", "")
                                                        .put("integration", "custom")
                                                        .put("exists", "true")
                                                        .build())
                                                    .build())
                                                .build()))
                                .artifacts(new Artifact()
                                        .name("VSF Test Report (html)")
                                        .copyPattern("grt_summary.htm")
                                        .location("Tests/vsf"),
                                    new Artifact()
                                        .name("Clover Report (System)")
                                        .copyPattern("**/*.*")
                                        .location("target/site/clover"))
                                .tasks(//new VcsCheckoutTask()
                                        //.description("Checkout Default Repository")
                                        //.checkoutItems(new CheckoutItem().defaultRepository()),
                                    new CommandTask()
                                        .description("Deploy Stack (OCD)")
                                        .executable("ocd")
                                        .argument("DeployAll"),
                                    new AntTask()
                                        .description("Deploy Instrumented Apps (Clover)")
                                        .target("clover.deploy -Dport=8002")
                                        .jdk("JDK 1.8")
                                        .executableLabel("Ant"),
                                    new CommandTask()
                                        .description("Integration Tests (GRT)")
                                        .executable("grt")
                                        .argument("-n --junit junit.xml")
                                        .workingSubdirectory("Tests/vsf"))
                                .finalTasks(new AntTask()
                                        .description("Produce Clover Report")
                                        .target("clover.report")
                                        .jdk("JDK 1.8")
                                        .executableLabel("Ant") ),
//                                    new TestParserTask(TestParserTaskProperties.TestType.JUNIT)
//                                        .description("grt junit parse results")
//                                        .resultDirectories("Tests/vsf/junit.xml")),
                            new Job("Production Package",
                                new BambooKey("PPJOB"))
                                .pluginConfigurations(new AllOtherPluginsConfiguration()
                                        .configuration(new MapBuilder()
                                                .put("repositoryDefiningWorkingDirectory", -1)
                                                .build()))
                                .artifacts(new Artifact()
                                        .name("Package")
                                        .copyPattern("**/*")
                                        .location("dist")
                                        .shared(true),
                                    new Artifact()
                                        .name("Properties")
                                        .copyPattern("**/*")
                                        .location("etc"),
                                    new Artifact()
                                        .name("SIM Properties")
                                        .copyPattern("**/*")
                                        .location("sim/etc"),
                                    new Artifact()
                                        .name("Code Analysis Reports")
                                        .copyPattern("**/*")
                                        .location("analysis_results"),
                                    new Artifact()
                                        .name("POM")
                                        .copyPattern("pom.xml")
                                        .shared(true))
                                .tasks(//new VcsCheckoutTask()
                                        //.description("Checkout Default Repository")
                                        //.checkoutItems(new CheckoutItem().defaultRepository()),
                                    new AntTask()
                                        .description("Package")
                                        .target("package findbugs pmd checkstyle")
                                        .jdk("JDK 1.8")
                                        .executableLabel("Ant"))
                                .requirements(new Requirement("package_release")
                                        .matchType(Requirement.MatchType.EXISTS))
                                )
                        );
    }
}
