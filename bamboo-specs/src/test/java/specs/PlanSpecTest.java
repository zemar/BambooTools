package specs;

import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders;

import specs.PlanSpec;

import org.junit.Test;

public class PlanSpecTest {
    @Test
    public void checkYourPlanOffline() throws PropertiesValidationException {
        Plan plan = new PlanSpec().createPlan();

        EntityPropertiesBuilders.build(plan);
    }
}
